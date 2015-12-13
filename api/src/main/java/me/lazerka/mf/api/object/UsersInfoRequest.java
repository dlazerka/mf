package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import javax.annotation.Nullable;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * To know if the users have ever installed the app.
 *
 * @author Dzmitry Lazerka
 */
public class UsersInfoRequest implements ApiObject {
	public static final String PATH = "/rest/user/friends";

	@JsonProperty
	private Set<String> emails;

	// For Jackson.
	private UsersInfoRequest() {}

	public UsersInfoRequest(Set<String> emails) {
		this.emails = checkNotNull(emails);
	}

	@Override
	public String getPath() {
		return PATH;
	}

	@Nullable
	public Set<String> getEmails() {
		return emails;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("emails", emails)
				.toString();
	}
}
