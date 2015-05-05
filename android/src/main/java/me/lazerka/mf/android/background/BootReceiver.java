package me.lazerka.mf.android.background;

import android.app.Activity;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.widget.Toast;
import me.lazerka.mf.android.auth.GcmAuthenticator;

/**
 * @author Dzmitry Lazerka
 */
public class BootReceiver extends WakefulBroadcastReceiver {
	protected final String TAG = getClass().getName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive");

		// Explicitly specify service that will handle the intent.
		intent.setComponent(new ComponentName(context, GcmCheckService.class));

		// Start the service, keeping the device awake while it is launching.
		startWakefulService(context, intent);
		setResultCode(Activity.RESULT_OK);
	}

	/**
	 * Check GCM registration in background.
	 */
	private static class GcmCheckService extends IntentService {
		private final String TAG = getClass().getName();

		public GcmCheckService() {
			super(GcmCheckService.class.getSimpleName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {
			Log.v(TAG, "onHandleIntent");
			Toast.makeText(this, "TEST test", Toast.LENGTH_LONG).show();
			// Make sure we're listening GCM.
			GcmAuthenticator gcmAuthenticator = new GcmAuthenticator(this);
			gcmAuthenticator.checkRegistration();

		}
	}
}
