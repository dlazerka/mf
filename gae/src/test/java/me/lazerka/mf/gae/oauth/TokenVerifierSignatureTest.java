package me.lazerka.mf.gae.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.fail;

/**
 * Using PowerMock because Google API libraries are test-hostile (final methods).
 *
 * Instead of extending PowerMockTestCase we could use @ObjectFactory, but that may trigger PowerMock bug
 * https://github.com/jayway/powermock/issues/434
 *
 * @author Dzmitry Lazerka
 */
@PrepareForTest(value = {GoogleIdTokenVerifier.class, GoogleIdToken.class})
public class TokenVerifierSignatureTest extends PowerMockTestCase {
	String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJzdWIiOiIxMTAxNjk0ODQ0NzQzODYyNzYzMzQiLCJhenAiOiIxMDA4NzE5OTcwOTc4LWhiMjRuMmRzdGI0MG80NWQ0ZmV1bzJ1a3FtY2M2MzgxLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwiZW1haWxfdmVyaWZpZWQiOiJ0cnVlIiwiZW1haWwiOiJiaWxsZDE2MDBAZ21haWwuY29tIiwibmFtZSI6IlRlc3QgVGVzdCIsImF1ZCI6IjEwMDg3MTk5NzA5NzgtaGIyNG4yZHN0YjQwbzQ1ZDRmZXVvMnVrcW1jYzYzODEuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJpYXQiOjE0MzM5NzgzNTMsImV4cCI6MTQzMzk4MTk1M30.GC1hAjr8DbAT5CkEL19wCUqZHsDH1SklFPL2ZJxezW8";

	TokenVerifierSignature unit;

	@BeforeMethod
	public void setUp() throws URISyntaxException, IOException {
		unit = new TokenVerifierSignature();
		unit.tokenVerifier = mock(GoogleIdTokenVerifier.class);
		when(unit.tokenVerifier.getJsonFactory())
				.thenReturn(JacksonFactory.getDefaultInstance());
		mockStatic(GoogleIdToken.class);
	}

	@Test
	public void testVerifyOk() throws Exception {
		GoogleIdToken idToken = mock(GoogleIdToken.class);
		when(idToken.getPayload())
				.thenReturn(new Payload().setSubject("1234").setEmail("test@example.com"));
		when(GoogleIdToken.parse(any(JsonFactory.class), any(String.class)))
				.thenReturn(idToken);
		when(unit.tokenVerifier.verify(idToken))
				.thenReturn(true);

		unit.verify(token);
	}

	@Test
	public void testVerify() throws Exception {
		when(unit.tokenVerifier.verify(token))
				.thenReturn(null);

		try {
			unit.verify(token);
		} catch (InvalidKeyException e ){
			assertThat(e.getMessage(), containsString("billd1600@gmail.com"));
			return;
		}
		fail();
	}
}
