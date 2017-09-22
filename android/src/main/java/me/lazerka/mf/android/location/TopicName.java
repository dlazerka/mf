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

package me.lazerka.mf.android.location;

import com.google.common.base.Splitter;

import java.security.SecureRandom;
import java.util.List;

/**
 * TODO: "/topics/" ?
 *
 * Topic name must match [a-zA-Z0-9-_.~%]{1,900} .
 *
 * @author Dzmitry Lazerka
 */
public class TopicName {
	private static final char SEPARATOR = '-';

	private static final SecureRandom rng = new SecureRandom();

	private final String secretRandom;
	private final String friendLookupKey;

	public static TopicName parse(String topicName) {
		List<String> list = Splitter.on(SEPARATOR).splitToList(topicName);
		if (list.size() != 2) {
			throw new IllegalArgumentException(topicName);
		}

		return new TopicName(list.get(0), list.get(1));
	}

	private TopicName(String secretRandom, String friendLookupKey) {
		this.secretRandom = secretRandom;
		this.friendLookupKey = friendLookupKey;
	}

	public String getFriendLookupKey() {
		return friendLookupKey;
	}

	@Override
	public String toString() {
		return secretRandom + SEPARATOR + friendLookupKey;
	}

	public static TopicName random(String userId) {
		// 64 bits.
		String randomSecret = Integer.toHexString(rng.nextInt()) + Integer.toHexString(rng.nextInt());

		return new TopicName(randomSecret, userId);
	}
}
