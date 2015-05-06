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
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Toast;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.FriendListAdapter;

import java.util.LinkedHashSet;

/**
 * @author Dzmitry Lazerka
 * TODO: add null activity handling
 */
public class ContactsFragment extends Fragment {
	private static final String TAG = "ContactsFragment";

	/** Result code of ContactPicker dialog. */
	private final int CONTACT_PICKER_RESULT = 1;

	private FriendListAdapter friendListAdapter;

	@Nullable
	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
	) {
		Log.v(TAG, "onCreateView");
		View view = inflater.inflate(R.layout.fragment_contacts, container, false);
		initFriendList(view);

//		initFloatingActionButton(view);

//		if (Application.preferences.getFriends().isEmpty()) {
//			openContactPicker();
//		}

		return view;
	}

	private void initFriendList(View view) {
		AbsListView list = (AbsListView) view.findViewById(R.id.contacts_list);

		friendListAdapter = new FriendListAdapter(this.getActivity());
		list.setAdapter(friendListAdapter);
		list.setOnItemClickListener(new OnItemClickListener());
		list.setOnTouchListener(
				new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						// Disallow the touch request for parent scroll on touch of child view
						v.getParent().requestDisallowInterceptTouchEvent(true);
						return false;
					}
				});
	}

	private void initFloatingActionButton(View view) {
		ImageButton fab = (ImageButton) view.findViewById(R.id.fab_add);
		fab.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
		fab.setClipToOutline(true);

		fab.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
				startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
			}
		});
	}

	private class OnItemClickListener implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Log.d(TAG, "click " + id);
			Uri contactUri = friendListAdapter.getContactAtPosition(position);
			LinkedHashSet<String> emails = getContactEmails(contactUri);

			MainActivity activity = (MainActivity) getActivity();
			activity.showLocation(emails);
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
			friendListAdapter.refresh();
		}
	}

}
