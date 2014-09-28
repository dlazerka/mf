package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Server's response to accepted {@link LocationRequest}.
 *
 * @author Dzmitry Lazerka
 */
public class LocationRequestResult {
	/**
	 * User's email where the request was sent.
	 */
	@JsonProperty
	private String email;

	@JsonProperty
	private List<GcmResult> results;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public List<GcmResult> getResults() {
		return results;
	}

	public void setResults(List<GcmResult> results) {
		this.results = results;
	}

	public static class GcmResult {
		/**
		 * SHA-256 hash of all friends GCM Registration IDs.
		 * User should not know others registration IDs, especially if they aren't friends.
		 */
		@JsonProperty
		private String deviceRegistrationHash;

		/**
		 * String representing the message when it was successfully processed.
		 */
		@JsonProperty("message_id")
		private String messageId;

		/**
		 * String describing an error that occurred while processing the message for that recipient.
		 * The possible values are the same as documented in the above table,
		 * plus "Unavailable" (meaning GCM servers were busy and could not process the message for that particular
		 * recipient, so it could be retried).
		 */
		@JsonProperty("error")
		private String error;

		public String getDeviceRegistrationHash() {
			return deviceRegistrationHash;
		}

		public void setDeviceRegistrationHash(String deviceRegistrationHash) {
			this.deviceRegistrationHash = deviceRegistrationHash;
		}

		public String getMessageId() {
			return messageId;
		}

		public void setMessageId(String messageId) {
			this.messageId = messageId;
		}

		public String getError() {
			return error;
		}

		public void setError(String error) {
			this.error = error;
		}
	}

}
