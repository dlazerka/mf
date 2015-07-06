package me.lazerka.mf.gae;

import me.lazerka.mf.gae.UserUtils.IllegalEmailFormatException;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.fail;

/**
 * @author Dzmitry Lazerka
 */
public class UserUtilsTest {

	@Test
	public void testCanonicalizeGmailAddress() throws Exception {
		assertThat(UserUtils.normalizeGmailAddress("email@example.com"), is("email@example.com"));

		assertThat(UserUtils.normalizeGmailAddress("email+extra@example.com"), is("email@example.com"));

		assertThat(UserUtils.normalizeGmailAddress("email+extra+another@example.com"), is("email@example.com"));

		assertThat(
				UserUtils.normalizeGmailAddress(
						"first.middle.last@example.com"), is("firstmiddlelast@example.com"));

		assertThat(
				UserUtils.normalizeGmailAddress("CapitalCase@eXample.com"), is(
						"capitalcase@example.com"));

		assertThat(UserUtils.normalizeGmailAddress("allTogether+here@exampLe.com"), is("alltogether@example.com"));

		try {
			UserUtils.normalizeGmailAddress("wrong.email.com");
			fail();
		} catch (IllegalEmailFormatException e) {
			// ok
		}
	}
}
