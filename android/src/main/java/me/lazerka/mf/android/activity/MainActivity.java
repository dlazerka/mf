package me.lazerka.mf.android.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.UiThread;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.squareup.okhttp.*;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.FriendInfo;
import me.lazerka.mf.android.background.ApiPost;
import me.lazerka.mf.android.background.gcm.GcmRegisterIntentService;
import me.lazerka.mf.api.object.GcmResult;
import me.lazerka.mf.api.object.LocationRequest;
import me.lazerka.mf.api.object.LocationRequestResult;
import org.acra.ACRA;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.joda.time.DateTimeZone.UTC;

/**
 * Extends FragmentActivity only for GoogleApiClient.
 *
 * @author Dzmitry Lazerka
 */
public class MainActivity extends GoogleApiActivity {
	private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					// replace(), not add, because this is called
					.replace(R.id.bottom_fragment_container, new ContactsFragment())
					.commit();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		// Make sure server knows our GCM token.
		startService(new Intent(this, GcmRegisterIntentService.class));
	}

	@Override
	protected void handleSignInFailed() {
		Intent intent = new Intent(this, LoginActivity.class);
		startActivity(intent);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// TODO implement settings
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
			case R.id.action_logout:
				Application.preferences.clearAccount();
				intent = new Intent(this, LoginActivity.class);
				startActivity(intent);
				finish();
				break;
			case R.id.clear_token:
				//
				//Application.preferences.clearGcmToken();
				recreate();
				break;
			case R.id.action_quit:
				this.finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void requestLocationUpdates(@Nonnull FriendInfo friendInfo) {
		checkArgument(!friendInfo.emails.isEmpty());

		String requestId = String.valueOf(SystemClock.uptimeMillis());
		DateTime sentAt = DateTime.now(UTC);
		Duration duration = Duration.standardMinutes(15);
		LocationRequest locationRequest = new LocationRequest(requestId, friendInfo.emails, sentAt, duration);

		GoogleSignInAccount account = getAccount();
		if (account == null) {
			String message = "Account is null";
			ACRA.getErrorReporter().handleSilentException(new IllegalStateException(message));
			Toast.makeText(this, R.string.sign_in_account_null, Toast.LENGTH_LONG)
					.show();
			return;
		}

		Call call = new ApiPost(locationRequest).newCall(account);
		call.enqueue(new LocationRequestCallback(friendInfo));

		// Todo change to UI text instead of a Toast.
		String text = getString(R.string.sending_location_request, friendInfo.displayName);
		Toast.makeText(this, text, Toast.LENGTH_LONG)
				.show();
	}

	private class LocationRequestCallback extends JsonParsingCallback<LocationRequestResult> {
		private final Context context = MainActivity.this;
		private final FriendInfo friendInfo;

		public LocationRequestCallback(FriendInfo friendInfo) {
			super(MainActivity.this, LocationRequestResult.class);
			this.friendInfo = friendInfo;
		}

		@UiThread
		@Override
		protected void onResult(LocationRequestResult result) {
			final List<GcmResult> gcmResults = result.getGcmResults();
			if (gcmResults == null || gcmResults.isEmpty()) {
				logger.warn("Empty gcmResults list in LocationRequestResult " + gcmResults);
				return;
			}

			// If at least one result is successful -- show it, otherwise show the first error.
			// TODO we can handle some of GCM error responses, like GcmConstants.ERROR_UNAVAILABLE
			GcmResult oneResult = gcmResults.get(0);
			for(GcmResult gcmResult : gcmResults) {
				if (gcmResult.getError() == null) {
					oneResult = gcmResult;
					break;
				}
			}
			String error = oneResult.getError();
			if (error == null) {
				String msg = getString(
						R.string.sent_location_request,
						result.getEmail());
				logger.info(msg);
				Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
			} else {
				String msg = getString(R.string.gcm_error, error);
				logger.warn(msg);
				ACRA.getErrorReporter().handleSilentException(new Exception(msg));
				Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
			}
		}

		@UiThread
		@Override
		protected void onNotFound() {
			// TODO: show dialog suggesting to send a message to friend.
			String msg = getString(R.string.contact_havent_installed_app, friendInfo.displayName);
			logger.warn(msg);
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
		}

		@UiThread
		@Override
		protected void onUnknownErrorResponse(Response response) {
			String msg = getString(
					R.string.error_relaying_request,
					response.code(),
					response.message());
			logger.error(msg);
			ACRA.getErrorReporter().handleSilentException(new Exception(logger.getName() + ": " + msg));
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
		}

		@UiThread
		@Override
		public void onNetworkException(Request request, IOException e) {
			String msg;
			if (e instanceof SocketTimeoutException) {
				msg = getString(R.string.error_socket_timeout);
			} else if (e instanceof ConnectException) {
				msg = getString(R.string.error_connection_exception);
			} else if (e.getMessage() != null) {
				msg = getString(R.string.error_sending_request, e.getMessage());
			} else {
				msg = getString(R.string.error_sending_request, e.getClass().getSimpleName());
			}

			logger.warn(msg, e);
			ACRA.getErrorReporter().handleSilentException(e);
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
		}
	}
}
