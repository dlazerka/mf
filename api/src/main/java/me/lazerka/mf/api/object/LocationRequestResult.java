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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Server's response to accepted {@link LocationRequest}.
 *
 * @author Dzmitry Lazerka
 */
@JsonInclude(Include.NON_NULL)
public class LocationRequestResult {
	public static final String ERROR_USER_NOT_FOUND = "user_not_found";

	/**
	 * User's email where the request was sent.
	 */
	@JsonProperty
	private String email;

	@JsonProperty
	private List<GcmResult> gcmResults;

	@JsonProperty
	private Error error;

	// For Jackson.
	private LocationRequestResult() {}

	public LocationRequestResult(String email, List<GcmResult> gcmResults) {
		this.email = email;
		this.gcmResults = gcmResults;
	}

	@Nullable
	public String getEmail() {
		return email;
	}

	@Nullable
	public List<GcmResult> getGcmResults() {
		return gcmResults;
	}

	public Error getError() {
		return error;
	}

	public boolean isSuccess() {
		return error == null;
	}

	public static LocationRequestResult notFound() {
		LocationRequestResult result = new LocationRequestResult();
		result.error = new Error(ERROR_USER_NOT_FOUND, null);
		return result;
	}
}
