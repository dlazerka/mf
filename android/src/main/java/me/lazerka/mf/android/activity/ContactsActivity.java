package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewOutlineProvider;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.FriendListAdapter;

import java.util.LinkedHashSet;

/**
 * @author Dzmitry Lazerka
 */
public class ContactsActivity extends Activity {
	private static final String TAG = "ContactsFragment";

	/** Result code of ContactPicker dialog. */
	private final int CONTACT_PICKER_RESULT = 1;

	private ListView friendList;
	private FriendListAdapter friendListAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contacts);

		initFriendList();
		initFloatingActionButton();

		if (Application.preferences.getFriends().isEmpty()) {
			openContactPicker();
		}
	}

	private void initFriendList() {
		friendList = (ListView) findViewById(android.R.id.list);

		friendListAdapter = new FriendListAdapter(this);
		friendList.setAdapter(friendListAdapter);
		friendList.setOnItemClickListener(new OnItemClickListener());
	}

	private void initFloatingActionButton() {
		ImageButton fab = (ImageButton) findViewById(R.id.fab_add);
		fab.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
		fab.setClipToOutline(true);

		fab.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				openContactPicker();
			}
		});
	}

	private void openContactPicker() {
		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
		startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
	}

	private class OnItemClickListener implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Log.d(TAG, "click " + id);
			Uri contactUri = friendListAdapter.getContactAtPosition(position);
			LinkedHashSet<String> emails = getContactEmails(contactUri);

			Intent intent = new Intent(getBaseContext(), MainActivity.class);
			intent.putExtra(MainActivity.REQUEST_CONTACT_EMAILS, emails);
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
	}

	private LinkedHashSet<String> getContactEmails(Uri contactUri) {
		String lookupKey = Uri.encode(contactUri.getPathSegments().get(2));

		ContentResolver contentResolver = getContentResolver();

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
		cursor.close();

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
				String msg = getString(R.string.contact_no_emails);
				Toast.makeText(ContactsActivity.this, msg, Toast.LENGTH_LONG)
						.show();
				return;
			}

			Application.preferences.addFriend(contactUri);
			friendListAdapter.refresh();
		}
	}

}
