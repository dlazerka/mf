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

import com.baraded.mf.logging.LogService;
import com.baraded.mf.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.messaging.RemoteMessage.Builder;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.adapter.PersonInfo;
import me.lazerka.mf.android.contacts.FriendsManager;
import me.lazerka.mf.android.di.Injector;
import me.lazerka.mf.api.gcm.GcmPayload;
import me.lazerka.mf.api.object.LocationRequest2;
import me.lazerka.mf.api.object.LocationResponse;
import me.lazerka.mf.api.object.UserFindId;
import org.joda.time.Duration;

import javax.inject.Inject;

/**
 * Handles location-related things.
 *
 * @author Dzmitry Lazerka
 */
public class LocationService {
	private static final Logger log = LogService.getLogger(LocationService.class);

	public static final int TRACKING_NOTIFICATION_ID = 4321;
	public static final int FORBIDDEN_NOTIFICATION_ID = 4322;

	//private final LocationRequestHandler locationRequestHandler;

	private final BehaviorSubject<FriendLocationResponse> locationUpdates = BehaviorSubject.create();
	private final FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();

	@Inject
	FriendsManager friendsManager;

	public LocationService() {

		Injector.applicationComponent().inject(this);

		//this.locationRequestHandler = new LocationRequestHandler(context);
	}

	/**
	 * @return "769083712074"
	 */
	//String getDefaultSenderId() {
		//return context.getString(R.string.gcm_defaultSenderId);
	//}

	public Observable<FriendLocationResponse> getLocationUpdates() {
		return locationUpdates;
	}

	//public void requestLocationUpdates(
	//		GoogleSignInAccount account,
	//		PersonInfo to,
	//		Duration duration,
	//		Callback locationRequestCallback
	//) throws NoEmailsException
	//{
	//	LocationRequest2 locationRequest = buildLocationRequest(to, duration);
	//
	//	Call call = new ApiPost(locationRequest).newCall(account);
	//	call.enqueue(locationRequestCallback);
	//
	//	String topic = locationRequest.getUpdatesTopic();
	//	firebaseMessaging.subscribeToTopic(topic);
	//
	//	// DEBUG
	//	//log.info("Subscribing to {}", topic);
	//
	//	saveToSharedPreferences(topic);
	//}

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

	//public void handleRequest(
	//		LocationRequestFromServer locationRequest,
	//		String gcmMessageFrom,
	//		DateTime gcmSentAt
	//) {
	//	if (!gcmMessageFrom.equals(getDefaultSenderId())) {
	//		String msg = "GCM message from unknown sender rejected: " + gcmMessageFrom;
	//		log.error(msg);
	//		return;
	//	}
	//
	//	String requesterEmail = locationRequest.getRequesterEmail();
	//	log.info("Received location request from " + requesterEmail);
	//
	//	// Authorize request.
	//	PersonInfo friend = authorizeRequest(requesterEmail);
	//
	//	// TODO: if no such user in friends, we may still want to show a confirmation popup to user,
	//	// so they could authorize a verified email even if it's not friended yet.
	//
	//	if (friend != null) {
	//		locationRequestHandler.processAuthorizedRequest(locationRequest, friend);
	//	} else {
	//
	//		LocationResponse locationResponse = LocationResponse.denied();
	//
	//		sendLocationUpdate(locationResponse, locationRequest.getUpdatesTopic());
	//
	//		// TODO add setting "Ignore requests from non-friends to prevent spamming".
	//		showForbiddenNotification(requesterEmail);
	//	}
	//}

	//private PersonInfo authorizeRequest(String requesterEmail) {
	//	List<PersonInfo> friends;
	//
	//	try {
	//		Future<List<PersonInfo>> future = friendsManager.getFriends();
	//
	//		friends = future.get();
	//	} catch (InterruptedException | ExecutionException e) {
	//		log.error(e);
	//		return null;
	//	}
	//
	//	for(PersonInfo friend : friends) {
	//		if (friend.emails.contains(requesterEmail)) {
	//			return friend;
	//		}
	//
	//		for(String friendEmail : friend.emails) {
	//			EmailNormalized normalized = EmailNormalized.normalizeEmail(friendEmail);
	//			if (normalized.getEmail().equals(requesterEmail)) {
	//				return friend;
	//			}
	//		}
	//	}
	//
	//	log.warn("Requester not in friends list, rejecting " + requesterEmail);
	//
	//	return null;
	//}
	//
	//private void showForbiddenNotification(String requesterEmail) {
	//	String message = context.getString(R.string.requester_not_in_friends, requesterEmail);
	//	Notification notification = getNotificationBuilder(message)
	//			.setAutoCancel(true)
	//			.build();
	//
	//	NotificationManager notificationManager =
	//			(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	//	notificationManager.notify(requesterEmail, FORBIDDEN_NOTIFICATION_ID, notification);
	//}

	//private NotificationCompat.Builder getNotificationBuilder(String message) {
	//	Intent intent = new Intent(context, MainActivity.class);
	//	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	//	PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
	//
	//	Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	//	return new NotificationCompat.Builder(context)
	//			.setSmallIcon(R.mipmap.ic_launcher)
	//			.setContentTitle(context.getString(R.string.app_name))
	//			.setStyle(new BigTextStyle().bigText(message))
	//			.setSound(defaultSoundUri)
	//			.setContentText(message)
	//			.setContentIntent(pendingIntent);
	//}

	public void sendLocationUpdate(LocationResponse locationResponse, String topic) {
		String json;
		try {
			json = Application.getJsonMapper().writeValueAsString(locationResponse);
		} catch (JsonProcessingException e) {
			log.error(e);
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
		//log.info("Sending location update to {}", topic);
		log.info("Sending location update", topic);
		firebaseMessaging.send(gcmMessage);
	}

	/**
	 * @param topicFrom Like "/topics/61b166a188d-1715ia0e28ef087ba578"
	 */
	public void handleLocationResponse(LocationResponse response, String topicFrom) {

		if (response.isComplete()) {
			log.info("Finished location session {}, unsubscribing.", topicFrom);
			firebaseMessaging.unsubscribeFromTopic(topicFrom);
		}

		if (!response.isSuccessful()) {
			log.warn("LocationResponse unsuccessful: {}, unsubscribing from {}.", response.getError(), topicFrom);
			firebaseMessaging.unsubscribeFromTopic(topicFrom);
		}

		if (response.getLocation() != null) {
			TopicName topicName = TopicName.parse(topicFrom);

			PersonInfo senderContact = friendsManager.getFriend(topicName.getFriendLookupKey());

			FriendLocationResponse friendLocationResponse = new FriendLocationResponse(senderContact, response);
			locationUpdates.onNext(friendLocationResponse);
		}
	}

	public static class NoEmailsException extends Exception {
	}
}
