package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
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
import java.util.Set;

/**
 * @author Dzmitry Lazerka
 */
public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getName();

	private final int CONTACT_PICKER_RESULT = 1;

	//private DrawerLayout drawer;
	//private ListView drawerList;
	//private Button addFriendButton;
	//private FrameLayout contentFrame;

	private float offset;
	private boolean flipped;

	//private TabsAdapter mTabsAdapter;
	//private ActionBar mActionBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/*
		drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerList = (ListView) findViewById(R.id.left_drawer);
		addFriendButton = (Button) findViewById(R.id.add_friend);

		addFriendButton.setOnClickListener(new OnAddFriendClickListener());

		FriendsListAdapter adapter = new FriendsListAdapter(this);
		drawerList.setAdapter(adapter);
		// Set the list's click listener
		drawerList.setOnItemClickListener(new DrawerItemClickListener());

		final ImageView drawerIcon = (ImageView) findViewById(R.id.drawer_indicator);

		drawerIcon.setOnClickListener(
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (drawer.isDrawerVisible(START)) {
						drawer.closeDrawer(START);
					} else {
						drawer.openDrawer(START);
					}
				}
			}
		);
*/
		//contentFrame = (FrameLayout) findViewById(R.id.content_frame);
		//FragmentManager fragmentManager = getFragmentManager();
		//fragmentManager.beginTransaction()
		//	.add(R.id.content_frame, new MapFragment())
		//	.commit();
		//ViewPager viewPager = (ViewPager) findViewById(R.id.pager);

		//mActionBar = getActionBar();
		//assert mActionBar != null; // Just to silence IDE warning.
		//mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		//
		//mTabsAdapter = new TabsAdapter(getFragmentManager(), mActionBar, viewPager);
		//mTabsAdapter.init();

		//new GcmAuthenticator(this).checkRegistration();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != CONTACT_PICKER_RESULT) {
			Log.w(TAG, "Unknown request code: " + requestCode);
			super.onActivityResult(requestCode, resultCode, data);
		}

		if (resultCode == Activity.RESULT_OK) {
			Uri contactUri = data.getData();
			Log.i(TAG, "Adding friend: " + contactUri);

			//LinkedHashSet<String> contactEmails = getContactEmails(contactUri);
			//if (contactEmails.isEmpty()) {
			//	String msg = getActivity().getString(R.string.contact_no_emails);
			//	Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG)
			//		.show();
			//	return;
			//}
			//
			//Application.preferences.addFriend(contactUri);
			//mAdapter.refresh();
		}
	}


	@Override
	protected void onResume() {
		Log.v(TAG, "onResume");
		super.onResume();

		new GcmAuthenticator(this).checkPlayServices();
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
				break;
			case R.id.action_quit:
				this.finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void showLocation(Set<String> emails) {
		LocationRequest locationRequest = new LocationRequest();
		locationRequest.setEmails(emails);
		new LocationRequester(locationRequest)
			.send();
		//mTabsAdapter.selectMapTab();
	}

	private class LocationRequester extends JsonRequester<LocationRequest, LocationRequestResult> {
		public LocationRequester(@Nullable LocationRequest request) {
			super(Method.POST, LocationRequest.PATH, request, LocationRequestResult.class);
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
			if (error.networkResponse.statusCode == 404) {
				if (getRequest().getEmails().size() > 1) {
					msg = "None of your friend's email addresses were found in database. " +
						"Did your friend installed the app?";
				} else {
					String email = getRequest().getEmails().iterator().next();
					msg = email + " was found in database. " +
						"Did your friend installed the app?";
				}
				Log.w(TAG, msg);

			} else {
				byte[] data = error.networkResponse.data;
				if (data != null) {
					String errorMessage = new String(data, Charsets.UTF_8);
					msg = "Error requesting location: " + errorMessage;
				} else {
					msg = "Error requesting message: " + error.networkResponse.statusCode;
				}
				Log.e(TAG, msg);
			}
			Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
		}
	}

	private class DrawerItemClickListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Log.i(TAG, "Click" + id + ", " + position);
		}
	}

	private class OnAddFriendClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
			startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
		}
	}
}
