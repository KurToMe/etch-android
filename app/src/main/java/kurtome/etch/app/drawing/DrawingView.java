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

    private Path mDrawPath;
    private Canvas mDrawCanvas;
    private Bitmap mCanvasBitmap;
    private DrawingBrush mCurrentBrush;

    private float mLastX, mLastY;

    public static final int IMAGE_SIZE_PIXELS = 1000;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        mDrawPath = new Path();
        mCurrentBrush = new DrawingBrush();
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
                mDrawPath.moveTo(touchX, touchY);
                mDrawCanvas.save();
                break;
            case MotionEvent.ACTION_MOVE:
                mDrawCanvas.restore();
                mDrawCanvas.save();
                drawPathForMotion(event);
               break;
            case MotionEvent.ACTION_UP:
                mDrawCanvas.restore();
                drawPathForMotion(event);
//                mDrawCanvas.fl
                mDrawPath.reset();
                break;
            default:
                return false;
        }
        //mDrawCanvas.drawBitmap(mCanvasBitmap, 0, 0, DrawingBrush.BASIC_PAINT);

        mLastX = touchX;
        mLastY = touchY;
        invalidate();
        return true;
    }

    private void drawPathForMotion(MotionEvent event) {
//        mDrawPath.moveTo(mLastX, mLastY);
        quadThroughMotionCoords(event);
        mDrawCanvas.drawPath(mDrawPath, mCurrentBrush.getPaint());
    }

    private void quadThroughMotionCoords(MotionEvent event) {
        int historySize = event.getHistorySize();
        float historicalLastX = mLastX;
        float historicalLastY = mLastY;
        for (int historyPos = 0; historyPos < historySize; historyPos++) {
            float x = event.getHistoricalX(historyPos);
            float y = event.getHistoricalY(historyPos);
            quadPathTo(historicalLastX, historicalLastY, x, y);
        }
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