/*
 *     Copyright (C) 2017 Dzmitry Lazerka
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package me.lazerka.mf.android.location;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.util.Log;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.messaging.RemoteMessage.Builder;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.activity.MainActivity;
import me.lazerka.mf.android.adapter.PersonInfo;
import me.lazerka.mf.android.background.ApiPost;
import me.lazerka.mf.android.background.location.LocationRequestHandler;
import me.lazerka.mf.api.EmailNormalized;
import me.lazerka.mf.api.gcm.GcmPayload;
import me.lazerka.mf.api.object.LocationRequest2;
import me.lazerka.mf.api.object.LocationRequestFromServer;
import me.lazerka.mf.api.object.LocationResponse;
import me.lazerka.mf.api.object.UserFindId;
import okhttp3.Call;
import okhttp3.Callback;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static android.content.Context.MODE_PRIVATE;

/**
 * Handles location-related things.
 *
 * @author Dzmitry Lazerka
 */
public class LocationService {
	private static final Logger logger = LoggerFactory.getLogger(LocationService.class);
	private static final String TOPICS_SUBSCRIBED = "topicsSubscribed";

	public static final int TRACKING_NOTIFICATION_ID = 4321;
	public static final int FORBIDDEN_NOTIFICATION_ID = 4322;

	private final Context context;
	private final SharedPreferences preferences;
	private final LocationRequestHandler locationRequestHandler;

	private final BehaviorSubject<FriendLocationResponse> locationUpdates = BehaviorSubject.create();
	private final FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();

	public LocationService(Context context) {
		this.context = context;
		this.preferences = context.getSharedPreferences(context.getString(R.string.preferences_file_friends),
				MODE_PRIVATE);
		this.locationRequestHandler = new LocationRequestHandler(context);
	}

	/**
	 * @return "769083712074"
	 */
	String getDefaultSenderId() {
		return context.getString(R.string.gcm_defaultSenderId);
	}

	public Observable<FriendLocationResponse> getLocationUpdates() {
		return locationUpdates;
	}

	public void requestLocationUpdates(
			GoogleSignInAccount account,
			PersonInfo to,
			Duration duration,
			Callback locationRequestCallback
	) throws NoEmailsException
	{
		LocationRequest2 locationRequest = buildLocationRequest(to, duration);

		Call call = new ApiPost(locationRequest).newCall(account);
		call.enqueue(locationRequestCallback);

		String topic = locationRequest.getUpdatesTopic();
		firebaseMessaging.subscribeToTopic(topic);

		// DEBUG
		//logger.info("Subscribing to {}", topic);

		saveToSharedPreferences(topic);
	}

	LocationRequest2 buildLocationRequest(
			PersonInfo to,
			Duration duration
	) throws NoEmailsException
	{
		String updatesTopic = TopicName.random(to.lookupKey)
				.toString();

		if (to.emails == null || to.emails.isEmpty()) {
			throw new NoEmailsException();
		}

		return new LocationRequest2(
				updatesTopic,
				new UserFindId(to.emails),
				duration);
	}

	public void handleRequest(
			LocationRequestFromServer locationRequest,
			String gcmMessageFrom,
			DateTime gcmSentAt
	) {
		if (!gcmMessageFrom.equals(getDefaultSenderId())) {
			String msg = "GCM message from unknown sender rejected: " + gcmMessageFrom;
			logger.error(msg);
			FirebaseCrash.report(new IllegalArgumentException(msg));
			return;
		}

		String requesterEmail = locationRequest.getRequesterEmail();
		logger.info("Received location request from " + requesterEmail);

		// Authorize request.
		PersonInfo friend = authorizeRequest(requesterEmail);

		// TODO: if no such user in friends, we may still want to show a confirmation popup to user,
		// so they could authorize a verified email even if it's not friended yet.

		if (friend != null) {
			locationRequestHandler.processAuthorizedRequest(locationRequest, friend);
		} else {

			LocationResponse locationResponse = LocationResponse.denied();

			sendLocationUpdate(locationResponse, locationRequest.getUpdatesTopic());

			// TODO add setting "Ignore requests from non-friends to prevent spamming".
			showForbiddenNotification(requesterEmail);
		}
	}

