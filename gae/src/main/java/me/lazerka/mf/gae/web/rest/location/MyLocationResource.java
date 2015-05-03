package me.lazerka.mf.gae.web.rest.location;

import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.gcm.MyLocationGcmPayload;
import me.lazerka.mf.api.object.MyLocation;
import me.lazerka.mf.api.object.MyLocationResponse;
import me.lazerka.mf.gae.entity.MfUser;
import me.lazerka.mf.gae.gcm.GcmService;
import me.lazerka.mf.gae.gcm.MfUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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

	@POST
	@Consumes("application/json")
	public MyLocationResponse post(MyLocation myLocation) {
		logger.trace("post {} for {}", myLocation.getRequestId(), myLocation.getRequesterEmail());

		MfUser requester = mfUserService.getUserByEmail(myLocation.getRequesterEmail());

		MyLocationGcmPayload payload = new MyLocationGcmPayload(myLocation.getRequestId(), myLocation.getLocation());

		gcmService.send(requester, payload);

		return new MyLocationResponse(myLocation.getRequestId());
	}
}
