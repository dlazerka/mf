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

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Authenticates our Android to GAE servers, using Google's default authentication mechanism.
 *
 * Essentially, exchanges Android-token (see {@link AndroidAuthenticator} for obtaining it)
 * for GAE-token. The GAE-token (or, simply auth-token) is then provided to our server as Cookie header,
 * so we use default Google authentication, and not invent our own.
 *
 * Google servers will validate the auth-token, and tell our server app that user is really who he is.
 *
 * Must be called from background thread, because does network operations.
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
	@Nullable
	public String authenticate() {
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
		} catch (OperationCanceledException | IOException | AuthenticatorException e) {
			Log.e(TAG, "Cannot get token", e);
		}
		return null;
	}

	private String fetchGaeAuthToken(String androidAuthToken) {
		Log.v(TAG, "fetchGaeAuthToken");

		Account account = Application.preferences.getAccount();
		if (account == null) {
			Log.e(TAG, "Account is null?!");
			return null;
		}

		String userAgent = Application.USER_AGENT;
		AndroidHttpClient httpClient = AndroidHttpClient.newInstance(userAgent, null);
		// Don't follow redirects.
		httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
		HttpContext httpContext = new BasicHttpContext();
		BasicCookieStore cookieStore = new BasicCookieStore();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

		try {

			if (Application.SERVER_ROOT.getHost().contains("192.168.1")) {
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
				Log.w(TAG, "StatusCode=200, looks like dev appserver, while should be not.");
				return null;
			}

			if (statusCode != 302) {
				String msg = "GAE auth response code is not 302, but is " + statusCode;
				Log.w(TAG, msg);
				return null;
			}

			for (Cookie cookie : cookieStore.getCookies()) {
				if (cookie.getName().equals(AUTH_TOKEN_COOKIE_NAME)) {
					Log.i(TAG, "GAE cookie retrieval successful.");
					return cookie.getValue();
				}
			}
			Log.e(TAG, "GAE auth response code is 302, but cookie ACSID not set.");
		} catch (IOException e) {
			Log.e(TAG, "Cannot request GAE cookie: " + e.getMessage());
			//			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
		} finally {
			httpClient.close();
		}
		return null;
	}

	private String authDevAppserver(
			String email,
			AndroidHttpClient httpClient,
			HttpContext httpContext,
	        CookieStore cookieStore
	) throws IOException {
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
			return null;
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
		return null;
	}
}
