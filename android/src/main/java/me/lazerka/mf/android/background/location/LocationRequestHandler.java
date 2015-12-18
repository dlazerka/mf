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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

import org.acra.ACRA;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.activity.MainActivity;
import me.lazerka.mf.android.adapter.FriendInfo;
import me.lazerka.mf.android.adapter.FriendsLoader;
import me.lazerka.mf.android.auth.SignInManager;
import me.lazerka.mf.api.object.LocationRequest;

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
	private static final int NOTIFICATION_ID = 4321;
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
		FriendInfo authorizedFriend = authorizeRequestFrom(requesterEmail);
		if (authorizedFriend != null) {
			processAuthorizedRequest(gcmRequest, authorizedFriend);
		}
	}

	private void processAuthorizedRequest(LocationRequest gcmRequest, FriendInfo authorizedFriend) {

		// TODO show confirmation dialog
		showNotification(R.string.asked_your_location, authorizedFriend.displayName);

		Intent intent = getLocationListenerIntent(gcmRequest);

		// Unique per requester user. So FLAG_UPDATE_CURRENT updates only requests by this user.
		int requestCode = (int) authorizedFriend.id;
		PendingIntent listener = PendingIntent.getService(context, requestCode, intent, FLAG_UPDATE_CURRENT);

		// Last location may be to coarse, and users get disappointed.
		// sendLastKnownLocation(gcmRequest, apiClient);

		Duration duration = gcmRequest.getDuration() != null
		                    ? gcmRequest.getDuration()
		                    : Duration.standardMinutes(5);

		com.google.android.gms.location.LocationRequest locationRequest =
				new com.google.android.gms.location.LocationRequest()
						.setPriority(PRIORITY_HIGH_ACCURACY)
						.setInterval(TRACKING_INTERVAL.getMillis())
								//.setMaxWaitTime(3000)
						.setSmallestDisplacement(0)
						.setFastestInterval(TRACKING_INTERVAL_FASTEST.getMillis())
						.setExpirationDuration(duration.getMillis());

		// Buggy for now: https://code.google.com/p/android/issues/detail?id=197296
		//scheduleFusedLocationApi(listener, locationRequest);
		scheduleLocationManager(locationRequest, listener, requestCode);
	}

	/**
	 * Schedules location updates using old LocationManager.
	 * Doesn't require GoogleApiClient, so doesn't depend on Play Services.
	 * Cannot specify duration, so have to manually call removeUpdates().
	 */
	private void scheduleLocationManager(
			com.google.android.gms.location.LocationRequest locationRequest,
			PendingIntent updateListener,
			int requestCode) {
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		try {

			// Schedule removeUpdates() even before we request them.
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent stopIntent = LocationStopListener.makeIntent(context, updateListener);
			PendingIntent pendingStopIntent =
					PendingIntent.getBroadcast(context, requestCode, stopIntent, FLAG_UPDATE_CURRENT);
			long stopAt = System.currentTimeMillis() + locationRequest.getExpirationTime();
			alarmManager.set(AlarmManager.RTC, stopAt, pendingStopIntent);
			logger.info("Scheduled updates to stop at " + stopAt);

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
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}

	/**
	 * Schedules location updates using recommended FusedLocationApi.
	 */
	private void scheduleFusedLocationApi(
			PendingIntent listener,
			com.google.android.gms.location.LocationRequest locationRequest
	) {
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

			// TODO check new Android 6.0 permissions
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
				ACRA.getErrorReporter().handleSilentException(new Exception(msg));

				// We could also send response back to server and requester.
			}
		} catch (Exception e) {
			logger.error("Unable to schedule location updates", e);
			ACRA.getErrorReporter().handleException(e);
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
		android.location.Location lastLocation = FusedLocationApi.getLastLocation(apiClient);
		if (lastLocation != null) {
			Intent intent = getLocationListenerIntent(gcmRequest);
			intent.putExtra("com.google.android.gms.location.EXTRA_LOCATION_RESULT", lastLocation);
			context.startService(intent);
		} else {
			logger.info("No lastLocation");
		}
	}

	private void showNotification(int resId, String... params) {
		String message = context.getString(resId, params);

		NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		Intent intent = new Intent(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

		Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Notification notification =
				new Builder(context)
						.setSmallIcon(R.mipmap.ic_launcher)
						.setContentTitle(context.getString(R.string.app_name))
						.setStyle(new BigTextStyle().bigText(message))
						.setAutoCancel(true)
						.setSound(defaultSoundUri)
						.setContentText(message)
						.setContentIntent(pendingIntent)
						.build();

		notificationManager.notify(NOTIFICATION_ID, notification);
	}

	/**
	 * @return friendInfo if authorized, or null otherwise.
	 */
	@Nullable
	private FriendInfo authorizeRequestFrom(@Nullable String requesterEmail) {
		FriendsLoader friendsLoader = new FriendsLoader(context);
		List<FriendInfo> friendInfos = friendsLoader.loadInBackground();

		FriendInfo friend = null;
		for(FriendInfo friendInfo : friendInfos) {
			// TODO check normalized
			if (friendInfo.emails.contains(requesterEmail)) {
				friend = friendInfo;
				break;
			}
		}

		if (friend == null) {
			logger.warn("Requester not in friends list, rejecting " + requesterEmail);

			// TODO add setting "Ignore unknown to prevent spamming".
			showNotification(R.string.requester_not_in_friends, requesterEmail);
		}

		return friend;
	}
}
