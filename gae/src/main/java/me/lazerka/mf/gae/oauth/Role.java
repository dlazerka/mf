package me.lazerka.mf.gae.oauth;

import com.google.appengine.api.users.UserService;

/**
 * @author Dzmitry Lazerka
 */
public class Role {
	/**
	 * Application admins, as determined by {@link UserService#isUserAdmin()}
	 */
	public static final String ADMIN = "ADMIN";

	/**
	 * Any user on local development server.
	 */
	public static final String DEVSERVER = "DEVSERVER";

	/**
	 * Any user with valid OAUTH token.
	 */
	public static final String OAUTH = "OAUTH";
}
