package me.lazerka.mf.gae.gcm;

import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.users.User;
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
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
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

	GcmService unit;
	MfUser recipient;

	@BeforeMethod
	public void setUpUnit() {
		unit = new GcmService();
		unit.gcmApiKey = "test.key";
		unit.now = may1;
		unit.objectMapper = new JsonMapper();
		unit.urlFetchService = urlFetchService;
		unit.user = new MfUser(new User("test@example.com", "example.com", "12345"));
	}

	@BeforeMethod
	public void setUpRecipient() {
		recipient = new MfUser(new User("test@example.com", "example.com", "01325"));
		recipient.getGcmRegistrationEntities().add(new GcmRegistrationEntity("gcmTestToken", may1.minusHours(1)));
		ofy().save().entity(recipient);
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
		assertThat(results.get(0).getDeviceRegistrationHash(), startsWith("e29c9c18"));
		assertThat(results.get(0).getError(), nullValue());
	}

	@Test
	public void testSendMultiple() throws Exception {
		recipient.getGcmRegistrationEntities().add(new GcmRegistrationEntity("gcmTestToken2", may1.minusHours(1)));
		recipient.getGcmRegistrationEntities().add(new GcmRegistrationEntity("gcmTestToken3", may1.minusHours(1)));
		recipient.getGcmRegistrationEntities().add(new GcmRegistrationEntity("gcmTestToken4", may1.minusHours(1)));
		recipient.getGcmRegistrationEntities().add(new GcmRegistrationEntity("gcmTestToken5", may1.minusHours(1)));
		recipient.getGcmRegistrationEntities().add(new GcmRegistrationEntity("gcmTestToken6", may1.minusHours(1)));

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
		assertThat(results.get(4).getDeviceRegistrationHash(), startsWith("e29"));
		assertThat(results.get(4).getError(), nullValue());

		assertThat(results.get(5).getMessageId(), nullValue());
		assertThat(results.get(5).getError(), is(GcmConstants.ERROR_NOT_REGISTERED));

		List<String> ids = recipient.getGcmRegistrationIds();
		assertThat(ids, not(contains("gcmTestToken6")));
	}
}
