package me.lazerka.mf.api.gcm;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * GCM message that is sent to GCM.
 *
 * See https://developer.android.com/google/gcm/server.html for specification.
 *
 * @author Dzmitry Lazerka
 */
public class GcmRequest {
	@JsonProperty("registration_ids")
	private List<String> registrationIds;

	@JsonProperty("collapse_key")
	private String collapseKey;

	@JsonProperty("data")
	private Object data;

	@JsonProperty("time_to_live")
	private int timeToLiveSeconds;

	@JsonProperty("restricted_package_name")
	private String restrictedPackageName;

	@JsonProperty("dry_run")
	private boolean dryRun;

	public void setRegistrationIds(List<String> registrationIds) {
		this.registrationIds = registrationIds;
	}

	public void setCollapseKey(String collapseKey) {
		this.collapseKey = collapseKey;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public void setTimeToLiveSeconds(int timeToLiveSeconds) {
		this.timeToLiveSeconds = timeToLiveSeconds;
	}

	@Override
	public String toString() {
		return "GcmLocationRequest{" +
				"registrationIds.size()=" + (registrationIds == null ? "null" : registrationIds.size()) +
				", collapseKey='" + collapseKey + '\'' +
				", data=" + data +
				", timeToLiveSeconds=" + timeToLiveSeconds +
				", restrictedPackageName='" + restrictedPackageName + '\'' +
				", dryRun=" + dryRun +
				'}';
	}

}