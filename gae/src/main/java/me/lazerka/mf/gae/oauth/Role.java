package me.lazerka.mf.gae.oauth;

import com.google.appengine.api.users.UserService;

/**
 * @author Dzmitry Lazerka
 */
public class Role {
	/**
	 * Any authenticated user.
	 */
	public static final String AUTHENTICATED = "AUTHENTICATED";

	/**
	 * Any user with valid OAUTH token.
	 */
	public static final String OAUTH = "OAUTH";

	/**
	 * Any user with authenticated using default Google Accounts.
	 */
	public static final String GAE = "GAE";

	/**
	 * Application admins, as determined by {@link UserService#isUserAdmin()}
	 */
	public static final String ADMIN = "ADMIN";

	/**
	 * Any user on local development server.
	 */
	public static final String DEVSERVER = "DEVSERVER";
}
