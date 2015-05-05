package me.lazerka.mf.android.activity;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import me.lazerka.mf.android.background.GcmIntentService.ServiceBinder;
import me.lazerka.mf.api.gcm.MyLocationGcmPayload;

/**
 * @author Dzmitry Lazerka
 */
public class GcmServiceConnection implements ServiceConnection {
	protected final String TAG = getClass().getName();
	private Handler handler;

	public GcmServiceConnection(Handler handler) {
		this.handler = handler;
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		Log.v(TAG, "onServiceConnected: " + name.toString());

		ServiceBinder serviceBinder = (ServiceBinder) binder;
		serviceBinder.bind(MyLocationGcmPayload.TYPE, handler);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		Log.v(TAG, "onServiceDisconnected: " + name.toString());
	}
}
