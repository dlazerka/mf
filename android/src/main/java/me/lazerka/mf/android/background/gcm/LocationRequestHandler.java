package me.lazerka.mf.android.background.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.activity.MainActivity;
import me.lazerka.mf.android.adapter.FriendInfo;
import me.lazerka.mf.android.adapter.FriendsLoader;
import me.lazerka.mf.android.auth.SignInManager;
import me.lazerka.mf.api.object.LocationRequest;
import org.acra.ACRA;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;
import static com.google.android.gms.location.LocationServices.FusedLocationApi;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Ever-running background service that handles requests for our location.
 *
 * This handler can run in background, heed the wake lock.
 *
 * Keeps GoogleApiClient connected for the duration of the whole tracking session.
 *
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
		checkNotNull(authorizedFriend);

		// TODO show confirmation dialog
		String requesterEmail = checkNotNull(gcmRequest.getRequesterEmail());
		showNotification(R.string.asked_your_location, authorizedFriend.displayName);

		SignInManager authenticator = new SignInManager();
		GoogleApiClient apiClient = authenticator.newClient(context);

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
			// Last location may be to coarse, and users get disappointed.
			// sendLastKnownLocation(gcmRequest, apiClient);

			Duration duration = gcmRequest.getDuration() != null
			                    ? gcmRequest.getDuration()
			                    : Duration.standardMinutes(5);

			com.google.android.gms.location.LocationRequest locationRequest =
					new com.google.android.gms.location.LocationRequest()
							.setPriority(PRIORITY_HIGH_ACCURACY)
							.setInterval(TRACKING_INTERVAL.getMillis())
							.setMaxWaitTime(3000)
							.setSmallestDisplacement(0.1f)
							.setFastestInterval(TRACKING_INTERVAL_FASTEST.getMillis())
							.setExpirationDuration(duration.getMillis());

			// TODO check new Android 6.0 permissions
			Intent intent = getLocationListenerIntent(gcmRequest);
			PendingIntent pendingIntent = PendingIntent.getService(context, requesterEmail.hashCode(), intent, 0);
			PendingResult<Status> pendingResult = FusedLocationApi.requestLocationUpdates(
					apiClient,
					locationRequest,
					pendingIntent);

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
		} catch (Exception e){
			logger.error("Unable to schedule location updates", e);
			ACRA.getErrorReporter().handleException(e);
		} finally {
			apiClient.disconnect();
		}
	}

	@NonNull
	private Intent getLocationListenerIntent(LocationRequest gcmRequest) throws JsonProcessingException {
		// We already received and parsed it so
		byte[] bytes = Application.jsonMapper.writeValueAsBytes(gcmRequest);
		Intent intent = new Intent(context, LocationUpdateListener.class);
		intent.putExtra(LocationUpdateListener.EXTRA_GCM_REQUEST, bytes);
		return intent;
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

	/** @return friendInfo if authorized, or null otherwise. */
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
