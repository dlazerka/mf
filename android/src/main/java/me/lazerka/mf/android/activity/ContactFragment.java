package me.lazerka.mf.android.activity;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.FriendInfo;
import me.lazerka.mf.android.adapter.FriendViewHolder;

/**
 * @author Dzmitry Lazerka
 * TODO: add null activity handling
 */
public class ContactFragment extends Fragment {
	private static final String TAG = ContactFragment.class.getName();

	private FriendInfo friendInfo;

	@Nullable
	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
	) {
		Log.v(TAG, "onCreateView");
		View view = inflater.inflate(R.layout.fragment_contact, container, false);

		Bundle arguments = getArguments();
		friendInfo = FriendInfo.fromBundle(arguments);

		FriendViewHolder friendViewHolder = new FriendViewHolder(view);
		friendViewHolder.bindFriend(friendInfo);

		View locate = view.findViewById(R.id.fab_locate);
		locate.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						MainActivity activity = (MainActivity) getActivity();
						if (!friendInfo.emails.isEmpty()) {
							activity.showLocation(friendInfo.emails);
						} else {
							String msg = getString(R.string.contact_no_emails);
							Toast.makeText(activity, msg, Toast.LENGTH_LONG)
									.show();
						}

					}
				});

		return view;
	}
}
