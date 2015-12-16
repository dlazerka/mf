package me.lazerka.mf.android.background.gcm;

import android.content.Context;
import android.content.Intent;
import com.google.android.gms.gcm.GcmReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receiver of GCM messages.
 *
 * Hands off actual processing to GcmReceiveService, and prevents device from going to sleep.
 *
 * @author Dzmitry Lazerka
 */
public class GcmBroadcastReceiver extends GcmReceiver {
	private static final Logger logger = LoggerFactory.getLogger(GcmBroadcastReceiver.class);

	@Override
	public void onReceive(Context context, Intent intent) {
		logger.trace("onReceive");
		super.onReceive(context, intent);
	}
}
