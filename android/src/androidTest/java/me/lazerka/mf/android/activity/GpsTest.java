package me.lazerka.mf.android.activity;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import com.google.android.gms.maps.model.CameraPosition;
import me.lazerka.mf.android.R;
import org.joda.time.DateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * @author Dzmitry Lazerka
 */
public class GpsTest extends ActivityInstrumentationTestCase2<MainActivity> {
	private static final String TEST = "test";
	private LocationManager locationManager;

	public GpsTest() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		if (locationManager.getProvider(TEST) == null) {
			locationManager.addTestProvider(
					TEST,
					false,
					false,
					false,
					false,
					true,
					true,
					true,
					Criteria.NO_REQUIREMENT,
					Criteria.ACCURACY_FINE);
			locationManager.setTestProviderEnabled(TEST, true);
		}
	}

	@Override
	protected void tearDown() throws Exception {
		locationManager.removeTestProvider(TEST);
		super.tearDown();
	}

	/**
	 * Ignored because doesn't work.
	 */
	@UiThreadTest
	public void ignoreTestGPS() {
		MainActivity activity = getActivity();

		Location location = new Location(TEST);
		location.setTime(
				DateTime.parse("2015-04-25T00:00:00Z")
						.getMillis());
		location.setLatitude(37.0);
		location.setLongitude(-122.0);
		location.setAccuracy(10.0f);
		location.setElapsedRealtimeNanos(123);

		MapFragment mapFragment = (MapFragment) activity.getFragmentManager().findFragmentById(R.id.map);

		CameraPosition cameraPosition1 = mapFragment.map.getCameraPosition();

		locationManager.setTestProviderLocation(TEST, location);

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		CameraPosition cameraPosition2 = mapFragment.map.getCameraPosition();

		assertThat(cameraPosition1, not(is(cameraPosition2)));
	}
}
