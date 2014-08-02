package kurtome.etch.app.drawing.strategy;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.view.MotionEvent;
import kurtome.etch.app.drawing.DrawingBrush;

public class BitmapDrawingStrategy implements DrawingStrategy {
    private static final int MAX_BREAK_COUNT = 100;

    private Path mDrawPath;
    private Canvas mDrawCanvas;
    private Bitmap mCanvasBitmap;
    private DrawingBrush mCurrentBrush;

    private float mLastX, mLastY;
    private float mLastLastX, mLastLastY;
    private int mSaveCount;

    /**
     * Some kind of crazy because too long a path causes performance issues.
     */
    private int mLastBreakCount;

    public BitmapDrawingStrategy(Canvas drawCanvas, Bitmap canvasBitmap) {
        this.mDrawCanvas = drawCanvas;
        this.mCanvasBitmap = canvasBitmap;
        mDrawPath = new Path();
        mCurrentBrush = new DrawingBrush();
    }

    //view assigned size
    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(mCanvasBitmap, 0, 0, DrawingBrush.BASIC_PAINT);
        //canvas.drawPath(mDrawPath, mCurrentBrush.getPaint());
    }

    //respond to touch interaction
    @Override
    public boolean touchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        // NOTE: try not o create new objects in here, this is called A LOT

        //respond to down, move and up events
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastBreakCount = 0;
                mLastX = touchX;
                mLastY = touchY;
                mDrawPath.moveTo(touchX, touchY);
                mSaveCount = mDrawCanvas.saveLayer(null, DrawingBrush.BASIC_PAINT, Canvas.ALL_SAVE_FLAG);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mSaveCount > 0) {
                    // Since we are drawing to the canvas permanently on each motion event
                    // we need to do save/restore craziness to only draw each path once.
                    //
                    // The more sane (and performant) way to do this is by only drawing the part of the path
                    // we have made so far in draw and drawing it permanently to canvas in MOTION_UP....
                    // ...BUT  this causes issues with PorterDuff.Mode.SRC which we need for erasing etc.

//                    mDrawCanvas.restoreToCount(mSaveCount);
//                    mSaveCount = 0;
                }
                if (mLastBreakCount < MAX_BREAK_COUNT) {
//                    mSaveCount = mDrawCanvas.saveLayer(0, 0, IMAGE_SIZE_PIXELS, IMAGE_SIZE_PIXELS, DrawingBrush.BASIC_PAINT, Canvas.ALL_SAVE_FLAG);
                }
                drawPathForMotion(event);
                if (mLastBreakCount >= MAX_BREAK_COUNT) {
                    breakPath();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mSaveCount > 0) {
                    // Since we are drawing to the canvas permanently on each motion event
                    // we need to do save/restore craziness to only draw each path once.
                    //
                    // The more sane (and performant) way to do this is by only drawing the part of the path
                    // we have made so far in draw and drawing it permanently to canvas in MOTION_UP....
                    // ...BUT  this causes issues with PorterDuff.Mode.SRC which we need for erasing etc.

                    mDrawCanvas.restoreToCount(mSaveCount);
                    mSaveCount = 0;
                }
                drawPathForMotion(event);
                breakPath();
                break;
            default:
                return false;
        }

        mLastLastX = mLastX;
        mLastLastY = mLastY;
        mLastX = touchX;
        mLastY = touchY;
        return true;
    }

    @Override
    public DrawingBrush getBrush() {
        return mCurrentBrush;
    }

    private void breakPath() {
        mDrawPath.reset();
        mDrawPath.moveTo(mLastX, mLastY);
        mLastBreakCount = 0;
    }

    private void drawPathForMotion(MotionEvent event) {
//        if (mLastBreakCount == 0) {
//            mDrawPath.moveTo(mLastX, mLastY);
//        }
        quadThroughMotionCoords(event);
        mDrawCanvas.drawPath(mDrawPath, mCurrentBrush.getPaint());
        mLastBreakCount++;
    }

    private void quadThroughMotionCoords(MotionEvent event) {
        int historySize = event.getHistorySize();
        float historicalLastX = mLastX;
        float historicalLastY = mLastY;
        for (int historyPos = 0; historyPos < historySize; historyPos++) {
            float x = event.getHistoricalX(historyPos);
            float y = event.getHistoricalY(historyPos);
            quadPathTo(historicalLastX, historicalLastY, x, y);
            historicalLastX = x;
            historicalLastY = y;
        }
//        quadPathTo(historicalLastX, historicalLastY, event.getX(), event.getY());
    }

    private void quadPathTo(float lastX, float lastY, float touchX, float touchY) {
        final float x2 = (touchX + lastX) / 2;
        final float y2 = (touchY + lastY) / 2;
//        mDrawPath.quadTo(x2, y2, touchX, touchY);
        mDrawPath.lineTo(touchX, touchY);
    }

}
