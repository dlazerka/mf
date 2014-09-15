package me.lazerka.mf.android.background;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import com.google.common.collect.Lists;
import me.lazerka.mf.android.background.SenderService.ServiceBinder;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Dzmitry Lazerka
 */
public class ServerConnection implements ServiceConnection {
	protected final String TAG = ((Object) this).getClass().getName();

	private SenderService service;
	private final LinkedList<ApiRequest> requestQueue = Lists.newLinkedList();

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		Log.v(TAG, "onServiceConnected: " + name.toString());
		ServiceBinder binder = (ServiceBinder) service;
		this.service = binder.getService();

		Iterator<ApiRequest> iterator = requestQueue.iterator();
		while(iterator.hasNext()) {
			ApiRequest apiRequest = iterator.next();
			this.service.send(apiRequest);
			iterator.remove();
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		Log.v(TAG, "onServiceDisconnected: " + name.toString());
		service = null;
		if (!requestQueue.isEmpty()) {
			Log.w(TAG, "Request queue was not empty! Size: " + requestQueue.size());
		}
	}

	public void send(ApiRequest request) {
		Log.d(TAG, "send: " + request);

		if (service != null) {
			service.send(request);
		} else {
			Log.w(TAG, "Not connected, enqueueing");
			requestQueue.add(request);
		}
	}

}
