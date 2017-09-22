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
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class UserInfo {
	/** Normalized on server side. May differ from user's contacts. */
	@JsonProperty
	private String normalizedEmail;

	// For Jackson.
	private UserInfo() {}

	public UserInfo(
			@Nonnull String normalizedEmail
	) {
		this.normalizedEmail = checkNotNull(normalizedEmail);
	}

	@Nullable
	public String getNormalizedEmail() {
		return normalizedEmail;
	}

	@Override
	public String toString() {
		return normalizedEmail;
	}
}
