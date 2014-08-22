/*
 * Copyright (C) 2010 Daniel Nilsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kurtome.etch.app.colorpickerview.drawable;

import android.graphics.*;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;

/**
 * This drawable will draw a simple white and gray chessboard pattern.
 * It's pattern you will often see as a background behind a
 * partly transparent image in many applications.
 *
 * @author Daniel Nilsson
 */
public class AlphaPatternDrawable extends Drawable {

    private final static int DEFAULT_RECTANGLE_SIZE_PX = 10;

    private final int mRectangleSize;

    private Paint mPaint = new Paint();

    private static Paint sPaintWhite = new Paint();
    private static Paint sPaintGray = new Paint();

    /**
     * Bitmap in which the pattern will be cached.
     * This is so the pattern will not have to be recreated each time draw() gets called.
     * Because recreating the pattern i rather expensive. I will only be recreated if the
     * size changes.
     */
    private Bitmap mBitmap;

    static {
        sPaintWhite.setColor(0xffffffff);
        sPaintGray.setColor(0xffcbcbcb);
    }

    public AlphaPatternDrawable(int rectangleSize) {
        mRectangleSize = rectangleSize;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, null, getBounds(), mPaint);
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public void setAlpha(int alpha) {
        throw new UnsupportedOperationException("Alpha is not supported by this drawwable.");
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        throw new UnsupportedOperationException("ColorFilter is not supported by this drawwable.");
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        if (getBounds().width() <= 0 || getBounds().height() <= 0) {
            return;
        }

        /**
        * We do this to chache the bitmap so we don't need to
        * recreate it each time draw() is called since it
        * takes a few milliseconds.
        */
        mBitmap = createPatternBitmap(getBounds().width(), getBounds().height(), mRectangleSize);
    }

    /**
     * This will generate a bitmap with the pattern
     * as big as the rectangle we were allow to draw on.
     *
     * TODO - probably move this to another class
     */
    public static Bitmap createPatternBitmap(int width, int height, int rectSize) {
        if (width <= 0 || height <= 0 || rectSize <= 0) {
            throw new IllegalArgumentException("Invalid dimensions.");
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Rect r = new Rect();
        boolean verticalStartWhite = true;

        int numRectanglesHorizontal = (int) Math.ceil((width / rectSize));
        int numRectanglesVertical = (int) Math.ceil(height / rectSize);

        for (int i = 0; i <= numRectanglesVertical; i++) {

            boolean isWhite = verticalStartWhite;
            for (int j = 0; j <= numRectanglesHorizontal; j++) {

                r.top = i * rectSize;
                r.left = j * rectSize;
                r.bottom = r.top + rectSize;
                r.right = r.left + rectSize;

                canvas.drawRect(r, isWhite ? sPaintWhite : sPaintGray);

                isWhite = !isWhite;
            }

            verticalStartWhite = !verticalStartWhite;

        }
        return bitmap;
    }

}
