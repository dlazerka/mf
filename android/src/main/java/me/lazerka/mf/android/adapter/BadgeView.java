package me.lazerka.mf.android.adapter;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * @author Dzmitry Lazerka
 */
public class BadgeView extends ImageView {
	public BadgeView(Context context) {
		super(context);
	}

	public BadgeView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BadgeView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public BadgeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Yep, width twice, to make rectangular.
		super.onMeasure(widthMeasureSpec, widthMeasureSpec);
	}
}
