package me.lazerka.mf.gae.web.rest.location;

import com.google.appengine.api.urlfetch.URLFetchService;
import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.JsonMapper;
import me.lazerka.mf.api.gcm.LocationRequestGcmPayload;
import me.lazerka.mf.api.object.LocationRequest;
import me.lazerka.mf.api.object.LocationRequestResult;
import me.lazerka.mf.api.object.LocationRequestResult.GcmResult;
import me.lazerka.mf.gae.entity.MfUser;
import me.lazerka.mf.gae.gcm.GcmService;
import me.lazerka.mf.gae.gcm.MfUserService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dzmitry Lazerka
 */
@Path(LocationRequest.PATH)
@Produces(ApiConstants.APPLICATION_JSON)
public class LocationRequestResource {
	private static final Logger logger = LoggerFactory.getLogger(LocationRequestResource.class);

	private static final String GCM_ENDPOINT_URL = "https://android.googleapis.com/gcm/send";

	@Inject
	MfUser user;

	@Inject
	@Named("now")
	DateTime now;

	@Inject
	URLFetchService urlFetchService;

	@Inject
	@Named("gcm.api.key")
	String gcmApiKey;

	@Inject
	JsonMapper objectMapper;

	@Inject
	MfUserService mfUserService;

	@Inject
	GcmService gcmService;

	/**
	 * Finds a user by given emails (must belong to only one user = google account),
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
	public LocationRequestResult byEmail(LocationRequest locationRequest) {
		logger.trace("byEmail for {}", locationRequest.getEmails());

		ArrayList<String> emails = new ArrayList<>(locationRequest.getEmails());
		MfUser recipientUser = mfUserService.getUserByEmails(emails);

		LocationRequestGcmPayload payload = new LocationRequestGcmPayload(
				locationRequest.getRequestId(),
				user.getEmail(),
				now
		);

		List<GcmResult> gcmResults = gcmService.send(recipientUser, payload);

		LocationRequestResult result = new LocationRequestResult();
		result.setEmail(recipientUser.getEmail());
		result.setResults(gcmResults);
		return result;
	}
}
