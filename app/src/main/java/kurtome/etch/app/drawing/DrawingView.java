package kurtome.etch.app.drawing;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
import kurtome.etch.app.drawing.strategy.BitmapDrawingStrategy;
import kurtome.etch.app.drawing.strategy.DrawingStrategy;
import kurtome.etch.app.drawing.strategy.SecondBitmapDrawingStrategy;
import kurtome.etch.app.drawing.strategy.SimpleDrawingStrategy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class DrawingView extends View {

    private static final Logger logger = LoggerManager.getLogger();

    private Canvas mDrawCanvas;
    private Bitmap mCanvasBitmap;

    private DrawingStrategy mDrawingStrategy;

    /**
     * Some kind of crazy because too long a path causes performance issues.
     */
    private int mLastBreakCount;

    public static final int IMAGE_SIZE_PIXELS = 1000;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mCanvasBitmap = Bitmap.createBitmap(IMAGE_SIZE_PIXELS, IMAGE_SIZE_PIXELS, Bitmap.Config.ARGB_8888);

        mDrawCanvas = new Canvas(mCanvasBitmap);

        mDrawingStrategy = new SecondBitmapDrawingStrategy(mDrawCanvas, mCanvasBitmap);
    }


    //view assigned size
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    //draw view
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mDrawingStrategy.onDraw(canvas);
    }

    //respond to touch interaction
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        mDrawingStrategy.onTouchEvent(event);
        invalidate();
        return true;
    }

    public DrawingBrush getDrawingBrush()  {
        return mDrawingStrategy.getBrush();
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