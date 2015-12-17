/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2015 Dzmitry Lazerka dlazerka@gmail.com
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package me.lazerka.mf.android.activity;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.model.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.background.gcm.GcmReceiveService;
import me.lazerka.mf.api.object.Location;
import me.lazerka.mf.api.object.LocationUpdate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import java.text.DateFormat;
import java.util.Locale;
import java.util.Map;

public class MapFragment extends Fragment {
	private static final Logger logger = LoggerFactory.getLogger(MapFragment.class);

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

	private final FriendLocationObserver observer = new FriendLocationObserver();
	private Subscription subscription = null;

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
	public void onResume() {
		super.onResume();
		mapView.onResume();

		subscription =
				GcmReceiveService.getLocationReceivedObservable()
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(observer);
	}

	@Override
	public void onPause() {
		super.onPause();

		if (subscription != null) {
			subscription.unsubscribe();
			subscription = null;
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
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(position, zoom);
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
			return false;
		}
	}
	*/

	public void showLocation(Location location) {
		try {
			drawLocation(location);
		} catch (ActivityIsNullException e) {
			logger.warn("showLocation: activity is null");
		}

	}

	private class FriendLocationObserver extends Subscriber<LocationUpdate> {
		@Override
		public void onNext(LocationUpdate payload) {
			showLocation(payload.getLocation());
		}

		@Override
		public void onCompleted() {
			logger.error("onCompleted() aren't to be called.");
		}

		@Override
		public void onError(Throwable e) {
			logger.warn("onError {}", e.getMessage());
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
				set = true;
				LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
				int zoom = getNiceZoom(location.getAccuracy());
				CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
				map.moveCamera(cameraUpdate);
				map.setOnMyLocationChangeListener(null);
			} catch (ActivityIsNullException e) {
				logger.info("onMyLocationChange: activity is null, doing nothing.");
			}
		}
	}

	private class ActivityIsNullException extends Exception {}
}

