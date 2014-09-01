package me.lazerka.mf.android.activity;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import me.lazerka.mf.android.R;

/**
 * @author Dzmitry Lazerka
 */
public class ContactsFragment extends Fragment {
	private ListView mContactsList;

	/** An adapter that binds the result Cursor to the ListView */
	private SimpleCursorAdapter mCursorAdapter;

	long mContactId;
	// The contact's LOOKUP_KEY
	String mContactKey;
	// A content URI for the selected contact
	Uri mContactUri;

	private static final String[] PROJECTION = {
		Contacts._ID,
		Contacts.LOOKUP_KEY,
		Contacts.DISPLAY_NAME_PRIMARY
	};

	// The column index for the _ID column
	private static final int CONTACT_ID_INDEX = 0;
	private static final int CONTACT_KEY_INDEX = 1;

	// A UI Fragment must inflate its View
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_contacts,container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mContactsList = (ListView) getActivity().findViewById(android.R.id.list);

		// Init CursorAdapter.
		mCursorAdapter = new SimpleCursorAdapter(
				getActivity(),
				R.layout.contacts_item,
				null,
				new String[] {Contacts.DISPLAY_NAME_PRIMARY},
				new int[] {android.R.id.text1},
				0
		);
		// Sets the adapter for the ListView
		mContactsList.setAdapter(mCursorAdapter);
		mContactsList.setOnItemClickListener(new OnItemClickListener());

		// Init Loader.
		getLoaderManager().initLoader(0, null, new LoaderCallbacks());
	}

	private class LoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			// Starts the query
			return new CursorLoader(
					getActivity(),
					Contacts.CONTENT_URI,
					PROJECTION,
					null, // No WHERE
					//Contacts.DISPLAY_NAME_PRIMARY + " LIKE ?",
					null, // No WHERE arguments
					//mSelectionArgs,
					null // Default sort
			);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			mCursorAdapter.swapCursor(cursor);
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			mCursorAdapter.swapCursor(null);
		}
	}

	private class OnItemClickListener implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// Get the Cursor
			SimpleCursorAdapter adapter = (SimpleCursorAdapter) parent.getAdapter();
			Cursor cursor = adapter.getCursor();
			// Move to the selected contact
			cursor.moveToPosition(position);
			// Get the _ID value
			mContactId = cursor.getLong(CONTACT_ID_INDEX);
			// Get the selected LOOKUP KEY
			mContactKey = cursor.getString(CONTACT_KEY_INDEX);
			// Create the contact's content Uri
			mContactUri = Contacts.getLookupUri(mContactId, mContactKey);
        /*
         * You can use mContactUri as the content URI for retrieving
         * the details for a contact.
         */
		}
	}
}
