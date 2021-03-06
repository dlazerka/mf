/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2015 Dzmitry Lazerka dlazerka@gmail.com
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

package me.lazerka.mf.android.background.location;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.firebase.crash.FirebaseCrash;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.activity.MainActivity;
import me.lazerka.mf.android.adapter.FriendsLoader;
import me.lazerka.mf.android.adapter.PersonInfo;
import me.lazerka.mf.android.auth.SignInManager;
import me.lazerka.mf.api.object.LocationRequest;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;
import static com.google.android.gms.location.LocationServices.FusedLocationApi;

/**
 * Ever-running background service that handles requests for our location.
 * <p/>
 * This handler can run in background, heed the wake lock.
 * <p/>
 * Keeps GoogleApiClient connected for the duration of the whole tracking session.
 * <p/>
 * TODO: show persistent notification for as long as we're tracking.
 *
 * @author Dzmitry Lazerka
 */
public class LocationRequestHandler {
	private static final Logger logger = LoggerFactory.getLogger(LocationRequestHandler.class);

	/**
	 * We want to merge notifications/updates etc per requester.
	 * For this, we need a unique integer. We probably could just use {@link PersonInfo#id}.intValue(),
	 * but that's prone to collisions, so let's keep it safe.
	 */
	private static final ConcurrentHashMap<String, Integer> requesterCodes = new ConcurrentHashMap<>();
	private static final AtomicInteger nextCode = new AtomicInteger(1);

	public static final int TRACKING_NOTIFICATION_ID = 4321;
	public static final int FORBIDDEN_NOTIFICATION_ID = 4322;

	private static final Duration TRACKING_INTERVAL = Duration.standardSeconds(3);
	private static final Duration TRACKING_INTERVAL_FASTEST = Duration.standardSeconds(1);

	private final Context context;

	public LocationRequestHandler(Context context) {
		this.context = context;
	}

	public void handle(LocationRequest gcmRequest) {
		String requesterEmail = gcmRequest.getRequesterEmail();
		logger.info("Received location request from " + requesterEmail);

		// Authorize request.
		PersonInfo authorizedFriend = authorizeRequestFrom(requesterEmail);

		if (authorizedFriend != null) {
			processAuthorizedRequest(gcmRequest, authorizedFriend);
		}
	}

	private void processAuthorizedRequest(LocationRequest gcmRequest, PersonInfo requester) {
		requesterCodes.putIfAbsent(requester.lookupKey, nextCode.getAndIncrement());
		int requesterCode = requesterCodes.get(requester.lookupKey);
		int notificationId = TRACKING_NOTIFICATION_ID;
		String notificationTag = requester.lookupKey;

		// The main thing -- location updates.
		Intent intent = getLocationListenerIntent(gcmRequest);
		PendingIntent updateListener =
				PendingIntent.getService(context, requesterCode, intent, FLAG_UPDATE_CURRENT);

		// To stop location updates.
		Intent stopIntent = LocationStopListener.makeIntent(
				context, intent, requesterCode, notificationId, notificationTag);
		PendingIntent stopListener =
				PendingIntent.getService(context, requesterCode, stopIntent, FLAG_UPDATE_CURRENT);

		// Last location may be to coarse, and users get disappointed.
		// sendLastKnownLocation(gcmRequest, apiClient);

		Duration duration = gcmRequest.getDuration() != null
		                    ? gcmRequest.getDuration()
		                    : Duration.standardMinutes(5);

		// To notify user they are being tracked.
		showTrackingNotification(requester, stopListener, notificationId, notificationTag, duration);

		com.google.android.gms.location.LocationRequest locationRequest =
				new com.google.android.gms.location.LocationRequest()
						.setPriority(PRIORITY_HIGH_ACCURACY)
						.setInterval(TRACKING_INTERVAL.getMillis())
						//.setMaxWaitTime(3000)
						.setSmallestDisplacement(0)
						.setFastestInterval(TRACKING_INTERVAL_FASTEST.getMillis())
						.setExpirationDuration(duration.getMillis());

		// Buggy for now: https://code.google.com/p/android/issues/detail?id=197296
		//scheduleFusedLocationApi(updateListener, locationRequest);
		scheduleLocationManager(locationRequest, updateListener, stopListener);
	}

