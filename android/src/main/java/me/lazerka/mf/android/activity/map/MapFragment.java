package me.lazerka.mf.android.activity.map;

import android.app.Fragment;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.common.collect.Maps;
import me.lazerka.mf.android.R;
import me.lazerka.mf.api.LocationEvent;
import org.joda.time.Period;

import java.util.Map;

public class MapFragment extends Fragment {
	public static final String CAMERA_POSITION = "cameraPosition";
	private final String TAG = ((Object) this).getClass().getName();

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
		map.setMyLocationEnabled(true);

		// Needs to call MapsInitializer before doing any CameraUpdateFactory calls
		MapsInitializer.initialize(this.getActivity());

		// Watch for button clicks.
		Button button = (Button) view.findViewById(R.id.send_my);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Toast.makeText(getActivity(), "Work In Progress", Toast.LENGTH_SHORT).show();
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

	@Override
	public void onStart() {
		super.onStart();

		if (true) {
			return;
		}

		if (map != null) {
			// Happens when activity stops and then starts again (e.g. Home button).
			return;
		}

		//map = getMap();
		map.getUiSettings().setZoomControlsEnabled(true);
		if (map == null) {
			Toast.makeText(this.getActivity(), "GoogleMap is null, is Google Play services enabled?", Toast.LENGTH_LONG).show();
			return;
		}

		map.setMyLocationEnabled(true);
		map.setTrafficEnabled(true);

		if (map.getCameraPosition().zoom < 2) {
			Location myLocation = map.getMyLocation();
			if (myLocation != null) {
				LatLng latLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
				CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
				map.moveCamera(cameraUpdate);
			}
		}

		//		map.setOnMarkerClickListener(new MyMarkerClickListener());
//		LatLng latLng = new LatLng(37.76, -122.46);
//		CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, 10);
//		map.moveCamera(update);

//		addTestMarker();
	}

	void drawLocation(String email, LocationEvent location) {
		LatLng position = new LatLng(location.getLat(), location.getLon());

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

		long ms = location.getMs();
		Period period = new Period(ms, System.currentTimeMillis());
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

	private class MyLocationChangeListener implements OnMyLocationChangeListener {
		@Override
		public void onMyLocationChange(android.location.Location location) {
			Log.d(TAG, "onMyLocationChange" + location);
			LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 14f);
			map.moveCamera(cameraUpdate);
			map.setOnMyLocationButtonClickListener(null);
		}
	}
	*/
}

