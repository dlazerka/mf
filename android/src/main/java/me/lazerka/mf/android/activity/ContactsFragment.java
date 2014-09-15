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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.CursorAdapter;

import java.util.LinkedHashSet;

/**
 * @author Dzmitry Lazerka
 */
public class ContactsFragment extends Fragment {
	private final int CONTACT_PICKER_RESULT = 1;
	private static final String TAG = "ContactsFragment";
	private ListView mContactsList;
	private me.lazerka.mf.android.adapter.CursorAdapter mAdapter;

	// A UI Fragment must inflate its View
	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_contacts, container, false);

		Button addButton = (Button) view.findViewById(R.id.add_friend);
		addButton.setOnClickListener(new OnAddListener());
		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mContactsList = (ListView) getActivity().findViewById(android.R.id.list);

		mAdapter = new CursorAdapter(getActivity());
		mContactsList.setAdapter(mAdapter);
		mContactsList.setOnItemClickListener(new OnItemClickListener());
	}

	private class OnItemClickListener implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Log.d(TAG, "click " + id);
			Uri contactUri = mAdapter.getContactAtPosition(position);
			LinkedHashSet<String> emails = getContactEmails(contactUri);

			MainActivity activity = (MainActivity) getActivity();
			activity.showLocation(emails);
		}
	}

	private LinkedHashSet<String> getContactEmails(Uri contactUri) {
		String lookupKey = Uri.encode(contactUri.getPathSegments().get(2));

		ContentResolver contentResolver = getActivity().getContentResolver();

		Cursor cursor = contentResolver.query(
				Email.CONTENT_URI,
				null, // TODO: use projection
				//null,
				//null,
				Email.LOOKUP_KEY + " = ?",
				new String[] {lookupKey},
				Email.SORT_KEY_PRIMARY
		);
		LinkedHashSet<String> result = new LinkedHashSet<>(cursor.getCount());
		while (cursor.moveToNext()) {
			String email = cursor.getString(cursor.getColumnIndex(Email.ADDRESS));
			result.add(email);
		}

		return result;
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
				String msg = getActivity().getString(R.string.contact_no_emails);
				Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG)
						.show();
				return;
			}

			Application.preferences.addFriend(contactUri);
			mAdapter.refresh();
		}
	}

	private class OnAddListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
			startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
		}
	}
}
