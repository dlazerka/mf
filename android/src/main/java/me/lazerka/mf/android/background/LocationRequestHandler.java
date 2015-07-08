package me.lazerka.mf.android.background;

import android.content.Context;
import android.location.LocationManager;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static org.joda.time.DateTimeZone.UTC;

/**
 * @author Dzmitry Lazerka
 */
public class LocationRequestHandler extends GcmMessageHandler<LocationRequestGcmPayload> {
	private final String TAG = getClass().getName();

	/**
	 * If passing this as usual, then service cannot get GCed.
	 */
	private final WeakReference<GcmIntentService> serviceRef;

	public LocationRequestHandler(GcmIntentService service) {
		this.serviceRef = new WeakReference<>(service);
	}

	@Override
	public void handleMessage(Message message) {
		Log.v(TAG, "handleMessage " + message);

		GcmIntentService service = serviceRef.get();
		if (service == null) {
			// Probably happens when device is turning off.
			String msg = "Service is GCed, not sending my location";
			Log.e(TAG, msg);
			ACRA.getErrorReporter().handleException(new RuntimeException(TAG + ": " + msg));
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
			Log.w(TAG, "Requester not in friends list, rejecting " + requesterEmail);

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
		} else {
			// TODO implement tracking
			Toast.makeText(service, "No lastKnownLocation", Toast.LENGTH_LONG)
				.show();
			Log.e(TAG, "No lastKnownLocation");
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

	private class LocationSender extends JsonRequester<MyLocation, MyLocationResponse> {
		public LocationSender(MyLocation request, Context context) {
			super(Method.POST, MyLocation.PATH, request, MyLocationResponse.class, context);
		}

		@Override
		public void onResponse(MyLocationResponse response) {
			Log.d(TAG, "onResponse: " + response.toString());
		}

		@Override
		public void onErrorResponse(VolleyError error) {
			super.onErrorResponse(error);
		}
	}
}
