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

package me.lazerka.mf.gae;

import com.google.inject.AbstractModule;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.impl.translate.opt.joda.JodaTimeTranslators;
import me.lazerka.mf.gae.entity.GcmRegistrationEntity;
import me.lazerka.mf.gae.user.MfUser;
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
		factory.register(MfUser.class);
		factory.register(GcmRegistrationEntity.class);
	}
}
