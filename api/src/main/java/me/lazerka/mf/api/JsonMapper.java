package me.lazerka.mf.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 * Just an {@link ObjectMapper} with desired config, so that both API sides use the same feature set.
 *
 * @author Dzmitry Lazerka
 */
public class JsonMapper extends ObjectMapper {
	public JsonMapper() {
		disable(MapperFeature.AUTO_DETECT_GETTERS);
		disable(MapperFeature.AUTO_DETECT_IS_GETTERS);
		disable(MapperFeature.AUTO_DETECT_SETTERS);
		enable(MapperFeature.USE_GETTERS_AS_SETTERS); // default
		enable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS); // default

		enable(SerializationFeature.INDENT_OUTPUT);
		disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // default
		enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
		enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
		disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES); // default
		registerModule(new JodaModule());
	}
}
