package me.lazerka.mf.gae;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dzmitry Lazerka
 */
public class UserUtils {

	/** Any period that is followed by @. */
	private static final Pattern emailAddressSplitPattern = Pattern.compile("^(.*)(@.*)$");

	/**
	 * Pretty naive email normalizer for GMail.
	 *
	 * GMail emails are case-insensitive, although standard says otherwise.
	 * Currently we work only with GMail authentication, so following GMail logic.
	 */
	public static String canonicalizeGmailAddress(String address) {
		Matcher matcher = emailAddressSplitPattern.matcher(address);
		if (!matcher.matches()) {
			throw new IllegalArgumentException(address);
		}

		String localPart = matcher.group(1);

		// Remove everything after plus sign (GMail-specific feature).
		int plusIndex = localPart.indexOf('+');
		if (plusIndex != -1) {
			localPart = localPart.substring(0, plusIndex);
		}

		String periodless = localPart.replace(".", "") + matcher.group(2);

		return periodless.toLowerCase(Locale.US);
	}
}
