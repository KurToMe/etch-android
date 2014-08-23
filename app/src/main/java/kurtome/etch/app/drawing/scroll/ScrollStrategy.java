package kurtome.etch.app.drawing.scroll;

import android.view.MotionEvent;
import kurtome.etch.app.drawing.ScrollInfo;

public class ScrollStrategy {
    private final ScrollInfo mScrollInfo = new ScrollInfo();

    private int mScrollActionIndex;
    private float mLastX, mLastY = 0;

    private final int mMaxWidthOffset;
    private final int mMaxHeightOffset;
    private final int mMinWidthOffset;
    private final int mMinHeightOffset;

    public ScrollStrategy(int width, int height) {
        mMaxWidthOffset = width;
        mMaxHeightOffset = height;
        mMinWidthOffset = -width;
        mMinHeightOffset = -height;
    }

    public ScrollInfo getScrollInfo() {
        return mScrollInfo;
    }

    public boolean touchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            mScrollActionIndex = 0;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mLastX = event.getX(mScrollActionIndex);
            mLastY = event.getY(mScrollActionIndex);
        }
        else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (mScrollActionIndex > 0) {
                float diffX = event.getX(mScrollActionIndex) - mLastX;
                float diffY = event.getY(mScrollActionIndex) - mLastY;
                mScrollInfo.x += diffX;
                mScrollInfo.y += diffY;
                normalizeScroll();
            }
        }
        else if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
            if (event.getActionIndex() == mScrollActionIndex) {
                mScrollActionIndex = 0;
            }
        }
        else if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            if (mScrollActionIndex == 0) {
                mScrollActionIndex = event.getActionIndex();
            }
        }
        else if (event.getActionMasked() == MotionEvent.ACTION_UP) {

            mScrollActionIndex = 0;
        }

        mLastX = event.getX(mScrollActionIndex);
        mLastY = event.getY(mScrollActionIndex);

        return true;
    }

    private void normalizeScroll() {
        mScrollInfo.x = normalizeValue(mScrollInfo.x, mMinWidthOffset, mMaxWidthOffset);
        mScrollInfo.y = normalizeValue(mScrollInfo.y, mMinHeightOffset, mMaxHeightOffset);
    }

    private float normalizeValue(float scroll, float min, float max) {
        if (scroll < min) {
            return min;
        }
        if (scroll > max) {
            return max;
        }
        return scroll;
    }

}
