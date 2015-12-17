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

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class GaeSecurityContext implements SecurityContext {
	private final UserPrincipal user;
	private final boolean secure;
	private final Set<String> roles;

	public GaeSecurityContext(UserPrincipal user, boolean secure, Set<String> role) {
		this.user = checkNotNull(user);
		this.secure = secure;
		this.roles = checkNotNull(role);
	}

	@Override
	public Principal getUserPrincipal() {
		return user;
	}

	@Override
	public boolean isUserInRole(String role) {
		return roles.contains(role);
	}

	@Override
	public boolean isSecure() {
		return secure;
	}

	@Override
	public String getAuthenticationScheme() {
		return "GAE";
	}
}
