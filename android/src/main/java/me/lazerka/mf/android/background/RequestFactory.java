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

package me.lazerka.mf.android.background;

import com.baraded.mf.Util;
import com.baraded.mf.io.JsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import me.lazerka.mf.android.BuildConfig;
import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.object.ApiObject;
import okhttp3.*;
import okhttp3.Request.Builder;
import okio.BufferedSink;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.net.URI;

import static me.lazerka.mf.android.Util.checkNotNull;


/**
 * @author Dzmitry Lazerka
 */
public class RequestFactory {
	private final OkHttpClient httpClient;
	private Provider<GoogleSignInAccount> accountProvider;
	private final HttpUrl rootHttpUrl;

	@Inject
	public RequestFactory(OkHttpClient httpClient, @Nonnull Provider<GoogleSignInAccount> accountProvider) {
		this.httpClient = httpClient;
		this.accountProvider = accountProvider;
		rootHttpUrl = checkNotNull(HttpUrl.get(URI.create(BuildConfig.BACKEND_ROOT)));
	}

	protected HttpUrl url(String path) {
		return rootHttpUrl.resolve(path);
	}

	private Builder newCallBuilder(String path) {
		GoogleSignInAccount account = accountProvider.get();
		String oauthToken = Util.INSTANCE.checkNotNull(account.getIdToken());
		return new Request.Builder()
				.url(url(path))
				.header("Authorization", "Bearer " + oauthToken);
	}

	public Call newGet(ApiObject content) {
		Request request = newCallBuilder(content.getPath())
				.get()
				.build();

		return httpClient.newCall(request);
	}

	public Call newPost(ApiObject content) {
		Request request = newCallBuilder(content.getPath())
				.post(new JsonRequestBody<>(content))
				.build();

		return httpClient.newCall(request);
	}

	/** Serializes JSON. */
	private static class JsonRequestBody<T extends ApiObject> extends RequestBody {
		private static final MediaType MEDIA_TYPE = MediaType.parse(ApiConstants.APPLICATION_JSON);
		private final T object;
		private byte[] bytes;

		public JsonRequestBody(T object) {
			this.object = object;
		}

		@Override
		public MediaType contentType() {
			return MEDIA_TYPE;
		}

		private byte[] getBytes() throws JsonProcessingException {
			if (bytes == null) {
				bytes = JsonMapper.INSTANCE.writeValueAsBytes(object);
			}
			return bytes;
		}

		@Override
		public long contentLength() throws IOException {
			return getBytes().length;
		}

		@Override
		public void writeTo(@Nonnull BufferedSink sink) throws IOException {
			sink.write(getBytes());
		}
	}

}
