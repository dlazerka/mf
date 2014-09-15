package me.lazerka.mf.web.rest.user;

import com.googlecode.objectify.Objectify;
import me.lazerka.mf.entity.MfUser;
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
public class UserResource {
	private static final Logger logger = LoggerFactory.getLogger(UserResource.class);

	@Inject
	Objectify ofy;

	@Inject
	MfUser user;

	@GET
	@Path("/me")
	@Produces("application/json")
	public UserBean me() {
		return new UserBean(user);
	}
}
