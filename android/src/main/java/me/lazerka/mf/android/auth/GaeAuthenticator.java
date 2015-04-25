package me.lazerka.mf.android.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.util.Log;
import com.google.common.collect.Lists;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.api.ApiConstants;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

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

		String userAgent = Application.USER_AGENT;
		AndroidHttpClient httpClient = AndroidHttpClient.newInstance(userAgent, null);
		// Don't follow redirects.
		httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
		HttpContext httpContext = new BasicHttpContext();
		BasicCookieStore cookieStore = new BasicCookieStore();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

		try {

			if (Application.isServerDev()) {
				return authDevAppserver(account.name, httpClient, httpContext, cookieStore);
			}

			URI uri = Application.SERVER_ROOT.resolve("/_ah/login?auth=" + androidAuthToken + "&continue=http://0.0.0.0/");
			HttpGet httpGet = new HttpGet(uri);
			HttpResponse response = httpClient.execute(httpGet, httpContext);
			HttpEntity entity = response.getEntity();
			InputStream is = entity.getContent();
			Header contentEncoding = entity.getContentEncoding();
			String encoding = contentEncoding != null ? contentEncoding.getValue() : null;

			String responseContent = IOUtils.toString(is, encoding);
			Log.i(TAG, "Response: " + responseContent);

			int statusCode = response.getStatusLine().getStatusCode();

			// dev_appserver case.
			if (statusCode == 200 && "text/html".equals(entity.getContentType().getValue())) {
				String msg = "Auth response should be 302, but is 200, looks like dev appserver";
				Log.w(TAG, msg);
				throw new AuthenticationException(msg);
			}

			if (statusCode != 302) {
				String msg = "GAE auth response code is not 302, but is " + statusCode;
				Log.w(TAG, msg);
				throw new AuthenticationException(msg);
			}

			for (Cookie cookie : cookieStore.getCookies()) {
				if (cookie.getName().equals(AUTH_TOKEN_COOKIE_NAME)) {
					Log.i(TAG, "GAE cookie retrieval successful.");
					return cookie.getValue();
				}
			}
			String msg = "GAE auth response code is 302, but cookie ACSID not set.";
			Log.e(TAG, msg);
			throw new AuthenticationException(msg);
		} finally {
			httpClient.close();
		}
	}

	private String authDevAppserver(
			String email,
			AndroidHttpClient httpClient,
			HttpContext httpContext,
			CookieStore cookieStore
	) throws IOException, AuthenticationException {
		int statusCode;
		Log.i(TAG, "Status code 200, must be dev_appserver, sending regular form...");
		URI uriDev = Application.SERVER_ROOT.resolve("/_ah/login");
		HttpPost post = new HttpPost(uriDev);

		List<NameValuePair> pairs = Lists.newArrayList();
		pairs.add(new BasicNameValuePair("email", email));
		pairs.add(new BasicNameValuePair("continue", "http://0.0.0.0/"));
		post.setEntity(new UrlEncodedFormEntity(pairs));
		post.setHeader("Content-Type", "application/x-www-form-urlencoded");

		HttpResponse response = httpClient.execute(post, httpContext);

		statusCode = response.getStatusLine().getStatusCode();

		if (statusCode != 302) {
			String msg = "GAE auth response code is not 302, but is " + statusCode;
			Log.w(TAG, msg);
			throw new AuthenticationException(msg);
		}

		for (Cookie cookie : cookieStore.getCookies()) {
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
