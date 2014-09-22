package me.lazerka.mf.android.activity.map;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.model.*;
import com.google.common.collect.Maps;
import me.lazerka.mf.android.R;
import me.lazerka.mf.api.object.Location;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.Map;

public class MapFragment extends Fragment {
	private final int CONTACT_PICKER_RESULT = 1;

	public static final String CAMERA_POSITION = "cameraPosition";
	private final String TAG = getClass().getName();

	private final int circleArea = Color.parseColor("#55DAEAFF");
	private final int circleStroke = Color.parseColor("#FF84B8FE");
	private final float circleStrokeWidth = 3f;

	// Zoom:
	// 1 -- world
	// 10 -- bay area
	// 14 -- max
	private GoogleMap map;

	private final Map<String, Item> items = Maps.newHashMap();
	private MapView mMapView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_map, container, false);

		// Gets the MapView from the XML layout and creates it
		mMapView = (MapView) view.findViewById(R.id.mapview);
		mMapView.onCreate(savedInstanceState);

		// Gets to GoogleMap from the MapView and does initialization stuff
		map = mMapView.getMap();
		map.getUiSettings().setMyLocationButtonEnabled(true);
		map.setOnMyLocationChangeListener(new MyLocationChangeListener());
		map.setMyLocationEnabled(true);

		// Needs to call MapsInitializer before doing any CameraUpdateFactory calls
		MapsInitializer.initialize(this.getActivity());

		// Watch for button clicks.
		Button button = (Button) view.findViewById(R.id.send_my);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				android.location.Location myLocation = map.getMyLocation();
				if (myLocation == null) {
					Toast.makeText(getActivity(), "Current location is unknown, try later", Toast.LENGTH_LONG)
							.show();
					return;
				}

				double lat = myLocation.getLatitude();
				double lon = myLocation.getLongitude();
				double acc = myLocation.getAccuracy();

				int zoom = toZoom(acc);

				// Like "https://www.google.com/maps/@40.5697761,-119.7923031,8z"
				String url = getActivity().getString(R.string.gmaps_url, (Double) lat, (Double) lon, zoom);
				String appName = getActivity().getString(R.string.app_name);
				String text = getActivity().getString(R.string.my_location_message_text, url, appName);
				String html = getActivity().getString(R.string.my_location_message_html, url, appName);
				String subject = getActivity().getString(R.string.my_location_subject);

				Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_TEXT, text);
				sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
				sendIntent.putExtra(Intent.EXTRA_HTML_TEXT, html); // Doesn't work, actually.
				sendIntent.setType("text/plain");
				startActivity(Intent.createChooser(sendIntent, url));
			}
		});

		// Updates the location and zoom of the MapView
		//CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(43.1, -87.9), 10);
		//map.animateCamera(cameraUpdate);

		return view;
	}

	@Override
	public void onResume() {
		mMapView.onResume();
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mMapView.onDestroy();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mMapView.onLowMemory();
	}

	private int toZoom(double accuracy) {
		// From http://stackoverflow.com/questions/18383236
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager windowManager = getActivity().getWindowManager();
		Display defaultDisplay = windowManager.getDefaultDisplay();
		defaultDisplay.getMetrics(metrics);
		int screenSize = Math.min(metrics.widthPixels, metrics.heightPixels);
		double mpp = accuracy/screenSize;

		long equatorInMeters = 40075004;
		return (int) (((Math.log(equatorInMeters / (256 * mpp))) / Math.log(2)) + 1);
	}

	private int getNiceZoom(float accuracy) {
		int zoom = toZoom(accuracy);
		return zoom > 4 ? zoom - 3 : zoom;
	}

	public void showLocation(Location location) {
		drawLocation(location);

		LatLng latLng = new LatLng(location.getLat(), location.getLon());

		int zoom = getNiceZoom(location.getAcc());
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom + 2);
		map.moveCamera(cameraUpdate);
		map.setOnMyLocationButtonClickListener(null);
	}

	public void drawLocation(Location location) {
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
		} else {
			item.marker.setPosition(position);
			item.circle.setCenter(position);
		}

		DateTime when = location.getWhen();
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

	private class MyLocationChangeListener implements OnMyLocationChangeListener {
		boolean set = false;

		@Override
		public void onMyLocationChange(android.location.Location location) {
			if (set) {
				return;
			}
			Log.d(TAG, "onMyLocationChange" + location);
			set = true;
			LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
			int zoom = getNiceZoom(location.getAccuracy());
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
			map.moveCamera(cameraUpdate);
			map.setOnMyLocationButtonClickListener(null);
		}
	}
}

