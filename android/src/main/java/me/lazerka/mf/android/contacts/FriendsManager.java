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

package me.lazerka.mf.android.contacts;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.support.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.*;
import com.google.firebase.database.DatabaseReference.CompletionListener;
import me.lazerka.mf.android.AndroidTicker;
import me.lazerka.mf.android.PermissionAsker;
import me.lazerka.mf.android.adapter.PersonInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Friend list manager.
 */
public class FriendsManager {
	private static final Logger logger = LoggerFactory.getLogger(FriendsManager.class);

	private static DatabaseReference getFriendsReference(FirebaseUser currentUser) {
		return FirebaseDatabase.getInstance().getReference("private/" + currentUser.getUid() + "/friends");
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

	/**
	 * @param oneoff Emit onComplete() after first value received.
	 * @return Cold observable that never completes, but can terminate with error.
	 */
	public Observable<List<PersonInfo>> viewFriends(Context context, boolean oneoff) {
		return fetchFriendKeys(oneoff)
				.map(new FetchContactInfo(context));
	}

	Observable<List<String>> fetchFriendKeys(boolean oneoff) {
		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			logger.error("currentUser is null");
			IllegalStateException e = new IllegalStateException("FriendsManager.getFriends(): currentUser is null");
			FirebaseCrash.report(e);
			return Observable.error(e);
		};

		return Observable.create(new OnSubscribe<List<String>>() {
			@Override
			public void call(Subscriber<? super List<String>> subscriber) {
				Stopwatch stopwatch = AndroidTicker.started();
				DatabaseReference friendsReference = getFriendsReference(currentUser);

				ValueEventListener[] holder = new ValueEventListener[1];

				holder[0] = new ValueEventListener() {
					@Override
					public void onDataChange(DataSnapshot dataSnapshot) {
						logger.info("onDataChange in {}ms", stopwatch.elapsed(MILLISECONDS));

						@SuppressWarnings("unchecked")
						List<String> friendsLookupKeys = dataSnapshot.getValue(List.class);
						if (friendsLookupKeys != null) {
							subscriber.onNext(friendsLookupKeys);
						}

						if (oneoff) {
							if (holder[0] != null) {
								friendsReference.removeEventListener(holder[0]);
							}
							subscriber.onCompleted();
						}
					}

					@Override
					public void onCancelled(DatabaseError databaseError) {
						logger.info("onCancelled {}: {} in {}ms",
								databaseError.getMessage(),
								databaseError.getDetails(),
								stopwatch.elapsed(MILLISECONDS));

						// Subscriber should report the crash.
						subscriber.onError(databaseError.toException());
					}
				};

				friendsReference.addListenerForSingleValueEvent(holder[0]);
			}
		});

	}

	public void addFriend(Uri lookupUri) {
		String lookupKey = toLookupKey(lookupUri);
		logger.info("addFriend({})", lookupKey);

		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			logger.error("currentUser is null");
			FirebaseCrash.report(new IllegalStateException("FriendsManager.getFriends(): currentUser is null"));
		};

		Stopwatch stopwatch = AndroidTicker.started();
		getFriendsReference(currentUser)
			.push()
			.setValue(lookupKey)
			.addOnCompleteListener(new OnCompleteListener<Void>() {
				@Override
				public void onComplete(@NonNull Task<Void> task) {
					logger.info(
							"Adding friend {} successful={} in {}ms",
							lookupKey,
							task.isSuccessful(),
							stopwatch.elapsed(MILLISECONDS));
				}
			});
	}

	public void removeFriend(Uri lookupUri) {
		String lookupKey = toLookupKey(lookupUri);
		logger.info("Removing friend {}", lookupKey);

		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			logger.error("currentUser is null");
			FirebaseCrash.report(new IllegalStateException("FriendsManager.getFriends(): currentUser is null"));
		};

		Stopwatch stopwatch = AndroidTicker.started();
		getFriendsReference(currentUser)
			.child(lookupKey)
			.removeValue(new CompletionListener() {
				@Override
				public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
					logger.info(
							"Removing friend {} successful={} in {}ms",
							databaseError == null,
							stopwatch.elapsed(MILLISECONDS));
					if (databaseError != null) {
						FirebaseCrash.report(databaseError.toException());
					}
				}
			});
	}

	private static class FetchContactInfo implements Func1<List<String>, List<PersonInfo>> {
		private final Context context;

		public FetchContactInfo(Context context) {
			this.context = context;
		}

		@Override
		public List<PersonInfo> call(List<String> lookupKeys) {
			if (PermissionAsker.hasPermission(Manifest.permission.READ_CONTACTS, context)) {
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

			logger.info("Fetched {} contacts in {}ms", result.size(), stopwatch.elapsed(MILLISECONDS));

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
