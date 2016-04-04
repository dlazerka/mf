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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import me.lazerka.gae.jersey.oauth2.Role;
import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.gcm.GcmRegistrationResponse;
import me.lazerka.mf.api.object.GcmToken;
import me.lazerka.mf.gae.entity.GcmRegistrationEntity;
import me.lazerka.mf.gae.user.MfUser;
import me.lazerka.mf.gae.user.UserService;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Accepts user's GCM Registration IDs and saves them to DB, so that later we can send messages to the user.
 *
 * @author Dzmitry Lazerka
 */
@Path(GcmToken.PATH)
@Produces(ApiConstants.APPLICATION_JSON)
@RolesAllowed(Role.USER)
public class GcmRegistrationResource {
	private static final Logger logger = LoggerFactory.getLogger(GcmRegistrationResource.class);

	@Inject
	UserService userService;

	@Inject
	@Named("now")
	DateTime now;

	/**
	 * Adds the token to a collection of current user's tokens.
	 * User can have multiple tokens -- one per device.
	 *
	 * Instead of the collection, we could have a map of (device_id -> token) to avoid keeping outdated tokens
	 * for the same device, but we don't want to know device_id, so keep them all.
	 * Outdated ones will be cleared when we try to send a message to them.
	 */
	@POST
	@Consumes("application/json")
	public GcmRegistrationResponse save(GcmToken bean) {
		MfUser user = userService.getCurrentUser();
		logger.info("Saving GcmRegistrationEntity for {}", user.getEmail());

		GcmRegistrationEntity entity = new GcmRegistrationEntity(user, bean.getToken(), now, bean.getAppVersion());
		ofy().save().entity(entity).now();
		return new GcmRegistrationResponse();
	}

	/** Doesn't throw 404 in case of not existent. */
	@DELETE
	public void delete(GcmToken bean) {
		MfUser user = userService.getCurrentUser();
		logger.info("Deleting GcmRegistrationEntity for {}", user.getEmail());

		GcmRegistrationEntity entity = new GcmRegistrationEntity(user, bean.getToken(), now);
		ofy().delete().entity(entity).now();
	}
}
