package me.lazerka.mf.android.background;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.auth.GcmAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

		GcmAuthenticator gcmAuthenticator = new GcmAuthenticator(this);
		gcmAuthenticator.renewRegistration();

		// Release the wake lock provided by the WakefulBroadcastReceiver, if was started with.
		WakefulBroadcastReceiver.completeWakefulIntent(intent);
	}
}
