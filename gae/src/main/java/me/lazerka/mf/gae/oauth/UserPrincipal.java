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

package me.lazerka.mf.gae.oauth;

import javax.annotation.Nonnull;
import java.security.Principal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class UserPrincipal implements Principal {
	private final String id;
	private final String email;

	public UserPrincipal(@Nonnull String id, @Nonnull String email) {
		this.id = checkNotNull(id);
		this.email = checkNotNull(email);
		checkArgument(email.indexOf('@') != -1, "Wrong order of arguments");
	}

	@Nonnull
	@Override
	public String getName() {
		return id;
	}

	@Nonnull
	public String getId() {
		return id;
	}

	@Nonnull
	public String getEmail() {
		return email;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		UserPrincipal that = (UserPrincipal) o;

		return id.equals(that.id);

	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return email + ' ' + id;
	}
}
