package me.lazerka.mf.android.background;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import me.lazerka.mf.android.BuildConfig;
import me.lazerka.mf.api.object.ApiObject;

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
			httpClient.setFollowSslRedirects(false);

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
