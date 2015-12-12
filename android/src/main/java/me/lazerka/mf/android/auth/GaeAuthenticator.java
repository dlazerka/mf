package me.lazerka.mf.android.auth;

import android.accounts.Account;
import com.squareup.okhttp.*;
import me.lazerka.mf.android.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;

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
	private static final Logger logger = LoggerFactory.getLogger(GaeAuthenticator.class);

	public static final String AUTH_TOKEN_COOKIE_NAME = "SACSID";

	private final OkHttpClient httpClient;
	private final CookieManager cookieManager;

	public GaeAuthenticator() {
		cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

		httpClient = new OkHttpClient();
		httpClient.setFollowRedirects(false);
		httpClient.setCookieHandler(cookieManager);
	}

	@Nonnull
	private String fetchGaeAuthToken(String androidAuthToken) throws IOException, AuthenticationException {
		Account account = Application.preferences.getAccount();
		if (account == null) {
			logger.error("Account is null?!");
			throw new NoAccountException();
		}

		if (Application.isServerDev()) {
			return authDevAppserver(account.name);
		}

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

		logger.info("Response: " + response.message());

		int statusCode = response.code();

		// dev_appserver case.
		String contentType = response.header("Content-Type");
		if (statusCode == 200 && "text/html".equals(contentType)) {
			String msg = "Auth response should be 302, but is 200, looks like dev appserver";
			logger.warn(msg);
			throw new AuthenticationException(msg);
		}

		if (statusCode != 302) {
			String msg = "GAE auth response code is not 302, but is " + statusCode;
			logger.warn(msg);
			throw new AuthenticationException(msg);
		}

		for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
			if (cookie.getName().equals(AUTH_TOKEN_COOKIE_NAME)) {
				logger.info("GAE cookie retrieval successful.");
				return cookie.getValue();
			}
		}
		String msg = "GAE auth response code is 302, but cookie ACSID not set.";
		logger.error(msg);
		throw new AuthenticationException(msg);
	}

	private String authDevAppserver(String email) throws IOException, AuthenticationException {
		logger.info("Status code 200, must be dev_appserver, sending regular form...");
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

		if (response.code() != 302) {
			String msg = "GAE auth response code is not 302, but is " + response.code();
			logger.warn(msg);
			throw new AuthenticationException(msg);
		}

		for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
			if (cookie.getName().equals("dev_appserver_login")) {
				// It's OK to log this auth cookie, because it's local dev server only.
				logger.info("GAE cookie retrieval successful: " + cookie.getValue());
				return cookie.getValue();
			}
		}

		String msg = "GAE auth response code is 302, but cookie dev_appserver_login not set.";
		logger.error(msg);
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
