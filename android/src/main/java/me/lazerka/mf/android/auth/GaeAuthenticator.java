package me.lazerka.mf.android.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.util.Log;
import com.squareup.okhttp.*;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.api.ApiConstants;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Authenticates our Android device to App Engine servers, using Google's default authentication mechanism.
 *
 * Essentially, exchanges Android-token (see {@link AndroidAuthenticator} for obtaining it)
 * for GAE-token. The GAE-token (or, simply auth-token) is then provided to our server as Cookie header,
 * so we use default Google authentication, and not invent our own.
 *
 * Google servers will validate the auth-token, and tell our server app that user is really who he is.
 *
 * Must be called from background thread, because does network operations. Currently called only when old token is
 * expired or non-existent.
 *
 * @see AndroidAuthenticator
 * @author Dzmitry Lazerka
 */
public class GaeAuthenticator {
	public static final String AUTH_TOKEN_COOKIE_NAME = "SACSID";
	private static final String TAG = GaeAuthenticator.class.getName();

	private final OkHttpClient httpClient;
	private final CookieManager cookieManager;

	public GaeAuthenticator() {
		cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

		httpClient = new OkHttpClient();
		httpClient.setFollowRedirects(false);
		httpClient.setCookieHandler(cookieManager);
	}

	/**
	 * For invoking from background.
	 *
	 *
	 * Invalidates token. Is called only when authentication fails, so probably token has expired.
	 * Shows notification on auth failure.
	 * @return authToken
	 */
	@Nonnull
	public String authenticate()
			throws IOException, AuthenticatorException, OperationCanceledException, AuthenticationException {
		Account account = checkNotNull(Application.preferences.getAccount());
		String androidAuthToken;
		try {
			AccountManager accountManager =
					(AccountManager) Application.context.getSystemService(Context.ACCOUNT_SERVICE);
			androidAuthToken = accountManager.blockingGetAuthToken(account, ApiConstants.ANDROID_AUTH_SCOPE, true);
			if (androidAuthToken != null) {
				accountManager.invalidateAuthToken(AndroidAuthenticator.ACCOUNT_TYPE, androidAuthToken);
				androidAuthToken = accountManager.blockingGetAuthToken(account, ApiConstants.ANDROID_AUTH_SCOPE, true);
			}
			return fetchGaeAuthToken(androidAuthToken);
		} catch (OperationCanceledException | AuthenticatorException e) {
			Log.e(TAG, "Cannot get token", e);
			throw e;
		}
	}

	@Nonnull
	private String fetchGaeAuthToken(String androidAuthToken) throws IOException, AuthenticationException {
		Log.v(TAG, "fetchGaeAuthToken");

		Account account = Application.preferences.getAccount();
		if (account == null) {
			Log.e(TAG, "Account is null?!");
			throw new NoAccountException();
		}

//		String userAgent = Application.USER_AGENT;
//		AndroidHttpClient httpClient = AndroidHttpClient.newInstance(userAgent, null);
		// Don't follow redirects.
//		httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
//		HttpContext httpContext = new BasicHttpContext();
//		BasicCookieStore cookieStore = new BasicCookieStore();
//		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

			if (Application.isServerDev()) {
				return authDevAppserver(account.name);
			}

			URI uri = Application.SERVER_ROOT.resolve("/_ah/login?auth=" + androidAuthToken + "&continue=http://0.0.0.0/");
//			HttpGet httpGet = new HttpGet(uri);
//			HttpResponse response = httpClient.execute(httpGet, httpContext);

			HttpUrl httpUrl  = HttpUrl.get(Application.SERVER_ROOT)
					.resolve("/_ah/login")
					.newBuilder()
					.addQueryParameter("auth", androidAuthToken)
					.addQueryParameter("continue", "http://0.0.0.0/")
					.build();

			Request request = new Request.Builder()
					.url(httpUrl)
					.build();
			Response response = httpClient.newCall(request).execute();

//            HttpEntity entity = response.getEntity();
//			InputStream is = entity.getContent();
//			Header contentEncoding = entity.getContentEncoding();
//			String encoding = contentEncoding != null ? contentEncoding.getValue() : null;

//			String responseContent = IOUtils.toString(is, encoding);
			Log.i(TAG, "Response: " + response.message());

			int statusCode = response.code();

			// dev_appserver case.
			String contentType = response.header("Content-Type");
			if (statusCode == 200 && "text/html".equals(contentType)) {
				String msg = "Auth response should be 302, but is 200, looks like dev appserver";
				Log.w(TAG, msg);
				throw new AuthenticationException(msg);
			}

			if (statusCode != 302) {
				String msg = "GAE auth response code is not 302, but is " + statusCode;
				Log.w(TAG, msg);
				throw new AuthenticationException(msg);
			}

			for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
				if (cookie.getName().equals(AUTH_TOKEN_COOKIE_NAME)) {
					Log.i(TAG, "GAE cookie retrieval successful.");
					return cookie.getValue();
				}
			}
			String msg = "GAE auth response code is 302, but cookie ACSID not set.";
			Log.e(TAG, msg);
			throw new AuthenticationException(msg);
	}

	private String authDevAppserver(String email) throws IOException, AuthenticationException {
//		int statusCode;
		Log.i(TAG, "Status code 200, must be dev_appserver, sending regular form...");
		HttpUrl httpUrl = HttpUrl.get(Application.SERVER_ROOT)
				.resolve("/_ah/login");

		RequestBody body = new FormEncodingBuilder()
				.add("email", email)
				.add("continue", "http://0.0.0.0/")
				.build();
		Request request = new Request.Builder()
				.url(httpUrl)
				.post(body)
				.build();

		Response response = httpClient.newCall(request).execute();

//        URI uriDev = Application.SERVER_ROOT.resolve("/_ah/login");
//		HttpPost post = new HttpPost(uriDev);

//		List<NameValuePair> pairs = Lists.newArrayList();
//		pairs.add(new BasicNameValuePair("email", email));
//		pairs.add(new BasicNameValuePair("continue", "http://0.0.0.0/"));
//		post.setEntity(new UrlEncodedFormEntity(pairs));
//		post.setHeader("Content-Type", "application/x-www-form-urlencoded");

//		HttpResponse response = httpClient.execute(post, httpContext);

//		statusCode = response.getStatusLine().getStatusCode();

		if (response.code() != 302) {
			String msg = "GAE auth response code is not 302, but is " + response.code();
			Log.w(TAG, msg);
			throw new AuthenticationException(msg);
		}

		for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
			if (cookie.getName().equals("dev_appserver_login")) {
				// It's OK to log this auth cookie, because it's local dev server only.
				Log.i(TAG, "GAE cookie retrieval successful: " + cookie.getValue());
				return cookie.getValue();
			}
		}

		String msg = "GAE auth response code is 302, but cookie dev_appserver_login not set.";
		Log.e(TAG, msg);
		throw new AuthenticationException(msg);
	}

	public class AuthenticationException extends Exception {
		public AuthenticationException() {}

		public AuthenticationException(String detailMessage) {
			super(detailMessage);
		}
	}

	private class NoAccountException extends AuthenticationException {
	}
}
