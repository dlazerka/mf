package me.lazerka.mf.gae.gcm;

import com.google.common.collect.ImmutableList;
import me.lazerka.mf.gae.entity.MfUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Dzmitry Lazerka
 */
public class MfUserService {
	private static final Logger logger = LoggerFactory.getLogger(MfUserService.class);

	public MfUser getUserByEmail(@Nonnull String email) {
		return getUserByEmails(ImmutableList.of(email));
	}

	public MfUser getUserByEmails(List<String> emails) {
		logger.trace("Requesting user by emails {}", emails);

		// All emails must belong to only one user
		MfUser otherUser = ofy().load()
				.type(MfUser.class)
				.filter("email IN ", emails)
				.first()
				.now();
		if (otherUser == null) {
			logger.warn("No user found by emails: {}", emails);
			throw new WebApplicationException(Response.status(404).entity("User not found").build());
		}
		return otherUser;
	}
}
