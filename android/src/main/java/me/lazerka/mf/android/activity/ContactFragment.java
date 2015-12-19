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
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import org.joda.time.Duration;

import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.FriendInfo;
import me.lazerka.mf.android.adapter.FriendViewHolder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle arguments = getArguments();
		friendInfo = checkNotNull(arguments.<FriendInfo>getParcelable(FRIEND_INFO));
	}

	@Nullable
	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
	) {
		View view = inflater.inflate(R.layout.fragment_contact, container, false);

		FriendViewHolder friendViewHolder = new FriendViewHolder(view);
		friendViewHolder.bindFriend(friendInfo);

		Spinner spinner = (Spinner) view.findViewById(R.id.duration);

		CharSequence[] durationTexts = getResources().getTextArray(R.array.durations_text);
		int[] durationValues = getResources().getIntArray(R.array.durations_seconds);
		final DurationsAdapter durationsAdapter = new DurationsAdapter(getActivity(), durationTexts, durationValues);
		spinner.setAdapter(durationsAdapter);
		spinner.setOnItemSelectedListener(durationsAdapter);

		View locate = view.findViewById(R.id.fab_locate);
		locate.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						MainActivity activity = (MainActivity) getActivity();
						if (!friendInfo.emails.isEmpty()) {

							Duration duration = durationsAdapter.getSelectedDuration();
							activity.requestLocationUpdates(friendInfo, duration);
						} else {
							// TODO disable FAB at all and show red warning instead
							String msg = getString(R.string.contact_no_emails, friendInfo.displayName);
							Toast.makeText(activity, msg, Toast.LENGTH_LONG)
									.show();
						}
					}
				});

		return view;
	}

	private static class DurationsAdapter
			extends ArrayAdapter<CharSequence>
			implements AdapterView.OnItemSelectedListener {
		private final int[] itemValues;
		private int selectedPosition;

		public DurationsAdapter(Context context, CharSequence[] itemLabels, int[] itemValues) {
			super(context, android.R.layout.simple_spinner_item, itemLabels);
			this.itemValues = itemValues;
			checkState(itemValues.length == itemLabels.length);
			selectedPosition = 0;
		}

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			selectedPosition = position;
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			selectedPosition = 0;
		}

		public Duration getSelectedDuration() {
			int selectedValue = itemValues[selectedPosition];
			return Duration.standardSeconds(selectedValue);
		}
	}
}
