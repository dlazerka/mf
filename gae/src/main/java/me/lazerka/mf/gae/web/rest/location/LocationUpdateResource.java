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

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import me.lazerka.gae.jersey.oauth2.Role;
import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.object.GcmResult;
import me.lazerka.mf.api.object.Location;
import me.lazerka.mf.api.object.LocationRequest;
import me.lazerka.mf.api.object.LocationUpdate;
import me.lazerka.mf.api.object.LocationUpdateResponse;
import me.lazerka.mf.gae.gcm.GcmService;
import me.lazerka.mf.gae.user.MfUser;
import me.lazerka.mf.gae.user.UserService;

import static me.lazerka.mf.gae.web.rest.JerseyUtil.throwIfNull;

/**
 * @author Dzmitry Lazerka
 */
@Path(LocationUpdate.PATH)
@Produces(ApiConstants.APPLICATION_JSON)
@RolesAllowed(Role.USER)
public class LocationUpdateResource {
	private static final Logger logger = LoggerFactory.getLogger(LocationUpdateResource.class);

	@Inject
	UserService userService;

	@Inject
	GcmService gcmService;

	@Inject
	DateTime now;

	@POST
	@Consumes("application/json")
	public LocationUpdateResponse post(LocationUpdate locationUpdate) {
		logger.trace(locationUpdate.toString());
		LocationRequest sourceRequest = throwIfNull(locationUpdate.getLocationRequest(), LocationUpdate.LOCATION_REQUEST);
		String requesterEmail = throwIfNull(sourceRequest.getRequesterEmail(), "requesterEmail");
		String requestId = throwIfNull(sourceRequest.getRequestId(), "requestId");
		Location location = throwIfNull(locationUpdate.getLocation(), LocationUpdate.LOCATION);

		logger.info("post {} for {}", requestId, requesterEmail);

		MfUser user = userService.getCurrentUser();

		// We don't trust client to set this, obviously.
		location.setEmail(user.getEmail().getEmail());

		DateTime clientsWhen = location.getWhen();
		if (new Duration(now, clientsWhen).isLongerThan(Duration.standardMinutes(1))) {
			logger.warn(
					"Client's location 'time' deviates from real one by more than a minute, overwriting: {}, {}",
					now,
					clientsWhen);
			location.setWhen(now);
		}

		MfUser requester = userService.getUserByEmail(requesterEmail);

		if (requester == null) {
			logger.warn("No user found by emails: {}", requesterEmail);
			throw new WebApplicationException(Response.status(404).entity("User not found").build());
		}

		LocationUpdate payload = new LocationUpdate(location, locationUpdate.getLocationRequest());

		List<GcmResult> gcmResults = gcmService.send(requester, payload);

		for(GcmResult gcmResult : gcmResults) {
			if (gcmResult.getError() != null) {
				logger.warn(
						"GCM error while sending to {} for {}: {}",
						requester.getEmail(),
						location.getEmail(),
						gcmResult.getError());
			}
		}

		return new LocationUpdateResponse(locationUpdate, gcmResults);
	}
}
