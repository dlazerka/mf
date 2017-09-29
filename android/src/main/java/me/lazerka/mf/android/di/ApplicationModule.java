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
import com.baraded.mf.io.JsonMapper;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.analytics.FirebaseAnalytics;
import dagger.Module;
import dagger.Provides;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.GcmManager;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.contacts.FriendsManager;
import me.lazerka.mf.android.location.LocationService;
import okhttp3.OkHttpClient;

import javax.inject.Singleton;

import static android.content.Context.MODE_PRIVATE;

@Module
class ApplicationModule {

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
	public FirebaseAnalytics provideFirebaseAnalytics(Application application) {
		return FirebaseAnalytics.getInstance(application);
	}

	@Provides
	@Singleton
	public FriendsManager provideFriendsManager(Application application) {
		String fileName = application.getString(R.string.preferences_file_friends);
		SharedPreferences sharedPreferences = application.getSharedPreferences(fileName, MODE_PRIVATE);
		return new FriendsManager(sharedPreferences, application);
	}

	@Provides
	@Singleton
	public GcmManager provideGcmManager(PackageInfo packageInfo) {
		String fileName = application.getString(R.string.preferences_file_gcm);
		SharedPreferences sharedPreferences = application.getSharedPreferences(fileName, MODE_PRIVATE);
		return new GcmManager(packageInfo, sharedPreferences);
	}

	@Provides
	@Singleton
	public LocationService provideLocationService() {
		return new LocationService();
	}

	@Provides
	@Singleton
	public JsonMapper provideJsonMapper() {
		return JsonMapper.INSTANCE;
	}

	/**
	 * @return Application's version code from the `PackageManager`.
	 */
	@Provides
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

	@Provides
	@Singleton
	OkHttpClient provideOkHttp() {
		return new OkHttpClient.Builder()
				// Don't follow from HTTPS to HTTP.
				.followRedirects(false)

				// We don't use authenticator, because it kicks in only on unsuccessful response,
				// and currently only supports BASIC authentication.
				// But we must provide OAuth token in each single request.
				//.authenticator(new GoogleSignInAuthenticator() {})

				// Nor we use interceptors for authentication, because SignIn authentication
				// requires GoogleApiClient, which requires Context,
				// so it must be provided by calling Activity/Service.
				// .interceptors().add();

				// Same reason we don't use Retrofit -- unable to make Authorization header right.
				.build();

	}

	@Provides
	GoogleSignInAccount provideAccount() {
		// TODO
		return null;
	}
}
