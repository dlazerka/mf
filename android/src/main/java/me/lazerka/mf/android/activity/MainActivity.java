package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request.Method;
import com.android.volley.VolleyError;
import com.google.common.base.Charsets;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.auth.GcmAuthenticator;
import me.lazerka.mf.android.http.JsonRequester;
import me.lazerka.mf.api.object.LocationRequest;
import me.lazerka.mf.api.object.LocationRequestResult;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Dzmitry Lazerka
 */
public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getName();

	/** ContactActivity result code */
	private final int CONTACT_ACTIVITY_RESULT = 1;
	/** What users to show. */
	public static final String REQUEST_CONTACT_EMAILS = "REQUEST_CONTACT_EMAILS";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button contactsButton = (Button) findViewById(R.id.choose_contact_btn);
		contactsButton.setOnClickListener(new OnContactsClickListener());
	}

	private class OnContactsClickListener
			implements OnClickListener {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(getBaseContext(), ContactsActivity.class);
			startActivityForResult(intent, CONTACT_ACTIVITY_RESULT);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CONTACT_ACTIVITY_RESULT) {
			if (resultCode == Activity.RESULT_OK) {
				Set<String> emails = new LinkedHashSet<>(data.getStringArrayListExtra(REQUEST_CONTACT_EMAILS));
				Toast.makeText(this, "Requesting emails: " + emails, Toast.LENGTH_LONG)
						.show();
				showLocation(emails);
			}
		} else {
			Log.w(TAG, "Unknown request code: " + requestCode);
			super.onActivityResult(requestCode, resultCode, data);
		}
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
	}

	private class LocationRequester extends JsonRequester<LocationRequest, LocationRequestResult> {
		public LocationRequester(@Nullable LocationRequest request) {
			super(Method.POST, LocationRequest.PATH, request, LocationRequestResult.class, MainActivity.this);
		}

		@Override
		public void onResponse(LocationRequestResult response) {
			int devices = response.getResults().size();
			if (devices == 1) {
				String error = response.getResults().get(0).getError();
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
			} else {
				Toast.makeText(
					MainActivity.this,
					"Sent location request to " + devices + " devices of " + response.getEmail(),
					Toast.LENGTH_LONG)
					.show();
				// TODO show nicer message to user in multiple devices case.
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
					msg = email + " not found: " + new String(error.networkResponse.data, UTF_8);
				}
				Log.w(TAG, msg);

			} else {
				byte[] data = error.networkResponse.data;
				if (data != null) {
					String errorData = new String(data, Charsets.UTF_8);
					msg = "Error requesting location: " + errorData;
				} else {
					msg = "Error requesting message: " + error.networkResponse.statusCode;
				}
				Log.e(TAG, msg);
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
				return email + " not found: " + new String(networkResponse.data, UTF_8);
			}
		}
	}
}
