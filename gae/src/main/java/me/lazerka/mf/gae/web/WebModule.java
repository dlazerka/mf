package me.lazerka.mf.gae.web;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.api.client.util.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.googlecode.objectify.ObjectifyFilter;
import com.googlecode.objectify.util.jackson.ObjectifyJacksonModule;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import me.lazerka.mf.api.JsonMapper;
import me.lazerka.mf.api.object.LocationRequest;
import me.lazerka.mf.gae.gcm.GcmModule;
import me.lazerka.mf.gae.oauth.AuthFilterFactory;
import me.lazerka.mf.gae.oauth.OauthModule;
import me.lazerka.mf.gae.web.rest.AcraExceptionResource;
import me.lazerka.mf.gae.web.rest.location.GcmRegistrationResource;
import me.lazerka.mf.gae.web.rest.location.LocationUpdateResource;
import me.lazerka.mf.gae.web.rest.user.UserResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
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

		install(new OauthModule());
		install(new GcmModule());

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
		Map<String, String> params = Maps.newHashMap();

		params.put(ServletContainer.RESOURCE_CONFIG_CLASS, ClassNamesResourceConfig.class.getName());

		// Handy, but slower to start.
		// params.put(PackagesResourceConfig.PROPERTY_PACKAGES, "me.lazerka.mf.gae.web");
		String classNames = Joiner.on(',').join(ImmutableList.of(
				UserResource.class.getName(),
				GcmRegistrationResource.class.getName(),
				LocationRequest.class.getName(),
				LocationUpdateResource.class.getName(),
				AcraExceptionResource.class.getName()
		));
		params.put(ClassNamesResourceConfig.PROPERTY_CLASSNAMES, classNames);

		// Read somewhere that it's needed for GAE.
		params.put(ResourceConfig.FEATURE_DISABLE_WADL, "true");

		// This makes use of custom Auth+filters using OAuth2.
		params.put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, AuthFilterFactory.class.getName());

		//params.put("com.sun.jersey.spi.container.ContainerRequestFilters", "com.sun.jersey.api.container.filter.LoggingFilter");
		//params.put("com.sun.jersey.spi.container.ContainerResponseFilters", "com.sun.jersey.api.container.filter.LoggingFilter");
		//params.put("com.sun.jersey.config.feature.logging.DisableEntitylogging", "true");
		//params.put("com.sun.jersey.config.feature.Trace", "true");
		return params;
	}
}
