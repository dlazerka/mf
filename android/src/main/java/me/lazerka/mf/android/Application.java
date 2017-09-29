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
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Debug;
import android.support.multidex.MultiDexApplication;
import com.baraded.mf.logging.LogService;
import com.baraded.mf.logging.Logger;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import me.lazerka.mf.android.di.Injector;
import me.lazerka.mf.api.JsonMapper;

import java.io.IOException;
import java.util.Locale;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**b
 * Extension of {@link android.app.Application}.
 *
 * @author Dzmitry
 */

public class Application extends MultiDexApplication {
	private static final Logger log = LogService.getLogger(Application.class);

	/**
	 * Shared static instance, as it's a little expensive to create a new one each time.
	 */
	private static JsonMapper jsonMapper;

	private static Locale locale;

	@Override
	public void onCreate() {
		super.onCreate();

		//context = this;

		injectDependencies();

		locale = getResources().getConfiguration().locale;
		//friendsSharedPreferences = getSharedPreferences(getString(R.string.preferences_file_friends), MODE_PRIVATE);
	}

	public static Locale getLocale() {
		return locale;
	}

	private void injectDependencies() {
		Injector.initialize(this);
		Injector.applicationComponent().inject(this);
    }

	//public static FriendsManager getFriendsManager() {
	//	if (friendsManager == null) {
	//		checkContext();
	//		if (friendsSharedPreferences == null) {
	//			throw new IllegalStateException("app not created yet");
	//		}
	//
	//		friendsManager = new FriendsManager(friendsSharedPreferences, context);
	//	}
	//
	//	return friendsManager;
	//}

	private static boolean isInsideEmulator() {
		return Build.DEVICE.startsWith("generic");
	}


	private boolean isDebugRun() {
		return Debug.isDebuggerConnected();
	}

	private boolean isDebugBuild() {
		return ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0);
	}


	public static boolean hasLocationPermission(Context context) {
		return PermissionAsker.hasPermission(ACCESS_FINE_LOCATION, context)
				|| PermissionAsker.hasPermission(ACCESS_COARSE_LOCATION, context);
	}
}
