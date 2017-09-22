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

package com.baraded.mf.logging

/**
 * Logs messages/errors to LogCat and FirebaseCrash.
 */
object LogService {

	@JvmStatic fun getLogger(clazz: Class<*>) = getLogger(clazz.simpleName)

	@JvmStatic fun getLogger(name: String): Logger {
		if (name.length <= 23) {
			return Logger(name)
		} else {
			// throw IllegalArgumentException("Log tag $name exceeds limit of 23 characters");
			return Logger(name.substring(0, 23))
		}
	}
}
