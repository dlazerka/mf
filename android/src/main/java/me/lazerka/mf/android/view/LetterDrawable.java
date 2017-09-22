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

package me.lazerka.mf.android.view;

import android.graphics.*;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;

import java.util.Arrays;
import java.util.List;

/**
 * @author Dzmitry Lazerka
 */
public class LetterDrawable extends ShapeDrawable {
	private static final List<Integer> MATERIAL_COLORS = Arrays.asList(
			0xffe57373,
			0xfff06292,
			0xffba68c8,
			0xff9575cd,
			0xff7986cb,
			0xff64b5f6,
			0xff4fc3f7,
			0xff4dd0e1,
			0xff4db6ac,
			0xff81c784,
			0xffaed581,
			0xffff8a65,
			0xffd4e157,
			0xffffd54f,
			0xffffb74d,
			0xffa1887f,
			0xff90a4ae
	);

	private static Typeface typeface;

	private final int color;
	private final char[] text;

	public LetterDrawable(Shape shape, char letter, int id) {
		super(shape);
		this.text = new char[] {letter};
		this.color = MATERIAL_COLORS.get(Math.abs(id) % MATERIAL_COLORS.size());

		if (typeface == null) {
			typeface = Typeface.create("sans-serif-light", Typeface.NORMAL);
		}
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);

		// text paint settings
		Paint textPaint = new Paint();
		textPaint.setColor(Color.WHITE);
		textPaint.setAntiAlias(true);
		textPaint.setStyle(Paint.Style.FILL);
		textPaint.setTypeface(typeface);
		textPaint.setTextAlign(Paint.Align.CENTER);

		// Fill background.
		Paint paint = getPaint();
		paint.setColor(color);
		getShape().draw(canvas, paint);

		Rect r = getBounds();
		int count = canvas.save();
		canvas.translate(r.left, r.top);

		// draw text
		int width = r.width();
		int height = r.height();
		int fontSize = Math.min(width, height) / 2;
		textPaint.setTextSize(fontSize);
		canvas.drawText(
				text,
				0,
				text.length,
				width / 2,
				height / 2 - ((textPaint.descent() + textPaint.ascent()) / 2),
				textPaint
		);

		canvas.restoreToCount(count);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public int getIntrinsicWidth() {
		return -1;
	}

	@Override
	public int getIntrinsicHeight() {
		return -1;
	}
}
