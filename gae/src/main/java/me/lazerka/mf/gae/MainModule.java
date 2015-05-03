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
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.impl.Keys;
import me.lazerka.mf.gae.entity.MfUser;
import me.lazerka.mf.gae.web.WebModule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

import static com.googlecode.objectify.ObjectifyService.ofy;

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

		logger.debug(MainModule.class.getSimpleName() + " set up.");
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

	/**
	 * Returns current user, creating entity if doesn't exist.
	 */
	@Provides
	private MfUser provideUser(UserService userService) {
		Keys keys = ObjectifyService.factory().keys();

		if (!userService.isUserLoggedIn()) {
			throw new IllegalStateException("User is not logged in");
		}

		final User user = userService.getCurrentUser();
		final Key<MfUser> key = keys.keyOf(new MfUser(user));

		return ofy().transact(new Work<MfUser>() {
			@Override
			public MfUser run() {
				MfUser mfUser = ofy().load().key(key).now();
				if (mfUser != null) {
					return mfUser;
				}
				mfUser = new MfUser(user);
				ofy().save().entity(mfUser).now();
				return mfUser;
			}
		});
	}

	@Provides
	@Named("now")
	private DateTime now() {
		return DateTime.now(DateTimeZone.UTC);
	}

}
