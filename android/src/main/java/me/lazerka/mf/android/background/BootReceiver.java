package me.lazerka.mf.android.background;

import android.app.Activity;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.auth.GcmAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dzmitry Lazerka
 */
public class BootReceiver extends WakefulBroadcastReceiver {
	private static final Logger logger = LoggerFactory.getLogger(BootReceiver.class);

	@Override
	public void onReceive(Context context, Intent intent) {
		logger.info("onReceive");

		// Explicitly specify service that will handle the intent.
		intent.setComponent(new ComponentName(context, GcmCheckService.class));

		// Start the service, keeping the device awake while it is launching.
		startWakefulService(context, intent);
		setResultCode(Activity.RESULT_OK);
	}

	/**
	 * Makes sure GCM registration is active in background.
	 */
	public static class GcmCheckService extends IntentService {
		public GcmCheckService() {
			super(GcmCheckService.class.getSimpleName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {
			if (Application.preferences.getAccount() == null) {
				logger.warn("Account is null, no use registering for GCM");
				return;
			}

			GcmAuthenticator gcmAuthenticator = new GcmAuthenticator(this);
			gcmAuthenticator.checkRegistration();
		}
	}
}
