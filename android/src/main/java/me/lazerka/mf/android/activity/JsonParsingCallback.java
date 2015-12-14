package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import me.lazerka.mf.android.Application;
import org.acra.ACRA;
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
	public void onResponse(final Response response) throws IOException {
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
			ACRA.getErrorReporter().handleException(new Exception(msg));
			return;
		}

		final R result = Application.jsonMapper.readValue(
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
	public void onFailure(final Request request, final IOException e) {
		activity.runOnUiThread(
				new Runnable() {
					@Override
					public void run() {
						onNetworkException(request, e);
					}
				});
	}

	@UiThread
	protected abstract void onNetworkException(Request request, IOException e);
}
