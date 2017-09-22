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

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewOutlineProvider;
import me.lazerka.mf.android.R;

/**
 * @author Dzmitry Lazerka
 */
public class AddFriendViewHolder extends ViewHolder {

	private final View button;

	public AddFriendViewHolder(View view) {
		super(view);

		button = itemView.findViewById(R.id.fab_add);
		button.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
		button.setClipToOutline(true);
	}

	public void bind(OnClickListener listener) {
		button.setOnClickListener(listener);
	}
}
