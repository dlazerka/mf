package me.lazerka.mf.api.gcm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * GCM message that is sent to GCM.
 *
 * See https://developer.android.com/google/gcm/server.html for specification.
 *
 * @author Dzmitry Lazerka
 */
@JsonInclude(Include.NON_DEFAULT)
public class GcmRequest {
	@JsonProperty("registration_ids")
	private List<String> registrationIds;

	@JsonProperty("collapse_key")
	private String collapseKey;

	@JsonProperty("data")
	private Data data;

	@JsonProperty("time_to_live")
	private int timeToLiveSeconds;

	@JsonProperty("restricted_package_name")
	private String restrictedPackageName;

	@JsonProperty("dry_run")
	private boolean dryRun;

	// For Jackson
	private GcmRequest() {}

	public GcmRequest(
			@Nonnull List<String> registrationIds,
			@Nullable String collapseKey,
			@Nonnull GcmPayload payload,
			int timeToLiveSeconds) {
		this.registrationIds = checkNotNull(registrationIds);
		this.collapseKey = collapseKey;
		this.data = new Data(payload);
		this.timeToLiveSeconds = timeToLiveSeconds;
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

	public static class Data {
		@JsonProperty(GcmPayload.TYPE_FIELD)
		String type;

		@JsonProperty(GcmPayload.PAYLOAD_FIELD)
		GcmPayload payload;

		public Data(@Nonnull GcmPayload payload) {
			this.type = checkNotNull(payload.getType());
			this.payload = checkNotNull(payload);
		}
	}
}
