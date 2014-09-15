package me.lazerka.mf.android.activity.map;

import android.app.Activity;
import android.os.Bundle;
import me.lazerka.mf.android.R;

import java.util.concurrent.ScheduledThreadPoolExecutor;

public class MapActivity extends Activity {
	private final String TAG = getClass().getName();

	private MapFragment mapFragment;
	private ScheduledThreadPoolExecutor executor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
	}

	@Override
	protected void onStart() {
		super.onStart();

		executor = new ScheduledThreadPoolExecutor(1);
		//executor.scheduleWithFixedDelay(new LocationRefresher(), 0, 10, TimeUnit.SECONDS);
	}

	@Override
	protected void onStop() {
		executor.shutdown();
		executor = null;
		super.onStop();
	}

	/*
	private class LocationRefresher implements Runnable {
		private final LocationReceiver locationReceiver = new LocationReceiver();

		@Override
		public void run() {
			String url = LocationEvent.getTeamPath(teamId);
			ApiRequest request = ApiRequest.get(url, locationReceiver);
			getEventServiceConnection().send(request);
		}
	}

	private class LocationReceiver extends ApiResponseHandler {
		@Override
		protected void handleSuccess(@Nullable String json) {
			Log.v(TAG, "handleSuccess");

			LocationEvent response;
			try {
				ObjectMapper mapper = Application.JSON_MAPPER;
				response = mapper.readValue(json, TeamLocations.class);
			} catch (IOException e) {
				Log.w(TAG, e.getMessage(), e);
				return;
			}

			Map<String,LocationEvent> emailToLocation = response.getEmailToLocation();
			for (String email : emailToLocation.keySet()) {
				LocationEvent event = emailToLocation.get(email);
				mapFragment.drawLocation(email, event);
			}
		}
	}
	*/
}

