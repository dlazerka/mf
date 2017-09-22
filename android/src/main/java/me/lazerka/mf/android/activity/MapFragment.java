/*
 *     Copyright (C) 2017 Dzmitry Lazerka
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package me.lazerka.mf.android.activity;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.*;
import com.baraded.mf.Sw;
import com.baraded.mf.logging.LogService;
import com.baraded.mf.logging.Logger;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.PersonInfo;
import me.lazerka.mf.android.location.FriendLocationResponse;
import me.lazerka.mf.api.object.Location;
import me.lazerka.mf.api.object.LocationResponse;
import org.joda.time.DateTime;

import java.text.DateFormat;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class MapFragment extends Fragment {
	private static final Logger log = LogService.getLogger(MapFragment.class);

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

	private FriendLocationObserver observer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_map, container, false);

		// Gets the MapView from the XML layout and creates it
		mapView = (MapView) view.findViewById(R.id.mapview);
		mapView.onCreate(savedInstanceState);

		// Gets to GoogleMap from the MapView and does initialization stuff
		final Sw uptimeSw = Sw.uptime();
		//noinspection Convert2Lambda lint doesn't get it.
		mapView.getMapAsync(new OnMapReadyCallback() {
			@SuppressLint("MissingPermission")
			@Override
			public void onMapReady(GoogleMap googleMap) {
				log.info("map ready in {}ms", uptimeSw.ms());
				Application.getEventLogger("mapReady")
					.param("ms", uptimeSw.ms())
					.send();

				map = googleMap;
				map.getUiSettings().setMyLocationButtonEnabled(true);

				if (Application.hasLocationPermission()) {
					//noinspection MissingPermission
					map.setMyLocationEnabled(true);
					//map.setOnMyLocationChangeListener(new MyLocationChangeListener());
				}
			}
		});

		// Needs to call MapsInitializer before doing any CameraUpdateFactory calls
		MapsInitializer.initialize(this.getActivity());

		// Updates the location and zoom of the MapView
		//CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(43.1, -87.9), 10);
		//map.animateCamera(cameraUpdate);

		return view;
	}


	@Override
	public void onResume() {
		super.onResume();
		mapView.onResume();

		observer = Application.getLocationService()
			.getLocationUpdates()
			.subscribeOn(AndroidSchedulers.mainThread())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribeWith(new FriendLocationObserver());
	}

	@Override
	public void onPause() {
		super.onPause();

		if (observer != null) {
			observer.dispose();
			observer = null;
		}

		mapView.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onLowMemory() {
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

	private void drawLocation(FriendLocationResponse locationResponse) throws ActivityIsNullException {
		if (map == null) {
			log.error("map is null");
			return;
		}

		LocationResponse response = locationResponse.getResponse();
		Location location = checkNotNull(response.getLocation());
		PersonInfo personInfo = checkNotNull(locationResponse.getContact());

		LatLng position = new LatLng(location.getLat(), location.getLon());

		String displayName = personInfo.displayName;
		Item item = items.get(displayName);
		if (item == null) {
			item = new Item();
			items.put(displayName, item);

			MarkerOptions markerOptions = new MarkerOptions();
			markerOptions.position(position);
			markerOptions.title(displayName);
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
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(position, zoom);
			map.moveCamera(cameraUpdate);

		} else {
			item.marker.setPosition(position);
			item.circle.setCenter(position);
			item.circle.setRadius(location.getAcc());
		}

		//noinspection deprecation the recommended fix doesn't work for API < 24.
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
			return false;
		}
	}
	*/

	public void showLocation(FriendLocationResponse friendLocationResponse) {
		try {
			drawLocation(friendLocationResponse);
		} catch (ActivityIsNullException e) {
			log.warn("showLocation: activity is null");
		}

	}

	private class FriendLocationObserver extends DisposableObserver<FriendLocationResponse> {

		@Override
		public void onNext(FriendLocationResponse response) {
			showLocation(response);
		}

		@Override
		public void onComplete() {
			log.error("onComplete() aren't to be called.");
		}

		@Override
		public void onError(Throwable e) {
			log.warn("onError {}", e.getMessage());
		}

	}

	//private class MyLocationChangeListener implements OnMyLocationChangeListener {
	//	boolean set = false;
	//
	//	@Override
	//	public void onMyLocationChange(android.location.Location location) {
	//		if (set) {
	//			return;
	//		}
	//
	//		try {
	//			set = true;
	//			LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
	//			int zoom = getNiceZoom(location.getAccuracy());
	//			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
	//			map.moveCamera(cameraUpdate);
	//			map.setOnMyLocationChangeListener(null);
	//		} catch (ActivityIsNullException e) {
	//			log.info("onMyLocationChange: activity is null, doing nothing.");
	//		}
	//	}
	//}
	//
	private static class ActivityIsNullException extends Exception {
		private static final long serialVersionUID = 1;
	}
}

