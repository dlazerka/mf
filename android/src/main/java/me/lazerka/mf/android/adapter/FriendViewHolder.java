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

package me.lazerka.mf.android.adapter;

import android.graphics.Outline;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
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

	public void bindFriend(FriendInfo friendInfo) {
		// Set name.
		TextView nameView = (TextView) this.itemView.findViewById(R.id.name);
		nameView.setText(friendInfo.displayName);

		// Set image.
		ImageView imageView = (ImageView) this.itemView.findViewById(R.id.userpic);
		if (friendInfo.photoUri == null || friendInfo.photoUri.isEmpty()) {
			String displayName = friendInfo.displayName;

			char letter = displayName.charAt(0);
			LetterDrawable drawable = new LetterDrawable(SHAPE, letter, displayName.hashCode());
			imageView.setImageDrawable(drawable);

			// This comment was relevant to CursorAdapter, not sure it's needed for RecyclerView anymore.
			// Even if you don't set anything, make sure to call with null, to clear image of a newly added item.
			// v.setImageBitmap(null);
		} else {
			imageView.setImageURI(Uri.parse(friendInfo.photoUri));
			imageView.setOutlineProvider(
					new ViewOutlineProvider() {
						@Override
						public void getOutline(View view, Outline outline) {
							// Also see RectShape above.
							outline.setRect(0, 0, view.getWidth(), view.getWidth());
						}
					});
			imageView.setClipToOutline(true);
		}
	}
}
