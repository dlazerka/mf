package me.lazerka.mf.android.background.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.WorkerThread;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Makes sure GCM registration is active in background.
 */
public class RenewGcmTokenService extends IntentService {
	private static final Logger logger = LoggerFactory.getLogger(RenewGcmTokenService.class);

	public RenewGcmTokenService() {
		super(RenewGcmTokenService.class.getSimpleName());
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (Application.preferences.getAccount() == null) {
			logger.warn("Account is null, no use registering for GCM");
			return;
		}

		renewRegistration();

	}

	/**
	 * Checks and renews GCM token.
	 * Sends to server the new one.
	 */
	@WorkerThread
	public void renewRegistration() {
		try {
			String token = getToken();
			sendGcmToken(token);
		} catch (IOException e) {
			// On Sony Xperia happens all the time, but fortunately GcmBroadcastReceiver receives the regId.
			logger.warn("GCM.register() failed: " + e.getMessage());
			// If there is an error, don't just keep trying to register.
			// Require the user to click a button again, or perform
			// exponential back-off.
		}
	}

	private String getToken() throws IOException {
		InstanceID instanceID = InstanceID.getInstance(this);
		// Initially this call goes out to the network to retrieve the token,
		// subsequent calls are local.
		return instanceID.getToken(
				getString(R.string.gcm_sender_id),
				GoogleCloudMessaging.INSTANCE_ID_SCOPE);
	}


	/**
	 * @param gcmToken to store.
	 */
	@WorkerThread
	public void sendGcmToken(final String gcmToken) throws IOException {
	}

}
