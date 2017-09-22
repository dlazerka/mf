/*
 *     Copyright (C) 2017 Dzmitry Lazerka
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package me.lazerka.mf.api;

import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import static me.lazerka.mf.api.EmailNormalized.normalizeEmail;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Dzmitry Lazerka
 */
public class EmailNormalizedTest {
	@Test
	public void testCanonicalizeGmailAddress() throws Exception {
		assertThat(normalizeEmail("email@example.com"), Matchers.is(new EmailNormalized("email@example.com")));

		assertThat(normalizeEmail("email+extra@example.com"), Matchers.is(new EmailNormalized("email+extra@example.com")));
		assertThat(normalizeEmail("email+extra@gmail.com"), Matchers.is(new EmailNormalized("email@gmail.com")));

		assertThat(normalizeEmail("email+extra+another@example.com"),
				Matchers.is(new EmailNormalized("email+extra+another@example.com")));
		assertThat(normalizeEmail("email+extra+another@gmail.com"), Matchers.is(new EmailNormalized("email@gmail.com")));

		assertThat(normalizeEmail("first.middle.last@example.com"),
				Matchers.is(new EmailNormalized("first.middle.last@example.com")));
		assertThat(normalizeEmail("first.middle.last@gmail.com"),
				Matchers.is(new EmailNormalized("firstmiddlelast@gmail.com")));

		assertThat(normalizeEmail("CapitalCase@eXample.com"), Matchers.is(new EmailNormalized("CapitalCase@example.com")));
		assertThat(normalizeEmail("CapitalCase@GMail.com"), Matchers.is(new EmailNormalized("capitalcase@gmail.com")));

		// That's right, we accept invalid emails, because see unit javadoc.
		assertThat(normalizeEmail("invalid.email.COM"), Matchers.is(new EmailNormalized("invalid.email.COM")));
	}
}
