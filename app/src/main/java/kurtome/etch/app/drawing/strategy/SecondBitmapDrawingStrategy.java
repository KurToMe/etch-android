package kurtome.etch.app.drawing.strategy;

import android.graphics.*;
import android.view.MotionEvent;
import kurtome.etch.app.drawing.CanvasUtils;
import kurtome.etch.app.drawing.DrawingBrush;

/**
 * Uses a second bitmap to blend before rendering so the blending
 * is always the same regardless of how/when the path is being drawn.
 *
 * (If you simply draw on the draw canvas it doesn't blend right
 *  with more complex PorterDuff modes, like SRC and OVERLAY)
 *
 */
public class SecondBitmapDrawingStrategy implements DrawingStrategy {

    private Canvas mSecondCanvas;
    private Path mDrawPath;
    private Canvas mDrawCanvas;
    private Bitmap mCanvasBitmap;
    private Bitmap mSecondBitmap;
    private DrawingBrush mCurrentBrush;

    private float mLastX, mLastY;

    public SecondBitmapDrawingStrategy(Canvas drawCanvas, Bitmap canvasBitmap) {
        this.mDrawCanvas = drawCanvas;
        this.mCanvasBitmap = canvasBitmap;
        this.mSecondBitmap = Bitmap.createBitmap(mCanvasBitmap);
        this.mSecondCanvas = new Canvas(mSecondBitmap);
        mDrawPath = new Path();
        mCurrentBrush = new DrawingBrush();
    }

    //view assigned size
    @Override
    public void draw(Canvas canvas) {
        // let the second canvas do the blending on its bitmap
        CanvasUtils.clearCanvas(mSecondCanvas);
        mSecondCanvas.drawBitmap(mCanvasBitmap, 0, 0, DrawingBrush.BASIC_PAINT);
        mSecondCanvas.drawPath(mDrawPath, mCurrentBrush.getPaint());

        canvas.drawBitmap(mSecondBitmap, 0, 0, DrawingBrush.BASIC_PAINT);
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
                mLastX = touchX;
                mLastY = touchY;
                mDrawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPathForMotion(event);
                break;
            case MotionEvent.ACTION_UP:
                drawPathForMotion(event);
                mDrawCanvas.drawPath(mDrawPath, mCurrentBrush.getPaint());
                mDrawPath.reset();
                break;
            default:
                return false;
        }

        mLastX = touchX;
        mLastY = touchY;
        return true;
    }


    private void drawPathForMotion(MotionEvent event) {
        quadThroughMotionCoords(event);
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
    }

    private void quadPathTo(float lastX, float lastY, float touchX, float touchY) {
        final float x2 = (touchX + lastX) / 2;
        final float y2 = (touchY + lastY) / 2;
        mDrawPath.quadTo(x2, y2, touchX, touchY);
    }

    @Override
    public DrawingBrush getBrush() {
        return mCurrentBrush;
    }
}
