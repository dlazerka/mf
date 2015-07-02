package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @see UsersInfoGet
 *
 * @author Dzmitry Lazerka
 */
public class UsersInfoResponse {
	@JsonProperty
	private Map<UserInfo, Set<String>> userInfos;

	// For Jackson.
	private UsersInfoResponse() {}

	public UsersInfoResponse(@Nonnull Map<UserInfo, Set<String>> userInfos) {
		this.userInfos = checkNotNull(userInfos);
	}

	/**
	 * Key is server response, value is users request (non-canonicalized).
	 */
	@Nullable // if request is manually mangled.
	public Map<UserInfo, Set<String>> getUserInfos() {
		return userInfos;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("userInfos", userInfos)
				.toString();
	}
}
