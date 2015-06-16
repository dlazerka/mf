package me.lazerka.mf.android.activity;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import com.google.common.base.Joiner;
import me.lazerka.mf.android.adapter.FriendInfo;
import me.lazerka.mf.android.adapter.FriendListAdapter2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Chained loader of Contacts and their Emails.
 *
 * @author Dzmitry Lazerka
 */
class FriendsLoader implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = FriendsLoader.class.getName();

	private static final int FRIENDS_CONTACTS_LOADER_ID = 0;
	private static final int FRIENDS_EMAILS_LOADER_ID = 1;

	private final Fragment fragment;
	private final List<String> lookupUris;
	private final FriendListAdapter2 friendListAdapter;

	FriendsLoader(Fragment fragment, List<String> lookupUris, FriendListAdapter2 friendListAdapter) {
		this.fragment = checkNotNull(fragment);
		this.lookupUris = checkNotNull(lookupUris);
		this.friendListAdapter = checkNotNull(friendListAdapter);
	}

	public void run() {
		fragment.getLoaderManager().initLoader(FRIENDS_CONTACTS_LOADER_ID, null, this);
	}

	/** @return "?,?,?,?" as many as there are lookupUris */
	private static String getQueryPlaceholders(List<String> lookupUris) {
		return Joiner.on(',').useForNull("?").join(new String[lookupUris.size()]);
	}

	private static String[] getSelectionArguments(List<String> lookupUris) {
		return lookupUris.toArray(new String[lookupUris.size()]);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
		if (loaderId != FRIENDS_CONTACTS_LOADER_ID) {
			Log.v(TAG, "FriendsLoader: loaderId " + loaderId + " not mine.");
			return null;
		}

		String[] projection = new String[]{
				Contacts._ID,
				Contacts.LOOKUP_KEY,
				Contacts.DISPLAY_NAME_PRIMARY,
				Contacts.PHOTO_URI,
		};

		String placeholders = getQueryPlaceholders(lookupUris);
		String[] selectionArgs = getSelectionArguments(lookupUris);
		return new CursorLoader(
				fragment.getActivity(), // Parent activity activity
				Contacts.CONTENT_URI, // Table to query
				projection, // Projection to return
				Contacts.LOOKUP_KEY + " IN (" + placeholders + ")", // No selection clause
				selectionArgs, // No selection arguments
				Contacts.SORT_KEY_PRIMARY
		);
	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		Map<String, FriendInfo> data = new LinkedHashMap<>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			FriendInfo friendInfo = new FriendInfo();
			friendInfo.id = cursor.getLong(0);
			friendInfo.lookupKey = cursor.getString(1);
			friendInfo.displayName = cursor.getString(2);
			friendInfo.photoUri = cursor.getString(3);
			data.put(friendInfo.lookupKey, friendInfo);
			cursor.moveToNext();
		}

		fragment.getLoaderManager().initLoader(FRIENDS_EMAILS_LOADER_ID, null, new FriendEmailsLoader(lookupUris, data));
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		fragment.getLoaderManager().destroyLoader(FRIENDS_EMAILS_LOADER_ID);
	}

	/**
	 * Second loader, now for contacts' emails.
	 */
	private class FriendEmailsLoader implements LoaderManager.LoaderCallbacks<Cursor> {
		private final List<String> lookupUris;
		private final Map<String, FriendInfo> data;

		public FriendEmailsLoader(List<String> lookupUris, Map<String, FriendInfo> data) {
			this.lookupUris = lookupUris;
			this.data = data;
		}

		@Override
		public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
			if (loaderId != FRIENDS_EMAILS_LOADER_ID) {
				Log.v(TAG, "FriendsLoader: loaderId " + loaderId + " not mine.");
				return null;
			}

			String[] projection = new String[]{
					Email.LOOKUP_KEY,
					Email.ADDRESS
			};

			String placeholders = getQueryPlaceholders(lookupUris);
			String[] selectionArgs = getSelectionArguments(lookupUris);
			return new CursorLoader(
					fragment.getActivity(), // Parent activity activity
					Email.CONTENT_URI, // Table to query
					projection, // Projection to return
					Email.LOOKUP_KEY + " IN (" + placeholders + ")", // No selection clause
					selectionArgs, // No selection arguments
					Email.SORT_KEY_PRIMARY
			);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				String lookupKey = cursor.getString(0);
				FriendInfo friendInfo = data.get(lookupKey);
				if (friendInfo != null) {
					friendInfo.emails.add(cursor.getString(1));
				} else {
					Log.e(TAG, "No friendInfo for setting email to, by lookupKey " + lookupKey);
				}
				cursor.moveToNext();
			}

			friendListAdapter.setData(data.values());
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			friendListAdapter.resetData();
		}
	}
}
