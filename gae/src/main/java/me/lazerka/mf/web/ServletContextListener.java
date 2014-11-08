package me.lazerka.mf.web;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import me.lazerka.mf.MainModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

/**
 * @author Dzmitry Lazerka
 */
public class ServletContextListener extends GuiceServletContextListener {
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

		Injector injector = Guice.createInjector(
				//new HK2IntoGuiceBridge(serviceLocator),
				new MainModule()
		);
		servletContext.setAttribute(INJECTOR_NAME, injector);
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		logger.trace("contextDestroyed");
		// App Engine does not currently invoke this method.
		ServletContext servletContext = servletContextEvent.getServletContext();
		servletContext.removeAttribute(INJECTOR_NAME);
	}

	@Override
	protected Injector getInjector() {
		logger.trace("Lift off! Creating Guice Injector.");
		return Guice.createInjector(new MainModule());
	}
}
