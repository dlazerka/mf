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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;
import com.android.volley.NetworkResponse;
import com.android.volley.Request.Method;
import com.android.volley.VolleyError;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.FriendInfo;
import me.lazerka.mf.android.adapter.FriendListAdapter;
import me.lazerka.mf.android.adapter.FriendsLoader;
import me.lazerka.mf.android.http.HttpUtils;
import me.lazerka.mf.android.http.JsonRequester;
import me.lazerka.mf.api.object.*;
import org.acra.ACRA;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 * TODO: add null activity handling
 */
public class ContactsFragment extends Fragment {
	private static final String TAG = ContactsFragment.class.getName();

	private static final int FRIENDS_LOADER_ID = 12345;

	/** Result code of ContactPicker dialog. */
	private final int CONTACT_PICKER_RESULT = 1;

	private FriendListAdapter friendListAdapter;
	private FriendsLoaderCallbacks friendsLoaderCallbacks;

	@Nullable
	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
	) {
		Log.v(TAG, "onCreateView");
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

		friendListAdapter = new FriendListAdapter(new OnItemClickListener(), new OnAddFriendClickListener());
		recyclerView.setAdapter(friendListAdapter);

		friendsLoaderCallbacks = new FriendsLoaderCallbacks(friendListAdapter);
		getLoaderManager().initLoader(FRIENDS_LOADER_ID, null, friendsLoaderCallbacks);
	}

	private class OnItemClickListener implements FriendListAdapter.OnFriendClickListener {
		@Override
		public void onClick(FriendInfo friendInfo) {
			Log.d(TAG, "click " + friendInfo.displayName);

			ContactFragment fragment = new ContactFragment();
			fragment.setArguments(friendInfo.toBundle());
			getFragmentManager().beginTransaction()
					.setCustomAnimations(
							R.animator.slide_from_below, R.animator.slide_to_above,
							R.animator.slide_from_above, R.animator.slide_to_below
					)
					.replace(R.id.bottom_menu, fragment)
					.addToBackStack(null)
					.commit();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != CONTACT_PICKER_RESULT) {
			Log.w(TAG, "Unknown request code: " + requestCode);
			super.onActivityResult(requestCode, resultCode, data);
		}

		if (resultCode == Activity.RESULT_OK) {
			Uri contactUri = data.getData();
			Log.i(TAG, "Adding friend: " + contactUri);

			Application.preferences.addFriend(contactUri);

			getLoaderManager().restartLoader(FRIENDS_LOADER_ID, null, friendsLoaderCallbacks);

			new FriendsStorer(new UserFriendsPut());
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
				Log.v(TAG, "FriendsLoaderCallbacks: loaderId " + loaderId + " not mine.");
				return null;
			}

			return new FriendsLoader(getActivity());
		}

		@Override
		public void onLoadFinished(Loader<List<FriendInfo>> loader, List<FriendInfo> data) {
			Log.v(TAG, "FriendsLoaderCallbacks: onLoadFinished " + loader.getId() + ", " + data.size());
			friendListAdapter.setData(data);
			Set<String> emails = new HashSet<>(data.size());
			for(FriendInfo friendInfo : data) {
				emails.addAll(friendInfo.emails);
			}

			UsersInfoGet usersInfoGet = new UsersInfoGet(emails);
			new UsersInfoRequester(usersInfoGet)
					.send();
		}

		@Override
		public void onLoaderReset(Loader<List<FriendInfo>> loader) {
			Log.v(TAG, "FriendsLoaderCallbacks: onLoaderReset " + loader.getId());
			friendListAdapter.resetData();
		}
	}

	/** Requests server to see if my friends ever installed the app and have me in their friends. */
	private class UsersInfoRequester extends JsonRequester<UsersInfoGet, UsersInfoResponse> {
		public UsersInfoRequester(@Nullable UsersInfoGet request) {
			super(Method.POST, LocationRequest.PATH, request, UsersInfoResponse.class, getActivity());
		}

		@Override
		public void onResponse(UsersInfoResponse response) {
			Map<UserInfo, Set<String>> userInfos = response.getUserInfos();
			userInfos = userInfos == null ? new HashMap<UserInfo, Set<String>>() : userInfos;
			Log.v(TAG, "Received " + userInfos.size() + " friend infos");
			friendListAdapter.setServerInfos(userInfos);
		}
	}

	private class FriendsStorer extends JsonRequester<UserFriendsPut, Void> {
		public FriendsStorer(@Nullable UserFriendsPut request) {
			super(Method.PUT, UserFriendsPut.PATH, request, Void.class, getActivity());
		}

		@Override
		public void onResponse(Void response) {
			Log.v(TAG, "Store friends successful");
		}
	}

}
