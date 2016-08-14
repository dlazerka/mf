/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2016 Dzmitry Lazerka dlazerka@gmail.com
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

package me.lazerka.mf.api;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * See {@link #normalizeEmail}.
 *
 * @author Dzmitry Lazerka
 */
public class EmailNormalized {
	/**
	 * Any period that is followed by @.
	 */
	private static final Pattern emailAddressSplitPattern = Pattern.compile("^(.*)(@.*)$");
	/**
	 * Just a single period.
	 */
	private static final Pattern periodRegex = Pattern.compile(".", Pattern.LITERAL);

	@Nonnull
	private final String email;

	/**
	 * Try to normalize email addresses by lowercasing domain part.
	 * <p>
	 * If we detect address is GMail one, we also apply GMail specific features normalizer.
	 * <p>
	 * If we cannot parse email, we log a warning and return non-normalized email instead of throwing an exception,
	 * because email addresses could be very tricky to parse, and there's no silver bullet despite RFCs.
	 */
	public static EmailNormalized normalizeEmail(String address) {
		Matcher matcher = emailAddressSplitPattern.matcher(address);
		if (!matcher.matches()) {
			return new EmailNormalized(address);
		}

		String localPart = matcher.group(1);
		String domainPart = matcher.group(2);

		domainPart = domainPart.toLowerCase(Locale.US);

		if (domainPart.equals("@gmail.com") || domainPart.equals("@googlemail.com")) {
			// Remove everything after plus sign (GMail-specific feature).
			int plusIndex = localPart.indexOf('+');
			if (plusIndex != -1) {
				localPart = localPart.substring(0, plusIndex);
			}

			// Remove periods.
			localPart = periodRegex.matcher(localPart).replaceAll("");

			// GMail addresses are case-insensitive.
			localPart = localPart.toLowerCase(Locale.US);
		}

		return new EmailNormalized(localPart + domainPart);
	}

	public EmailNormalized(@Nonnull String email) {
		this.email = checkNotNull(email);
	}

	@Nonnull
	public String getEmail() {
		return email;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EmailNormalized that = (EmailNormalized) o;
		return Objects.equals(email, that.email);
	}

	@Override
	public int hashCode() {
		return Objects.hash(email);
	}

	@Override
	public String toString() {
		return email;
	}
}
