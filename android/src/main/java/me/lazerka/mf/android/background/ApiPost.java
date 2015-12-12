package me.lazerka.mf.android.background;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Request.Builder;
import com.squareup.okhttp.Response;
import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.object.ApiObject;

import java.io.IOException;

/**
 * @author Dzmitry Lazerka
 */
public class ApiPost extends Api {
	private final ApiObject content;

	public ApiPost(ApiObject content) {
		this.content = content;
	}

	public Call newCall(GoogleSignInAccount account) {
		String oauthToken = account.getIdToken();
		Request request = new Builder()
				.url(url(content))
				.header(ApiConstants.COOKIE_NAME_AUTH_TOKEN, oauthToken)
				.post(new JsonRequestBody<>(content))
				.build();

		return httpClient.newCall(request);
	}
}
