package me.lazerka.mf.web.rest.location;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.urlfetch.*;
import com.google.appengine.api.urlfetch.FetchOptions.Builder;
import com.google.common.base.Charsets;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Work;
import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.gcm.GcmDataLocation;
import me.lazerka.mf.api.gcm.GcmRequest;
import me.lazerka.mf.api.gcm.GcmResponse;
import me.lazerka.mf.api.gcm.GcmResponse.Result;
import me.lazerka.mf.api.object.LocationRequest;
import me.lazerka.mf.api.object.LocationRequestResult;
import me.lazerka.mf.api.object.LocationRequestResult.GcmResult;
import me.lazerka.mf.entity.GcmRegistrationEntity;
import me.lazerka.mf.entity.MfUser;
import me.lazerka.mf.gae.Pair;
import me.lazerka.mf.gae.PairedList;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
	Objectify ofy;

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
	ObjectMapper objectMapper;

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

		MfUser recipientUser = getRecipientUser(emails);
		List<String> registrationIds = getRegistrationIds(recipientUser);
		GcmRequest gcmRequest = createGcmRequest(registrationIds);
		GcmResponse gcmResponse = sendGcmRequest(gcmRequest, emails);

		PairedList<GcmRegistrationEntity, Result> pairs =
				new PairedList<>(recipientUser.getGcmRegistrations(), gcmResponse.getResults());
		List<GcmResult> gcmResults = new ArrayList<>();
		for(Pair<GcmRegistrationEntity, Result> pair : pairs) {
			GcmRegistrationEntity registration = pair.getFirst();
			Result result = pair.getSecond();
			String newRegistrationId = result.getRegistrationId();
			if (newRegistrationId != null) {
				GcmRegistrationEntity newRegistration = new GcmRegistrationEntity(recipientUser, newRegistrationId, now);
				recipientUser = replaceRegistrationId(recipientUser, registration, newRegistration);
				registration = newRegistration;
			}

			GcmResult gcmResult = new GcmResult();
			gcmResult.setMessageId(result.getMessageId());
			gcmResult.setDeviceRegistrationHash(registration.getId());
			gcmResult.setError(result.getError());
			gcmResults.add(gcmResult);
		}

		LocationRequestResult result = new LocationRequestResult();
		result.setEmail(recipientUser.getEmail());
		result.setResults(gcmResults);
		return result;
	}

	private MfUser replaceRegistrationId(
			final MfUser recipientUser,
			final GcmRegistrationEntity oldEntity,
			final GcmRegistrationEntity newEntity) {
		logger.info("Replacing tokens for user {}", recipientUser);
		return ofy.transactNew(5, new Work<MfUser>() {
			@Override
			public MfUser run() {
				// Current state of recipientUser entity.
				MfUser currentState = ofy.load().entity(recipientUser).safe();

				List<GcmRegistrationEntity> list = currentState.getGcmRegistrations();
				if (list.indexOf(oldEntity) != -1) {
					logger.trace("Removing old token");
					list.remove(list.indexOf(oldEntity));
				}

				int iNew = list.indexOf(newEntity);
				if (iNew != -1) {
					logger.trace("Adding new token");
					list.add(newEntity);
				}

				ofy.save().entity(currentState);
				ofy.delete().entity(oldEntity);
				ofy.save().entity(newEntity);

				return currentState;
			}
		});
	}

	private GcmResponse sendGcmRequest(GcmRequest gcmRequest, List<String> emails) {
		String requestJson = serializeToJson(gcmRequest);
		logger.trace(requestJson);

		// Send the request to GCM.
		GcmResponse gcmResponse;
		try {
			FetchOptions fetchOptions = Builder.validateCertificate().followRedirects();
			HTTPRequest request = new HTTPRequest(new URL(GCM_ENDPOINT_URL), HTTPMethod.POST, fetchOptions);
			request.addHeader(new HTTPHeader("Content-Type", "application/json"));
			request.addHeader(new HTTPHeader("Authorization", "key=" + gcmApiKey));
			request.setPayload(requestJson.getBytes(Charsets.UTF_8));

			HTTPResponse httpResponse = urlFetchService.fetch(request);
			String responseContent = new String(httpResponse.getContent(), Charsets.UTF_8);
			logger.trace(responseContent);
			if (httpResponse.getResponseCode() == 200) {
				gcmResponse = objectMapper.readValue(responseContent, GcmResponse.class);
			} else {
				logger.error("Sending GCM message to {} failed: {}", emails, responseContent);
				Response response = Response.status(Status.INTERNAL_SERVER_ERROR).entity("GCM sending error " + httpResponse
						.getResponseCode()).build();
				throw new WebApplicationException(response);
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException("No way", e);
		} catch (IOException e) {
			Response entity = Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
			throw new WebApplicationException(entity);
		}
		return gcmResponse;
	}

	private MfUser getRecipientUser(List<String> emails) {
		// All emails must belong to only
		MfUser otherUser = ofy.load()
				.type(MfUser.class)
				.filter("email IN ", emails)
				.first()
				.now();
		if (otherUser == null) {
			throw new WebApplicationException(Response.status(404).entity("User not found").build());
		}
		return otherUser;
	}

	/**
	 * Results unique, ensured by Datastore.
	 * @see me.lazerka.mf.entity.GcmRegistrationEntity
	 */
	private List<String> getRegistrationIds(MfUser user) {
		List<GcmRegistrationEntity> gcmRegistrations = user.getGcmRegistrations();
		if (gcmRegistrations.isEmpty()) {
			throw new WebApplicationException(
					Response.status(404).entity("User doesn't have any GCM registrations").build());
		}
		List<String> registrationIds = new ArrayList<>(gcmRegistrations.size());
		for(GcmRegistrationEntity gcmRegistration : gcmRegistrations) {
			registrationIds.add(gcmRegistration.getToken());
		}
		return registrationIds;
	}

	private GcmRequest createGcmRequest(List<String> registrationIds) {
		// Compose request to GCM.
		GcmRequest gcmRequest = new GcmRequest();
		gcmRequest.setRegistrationIds(registrationIds);
		gcmRequest.setTimeToLiveSeconds(30);
		GcmDataLocation data = new GcmDataLocation();
		data.setRequesterEmail(user.getEmail());
		data.setSentAt(now);
		gcmRequest.setData(data);
		return gcmRequest;
	}

	private String serializeToJson(GcmRequest gcmRequest) {
		try {
			return objectMapper.writeValueAsString(gcmRequest);
		} catch (JsonProcessingException e) {
			Response response = Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Unable to serialize to JSON")
					.build();
			throw new WebApplicationException(response);
		}
	}
}
