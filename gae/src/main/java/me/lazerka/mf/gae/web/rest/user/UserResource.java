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

package me.lazerka.mf.gae.web.rest.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.gae.oauth.Role;
import me.lazerka.mf.gae.user.UserService;

/**
 * @author Dzmitry Lazerka
 */
@Path("/rest/user")
@Produces(ApiConstants.APPLICATION_JSON)
@RolesAllowed(Role.USER)
public class UserResource {
	private static final Logger logger = LoggerFactory.getLogger(UserResource.class);

	@Inject
	UserService userService;

	@GET
	@Path("/me")
	public CurrentUserBean me() {
		return new CurrentUserBean(userService.getCurrentUser());
	}
}
