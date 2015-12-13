package me.lazerka.mf.gae.web.rest.location;

import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.gcm.MyLocationGcmPayload;
import me.lazerka.mf.api.object.Location;
import me.lazerka.mf.api.object.LocationRequest;
import me.lazerka.mf.api.object.MyLocation;
import me.lazerka.mf.api.object.MyLocationResponse;
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
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static me.lazerka.mf.gae.web.rest.JerseyUtil.throwIfNull;

/**
 * @author Dzmitry Lazerka
 */
@Path(MyLocation.PATH)
@Produces(ApiConstants.APPLICATION_JSON)
@RolesAllowed(Role.USER)
public class MyLocationResource {
	private static final Logger logger = LoggerFactory.getLogger(MyLocationResource.class);

	@Inject
	UserService userService;

	@Inject
	GcmService gcmService;

	@Inject
	DateTime now;

	@POST
	@Consumes("application/json")
	public MyLocationResponse post(MyLocation myLocation) {
		logger.trace(myLocation.toString());
		LocationRequest sourceRequest = throwIfNull(myLocation.getLocationRequest(), MyLocation.LOCATION_REQUEST);
		String requesterEmail = throwIfNull(sourceRequest.getRequesterEmail(), LocationRequest.REQUESTER_EMAIL);
		String requestId = throwIfNull(sourceRequest.getRequestId(), LocationRequest.REQUEST_ID);
		Location location = throwIfNull(myLocation.getLocation(), MyLocation.LOCATION);

		logger.info("post {} for {}", requestId, requesterEmail);

		MfUser user = userService.getCurrentUser();

		// We don't trust client to set this, obviously.
		location.setEmail(user.getEmail().getEmail());

		DateTime clientsWhen = location.getWhen();
		if (new Duration(now, clientsWhen).isLongerThan(Duration.standardMinutes(1))) {
			logger.warn(
					"Client's location 'time' deviates from real one by more than a minute, overwriting: {}, {}",
					now,
					clientsWhen);
			location.setWhen(now);
		}

		MfUser requester = userService.getUserByEmail(requesterEmail);

		if (requester == null) {
			logger.warn("No user found by emails: {}", requesterEmail);
			throw new WebApplicationException(Response.status(404).entity("User not found").build());
		}

		MyLocationGcmPayload payload = new MyLocationGcmPayload(requestId, location);

		gcmService.send(requester, payload);

		return new MyLocationResponse(requestId);
	}
}
