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

package me.lazerka.mf.android.adapter;

import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import me.lazerka.mf.android.R;
import me.lazerka.mf.android.view.LetterDrawable;

/**
 * @author Dzmitry Lazerka
 */
public class FriendViewHolder extends ViewHolder {

	// Also see setRect() below.
	private static final RectShape SHAPE = new RectShape();

	public FriendViewHolder(View itemView) {
		super(itemView);
	}

	public void bindFriend(PersonInfo personInfo, View.OnClickListener listener) {
		// Set name.
		TextView nameView = (TextView) itemView.findViewById(R.id.name);
		nameView.setText(personInfo.displayName);

		QuickContactBadge badge = (QuickContactBadge) itemView.findViewById(R.id.quickbadge);
		badge.assignContactUri(personInfo.lookupUri);

		if (personInfo.photoUri != null) {
			badge.setImageURI(Uri.parse(personInfo.photoUri));
		} else {
			String displayName = personInfo.displayName;

			char letter = displayName.charAt(0);
			LetterDrawable drawable = new LetterDrawable(SHAPE, letter, displayName.hashCode());
			badge.setImageDrawable(drawable);

			// This comment was relevant to CursorAdapter, not sure it's needed for RecyclerView anymore.
			// Even if you don't set anything, make sure to call with null, to clear image of a newly added item.
			// v.setImageBitmap(null);
		}

		badge.setOnClickListener(listener);
	}
}
