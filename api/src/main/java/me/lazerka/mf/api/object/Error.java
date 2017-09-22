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

/**
 * @author Dzmitry Lazerka
 */
public class Error {
	public final String code;

	/**
	 * For unknown errors to show to user.
	 */
	public final String message;

	public Error(
		@JsonProperty String code,
		@JsonProperty String message
	) {
		this.code = code;
		this.message = message;
	}

	@Override
	public String toString() {
		return code + (message == null ? "" : ": " + message);
	}
}
