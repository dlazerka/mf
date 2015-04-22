package me.lazerka.mf.android.background;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.activity.MainActivity;
import me.lazerka.mf.android.auth.GcmAuthenticator;

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
					sendNotification("Send error: " + extras.toString());
					break;
				case GoogleCloudMessaging.MESSAGE_TYPE_DELETED:
					sendNotification("Deleted messages on server: " + extras.toString());
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
		// TODO
		Log.i(TAG, "Received message: " + extras.toString());
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
	private void sendNotification(String msg) {
		mNotificationManager = (NotificationManager)
				this.getSystemService(Context.NOTIFICATION_SERVICE);

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.mipmap.ic_launcher)
						.setContentTitle("GCM Notification")
						.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
						.setContentText(msg);

		mBuilder.setContentIntent(pendingIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

/*
	private class LocationReceiver extends ApiResponseHandler {
		@Override
		protected void handleSuccess(@Nullable String json) {
			Log.v(TAG, "handleSuccess");

			Location location;
			try {
				ObjectMapper mapper = Application.jsonMapper;
				location = mapper.readValue(json, Location.class);
			} catch (IOException e) {
				Log.w(TAG, e.getMessage(), e);
				return;
			}

			MapFragment mapFragment = mTabsAdapter.getMapFragment();
			mapFragment.showLocation(location);
		}
	}*/
}
