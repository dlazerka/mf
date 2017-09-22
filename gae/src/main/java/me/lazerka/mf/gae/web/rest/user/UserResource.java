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

package me.lazerka.mf.gae.web.rest.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import me.lazerka.gae.jersey.oauth2.Role;
import me.lazerka.mf.api.ApiConstants;
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
