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

package me.lazerka.mf.android.contacts;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.support.annotation.NonNull;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.firebase.crash.FirebaseCrash;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import me.lazerka.mf.android.AndroidTicker;
import me.lazerka.mf.android.PermissionAsker;
import me.lazerka.mf.android.adapter.PersonInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Friend list manager.
 *
 * We store full lookup-uri in SharedProperties,
 * like "content://com.android.contacts/contacts/lookup/1715ia0e28ef087ba578/983"
 *
 * But for querying, we need only lookup-key (1715ia0e28ef087ba578).
 * The last path part (983) is unstable row id which we don't use.
 */
public class FriendsManager {
	private static final Logger logger = LoggerFactory.getLogger(FriendsManager.class);

	private static final String KEY = "mf.friends";
	private final SharedPreferences preferences;
	private final Context context;

	private final PublishSubject<Void> changes = PublishSubject.create();

	public FriendsManager(SharedPreferences preferences, Context context) {
		this.preferences = preferences;
		this.context = context;
	}

	@Nonnull
	public Set<Uri> getFriendsLookupUris() {
		Set<String> stringUris = preferences.getStringSet(KEY, Collections.<String>emptySet());
		Set<Uri> result = new LinkedHashSet<>(stringUris.size());
		for (String stringUri : stringUris) {
			result.add(Uri.parse(stringUri));
		}
		return Collections.unmodifiableSet(result);
	}

	public List<String> getFriendsLookupKeys() {
		Set<Uri> friends = getFriendsLookupUris();
		LinkedHashSet<String> result = new LinkedHashSet<>(friends.size());
		for (Uri uri : friends) {
			result.add(toLookupKey(uri));
		}
		return ImmutableList.copyOf(result);
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

	public Future<List<PersonInfo>> getFriends() {
		return watchFriends()
				.firstOrError()
				.toFuture();
	}

	/**
	 * @return Observable that never completes, but can terminate with error.
	 */
	public Observable<List<PersonInfo>> watchFriends() {
		return watchFriendKeys()
				.observeOn(Schedulers.io())
				.map(new FetchContactInfo());
	}

	private Observable<List<String>> watchFriendKeys() {
		return changes
			.map(new Function<Void, List<String>>() {
				@Override
				public List<String> apply(Void aVoid) throws Exception {
					return getFriendsLookupKeys();
				}
			})
			// Return current first.
			.startWith(getFriendsLookupKeys());
	}

	/**
	 * @return Whether friend list changed.
	 */
	public boolean addFriend(Uri lookupUri) {
		logger.info("addFriend({}): {}", lookupUri);

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

		changes.onNext(null);

		return true;
	}

	/**
	 * @return Whether friend list changed.
	 */
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

		changes.onNext(null);

		return true;
	}

	public PersonInfo getFriend(String lookupKey) {
		List<PersonInfo> infos = new FetchContactInfo()
				.apply(ImmutableList.of(lookupKey));
		if (infos == null || infos.isEmpty()) {
			return null;
		} else {
			return infos.get(0);
		}
	}

	private class FetchContactInfo implements Function<List<String>, List<PersonInfo>> {
		@Override
		public List<PersonInfo> apply(List<String> lookupKeys) {
			// No need to check for permission when it's empty. Useful on first ever start.
			if (lookupKeys.isEmpty()) {
				return Collections.emptyList();
			}

			if (!PermissionAsker.hasPermission(Manifest.permission.READ_CONTACTS, context)) {
				FirebaseCrash.report(new IllegalStateException("No READ_CONTACTS perm while loading contacts"));
				logger.error("No READ_CONTACTS perm while loading contacts");
				return Collections.emptyList();
			}

			Stopwatch stopwatch = AndroidTicker.started();

			ContentResolver contentResolver = context.getContentResolver();

			// "?,?,?,?" as many as there are lookupUris
			String placeholders = Joiner.on(',').useForNull("?").join(new String[lookupKeys.size()]);
			// Values for query.
			String[] selectionArgs = lookupKeys.toArray(new String[lookupKeys.size()]);

			SetMultimap<String, String> allEmails = fetchEmails(contentResolver, placeholders, selectionArgs);

			List<PersonInfo> result = Collections.emptyList();
			{
				Cursor contactsCursor = contentResolver.query(
						Contacts.CONTENT_URI,
						new String[]{
								Contacts._ID,
								Contacts.LOOKUP_KEY,
								Contacts.DISPLAY_NAME_PRIMARY,
								Contacts.PHOTO_URI,
						},
						Contacts.LOOKUP_KEY + " IN (" + placeholders + ")",
						selectionArgs,
						Contacts.SORT_KEY_PRIMARY,
						null);
				if (contactsCursor != null) {
					try {
						// Handle contacts.
						result = new ArrayList<>(contactsCursor.getCount());
						for(contactsCursor.moveToFirst(); !contactsCursor.isAfterLast(); contactsCursor.moveToNext()) {
							long id = checkNotNull(contactsCursor.getLong(0));
							String lookupKey = checkNotNull(contactsCursor.getString(1));
							String displayName = checkNotNull(contactsCursor.getString(2));
							String photoUri = contactsCursor.getString(3);
							Uri lookupUri = Contacts.getLookupUri(id, lookupKey);
							Set<String> emails = allEmails.get(lookupKey);

							result.add(new PersonInfo(
									id,
									lookupKey,
									lookupUri,
									displayName,
									photoUri,
									emails
							));
						}
					} finally {
						contactsCursor.close();
					}
				}
			}

			logger.info("Fetched {} contacts for {} keys in {}ms",
					result.size(), lookupKeys.size(), stopwatch.elapsed(MILLISECONDS));

			return result;
		}

		@NonNull
		private SetMultimap<String, String> fetchEmails(
				ContentResolver contentResolver,
				String placeholders,
				String[] selectionArgs)
		{
			// lookupKey -> email
			SetMultimap<String, String> allEmails = LinkedHashMultimap.create();
			{
				Cursor emailsCursor = contentResolver.query(
						Email.CONTENT_URI,
						new String[]{
								Email.LOOKUP_KEY,
								Email.ADDRESS,
						},
						Email.LOOKUP_KEY + " IN (" + placeholders + ")",
						selectionArgs,
						Email.SORT_KEY_PRIMARY,
						null);


				if (emailsCursor != null) {
					try {
						for(emailsCursor.moveToFirst(); !emailsCursor.isAfterLast(); emailsCursor.moveToNext()) {
							String lookupKey = checkNotNull(emailsCursor.getString(0));
							String email = checkNotNull(emailsCursor.getString(1));
							allEmails.put(lookupKey, email);
						}
					} finally {
						emailsCursor.close();
					}
				}
			}
			return allEmails;
		}
	}
}
