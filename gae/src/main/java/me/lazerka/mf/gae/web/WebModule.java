/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2015 Dzmitry Lazerka dlazerka@gmail.com
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package me.lazerka.mf.gae.web;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.ImmutableMap;
import com.googlecode.objectify.ObjectifyFilter;
import com.googlecode.objectify.util.jackson.ObjectifyJacksonModule;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import me.lazerka.gae.jersey.oauth2.AuthFilterFactory;
import me.lazerka.gae.jersey.oauth2.OauthModule;
import me.lazerka.mf.api.JsonMapper;
import me.lazerka.mf.gae.gcm.GcmModule;
import me.lazerka.mf.gae.web.rest.location.GcmRegistrationResource;
import me.lazerka.mf.gae.web.rest.location.LocationRequestResource;
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

		// Map exceptions to fancy pages.
		bind(UnhandledExceptionMapper.class);

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

		bind(UserResource.class);
		bind(GcmRegistrationResource.class);
		bind(LocationRequestResource.class);
		bind(LocationUpdateResource.class);
	}

	private void setUpJackson() {
		// Handle "application/json" by Jackson.
		JsonMapper mapper = new JsonMapper();
		// Probably we don't want to serialize Ref in full, but as Key always.
		mapper.registerModule(new ObjectifyJacksonModule());

		JacksonJsonProvider provider = new JacksonJsonProvider(mapper);

		bind(JacksonJsonProvider.class).toInstance(provider);
	}

	/**
	 * Servlet parameters that usually go in web.xml servlet definition.
	 */
	private Map<String, String> getJerseyParams() {
		return ImmutableMap.of(
				// Speed up start up time. We don't use WADL anyway, we use annotations.
				ResourceConfig.FEATURE_DISABLE_WADL, "true",

				// Handy, but slower to start.
				// params.put(PackagesResourceConfig.PROPERTY_PACKAGES, "me.lazerka.mf.gae.web");

				// Another way to specify
				//params.put(ServletContainer.RESOURCE_CONFIG_CLASS, ClassNamesResourceConfig.class.getName());
				//String classNames = Joiner.on(',').join(ImmutableList.of(
				//		UserResource.class.getName(),
				//		...
				//));
				//params.put(ClassNamesResourceConfig.PROPERTY_CLASSNAMES, classNames);

						// This makes use of custom Auth+filters using OAuth2.
				ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, AuthFilterFactory.class.getName()

				//params.put("com.sun.jersey.spi.container.ContainerRequestFilters", "com.sun.jersey.api.container.filter.LoggingFilter");
				//params.put("com.sun.jersey.spi.container.ContainerResponseFilters", "com.sun.jersey.api.container.filter.LoggingFilter");
				//params.put("com.sun.jersey.config.feature.logging.DisableEntitylogging", "true");
				//params.put("com.sun.jersey.config.feature.Trace", "true");
		);
	}
}
