package me.lazerka.mf.web;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.lazerka.mf.MainModule;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.jvnet.hk2.guice.bridge.api.HK2IntoGuiceBridge;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;

/**
 * @author Dzmitry Lazerka
 */
@ApplicationPath("/")
public class JerseyResourceConfig extends ResourceConfig {

	@Inject
	public JerseyResourceConfig(ServiceLocator serviceLocator, ServletContext servletContext) {

		// Tell where to scan for servlets.
		packages("me.lazerka.mf.web");

		// Tie with Guice.
		//Injector injector = (Injector) servletContext.getAttribute(Injector.class.getName());

		Injector injector = Guice.createInjector(
				new HK2IntoGuiceBridge(serviceLocator),
				new MainModule()
		);

		GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
		GuiceIntoHK2Bridge guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);
		guiceBridge.bridgeGuiceInjector(injector);
	}


}
