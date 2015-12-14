package me.lazerka.mf.android.background;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import me.lazerka.mf.android.background.gcm.GcmRegisterIntentService;
import me.lazerka.mf.android.background.gcm.LocationRequestHandler;
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

		// Renew GCM token.
		intent.setComponent(new ComponentName(context, GcmRegisterIntentService.class));
		startWakefulService(context, intent);


		Intent locationRequestHandler = new Intent(context, LocationRequestHandler.class);
		context.startService(locationRequestHandler);


		setResultCode(Activity.RESULT_OK);
	}
}
