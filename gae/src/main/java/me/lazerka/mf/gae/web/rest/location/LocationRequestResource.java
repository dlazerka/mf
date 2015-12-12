package me.lazerka.mf.gae.web.rest.location;

import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.gcm.LocationRequest;
import me.lazerka.mf.api.object.LocationRequestResult;
import me.lazerka.mf.api.object.LocationRequestResult.GcmResult;
import me.lazerka.mf.gae.user.MfUser;
import me.lazerka.mf.gae.gcm.GcmService;
import me.lazerka.mf.gae.user.UserService;
import me.lazerka.mf.gae.oauth.Role;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dzmitry Lazerka
 */
@Path(me.lazerka.mf.api.object.LocationRequest.PATH)
@Produces(ApiConstants.APPLICATION_JSON)
@RolesAllowed(Role.USER)
public class LocationRequestResource {
	private static final Logger logger = LoggerFactory.getLogger(LocationRequestResource.class);

	@Inject
	@Named("now")
	DateTime now;

	@Inject
	UserService userService;

	@Inject
	GcmService gcmService;

	/**
	 * Finds a user by given emails (must belong to only one user),
	 * then sends GCM message to all the registrations by this user (devices where he/she installed the app).
	 * Later, the other user will send its location to server, and we will send them back to requester using GCM.
	 *
	 * All given emails must belong to a single user, registered in the app.
	 *
	 * @return The GcmResponse received from GCM, but with removed registration IDs. User should not know
	 *         his friend's registration ids.
	 */
	@POST
	@Consumes("application/json")
	public LocationRequestResult byEmail(me.lazerka.mf.api.object.LocationRequest locationRequest) {
		MfUser user = userService.getCurrentUser();
		logger.trace("byEmail for {}", locationRequest.getEmails());

		ArrayList<String> emails = new ArrayList<>(locationRequest.getEmails());
		MfUser recipientUser = userService.getUserByEmails(emails);
		if (recipientUser == null) {
			logger.warn("No user found by emails: {}", emails);
			throw new WebApplicationException(Response.status(404).entity("User not found").build());
		}

		LocationRequest payload = new LocationRequest(
				locationRequest.getRequestId(),
				user.getEmail().getEmail(),
				now,
				Duration.standardMinutes(1) // TODO make configurable
		);

		List<GcmResult> gcmResults = gcmService.send(recipientUser, payload);

		LocationRequestResult result = new LocationRequestResult(
				recipientUser.getEmail().getEmail(),
				gcmResults
		);
		return result;
	}
}
