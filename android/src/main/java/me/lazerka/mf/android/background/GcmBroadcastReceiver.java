package me.lazerka.mf.android.background;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Receiver of GCM messages.
 *
 * Hands off actual processing to GcmIntentService to prevent device from going to sleep.
 *
 * @author Dzmitry Lazerka
 */
public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
	protected final String TAG = getClass().getName();

	@Override
	public void onReceive(Context context, Intent intent) {
		// Explicitly specify that GcmIntentService will handle the intent.
		intent.setComponent(new ComponentName(context, GcmIntentService.class));

		// Start the service, keeping the device awake while it is launching.
		startWakefulService(context, intent);
		setResultCode(Activity.RESULT_OK);
	}
}
