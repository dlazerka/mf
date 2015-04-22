package me.lazerka.mf.android.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import com.google.common.base.Joiner;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dzmitry Lazerka
 */
public class FriendsListAdapter extends SimpleCursorAdapter {
	Context mContext;
	Cursor mCursor;

	public FriendsListAdapter(Context context) {
		super(
				context,
				R.layout.contacts_item,
				null,
				new String[] {
						Contacts.PHOTO_THUMBNAIL_URI, // See ViewBinder below.
						Contacts.DISPLAY_NAME_PRIMARY,
				}, // From
				new int[]{
						R.id.userpic,
						R.id.name,
				},// To
				0
		);
		mContext = context;
		refresh();
	}

	public void refresh() {
		mCursor = fetchDataCursor();
		swapCursor(mCursor);
	}

	private Cursor fetchDataCursor() {
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

		ContentResolver contentResolver = mContext.getContentResolver();
		return contentResolver.query(
				Contacts.CONTENT_URI,
				projection,
				Contacts.LOOKUP_KEY + " IN (" + placeholders + ")",
				selectionArgs,
				Contacts.SORT_KEY_PRIMARY
		);
	}

	@Override
	public void setViewImage(@Nonnull ImageView v, String value) {
		// Just to not try to bind null photo uri and spam log with warnings.
		if (!value.isEmpty()) {
			v.setImageURI(Uri.parse(value));
		} else {
			// To clear image of a newly added item.
			v.setImageURI(null);
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
		// Otherwise item doesn't catch click events http://stackoverflow.com/questions/7645880
		removeButton.setFocusable(false);
	}

	public Uri getContactAtPosition(int position) {
		String lookupKey = mCursor.getString(mCursor.getColumnIndex(Contacts.LOOKUP_KEY));
		long contactId = mCursor.getLong(mCursor.getColumnIndex(Contacts._ID));
		return Contacts.getLookupUri(contactId, lookupKey);
	}

	private class OnDeleteListener implements OnClickListener {
		private final Uri contactUri;

		OnDeleteListener(Uri contactUri) {
			this.contactUri = contactUri;
		}

		@Override
		public void onClick(View v) {
			Application.preferences.removeFriend(contactUri);
			refresh();
		}

	}

}
