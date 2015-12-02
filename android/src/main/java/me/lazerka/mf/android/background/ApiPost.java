package me.lazerka.mf.android.background;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Request.Builder;
import com.squareup.okhttp.Response;
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

	@Override
	public Response execute() throws IOException {
		Request request = new Builder()
				.url(url(content))
				.post(new JsonRequestBody<>(content))
				.build();

		return httpClient.newCall(request).execute();
	}
}
