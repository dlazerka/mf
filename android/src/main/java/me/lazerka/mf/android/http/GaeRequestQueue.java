package me.lazerka.mf.android.http;

import android.util.Log;
import com.android.volley.*;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.auth.GaeAuthenticator;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Volley's RequestQueue fitted for our needs.
 *
 * Not-caching to disk, to not require EXTERNAL_STORAGE permission.
 *
 * @author Dzmitry Lazerka
 */
public class GaeRequestQueue extends RequestQueue {
	/**
	 * Characters allowed in Cookie header, value part.
	 * See RFC6265: US-ASCII characters excluding CTLs, whitespace DQUOTE, comma, semicolon, and backslash.
	 */
	private static final Pattern COOKIE_SAFE_VALUE =
			Pattern.compile("[\\x21\\x23-\\x2B\\x2D-\\x3A\\x3C-\\x5B\\x5D-\\x7E]*");

	/**
	 * This is important to avoid race conditions and infinite loops.
	 */
	private static final int THREAD_POOL_SIZE = 1;

	public static GaeRequestQueue create() {
		GaeAuthenticator authenticator = new GaeAuthenticator();
		Network network = new GaeNetwork(new HurlStack(), authenticator);
		GaeRequestQueue queue = new GaeRequestQueue(new NoCache(), network);
		queue.start();
		return queue;
	}

	public GaeRequestQueue(Cache cache, Network network) {
		super(cache, network, THREAD_POOL_SIZE);
	}

	private static class GaeNetwork extends BasicNetwork {
		private static final String TAG = GaeNetwork.class.getName();
		private final GaeAuthenticator authenticator;

		public GaeNetwork(HttpStack httpStack, GaeAuthenticator authenticator) {
			super(httpStack);
			this.authenticator = authenticator;
		}

		@Override
		public NetworkResponse performRequest(Request<?> req) throws VolleyError {
			checkArgument(req instanceof JsonSerializingRequest);
			JsonSerializingRequest<?> request = (JsonSerializingRequest<?>) req;

			String gaeAuthToken = Application.preferences.getGaeAuthToken();
			if (gaeAuthToken == null) {
				gaeAuthToken = obtainGaeAuthToken();
			}

			setAuthCookie(gaeAuthToken, request);

			NetworkResponse response = super.performRequest(request);

			if (shouldAuthenticateResponse(response.statusCode)) {
				Log.i(TAG, "Should authenticate request #" + request.getSequence());
				gaeAuthToken = obtainGaeAuthToken();

				// Race condition (between concurrent requests) OK.
				Application.preferences.setGaeAuthToken(gaeAuthToken);

				setAuthCookie(gaeAuthToken, request);

				Log.v(TAG, "Sending request #" + request.getSequence() + " again.");
				response = super.performRequest(request);

				if (shouldAuthenticateResponse(response.statusCode)) {
					Log.i(TAG, "Still not authenticated request #" + request.getSequence() + ", aborting.");
					throw new AuthFailureError("Unable to authenticate: " + response.statusCode);
				}
			}

			return response;
		}

		/**
		 * Main purpose of this class.
		 */
		private void setAuthCookie(String gaeAuthToken, JsonSerializingRequest request) {
			String name = Application.IS_SERVER_LOCAL
					? "dev_appserver_login"
					: GaeAuthenticator.AUTH_TOKEN_COOKIE_NAME;

			// Check at low-level that the token is never sent over insecure HTTP, unless on local dev server.
			if (!Application.IS_SERVER_LOCAL && !request.getUrl().startsWith("https")) {
				// Weird Android SDK: have to cast to Object.
				String requestClassName = ((Object) request).getClass().getName();
				throw new RuntimeException(
						"Production GAE auth token is sent over insecure protocol " + requestClassName);
			}

			// Check that auth-token contains only RFCvalid chars.
			checkArgument(COOKIE_SAFE_VALUE.matcher(gaeAuthToken).matches(),
					"Auth cookie value not cookie-safe: {}", gaeAuthToken);

			request.setHeader("Cookie", name + "=" + gaeAuthToken);
		}

		private boolean shouldAuthenticateResponse(int statusCode) {
			return statusCode == HttpStatus.SC_FORBIDDEN || statusCode == HttpStatus.SC_MOVED_TEMPORARILY;
		}

		@Nonnull
		private String obtainGaeAuthToken() throws AuthFailureError {
			Log.i(TAG, "GaeAuthToken null, authenticating");
			String result = authenticator.authenticate();
			if (result == null) {
				Log.e(TAG, "GaeAuthToken is null, something is wrong");
				throw new AuthFailureError();
			}
			return result;
		}
	}

}
