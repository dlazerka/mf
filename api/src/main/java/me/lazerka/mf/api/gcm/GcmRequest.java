/*
 *     Copyright (C) 2017 Dzmitry Lazerka
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package me.lazerka.mf.api.gcm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static me.lazerka.mf.api.Util.checkNotNull;


/**
 * GCM message that is sent to GCM.
 *
 * See https://developer.android.com/google/gcm/server.html for specification.
 *
 * @author Dzmitry Lazerka
 */
@JsonInclude(Include.NON_DEFAULT)
public class GcmRequest {
	@JsonProperty("registration_ids")
	private List<String> registrationIds;

	@JsonProperty("collapse_key")
	private String collapseKey;

	@JsonProperty("data")
	private Data data;

	@JsonProperty("time_to_live")
	private int timeToLiveSeconds;

	@JsonProperty("restricted_package_name")
	private String restrictedPackageName;

	@JsonProperty("dry_run")
	private boolean dryRun;

	// For Jackson
	private GcmRequest() {}

	public GcmRequest(
			@Nonnull List<String> registrationIds,
			@Nullable String collapseKey,
			@Nonnull GcmPayload payload,
			int timeToLiveSeconds) {
		this.registrationIds = checkNotNull(registrationIds);
		this.collapseKey = collapseKey;
		this.data = new Data(payload);
		this.timeToLiveSeconds = timeToLiveSeconds;
	}

	public void setTimeToLiveSeconds(int timeToLiveSeconds) {
		this.timeToLiveSeconds = timeToLiveSeconds;
	}

	@Override
	public String toString() {
		return "GcmLocationRequest{" +
				"registrationIds.size()=" + (registrationIds == null ? "null" : registrationIds.size()) +
				", collapseKey='" + collapseKey + '\'' +
				", data=" + data +
				", timeToLiveSeconds=" + timeToLiveSeconds +
				", restrictedPackageName='" + restrictedPackageName + '\'' +
				", dryRun=" + dryRun +
				'}';
	}

	public static class Data {
		@JsonProperty(GcmPayload.TYPE_FIELD)
		String type;

		@JsonProperty(GcmPayload.PAYLOAD_FIELD)
		GcmPayload payload;

		public Data(@Nonnull GcmPayload payload) {
			this.type = checkNotNull(payload.getType());
			this.payload = checkNotNull(payload);
		}
	}
}