	/**
	 * Schedules location updates using old LocationManager.
	 * Doesn't require GoogleApiClient, so doesn't depend on Play Services.
	 * Cannot specify duration, so have to manually call removeUpdates().
	 */
	private void scheduleLocationManager(
			com.google.android.gms.location.LocationRequest locationRequest,
			PendingIntent updateListener,
			PendingIntent stopListener)
	{
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		try {

			// Schedule removeUpdates() even before we request them.
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			long stopAtUptime = locationRequest.getExpirationTime();
			alarmManager.set(AlarmManager.ELAPSED_REALTIME, stopAtUptime, stopListener);
			logger.info("Scheduled updates to stop at {}ms uptime, now: {}",
					stopAtUptime, SystemClock.elapsedRealtime());

			// Too bad it's @SystemApi.
			// locationManager.requestLocationUpdates(locationRequest, updateListener);

			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER,
					locationRequest.getInterval(),
					locationRequest.getSmallestDisplacement(),
					updateListener);
			logger.info("Scheduled updates using LocationManager");
		} catch (SecurityException e) {
			logger.error("No location permissions", e);
			FirebaseCrash.report(e);
		}
	}

	/**
	 * Schedules location updates using recommended FusedLocationApi.
	 */
	 @SuppressLint("MissingPermission")
	private void scheduleFusedLocationApi(
			PendingIntent listener,
			com.google.android.gms.location.LocationRequest locationRequest
	)
	{
		SignInManager authenticator = new SignInManager();
		GoogleApiClient apiClient = authenticator.buildClient(context);

		ConnectionResult connectionResult = apiClient.blockingConnect();
		if (!connectionResult.isSuccess()) {
			logger.warn(
					"GoogleApiClient not connected: {}, {}",
					connectionResult.getErrorCode(),
					connectionResult.getErrorMessage());
			// TODO retry
			return;
		}

		try {

			if (!Application.hasLocationPermission()) {
				FirebaseCrash.logcat(Log.WARN, logger.getName(), "No location permission");
				return;
			}
			//noinspection MissingPermission
			PendingResult<Status> pendingResult = FusedLocationApi.requestLocationUpdates(
					apiClient,
					locationRequest,
					listener);

			Status status = pendingResult.await();
			if (status.isSuccess()) {
				logger.info("Successfully requested location updates.");
			} else {
				String msg = "requestLocationUpdates unsuccessful: "
						+ status.getStatusCode() + " " + status.getStatusMessage();
				logger.warn(msg);
				FirebaseCrash.report(new Exception(msg));

				// We could also send response back to server and requester.
			}
		} catch (Exception e) {
			logger.error("Unable to schedule location updates", e);
			FirebaseCrash.report(e);
		} finally {
			apiClient.disconnect();
		}
	}

	@NonNull
	private Intent getLocationListenerIntent(LocationRequest gcmRequest) {
		// We already received and parsed it so
		try {
			byte[] bytes = Application.jsonMapper.writeValueAsBytes(gcmRequest);
			Intent intent = new Intent(context, LocationUpdateListener.class);
			intent.putExtra(LocationUpdateListener.EXTRA_GCM_REQUEST, bytes);
			return intent;
		} catch (JsonProcessingException e) {
			// Highly unlikely, we already parsed it.
			throw new RuntimeException(e);
		}
	}

	private void sendLastKnownLocation(
			LocationRequest gcmRequest,
			GoogleApiClient apiClient
	) throws JsonProcessingException {
		if (!Application.hasLocationPermission()) {
			FirebaseCrash.logcat(Log.WARN, logger.getName(), "No location permission");
			return;
		}
		//noinspection MissingPermission
		android.location.Location lastLocation = FusedLocationApi.getLastLocation(apiClient);
		if (lastLocation != null) {
			Intent intent = getLocationListenerIntent(gcmRequest);
			intent.putExtra("com.google.android.gms.location.EXTRA_LOCATION_RESULT", lastLocation);
			context.startService(intent);
		} else {
			logger.info("No lastLocation");
		}
	}

	/**
	 * @return friendInfo if authorized, or null otherwise.
	 */
	@Nullable
	private PersonInfo authorizeRequestFrom(@Nullable String requesterEmail) {
		FriendsLoader friendsLoader = new FriendsLoader(context);
		List<PersonInfo> personInfos = friendsLoader.loadInBackground();

		PersonInfo friend = null;
		for(PersonInfo personInfo : personInfos) {
			// TODO check normalized
			if (personInfo.emails.contains(requesterEmail)) {
				friend = personInfo;
				break;
			}
		}

		if (friend == null) {
			logger.warn("Requester not in friends list, rejecting " + requesterEmail);

			// TODO add setting "Ignore unknown to prevent spamming".
			showForbiddenNotification(requesterEmail);
		}

		return friend;
	}

	private void showTrackingNotification(
			PersonInfo person,
			PendingIntent stopListener,
			int notificationId,
			String notificationTag,
			Duration duration) {

		int minutes = (int) duration.getStandardMinutes();

		String message = context.getString(R.string.tracking_you, person.displayName, minutes);
		Notification notification = getNotificationBuilder(message)
				.addPerson(person.lookupKey)
				.addAction(R.drawable.close, context.getString(R.string.stop), stopListener)
				.setOngoing(true)
				.build();

		NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(notificationTag, notificationId, notification);
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

	private Builder getNotificationBuilder(String message) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

		Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		return new Builder(context)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(context.getString(R.string.app_name))
				.setStyle(new BigTextStyle().bigText(message))
				.setSound(defaultSoundUri)
				.setContentText(message)
				.setContentIntent(pendingIntent);
	}
}
