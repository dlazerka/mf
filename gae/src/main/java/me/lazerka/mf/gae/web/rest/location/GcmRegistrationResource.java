package me.lazerka.mf.gae.web.rest.location;

import com.googlecode.objectify.Work;
import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.gcm.GcmRegistrationResponse;
import me.lazerka.mf.gae.entity.GcmRegistrationEntity;
import me.lazerka.mf.gae.entity.MfUser;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Accepts user's GCM Registration IDs and saves them to DB, so that later we can send messages to the user.
 *
 * @author Dzmitry Lazerka
 */
@Path(me.lazerka.mf.api.object.GcmRegistration.PATH)
@Produces(ApiConstants.APPLICATION_JSON)
public class GcmRegistrationResource {
	private static final Logger logger = LoggerFactory.getLogger(GcmRegistrationResource.class);

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
	public GcmRegistrationResponse save(me.lazerka.mf.api.object.GcmRegistration bean) {
		logger.info("Saving GcmRegistrationEntity by {}", user.getEmail());

		final GcmRegistrationEntity entity = new GcmRegistrationEntity(bean.getId(), now);
		entity.setAppVersion(bean.getAppVersion());

		user = ofy().transactNew(10, new Work<MfUser>() {
			@Override
			public MfUser run() {
				user = ofy().load().entity(user).safe();
				List<GcmRegistrationEntity> list = user.getGcmRegistrationEntities();

				if (!list.contains(entity)) {
					logger.trace("Adding GcmRegistrationEntity to list of user's registrations.");
					list.add(entity);
				} else {
					logger.trace("GcmRegistrationEntity was already in the list of user's registrations.");
				}

				ofy().save().entity(user);
				return user;
			}
		});

		return new GcmRegistrationResponse();
	}

	/** Doesn't throw 404 in case of not existent. */
	@DELETE
	public void delete(me.lazerka.mf.api.object.GcmRegistration object) {
		logger.info("Deleting GcmRegistrationEntity by {}", user.getEmail());
		final GcmRegistrationEntity entity = new GcmRegistrationEntity(object.getId(), now);

		user = ofy().transactNew(10, new Work<MfUser>() {
			@Override
			public MfUser run() {
				user = ofy().load().entity(user).safe();
				List<GcmRegistrationEntity> list = user.getGcmRegistrationEntities();
				int i = list.indexOf(entity);
				if (i != -1) {
					logger.trace("Removing GcmRegistrationEntity from list of user's registrations.");
					list.remove(entity);
				} else {
					logger.trace("GcmRegistrationEntity was already removed from the list of user's registrations.");
				}

				ofy().save().entity(user);
				return user;
			}
		});
	}
}
