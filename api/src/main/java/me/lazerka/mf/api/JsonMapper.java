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
