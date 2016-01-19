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

package me.lazerka.mf.android.adapter;

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Looper;
import android.os.OperationCanceledException;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.support.annotation.WorkerThread;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.FriendsManager;
import rx.Subscription;
import rx.functions.Action1;
import rx.observers.Subscribers;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Loads contacts along with their Emails.
 *
 * Almost a copy-paste of {@link CursorLoader}, but for two {@link Cursor}s.
 * Our job is a little easier, because we read the whole cursor at once.
 *
 * @author Dzmitry Lazerka
 */
public class FriendsLoader extends AsyncTaskLoader<List<PersonInfo>> {
	private static final Logger logger = LoggerFactory.getLogger(FriendsLoader.class);

	private final ForceLoadContentObserver observer;

	private CancellationSignal contactsCancellationSignal;
	private CancellationSignal emailCancellationSignal;

	private final FriendsManager friendsManager = Application.friendsManager;
	private Subscription subscription;

	public FriendsLoader(Context context) {
		super(context);

		// Can be called from background.
		if (Looper.myLooper() != Looper.getMainLooper()) {
			Looper.prepare();
		}

		// Must come after Looper.prepare().
		observer = new ForceLoadContentObserver();
	}

	@WorkerThread
	@Override
	public ArrayList<PersonInfo> loadInBackground() {
		synchronized (this) {
			if (isLoadInBackgroundCanceled()) {
				throw new OperationCanceledException();
			}
			contactsCancellationSignal = new CancellationSignal();
			emailCancellationSignal = new CancellationSignal();
		}

		Set<String> lookupKeys = friendsManager.getFriendsLookupKeys();

		// "?,?,?,?" as many as there are lookupUris
		String placeholders = Joiner.on(',').useForNull("?").join(new String[lookupKeys.size()]);

		String[] selectionArgs = lookupKeys.toArray(new String[lookupKeys.size()]);

		try {
			ContentResolver contentResolver = getContext().getContentResolver();
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
					contactsCancellationSignal);
			Cursor emailsCursor = contentResolver.query(
					Email.CONTENT_URI,
					new String[]{
							Email.LOOKUP_KEY,
							Email.ADDRESS,
					},
					Email.LOOKUP_KEY + " IN (" + placeholders + ")",
					selectionArgs,
					Email.SORT_KEY_PRIMARY,
					emailCancellationSignal);

			ArrayList<PersonInfo> result = null;

			if (contactsCursor != null && emailsCursor != null) {
				try {
					// Ensure the cursor window is filled.
					contactsCursor.getCount();
					contactsCursor.registerContentObserver(observer);
					emailsCursor.getCount();
					emailsCursor.registerContentObserver(observer);

					// Handle emails.
					SetMultimap<String, String> allEmails = LinkedHashMultimap.create();
					for (emailsCursor.moveToFirst(); !emailsCursor.isAfterLast(); emailsCursor.moveToNext()) {
						String lookupKey = checkNotNull(emailsCursor.getString(0));
						String email = checkNotNull(emailsCursor.getString(1));
						allEmails.put(lookupKey, email);
					}

					// Handle contacts.

					result = new ArrayList<>(contactsCursor.getCount());
					for (contactsCursor.moveToFirst(); !contactsCursor.isAfterLast(); contactsCursor.moveToNext()) {
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
					emailsCursor.close();
				}
			}

			return result;

		} finally {
			synchronized (this) {
				contactsCancellationSignal = null;
				emailCancellationSignal = null;
			}
		}
	}

	@Override
	public void cancelLoadInBackground() {
		super.cancelLoadInBackground();
		synchronized (this) {
			if (contactsCancellationSignal != null) {
				contactsCancellationSignal.cancel();
			}
			if (emailCancellationSignal != null) {
				emailCancellationSignal.cancel();
			}
		}
	}

	// No deliverResult(), because we've read all the data at once.

	@Override
	protected void onStartLoading() {
		if (subscription == null) {
			subscription = friendsManager.observable()
					// Multithreaded OK.
					//.subscribeOn(AndroidSchedulers.mainThread())
					//.observeOn(AndroidSchedulers.mainThread())
					.subscribe(Subscribers.create(new Action1<FriendsManager.Change>() {
						@Override
						public void call(FriendsManager.Change change) {
							logger.info("Detected friends list changed, firing onChange.");
							observer.onChange(true);
						}
					}));
		}

		if (isStarted()) {
			takeContentChanged();
			forceLoad();
		}
	}

	/**
	 * Called when e.g. Activity is stopped.
	 */
	@Override
	protected void onStopLoading() {
		cancelLoad();
	}

	@Override
	public void onCanceled(List<PersonInfo> data) {
		super.onCanceled(data);
		// Nothing to release for `data`.
	}

	@Override
	protected void onReset() {
		super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

		if (subscription != null) {
			subscription.unsubscribe();
			subscription = null;
		}
	}
}
