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

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Used to identify app users.
 *
 * Currently only email (provided by Google), but later can be added FB and phone number as well.
 *
 * @author Dzmitry Lazerka
 */
public class UserFindId {
	@JsonProperty
	Set<String> emails;

	// For Jackson
	private UserFindId() {}

	public UserFindId(Set<String> emails) {
		this.emails = checkNotNull(emails);
		checkArgument(!emails.isEmpty());
	}

	public Set<String> getEmails() {
		return emails;
	}

	@Override
	public String toString() {
		return String.valueOf(emails);
	}
}
