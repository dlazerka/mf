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

import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.mail.MailService;
import com.google.appengine.api.mail.MailServiceFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import me.lazerka.mf.gae.oauth.OauthModule;
import me.lazerka.mf.gae.web.WebModule;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Main project configuration.
 *
 * @author Dzmitry Lazerka
 */
public class MainModule extends AbstractModule {
	private static final Logger logger = LoggerFactory.getLogger(MainModule.class);

	@Override
	protected void configure() {
		install(new WebModule());

		install(new ObjectifyModule());
		useOfy();

		bindGaeServices();

		logger.info(MainModule.class.getSimpleName() + " set up.");
	}

	private void useOfy() {
		//bind(TradeService.class).to(TradeServiceOfy.class);
	}

	private void bindGaeServices() {
		bind(BlobstoreService.class).toInstance(BlobstoreServiceFactory.getBlobstoreService());
		bind(ChannelService.class).toInstance(ChannelServiceFactory.getChannelService());
		bind(DatastoreService.class).toInstance(DatastoreServiceFactory.getDatastoreService());
		bind(ImagesService.class).toInstance(ImagesServiceFactory.getImagesService());
		bind(MailService.class).toInstance(MailServiceFactory.getMailService());
		bind(MemcacheService.class).toInstance(MemcacheServiceFactory.getMemcacheService());
		bind(URLFetchService.class).toInstance(URLFetchServiceFactory.getURLFetchService());
		bind(UserService.class).toInstance(UserServiceFactory.getUserService());
	}

	@Provides
	@Named("now")
	private DateTime now() {
		return DateTime.now(DateTimeZone.UTC);
	}

	/**
	 * Reads whole file as a string.
	 */
	public static String readFileString(File file, String notFoundMsg) {
		logger.trace("Reading {}", file.getAbsolutePath());
		try (FileInputStream is = new FileInputStream(file)) {
			String result = IOUtils.toString(is, UTF_8).trim();
			if (result.isEmpty()) {
				throw new RuntimeException("File is empty: " + file.getAbsolutePath());
			}
			return result;
		} catch (FileNotFoundException e) {
			throw new RuntimeException("File " + file.getAbsolutePath() + " not found. " + notFoundMsg);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
