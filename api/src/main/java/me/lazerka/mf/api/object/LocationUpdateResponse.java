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

package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * Server's response to accepted {@link LocationUpdate}.
 *
 * @author Dzmitry Lazerka
 */
public class LocationUpdateResponse {
	@JsonProperty
	private LocationUpdate locationUpdate;

	@JsonProperty
	private List<GcmResult> gcmResults;

	// For Jackson.
	private LocationUpdateResponse() {}

	public LocationUpdateResponse(LocationUpdate locationUpdate, List<GcmResult> gcmResults) {

		this.locationUpdate = locationUpdate;
		this.gcmResults = gcmResults;
	}

	public LocationUpdate getLocationUpdate() {
		return locationUpdate;
	}

	public List<GcmResult> getGcmResults() {
		return gcmResults;
	}

	/**
	 * @return whethere there's at least one successful result.
	 */
	public boolean hasSuccess() {
		if (gcmResults == null) {
			return false;
		}

		for(GcmResult gcmResult : gcmResults) {
			if (gcmResult.isSuccessful()) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
		                  .add("locationUpdate", locationUpdate)
		                  .add("gcmResults", gcmResults)
		                  .toString();
	}
}
