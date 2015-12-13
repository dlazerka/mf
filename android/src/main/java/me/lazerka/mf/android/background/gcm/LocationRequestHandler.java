package me.lazerka.mf.android.background.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.activity.MainActivity;
import me.lazerka.mf.android.adapter.FriendInfo;
import me.lazerka.mf.android.adapter.FriendsLoader;
import me.lazerka.mf.android.auth.AndroidAuthenticator;
import me.lazerka.mf.android.auth.GoogleApiException;
import me.lazerka.mf.android.background.ApiPost;
import me.lazerka.mf.api.object.Location;
import me.lazerka.mf.api.object.LocationRequest;
import me.lazerka.mf.api.object.MyLocation;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observer;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.List;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;
import static org.joda.time.DateTimeZone.UTC;

/**
 * Ever-running background service that handles requests for our location.
 *
 * Keeps GoogleApiClient connected for the duration of the whole tracking session.
 *
 * @author Dzmitry Lazerka
 */
public class LocationRequestHandler extends Service implements Observer<LocationRequest> {
	private static final Logger logger = LoggerFactory.getLogger(LocationRequestHandler.class);
	private static final int NOTIFICATION_ID = 4321;
	private static final Duration TRACKING_INTERVAL = Duration.standardSeconds(3);

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		// We don't bind.
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		GcmReceiveService.getLocationRequestObservable()
				.observeOn(Schedulers.newThread())
				.subscribe(this);

		// Keep in Started state.
		return START_STICKY;
	}

	@Override
	public void onNext(LocationRequest gcmRequest) {
		String requesterEmail = gcmRequest.getRequesterEmail();

		// Authorize request.
		FriendInfo authorizedFriend = authorizeRequestFrom(requesterEmail);
		if (authorizedFriend == null) {
			logger.warn("Requester not in friends list, rejecting " + requesterEmail);

			// TODO add setting "Ignore unknown to prevent spamming".
			sendNotification(requesterEmail + " requested your location, but not in friends list, rejected");

			return;
		} else {
			assert requesterEmail != null;
		}

		// TODO show confirmation dialog
		sendNotification(requesterEmail + " requested your location");

		AndroidAuthenticator authenticator = new AndroidAuthenticator(this);
		GoogleApiClient apiClient = authenticator.getGoogleApiClient().build();

		ConnectionResult connectionResult = apiClient.blockingConnect();
		if (!connectionResult.isSuccess()) {
			logger.warn(
					"GoogleApiClient not connected: {}, {}",
					connectionResult.getErrorCode(),
					connectionResult.getErrorMessage());
			// TODO
			return;
		}

		try {
			GoogleSignInAccount account;
			try {
				account = authenticator.blockingGetAccount(apiClient);
			} catch (GoogleApiException e) {
				logger.warn("Unable to contact GoogleApi: {} {}", e.getCode(), e.getMessage());
				// TODO implement reconnection logic
				return;
			}

			// Last location may be to coarse, and users get disappointed.
			// sendLastKnownLocation(gcmRequest, apiClient, account);

			Duration duration = gcmRequest.getDuration() != null
					? gcmRequest.getDuration()
					: Duration.standardMinutes(5);

			com.google.android.gms.location.LocationRequest locationRequest =
					new com.google.android.gms.location.LocationRequest()
					.setInterval(TRACKING_INTERVAL.getMillis())
					.setExpirationDuration(duration.getMillis());

			// TODO check new Android 6.0 permissions
			LocationSender callback = new LocationSender(gcmRequest, account);
			// TODO use another method with PengingIntent, so it would work even if system killed the app.
			PendingResult<Status> pendingResult = FusedLocationApi.requestLocationUpdates(
					apiClient,
					locationRequest,
					callback,
					null);
			pendingResult.await();
		} finally {
			apiClient.disconnect();
		}
	}

	private void sendLastKnownLocation(
			LocationRequest gcmRequest,
			GoogleApiClient apiClient,
			GoogleSignInAccount account
	) {
		android.location.Location lastLocation = FusedLocationApi.getLastLocation(apiClient);
		if (lastLocation != null) {
			new LocationSender(gcmRequest, account)
					.sendLocation(lastLocation);
		} else {
			logger.info("No lastLocation");
		}
	}

	private void sendNotification(String message) {
		NotificationManager notificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

		NotificationCompat.Builder builder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.mipmap.ic_launcher)
						.setContentTitle(getString(R.string.app_name))
						.setStyle(new NotificationCompat.BigTextStyle().bigText(message))
						.setContentText(message);

		builder.setContentIntent(pendingIntent);
		notificationManager.notify(NOTIFICATION_ID, builder.build());
	}

	/** @return friendInfo if authorized, or null otherwise. */
	@Nullable
	private FriendInfo authorizeRequestFrom(@Nullable String requesterEmail) {
		FriendsLoader friendsLoader = new FriendsLoader(this);
		List<FriendInfo> friendInfos = friendsLoader.loadInBackground();

		FriendInfo friend = null;
		for(FriendInfo friendInfo : friendInfos) {
			// TODO check normalized
			if (friendInfo.emails.contains(requesterEmail)) {
				friend = friendInfo;
				break;
			}
		}
		return friend;
	}

	@Override
	public void onCompleted() {
		logger.error("Should never be called");
	}

	@Override
	public void onError(Throwable e) {
		logger.error("onError: ", e);
	}

	private static class LocationSender extends LocationCallback {
		private final LocationRequest gcmRequest;
		private final GoogleSignInAccount account;

		private LocationSender(LocationRequest gcmRequest, GoogleSignInAccount account) {
			this.gcmRequest = gcmRequest;
			this.account = account;
		}

		@Override
		public void onLocationResult(LocationResult result) {
			android.location.Location location = result.getLastLocation();
			sendLocation(location);
		}

		void sendLocation(android.location.Location location) {
			Location locationBean = new Location(
					DateTime.now(UTC),
					account.getEmail(),
					location.getLatitude(),
					location.getLongitude(),
					location.getAccuracy()
			);

			MyLocation myLocation = new MyLocation(locationBean, gcmRequest);

			ApiPost post = new ApiPost(myLocation);

			post.newCall(account).enqueue(new MyCallback());
		}

		@Override
		public void onLocationAvailability(LocationAvailability locationAvailability) {
			logger.info(locationAvailability.toString());
		}

		private static class MyCallback implements Callback {
			@Override
			public void onFailure(Request request, IOException e) {
				logger.warn("IOException: {}", e.getMessage());
			}

			@Override
			public void onResponse(Response response) throws IOException {
				if (response.code() != 200) {
					logger.warn("Failed: {}, {}", response.code(), response.message());
				}
			}
		}
	}
}
