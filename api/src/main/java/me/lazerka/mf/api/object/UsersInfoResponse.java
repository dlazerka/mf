package me.lazerka.mf.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @see UsersInfoRequest
 *
 * @author Dzmitry Lazerka
 */
public class UsersInfoResponse {
	@JsonProperty
	private List<UserInfo> userInfos;

	// For Jackson.
	private UsersInfoResponse() {}

	public UsersInfoResponse(@Nonnull List<UserInfo> userInfos) {
		this.userInfos = checkNotNull(userInfos);
	}

	/**
	 * Key is server response, value is users request (non-canonicalized).
	 */
	@Nonnull
	public List<UserInfo> getUserInfos() {
		return userInfos == null ? Collections.<UserInfo>emptyList() : userInfos;
	}

	@Override
	public String toString() {
		return userInfos.toString();
	}
}
