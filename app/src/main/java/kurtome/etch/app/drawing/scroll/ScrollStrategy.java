package kurtome.etch.app.drawing.scroll;

import android.view.MotionEvent;
import kurtome.etch.app.drawing.DrawingView;
import kurtome.etch.app.drawing.ScrollInfo;

public class ScrollStrategy {
    private final ScrollInfo mScrollInfo = new ScrollInfo();

    private int mScrollActionIndex;
    private float mLastX, mLastY = 0;

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
        mScrollInfo.x = normalizeScrollValue(mScrollInfo.x);
        mScrollInfo.y = normalizeScrollValue(mScrollInfo.y);
    }

    private float normalizeScrollValue(float scroll) {
        if (scroll < -DrawingView.IMAGE_SIZE_PIXELS) {
            return -DrawingView.IMAGE_SIZE_PIXELS;
        }
        if (scroll > DrawingView.IMAGE_SIZE_PIXELS) {
            return DrawingView.IMAGE_SIZE_PIXELS;
        }
        return scroll;
    }

}
