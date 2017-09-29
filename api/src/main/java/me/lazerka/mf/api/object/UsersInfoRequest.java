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

import javax.annotation.Nullable;
import java.util.Set;

import static me.lazerka.mf.api.Util.checkNotNull;

/**
 * To know if the users have ever installed the app.
 *
 * @author Dzmitry Lazerka
 */
public class UsersInfoRequest implements ApiObject {
	public static final String PATH = "/rest/user/friends";

	@JsonProperty
	private Set<String> emails;

	// For Jackson.
	private UsersInfoRequest() {}

	public UsersInfoRequest(Set<String> emails) {
		this.emails = checkNotNull(emails);
	}

	@Override
	public String getPath() {
		return PATH;
	}

	@Nullable
	public Set<String> getEmails() {
		return emails;
	}

	@Override
	public String toString() {
		return "UsersInfoRequest{" +
				"emails=" + emails +
				'}';
	}
}
