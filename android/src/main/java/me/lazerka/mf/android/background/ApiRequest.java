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

package me.lazerka.mf.android.background;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import me.lazerka.mf.android.BuildConfig;
import me.lazerka.mf.api.object.ApiObject;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import java.net.URI;

/**
 * @author Dzmitry Lazerka
 */
abstract class ApiRequest {
	protected static OkHttpClient httpClient;

	public ApiRequest() {
		if (httpClient == null) {
			httpClient = new OkHttpClient();
			// Don't follow from HTTPS to HTTP.
			//httpClient.followRedirects();

			// We don't use authenticator, because it kicks in only on unsuccessful response,
			// and currently only supports BASIC authentication.
			// But we must provide OAuth token in each single request.
			// httpClient.setAuthenticator(...);

			// Nor we use interceptors for authentication, because SignIn authentication
			// requires GoogleApiClient, which requires Context,
			// so it must be provided by calling Activity/Service.
			// httpClient.interceptors().add();

			// Same reason we don't use Retrofit -- unable to make Authorization header right.
		}
	}

	protected HttpUrl url(ApiObject object) {
		return HttpUrl.get(URI.create(BuildConfig.BACKEND_ROOT)).resolve(object.getPath());
	}

	public abstract Call newCall(GoogleSignInAccount account);
}
