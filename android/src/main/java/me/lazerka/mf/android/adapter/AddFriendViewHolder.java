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
