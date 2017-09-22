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

package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import com.google.firebase.crash.FirebaseCrash;
import me.lazerka.mf.android.Application;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

/**
 * Common implementation of HTTP {@link Callback} that parses JSON in a worker thread,
 * but does everything else on UI thread.
 *
 * @param <R> type of response to parse.
 */
public abstract class JsonParsingCallback<R> implements Callback {
	private static final Logger logger = LoggerFactory.getLogger(JsonParsingCallback.class);

	private final Activity activity;
	private final Class<R> responseType;

	public JsonParsingCallback(Activity activity, Class<R> responseType) {
		this.activity = activity;
		this.responseType = responseType;
	}

	@WorkerThread
	@Override
	public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
		if (response.isSuccessful()) {
			onSuccess(response.body());
		}
		else {
			activity.runOnUiThread(
					new Runnable() {
						@Override
						public void run() {
							onErrorResponse(response);
						}
					});
		}
	}

	@WorkerThread
	protected void onSuccess(ResponseBody body) throws IOException {
		if (body == null || body.contentLength() == 0) {
			String msg = "Empty body for " + responseType.getSimpleName();
			logger.warn(msg);
			FirebaseCrash.report(new Exception(msg));
			return;
		}

		final R result = Application.getJsonMapper().readValue(
				body.string(),
				responseType);

		activity.runOnUiThread(
				new Runnable() {
					@Override
					public void run() {
						onResult(result);
					}
				});
	}

	@UiThread
	protected abstract void onResult(R result);

	@UiThread
	protected void onErrorResponse(Response response) {
		if (response.code() == HTTP_NOT_FOUND) {
			onNotFound();
		} else {
			onUnknownErrorResponse(response);
		}
	}

	@UiThread
	protected abstract void onNotFound();

	@UiThread
	protected void onUnknownErrorResponse(Response response) {
	}

	@WorkerThread
	@Override
	public void onFailure(@NonNull Call call, @NonNull IOException e) {
		activity.runOnUiThread(
				new Runnable() {
					@Override
					public void run() {
						onNetworkException(call, e);
					}
				});
	}

	@UiThread
	protected abstract void onNetworkException(@NonNull Call call, IOException e);
}
