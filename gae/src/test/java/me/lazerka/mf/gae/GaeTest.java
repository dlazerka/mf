package me.lazerka.mf.gae;


import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.dev.HighRepJobPolicy;
import com.google.appengine.api.users.User;
import com.google.appengine.tools.development.Clock;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalUserServiceTestConfig;
import com.google.apphosting.api.ApiProxy;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import me.lazerka.mf.gae.entity.MfUser;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.testng.Assert.assertNotNull;

/**
 * Parent class for all tests that mock the whole GAE infrastructure.
 *
 * For pure unit-test see {@link MockObjectifyTest}.
 *
 * @author Dzmitry Lazerka
 */
public abstract class GaeTest {

	/// Injectable stuff.
	protected MfUser user;

	private final MyClock clock = new MyClock();

	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig()
					// Enable HRD.
					.setAlternateHighRepJobPolicyClass(MyHighRepJobPolicy.class),
			new LocalMemcacheServiceTestConfig(),
			new LocalUserServiceTestConfig()
			//new LocalTaskQueueTestConfig() mocking it
	).setClock(clock);
	private Closeable closeable;

	@BeforeClass
	public void setUpObjectify() {
		new ObjectifyModule().configure();

		//ObjectifyService.register(TestUserEntity.class);
	}

	@BeforeMethod
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
	}

	@BeforeMethod
	public void setUpGae() {
		helper.setUp();
		closeable = ObjectifyService.begin();
		assertNotNull(ApiProxy.getCurrentEnvironment());
	}

	@BeforeMethod
	public void storeFixture() {
		user = new MfUser(new User("test@example.com", "example.com", "123"));
		ofy().save().entity(user).now();
	}

	@AfterMethod
	public void tearDown() throws Exception {
		closeable.close();
		helper.tearDown();
	}

	protected void setCurrentTime(long ms) {
		clock.currentTime = ms;
	}

	protected void setHrdShouldApplyNewJob(boolean v) {
		MyHighRepJobPolicy.hrdShouldApplyNewJob = v;
	}

	protected void setHrdShouldRollForwardExistingJob(boolean v) {
		MyHighRepJobPolicy.hrdShouldRollForwardExistingJob = v;
	}

	private static class MyClock implements Clock {
		private long currentTime;

		@Override
		public long getCurrentTime() {
			return currentTime;
		}
	}

	private static class MyHighRepJobPolicy implements HighRepJobPolicy {
		private static boolean hrdShouldApplyNewJob = true;
		private static boolean hrdShouldRollForwardExistingJob = false;

		@Override
		public boolean shouldApplyNewJob(Key key) {
			return hrdShouldApplyNewJob;
		}

		@Override
		public boolean shouldRollForwardExistingJob(Key key) {
			return hrdShouldRollForwardExistingJob;
		}
	}
}
