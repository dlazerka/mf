package me.lazerka.mf.android.activity;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;

/**
 * @author Dzmitry Lazerka
 */
public class ContactsFragment extends Fragment {
	private ListView mContactsList;

	// A UI Fragment must inflate its View
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_contacts, container, false);
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

		mContactsList.setAdapter(new ListAdapter());
		mContactsList.setOnItemClickListener(new OnItemClickListener());
	}

	private class ListAdapter extends ArrayAdapter<String> {
		public ListAdapter() {
			super(getActivity(), R.layout.contacts_item, android.R.id.text1);
			addAll(Application.preferences.getFriends());
		}

		@Override
		public int getCount() {
			return super.getCount();
		}

		@Override
		public String getItem(int position) {
			return super.getItem(position);
		}

		@Override
		public int getPosition(String item) {
			return super.getPosition(item);
		}

		@Override
		public long getItemId(int position) {
			return super.getItemId(position);
		}
	}

	private class OnItemClickListener implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// Get the Cursor
			ListAdapter adapter = (ListAdapter) parent.getAdapter();
		}
	}
}
