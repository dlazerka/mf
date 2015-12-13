package me.lazerka.mf.gae.web.rest.location;

import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.object.LocationRequest;
import me.lazerka.mf.api.object.LocationRequestResult;
import me.lazerka.mf.api.object.LocationRequestResult.GcmResult;
import me.lazerka.mf.gae.gcm.GcmService;
import me.lazerka.mf.gae.oauth.Role;
import me.lazerka.mf.gae.user.MfUser;
import me.lazerka.mf.gae.user.UserService;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

import static me.lazerka.mf.gae.web.rest.JerseyUtil.throwIfNull;

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
		Set<String> forEmails = throwIfNull(locationRequest.getEmails(), LocationRequest.EMAILS);
		logger.trace("byEmail for {}", forEmails);

		MfUser sender = userService.getCurrentUser();
		MfUser recipient = userService.getUserByEmails(forEmails);

		if (recipient == null) {
			logger.warn("No user found by emails: {}", forEmails);
			throw new WebApplicationException(Response.status(404).entity("User not found").build());
		}

		Duration duration = locationRequest.getDuration() == null
				? Duration.standardMinutes(15)
				: locationRequest.getDuration();
		LocationRequest payload = new LocationRequest(
				locationRequest.getRequestId(),
				sender.getEmail().getEmail(), // requesterEmail
				now,
				duration // TODO make configurable
		);

		List<GcmResult> gcmResults = gcmService.send(recipient, locationRequest);

		LocationRequestResult result = new LocationRequestResult(
				recipient.getEmail().getEmail(),
				gcmResults
		);
		return result;
	}
}
