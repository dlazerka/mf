package me.lazerka.mf.gae.oauth;

import com.google.appengine.api.users.UserService;

/**
 * @author Dzmitry Lazerka
 */
public class Role {
	/**
	 * Any user with valid OAUTH token.
	 */
	public static final String USER = "USER";

	/**
	 * Application admins, as determined by {@link UserService#isUserAdmin()}
	 */
	public static final String ADMIN = "ADMIN";
}
