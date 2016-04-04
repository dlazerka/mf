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

package me.lazerka.mf.gae.web.rest.location;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import me.lazerka.gae.jersey.oauth2.Role;
import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.object.GcmResult;
import me.lazerka.mf.api.object.LocationRequest;
import me.lazerka.mf.api.object.LocationRequestResult;
import me.lazerka.mf.gae.gcm.GcmService;
import me.lazerka.mf.gae.user.MfUser;
import me.lazerka.mf.gae.user.UserService;

import static me.lazerka.mf.gae.web.rest.JerseyUtil.throwIfNull;

/**
 * @author Dzmitry Lazerka
 */
@Path(LocationRequest.PATH)
@Produces(ApiConstants.APPLICATION_JSON)
@RolesAllowed(Role.USER)
public class LocationRequestResource {
	private static final Logger logger = LoggerFactory.getLogger(LocationRequestResource.class);

	@Inject
	@Named("now")
	DateTime now;

	@Inject
	UserService userService;

	@Inject
	GcmService gcmService;

	/**
	 * Finds a user by given emails (must belong to only one user),
	 * then sends GCM message to all the registrations by this user (devices where he/she installed the app).
	 * Later, the other user will send its location to server, and we will send them back to requester using GCM.
	 *
	 * All given emails must belong to a single user, registered in the app.
	 *
	 * @return The GcmResult received from GCM, but with removed registration IDs. User should not know
	 *         his friend's registration ids.
	 */
	@POST
	@Consumes("application/json")
	public LocationRequestResult byEmail(LocationRequest locationRequest) {
		Set<String> forEmails = throwIfNull(locationRequest.getEmails(), "emails");
		logger.trace("byEmail for {}", forEmails);

		MfUser sender = userService.getCurrentUser();
		MfUser recipient = userService.getUserByEmails(forEmails);

		if (recipient == null) {
			logger.warn("No user found by emails: {}", forEmails);
			throw new WebApplicationException(Response.status(404).entity("User not found").build());
		}

		Duration duration = locationRequest.getDuration() == null
				? Duration.standardMinutes(15)
				: locationRequest.getDuration();
		LocationRequest cleanCopy = new LocationRequest(
				locationRequest.getRequestId(),
				sender.getEmail().getEmail(), // requesterEmail
				now,
				duration
		);

		List<GcmResult> gcmResults = gcmService.send(recipient, cleanCopy);

		LocationRequestResult result = new LocationRequestResult(
				recipient.getEmail().getEmail(),
				gcmResults
		);
		return result;
	}
}
