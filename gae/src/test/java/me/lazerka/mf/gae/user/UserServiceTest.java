/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2015 Dzmitry Lazerka dlazerka@gmail.com
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

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
