package me.lazerka.mf.android.background.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationResult;
import com.squareup.okhttp.Response;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.auth.GoogleApiException;
import me.lazerka.mf.android.auth.SignInManager;
import me.lazerka.mf.android.background.ApiPost;
import me.lazerka.mf.api.object.*;
import org.acra.ACRA;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.joda.time.DateTimeZone.UTC;

/**
 * Sends location update to server.
 *
 * Is invoked on each location update.
 */
public class LocationUpdateListener extends IntentService {
	private static final Logger logger = LoggerFactory.getLogger(LocationRequestHandler.class);

	static final String EXTRA_GCM_REQUEST = "gcmRequest";

	public LocationUpdateListener() {
		super(LocationUpdateListener.class.getSimpleName());
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (LocationAvailability.hasLocationAvailability(intent)) {
			LocationAvailability availability = LocationAvailability.extractLocationAvailability(intent);
			logger.trace(availability.toString());
		}

		if (LocationResult.hasResult(intent)) {
			LocationResult locationResult = LocationResult.extractResult(intent);
			logger.info(locationResult.getLastLocation().toString()); // todo trace

			Toast.makeText(this, "sdfgsdfg", Toast.LENGTH_SHORT).show();

			try {
				byte[] json = checkNotNull(intent.getByteArrayExtra(EXTRA_GCM_REQUEST));
				LocationRequest gcmRequest = Application.jsonMapper.readValue(json, LocationRequest.class);
				sendLocation(locationResult.getLastLocation(), gcmRequest);
			} catch (IOException e) {
				// Unrealistic, we already parsed it once in GcmReceiveService.
				logger.error("Cannot parse", e);
				ACRA.getErrorReporter().handleSilentException(e);
			}
		}
	}

	void sendLocation(android.location.Location location, LocationRequest gcmRequest) {
		SignInManager signInManager = new SignInManager();
		GoogleSignInAccount account;
		try {
			account = signInManager.getAccountBlocking(this);
		} catch (GoogleApiException e) {
			logger.warn("Unable to sign in: {} {}", e.getCode(), e.getMessage());
			// TODO implement reconnection logic
			return;
		}

		Location locationBean = new Location(
				DateTime.now(UTC),
				account.getEmail(),
				location.getLatitude(),
				location.getLongitude(),
				location.getAccuracy()
		);

		LocationUpdate locationUpdate = new LocationUpdate(locationBean, gcmRequest);

		ApiPost post = new ApiPost(locationUpdate);
		try {
			Response response = post.newCall(account).execute();

			if (response.code() == 200) {
				String json = response.body().string();
				LocationUpdateResponse bean =
						Application.jsonMapper.readValue(json, LocationUpdateResponse.class);
				List<GcmResult> gcmResults = bean.getGcmResults();

				for(GcmResult gcmResult : gcmResults) {
					if (!gcmResult.isSuccessful()) {
						logger.warn("Unsuccessful sending: " + gcmResult.getError());
						break;
					}
				}
			} else {
				logger.warn("Failed: {}, {}", response.code(), response.message());
			}
		} catch (IOException e) {
			logger.warn("IOException: {}", e.getMessage());
			// Not sending ACRA, because this is spammy.
		}
	}
}
