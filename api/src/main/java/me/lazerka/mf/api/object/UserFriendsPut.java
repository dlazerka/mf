package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * To know if the users have ever installed the app.
 *
 * @author Dzmitry Lazerka
 */
public class UserFriendsPut {
	public static final String PATH = "/rest/user/friends";

	@JsonProperty
	private Set<String> emails;

	// For Jackson.
	private UserFriendsPut() {}

	public UserFriendsPut(Set<String> emails) {
		this.emails = emails;
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
