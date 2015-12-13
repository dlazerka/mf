package me.lazerka.mf.android.background.gcm;

import android.content.Intent;
import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * @author Dzmitry Lazerka
 */
public class InstanceIdService extends InstanceIDListenerService {
	@Override
	public void onTokenRefresh() {
		// Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
		Intent intent = new Intent(this, GcmRegisterIntentService.class);
		startService(intent);
	}
}
