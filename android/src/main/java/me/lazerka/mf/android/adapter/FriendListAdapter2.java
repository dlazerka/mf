package me.lazerka.mf.android.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import me.lazerka.mf.android.R;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class FriendListAdapter2
		extends RecyclerView.Adapter<FriendViewHolder> {
	private static final String TAG = FriendListAdapter2.class.getName();

	private final List<FriendInfo> data = new ArrayList<>();
	private final OnFriendClickListener onFriendClickListener;

	public FriendListAdapter2(OnFriendClickListener onFriendClickListener) {
		super();
		this.onFriendClickListener = checkNotNull(onFriendClickListener);
	}

	@Override
	public FriendViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View itemView = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_contacts, parent, false);
		return new FriendViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(FriendViewHolder holder, int position) {
		if (data.size() <= position) {
			Log.e(TAG, "Illegal item position: " + position + " vs " + getItemCount());
			return;
		}
		FriendInfo friendInfo = data.get(position);
		holder.bindFriend(friendInfo);
		holder.itemView.setOnClickListener(new OnItemClickListener(friendInfo));
	}

	@Override
	public int getItemCount() {
		return data.size();
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
			Log.d(TAG, "click " + friendInfo.displayName);
			onFriendClickListener.onClick(friendInfo);
		}
	}
}
