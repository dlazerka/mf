package me.lazerka.mf.gae.web.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.lazerka.mf.gae.entity.MfUser;
import me.lazerka.mf.gae.oauth.Role;

import javax.annotation.security.RolesAllowed;

/**
 * @author Dzmitry Lazerka
 */
@RolesAllowed(Role.OAUTH)
public class CurrentUserBean {
	@JsonProperty
	String id;

	@JsonProperty
	String email;

	public CurrentUserBean() {}

	public CurrentUserBean(MfUser user) {
		id = user.getId();
		email = user.getEmail();
	}
}
