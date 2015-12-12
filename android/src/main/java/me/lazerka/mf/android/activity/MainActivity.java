package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request.Method;
import com.android.volley.VolleyError;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.background.gcm.RenewGcmTokenService;
import me.lazerka.mf.android.http.HttpUtils;
import me.lazerka.mf.android.http.JsonRequester;
import me.lazerka.mf.api.object.LocationRequest;
import me.lazerka.mf.api.object.LocationRequestResult;
import me.lazerka.mf.api.object.LocationRequestResult.GcmResult;
import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * @author Dzmitry Lazerka
 */
public class MainActivity extends Activity {
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

		// Make sure server has our GCM token.
		startService(new Intent(this, RenewGcmTokenService.class));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

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

	public void showLocation(Set<String> emails) {
		String requestId = String.valueOf(SystemClock.uptimeMillis());
		LocationRequest locationRequest = new LocationRequest(requestId, emails);
		new LocationRequester(locationRequest)
			.send();

		// Todo change to UI text instead of a Toast.
		Toast.makeText(this, "Requesting emails: " + emails, Toast.LENGTH_LONG)
				.show();
	}

	private class LocationRequester extends JsonRequester<LocationRequest, LocationRequestResult> {
		public LocationRequester(@Nullable LocationRequest request) {
			super(Method.POST, LocationRequest.PATH, request, LocationRequestResult.class, MainActivity.this);
		}

		@Override
		public void onResponse(LocationRequestResult response) {
			List<GcmResult> results = response.getResults();
			if (results == null || results.isEmpty()) {
				logger.warn("Empty results list in LocationRequestResult " + results);
			} else {

				// If at least one result is successful -- show it, otherwise show any error.
				GcmResult oneResult = results.get(0);
				for(GcmResult result : results) {
					if (result.getError() == null) {
						oneResult = result;
						break;
					}
				}

				String error = oneResult.getError();
				if (error == null) {
					String msg = "Sent location request to " + response.getEmail();
					logger.info(msg);
					Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT)
						.show();
				} else {
					String msg = "Error sending location request to " + response.getEmail() + ": " + error;
					logger.warn(msg);
					Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT)
						.show();
				}
			}
		}

		@Override
		public void onErrorResponse(VolleyError error) {
			super.onErrorResponse(error);

			String msg;
			String errorMessage = error.getMessage() != null ? (": " + error.getMessage()) : "";
			if (error instanceof AuthFailureError) {
				logger.error("AuthFailureError", error);
				msg = "Authentication error" + errorMessage;
			} else if (error.networkResponse == null) {
				msg = "Error requesting location" + errorMessage;
			} else if (error.networkResponse.statusCode == 404) {
				if (getRequest().getEmails().size() > 1) {
					msg = "None of your friend's email addresses were found in database. " +
						"Did your friend installed the app?";
				} else {
					String email = getRequest().getEmails().iterator().next();
					String responseContent = HttpUtils.decodeNetworkResponseCharset(error.networkResponse, logger);
					msg = email + " not found: " + responseContent;
				}
				logger.warn(msg);

			} else {
				String responseContent = HttpUtils.decodeNetworkResponseCharset(error.networkResponse, logger);
				if (!responseContent.isEmpty()) {
					msg = "Error requesting location: " + responseContent;
				} else {
					msg = "Error requesting location: " + error.networkResponse.statusCode;
				}
				logger.error(msg);
				ACRA.getErrorReporter().handleException(new IllegalStateException(logger.getName() + ": " + msg));
			}
			Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
		}

		@Override
		protected String getMessage404(NetworkResponse networkResponse) {
			if (getRequest().getEmails().size() > 1) {
				return "None of your friend's email addresses were found in database. " +
						"Did your friend installed the app?";
			} else {
				String email = getRequest().getEmails().iterator().next();
				String responseContent = HttpUtils.decodeNetworkResponseCharset(networkResponse, logger);
				return email + " not found: " + responseContent;
			}
		}
	}
}
