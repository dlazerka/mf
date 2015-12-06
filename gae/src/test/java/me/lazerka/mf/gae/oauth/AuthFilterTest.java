package me.lazerka.mf.gae.oauth;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.api.utils.SystemProperty.Environment.Value;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sun.jersey.spi.container.ContainerRequest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.IObjectFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;

import static me.lazerka.mf.api.ApiConstants.COOKIE_NAME_AUTH_TOKEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

/**
 * @author Dzmitry Lazerka
 */
public class AuthFilterTest {
	String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJzdWIiOiIxMTAxNjk0ODQ0NzQzODYyNzYzMzQiLCJhenAiOiIxMDA4NzE5OTcwOTc4LWhiMjRuMmRzdGI0MG80NWQ0ZmV1bzJ1a3FtY2M2MzgxLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwiZW1haWxfdmVyaWZpZWQiOiJ0cnVlIiwiZW1haWwiOiJiaWxsZDE2MDBAZ21haWwuY29tIiwibmFtZSI6IlRlc3QgVGVzdCIsImF1ZCI6IjEwMDg3MTk5NzA5NzgtaGIyNG4yZHN0YjQwbzQ1ZDRmZXVvMnVrcW1jYzYzODEuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJpYXQiOjE0MzM5NzgzNTMsImV4cCI6MTQzMzk4MTk1M30.GC1hAjr8DbAT5CkEL19wCUqZHsDH1SklFPL2ZJxezW8";

	ContainerRequest request = mock(ContainerRequest.class);

	AuthFilter unit;

	@ObjectFactory
	public IObjectFactory getObjectFactory() {
		return new PowerMockObjectFactory();
	}

	@BeforeMethod
	public void setUp() throws URISyntaxException, IOException {
		unit = new AuthFilter();
		unit.tokenVerifier = mock(TokenVerifier.class);

		unit.setRoles(ImmutableSet.of(Role.OAUTH));
		unit.userService = mock(UserService.class);

		when(request.getRequestUri())
				.thenReturn(URI.create("https://example.com"));
	}

	@Test
	public void testFilterOk() throws GeneralSecurityException, IOException {
		when(request.isSecure()).thenReturn(true);
		Cookie cookie = new Cookie(COOKIE_NAME_AUTH_TOKEN, token);
		when(request.getCookies()).thenReturn(ImmutableMap.of(COOKIE_NAME_AUTH_TOKEN, cookie));
		SystemProperty.environment.set(Value.Production);

		when(unit.tokenVerifier.verify(token))
				.thenReturn(new OauthUser("123", "test@example.com"));

		unit.filter(request);

		verify(request).setSecurityContext(any(OauthSecurityContext.class));
	}

	@Test
	public void testFilterFail() throws GeneralSecurityException, IOException {
		when(request.isSecure()).thenReturn(true);
		Cookie cookie = new Cookie(COOKIE_NAME_AUTH_TOKEN, token);
		when(request.getCookies()).thenReturn(ImmutableMap.of(COOKIE_NAME_AUTH_TOKEN, cookie));
		SystemProperty.environment.set(Value.Production);

		when(unit.tokenVerifier.verify(token))
				.thenThrow(new InvalidKeyException("Test msg"));

		try {
			unit.filter(request);
			fail();
		} catch (WebApplicationException e) {
			assertThat(e.getCause(), instanceOf(InvalidKeyException.class));
			assertThat(e.getResponse().getStatus(), is(403));
			verify(request, never()).setSecurityContext(any(OauthSecurityContext.class));
		}
	}

	@Test
	public void testFilterInsecure() {
		SystemProperty.environment.set(Value.Production);
		when(request.isSecure()).thenReturn(false);

		try {
			unit.filter(request);
		} catch (WebApplicationException e) {
			assertThat(e.getResponse().getStatus(), is(403));
			return;
		}

		fail();
	}
}
