package me.lazerka.mf;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.impl.translate.opt.joda.JodaTimeTranslators;
import me.lazerka.mf.entity.MfUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration for Objectify.
 *
 * @author Dzmitry Lazerka
 */
public class ObjectifyModule extends AbstractModule {
	private static final Logger logger = LoggerFactory.getLogger(ObjectifyModule.class);

	@Override
	protected void configure() {
		ObjectifyFactory factory = ObjectifyService.factory();

		// Install [de]serializers of Joda types.
		JodaTimeTranslators.add(factory);

		// From Objectify docs: example for setting up @Transact annotation (DIY).
		//bindInterceptor(Matchers.any(), Matchers.annotatedWith(Transact.class), new TransactInterceptor());

		factory.register(MfUser.class);

		logger.debug("Objectify set up.");
	}

	@Provides
	private Objectify provideOfy() {
		return ObjectifyService.ofy();
	}

}
