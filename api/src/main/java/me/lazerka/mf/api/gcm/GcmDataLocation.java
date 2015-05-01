package me.lazerka.mf.api.gcm;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

/**
 * The `data` field of a request for location that comes to a friend's device.
 *
 * @author Dzmitry Lazerka
 */
public class GcmDataLocation {
	public static final String SENT_AT = "sentAt";
	public static final String REQUESTER_EMAIL = "requesterEmail";

	/**
	 * Who's asking for location.
	 * User's email as given by Google authentication service.
	 * Receiver smartphone must check if this user is a "friend".
	 */
	@JsonProperty(REQUESTER_EMAIL)
	String requesterEmail;

	/**
	 * When the message was received by server from the requester.
	 */
	@JsonProperty(SENT_AT)
	DateTime sentAt;

	public String getRequesterEmail() {
		return requesterEmail;
	}

	public void setRequesterEmail(String requesterEmail) {
		this.requesterEmail = requesterEmail;
	}

	public DateTime getSentAt() {
		return sentAt;
	}

	public void setSentAt(DateTime sentAt) {
		this.sentAt = sentAt;
	}

	@Override
	public String toString() {
		return "Data{" +
				"requesterEmail='" + requesterEmail + '\'' +
				", sentAt=" + sentAt +
				'}';
	}
}
