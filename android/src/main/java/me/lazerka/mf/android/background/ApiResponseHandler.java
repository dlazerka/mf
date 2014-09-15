package me.lazerka.mf.android.background;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import org.apache.http.HttpStatus;

import javax.annotation.Nullable;

/**
 * @author Dzmitry Lazerka
 */
public abstract class ApiResponseHandler extends Handler {
	protected final String TAG = getClass().getName();

	public Message obtainMyMessage(int responseCode, String responseJson) {
		return Message.obtain(this, responseCode, responseJson);
	}

	@Override
	public void handleMessage(Message msg) {
		String json = (String) msg.obj;
		int httpCode = msg.what;
		handleResponse(httpCode, json);
	}

	protected void handleResponse(int httpCode, @Nullable String json) {
		if (httpCode == HttpStatus.SC_OK) {
			handleSuccess(json);
		} else {
			Log.w(TAG, httpCode + ": " + json);
			handleError(httpCode, json);
		}
	}

	protected void handleSuccess(@Nullable String json) {
		throw new UnsupportedOperationException("Implement handleSuccess()");
	}

	protected void handleError(int httpCode, @Nullable String errorMessage) {
		Log.w(TAG, "Api error " + httpCode + ": " + errorMessage);
	}

}
