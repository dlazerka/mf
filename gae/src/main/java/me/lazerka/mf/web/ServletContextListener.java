package me.lazerka.mf.web;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import com.squarespace.jersey2.guice.JerseyGuiceServletContextListener;
import me.lazerka.mf.MainModule;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import java.util.List;

/**
 * @author Dzmitry Lazerka
 */
public class ServletContextListener extends JerseyGuiceServletContextListener {
	private static final Logger logger = LoggerFactory.getLogger(ServletContextListener.class);

	@Override
	public void contextInitialized(ServletContextEvent sce) {

		sce.getServletContext().setAttribute(ServerProperties.PROVIDER_PACKAGES, "asd");


		super.contextInitialized(sce);
	}

	@Override
	protected List<? extends Module> modules() {
		logger.trace("Lift off!");
		return ImmutableList.of(new MainModule());
	}

	/*
	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		logger.trace("contextInitialized");

		// Redirect java.util.logging through SLF4J.
		// Doesn't work in GAE, cause java.util.logging.LogManager is restricted.
		//SLF4JBridgeHandler.removeHandlersForRootLogger();
		//SLF4JBridgeHandler.install();

		ServletContext servletContext = servletContextEvent.getServletContext();

		Injector injector = Guice.createInjector();
		servletContext.setAttribute(INJECTOR_NAME, injector);
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		logger.trace("contextDestroyed");
		// App Engine does not currently invoke this method.
		ServletContext servletContext = servletContextEvent.getServletContext();
		servletContext.removeAttribute(INJECTOR_NAME);
	}
	*/
}
