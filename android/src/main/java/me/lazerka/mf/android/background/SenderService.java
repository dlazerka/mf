package me.lazerka.mf.android.background;

import android.app.Service;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.*;
import android.util.Log;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.auth.GaeAuthenticator;
import org.apache.http.params.HttpConnectionParams;

/**
 * Like IntentService, but doesn't stop itself after processing message.
 *
 * @author Dzmitry
 */
public class SenderService extends Service {
	protected final String TAG = ((Object) this).getClass().getName();

	// Binder given to clients
	private final IBinder binder = new ServiceBinder();

	private volatile HttpSenderOld handler;
	private volatile Looper looper;
	private AndroidHttpClient httpClient;

	@Override
	public void onCreate() {
		Log.v(TAG, "onCreate");
		super.onCreate();

		String userAgent = "Where My Friends " + Application.VERSION;
		httpClient = AndroidHttpClient.newInstance(userAgent, this);
		HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), 10000);

		HandlerThread thread = new HandlerThread(getClass().getSimpleName());
		thread.start();
		looper = thread.getLooper();
		handler = new HttpSenderOld(looper, httpClient, new GaeAuthenticator());
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "onBind");
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "onUnbind");
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy");
		httpClient.close();
		looper.quit();
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "onStartCommand");
		return START_STICKY;
	}

	public void send(ApiRequest apiRequest) {
		Log.v(TAG, "send");
		Message message = handler.obtainMessage();
		message.obj = apiRequest;
		handler.sendMessage(message);
	}

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class ServiceBinder extends Binder {
		public SenderService getService() {
			// Return this instance of LocalService so clients can call public methods
			return SenderService.this;
		}
	}

}
