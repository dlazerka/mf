package me.lazerka.mf.android.background;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import me.lazerka.mf.android.auth.GcmAuthenticator;
import me.lazerka.mf.api.gcm.LocationRequestGcmPayload;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles messages from GCM. Basically, there's only one message -- if someone requests our location.
 *
 * @author Dzmitry Lazerka
 */
public class GcmIntentService extends IntentService {
	private static final String TAG = GcmIntentService.class.getName();

	/**
	 * Message Bundle key that contains our payload from GCM.
	 */
	public static final String JSON = "json";

	private final Map<String, Handler> messageHandlers = new HashMap<>();

	// Binder given to clients
	private final ServiceBinder binder = new ServiceBinder();

	public GcmIntentService() {
		super("GcmIntentService");

		binder.bind(LocationRequestGcmPayload.TYPE, new LocationRequestHandler(this));
	}

	@Override
	public void onCreate() {
		Log.v(TAG, "onCreate");
		super.onCreate();

		// Mostly for calling from BootReceiver.
		new GcmAuthenticator(this)
				.checkRegistration();
	}

	@Override
	public ServiceBinder onBind(Intent intent) {
		Log.v(TAG, "onBind");
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "onUnbind");
		return super.onUnbind(intent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		// Old deprecated way of receiving GCM Registration ID, but on Sony Xperia Ultra that's the only way.
		if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
			handleRegistrationId(intent);
		} else {
			handleReceiveMessage(intent);
		}

		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
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

	private void handleReceiveMessage(Intent intent) {
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
					Log.i(TAG, "GCM Send error: " + extras.toString());
					break;
				case GoogleCloudMessaging.MESSAGE_TYPE_DELETED:
					Log.i(TAG, "GCM Deleted messages: " + extras.toString());
					break;
				case GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE:
					Log.v(TAG, "GCM message: " + extras.toString());
					// Look through messageHandlers to see if there's a message for them.
					boolean fired = false;
					for(String type : messageHandlers.keySet()) {

						String json = extras.getString(type);
						if (json != null) {
							Log.v(TAG, "Handler found for " + type);

							Handler handler = messageHandlers.get(type);
							fired = true;

							// Pass the message payload to the handler.
							Bundle bundle = new Bundle(1);
							bundle.putString(JSON, json);
							Message message = Message.obtain(handler);
							message.setData(bundle);

							handler.sendMessage(message);
						}
					}

					if (!fired) {
						// May happen when we restarted app, and received enqueued GCM messages before map initializes
						// and sets it's handler.
						Log.w(TAG, "Not found any handler for GCM message " + extras.toString());
					}

					break;
			}
		}
	}

	/**
	 * Class used for the client Binder.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class ServiceBinder extends Binder {
		public GcmIntentService getService() {
			// Return this instance of LocalService so clients can call public methods
			return GcmIntentService.this;
		}

		public void bind(String type, Handler handler) {
			messageHandlers.put(type, handler);
		}
	}
}
