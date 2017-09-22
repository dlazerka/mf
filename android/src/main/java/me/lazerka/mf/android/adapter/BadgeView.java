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
