package kurtome.etch.app.drawing;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class DrawingView extends View {

    private static final Logger logger = LoggerManager.getLogger();

    private static final int MAX_BREAK_COUNT = 500;

    private Path mDrawPath;
    private Canvas mDrawCanvas;
    private Bitmap mCanvasBitmap;
    private DrawingBrush mCurrentBrush;

    private float mLastX, mLastY;
    private float mLastLastX, mLastLastY;

    /**
     * Some kind of crazy because too long a path causes performance issues.
     */
    private int mLastBreakCount;

    public static final int IMAGE_SIZE_PIXELS = 1000;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        mDrawPath = new Path();
        mCurrentBrush = new DrawingBrush(getContext());
    }

    //view assigned size
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCanvasBitmap = Bitmap.createBitmap(IMAGE_SIZE_PIXELS, IMAGE_SIZE_PIXELS, Bitmap.Config.ARGB_8888);

        mDrawCanvas = new Canvas(mCanvasBitmap);
    }

    //draw view
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mCanvasBitmap, 0, 0, DrawingBrush.BASIC_PAINT);
        //canvas.drawPath(mDrawPath, mCurrentBrush.getPaint());
    }

    //respond to touch interaction
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
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
                mDrawCanvas.save();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mLastBreakCount < MAX_BREAK_COUNT) {
                    mDrawCanvas.save();
                }
                drawPathForMotion(event);
                if (mLastBreakCount > MAX_BREAK_COUNT) {
                    breakPath();
                }
               break;
            case MotionEvent.ACTION_UP:
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
        invalidate();
        return true;
    }

    private void breakPath() {
        mDrawPath.reset();
        mLastBreakCount = 0;
    }

    private void drawPathForMotion(MotionEvent event) {
        if (mDrawCanvas.getSaveCount() > 0) {
            // Since we are drawing to the canvas permanently on each motion event
            // we need to do save/restore craziness to only draw each path once.
            //
            // The more sane (and performant) way to do this is by only drawing the part of the path
            // we have made so far in onDraw and drawing it permanently to canvas in MOTION_UP....
            // ...BUT  this causes issues with PorterDuff.Mode.SRC which we need for erasing etc.

            mDrawCanvas.restore();
        }

        if (mLastBreakCount == 0) {
            mDrawPath.moveTo(mLastLastX, mLastLastY);
        }
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
        quadPathTo(historicalLastX, historicalLastY, event.getX(), event.getY());
    }

    private void quadPathTo(float lastX, float lastY, float touchX, float touchY) {
        final float x2 = (touchX + lastX) / 2;
        final float y2 = (touchY + lastY) / 2;
        mDrawPath.quadTo(x2, y2, touchX, touchY);
    }

    public DrawingBrush getDrawingBrush()  {
        return mCurrentBrush;
    }


    public void setCurrentImage(byte[] gzipImage) {
        CanvasUtils.clearCanvas(mDrawCanvas);
        if (gzipImage.length > 0) {
            drawEtchToCanvas(gzipImage);
        }
        invalidate();
    }

    private void drawEtchToCanvas(byte[] gzipImage) {
        CanvasUtils.drawBitmapFromGzip(mDrawCanvas, gzipImage);
    }

    public Bitmap getCopyOfCurrentBitmap() {
        return mCanvasBitmap.copy(Bitmap.Config.ARGB_8888, false);
    }

    public byte[] getCurrentImage() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        GZIPOutputStream gzipStream = null;
        try {
            gzipStream = new GZIPOutputStream(stream);
            mCanvasBitmap.compress(Bitmap.CompressFormat.PNG, 100, gzipStream);
//            byte[] bytes = new byte[stream.size()];
//            gzipStream.write(bytes);
            gzipStream.close();
            return stream.toByteArray();
        }
        catch (IOException e) {
            logger.e("Unable to get",e);
            return null;
        }
    }
}