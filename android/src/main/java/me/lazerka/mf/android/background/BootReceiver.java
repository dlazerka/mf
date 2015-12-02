package me.lazerka.mf.android.background;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
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
		intent.setComponent(new ComponentName(context, RenewGcmTokenService.class));

		// Start the service, keeping the device awake while it is launching.
		startWakefulService(context, intent);
		setResultCode(Activity.RESULT_OK);
	}

}
