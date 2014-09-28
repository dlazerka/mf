package me.lazerka.mf.android;

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.common.AccountPicker;
import com.google.common.collect.Lists;
import me.lazerka.mf.android.activity.login.LoginActivity;
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
 * Handles authentication.
 * First, authenticates android Account from preferences.
 * Then, makes request to GAE to authenticate obtained authToken.
 *
 * @author Dzmitry Lazerka
 */
public class Authenticator {
	public static final String AUTH_TOKEN_COOKIE_NAME = "SACSID";
	private final String TAG = getClass().getName();
	private final String ACCOUNT_TYPE = "com.google";

	private final AccountManager accountManager;

	public Authenticator() {
		accountManager = (AccountManager) Application.context.getSystemService(Context.ACCOUNT_SERVICE);
	}

	/**
	 * @return null if valid, intent to show otherwise.
	 */
	public Intent checkAccountValid() {
		Account account = Application.preferences.getAccount();
		if (account == null || !isAccountAvailable(account)) {
			return AccountPicker.newChooseAccountIntent(
					account, null, new String[]{ACCOUNT_TYPE}, false, null, null, null, null);
		}
		return null;
	}

	private boolean isAccountAvailable(Account account) {
		Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
		for (Account availableAccount : accounts) {
			if (account.equals(availableAccount)) {
				return true;
			}
		}
		return false;
	}

	public interface AuthenticatorCallback {
		void onSuccess(String authToken);
		void onUserInputRequired(Intent intent);
		void onIOException(IOException e);
		void onAuthenticatorException(AuthenticatorException e);
		void onOperationCanceledException(OperationCanceledException e);
	}

	/**
	 * For invoking from foreground.
	 * Launches credentials prompt user hasn't approved service usage, and token cannot be issued.
	 * If user approved, do nothing with the token (see {@link #authenticate()} for real authentication).
	 *
	 * Note that GoogleAuthUtil.getToken() is completely different from AccountManager.
	 */
	public void checkUserPermission(final LoginActivity activity, final AuthenticatorCallback callback) {
		Account account = Application.preferences.getAccount();

		// It may start activity asking user for permission.
		AccountManagerCallback<Bundle> myCallback = new AccountManagerCallback<Bundle>() {
			@Override
			public void run(AccountManagerFuture<Bundle> future) {
				try {
					Bundle bundle = future.getResult();

					Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
					if (intent != null) {
						callback.onUserInputRequired(intent);
					}
					callback.onSuccess(bundle.getString(AccountManager.KEY_AUTHTOKEN));
				} catch (OperationCanceledException e) {
					callback.onOperationCanceledException(e);
				} catch (IOException e) {
					callback.onIOException(e);
				} catch (AuthenticatorException e) {
					callback.onAuthenticatorException(e);
				}
			}
		};
		accountManager.getAuthToken(account, ApiConstants.ANDROID_AUTH_SCOPE, Bundle.EMPTY, activity, myCallback, null);

		//GetAuthTokenCallback callback = new GetAuthTokenCallback(activity);
		//accountManager.getAuthToken(account, Constants.ANDROID_AUTH_SCOPE, Bundle.EMPTY, activity, callback, null);
	}

	/**
	 * For invoking from background.
	 * Invalidates token. Is called only when authentication fails, so probably token has expired.
	 * Shows notification on auth failure.
	 * @return authToken
	 */
	@Nullable
	public String authenticate() {
		Account account = checkNotNull(Application.preferences.getAccount());
		String authToken;
		try {
			authToken = accountManager.blockingGetAuthToken(account, ApiConstants.ANDROID_AUTH_SCOPE, true);
			if (authToken != null) {
				accountManager.invalidateAuthToken(ACCOUNT_TYPE, authToken);
				authToken = accountManager.blockingGetAuthToken(account, ApiConstants.ANDROID_AUTH_SCOPE, true);
			}
			return fetchGaeAuthToken(authToken);
		} catch (OperationCanceledException | IOException | AuthenticatorException e) {
			Log.e(TAG, "Cannot get token", e);
		}
		return null;
	}

	private String fetchGaeAuthToken(String authToken) {
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

			//URI uri = Application.SERVER_ROOT.resolve("https://www.google.com/accounts/ServiceLogin?service=ah&" +
			//		"passive=true&amp;continue=https://appengine.google.com/_ah/conflogin%3Fcontinue%3D&amp;" +
			//		"ltmpl=gm&amp;shdf= /_ah/login?auth=" + authToken + "&continue=http://0.0.0.0/");
			URI uri = Application.SERVER_ROOT.resolve("/_ah/login?auth=" + authToken + "&continue=http://0.0.0.0/");
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


	//private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
	//	private final LoginActivity activity;
	//
	//	public GetAuthTokenCallback(LoginActivity activity) {
	//		this.activity = activity;
	//	}
	//
	//	public void run(AccountManagerFuture<Bundle> future) {
	//		Bundle bundle = getResult(future);
	//
	//		Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
	//		if (intent != null) {
	//			Log.i(TAG, "Intent is non-null, starting credentials prompt activity.");
	//			// User input required
	//			activity.startActivity(intent);
	//		}
	//	}
	//}

}
