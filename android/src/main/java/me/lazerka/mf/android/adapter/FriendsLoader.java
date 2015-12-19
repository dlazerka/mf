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

import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.lazerka.mf.android.Application;

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

	private final CursorLoader contactsLoader;
	private final CursorLoader emailsLoader;

	public FriendsLoader(Context context) {
		super(context);

		// Can be called from background.
		if (Looper.myLooper() != Looper.getMainLooper()) {
			Looper.prepare();
		}

		List<String> lookupUris = getFriendsLookupUris();

		// "?,?,?,?" as many as there are lookupUris
		String placeholders = Joiner.on(',').useForNull("?").join(new String[lookupUris.size()]);

		String[] selectionArgs = lookupUris.toArray(new String[lookupUris.size()]);

		contactsLoader = new CursorLoader(
				context,
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
				context,
				Email.CONTENT_URI, // Table to query
				new String[]{
						Email.LOOKUP_KEY,
						Email.ADDRESS
				}, // Projection to return
				Email.LOOKUP_KEY + " IN (" + placeholders + ")", // No selection clause
				selectionArgs, // No selection arguments
				Email.SORT_KEY_PRIMARY
		);
	}

	private static List<String> getFriendsLookupUris() {
		List<Uri> friends = Application.preferences.getFriends();
		List<String> lookups = new ArrayList<>(friends.size());
		for(Uri uri : friends) {
			// Uri is like content://com.android.contacts/contacts/lookup/822ig%3A105666563920567332652/2379
			// Last segment is "_id" (unstable), and before that is "lookup" (stable).
			List<String> pathSegments = uri.getPathSegments();
			// Need to encode, because they're stored that way.
			String lookup = Uri.encode(pathSegments.get(2));
			lookups.add(lookup);
		}
		return lookups;
	}

	@Override
	public ArrayList<PersonInfo> loadInBackground() {
		Cursor contactsCursor = contactsLoader.loadInBackground();
		Cursor emailsCursor = emailsLoader.loadInBackground();

		// Handle contacts.
		Map<String, PersonInfo> data = new LinkedHashMap<>(contactsCursor.getCount());
		for (contactsCursor.moveToFirst(); !contactsCursor.isAfterLast(); contactsCursor.moveToNext()) {
			long id = checkNotNull(contactsCursor.getLong(0));
			String lookupKey = checkNotNull(contactsCursor.getString(1));
			String displayName = checkNotNull(contactsCursor.getString(2));
			String photoUri = contactsCursor.getString(3);

			PersonInfo personInfo = new PersonInfo(
					id,
					lookupKey,
					displayName,
					photoUri,
					Collections.<String>emptyList()
			);
			data.put(personInfo.lookupKey, personInfo);
		}
		contactsCursor.close();

		// Handle emails.
		for (emailsCursor.moveToFirst(); !emailsCursor.isAfterLast(); emailsCursor.moveToNext()) {
			String lookupKey = emailsCursor.getString(0);
			PersonInfo personInfo = data.get(lookupKey);
			if (personInfo != null) {
				personInfo.emails.add(emailsCursor.getString(1));
			} else {
				String msg = "No personInfo for setting email to, by lookupKey " + lookupKey;
				logger.error(msg);
				ACRA.getErrorReporter().handleException(new IllegalStateException(msg));
			}
		}
		emailsCursor.close();

		return new ArrayList<>(data.values());
	}

	@Override
	public void cancelLoadInBackground() {
		contactsLoader.cancelLoadInBackground();
		emailsLoader.cancelLoadInBackground();
	}

	@Override
	protected void onStartLoading() {
		super.onStartLoading();

		if (isStarted()) {
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		super.onStopLoading();
	}

	@Override
	public void onCanceled(List<PersonInfo> data) {
		super.onCanceled(data);
	}

	@Override
	protected void onReset() {
		super.onReset();
	}

}
