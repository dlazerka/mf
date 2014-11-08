package me.lazerka.mf.web;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.Provides;
import com.google.inject.servlet.ServletModule;
import com.googlecode.objectify.ObjectifyFilter;
import com.googlecode.objectify.util.jackson.ObjectifyJacksonModule;
import me.lazerka.mf.api.JsonMapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Web stuff configuration (servlets, filters, etc).
 *
 * @author Dzmitry Lazerka
 */
public class WebModule extends ServletModule {
	private static final Logger logger = LoggerFactory.getLogger(WebModule.class);

	@Override
	protected void configureServlets() {
		logger.trace("configureServlets");

		// Objectify requires this while using Async+Caching
		// until https://code.google.com/p/googleappengine/issues/detail?id=4271 gets fixed.
		bind(ObjectifyFilter.class).in(Singleton.class);
		filter("/*").through(ObjectifyFilter.class);

		setUpJackson();

		// Jersey.

		//serve("/image/blobstore-callback-dev").with(BlobstoreCallbackServlet.class);

		//filter("/*").through(ServletContainer.class, ImmutableMap.of(
		//		ServletProperties.JAXRS_APPLICATION_CLASS, JerseyResourceConfig.class.getName()
		//));
	}

	private void setUpJackson() {
		// Handle "application/json" by Jackson.
		JsonMapper mapper = new JsonMapper();
		// Probably we don't want to serialize Ref as it's object, but as Key always.
		mapper.registerModule(new ObjectifyJacksonModule());

		JacksonJsonProvider provider = new JacksonJsonProvider(mapper);

		 /* bind jackson converters for JAXB/JSON serialization */
		bind(MessageBodyReader.class).toInstance(provider);
		bind(MessageBodyWriter.class).toInstance(provider);
	}
	/*

	private Map<String, String> getJerseyParams() {
		Map<String,String> params = Maps.newHashMap();

		params.put(PackagesResourceConfig.PROPERTY_PACKAGES, "me.lazerka.mf.web");
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


	private Map<String, String> getJersey2Params() {

		final ResourceConfig rc = new ResourceConfig().packages("com.example");

		// create and start a new instance of grizzly http server
		// exposing the Jersey application at BASE_URI
		return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);


		Map<String,String> params = Maps.newHashMap();

		params.put(PackagesResourceConfig.PROPERTY_PACKAGES, "me.lazerka.mf.web");
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
	*/

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
