package me.lazerka.mf.android.background;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.api.ApiConstants;
import me.lazerka.mf.api.object.ApiObject;
import okio.BufferedSink;

import java.io.IOException;

/**
 * @author Dzmitry Lazerka
 */
public class JsonRequestBody<T extends ApiObject> extends RequestBody {
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
