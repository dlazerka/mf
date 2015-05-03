package me.lazerka.mf.android.http;

import android.os.SystemClock;
import android.util.Log;
import com.android.volley.*;
import com.android.volley.Cache.Entry;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import me.lazerka.mf.android.Application;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Own implementation of Volley's JsonRequest to hide JSON [de]serialization inside,
 * and to do serialization in a background thread.
 *
 * @author Dzmitry Lazerka
 */
public class JsonSerializingRequest<T> extends com.android.volley.toolbox.JsonRequest<T> {
	/** Just to distinguish requests in logs. */
	private final String TAG = JsonSerializingRequest.class.getName() + "-" + SystemClock.uptimeMillis();
	private final Map<String, String> headers = new LinkedHashMap<>();

	/**
	 * Authentication token for server (GAE). Should be shared between requests to not authenticate each time.
	 * I have a mixed feeling of storing it in a static
	 */
	private static String gaeAuthToken;

	private final Object requestObject;
	private final Class<T> responseClass;

	protected JsonSerializingRequest(
			int method,
			@Nonnull String url,
			@Nullable Object request,
			@Nullable Class<T> responseClass,
			@Nullable Listener<T> listener,
			@Nullable ErrorListener errorListener
	) {
		super(
				method,
				url,
				null, // See getBody(), to carry serialization out of background thread.
				listener,
				errorListener);
		this.requestObject = request;
		this.responseClass = responseClass;
		checkArgument((listener == null) == (responseClass == null), "If you provide listener, provide response class");
	}

	@Override
	protected Response<T> parseNetworkResponse(NetworkResponse response) {
		try {
			String responseContent = new String(response.data, HttpHeaderParser.parseCharset(response.headers));

			if (response.statusCode == 200) {
				Log.v(TAG, "Request successful, parsing.");
				T responseObject = Application.jsonMapper.readValue(responseContent, responseClass);
				Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
				return Response.success(responseObject, cacheEntry);
			} else {
				Log.v(TAG, "Request error " + response.statusCode);
				return Response.error(new VolleyError(response));
			}
		} catch (IOException e) {
			return Response.error(new ParseError(e));
		}
	}

	@Override
	public byte[] getBody() {
		if (requestObject == null) {
			return null;
		}

		try {
			return Application.jsonMapper.writeValueAsBytes(requestObject);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, String> getHeaders() throws AuthFailureError {
		return headers;
	}

	public String setHeader(@Nonnull String name, @Nonnull String value) {
		return headers.put(checkNotNull(name), checkNotNull(value));
	}
}
