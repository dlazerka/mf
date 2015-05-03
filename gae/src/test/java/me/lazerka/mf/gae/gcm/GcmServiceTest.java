package me.lazerka.mf.gae.gcm;

import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.users.User;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import me.lazerka.mf.api.JsonMapper;
import me.lazerka.mf.api.gcm.GcmConstants;
import me.lazerka.mf.api.gcm.GcmPayload;
import me.lazerka.mf.api.object.LocationRequestResult.GcmResult;
import me.lazerka.mf.gae.GaeTest;
import me.lazerka.mf.gae.entity.GcmRegistrationEntity;
import me.lazerka.mf.gae.entity.MfUser;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.mockito.Answers;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

/**
 * @author Dzmitry Lazerka
 */
public class GcmServiceTest extends GaeTest {
	DateTime may1 = DateTime.parse("2015-05-01T15:01:46Z");

	@Mock
	URLFetchService urlFetchService;

	// Mocking because constructor is protected.
	@Mock(answer = Answers.CALLS_REAL_METHODS)
	HTTPResponse httpResponse;

	@Mock
	GcmPayload payload;

	MfUser recipient;
	GcmService unit;

	@BeforeMethod
	public void setUpUnit() {
		unit = new GcmService();
		unit.gcmApiKey = "test.key";
		unit.now = may1;
		unit.objectMapper = new JsonMapper();
		unit.urlFetchService = urlFetchService;
		unit.currentUser = user;
	}

	@BeforeMethod
	public void setUpRecipient() {
		recipient = new MfUser(new User("recipient@example.com", "example.com", "321recipient"));
		ofy().save().entity(new GcmRegistrationEntity(recipient, "gcmTestToken", may1));
		ofy().save().entity(recipient).now();
	}

	void initResponse(String fileName) throws Exception {
		when(urlFetchService.fetch(isA(HTTPRequest.class)))
				.thenReturn(httpResponse);

		InputStream is = checkNotNull(getClass().getResourceAsStream(fileName));
		byte[] json = IOUtils.toByteArray(is);
		when(httpResponse.getContent()).thenReturn(json);
		when(httpResponse.getResponseCode()).thenReturn(200);
	}

	@Test
	public void testSendSuccess() throws Exception {
		initResponse("GcmResponse.success.json");

		List<GcmResult> results = unit.send(recipient, payload);

		assertThat(results.get(0).getMessageId(), is("1:08"));
		assertThat(results.get(0).getError(), nullValue());
	}

	@Test
	public void testSendChangeRegistration() throws Exception {
		initResponse("GcmResponse.registration.json");

		List<GcmResult> results = unit.send(recipient, payload);

		assertThat(results.get(0).getMessageId(), is("1:2342"));
		assertThat(results.get(0).getError(), nullValue());
	}

	@Test
	public void testSendMultiple() throws Exception {
		List<GcmRegistrationEntity> registrations = ImmutableList.of(
				new GcmRegistrationEntity(recipient, "gcmTestToken2", may1),
				new GcmRegistrationEntity(recipient, "gcmTestToken3", may1),
				new GcmRegistrationEntity(recipient, "gcmTestToken4", may1),
				new GcmRegistrationEntity(recipient, "gcmTestToken5", may1),
				new GcmRegistrationEntity(recipient, "gcmTestToken6", may1));
		ofy().save().entities(registrations).now();

		initResponse("GcmResponse.error.json");

		List<GcmResult> results = unit.send(recipient, payload);

		assertThat(results.get(0).getMessageId(), is("1:0408"));
		assertThat(results.get(0).getError(), nullValue());

		assertThat(results.get(1).getMessageId(), nullValue());
		assertThat(results.get(1).getError(), is("Unavailable"));

		assertThat(results.get(2).getMessageId(), nullValue());
		assertThat(results.get(2).getError(), is("InvalidRegistration"));

		assertThat(results.get(3).getMessageId(), is("1:1516"));
		assertThat(results.get(3).getError(), nullValue());

		assertThat(results.get(4).getMessageId(), is("1:2342"));
		assertThat(results.get(4).getError(), nullValue());

		assertThat(results.get(5).getMessageId(), nullValue());
		assertThat(results.get(5).getError(), is(GcmConstants.ERROR_NOT_REGISTERED));

		List<GcmRegistrationEntity> newRegistrations =
				ofy().load().type(GcmRegistrationEntity.class).ancestor(recipient).list();

		ofy().flush();

		Set<String> newIds = new HashSet<>();
		for(GcmRegistrationEntity newRegistration : newRegistrations) {
			newIds.add(newRegistration.getId());
		}

		Set<String> expected = ImmutableSet.of(
				"gcmTestToken",
				"gcmTestToken2",
				"gcmTestToken4",
				"32"
		);
		assertThat(newIds, is(expected));
	}
}
