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

package me.lazerka.mf.android;

import android.content.SharedPreferences;
import android.net.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.PublishSubject;

import javax.annotation.Nonnull;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Friend list manager.
 */
public class FriendsManager {
	private static final Logger logger = LoggerFactory.getLogger(FriendsManager.class);

	private static final String KEY = "mf.friends";
	private final SharedPreferences preferences;

	private final PublishSubject<Change> changes = PublishSubject.create();

	protected FriendsManager(SharedPreferences preferences) {
		this.preferences = preferences;
	}

	@Nonnull
	public Set<Uri> getFriends() {
		Set<String> stringUris = preferences.getStringSet(KEY, Collections.<String>emptySet());
		LinkedHashSet<Uri> result = new LinkedHashSet<>(stringUris.size());
		for (String stringUri : stringUris) {
			result.add(Uri.parse(stringUri));
		}
		return Collections.unmodifiableSet(result);
	}

	public Observable<Change> observable() {
		return changes;
	}

	public boolean addFriend(Uri lookupUri) {
		String id = lookupUri.toString();

		logger.info("addFriend {}", id);
		synchronized (this) {
			// Clone, otherwise value won't be set.
			Set<String> friends = new LinkedHashSet<>(preferences.getStringSet(KEY, new HashSet<>(1)));

			boolean changed = friends.add(id);
			if (!changed) {
				logger.warn("Trying to add already friended {}", id);
				return false;
			}
			preferences.edit()
					.putStringSet(KEY, friends)
					.apply();
		}

		changes.onNext(new Change(Change.ADDED, lookupUri));

		return true;
	}

	public boolean removeFriend(Uri lookupUri) {
		String id = lookupUri.toString();

		logger.info("removeFriend {}", id);
		synchronized (this) {
			// Clone, otherwise value won't be set.
			Set<String> friends = new LinkedHashSet<>(preferences.getStringSet(KEY, new HashSet<>(0)));
			boolean changed = friends.remove(id);
			if (!changed) {
				logger.warn("Trying to remove nonexistent friend {}", id);
				return false;
			}
			preferences.edit()
					.putStringSet(KEY, friends)
					.apply();
		}

		changes.onNext(new Change(Change.REMOVED, lookupUri));

		return true;
	}

	public Set<String> getFriendsLookupKeys() {
		Set<Uri> friends = getFriends();
		LinkedHashSet<String> result = new LinkedHashSet<>(friends.size());
		for (Uri uri : friends) {
			result.add(toLookupKey(uri));
		}
		return result;
	}

	/**
	 * Converts "lookup URI" to "lookup key".
	 * Just like {@link android.provider.ContactsContract.Contacts#getLookupUri(long, String)}, but backwards.
	 *
	 * Uri is like "content://com.android.contacts/contacts/lookup/822ig%3A105666563920567332652/2379"
	 * Last segment is "_id" (unstable), and before that is "lookupKey" (stable).
	 *
	 * Hacky, but there's no official way of doing this.
	 */
	private static String toLookupKey(Uri lookupUri) {
		List<String> pathSegments = lookupUri.getPathSegments();
		checkArgument(pathSegments.get(1).equals("lookup"));
		// Need to encode, because they're stored that way.
		return Uri.encode(pathSegments.get(2));
	}

	public static class Change {
		public static final int ADDED = 1;
		public static final int REMOVED = 2;

		public final int action;

		public final Uri changed;

		private Change(int action, Uri changed) {
			this.action = action;
			this.changed = checkNotNull(changed);
		}
	}
}
