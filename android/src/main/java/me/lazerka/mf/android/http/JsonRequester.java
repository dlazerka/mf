package me.lazerka.mf.android.http;

import android.os.SystemClock;
import android.util.Log;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import me.lazerka.mf.android.Application;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Dzmitry Lazerka
 */
public abstract class JsonRequester<REQ, RESP> implements Listener<RESP>, ErrorListener {
	private final String TAG = getClass().getName() + "-" + SystemClock.uptimeMillis();

	private final JsonSerializingRequest<RESP> jsonSerializingRequest;
	private final REQ request;

	public JsonRequester(
			int httpMethod,
			@Nonnull String url,
			@Nullable REQ request,
			@Nullable Class<RESP> responseClass) {
		this.request = request;

		String absoluteUrl = Application.SERVER_ROOT.resolve(url).toString();
		jsonSerializingRequest = new JsonSerializingRequest<>(
				httpMethod,
				absoluteUrl,
				request,
				responseClass,
				this,
				this
		);
	}

	@Override
	public void onErrorResponse(VolleyError error) {
		Log.w(TAG, error.getMessage(), error);
	}

	public void send() {
		Application.requestQueue.add(jsonSerializingRequest);
	}

	public REQ getRequest() {
		return request;
	}
}
