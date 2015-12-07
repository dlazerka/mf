package me.lazerka.mf.gae.web.rest.user;

import com.google.common.collect.HashMultimap;
import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.object.UserInfo;
import me.lazerka.mf.api.object.UsersInfoRequest;
import me.lazerka.mf.api.object.UsersInfoResponse;
import me.lazerka.mf.gae.oauth.Role;
import me.lazerka.mf.gae.user.EmailNormalized;
import me.lazerka.mf.gae.user.MfUser;
import me.lazerka.mf.gae.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static me.lazerka.mf.gae.user.UserService.normalizeEmail;

/**
 * @author Dzmitry Lazerka
 */
@Path("/rest/user")
@Produces(ApiConstants.APPLICATION_JSON)
@RolesAllowed(Role.USER)
public class UserResource {
	private static final Logger logger = LoggerFactory.getLogger(UserResource.class);

	@Inject
	UserService userService;

	@GET
	@Path("/me")
	public CurrentUserBean me() {
		return new CurrentUserBean(userService.getCurrentUser());
	}

	/**
	 * A user requests available info on his friends (e.g. whether they installed the app at all).
	 *
	 * Note that we don't want to give anyone information about anyone. So we include in response only those users that
	 * already added requester to their friends.
	 *
	 * This doesn't mean user's allowed to see their location yet, of course. Location requests are authorized every
	 * time on client side by device who's being requested.
	 *
	 * TODO: do we really need this functionality? If forces us to store user friends on server-side, we could avoid
	 * that.
	 *
	 */
	@POST
	@Path("/friends")
	@Consumes("application/json")
	public UsersInfoResponse myEmail(UsersInfoRequest request) {
		MfUser currentUser = userService.getCurrentUser();
		logger.trace("By {} for {}", currentUser.getEmail(), request.getEmails());

		Set<String> emails = request.getEmails();
		if (emails == null) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("emails is null").build());
		}
		if (emails.size() > ApiConstants.MAXIMUM_FRIENDS_ALLOWED) {
			throw new WebApplicationException(
					Response.status(Status.BAD_REQUEST)
							.entity("emails is longer than " + ApiConstants.MAXIMUM_FRIENDS_ALLOWED).build());
		}

		HashMultimap<EmailNormalized, String> normalized = HashMultimap.create();
		for (String email : emails) {
			normalized.put(normalizeEmail(email), email);
		}

		// Save given emails as current user friends.
		currentUser.setFriendEmails(normalized.keySet());
		ofy().save().entity(currentUser); // async

		Set<MfUser> users = userService.getUsersByEmails(normalized.keySet());

		List<UserInfo> result = new ArrayList<>();
		for(MfUser user : users) {
			// Here we check if requested users have current user in their friends.
			Set<EmailNormalized> friendEmails = user.getFriendEmails();
			EmailNormalized currentUserEmail = currentUser.getEmail();
			if (friendEmails != null && friendEmails.contains(currentUserEmail)) {

				Set<String> clientEmails = normalized.get(user.getEmail());
				if (clientEmails == null) {
					logger.error("No client emails for normalized {}: {}", user.getEmail(), normalized);
				} else {
					UserInfo userInfo = new UserInfo(user.getEmail().getEmail(), clientEmails);
					result.add(userInfo);
				}
			}
		}

		return new UsersInfoResponse(result);
	}
}
