package me.lazerka.mf.gae.web.rest.location;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.impl.Keys;
import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.gcm.GcmRegistrationResponse;
import me.lazerka.mf.api.object.GcmRegistration;
import me.lazerka.mf.gae.entity.GcmRegistrationEntity;
import me.lazerka.mf.gae.entity.MfUser;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import java.util.List;

/**
 * Accepts user's GCM Registration IDs and saves them to DB, so that later we can send messages to the user.
 *
 * @author Dzmitry Lazerka
 */
@Path(GcmRegistration.PATH)
@Produces(ApiConstants.APPLICATION_JSON)
public class GcmRegistrationResource {
	private static final Logger logger = LoggerFactory.getLogger(GcmRegistrationResource.class);

	@Inject
	Objectify ofy;

	@Inject
	Keys keys;

	@Inject
	MfUser user;

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
		logger.trace("Saving GcmRegistration by {}", user.getEmail());

		final GcmRegistrationEntity entity = new GcmRegistrationEntity(user, bean.getToken(), now);
		entity.setAppVersion(bean.getAppVersion());

		user = ofy.transactNew(10, new Work<MfUser>() {
			@Override
			public MfUser run() {
				user = ofy.load().entity(user).safe();
				List<GcmRegistrationEntity> list = user.getGcmRegistrations();
				int i = list.indexOf(entity);
				if (i == -1) {
					logger.trace("Adding GcmRegistration to list of user's registrations.");
					list.add(entity);
				} else {
					logger.trace("GcmRegistration was already in the list of user's registrations.");
				}

				ofy.save().entity(user);
				ofy.save().entity(entity);
				return user;
			}
		});

		GcmRegistrationResponse response = new GcmRegistrationResponse();
		response.setId(entity.getId());
		return response;
	}

	/** Doesn't throw 404 in case of not existent. */
	@DELETE
	public void delete(GcmRegistration object) {
		logger.info("Deleting GcmRegistration by {}", user.getEmail());
		final GcmRegistrationEntity entity = new GcmRegistrationEntity(user, object.getToken(), now);

		user = ofy.transactNew(10, new Work<MfUser>() {
			@Override
			public MfUser run() {
				user = ofy.load().entity(user).safe();
				List<GcmRegistrationEntity> list = user.getGcmRegistrations();
				int i = list.indexOf(entity);
				if (i != -1) {
					logger.trace("Removing GcmRegistration from list of user's registrations.");
					list.remove(entity);
				} else {
					logger.trace("GcmRegistration was already removed from the list of user's registrations.");
				}

				ofy.save().entity(user);
				ofy.delete().entity(entity);
				return user;
			}
		});
	}
}
