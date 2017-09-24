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

package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;
import com.baraded.mf.logging.LogService;
import com.baraded.mf.logging.Logger;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.FriendListAdapter;
import me.lazerka.mf.android.adapter.PersonInfo;
import me.lazerka.mf.android.contacts.FriendsManager;
import me.lazerka.mf.android.di.Injector;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * @author Dzmitry Lazerka
 * TODO: add null activity handling
 */
public class ContactsFragment extends InjectedFragment {
	private static final Logger logger = LogService.getLogger(ContactsFragment.class);

	/** Result code of ContactPicker dialog. */
	private static final int RC_CONTACT_PICKER = 1;

	@Inject
	FriendsManager friendsManager;

	@Inject
	LogService logService;

	private FriendListAdapter friendListAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Injector.applicationComponent().inject(this);

		friendListAdapter = new FriendListAdapter(
				new OnItemClickListener(),
				new OnAddFriendClickListener());

	}

	@Nullable
	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
	) {
		View view = inflater.inflate(R.layout.fragment_contacts, container, false);

		initList(view);

		return view;
	}

	private void initList(View view) {
		RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.contacts_list);

		LinearLayoutManager layoutManager
			= new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
		recyclerView.setLayoutManager(layoutManager);

		// Optimization
		recyclerView.setHasFixedSize(true);
		recyclerView.setAdapter(friendListAdapter);

		friendsManager
				.watchFriends()
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new DisposableObserver<List<PersonInfo>>() {

					@Override
					public void onComplete() {
						// Never happens.
					}

					@Override
					public void onError(Throwable e) {
						logger.error(e);

						Toast.makeText(getActivity(),
								getResources().getString(R.string.error_fetching_friends, e.getMessage()),
								Toast.LENGTH_LONG).show();
					}

					@Override
					public void onNext(List<PersonInfo> data) {
						friendListAdapter.setData(data);
					}
				});
	}

	@Override
	public void onResume() {
		super.onResume();
		friendListAdapter.notifyDataSetChanged();
	}

	private class OnItemClickListener implements FriendListAdapter.OnFriendClickListener {
		@Override
		public void onClick(PersonInfo personInfo) {
			ContactFragment fragment = new ContactFragment();
			Bundle arguments = ContactFragment.makeArguments(personInfo);
			fragment.setArguments(arguments);

			getFragmentManager().beginTransaction()
					.setCustomAnimations(
							R.animator.slide_from_below, R.animator.slide_to_above,
							R.animator.slide_from_above, R.animator.slide_to_below
					)
					.replace(R.id.bottom_fragment_container, fragment)
					.addToBackStack("ContactsFragment")
					.commit();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != RC_CONTACT_PICKER) {
			logger.warn("Unknown request code: " + requestCode);
			super.onActivityResult(requestCode, resultCode, data);
		}

		if (resultCode == Activity.RESULT_OK) {
			Uri contactUri = data.getData();
			logger.info("Adding friend: " + contactUri);

			friendsManager
				.addFriend(contactUri);

			logService.getEventLogger("friend_added").send();
		}
	}

	public MainActivity getMainActivity() {
		return (MainActivity) getActivity();
	}

	private class OnAddFriendClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			logService.getEventLogger("friend_add_clicked").send();

			getMainActivity().permissionAsker.checkAndRun(
					READ_CONTACTS,
					new Runnable() {
						@Override
						public void run() {
							Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
							startActivityForResult(contactPickerIntent, RC_CONTACT_PICKER);
						}
					},
					new Runnable() {
						@Override
						public void run() {
							logger.warn("ContactsFragment: addFriend READ_CONTACTS permission declined");
							logService.getEventLogger("READ_CONTACTS_declined").send();
						}
					}
			);
		}
	}
}
