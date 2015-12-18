/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2015 Dzmitry Lazerka dlazerka@gmail.com
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package me.lazerka.mf.android.activity;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.FriendInfo;
import me.lazerka.mf.android.adapter.FriendViewHolder;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 * TODO: add null activity handling
 */
public class ContactFragment extends Fragment {
	private static final String FRIEND_INFO = "FRIEND_INFO";

	private FriendInfo friendInfo;

	public static Bundle makeArguments(FriendInfo friendInfo) {
		Bundle arguments = new Bundle(1);
		arguments.putParcelable(FRIEND_INFO, checkNotNull(friendInfo));
		return arguments;
	}

	@Nullable
	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
	) {
		View view = inflater.inflate(R.layout.fragment_contact, container, false);

		Bundle arguments = getArguments();
		friendInfo = arguments.getParcelable(FRIEND_INFO);

		FriendViewHolder friendViewHolder = new FriendViewHolder(view);
		friendViewHolder.bindFriend(friendInfo);

		View locate = view.findViewById(R.id.fab_locate);
		locate.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						MainActivity activity = (MainActivity) getActivity();
						if (!friendInfo.emails.isEmpty()) {
							activity.requestLocationUpdates(friendInfo);
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
