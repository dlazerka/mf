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

package me.lazerka.mf.gae.gcm;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Names;
import me.lazerka.mf.gae.MainModule;

import java.io.File;

/**
 * @author Dzmitry Lazerka
 */
public class GcmModule extends AbstractModule {
	public static final String GCM_API_KEY = "gcm.api.key";

	@Override
	protected void configure() {
		// Read config files early, so that errors would pop up on startup.
		bind(Key.get(String.class, Names.named(GCM_API_KEY)))
				.toInstance(readGcmApiKey());
	}

	String readGcmApiKey() {
		File file = new File("WEB-INF/keys/secret/gcm.api.key");
		String notFoundMsg = "Put there Google Cloud Messaging API key obtained as described here " +
				"http://developer.android.com/google/gcm/gs.html";
		return MainModule.readFileString(file, notFoundMsg);
	}
}
