package me.lazerka.mf.android.background;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import com.android.volley.Request.Method;
import com.android.volley.VolleyError;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.activity.MainActivity;
import me.lazerka.mf.android.auth.GcmAuthenticator;
import me.lazerka.mf.android.http.JsonRequester;
import me.lazerka.mf.api.gcm.LocationRequestGcmPayload;
import me.lazerka.mf.api.object.Location;
import me.lazerka.mf.api.object.MyLocation;
import me.lazerka.mf.api.object.MyLocationResponse;

import javax.annotation.Nullable;

/**
 * Handles messages from GCM. Basically, there's only one message -- if someone requests our location.
 *
 * @author Dzmitry Lazerka
 */
public class GcmIntentService extends IntentService {
	protected final String TAG = getClass().getName();

	public static final int NOTIFICATION_ID = 1;
	private NotificationManager mNotificationManager;

	public GcmIntentService() {
		super("GcmIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// Old deprecated way of receiving GCM Registration ID, but on Sony Xperia Ultra that's the only way.
		if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
			handleRegistrationId(intent);
		}

		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		String messageType = gcm.getMessageType(intent);

		if (messageType != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that GCM
			 * will be extended in the future with new message types, just ignore
			 * any message types you're not interested in, or that you don't
			 * recognize.
			 */
			switch (messageType) {
				case GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR:
					sendNotification("Send error: " + extras.toString(), "GCM Notification");
					break;
				case GoogleCloudMessaging.MESSAGE_TYPE_DELETED:
					sendNotification("Deleted messages on server: " + extras.toString(), "GCM Notification");
					// If it's a regular GCM message, do some work.
					break;
				case GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE:
					processMessage(extras);
					break;
			}
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	private void processMessage(Bundle extras) {
		Log.i(TAG, "Received message: " + extras.toString());
		// Available fields:
		// "sentAt", "requesterEmail" -- see LocationRequestGcmPayload
		// "from" -- GAE Project Number
		// "android.support.content.wakelockid" = set by GcmBroadcastReceiver at startWakefulService().
		// "collapse_key" -- See http://developer.android.com/training/cloudsync/gcm.html#collapse

		String requesterEmail = extras.getString(LocationRequestGcmPayload.REQUESTER_EMAIL);

		String appName = getResources().getString(R.string.app_name);
		sendNotification(requesterEmail + " requested your location", appName);

		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		android.location.Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location location = new Location();
		location.setAcc(lastKnownLocation.getAccuracy());
		location.setLat(lastKnownLocation.getLatitude());
		location.setLon(lastKnownLocation.getLongitude());

		String requestId = String.valueOf(SystemClock.uptimeMillis());
		MyLocation myLocation = new MyLocation(requestId, location, requesterEmail);

		new LocationSender(myLocation).send();
	}

	private void handleRegistrationId(Intent intent) {
		Log.i(TAG, "Received com.google.android.c2dm.intent.REGISTRATION");
		String gcmToken = intent.getStringExtra("registration_id");
		if (gcmToken != null) {
			GcmAuthenticator.storeGcmRegistration(gcmToken);
		} else {
			Log.w(TAG, "null registration id");
		}
	}

	// Put the message into a notification and post it.
	// This is just one simple example of what you might choose to do with
	// a GCM message.
	private void sendNotification(String msg, String contentTitle) {
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.mipmap.ic_launcher)
						.setContentTitle(contentTitle)
						.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
						.setContentText(msg);

		mBuilder.setContentIntent(pendingIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}


	private class LocationSender extends JsonRequester<MyLocation, MyLocationResponse> {
		public LocationSender(@Nullable MyLocation request) {
			super(Method.POST, MyLocation.PATH, request, MyLocationResponse.class);
		}

		@Override
		public void onResponse(MyLocationResponse response) {
			Log.d(TAG, "onResponse: " + response.toString());
		}

		@Override
		public void onErrorResponse(VolleyError error) {
			super.onErrorResponse(error);

			String errorMessage = error.getMessage() != null ? (": " + error.getMessage()) : "";
			String msg = "Network error " + errorMessage;
			Log.e(TAG, msg);
			Toast.makeText(GcmIntentService.this, msg, Toast.LENGTH_LONG).show();
		}
	}
}
