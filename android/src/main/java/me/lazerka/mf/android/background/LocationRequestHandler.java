package me.lazerka.mf.android.background;

import android.content.Context;
import android.location.LocationManager;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import com.android.volley.Request.Method;
import com.android.volley.VolleyError;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.http.JsonRequester;
import me.lazerka.mf.api.gcm.LocationRequestGcmPayload;
import me.lazerka.mf.api.object.Location;
import me.lazerka.mf.api.object.MyLocation;
import me.lazerka.mf.api.object.MyLocationResponse;
import org.joda.time.DateTime;

import java.io.IOException;
import java.lang.ref.WeakReference;

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
			Log.e(TAG, "Service is GCed, not sending my location");
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
		}
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
