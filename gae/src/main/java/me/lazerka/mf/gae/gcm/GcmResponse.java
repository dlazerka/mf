/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2015 Dzmitry Lazerka dlazerka@gmail.com
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package me.lazerka.mf.gae.gcm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * GCM message that is sent to GCM.
 *
 * See https://developer.android.com/google/gcm/http.html for specification.
 *
 * @author Dzmitry Lazerka
 */
public class GcmResponse {
	/**
	 * Unique ID (number) identifying the multicast message.
	 */
	@JsonProperty(value = "multicast_id", required = true)
	private long multicastId;

	/**
	 * Number of messages that were processed without an error.
	 */
	@JsonProperty(value = "success", required = true)
	private int success;

	/**
	 * Number of messages that could not be processed.
	 */
	@JsonProperty(value = "failure", required = true)
	private int failure;

	/**
	 * Number of results that contain a canonical registration ID. See Advanced Topics for more discussion of this topic.
	 */
	@JsonProperty(value = "canonical_ids", required = true)
	private int canonicalIds;

	/**
	 * Array of objects representing the status of the messages processed. The objects are listed in the same order
	 * as the request (i.e., for each registration ID in the request, its result is listed in the same index in the
	 * response).
	 */
	@JsonProperty(value = "results", required = true)
	private List<Result> results;

	public long getMulticastId() {
		return multicastId;
	}

	public int getSuccess() {
		return success;
	}

	public int getFailure() {
		return failure;
	}

	public int getCanonicalIds() {
		return canonicalIds;
	}

	public List<Result> getResults() {
		return results;
	}

	@Override
	public String toString() {
		return "GcmResponse{" +
				"multicastId=" + multicastId +
				", success=" + success +
				", failure=" + failure +
				", canonicalIds=" + canonicalIds +
				", results=" + results +
				'}';
	}

	public static class Result {
		/**
		 * String representing the message when it was successfully processed.
		 */
		@JsonProperty("message_id")
		String messageId;

		/**
		 * If set, means that GCM processed the message but it has another canonical registration ID for that device,
		 * so sender should replace the IDs on future requests (otherwise they might be rejected).
		 * This field is never set if there is an error in the request.
		 */
		String registrationId;

		/**
		 * String describing an error that occurred while processing the message for that recipient.
		 * The possible values are the same as documented in the above table,
		 * plus "Unavailable" (meaning GCM servers were busy and could not process the message for that particular
		 * recipient, so it could be retried).
		 */
		@JsonProperty("error")
		String error;

		public String getMessageId() {
			return messageId;
		}

		@JsonIgnore // Don't allow to send registration ID, only accept.
		public String getRegistrationId() {
			return registrationId;
		}

		@JsonProperty("registration_id")
		public void setRegistrationId(String registrationId) {
			this.registrationId = registrationId;
		}

		public String getError() {
			return error;
		}

		@Override
		public String toString() {
			return "Result{" +
					"messageId='" + messageId + '\'' +
					", registrationId.notNull='" + (registrationId != null) + '\'' +
					", error='" + error + '\'' +
					'}';

		}
	}
}
