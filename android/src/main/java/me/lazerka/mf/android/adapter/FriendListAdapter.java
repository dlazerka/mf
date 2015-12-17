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

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import me.lazerka.mf.android.R;
import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class FriendListAdapter extends RecyclerView.Adapter<ViewHolder> {
	private static final Logger logger = LoggerFactory.getLogger(FriendListAdapter.class);

	private final List<FriendInfo> data = new ArrayList<>();
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
			String msg = "Illegal item position: " + position + " vs " + getItemCount();
			logger.error(msg);
			ACRA.getErrorReporter().handleException(new IndexOutOfBoundsException(msg));
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
				String msg = "Holder is not of type FriendViewHolder for position " + position;
				logger.error(msg);
				ACRA.getErrorReporter().handleException(new IllegalStateException(msg, e));
				return;
			}

			FriendInfo friendInfo = data.get(position);
			friendViewHolder.bindFriend(friendInfo);
			friendViewHolder.itemView.setOnClickListener(new OnItemClickListener(friendInfo));
		} else if (data.size() == position) {
			// This is add button.
			((AddFriendViewHolder) holder).bind(onAddFriendClickListener);
		} else {
			String msg = "Illegal item position: " + position + " vs " + getItemCount();
			logger.error(msg);
			ACRA.getErrorReporter().handleException(new IllegalStateException(msg));
		}
	}

	@Override
	public int getItemCount() {
		return data.size() + 1;
	}

	public void setData(@Nonnull Collection<FriendInfo> data) {
		this.data.clear();
		this.data.addAll(data);
		notifyDataSetChanged();
	}

	public void resetData() {
		data.clear();
		notifyDataSetChanged();
	}

/*
	public void setServerInfos(List<UserInfo> serverInfos) {
		Map<String, UserInfo> byEmail = new HashMap<>();
		for(UserInfo serverInfo : serverInfos) {
			for(String email : serverInfo.getEmails()) {
				byEmail.put(email, serverInfo);
			}
		}

		for(FriendInfo friendInfo : data) {
			friendInfo.serverInfos = new HashMap<>();
			for(String email : friendInfo.emails) {
				UserInfo userInfo = byEmail.get(email);
				if (userInfo != null) {
					friendInfo.serverInfos.put(email, userInfo);
				}
			}
		}
	}
*/

	public interface OnFriendClickListener {
		void onClick(FriendInfo friendInfo);
	}

	private class OnItemClickListener implements OnClickListener {

		private final FriendInfo friendInfo;

		private OnItemClickListener(FriendInfo friendInfo) {
			this.friendInfo = friendInfo;
		}

		@Override
		public void onClick(View v) {
			onFriendClickListener.onClick(friendInfo);
		}
	}
}
