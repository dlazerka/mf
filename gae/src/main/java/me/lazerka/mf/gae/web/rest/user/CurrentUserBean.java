package me.lazerka.mf.gae.web.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.lazerka.mf.gae.user.MfUser;

/**
 * @author Dzmitry Lazerka
 */
public class CurrentUserBean {
	@JsonProperty
	String id;

	@JsonProperty
	String email;

	public CurrentUserBean() {}

	public CurrentUserBean(MfUser user) {
		id = user.getId();
		email = user.getEmail().getEmail();
	}
}
