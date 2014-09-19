package me.lazerka.mf.android.background;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 *
 *
 * @author Dzmitry Lazerka
 */
public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
	protected final String TAG = getClass().getName();

	@Override
	public void onReceive(Context context, Intent intent) {
		// Explicitly specify that GcmIntentService will handle the intent.
		ComponentName comp = new ComponentName(
				context.getPackageName(), GcmIntentService.class.getName());
		// Start the service, keeping the device awake while it is launching.
		startWakefulService(context, intent.setComponent(comp));
		setResultCode(Activity.RESULT_OK);
	}
}
