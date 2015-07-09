package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.model.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.background.GcmIntentService;
import me.lazerka.mf.android.background.GcmMessageHandler;
import me.lazerka.mf.api.gcm.MyLocationGcmPayload;
import me.lazerka.mf.api.object.Location;
import org.joda.time.DateTime;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Locale;
import java.util.Map;

public class MapFragment extends Fragment {
	private static final String TAG = MapFragment.class.getName();
	public static final String CAMERA_POSITION = "cameraPosition";

	private final int circleArea = Color.parseColor("#55DAEAFF");
	private final int circleStroke = Color.parseColor("#FF84B8FE");
	private final float circleStrokeWidth = 3f;

	// Zoom:
	// 1 -- world
	// 10 -- bay area
	// 14 -- max
	@VisibleForTesting
	GoogleMap map;

	private final Map<String, Item> items = Maps.newHashMap();
	private MapView mapView;

	private GcmServiceConnection gcmServiceConnection;

	@Override
	public void onAttach(Activity activity) {
		Log.v(TAG, "onAttach");
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		FriendLocationHandler handler = new FriendLocationHandler(this);
		gcmServiceConnection = new GcmServiceConnection(handler);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.v(TAG, "onCreateView");
		View view = inflater.inflate(R.layout.fragment_map, container, false);

		// Gets the MapView from the XML layout and creates it
		mapView = (MapView) view.findViewById(R.id.mapview);
		mapView.onCreate(savedInstanceState);

		// Gets to GoogleMap from the MapView and does initialization stuff
		map = mapView.getMap();
		map.getUiSettings().setMyLocationButtonEnabled(true);
		map.setMyLocationEnabled(true);
		map.setOnMyLocationChangeListener(new MyLocationChangeListener());

		// Needs to call MapsInitializer before doing any CameraUpdateFactory calls
		MapsInitializer.initialize(this.getActivity());

		// Updates the location and zoom of the MapView
		//CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(43.1, -87.9), 10);
		//map.animateCamera(cameraUpdate);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.v(TAG, "onStart");
		Intent intent = new Intent(getActivity(), GcmIntentService.class);
		getActivity().bindService(intent, gcmServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.v(TAG, "onStop");
		getActivity().unbindService(gcmServiceConnection);
	}

	@Override
	public void onResume() {
		mapView.onResume();
		Log.v(TAG, "onResume");
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.v(TAG, "onDestroy");
		mapView.onDestroy();
	}

	@Override
	public void onDetach() {
		Log.v(TAG, "onDetach");
		super.onDetach();
	}

	@Override
	public void onLowMemory() {
		Log.v(TAG, "onAttach");
		super.onLowMemory();
		mapView.onLowMemory();
	}

	private MainActivity getMyActivity() throws ActivityIsNullException {
		MainActivity result = (MainActivity) getActivity();
		if (result == null) {
			// Can happenen when activity goes away, but there's still a scheduled callback fires.
			throw new ActivityIsNullException();
		}
		return result;
	}

	private int toZoom(double accuracy) throws ActivityIsNullException {
		// From http://stackoverflow.com/questions/18383236
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager windowManager = getMyActivity().getWindowManager();
		Display defaultDisplay = windowManager.getDefaultDisplay();
		defaultDisplay.getMetrics(metrics);
		int screenSize = Math.min(metrics.widthPixels, metrics.heightPixels);
		double mpp = accuracy / screenSize;

		long equatorInMeters = 40075004;
		return (int) (((Math.log(equatorInMeters / (256 * mpp))) / Math.log(2)) + 1);
	}

	private int getNiceZoom(float accuracy) throws ActivityIsNullException {
		int zoom = toZoom(accuracy);
		return zoom > 4 ? zoom - 3 : zoom;
	}

	private void drawLocation(Location location) throws ActivityIsNullException {
		LatLng position = new LatLng(location.getLat(), location.getLon());

		String email = location.getEmail();
		Item item = items.get(email);
		if (item == null) {
			item = new Item();
			items.put(email, item);

			MarkerOptions markerOptions = new MarkerOptions();
			markerOptions.position(position);
			markerOptions.title(email);
			item.marker = map.addMarker(markerOptions);

			CircleOptions options = new CircleOptions();
			options.center(position);
			options.radius(location.getAcc());
			options.fillColor(R.color.transparent_blue);
			options.fillColor(circleArea);
			options.strokeColor(circleStroke);
			options.strokeWidth(circleStrokeWidth);
			item.circle = map.addCircle(options);

			// First time -- center camera and zoom out.
			int zoom = getNiceZoom(location.getAcc());
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(position, zoom - 3);
			map.moveCamera(cameraUpdate);

		} else {
			item.marker.setPosition(position);
			item.circle.setCenter(position);
			item.circle.setRadius(location.getAcc());
		}

		Locale locale = Application.context.getResources().getConfiguration().locale;
		DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale);

		DateTime when = location.getWhen();
		String timeFormatted = timeFormat.format(when.toDate());
		item.marker.setSnippet(timeFormatted);

/* This should be shown only with periodic task to update
		DateTime when = location.getWhen();
		if (when != null) {
			Period period = new Period(when.getMillis(), System.currentTimeMillis());
			String snippet;
			if (period.getDays() > 0) {
				snippet = period.getDays() + " days ago";
			} else if (period.getHours() > 0) {
				snippet = period.getHours() + " hours ago";
			} else if (period.getMinutes() > 0) {
				snippet = period.getMinutes() + " minutes ago";
			} else {
				snippet = period.getSeconds() + " seconds ago";
			}
			item.marker.setSnippet(snippet);
		}
*/
	}

