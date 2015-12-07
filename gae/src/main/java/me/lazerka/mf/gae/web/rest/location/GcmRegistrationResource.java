package me.lazerka.mf.gae.web.rest.location;

import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.gcm.GcmRegistrationResponse;
import me.lazerka.mf.api.object.GcmRegistration;
import me.lazerka.mf.gae.entity.GcmRegistrationEntity;
import me.lazerka.mf.gae.user.MfUser;
import me.lazerka.mf.gae.oauth.Role;
import me.lazerka.mf.gae.user.UserService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Accepts user's GCM Registration IDs and saves them to DB, so that later we can send messages to the user.
 *
 * @author Dzmitry Lazerka
 */
@Path(GcmRegistration.PATH)
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
	public GcmRegistrationResponse save(GcmRegistration bean) {
		MfUser user = userService.getCurrentUser();
		logger.info("Saving GcmRegistrationEntity by {}", user.getEmail());

		GcmRegistrationEntity entity = new GcmRegistrationEntity(user, bean.getId(), now, bean.getAppVersion());
		ofy().save().entity(entity).now();
		return new GcmRegistrationResponse();
	}

	/** Doesn't throw 404 in case of not existent. */
	@DELETE
	public void delete(GcmRegistration bean) {
		MfUser user = userService.getCurrentUser();
		logger.info("Deleting GcmRegistrationEntity for {}", user.getEmail());

		GcmRegistrationEntity entity = new GcmRegistrationEntity(user, bean.getId(), now);
		ofy().delete().entity(entity).now();
	}
}
