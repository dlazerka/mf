package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
		 * String representing the message when it was successfully processed.
		 */
		@JsonProperty("message_id")
		private final String messageId;

		/**
		 * String describing an error that occurred while processing the message for that recipient.
		 * The possible values are the same as documented in the above table,
		 * plus "Unavailable" (meaning GCM servers were busy and could not process the message for that particular
		 * recipient, so it could be retried).
		 */
		@JsonProperty("error")
		private final String error;

		public GcmResult(@Nonnull String messageId, @Nullable String error) {
			this.messageId = messageId;
			this.error = error;
		}

		public String getMessageId() {
			return messageId;
		}

		@Nullable
		public String getError() {
			return error;
		}
	}

}