	private PersonInfo authorizeRequest(String requesterEmail) {
		List<PersonInfo> friends;

		try {
			Future<List<PersonInfo>> future =
					Application.getFriendsManager().getFriends();

			friends = future.get();
		} catch (InterruptedException | ExecutionException e) {
			FirebaseCrash.report(e);
			return null;
		}

		for(PersonInfo friend : friends) {
			if (friend.emails.contains(requesterEmail)) {
				return friend;
			}

			for(String friendEmail : friend.emails) {
				EmailNormalized normalized = EmailNormalized.normalizeEmail(friendEmail);
				if (normalized.getEmail().equals(requesterEmail)) {
					return friend;
				}
			}
		}

		FirebaseCrash.logcat(
				Log.WARN, logger.getName(), "Requester not in friends list, rejecting " + requesterEmail);

		return null;
	}

	private void showForbiddenNotification(String requesterEmail) {
		String message = context.getString(R.string.requester_not_in_friends, requesterEmail);
		Notification notification = getNotificationBuilder(message)
				.setAutoCancel(true)
				.build();

		NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(requesterEmail, FORBIDDEN_NOTIFICATION_ID, notification);
	}

	private NotificationCompat.Builder getNotificationBuilder(String message) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

		Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		return new NotificationCompat.Builder(context)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(context.getString(R.string.app_name))
				.setStyle(new BigTextStyle().bigText(message))
				.setSound(defaultSoundUri)
				.setContentText(message)
				.setContentIntent(pendingIntent);
	}

	public void sendLocationUpdate(LocationResponse locationResponse, String topic) {
		String json;
		try {
			json = Application.getJsonMapper().writeValueAsString(locationResponse);
		} catch (JsonProcessingException e) {
			FirebaseCrash.report(e);
			return;
		}

		ImmutableMap<String, String> data = ImmutableMap.of(
				GcmPayload.TYPE_FIELD, LocationResponse.TYPE,
				GcmPayload.PAYLOAD_FIELD, json
		);
		RemoteMessage gcmMessage = new Builder("/topics/" + topic) // Should be prefixed with "/topics/"?
				.setData(data)
				.build();
		// DEBUG
		//logger.info("Sending location update to {}", topic);
		logger.info("Sending location update", topic);
		firebaseMessaging.send(gcmMessage);
	}

	/**
	 * @param topicFrom Like "/topics/61b166a188d-1715ia0e28ef087ba578"
	 */
	public void handleLocationResponse(LocationResponse response, String topicFrom) {

		if (response.isComplete()) {
			logger.info("Finished location session {}, unsubscribing.", topicFrom);
			firebaseMessaging.unsubscribeFromTopic(topicFrom);
		}

		if (!response.isSuccessful()) {
			logger.warn("LocationResponse unsuccessful: {}, unsubscribing from {}.", response.getError(), topicFrom);
			firebaseMessaging.unsubscribeFromTopic(topicFrom);
		}

		if (response.getLocation() != null) {
			TopicName topicName = TopicName.parse(topicFrom);

			PersonInfo senderContact = Application.getFriendsManager().getFriend(topicName.getFriendLookupKey());

			FriendLocationResponse friendLocationResponse = new FriendLocationResponse(senderContact, response);
			locationUpdates.onNext(friendLocationResponse);
		}
	}

	private boolean saveToSharedPreferences(String topic) {
		// Remember all the topics we listen to, to clean up later.
		synchronized (preferences) {
			// Clone, otherwise value won't be set.
			Set<String> set = new LinkedHashSet<>(preferences.getStringSet(TOPICS_SUBSCRIBED, new HashSet<>(1)));

			boolean changed = set.add(topic);
			if (!changed) {
				FirebaseCrash.logcat(Log.WARN, logger.getName(), "Already subscribed to topic");
				return true;
			}
			preferences.edit()
					.putStringSet(TOPICS_SUBSCRIBED, set)
					.apply();
		}
		return false;
	}

	private boolean removeFromSharedPreferences(String topic) {
		// Remember all the topics we listen to, to clean up later.
		synchronized (preferences) {
			// Clone, otherwise value won't be set.
			Set<String> set = new LinkedHashSet<>(preferences.getStringSet(TOPICS_SUBSCRIBED, new HashSet<>(0)));

			boolean changed = set.remove(topic);
			if (!changed) {
				FirebaseCrash.logcat(Log.WARN, logger.getName(), "Already unsubscribed from topic");
				return true;
			}
			preferences.edit()
					.putStringSet(TOPICS_SUBSCRIBED, set)
					.apply();
		}
		return false;
	}


	public static class NoEmailsException extends Exception {
	}
}
