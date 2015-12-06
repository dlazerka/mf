package me.lazerka.mf.gae.user;

import com.google.appengine.api.datastore.Email;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Work;
import me.lazerka.mf.gae.entity.CreateOrFetch;
import me.lazerka.mf.gae.oauth.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Manages MfUser entity in Datastore.
 *
 * @author Dzmitry Lazerka
 */
public class UserService {
	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	/** Any period that is followed by @. */
	private static final Pattern emailAddressSplitPattern = Pattern.compile("^(.*)(@.*)$");
	/** Just a single period. */
	private static final Pattern periodRegex = Pattern.compile(".", Pattern.LITERAL);

	@Inject
	SecurityContext securityContext;

	/**
	 * Try to normalize email addresses by lowercasing domain part.
	 *
	 * If we detect address is GMail one, we also apply GMail specific features normalizer.
	 *
	 * If we cannot parse email, we log a warning and return non-normalized email instead of throwing an exception,
	 * because email addresses could be very tricky to parse, and there's no silver bullet despite RFCs.
	 */
	public static EmailNormalized normalizeEmail(String address){
		Matcher matcher = emailAddressSplitPattern.matcher(address);
		if (!matcher.matches()) {
			logger.warn("Email address invalid: {}", address);
			return new EmailNormalized(address);
		}

		String localPart = matcher.group(1);
		String domainPart = matcher.group(2);

		domainPart = domainPart.toLowerCase(Locale.US);

		if (domainPart.equals("@gmail.com") || domainPart.equals("@googlemail.com")) {
			// Remove everything after plus sign (GMail-specific feature).
			int plusIndex = localPart.indexOf('+');
			if (plusIndex != -1) {
				localPart = localPart.substring(0, plusIndex);
			}

			// Remove periods.
			localPart = periodRegex.matcher(localPart).replaceAll("");

			// GMail addresses are case-insensitive.
			localPart = localPart.toLowerCase(Locale.US);
		}

		return new EmailNormalized(localPart + domainPart);
	}

	private UserPrincipal getCurrentOauthUser() {
		return (UserPrincipal) checkNotNull(securityContext.getUserPrincipal());
	}

	public MfUser getCurrentUser() {
		UserPrincipal oauthUser = getCurrentOauthUser();
		EmailNormalized normalized = normalizeEmail(oauthUser.getEmail());

		MfUser user = new MfUser(oauthUser.getId(), normalized);
		MfUser existingEntity = ofy().load().entity(user).now();

		if (existingEntity == null) {
			return create(user);
		}

		return existingEntity;
	}


	public MfUser create(final MfUser newEntity) {
		Work<MfUser> createWork = new CreateOrFetch<>(newEntity);
		return ofy().transact(createWork);
	}

	@Nullable
	public MfUser getUserByEmail(@Nonnull String email) {
		return getUserByEmails(ImmutableList.of(email));
	}

	/**
	 * Fetchs a single user by any of given emails.
	 */
	@Nullable
	public MfUser getUserByEmails(Collection<String> emails) {
		logger.trace("Requesting user by emails {}", emails);

		List<Email> normalized = new ArrayList<>(emails.size());
		for(String email : emails) {
			Email value = new Email(normalizeEmail(email).getEmail());
			normalized.add(value);
		}

		// All emails must belong to only one user
		return ofy().load()
				.type(MfUser.class)
				.filter("email IN ", normalized)
				.first()
				.now();
	}

	public Set<MfUser> getUsersByEmails(Set<EmailNormalized> emails) {
		logger.trace("Requesting users by emails {}", emails);

		List<MfUser> users = ofy().load()
				.type(MfUser.class)
				.filter("email IN ", emails)
				.list();
		if (users.isEmpty()) {
			logger.warn("No users found by emails: {}", emails);
		}
		logger.trace("Found {} users", users);
		return new LinkedHashSet<>(users);
	}

}