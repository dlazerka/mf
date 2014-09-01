package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import com.google.common.base.Joiner;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dzmitry Lazerka
 */
public class ContactsFragment extends Fragment {
	private static final int CONTACT_PICKER_RESULT = 1;
	private static final String TAG = "ContactsFragment";
	private ListView mContactsList;
	private CursorAdapter mAdapter;

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

		//mAdapter = new ListAdapter();

		mAdapter = new CursorAdapter(getActivity(), getDataCursor());
		mContactsList.setAdapter(mAdapter);
		mContactsList.setOnItemClickListener(new OnItemClickListener());
	}



	public Cursor getDataCursor() {
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

		String[] projection = new String[] {
			Contacts._ID,
			Contacts.LOOKUP_KEY,
			Contacts.DISPLAY_NAME_PRIMARY,
			Contacts.PHOTO_THUMBNAIL_URI,
		};

		// "?,?,?,?"
		String placeholders = Joiner.on(',').useForNull("?").join(new String[lookups.size()]);
		String[] selectionArgs = lookups.toArray(new String[lookups.size()]);

		ContentResolver contentResolver = getActivity().getContentResolver();

		return contentResolver.query(
				Contacts.CONTENT_URI,
				projection,
				Contacts.LOOKUP_KEY + " IN (" + placeholders + ")",
				selectionArgs,
				null
		);
	}

	private class CursorAdapter extends SimpleCursorAdapter {
		public CursorAdapter(Activity activity, Cursor cursor) {
			super(
					activity,
					R.layout.contacts_item,
					cursor,
					new String[] {
							Contacts.DISPLAY_NAME_PRIMARY,
							Contacts.PHOTO_THUMBNAIL_URI // See ViewBinder below.
					}, // From
					new int[]{
							android.R.id.text1,
							R.id.userpic
					},// To
					0
			);
		}

		@Override
		public void setViewImage(@Nonnull ImageView v, String value) {
			// Just to not try to bind null photo uri and spam log with warnings.
			if (!value.isEmpty()) {
				v.setImageURI(Uri.parse(value));
			}
		}

		@Override
		public void bindView(@Nonnull View view, Context context, @Nonnull Cursor cursor) {
			super.bindView(view, context, cursor);

			int id = cursor.getInt(cursor.getColumnIndexOrThrow(Contacts._ID));
			String lookup = cursor.getString(cursor.getColumnIndexOrThrow(Contacts.LOOKUP_KEY));
			Uri contactUri = Contacts.getLookupUri(id, lookup);

			View removeButton = view.findViewById(R.id.remove);
			removeButton.setOnClickListener(new OnDeleteListener(contactUri));
		}
	}

	private class OnItemClickListener implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// Get the Cursor
			ListAdapter adapter = (ListAdapter) parent.getAdapter();
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
			mAdapter.swapCursor(getDataCursor());
		}
	}

	private class OnAddListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
			startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
		}
	}

	private class OnDeleteListener implements OnClickListener {
		private final Uri contactUri;

		OnDeleteListener(Uri contactUri) {
			this.contactUri = contactUri;
		}

		@Override
		public void onClick(View v) {
			Application.preferences.removeFriend(contactUri);
			mAdapter.swapCursor(getDataCursor());
		}
	}
}
