package me.lazerka.mf.gae.web.rest.user;

import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.gae.entity.MfUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * @author Dzmitry Lazerka
 */
@Path("/rest/user")
@Produces(ApiConstants.APPLICATION_JSON)
public class UserResource {
	private static final Logger logger = LoggerFactory.getLogger(UserResource.class);

	@Inject
	MfUser user;

	@GET
	@Path("/me")
	public UserBean me() {
		return new UserBean(user);
	}
}