/*
 *     Copyright (C) 2017 Dzmitry Lazerka
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package me.lazerka.mf.gae.user;

import com.google.appengine.api.datastore.Email;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Work;
import me.lazerka.gae.jersey.oauth2.UserPrincipal;
import me.lazerka.gae.jersey.oauth2.google.GoogleUserPrincipal;
import me.lazerka.mf.api.EmailNormalized;
import me.lazerka.mf.gae.entity.CreateOrFetch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Manages MfUser entity in Datastore.
 *
 * @author Dzmitry Lazerka
 */
public class UserService {
	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	@Inject
	SecurityContext securityContext;

	private UserPrincipal getCurrentOauthUser() {
		return (UserPrincipal) checkNotNull(securityContext.getUserPrincipal());
	}

	@Nonnull
	public MfUser getCurrentUser() {
		UserPrincipal oauthUser = getCurrentOauthUser();
		String email = ((GoogleUserPrincipal) oauthUser).getEmail();
		EmailNormalized normalized = EmailNormalized.normalizeEmail(email);

		MfUser user = new MfUser(oauthUser.getId(), normalized);
		MfUser existingEntity = ofy().load().entity(user).now();

		if (existingEntity == null) {
			return create(user);
		}

		return existingEntity;
	}


	@Nonnull
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

		List<Email> normalizedList = new ArrayList<>(emails.size());
		for(String email : emails) {
			EmailNormalized normalized = EmailNormalized.normalizeEmail(email);
			Email value = new Email(normalized.getEmail());
			normalizedList.add(value);
		}

		// All emails must belong to only one user
		return ofy().load()
				.type(MfUser.class)
				.filter("email IN ", normalizedList)
				.first()
				.now();
	}
}
