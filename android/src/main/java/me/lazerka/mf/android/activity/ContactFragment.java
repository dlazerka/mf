/*
 *     Copyright (C) 2017 Dzmitry Lazerka
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package me.lazerka.mf.android.activity;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.baraded.mf.logging.LogService;
import com.baraded.mf.logging.Logger;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.FriendViewHolder;
import me.lazerka.mf.android.adapter.PersonInfo;
import org.joda.time.Duration;

import static android.provider.ContactsContract.QuickContact.MODE_LARGE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Dzmitry Lazerka
 * TODO: add null activity handling
 */
public class ContactFragment extends Fragment {
	private static final Logger logger = LogService.getLogger(ContactFragment.class);

	private static final String PERSON_INFO = "PERSON_INFO";

	private PersonInfo personInfo;
	private int[] durationValues;
	private Spinner spinner;

	public static Bundle makeArguments(PersonInfo personInfo) {
		Bundle arguments = new Bundle(1);
		arguments.putParcelable(PERSON_INFO, checkNotNull(personInfo));
		return arguments;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle arguments = getArguments();
		personInfo = checkNotNull(arguments.<PersonInfo>getParcelable(PERSON_INFO));
	}

	@Nullable
	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
	) {
		View view = inflater.inflate(R.layout.fragment_contact, container, false);

		FriendViewHolder friendViewHolder = new FriendViewHolder(view);
		friendViewHolder.bindFriend(personInfo, new OnClickListener() {
			@Override
			public void onClick(View v) {
				ContactsContract.QuickContact.showQuickContact(
						getActivity(), v, personInfo.lookupUri, MODE_LARGE, new String[0]
				);
			}
		});

		TextView findMsg = (TextView) view.findViewById(R.id.find_msg);
		findMsg.setText(getString(R.string.find_person, personInfo.displayName));

		View removeFriend = view.findViewById(R.id.remove_friend);
		removeFriend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String message = getString(R.string.confirm_remove, personInfo.displayName);
				AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
						.setMessage(message)
						.setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Application.getFriendsManager().removeFriend(personInfo.lookupUri);
								getFragmentManager().popBackStack();
							}
						})
						.setNegativeButton(R.string.cancel, null)
						.create();
				alertDialog.show();
			}
		});

		spinner = (Spinner) view.findViewById(R.id.duration);

		CharSequence[] durationTexts = getResources().getTextArray(R.array.durations_text);
		durationValues = getResources().getIntArray(R.array.durations_seconds);
		checkState(durationTexts.length == durationValues.length);

		final DurationsAdapter durationsAdapter = new DurationsAdapter(getActivity(), durationTexts);
		spinner.setAdapter(durationsAdapter);
		// Remember user selection in preferences.
		spinner.setSelection(2);

		View locate = view.findViewById(R.id.fab_locate);
		locate.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						//MainActivity activity = (MainActivity) getActivity();
						//if (!personInfo.emails.isEmpty()) {
						//
						//	Duration duration = getSelectedDuration();
						//	activity.requestLocationUpdates(personInfo, duration);
						//} else {
						//	// TODO disable FAB at all and show red warning instead
						//	String msg = getString(R.string.contact_no_emails, personInfo.displayName);
						//	Toast.makeText(activity, msg, Toast.LENGTH_LONG)
						//			.show();
						//}
					}
				});

		return view;
	}

	public Duration getSelectedDuration() {
		int position = spinner.getSelectedItemPosition();
		int selectedValue = durationValues[position];
		return Duration.standardSeconds(selectedValue);
	}

	private static class DurationsAdapter extends ArrayAdapter<CharSequence> {
		public DurationsAdapter(Context context, CharSequence[] itemLabels) {
			//super(context, R.layout.view_dropdown_item, itemLabels);
			super(context, android.R.layout.simple_spinner_item, itemLabels);

			// Item in opened dropdown.
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}
	}
}
