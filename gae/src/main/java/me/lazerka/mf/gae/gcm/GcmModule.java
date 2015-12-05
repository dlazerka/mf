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
		File file = new File("WEB-INF/secret/gcm.api.key");
		String notFoundMsg = "Put there Google Cloud Messaging API key obtained as described here " +
				"http://developer.android.com/google/gcm/gs.html";
		return MainModule.readFileString(file, notFoundMsg);
	}
}
