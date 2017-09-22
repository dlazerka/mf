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

import javax.annotation.Nonnull;

/**
 * @author Dzmitry Lazerka
 */
public class GcmToken implements ApiObject {
	public static final String PATH = "/rest/gcm/token";

	/**
	 * Max length: 4k (max length of a cookie).
	 */
	@JsonProperty
	private String token;

	/** As specified in AndroidManifest.xml */
	@JsonProperty
	private int appVersion;

	@Override
	public String getPath() {
		return PATH;
	}

	// For Jackson.
	private GcmToken() {
	}

	public GcmToken(@Nonnull String token, int appVersion) {
		this.token = token;
		this.appVersion = appVersion;
	}

	public String getToken() {
		return token;
	}

	public int getAppVersion() {
		return appVersion;
	}

	@Override
	public String toString() {
		return "GcmToken{" +
				"id=<removed>" +
				", appVersion='" + appVersion + '\'' +
				'}';

	}
}
