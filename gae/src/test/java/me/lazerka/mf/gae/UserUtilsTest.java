package me.lazerka.mf.gae;

import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Dzmitry Lazerka
 */
public class UserUtilsTest {

	@Test
	public void testCanonicalizeGmailAddress() throws Exception {
		assertThat(UserUtils.canonicalizeGmailAddress("email@example.com"), is("email@example.com"));

		assertThat(UserUtils.canonicalizeGmailAddress("email+extra@example.com"), is("email@example.com"));

		assertThat(UserUtils.canonicalizeGmailAddress("email+extra+another@example.com"), is("email@example.com"));

		assertThat(
				UserUtils.canonicalizeGmailAddress(
						"first.middle.last@example.com"), is("firstmiddlelast@example.com"));

		assertThat(
				UserUtils.canonicalizeGmailAddress("CapitalCase@eXample.com"), is(
						"capitalcase@example.com"));

		assertThat(UserUtils.canonicalizeGmailAddress("allTogether+here@exampLe.com"), is("alltogether@example.com"));
	}
}
