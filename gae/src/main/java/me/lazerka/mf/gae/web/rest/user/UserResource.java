package me.lazerka.mf.gae.web.rest.user;

import com.google.common.collect.HashMultimap;
import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.object.UserInfo;
import me.lazerka.mf.api.object.UsersInfoRequest;
import me.lazerka.mf.api.object.UsersInfoResponse;
import me.lazerka.mf.gae.UserUtils;
import me.lazerka.mf.gae.UserUtils.IllegalEmailFormatException;
import me.lazerka.mf.gae.entity.MfUser;
import me.lazerka.mf.gae.gcm.MfUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Dzmitry Lazerka
 */
@Path("/rest/user")
@Produces(ApiConstants.APPLICATION_JSON)
public class UserResource {
	private static final Logger logger = LoggerFactory.getLogger(UserResource.class);

	@Inject
	MfUser currentUser;

	@Inject
	MfUserService mfUserService;

	@GET
	@Path("/me")
	public CurrentUserBean me() {
		return new CurrentUserBean(currentUser);
	}

	/**
	 * A user requests available info on his friends (e.g. whether they installed the app at all).
	 *
	 * Note that we don't want to give anyone information about anyone. So we include in response only those users that
	 * already  * added requester to their friends.
	 *
	 * This doesn't mean user's allowed to see their location yet, of course. Location requests are authorized every
	 * time on client side by device who's being requested.
	 *
	 */
	@POST
	@Path("/friends")
	@Consumes("application/json")
	public UsersInfoResponse myEmail(UsersInfoRequest request) {
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

		HashMultimap<String, String> normalized = HashMultimap.create();
		for(String email : emails) {
			try {
				normalized.put(UserUtils.normalizeGmailAddress(email), email);
			} catch (IllegalEmailFormatException e) {
				logger.warn(email, e);
				// TODO: tell client-side about this
			}
		}

		// Save given emails as current user friends.
		currentUser.setFriendEmails(normalized.keySet());
		ofy().save().entity(currentUser); // async

		Set<MfUser> users = mfUserService.getUsersByEmails(normalized.keySet());

		List<UserInfo> result = new ArrayList<>();
		for(MfUser user : users) {
			// Here we check if requested users have current user in their friends.
			Set<String> friendEmails = user.getFriendEmails();
			String currentUserEmail = currentUser.getEmail();
			if (friendEmails != null && friendEmails.contains(currentUserEmail)) {

				Set<String> clientEmails = normalized.get(user.getEmail());
				if (clientEmails == null) {
					logger.error("No client emails for canonic {}: {}", user.getEmail(), normalized);
				} else {
					UserInfo userInfo = new UserInfo(user.getEmail(), clientEmails);
					result.add(userInfo);
				}
			}
		}

		return new UsersInfoResponse(result);
	}
}
