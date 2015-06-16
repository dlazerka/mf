package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.Toast;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.FriendInfo;
import me.lazerka.mf.android.adapter.FriendListAdapter2;

import java.util.*;

/**
 * @author Dzmitry Lazerka
 * TODO: add null activity handling
 */
public class ContactsFragment extends Fragment {
	private static final String TAG = "ContactsFragment";

	/** Result code of ContactPicker dialog. */
	private final int CONTACT_PICKER_RESULT = 1;

	private FriendListAdapter2 friendListAdapter;
	private FriendsLoader friendsLoader;

	@Nullable
	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
	) {
		Log.v(TAG, "onCreateView");
		View view = inflater.inflate(R.layout.fragment_contacts, container, false);
		RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.contacts_list);

		LinearLayoutManager layoutManager
			= new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
		recyclerView.setLayoutManager(layoutManager);

		friendListAdapter = new FriendListAdapter2(new OnItemClickListener());
		recyclerView.setAdapter(friendListAdapter);

		List<String> lookupUris = getFriendsLookupUris();
		friendsLoader = new FriendsLoader(this, lookupUris, friendListAdapter);
		friendsLoader.run();


//		if (Application.preferences.getFriends().isEmpty()) {
//			openContactPicker();
//		}

		return view;
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

	private void initFloatingActionButton(View view) {
		ImageButton fab = (ImageButton) view.findViewById(R.id.fab_add);
		fab.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
		fab.setClipToOutline(true);

		fab.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
						startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
					}
				});
	}

	private class OnItemClickListener implements FriendListAdapter2.OnFriendClickListener {
		@Override
		public void onClick(FriendInfo friendInfo) {
			Log.d(TAG, "click " + friendInfo.displayName);
			MainActivity activity = (MainActivity) getActivity();
			activity.showLocation(friendInfo.emails);
		}
	}

	private LinkedHashSet<String> getContactEmails(Uri contactUri) {
		String lookupKey = Uri.encode(contactUri.getPathSegments().get(2));

		ContentResolver contentResolver = getActivity().getContentResolver();

		try (Cursor cursor = contentResolver.query(
				Email.CONTENT_URI,
				new String[] {Email.ADDRESS},
				//null,
				//null,
				Email.LOOKUP_KEY + " = ?",
				new String[] {lookupKey},
				Email.SORT_KEY_PRIMARY
		)) {
			LinkedHashSet<String> result = new LinkedHashSet<>(cursor.getCount());
			while (cursor.moveToNext()) {
				String email = cursor.getString(0);
				result.add(email);
			}

			return result;
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

			LinkedHashSet<String> contactEmails = getContactEmails(contactUri);
			if (contactEmails.isEmpty()) {
				String msg = getString(R.string.contact_no_emails);
				Toast.makeText(ContactsFragment.this.getActivity(), msg, Toast.LENGTH_LONG)
						.show();
				return;
			}

			Application.preferences.addFriend(contactUri);

			// FriendsLoader must handle this automatically by resetting the loader. TODO: test
			//friendListAdapter.refresh();
		}
	}

}
