package me.lazerka.mf.gae.user;

import org.testng.annotations.Test;

import static me.lazerka.mf.gae.user.UserService.normalizeEmail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Dzmitry Lazerka
 */
public class UserServiceTest {
	@Test
	public void testCanonicalizeGmailAddress() throws Exception {
		assertThat(normalizeEmail("email@example.com"), is(new EmailNormalized("email@example.com")));

		assertThat(normalizeEmail("email+extra@example.com"), is(new EmailNormalized("email+extra@example.com")));
		assertThat(normalizeEmail("email+extra@gmail.com"), is(new EmailNormalized("email@gmail.com")));

		assertThat(normalizeEmail("email+extra+another@example.com"),
				is(new EmailNormalized("email+extra+another@example.com")));
		assertThat(normalizeEmail("email+extra+another@gmail.com"), is(new EmailNormalized("email@gmail.com")));

		assertThat(normalizeEmail("first.middle.last@example.com"),
				is(new EmailNormalized("first.middle.last@example.com")));
		assertThat(normalizeEmail("first.middle.last@gmail.com"),
				is(new EmailNormalized("firstmiddlelast@gmail.com")));

		assertThat(normalizeEmail("CapitalCase@eXample.com"), is(new EmailNormalized("CapitalCase@example.com")));
		assertThat(normalizeEmail("CapitalCase@GMail.com"), is(new EmailNormalized("capitalcase@gmail.com")));

		// That's right, we accept invalid emails, because see unit javadoc.
		assertThat(normalizeEmail("invalid.email.COM"), is(new EmailNormalized("invalid.email.COM")));
	}
}
