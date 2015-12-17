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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.squareup.okhttp.*;
import com.squareup.okhttp.Request.Builder;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.object.ApiObject;
import okio.BufferedSink;

import javax.annotation.Nonnull;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class ApiPost extends ApiRequest {
	private final ApiObject content;

	public ApiPost(ApiObject content) {
		this.content = content;
	}

	@Override
	public Call newCall(@Nonnull GoogleSignInAccount account) {
		String oauthToken = checkNotNull(account.getIdToken());
		Request request = new Builder()
				.url(url(content))
				.header("Authorization", "Bearer " + oauthToken)
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
				bytes = Application.jsonMapper.writeValueAsBytes(object);
			}
			return bytes;
		}

		@Override
		public long contentLength() throws IOException {
			return getBytes().length;
		}

		@Override
		public void writeTo(BufferedSink sink) throws IOException {
			sink.write(getBytes());
		}
	}
}
