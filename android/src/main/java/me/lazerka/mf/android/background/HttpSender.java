package me.lazerka.mf.android.background;

import android.net.http.AndroidHttpClient;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.Authenticator;
import me.lazerka.mf.api.object.ApiObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Sends API requests.
 * Runs in separate thread.
 */
class HttpSender extends Handler {
	protected final String TAG = getClass().getName();
	private final AndroidHttpClient httpClient;
	private final HttpContext httpContext;
	private final Authenticator authenticator;
	private final BasicCookieStore cookieStore;

	public HttpSender(Looper looper, AndroidHttpClient httpClient, Authenticator authenticator) {
		super(looper);
		this.httpClient = httpClient;
		this.authenticator = authenticator;
		cookieStore = new BasicCookieStore();
		httpContext = new BasicHttpContext();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	}

	public HttpRequestBase createRequest(ApiRequest apiRequest) {
		Log.i(TAG, "createRequest for " + apiRequest.getMethod() + ' ' + apiRequest.getUrl());

		String urlString = apiRequest.getUrl();
		String method = apiRequest.getMethod();

		if (urlString.endsWith("/")) {
			throw new IllegalArgumentException("Must not end with '/'.");
		}
		URI uri = Application.SERVER_ROOT.resolve(urlString);

		HttpRequestBase request;
		switch (method) {
			case "GET":
				request = new HttpGet(uri);
				break;
			case "POST":
				request = new HttpPost(uri);
				break;
			case "PUT":
				request = new HttpPut(uri);
				break;
			default:
				throw new IllegalArgumentException(method);
		}

		request.setHeader("User-Agent", Application.USER_AGENT);
		request.setHeader("Content-Type", "application/json; charset=UTF-8");

		ApiObject obj = apiRequest.getApiObject();
		if (obj != null) {

			ObjectMapper mapper = Application.JSON_MAPPER;
			String json;
			try {
				json = mapper.writeValueAsString(obj);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
			// Don't log full JSON on production server as there might be sensitive info.
			// Not that it's insecure, but let's be safe.
			if (Application.IS_SERVER_LOCAL) {
				Log.d(TAG, json);
			}

			HttpEntity entity = null;
			try {
				entity = new StringEntity(json, HTTP.UTF_8);
			} catch (UnsupportedEncodingException e) {
				Log.w(TAG, "Error encoding" + json + " to JSON: " + e.getMessage());
			}
			// Parent of both Post and Put.
			((HttpEntityEnclosingRequestBase) request).setEntity(entity);
		}

		return request;
	}

	@Override
	public void handleMessage(Message message) {
		Log.v(TAG, "handleMessage: " + message);

		ApiRequest apiRequest = (ApiRequest) checkNotNull(message.obj);

		HttpRequestBase request = createRequest(apiRequest);

		MyResponse response;
		try {
			response = execute(request);

			if (response.shouldAuthenticate()) {
				Log.i(TAG, "Status code " + response.getStatusCode() + ", authenticating to resend.");
				final String authToken = authenticator.authenticate();
				if (authToken != null) {
					cookieStore.addCookie(new AuthTokenCookie(authToken));
					response = execute(request);
				} else {
					Log.w(TAG, "AuthToken is null, not resending " + request.getURI());
					sendResponse(0, "Problem authenticating", apiRequest.getHandler());
					return;
				}
			}

			if (response.shouldAuthenticate()) {
				Log.w(TAG, "Still should authenticate, check permissions!");
				sendResponse(0, "Problem authenticating", apiRequest.getHandler());
				return;
			}

			Log.d(TAG, response.getContent());
			sendResponse(response.getStatusCode(), response.getContent(), apiRequest.getHandler());

		} catch (IOException e) {
			Log.w(TAG, "Error sending " + apiRequest + " request: " + e.getMessage());
			sendResponse(0, "Network error: " + e.getMessage(), apiRequest.getHandler());
		}
	}

	private void sendResponse(int statusCode, String content, @Nullable ApiResponseHandler handler) {
		if (handler != null) {
			Message msg = handler.obtainMyMessage(statusCode, content);
			handler.sendMessage(msg);
		}
	}

	private MyResponse execute(HttpRequestBase request) throws IOException {
		HttpResponse httpResponse = httpClient.execute(request, httpContext);
		MyResponse response = new MyResponse(httpResponse);
		Log.i(TAG, "Response " + response.getStatusCode() + " " + response.getReasonPhrase());
		return response;
	}

	private static class AuthTokenCookie implements Cookie {
		private final String authToken;

		public AuthTokenCookie(String authToken) {
			this.authToken = authToken;
		}

		@Override
		public String getName() {
			return Application.IS_SERVER_LOCAL ? "dev_appserver_login" : Authenticator.AUTH_TOKEN_COOKIE_NAME;
			//return Constants.COOKIE_NAME_AUTH_TOKEN;
		}

		@Override
		public String getValue() {
			return authToken;
		}

		@Override
		public String getComment() {
			return null;
		}

		@Override
		public String getCommentURL() {
			return null;
		}

		@Override
		public Date getExpiryDate() {
			return null;
		}

		@Override
		public boolean isPersistent() {
			return false;
		}

		@Override
		public String getDomain() {
			return Application.SERVER_ROOT.getHost();
		}

		@Override
		public String getPath() {
			return null;
		}

		@Override
		public int[] getPorts() {
			return new int[0];
		}

		@Override
		public boolean isSecure() {
			// Insecure for local server, as it doesn't support SSL.
			return !Application.IS_SERVER_LOCAL;
		}

		@Override
		public int getVersion() {
			return 0;
		}

		@Override
		public boolean isExpired(Date date) {
			return false;
		}
	}

	private class MyResponse {
		private final int statusCode;
		private final String reasonPhrase;
		private final String content;

		private MyResponse(HttpResponse response) throws IOException {
			this.statusCode = response.getStatusLine().getStatusCode();
			this.reasonPhrase = response.getStatusLine().getReasonPhrase();

			try {
				this.content = EntityUtils.toString(response.getEntity(), "UTF-8");
			} catch (IOException e) {
				throw new IOException("Unable to read response input stream", e);
			}
			response.getEntity().consumeContent();
		}

		public boolean shouldAuthenticate() {
			return getStatusCode() == HttpStatus.SC_FORBIDDEN || getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY;
		}

		public int getStatusCode() {
			return statusCode;
		}

		public String getReasonPhrase() {
			return reasonPhrase;
		}

		public String getContent() {
			return content;
		}
	}
}
