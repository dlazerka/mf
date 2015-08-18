package me.lazerka.mf.android.http;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
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
	private final Context context;

	public JsonRequester(
			int httpMethod,
			@Nonnull String url,
			@Nullable REQ request,
			@Nullable Class<RESP> responseClass
	) {
		this(httpMethod, url, request, responseClass, null);
	}

	public JsonRequester(
		int httpMethod,
		@Nonnull String url,
		@Nullable REQ request,
		@Nullable Class<RESP> responseClass,
		@Nullable Context context
	) {
		this.request = request;
		this.context = context;

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

		String msg;
		String errorMessage = error.getMessage() != null ? (": " + error.getMessage()) : "";
		if (error instanceof AuthFailureError) {
			Log.e(TAG, "AuthFailureError", error);
			msg = "Authentication error" + errorMessage;
		} else if (error.networkResponse == null) {
			msg = "Network error: " + errorMessage;
		} else if (error.networkResponse.statusCode == 404) {
			msg = getMessage404(error.networkResponse);
			Log.w(TAG, msg);
		} else {
			String errorData = HttpUtils.decodeNetworkResponseCharset(error.networkResponse, TAG);
			if (errorData.isEmpty()) {
				msg = "Server error: " + error.networkResponse.statusCode;
			} else {
				msg = "Server error: " + errorData;
			}
			Log.e(TAG, String.valueOf(request) + ": " + msg);
		}
		if (context != null) {
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
		}
	}

	protected String getMessage404(NetworkResponse networkResponse) {
		return "404 Not Found";
	}

	public void send() {
		Application.requestQueue.add(jsonSerializingRequest);
	}

	public REQ getRequest() {
		return request;
	}
}
