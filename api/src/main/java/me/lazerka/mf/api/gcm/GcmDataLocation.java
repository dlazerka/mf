package me.lazerka.mf.api.gcm;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class GcmDataLocation {
	/**
	 * Who's asking for location.
	 * User's email as given by Google authentication service.
	 * Receiver smartphone must check if this user is a "friend".
	 */
	@JsonProperty
	String requesterEmail;

	/**
	 * When the message was received by server from the requester.
	 */
	@JsonProperty
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
