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

package me.lazerka.mf.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

/**
 * Relative layout that supports its children to have animations based on their height.
 *
 * Inspired by http://trickyandroid.com/fragments-translate-animation/
 *
 * Also can be done by overriding onCreateAnimator() in fragments classes (subject to pre-draw as well).
 *
 * @author Dzmitry Lazerka
 */
public class SlidingFrameLayout extends FrameLayout {
	private float heightFraction;

	private ViewTreeObserver.OnPreDrawListener preDrawListener = null;

	public SlidingFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SlidingFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public SlidingFrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public void setTranslationHeightFraction(float fraction) {
		heightFraction = fraction;

		// We might not yet have our height measured, so let's add an OnPreDrawListener.
		if (getHeight() == 0) {
			if (preDrawListener == null) {
				preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
					@Override
					public boolean onPreDraw() {
						getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
						setTranslationHeightFraction(heightFraction);
						return true;
					}
				};
				getViewTreeObserver().addOnPreDrawListener(preDrawListener);
			}
			return;
		}

		float translationY = getHeight() * fraction;
		setTranslationY(translationY);
	}

	public float getTranslationHeightFraction() {
		return heightFraction;
	}
}
