package me.lazerka.mf.web.rest.user;

import com.googlecode.objectify.Objectify;
import me.lazerka.mf.entity.MfUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.ArrayList;
import java.util.List;

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

	/**
	 * Eventually consistent, so if a user was just created, it won't be returned.
	 * That concerns current user as well!
	 */
	@GET
	@Produces("application/json")
	public List<UserBean> list() {
		List<MfUser> users = ofy.load().type(MfUser.class).list();
		logger.trace("Found {} users", users.size());

		List<UserBean> result = new ArrayList<>(users.size());
		for(MfUser user : users) {
			result.add(new UserBean(user));
		}
		return result;
	}

	@GET
	@Path("/me")
	@Produces("application/json")
	public UserBean me() {
		return new UserBean(user);
	}
}
