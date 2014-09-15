package me.lazerka.mf.android.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.activity.map.MapFragment;
import me.lazerka.mf.android.adapter.TabsAdapter;
import me.lazerka.mf.android.background.ApiRequest;
import me.lazerka.mf.android.background.ApiResponseHandler;
import me.lazerka.mf.android.background.SenderService;
import me.lazerka.mf.android.background.ServerConnection;
import me.lazerka.mf.api.Location;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Set;

/**
 * @author Dzmitry Lazerka
 */
public class MainActivity extends Activity {
	private final String TAG = getClass().getName();

	private ServerConnection mServerConnection;
	private TabsAdapter mTabsAdapter;
	private ActionBar mActionBar;

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
	}

	@Override
	protected void onStart() {
		Log.v(TAG, "onStart");
		super.onStart();

		Intent intent = new Intent(this, SenderService.class);
		bindService(intent, mServerConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unbindService(mServerConnection);
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
				ObjectMapper mapper = Application.JSON_MAPPER;
				location = mapper.readValue(json, Location.class);
			} catch (IOException e) {
				Log.w(TAG, e.getMessage(), e);
				return;
			}

			MapFragment mapFragment = mTabsAdapter.getMapFragment();
			mapFragment.drawLocation(location);
		}
	}
}
