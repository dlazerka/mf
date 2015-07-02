package me.lazerka.mf.gae.web.rest.user;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.object.UserFriendsPut;
import me.lazerka.mf.api.object.UserInfo;
import me.lazerka.mf.api.object.UsersInfoGet;
import me.lazerka.mf.api.object.UsersInfoResponse;
import me.lazerka.mf.gae.UserUtils;
import me.lazerka.mf.gae.entity.MfUser;
import me.lazerka.mf.gae.gcm.MfUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.*;

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

	@PUT
	@Path("/friends")
	public void putFriends(UserFriendsPut req) {
		logger.trace("User {} puts his friends {}", currentUser.getEmail(), req.getEmails());

		if (req.getEmails() == null) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("emails is null").build());
		}

		HashSet<String> canonicalized = new HashSet<>();
		for(String friendEmail : req.getEmails()) {
			canonicalized.add(UserUtils.canonicalizeGmailAddress(friendEmail));
		}
		currentUser.setFriendEmails(canonicalized);
		ofy().save().entity(currentUser).now();
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
	@Consumes("application/json")
	public UsersInfoResponse byEmail(UsersInfoGet request) {
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

		ListMultimap<String, String> canonicalized = ArrayListMultimap.create();
		for(String email : emails) {
			canonicalized.put(UserUtils.canonicalizeGmailAddress(email), email);
		}

		Set<MfUser> users = mfUserService.getUsersByEmails(canonicalized.keySet());

		Map<UserInfo, Set<String>> result = new LinkedHashMap<>();
		for(MfUser user : users) {
			// Here we check if requested users have current user in their friends.
			Set<String> friendEmails = user.getFriendEmails();
			if (friendEmails != null && friendEmails.contains(currentUser.getEmail())) {

				List<String> clientEmails = canonicalized.get(user.getEmail());

				UserInfo userInfo = new UserInfo(user.getEmail());
				result.put(userInfo, new HashSet<>(clientEmails));
			}
		}

		return new UsersInfoResponse(result);
	}
}
