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

package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.FriendInfo;
import me.lazerka.mf.android.adapter.FriendListAdapter;
import me.lazerka.mf.android.adapter.FriendsLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 * TODO: add null activity handling
 */
public class ContactsFragment extends Fragment {
	private static final Logger logger = LoggerFactory.getLogger(ContactsFragment.class);

	private static final int FRIENDS_LOADER_ID = 12345;

	/** Result code of ContactPicker dialog. */
	private final int CONTACT_PICKER_RESULT = 1;

	private FriendsLoaderCallbacks friendsLoaderCallbacks;

	@Nullable
	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
	) {
		View view = inflater.inflate(R.layout.fragment_contacts, container, false);

		initList(view);

//		if (Application.preferences.getFriends().isEmpty()) {
//			openContactPicker();
//		}

		return view;
	}

	private void initList(View view) {
		RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.contacts_list);

		LinearLayoutManager layoutManager
			= new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
		recyclerView.setLayoutManager(layoutManager);

		// Optimization
		recyclerView.setHasFixedSize(true);

		FriendListAdapter friendListAdapter = new FriendListAdapter(
				new OnItemClickListener(),
				new OnAddFriendClickListener());
		recyclerView.setAdapter(friendListAdapter);

		friendsLoaderCallbacks = new FriendsLoaderCallbacks(friendListAdapter);
		getLoaderManager().initLoader(FRIENDS_LOADER_ID, null, friendsLoaderCallbacks);
	}

	private class OnItemClickListener implements FriendListAdapter.OnFriendClickListener {
		@Override
		public void onClick(FriendInfo friendInfo) {
			ContactFragment fragment = new ContactFragment();
			fragment.setArguments(friendInfo.toBundle());
			getFragmentManager().beginTransaction()
					.setCustomAnimations(
							R.animator.slide_from_below, R.animator.slide_to_above,
							R.animator.slide_from_above, R.animator.slide_to_below
					)
					.replace(R.id.bottom_fragment_container, fragment)
					.addToBackStack(null)
					.commit();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != CONTACT_PICKER_RESULT) {
			logger.warn("Unknown request code: " + requestCode);
			super.onActivityResult(requestCode, resultCode, data);
		}

		if (resultCode == Activity.RESULT_OK) {
			Uri contactUri = data.getData();
			logger.info("Adding friend: " + contactUri);

			Application.preferences.addFriend(contactUri);

			getLoaderManager().restartLoader(FRIENDS_LOADER_ID, null, friendsLoaderCallbacks);
		}
	}

	private class OnAddFriendClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
			startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
		}
	}

	/**
	 * Invokes {@link FriendsLoader} to load contacts and their emails.
	 *
	 * @author Dzmitry Lazerka
	 */
	public class FriendsLoaderCallbacks implements LoaderManager.LoaderCallbacks<List<FriendInfo>> {
		private final FriendListAdapter friendListAdapter;

		public FriendsLoaderCallbacks(FriendListAdapter friendListAdapter) {
			this.friendListAdapter = checkNotNull(friendListAdapter);
		}

		@Override
		public Loader<List<FriendInfo>> onCreateLoader(int loaderId, Bundle args) {
			if (loaderId != FRIENDS_LOADER_ID) {
				return null;
			}

			return new FriendsLoader(getActivity());
		}

		@Override
		public void onLoadFinished(Loader<List<FriendInfo>> loader, List<FriendInfo> data) {
			friendListAdapter.setData(data);

/*
			Set<String> emails = new HashSet<>(data.size());
			for(FriendInfo friendInfo : data) {
				emails.addAll(friendInfo.emails);
			}

			if (!emails.isEmpty()) {
				UsersInfoRequest usersInfoRequest = new UsersInfoRequest(emails);
				MainActivity activity = (MainActivity) getActivity();
				new ApiPost(usersInfoRequest).newCall().execute();

				List<UserInfo> userInfos = response.getUserInfos();
				friendListAdapter.setServerInfos(userInfos);
			}
*/
		}

		@Override
		public void onLoaderReset(Loader<List<FriendInfo>> loader) {
			friendListAdapter.resetData();
		}
	}
}
