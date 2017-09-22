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

package me.lazerka.mf.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Debug;
import android.support.multidex.MultiDexApplication;
import com.baraded.mf.logging.LogService;
import com.baraded.mf.logging.Logger;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.google.firebase.analytics.FirebaseAnalytics;
import me.lazerka.mf.android.activity.EventLogger;
import me.lazerka.mf.android.contacts.FriendsManager;
import me.lazerka.mf.android.location.LocationService;
import me.lazerka.mf.api.JsonMapper;

import java.io.IOException;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.google.common.base.Preconditions.checkState;

/**b
 * Extension of {@link android.app.Application}.
 *
 * @author Dzmitry
 */

public class Application extends MultiDexApplication {
	private static final Logger logger = LogService.getLogger(Application.class);

	/**
	 * Shared static instance, as it's a little expensive to create a new one each time.
	 */
	private static JsonMapper jsonMapper;

	public static GcmManager gcmManager;
	public static Context context;

	private static SharedPreferences friendsSharedPreferences;
	private static FriendsManager friendsManager;

	private static LocationService locationService;

	@Override
	public void onCreate() {
		super.onCreate();

		context = getApplicationContext();

		String preferencesFileGcm = getString(R.string.preferences_file_gcm);
		gcmManager = new GcmManager(getSharedPreferences(preferencesFileGcm, MODE_PRIVATE));

		friendsSharedPreferences = getSharedPreferences(getString(R.string.preferences_file_friends), MODE_PRIVATE);
	}

	private static void checkContext() {
		checkState(context != null, "app not created yet");
	}

	public static FriendsManager getFriendsManager() {
		if (friendsManager == null) {
			checkContext();
			if (friendsSharedPreferences == null) {
				throw new IllegalStateException("app not created yet");
			}

			friendsManager = new FriendsManager(friendsSharedPreferences, context);
		}

		return friendsManager;
	}

	public static FirebaseAnalytics getFirebaseAnalytics() {
		return FirebaseAnalytics.getInstance(context);
	}

	public static EventLogger getEventLogger(String eventName) {
		return new EventLogger(eventName, getFirebaseAnalytics());
	}

	private static boolean isInsideEmulator() {
		return Build.DEVICE.startsWith("generic");
	}

	public static JsonMapper getJsonMapper() {
		if (jsonMapper == null) {
			JsonMapper result = new JsonMapper();
			// Warn, but don't fail on unknown property.
			result.addHandler(new DeserializationProblemHandler() {
				@Override
				public boolean handleUnknownProperty(
						DeserializationContext deserializationContext,
						JsonParser jsonParser,
						JsonDeserializer<?> deserializer,
						Object beanOrClass,
						String propertyName
				) throws IOException
				{
					String msg = "Unknown property `" + propertyName + "` in " + beanOrClass;
					logger.warn(msg);
					jsonParser.skipChildren();
					return true;
				}
			});
			jsonMapper = result;
		}

		return jsonMapper;
	}

	public static LocationService getLocationService() {
		if (locationService == null) {
			checkContext();
			locationService = new LocationService(context);
		}
		return locationService;
	}

	private boolean isDebugRun() {
		return Debug.isDebuggerConnected();
	}

	private boolean isDebugBuild() {
		return ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0);
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	public static int getVersion() {
		String packageName = Application.context.getPackageName();
		PackageManager packageManager = Application.context.getPackageManager();
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	public static boolean hasLocationPermission() {
		return PermissionAsker.hasPermission(ACCESS_FINE_LOCATION, context)
				|| PermissionAsker.hasPermission(ACCESS_COARSE_LOCATION, context);
	}
}
