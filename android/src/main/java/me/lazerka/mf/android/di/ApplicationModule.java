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

package me.lazerka.mf.android.di;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.baraded.mf.logging.LogService;
import com.google.firebase.analytics.FirebaseAnalytics;
import dagger.Module;
import dagger.Provides;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.contacts.FriendsManager;
import me.lazerka.mf.android.location.LocationService;

import javax.inject.Inject;
import javax.inject.Singleton;

import static android.content.Context.MODE_PRIVATE;

@Module
public class ApplicationModule {

	private final Application application;

	public ApplicationModule(Application application) {
		this.application = application;
	}

	@Provides
	@Singleton
	public Application provideApplication() {
		return application;
	}

	@Provides
	@Singleton
	@Inject
	public LogService provideLogService(Application application) {
		FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(application);
		return new LogService(firebaseAnalytics);
	}

	@Provides
	@Singleton
	@Inject
	public FirebaseAnalytics provideFirebaseAnalytics(Application application) {
		return FirebaseAnalytics.getInstance(application);
	}

	@Provides
	@Singleton
	@Inject
	public FriendsManager provideFriendsManager(Application application) {
		String fileName = application.getString(R.string.preferences_file_friends);
		SharedPreferences sharedPreferences = application.getSharedPreferences(fileName, MODE_PRIVATE);
		return new FriendsManager(sharedPreferences, application);
	}

	@Provides
	@Singleton
	@Inject
	public LocationService provideLocationService() {
		return new LocationService();
	}

	/**
	 * @return Application's version code from the `PackageManager`.
	 */
	@Provides
	@Inject
	PackageInfo providePackageInfo(Application context) {
		String packageName = context.getPackageName();
		PackageManager packageManager = context.getPackageManager();
		try {
			return packageManager.getPackageInfo(packageName, 0);
		} catch (PackageManager.NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package info: ", e);
		}

	}
}
