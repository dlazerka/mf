package me.lazerka.mf.android.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.common.base.Joiner;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.activity.login.LoginActivity;
import me.lazerka.mf.android.activity.map.MapFragment;
import me.lazerka.mf.android.adapter.TabsAdapter;
import me.lazerka.mf.android.background.*;
import me.lazerka.mf.api.object.GcmRegistration;
import me.lazerka.mf.api.object.Location;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dzmitry Lazerka
 */
public class MainActivity extends Activity {
	private final String TAG = getClass().getName();

	private final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	/**
	 * Project number obtained from the API Console, as described in GCM "Getting Started."
	 */
	private final String SENDER_ID = "769083712074";

	private ServerConnection mServerConnection;
	private TabsAdapter mTabsAdapter;
	private ActionBar mActionBar;

	private GoogleCloudMessaging gcm;
	private AtomicInteger msgId = new AtomicInteger();
	private String gcmRegistrationId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mServerConnection = new ServerConnection();

		ViewPager viewPager = (ViewPager) findViewById(R.id.pager);

		mActionBar = getActionBar();
		assert mActionBar != null; // Just to silence IDE warning.
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mTabsAdapter = new TabsAdapter(getFragmentManager(), mActionBar, viewPager);
		mTabsAdapter.init();

		if (checkPlayServices()) {
			gcm = GoogleCloudMessaging.getInstance(this);
			gcmRegistrationId = Application.preferences.getGcmRegistrationId();

			if (gcmRegistrationId == null) {
				new GcmRegisterTask().execute();
			}
		} else {
			Log.e(TAG, "No valid Google Play Services APK found.");
			Toast.makeText(this, "Please install Google Play Services", Toast.LENGTH_LONG)
				.show();
			finish();
		}
	}

	/**
	 * https://developer.android.com/google/gcm/client.html
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			Log.w(TAG, "Google Play Services unavailable: " + resultCode);
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST)
						.show();
			} else {
				Log.i(TAG, "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}

	@Override
	protected void onStart() {
		Log.v(TAG, "onStart");
		super.onStart();

		Intent intent = new Intent(this, SenderService.class);
		bindService(intent, mServerConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		Log.v(TAG, "onResume");
		super.onResume();

		checkPlayServices();
	}

	@Override
	protected void onStop() {
		super.onStop();
		unbindService(mServerConnection);
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
				break;
			case R.id.action_quit:
				this.finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void showLocation(Set<String> emails) {
		String commaSeparatedEmails = Joiner.on(',').join(emails);
		ApiRequest apiRequest = ApiRequest.get(Location.PATH + "/" + commaSeparatedEmails, new LocationReceiver());
		mServerConnection.send(apiRequest);

		mTabsAdapter.selectMapTab();
	}

	private class LocationReceiver extends ApiResponseHandler {
		@Override
		protected void handleSuccess(@Nullable String json) {
			Log.v(TAG, "handleSuccess");

			Location location;
			try {
				ObjectMapper mapper = Application.jsonMapper;
				location = mapper.readValue(json, Location.class);
			} catch (IOException e) {
				Log.w(TAG, e.getMessage(), e);
				return;
			}

			MapFragment mapFragment = mTabsAdapter.getMapFragment();
			mapFragment.showLocation(location);
		}
	}

	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	private class GcmRegisterTask extends AsyncTask<Void, Void, String> {
		@Override
		protected String doInBackground(Void... params) {
			try {
				if (gcm == null) {
					gcm = GoogleCloudMessaging.getInstance(Application.context);
				}
				Log.i(TAG, "Calling GCM.register()");
				gcmRegistrationId = gcm.register(SENDER_ID);
				Log.i(TAG, "GCM.register() successful");

				// You should send the registration ID to your server over HTTP,
				// so it can use GCM/HTTP or CCS to send messages to your app.
				// The request to your server should be authenticated if your app
				// is using accounts.
				sendRegistrationIdToServer();

				// For this demo: we don't need to send it because the device
				// will send upstream messages to a server that echo back the
				// message using the 'from' address in the message.

				// Persist the regID - no need to register again.
				Application.preferences.setGcmRegistrationId(gcmRegistrationId);

				return "Device registered";
			} catch (IOException e) {
				Log.i(TAG, "GCM.register() failed: " + e.getMessage());
				return "Error: " + e.getMessage();
				// If there is an error, don't just keep trying to register.
				// Require the user to click a button again, or perform
				// exponential back-off.
			}
		}

		@Override
		protected void onPostExecute(String msg) {
			if (msg.contains("Error")) {
				Log.w(TAG, msg);
				Toast.makeText(MainActivity.this, "Google Cloud Messaging " + msg, Toast.LENGTH_LONG)
						.show();
			} else {
				Log.i(TAG, msg);
			}
		}
	}

	private void sendRegistrationIdToServer() {
		GcmRegistration gcmRegistration = new GcmRegistration(
				gcmRegistrationId,
				Application.preferences.getGcmAppVersion());

		JsonRequest<String> request = JsonRequest.post(
				GcmRegistration.PATH,
				gcmRegistration,
				new Listener<String>() {
					@Override
					public void onResponse(String serverId) {
						Log.i(TAG, "Server stored our registration ID as " + serverId);
						Application.preferences.setGcmRegistrationServerKnows(gcmRegistrationId);
					}
				},
				String.class,
				new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.w(TAG, error.getMessage(), error);
					}
				}
		);
		Application.requestQueue.add(request);
	}
}
