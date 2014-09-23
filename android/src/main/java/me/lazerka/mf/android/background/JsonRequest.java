package me.lazerka.mf.android.background;

import com.android.volley.Cache.Entry;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import me.lazerka.mf.android.Application;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Own implementation of Volley's JsonRequest to hide JSON [de]serialization] inside,
 * and to do serialization in a background thread.
 *
 * @author Dzmitry Lazerka
 */
public class JsonRequest<T> extends com.android.volley.toolbox.JsonRequest<T> {

	private final Object requestObject;
	private final Class<T> responseClass;

	public static <T> JsonRequest<T> get(
			@Nonnull String url,
			@Nullable Object request,
			@Nonnull Listener<T> listener,
			@Nonnull Class<T> responseClass,
			@Nonnull ErrorListener errorListener) {
		return new JsonRequest<>(Method.GET, url, request, listener, responseClass, errorListener);
	}

	public static <T> JsonRequest<T> post(
			@Nonnull String url,
			@Nonnull Object request,
			@Nullable Listener<T> listener,
			@Nullable Class<T> responseClass,
			@Nonnull ErrorListener errorListener) {
		return new JsonRequest<>(Method.POST, url, request, listener, responseClass, errorListener);
	}

	private JsonRequest(
			int method,
			@Nonnull String url,
			@Nullable Object request,
			@Nullable Listener<T> listener,
			@Nullable Class<T> responseClass,
			@Nullable ErrorListener errorListener) {
		super(
				method,
				url,
				null, // See getBody(), to carry serialization out of background thread.
				listener,
				errorListener);
		this.requestObject = request;
		this.responseClass = responseClass;
		checkArgument(listener == null ^ responseClass == null, "If you provide listener, provide response class");
	}

	@Override
	protected Response<T> parseNetworkResponse(NetworkResponse response) {
		try {
			String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
			T responseObject = Application.jsonMapper.readValue(jsonString, responseClass);
			Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
			return Response.success(responseObject, cacheEntry);
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
}
