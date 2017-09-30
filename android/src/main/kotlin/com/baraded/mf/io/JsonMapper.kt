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

package com.baraded.mf.io

import com.baraded.mf.logging.LogService
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler

object JsonMapper : me.lazerka.mf.api.JsonMapper() {
    val log = LogService.getLogger(JsonMapper::class.java)

    init {
        // Warn, but don't fail on unknown property.
        addHandler(object : DeserializationProblemHandler() {
            override fun handleUnknownProperty(
                    deserializationContext: DeserializationContext?,
                    jsonParser: JsonParser?,
                    deserializer: JsonDeserializer<*>?,
                    beanOrClass: Any?,
                    propertyName: String?
            ): Boolean {
                val msg = "Unknown property `$propertyName` in $beanOrClass"
                log.warn(msg)
                jsonParser?.skipChildren()
                return true
            }
        })

    }

}