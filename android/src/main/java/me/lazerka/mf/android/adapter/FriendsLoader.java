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
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.FriendsService;
import rx.Subscription;
import rx.functions.Action1;
import rx.observers.Subscribers;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Combination of two CursorLoaders -- for contacts, and for their emails.
 *
 * Chain down to CursorLoaders all and only methods invoked in background. Handles foreground methods itself.
 *
 * @author Dzmitry Lazerka
 */
public class FriendsLoader extends AsyncTaskLoader<List<PersonInfo>> {
	private static final Logger logger = LoggerFactory.getLogger(FriendsLoader.class);

	private final ForceLoadContentObserver observer = new ForceLoadContentObserver();

	private CursorLoader contactsLoader;
	private CursorLoader emailsLoader;

	private final FriendsService friendsService = Application.friendsService;
	private Subscription subscription;

	public FriendsLoader(Context context) {
		super(context);

		// Can be called from background.
		if (Looper.myLooper() != Looper.getMainLooper()) {
			Looper.prepare();
		}
	}

	@Override
	public ArrayList<PersonInfo> loadInBackground() {
		Set<String> lookupKeys = friendsService.getFriendsLookupKeys();

		// "?,?,?,?" as many as there are lookupUris
		String placeholders = Joiner.on(',').useForNull("?").join(new String[lookupKeys.size()]);

		String[] selectionArgs = lookupKeys.toArray(new String[lookupKeys.size()]);

		contactsLoader = new CursorLoader(
				getContext(),
				Contacts.CONTENT_URI, // Table to query
				new String[]{
						Contacts._ID,
						Contacts.LOOKUP_KEY,
						Contacts.DISPLAY_NAME_PRIMARY,
						Contacts.PHOTO_URI,
				}, // Projection to return
				Contacts.LOOKUP_KEY + " IN (" + placeholders + ")", // No selection clause
				selectionArgs, // No selection arguments
				Contacts.SORT_KEY_PRIMARY
		);

		emailsLoader = new CursorLoader(
				getContext(),
				Email.CONTENT_URI, // Table to query
				new String[]{
						Email.LOOKUP_KEY,
						Email.ADDRESS
				}, // Projection to return
				Email.LOOKUP_KEY + " IN (" + placeholders + ")", // No selection clause
				selectionArgs, // No selection arguments
				Email.SORT_KEY_PRIMARY
		);

		Cursor contactsCursor = contactsLoader.loadInBackground();
		Cursor emailsCursor = emailsLoader.loadInBackground();

		contactsCursor.registerContentObserver(observer);
		emailsCursor.registerContentObserver(observer);

		// Handle emails.
		SetMultimap<String, String> allEmails = LinkedHashMultimap.create();
		for (emailsCursor.moveToFirst(); !emailsCursor.isAfterLast(); emailsCursor.moveToNext()) {
			String lookupKey = checkNotNull(emailsCursor.getString(0));
			String email = checkNotNull(emailsCursor.getString(1));
			allEmails.put(lookupKey, email);
		}

		// Handle contacts.
		ArrayList<PersonInfo> data = new ArrayList<>(contactsCursor.getCount());
		for (contactsCursor.moveToFirst(); !contactsCursor.isAfterLast(); contactsCursor.moveToNext()) {
			long id = checkNotNull(contactsCursor.getLong(0));
			String lookupKey = checkNotNull(contactsCursor.getString(1));
			String displayName = checkNotNull(contactsCursor.getString(2));
			String photoUri = contactsCursor.getString(3);
			Uri lookupUri = Contacts.getLookupUri(id, lookupKey);
			Set<String> emails = allEmails.get(lookupKey);

			data.add(new PersonInfo(
					id,
					lookupKey,
					lookupUri,
					displayName,
					photoUri,
					emails
			));
		}

		return data;
	}

	@Override
	public void cancelLoadInBackground() {
		contactsLoader.cancelLoadInBackground();
		emailsLoader.cancelLoadInBackground();
	}

	@Override
	protected void onStartLoading() {
		if (subscription == null) {
			subscription = friendsService.observable()
					// Multithreaded OK.
					//.subscribeOn(AndroidSchedulers.mainThread())
					//.observeOn(AndroidSchedulers.mainThread())
					.subscribe(Subscribers.create(new Action1<FriendsService.Change>() {
						@Override
						public void call(FriendsService.Change change) {
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
		super.onStopLoading();
		contactsLoader.stopLoading();
		emailsLoader.stopLoading();
	}

	@Override
	protected void onReset() {
		super.onReset();

		if (subscription != null) {
			subscription.unsubscribe();
			subscription = null;
		}

		contactsLoader.reset();
		emailsLoader.reset();
	}

	@Override
	public void onCanceled(List<PersonInfo> data) {
		super.onCanceled(data);
		// Nothing to release for `data`.
	}

}
