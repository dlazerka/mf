package me.lazerka.mf.android.adapter;

import android.graphics.Outline;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
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
	private static final String TAG = FriendViewHolder.class.getName();

	// Also see setRect() below.
	private static final RectShape SHAPE = new RectShape();

	public FriendViewHolder(View itemView) {
		super(itemView);
	}

	public void bindFriend(FriendInfo friendInfo) {
		Log.v(TAG, "bindFriend: " + friendInfo.id);

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
