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
