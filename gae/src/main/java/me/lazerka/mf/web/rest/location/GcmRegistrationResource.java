package me.lazerka.mf.web.rest.location;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.impl.Keys;
import me.lazerka.mf.api.GcmRegistration;
import me.lazerka.mf.entity.GcmRegistrationEntity;
import me.lazerka.mf.entity.MfUser;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * @author Dzmitry Lazerka
 */
@Path(GcmRegistration.PATH)
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

	@POST
	@Consumes("application/json")
	public void save(GcmRegistration object) {
		logger.trace("Saving GcmRegistration by {}", user.getEmail());

		GcmRegistrationEntity entity = new GcmRegistrationEntity(user, object.getToken(), now);
		entity.setAppVersion(object.getAppVersion());

		ofy.save().entity(entity).now();
	}

	/** Doesn't throw 404 in case of not existent. */
	@DELETE
	public void delete(GcmRegistration object) {
		logger.info("Deleting GcmRegistration by {}", user.getEmail());
		GcmRegistrationEntity entity = new GcmRegistrationEntity(user, object.getToken(), now);
		ofy.delete().entity(entity).now();
	}
}
