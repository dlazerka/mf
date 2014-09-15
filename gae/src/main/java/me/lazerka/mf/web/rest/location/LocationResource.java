package me.lazerka.mf.web.rest.location;

import com.google.common.base.Splitter;
import com.googlecode.objectify.Objectify;
import me.lazerka.mf.api.Location;
import me.lazerka.mf.entity.MfUser;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.List;

/**
 * @author Dzmitry Lazerka
 */
@Path(Location.PATH)
public class LocationResource {
	private static final Logger logger = LoggerFactory.getLogger(LocationResource.class);

	@Inject
	Objectify ofy;

	@Inject
	MfUser user;

	@GET
	@Path("/{commaSeparatedEmails}")
	@Produces("application/json")
	public Location byEmail(@PathParam("commaSeparatedEmails") String commaSeparatedEmails) {
		logger.trace("byEmail for {}", commaSeparatedEmails);

		List<String> emails = Splitter.on(',').splitToList(commaSeparatedEmails);

		// TODO: dummy, read real location from GCM.
		Location location = new Location();
		location.setLat(37.783333);
		location.setLon(-122.416667);
		location.setAcc(30);
		location.setEmail(emails.get(0));
		location.setWhen(DateTime.now());

		return location;
	}
}
