package me.lazerka.mf.android.background;


import me.lazerka.mf.api.object.ApiObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Dzmitry Lazerka
 */
public class ApiRequest {
	private final String method;
	private final String url;
	private ApiObject apiObject;
	private ApiResponseHandler handler;

	public static ApiRequest get(String url, ApiResponseHandler handler) {
		return new ApiRequest("GET", url, null, handler);
	}

	public static ApiRequest put(String url, ApiObject obj) {
		return new ApiRequest("PUT", url, obj, null);
	}

	public static ApiRequest post(String url, ApiObject obj) {
		return new ApiRequest("POST", url, obj, null);
	}

	public ApiRequest(
			@Nonnull String method,
			@Nonnull String url,
			@Nullable ApiObject obj,
			@Nullable ApiResponseHandler handler
	) {
		this.method = method;
		this.url = url;
		this.apiObject = obj;
		this.handler = handler;
	}

	public String getMethod() {
		return method;
	}

	public String getUrl() {
		return url;
	}

	public ApiObject getApiObject() {
		return apiObject;
	}

	public void setApiObject(ApiObject apiObject) {
		this.apiObject = apiObject;
	}

	public ApiResponseHandler getHandler() {
		return handler;
	}

	public void setHandler(ApiResponseHandler handler) {
		this.handler = handler;
	}

	@Override
	public String toString() {
		return method + " " + url + (apiObject == null ? "" : " " + apiObject);
	}
}