	private void addTestMarker() {
		CircleOptions options = new CircleOptions();
		LatLng latLng1 = new LatLng(37.761, -122.46);
		options.center(latLng1);
		options.radius(150);

		options.fillColor(Color.parseColor("#55DAEAFF"));
		options.strokeColor(Color.parseColor("#FF84B8FE"));
		options.strokeWidth(circleStrokeWidth);
		map.addCircle(options);
		map.addMarker(new MarkerOptions().position(latLng1).title("Test"));
	}

	private static class Item {

		Circle circle;
		Marker marker;
	}
/*
	private class MyMarkerClickListener implements OnMarkerClickListener {
		@Override
		public boolean onMarkerClick(Marker marker) {
			Log.d(TAG, "onMarkerClick " + marker.toString());
			return false;
		}
	}
	*/

	public void showLocation(Location location) {
		Log.v(TAG, "Showing location: " + location);

		try {
			drawLocation(location);
		} catch (ActivityIsNullException e) {
			Log.w(TAG, "showLocation: activity is null");
		}

	}

	/**
	 * Passes message from background to foreground.
	 *
	 * @author Dzmitry Lazerka
	 */
	public static class FriendLocationHandler extends GcmMessageHandler<MyLocationGcmPayload> {
		private final String TAG = getClass().getName();

		/**
		 * If passing this as usual, then outer class cannot get GCed.
		 */
		private final WeakReference<MapFragment> fragmentWeakReference;

		public FriendLocationHandler(MapFragment fragmentWeakReference) {
			this.fragmentWeakReference = new WeakReference<>(fragmentWeakReference);
		}

		@Override
		public void handleMessage(Message message) {
			MapFragment mapFragment = fragmentWeakReference.get();
			if (mapFragment == null) {
				Log.i(TAG, "MapFragment got GCed, not handling friend's location");
				return;
			}

			MainActivity activity;
			try {
				activity = mapFragment.getMyActivity();
			} catch (ActivityIsNullException e) {
				Log.i(TAG, "ActivityIsNullException, not handling friend's location");
				return;
			}

			try {
				MyLocationGcmPayload payload = parseGcmPayload(message, MyLocationGcmPayload.class, activity);
				mapFragment.showLocation(payload.getLocation());
			} catch (IOException e) {
				// Nothing, already reported
			}
		}
	}

	private class MyLocationChangeListener implements OnMyLocationChangeListener {
		boolean set = false;

		@Override
		public void onMyLocationChange(android.location.Location location) {
			if (set) {
				return;
			}

			try {
				Log.d(TAG, "onMyLocationChange" + location);
				set = true;
				LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
				int zoom = getNiceZoom(location.getAccuracy());
				CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
				map.moveCamera(cameraUpdate);
				map.setOnMyLocationChangeListener(null);
			} catch (ActivityIsNullException e) {
				Log.i(TAG, "onMyLocationChange: activity is null, doing nothing.");
			}
		}
	}

	private class ActivityIsNullException extends Exception {}
}

