package me.lazerka.mf.gae;

import com.google.inject.AbstractModule;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.impl.translate.opt.joda.JodaTimeTranslators;
import me.lazerka.mf.gae.entity.AcraExceptionEntity;
import me.lazerka.mf.gae.entity.MfUser;
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

		registerEntities(factory);

		logger.debug("Objectify set up.");
	}

	private void registerEntities(ObjectifyFactory factory) {
		factory.register(AcraExceptionEntity.class);
		factory.register(MfUser.class);
	}
}