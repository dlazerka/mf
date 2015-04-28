package me.lazerka.mf.gae.web.rest;

import com.google.appengine.api.mail.MailService;
import com.google.appengine.api.mail.MailService.Attachment;
import com.google.appengine.api.mail.MailService.Message;
import com.google.appengine.api.users.UserService;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.googlecode.objectify.Objectify;
import me.lazerka.mf.api.object.AcraException;
import me.lazerka.mf.gae.entity.AcraExceptionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Dzmitry Lazerka
 */
@Path(AcraException.PATH + "/{reportId}")
public class AcraExceptionResource {
	private static final Logger logger = LoggerFactory.getLogger(AcraExceptionResource.class);

	@Inject
	MailService mailService;

	@Inject
	UserService userService;

	@Inject
	Objectify ofy;

	// Expected response will be logged, see org.acra.util.HttpRequest
	@PUT
	@Produces("text/plain")
	public String post(MultivaluedMap<String, String> map, @PathParam("reportId") String reportId) throws IOException {

		String exceptionMessage = getExceptionMessage(map);
		logger.info("Received {} {}", exceptionMessage, reportId);
		String report = format(map);

		// Save to Datastore.
		AcraExceptionEntity entity = new AcraExceptionEntity(reportId, exceptionMessage, report);
		ofy.save().entity(entity);

		// Send email;
		Message message = new Message();
		message.setSender(userService.isUserLoggedIn()
				? userService.getCurrentUser().getEmail()
				: "dlazerka@gmail.com");
		message.setTo("dlazerka@gmail.com");
		message.setSubject("Acra: " + exceptionMessage);
		message.setTextBody(getEmailBody(map));
		Attachment attachment = new Attachment(reportId + ".txt", report.getBytes(Charsets.UTF_8));
		message.setAttachments(attachment);
		mailService.send(message);

		return "ok";
	}

	private String format(MultivaluedMap<String, String> report) {
		Set<String> keySet = report.keySet();
		List<String> keys = new ArrayList<>(keySet);
		Collections.sort(keys);

		List<String> items = new ArrayList<>(keys.size());
		for (String key : keys) {
			items.add(format(report, key));
		}
		return Joiner.on("\n").join(items);
	}

	@Nullable
	private String getExceptionMessage(MultivaluedMap<String, String> report) {
		String stackTrace = report.getFirst("STACK_TRACE");
		String result = null;
		if (stackTrace != null) {
			int i = stackTrace.indexOf('\n');
			// Take first line.
			result = stackTrace.substring(0, i != -1 ? i : (stackTrace.length() - 1));
		}
		return result;
	}

	private String getEmailBody(MultivaluedMap<String, String> report) {
		List<String> lines = new ArrayList<>();
		lines.add(format(report, "ANDROID_VERSION"));
		lines.add(format(report, "APP_VERSION_CODE"));
		lines.add(format(report, "PHONE_MODEL"));
		lines.add(format(report, "USER_CRASH_DATE"));
		lines.add("");
		lines.add(format(report, "STACK_TRACE"));
		return Joiner.on("\n").join(lines);
	}

	/**
	 * If one value, just `Key: Value`, if many, then format fancier:
	 *
	 * <pre>
	 * Key:
	 *  Value1
	 *  Value2
	 * </pre>
	 */
	private String format(MultivaluedMap<String, String> map, String key) {
		List<String> values = map.get(key);
		String value;
		if (values == null || values.isEmpty()) {
			value = null;
		} else if (values.size() == 1) {
			value = values.get(0);
		} else {
			StringBuilder sb = new StringBuilder();
			for (String s : values) {
				sb.append("\n\t").append(s);
			}
			value = sb.toString();
		}
		return key + ": " + value;
	}
}
