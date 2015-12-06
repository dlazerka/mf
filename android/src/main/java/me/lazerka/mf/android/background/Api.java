package me.lazerka.mf.android.background;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.api.object.ApiObject;

import java.io.IOException;

/**
 * @author Dzmitry Lazerka
 */
abstract class Api {
	protected static OkHttpClient httpClient = new OkHttpClient();
	static {
		// Don't follow from HTTPS to HTTP.
		httpClient.setFollowSslRedirects(false);
	}

	protected HttpUrl url(ApiObject object) {
		return HttpUrl.get(Application.SERVER_ROOT).resolve(object.getPath());
	}

	public abstract Response execute() throws IOException;
}