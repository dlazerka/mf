package me.lazerka.mf.gae.web;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.Maps;
import com.google.inject.Provides;
import com.googlecode.objectify.ObjectifyFilter;
import com.googlecode.objectify.util.jackson.ObjectifyJacksonModule;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import me.lazerka.mf.api.JsonMapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

/**
 * Web stuff configuration (servlets, filters, etc).
 *
 * @author Dzmitry Lazerka
 */
public class WebModule extends JerseyServletModule {
	private static final Logger logger = LoggerFactory.getLogger(WebModule.class);

	@Override
	protected void configureServlets() {
		logger.trace("configureServlets");

		// Objectify requires this while using Async+Caching
		// until https://code.google.com/p/googleappengine/issues/detail?id=4271 gets fixed.
		bind(ObjectifyFilter.class).in(Singleton.class);
		filter("/*").through(ObjectifyFilter.class);

		// Route all requests through GuiceContainer.
		serve("/*").with(GuiceContainer.class, getJerseyParams());
		serve("/_ah/*").with(GuiceContainer.class, getJerseyParams());
		//serve("/image/blobstore-callback-dev").with(BlobstoreCallbackServlet.class);

		setUpJackson();
	}

	private void setUpJackson() {
		// Handle "application/json" by Jackson.
		JsonMapper mapper = new JsonMapper();
		// Probably we don't want to serialize Ref in full, but as Key always.
		mapper.registerModule(new ObjectifyJacksonModule());

		JacksonJsonProvider provider = new JacksonJsonProvider(mapper);

		bind(JacksonJsonProvider.class).toInstance(provider);
	}

	private Map<String, String> getJerseyParams() {
		Map<String,String> params = Maps.newHashMap();

		params.put(PackagesResourceConfig.PROPERTY_PACKAGES, "me.lazerka.mf.gae.web");
		// Read somewhere that it's needed for GAE.
		params.put(PackagesResourceConfig.FEATURE_DISABLE_WADL, "true");

		// This makes use of custom Auth+filters using OAuth2.
		// Commented because using GAE default authentication.
		// params.put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, AuthFilterFactory.class.getName());

		//params.put("com.sun.jersey.spi.container.ContainerRequestFilters", "com.sun.jersey.api.container.filter.LoggingFilter");
		//params.put("com.sun.jersey.spi.container.ContainerResponseFilters", "com.sun.jersey.api.container.filter.LoggingFilter");
		//params.put("com.sun.jersey.config.feature.logging.DisableEntitylogging", "true");
		//params.put("com.sun.jersey.config.feature.Trace", "true");
		return params;
	}

	@Provides
	@Singleton
	@Named("gcm.api.key")
	String provideGcmApiKey() {
		File file = new File("WEB-INF/gcm.api.key");
		try {
			FileReader fr = new FileReader(file);
			String result = IOUtils.toString(fr).trim();
			if (result.isEmpty()) {
				throw new RuntimeException("File is empty: " + file.getAbsolutePath());
			}
			return result;
		} catch (FileNotFoundException e) {
			throw new RuntimeException("File " + file.getAbsolutePath() + " not found. " +
					"Put there Google Cloud Messaging API key obtained as described here " +
					"http://developer.android.com/google/gcm/gs.html");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}