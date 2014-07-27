package kurtome.etch.app.drawing;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
import kurtome.etch.app.GzipUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class DrawingView extends View {
    private static final Logger logger = LoggerManager.getLogger();

    private Path drawPath;
    private Canvas mDrawCanvas;
    private Bitmap canvasBitmap;
    private DrawingBrush currentBrush;

    private float lastX, lastY;

    public static final int IMAGE_SIZE_PIXELS = 1000;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        drawPath = new Path();
        currentBrush = new DrawingBrush();
    }

    //view assigned size
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(IMAGE_SIZE_PIXELS, IMAGE_SIZE_PIXELS, Bitmap.Config.ARGB_8888);

        mDrawCanvas = new Canvas(canvasBitmap);
    }

    //draw view
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(canvasBitmap, 0, 0, DrawingBrush.BASIC_PAINT);
        canvas.drawPath(drawPath, currentBrush.getPaint());
    }

    //respond to touch interaction
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        float touchX = event.getX();
        float touchY = event.getY();

        //respond to down, move and up events
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                quadPathTo(touchX, touchY);
               break;
            case MotionEvent.ACTION_UP:
//                drawPath.lineTo(touchX, touchY);
                quadPathTo(touchX, touchY);
                mDrawCanvas.drawPath(drawPath, currentBrush.getPaint());
                drawPath = new Path();
                break;
            default:
                return false;
        }
        lastX = touchX;
        lastY = touchY;
        invalidate();
        return true;
    }

    private void quadPathTo(float touchX, float touchY) {
        final float x2 = (touchX + lastX) / 2;
        final float y2 = (touchY + lastY) / 2;
        drawPath.quadTo(x2, y2, touchX, touchY);
    }

    public DrawingBrush getDrawingBrush()  {
        return currentBrush;
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
        return canvasBitmap.copy(Bitmap.Config.ARGB_8888, false);
    }

    public byte[] getCurrentImage() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        GZIPOutputStream gzipStream = null;
        try {
            gzipStream = new GZIPOutputStream(stream);
            canvasBitmap.compress(Bitmap.CompressFormat.PNG, 100, gzipStream);
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