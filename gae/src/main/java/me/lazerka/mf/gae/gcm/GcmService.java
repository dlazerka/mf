package me.lazerka.mf.gae.gcm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.appengine.api.urlfetch.*;
import com.google.appengine.api.urlfetch.FetchOptions.Builder;
import com.google.common.base.Charsets;
import me.lazerka.mf.api.JsonMapper;
import me.lazerka.mf.api.gcm.GcmConstants;
import me.lazerka.mf.api.gcm.GcmPayload;
import me.lazerka.mf.api.gcm.GcmRequest;
import me.lazerka.mf.api.gcm.GcmResponse;
import me.lazerka.mf.api.gcm.GcmResponse.Result;
import me.lazerka.mf.api.object.LocationRequestResult.GcmResult;
import me.lazerka.mf.gae.Pair;
import me.lazerka.mf.gae.PairedList;
import me.lazerka.mf.gae.entity.GcmRegistrationEntity;
import me.lazerka.mf.gae.entity.MfUser;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Dzmitry Lazerka
 */
public class GcmService {
	private static final Logger logger = LoggerFactory.getLogger(GcmService.class);

	private static final int TIME_TO_LIVE_SECONDS = 60;

	@Inject
	URLFetchService urlFetchService;

	@Inject
	JsonMapper objectMapper;

	@Inject
	@Named("gcm.api.key")
	String gcmApiKey;

	@Inject
	@Named("now")
	DateTime now;

	@Inject
	MfUser currentUser;

	/**
	 * Sends given payload to given user through GCM.
	 *
	 * Since user may have multiple devices and GCM registration IDs, returns multiple {@link GcmResult}s.
	 */
	public List<GcmResult> send(MfUser recipient, GcmPayload payload) {
		List<String> registrationIds = getRegistrationIds(recipient);

		GcmRequest gcmRequest = createGcmRequest(registrationIds, payload);
		GcmResponse gcmResponse = sendGcmRequest(gcmRequest);

		List<GcmRegistrationEntity> registrations = fetchGcmRegistrationEntities(recipient);
		PairedList<GcmRegistrationEntity, Result> pairs =
				new PairedList<>(registrations, gcmResponse.getResults());
		List<GcmResult> gcmResults = new ArrayList<>(pairs.size());
		Set<GcmRegistrationEntity> toRemove = new HashSet<>();
		Set<GcmRegistrationEntity> toAdd = new HashSet<>();

		for(Pair<GcmRegistrationEntity, Result> pair : pairs) {
			GcmRegistrationEntity registration = pair.getFirst();
			Result result = pair.getSecond();

			String error = result.getError();
			if (error != null) {
				switch (error) {
					case GcmConstants.ERROR_UNAVAILABLE:
						logger.warn("GCM unavailable: {} for {}", error, recipient.getEmail());
						// Not sure it makes sense to retry right away, probably let's client decide.
						break;
					case GcmConstants.ERROR_NOT_REGISTERED:
					case GcmConstants.ERROR_INVALID_REGISTRATION:
						logger.warn("Sending GCM failed: {} for {}", error, recipient.getEmail());
						toRemove.add(registration);
						break;
					default:
						logger.error("Sending GCM failed: {} for {}", error, recipient.getEmail());
				}
			}
			String newRegistrationId = result.getRegistrationId();
			if (newRegistrationId != null) {
				toRemove.add(registration);
				registration = new GcmRegistrationEntity(recipient, newRegistrationId, now);
				toAdd.add(registration);
			}

			GcmResult gcmResult = new GcmResult(result.getMessageId(), error);
			gcmResults.add(gcmResult);
		}

		fixRegistrationIds(recipient, toRemove, toAdd);

		return gcmResults;
	}

	private GcmResponse sendGcmRequest(GcmRequest gcmRequest) {
		String requestJson = serializeToJson(gcmRequest);
		logger.trace(requestJson);

		// Send the request to GCM.
		GcmResponse gcmResponse;
		try {
			FetchOptions fetchOptions = Builder.validateCertificate().followRedirects();
			HTTPRequest request = new HTTPRequest(new URL(GcmConstants.GCM_SEND_ENDPOINT), HTTPMethod.POST, fetchOptions);
			request.addHeader(new HTTPHeader("Content-Type", "application/json"));
			request.addHeader(new HTTPHeader("Authorization", "key=" + gcmApiKey));
			request.setPayload(requestJson.getBytes(Charsets.UTF_8));

			HTTPResponse httpResponse = urlFetchService.fetch(request);
			String responseContent = new String(httpResponse.getContent(), Charsets.UTF_8);
			logger.trace(responseContent);
			if (httpResponse.getResponseCode() == 200) {
				gcmResponse = objectMapper.readValue(responseContent, GcmResponse.class);
			} else {
				logger.error("Sending GCM message {} failed: {}", gcmRequest, responseContent);
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


	private String serializeToJson(GcmRequest gcmRequest) {
		try {
			return objectMapper.writeValueAsString(gcmRequest);
		} catch (JsonProcessingException e) {
			Response response = Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Unable to serialize to JSON: " + e.getMessage())
					.build();
			throw new WebApplicationException(response);
		}
	}

	private GcmRequest createGcmRequest(List<String> registrationIds, GcmPayload payload) {
		// Compose request to GCM.
		GcmRequest gcmRequest = new GcmRequest();
		gcmRequest.setRegistrationIds(registrationIds);
		gcmRequest.setTimeToLiveSeconds(TIME_TO_LIVE_SECONDS);
		gcmRequest.putPayload(payload);
		gcmRequest.setCollapseKey(currentUser.getEmail());
		return gcmRequest;
	}

	/**
	 * Resul1s are unique, ensured by Datastore.
	 * @see GcmRegistrationEntity
	 */
	private List<String> getRegistrationIds(MfUser user) {
		List<GcmRegistrationEntity> gcmRegistrationEntities = fetchGcmRegistrationEntities(user);
		if (gcmRegistrationEntities.isEmpty()) {
			throw new WebApplicationException(
					Response.status(404).entity("User doesn't have any GCM registrations").build());
		}
		List<String> registrationIds = new ArrayList<>(gcmRegistrationEntities.size());
		for(GcmRegistrationEntity gcmRegistrationEntity : gcmRegistrationEntities) {
			registrationIds.add(gcmRegistrationEntity.getId());
		}
		return registrationIds;
	}

	private List<GcmRegistrationEntity> fetchGcmRegistrationEntities(MfUser user) {
		return ofy().load()
				.type(GcmRegistrationEntity.class)
				.ancestor(user)
				.chunkAll()
				.list();
	}

	private void fixRegistrationIds(
			final MfUser recipientUser,
			final Set<GcmRegistrationEntity> toRemove,
			final Set<GcmRegistrationEntity> toAdd
	) {
		logger.info(
				"Replacing tokens for user {}: adding {}, removing {}.",
				recipientUser,
				toAdd.size(),
				toRemove.size());
		ofy().defer().save().entities(toAdd);
		ofy().defer().delete().entities(toRemove);
	}
}
