/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2016 Dzmitry Lazerka dlazerka@gmail.com
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.activity.MainActivity;
import me.lazerka.mf.android.adapter.PersonInfo;
import me.lazerka.mf.android.background.location.LocationRequestHandler;
import me.lazerka.mf.api.gcm.GcmPayload;
import me.lazerka.mf.api.object.Location;
import me.lazerka.mf.api.object.LocationRequest2;
import me.lazerka.mf.api.object.LocationResponse;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private final SecureRandom rng = new SecureRandom();

	private final Context context;
	private final SharedPreferences preferences;
	private final LocationRequestHandler locationRequestHandler;

	public LocationService(Context context) {
		this.context = context;
		this.preferences = context.getSharedPreferences(context.getString(R.string.preferences_file_friends), MODE_PRIVATE);
		this.locationRequestHandler = new LocationRequestHandler(context);
	}

	/**
	 * @return "769083712074"
	 */
	String getDefaultSenderId() {
		return context.getString(R.string.gcm_defaultSenderId);
	}

	private static String getLocationRequestsTopic(@Nonnull String email) {
		String emailNormalized = normalizeEmail(email);
		return "location_request/" + emailNormalized;
	}

	public void requestLocationUpdates(
			GoogleSignInAccount account,
			PersonInfo to,
			Duration duration
	) {
		LocationRequest2 locationRequest = buildLocationRequest(account, duration);

		try {
			String json = Application.getJsonMapper().writeValueAsString(locationRequest);

			subscribeToTopic(locationRequest.getUpdatesTopic());

			sendRequest(to, json);

		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			FirebaseCrash.report(e);
		}
	}

	LocationRequest2 buildLocationRequest(
			GoogleSignInAccount account,
			Duration duration
	) {
		// 256 bits.
		String randomSecret =
				Integer.toHexString(rng.nextInt()) +
						Integer.toHexString(rng.nextInt()) +
						Integer.toHexString(rng.nextInt()) +
						Integer.toHexString(rng.nextInt());

		String updatesTopic = account.getEmail() + "/" + randomSecret;

		return new LocationRequest2(
				account.getIdToken(),
				updatesTopic,
				duration);
	}

	/**
	 * Sends requests to all emails we have for user, hoping that at least some of them are listened.
	 */
	void sendRequest(PersonInfo to, String json) {
		FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();

		for(String email : to.emails) {
			ImmutableMap<String, String> data = ImmutableMap.of(
					GcmPayload.TYPE_FIELD, LocationRequest2.TYPE,
					GcmPayload.PAYLOAD_FIELD, json
			);
			RemoteMessage remoteMessage = new Builder(getLocationRequestsTopic(email))
				.setData(data)
				.build();

			firebaseMessaging.send(remoteMessage);
		}
	}

	/** Anything containing @. */
	static final Pattern emailAddressSplitPattern = Pattern.compile("^(.*)(@.*)$");
	/** Just a single period. */
	static final Pattern periodRegex = Pattern.compile(".", Pattern.LITERAL);

	/**
	 * Try to normalize email addresses by lowercasing domain part.
	 *
	 * If we detect address is GMail one, we also apply GMail specific features normalizer.
	 *
	 * If we cannot parse email, we log a warning and return non-normalized email instead of throwing an exception,
	 * because email addresses could be very tricky to parse, and there's no silver bullet despite RFCs.
	 */
	static String normalizeEmail(String address){
		Matcher matcher = emailAddressSplitPattern.matcher(address);
		if (!matcher.matches()) {
			FirebaseCrash.logcat(Log.WARN, logger.getName(), "Email address invalid: {}" + address);
			return address;
		}

		String localPart = matcher.group(1);
		String domainPart = matcher.group(2);

		domainPart = domainPart.toLowerCase(Locale.US);

		if (domainPart.equals("@gmail.com") || domainPart.equals("@googlemail.com")) {
			// Remove everything after plus sign (GMail-specific feature).
			int plusIndex = localPart.indexOf('+');
			if (plusIndex != -1) {
				localPart = localPart.substring(0, plusIndex);
			}

			// Remove periods.
			localPart = periodRegex.matcher(localPart).replaceAll("");

			// GMail addresses are case-insensitive.
			localPart = localPart.toLowerCase(Locale.US);
		}

		return localPart + domainPart;
	}

	boolean subscribeToTopic(String topic) {
		FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
		firebaseMessaging.subscribeToTopic(topic);

		synchronized (this) {
			// Clone, otherwise value won't be set.
			Set<String> topicsSubscribed =
					new LinkedHashSet<>(preferences.getStringSet(TOPICS_SUBSCRIBED, new HashSet<>(1)));

			boolean changed = topicsSubscribed.add(topic);
			if (!changed) {
				FirebaseCrash.logcat(Log.WARN, logger.getName(), "Already subscribed to topic");
				return false;
			}
			preferences.edit()
			           .putStringSet(TOPICS_SUBSCRIBED, topicsSubscribed)
			           .apply();
		}

		return true;
	}

	public void handleRequest(LocationRequest2 locationRequest, String gcmMessageFrom, DateTime gcmSentAt) {
		if (!gcmMessageFrom.equals(getDefaultSenderId())) {
			String msg = "GCM message from unknown sender rejected: " + gcmMessageFrom;
			logger.error(msg);
			FirebaseCrash.report(new IllegalArgumentException(msg));
			return;
		}

		String requesterEmail = authenticateRequester(locationRequest.getGoogleAuthToken());
		logger.info("Received location request from " + requesterEmail);

		// Authorize request.
		PersonInfo friend = authorizeRequest(requesterEmail);
		if (friend != null) {
			locationRequestHandler.processAuthorizedRequest(locationRequest, friend);
		} else {
			// TODO add setting "Ignore unknown to prevent spamming".
			showForbiddenNotification(requesterEmail);
		}
	}

	private String authenticateRequester(String requesterEmail) {


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

			for (String friendEmail: friend.emails) {
				if (normalizeEmail(friendEmail).equals(requesterEmail)) {
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

	public void sendLocationUpdate(Location location, LocationRequest2 request) {
		LocationResponse locationResponse = new LocationResponse(location, request.getDuration());

	}
}
