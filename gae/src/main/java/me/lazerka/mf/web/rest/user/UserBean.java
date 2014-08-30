package me.lazerka.mf.web.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.lazerka.mf.entity.MfUser;

/**
 * @author Dzmitry Lazerka
 */
public class UserBean {
	@JsonProperty
	String id;

	@JsonProperty
	String email;

	public UserBean() {}

	public UserBean(MfUser user) {
		id = user.getId();
		email = user.getEmail();
	}
}
