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

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.baraded.mf.logging.LogService;
import com.baraded.mf.logging.Logger;
import me.lazerka.mf.android.R;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class FriendListAdapter extends RecyclerView.Adapter<ViewHolder> {
	private static final Logger logger = LogService.getLogger(FriendListAdapter.class);

	private final List<PersonInfo> data = new ArrayList<>();
	private final OnFriendClickListener onFriendClickListener;
	private final OnClickListener onAddFriendClickListener;

	public FriendListAdapter(
			OnFriendClickListener onFriendClickListener,
			OnClickListener onAddFriendClickListener
	) {
		super();
		this.onFriendClickListener = checkNotNull(onFriendClickListener);
		this.onAddFriendClickListener = checkNotNull(onAddFriendClickListener);
	}

	@Override
	public int getItemViewType(int position) {
		if (data.size() > position) {
			return R.layout.view_contact;
		} else if (data.size() == position) {
			return R.layout.view_add_contact;
		} else {
			logger.error("Illegal item position: {} vs {}", position, getItemCount());
			return 0;
		}
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View itemView = LayoutInflater.from(parent.getContext())
				.inflate(viewType, parent, false);
		if (viewType == R.layout.view_contact) {
			return new FriendViewHolder(itemView);
		} else {
			return new AddFriendViewHolder(itemView);
		}
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		if (data.size() > position) {
			FriendViewHolder friendViewHolder;
			try {
				friendViewHolder = (FriendViewHolder) holder;
			} catch (ClassCastException e) {
				logger.error("Holder is not of type FriendViewHolder for position {}", position);
				return;
			}

			PersonInfo personInfo = data.get(position);
			friendViewHolder.bindFriend(personInfo, new OnItemClickListener(personInfo));
		} else if (data.size() == position) {
			// This is add button.
			((AddFriendViewHolder) holder).bind(onAddFriendClickListener);
		} else {
			logger.error("Illegal item position: {} vs {}", position, getItemCount());
		}
	}

	@Override
	public int getItemCount() {
		return data.size() + 1;
	}

	public void setData(@Nonnull Collection<PersonInfo> data) {
		this.data.clear();
		this.data.addAll(data);
		notifyDataSetChanged();
	}

	public void resetData() {
		data.clear();
		notifyDataSetChanged();
	}

	public interface OnFriendClickListener {
		void onClick(PersonInfo personInfo);
	}

	private class OnItemClickListener implements OnClickListener {

		private final PersonInfo personInfo;

		private OnItemClickListener(PersonInfo personInfo) {
			this.personInfo = personInfo;
		}

		@Override
		public void onClick(View v) {
			onFriendClickListener.onClick(personInfo);
		}
	}
}
