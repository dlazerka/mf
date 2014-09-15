package me.lazerka.mf.android;

import android.util.Log;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;

import java.io.IOException;

/**
 * @author Dzmitry Lazerka
 */
public class JsonMapper extends ObjectMapper {
	private static final String TAG = JsonMapper.class.getName();

	public JsonMapper() {
		super();

		// Warn, but don't fail on unknown property.
		addHandler(new DeserializationProblemHandler() {
			@Override
			public boolean handleUnknownProperty(
					DeserializationContext deserializationContext,
					JsonParser jsonParser,
					JsonDeserializer<?> deserializer,
					Object beanOrClass,
					String propertyName
			) throws IOException {
				String msg = "Unknown property `" + propertyName + "` in " + beanOrClass;
				Log.w(TAG, msg);
				jsonParser.skipChildren();
				return true;
			}
		});
	}
}
