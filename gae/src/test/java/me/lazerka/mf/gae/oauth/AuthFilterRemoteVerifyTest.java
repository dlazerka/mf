package me.lazerka.mf.gae.oauth;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.users.UserService;
import com.google.common.collect.ImmutableSet;
import com.sun.jersey.spi.container.ContainerRequest;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.fail;

/**
 * @author Dzmitry Lazerka
 */
public class AuthFilterRemoteVerifyTest {
	String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJzdWIiOiIxMTAxNjk0ODQ0NzQzODYyNzYzMzQiLCJhenAiOiIxMDA4NzE5OTcwOTc4LWhiMjRuMmRzdGI0MG80NWQ0ZmV1bzJ1a3FtY2M2MzgxLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwiZW1haWxfdmVyaWZpZWQiOiJ0cnVlIiwiZW1haWwiOiJiaWxsZDE2MDBAZ21haWwuY29tIiwibmFtZSI6IlRlc3QgVGVzdCIsImF1ZCI6IjEwMDg3MTk5NzA5NzgtaGIyNG4yZHN0YjQwbzQ1ZDRmZXVvMnVrcW1jYzYzODEuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJpYXQiOjE0MzM5NzgzNTMsImV4cCI6MTQzMzk4MTk1M30.GC1hAjr8DbAT5CkEL19wCUqZHsDH1SklFPL2ZJxezW8";

	ContainerRequest request = mock(ContainerRequest.class);
	HTTPResponse remoteResponse = mock(HTTPResponse.class);

	AuthFilterRemoteVerify unit;

	@BeforeMethod
	public void setUp() throws URISyntaxException, IOException {
		unit = new AuthFilterRemoteVerify();

		unit.setRoles(ImmutableSet.of(Role.OAUTH));
		unit.userService = mock(UserService.class);
		unit.urlFetchService = mock(URLFetchService.class);
		unit.jsonFactory = JacksonFactory.getDefaultInstance();
		unit.oauthClientId = "web-client-id.apps.googleusercontent.com";

		when(request.getRequestUri())
				.thenReturn(URI.create("https://example.com"));
		when(unit.urlFetchService.fetch(any(HTTPRequest.class)))
				.thenReturn(remoteResponse);

	}

	@Test
	public void testVerifyOk() throws Exception {
		byte[] content = IOUtils.toByteArray(getClass().getResource("remote-response.ok.json"));

		when(remoteResponse.getResponseCode()).thenReturn(200);
		when(remoteResponse.getContent()).thenReturn(content);

		unit.verify(token);
	}

	@Test
	public void testVerify() throws Exception {
		byte[] content = IOUtils.toByteArray(getClass().getResource("remote-response.invalid-value.json"));
		when(remoteResponse.getResponseCode()).thenReturn(403);
		when(remoteResponse.getContent()).thenReturn(content);

		try {
			unit.verify(token);
		} catch (InvalidKeyException e ){
			assertThat(e.getMessage(), containsString("Invalid Value"));
			return;
		}
		fail();
	}
}
