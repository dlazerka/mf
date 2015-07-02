package me.lazerka.mf.android.adapter;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import me.lazerka.mf.android.R;
import me.lazerka.mf.api.object.UserInfo;
import org.acra.ACRA;

import javax.annotation.Nonnull;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public class FriendListAdapter extends RecyclerView.Adapter<ViewHolder> {
	private static final String TAG = FriendListAdapter.class.getName();

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
			Log.e(TAG, msg);
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
				Log.e(TAG, msg);
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
			Log.e(TAG, msg);
			ACRA.getErrorReporter().handleException(new IllegalStateException(msg));
		}
	}

	@Override
	public int getItemCount() {
		return data.size() + 1;
	}

	public void setData(@Nonnull Collection<FriendInfo> data) {
		Log.v(TAG, "setData");
		this.data.clear();
		this.data.addAll(data);
		notifyDataSetChanged();
	}

	public void resetData() {
		Log.v(TAG, "resetData");
		data.clear();
		notifyDataSetChanged();
	}

	public void setServerInfos(Map<String, UserInfo> serverInfos) {
		for(FriendInfo friendInfo : data) {
			friendInfo.serverInfos = new HashMap<>();

			for(String email : friendInfo.emails) {
				UserInfo userInfo = serverInfos.get(email);
				if (userInfo != null) {
					friendInfo.serverInfos.put(email, userInfo);
				}
			}
		}
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
