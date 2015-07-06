package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class UserInfo {
	/** Canonicalized on server side. May differ from user's contacts. */
	@JsonProperty
	private String canonicalEmail;

	/** As requested by user, not canonicalized. */
	@JsonProperty
	private Set<String> emails;

	// For Jackson.
	private UserInfo() {}

	public UserInfo(
			@Nonnull String canonicalEmail,
			@Nonnull Set<String> emails
	) {
		this.canonicalEmail = checkNotNull(canonicalEmail);
		this.emails = checkNotNull(emails);
	}

	@Nullable
	public String getCanonicalEmail() {
		return canonicalEmail;
	}

	@Nonnull
	public Set<String> getEmails() {
		return emails == null ? Collections.<String>emptySet() : emails;
	}

	@Override
	public String toString() {
		return canonicalEmail + ": " + emails;
	}
}
