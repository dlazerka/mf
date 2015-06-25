package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request.Method;
import com.android.volley.VolleyError;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.auth.GcmAuthenticator;
import me.lazerka.mf.android.http.HttpUtils;
import me.lazerka.mf.android.http.JsonRequester;
import me.lazerka.mf.api.object.LocationRequest;
import me.lazerka.mf.api.object.LocationRequestResult;
import me.lazerka.mf.api.object.LocationRequestResult.GcmResult;
import org.acra.ACRA;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * @author Dzmitry Lazerka
 */
public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getName();

	/** What users to show. */
	public static final String REQUEST_CONTACT_EMAILS = "REQUEST_CONTACT_EMAILS";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	protected void onResume() {
		Log.v(TAG, "onResume");
		super.onResume();

		GcmAuthenticator gcmAuthenticator = new GcmAuthenticator(this);
		gcmAuthenticator.checkRegistration();
	}

	@Override
	protected void onStop() {
		super.onStop();
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
				Application.preferences.clearGcmToken();
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
				Log.w(TAG, "Empty results list in LocationRequestResult " + results);
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
					Log.i(TAG, msg);
					Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT)
						.show();
				} else {
					String msg = "Error sending location request to " + response.getEmail() + ": " + error;
					Log.w(TAG, msg);
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
				Log.e(TAG, "AuthFailureError", error);
				msg = "Authentication error" + errorMessage;
			} else if (error.networkResponse == null) {
				msg = "Error requesting location" + errorMessage;
			} else if (error.networkResponse.statusCode == 404) {
				if (getRequest().getEmails().size() > 1) {
					msg = "None of your friend's email addresses were found in database. " +
						"Did your friend installed the app?";
				} else {
					String email = getRequest().getEmails().iterator().next();
					String responseContent = HttpUtils.decodeNetworkResponseCharset(error.networkResponse, TAG);
					msg = email + " not found: " + responseContent;
				}
				Log.w(TAG, msg);

			} else {
				String responseContent = HttpUtils.decodeNetworkResponseCharset(error.networkResponse, TAG);
				if (!responseContent.isEmpty()) {
					msg = "Error requesting location: " + responseContent;
				} else {
					msg = "Error requesting message: " + error.networkResponse.statusCode;
				}
				Log.e(TAG, msg);
				ACRA.getErrorReporter().handleException(new IllegalStateException(TAG + ": " + msg));
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
				String responseContent = HttpUtils.decodeNetworkResponseCharset(networkResponse, TAG);
				return email + " not found: " + responseContent;
			}
		}
	}
}
