package me.lazerka.mf.android.activity;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import me.lazerka.mf.android.adapter.FriendInfo;
import me.lazerka.mf.android.adapter.FriendListAdapter2;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Chained loader of Contacts and their Emails.
 *
 * @author Dzmitry Lazerka
 */
class FriendsLoaderCallbacks implements LoaderManager.LoaderCallbacks<List<FriendInfo>> {
	private static final String TAG = FriendsLoaderCallbacks.class.getName();

	private final Fragment fragment;
	private final FriendListAdapter2 friendListAdapter;

	FriendsLoaderCallbacks(Fragment fragment, FriendListAdapter2 friendListAdapter) {
		this.fragment = checkNotNull(fragment);
		this.friendListAdapter = checkNotNull(friendListAdapter);
	}

	@Override
	public Loader<List<FriendInfo>> onCreateLoader(int loaderId, Bundle args) {
		if (loaderId != ContactsFragment.FRIENDS_LOADER_ID) {
			Log.v(TAG, "FriendsLoaderCallbacks: loaderId " + loaderId + " not mine.");
			return null;
		}

		return new FriendsLoader(fragment.getActivity());
	}

	@Override
	public void onLoadFinished(Loader<List<FriendInfo>> loader, List<FriendInfo> data) {
		Log.v(TAG, "FriendsLoaderCallbacks: onLoadFinished " + loader.getId() + ", " + data.size());
		friendListAdapter.setData(data);
	}

	@Override
	public void onLoaderReset(Loader<List<FriendInfo>> loader) {
		Log.v(TAG, "FriendsLoaderCallbacks: onLoaderReset " + loader.getId());
		friendListAdapter.resetData();
	}
}
