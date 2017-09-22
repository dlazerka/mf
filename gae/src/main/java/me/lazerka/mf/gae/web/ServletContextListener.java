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

package me.lazerka.mf.gae.web;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.lazerka.mf.gae.MainModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

/**
 * @author Dzmitry Lazerka
 */
public class ServletContextListener implements javax.servlet.ServletContextListener {
	private static final Logger logger = LoggerFactory.getLogger(ServletContextListener.class);

	private static final String INJECTOR_NAME = Injector.class.getName();

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		logger.trace("contextInitialized");

		// Redirect java.util.logging through SLF4J.
		// Doesn't work in GAE, cause java.util.logging.LogManager is restricted.
		//SLF4JBridgeHandler.removeHandlersForRootLogger();
		//SLF4JBridgeHandler.install();

		ServletContext servletContext = servletContextEvent.getServletContext();

		Injector injector = Guice.createInjector(new MainModule());
		servletContext.setAttribute(INJECTOR_NAME, injector);
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		logger.trace("contextDestroyed");
		// App Engine does not currently invoke this method.
		ServletContext servletContext = servletContextEvent.getServletContext();
		servletContext.removeAttribute(INJECTOR_NAME);
	}
}
