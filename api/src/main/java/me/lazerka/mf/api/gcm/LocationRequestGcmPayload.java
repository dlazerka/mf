package me.lazerka.mf.api.gcm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.joda.time.DateTime;

/**
 * The `data` field of a request for location that comes to a friend's device.
 *
 * @author Dzmitry Lazerka
 */
public class LocationRequestGcmPayload extends GcmPayload {
	public static final String TYPE = "LocationRequest";

	public static final String SENT_AT = "sentAt";
	public static final String REQUEST_ID = "requestId";
	public static final String REQUESTER_EMAIL = "requesterEmail";

	@JsonProperty(REQUEST_ID)
	private String requestId;

	/**
	 * Who's asking for location.
	 * User's email as given by Google authentication service.
	 * Receiver smartphone must check if this user is a "friend".
	 */
	@JsonProperty(REQUESTER_EMAIL)
	private String requesterEmail;

	/**
	 * When the message was received by server from the requester.
	 */
	@JsonProperty(SENT_AT)
	private DateTime sentAt;

	// For Jackson.
	private LocationRequestGcmPayload() {}

	public LocationRequestGcmPayload(String requestId, String requesterEmail, DateTime sentAt) {
		this.requestId = requestId;
		this.requesterEmail = requesterEmail;
		this.sentAt = sentAt;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	public String getRequestId() {
		return requestId;
	}

	public String getRequesterEmail() {
		return requesterEmail;
	}

	public DateTime getSentAt() {
		return sentAt;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("requestId", requestId)
				.add("requesterEmail", requesterEmail)
				.add("sentAt", sentAt)
				.toString();
	}
}
