package me.lazerka.mf.android.background;

import android.content.Context;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.widget.Toast;
import com.android.volley.Request.Method;
import com.android.volley.VolleyError;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.FriendInfo;
import me.lazerka.mf.android.adapter.FriendsLoader;
import me.lazerka.mf.android.http.JsonRequester;
import me.lazerka.mf.api.gcm.LocationRequestGcmPayload;
import me.lazerka.mf.api.object.Location;
import me.lazerka.mf.api.object.MyLocation;
import me.lazerka.mf.api.object.MyLocationResponse;
import org.acra.ACRA;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static org.joda.time.DateTimeZone.UTC;

/**
 * @author Dzmitry Lazerka
 */
public class LocationRequestHandler extends GcmMessageHandler<LocationRequestGcmPayload> {
	private static final Logger logger = LoggerFactory.getLogger(LocationRequestHandler.class);

	/**
	 * If passing this as usual, then service cannot get GCed.
	 */
	private final WeakReference<GcmIntentService> serviceRef;

	public LocationRequestHandler(GcmIntentService service) {
		this.serviceRef = new WeakReference<>(service);
	}

	@Override
	public void handleMessage(Message message) {
		GcmIntentService service = serviceRef.get();
		if (service == null) {
			// Probably happens when device is turning off.
			String msg = "Service is GCed, not sending my location";
			logger.info(msg);
			ACRA.getErrorReporter().handleException(new RuntimeException(logger.getName() + ": " + msg));
			return;
		}

		LocationRequestGcmPayload payload;
		try {
			payload = parseGcmPayload(message, LocationRequestGcmPayload.class, service);
		} catch (IOException e) {
			// Nothing, already reported
			return;
		}

		String requesterEmail = payload.getRequesterEmail();

		// Not ideally clean to use FriendsLoader this way, but clean enough for now.
		FriendInfo friend = getFriendInfo(requesterEmail, service);
		if (friend == null) {
			logger.warn("Requester not in friends list, rejecting " + requesterEmail);

			String appName = service.getResources().getString(R.string.app_name);
			sendNotification(
					requesterEmail + " requested your location, but not in friends list, rejected",
					appName,
					service);

			return;
		}

		String appName = service.getResources().getString(R.string.app_name);
		sendNotification(requesterEmail + " requested your location", appName, service);

		LocationManager locationManager = (LocationManager) service.getSystemService(Context.LOCATION_SERVICE);
		try {
			android.location.Location lastKnownLocation =
					locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (lastKnownLocation != null) {
				Location location = new Location(
						DateTime.now(UTC),
						Application.preferences.getAccount().name,
						lastKnownLocation.getLatitude(),
						lastKnownLocation.getLongitude(),
						lastKnownLocation.getAccuracy()
				);

				String requestId = String.valueOf(SystemClock.uptimeMillis());
				MyLocation myLocation = new MyLocation(requestId, location, requesterEmail);

				new LocationSender(myLocation, service).send();

				int howLongMs = 300000; // TODO: make user-configurable
				scheduleSendingLocation(service, requesterEmail, howLongMs);
			} else {
				// TODO implement tracking
				Toast.makeText(service, "No lastKnownLocation", Toast.LENGTH_LONG)
						.show();
				logger.error("No lastKnownLocation");
			}
		} catch (SecurityException e) {
			logger.error("SecurityException on getLastKnownLocation()", e);
		}
	}

	@Nullable
	private FriendInfo getFriendInfo(String requesterEmail, GcmIntentService service) {
		FriendsLoader friendsLoader = new FriendsLoader(service);
		ArrayList<FriendInfo> friendInfos = friendsLoader.loadInBackground();

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

	private void scheduleSendingLocation(Context context, String requesterEmail, int howLongMs) {
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		LocationListener listener = new LocationListener(context, requesterEmail, howLongMs);
		try {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, listener);
		} catch (SecurityException e) {
			logger.error("SecurityException on requestLocationUpdates()", e);
		}
	}

	private static class LocationListener implements android.location.LocationListener {
		private final WeakReference<Context> contextRef;
		private final String myEmail;
		private final String requesterEmail;
		private final long stopAtMs;

		LocationListener(Context context, String requesterEmail, int howLongMs) {
			this.contextRef = new WeakReference<>(context);
			this.requesterEmail = requesterEmail;
			this.myEmail = Application.preferences.getAccount().name;
			this.stopAtMs = SystemClock.uptimeMillis() + howLongMs;
		}

		@Override
		public void onLocationChanged(android.location.Location loc) {
			Context context = contextRef.get();
			if (context == null) {
				logger.info("contextRef is GCed, not sending location");
				return;
			}

			Location location = new Location(
					DateTime.now(UTC),
					myEmail,
					loc.getLatitude(),
					loc.getLongitude(),
					loc.getAccuracy()
			);

			String requestId = String.valueOf(SystemClock.uptimeMillis());
			MyLocation myLocation = new MyLocation(requestId, location, requesterEmail);

			new LocationSender(myLocation, context).send();

			if (SystemClock.uptimeMillis() > stopAtMs) {
				LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
				try {
					locationManager.removeUpdates(this);
					logger.info("Stopped sending my location");
				} catch (SecurityException e) {
					logger.error("SecurityException on removeUpdates()", e);
				}

			}
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			logger.info(provider + " changed status to " + status);
		}

		@Override
		public void onProviderEnabled(String provider) {
			logger.info(provider + " enabled");
		}

		@Override
		public void onProviderDisabled(String provider) {
			logger.info(provider + " disabled");
		}

	}

	private static class LocationSender extends JsonRequester<MyLocation, MyLocationResponse> {
		public LocationSender(MyLocation request, Context context) {
			super(Method.POST, MyLocation.PATH, request, MyLocationResponse.class, context);
		}

		@Override
		public void onResponse(MyLocationResponse response) {
			logger.info("onResponse: " + response.toString());
		}

		@Override
		public void onErrorResponse(VolleyError error) {
			super.onErrorResponse(error);
		}
	}
}
