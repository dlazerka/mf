package me.lazerka.mf.android.background;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import me.lazerka.mf.api.gcm.LocationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles messages from GCM.
 *
 * @author Dzmitry Lazerka
 */
public class GcmIntentService extends IntentService {
	private static final Logger logger = LoggerFactory.getLogger(GcmIntentService.class);

	/**
	 * Message Bundle key that contains our payload from GCM.
	 */
	public static final String JSON = "json";

	private final Map<String, Handler> messageHandlers = new HashMap<>();

	// Binder given to clients
	private final ServiceBinder binder = new ServiceBinder();

	public GcmIntentService() {
		super("GcmIntentService");

		binder.bind(LocationRequest.TYPE, new LocationRequestHandlerOld(this));
	}

	@Override
	public ServiceBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
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
		logger.info("Received com.google.android.c2dm.intent.REGISTRATION");
		String gcmToken = intent.getStringExtra("registration_id");
		if (gcmToken != null) {
			logger.error("TODO");
			//GcmAuthenticator.storeGcmRegistration(gcmToken);
		} else {
			logger.warn("null registration id");
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
					logger.info("GCM Send error: " + extras.toString());
					break;
				case GoogleCloudMessaging.MESSAGE_TYPE_DELETED:
					logger.info("GCM Deleted messages: " + extras.toString());
					break;
				case GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE:
					// Look through messageHandlers to see if there's a message for them.
					boolean fired = false;
					for(String type : messageHandlers.keySet()) {

						String json = extras.getString(type);
						if (json != null) {
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
						logger.warn("Not found any handler for GCM message " + extras.toString());
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
