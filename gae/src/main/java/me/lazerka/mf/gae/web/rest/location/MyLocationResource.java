package me.lazerka.mf.gae.web.rest.location;

import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.gcm.MyLocationGcmPayload;
import me.lazerka.mf.api.object.Location;
import me.lazerka.mf.api.object.MyLocation;
import me.lazerka.mf.api.object.MyLocationResponse;
import me.lazerka.mf.gae.entity.MfUser;
import me.lazerka.mf.gae.gcm.GcmService;
import me.lazerka.mf.gae.gcm.MfUserService;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response.Status;

/**
 * @author Dzmitry Lazerka
 */
@Path(MyLocation.PATH)
@Produces(ApiConstants.APPLICATION_JSON)
public class MyLocationResource {
	private static final Logger logger = LoggerFactory.getLogger(MyLocationResource.class);

	@Inject
	MfUserService mfUserService;

	@Inject
	GcmService gcmService;

	@Inject
	MfUser user;

	@Inject
	DateTime now;

	@POST
	@Consumes("application/json")
	public MyLocationResponse post(MyLocation myLocation) {
		logger.trace(myLocation.toString());
		String requesterEmail = myLocation.getRequesterEmail();
		Location location = myLocation.getLocation();
		String requestId = myLocation.getRequestId();

		logger.info("post {} for {}", requestId, requesterEmail);

		if (requesterEmail == null || location == null || requestId == null) {
			logger.warn("Something is null");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		// We don't trust client to set this, obviously.
		location.setEmail(user.getEmail());

		DateTime clientsWhen = location.getWhen();
		if (new Duration(now, clientsWhen).isLongerThan(Duration.standardMinutes(1))) {
			logger.warn(
					"Client's location 'time' deviates from real one by more than a minute, overwriting: {}, {}",
					now,
					clientsWhen);
			location.setWhen(now);
		}

		MfUser requester = mfUserService.getUserByEmail(requesterEmail);

		MyLocationGcmPayload payload = new MyLocationGcmPayload(requestId, location);

		gcmService.send(requester, payload);

		return new MyLocationResponse(requestId);
	}
}
