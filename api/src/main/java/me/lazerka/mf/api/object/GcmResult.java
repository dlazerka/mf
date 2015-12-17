package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * What GCM service responded to our messages, but without any tokens.
 */
public class GcmResult {
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

	// For Jackson.
	private GcmResult() {}

	public GcmResult(@Nonnull String messageId, @Nullable String error) {
		this.messageId = messageId;
		this.error = error;
	}

	@Nullable
	public String getMessageId() {
		return messageId;
	}

	@Nullable
	public String getError() {
		return error;
	}

	public boolean isSuccessful() {
		return error == null;
	}
}
