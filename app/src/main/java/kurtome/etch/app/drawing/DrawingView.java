package kurtome.etch.app.drawing;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
import com.squareup.otto.Bus;
import kurtome.etch.app.drawing.scroll.ScrollStrategy;
import kurtome.etch.app.drawing.strategy.DrawingStrategy;
import kurtome.etch.app.drawing.strategy.SecondBitmapDrawingStrategy;
import kurtome.etch.app.gsm.EtchOverlayImage;
import kurtome.etch.app.util.RectangleDimensions;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class DrawingView extends View {

    private static final Logger logger = LoggerManager.getLogger();

    private final DrawingBrush mDrawingBrush;
    private Canvas mDrawCanvas;
    private Bitmap mCanvasBitmap;
    private TouchType mTouchType = TouchType.NONE;
    private boolean mInitialized;
    private double mAspectRatio;
    private RectangleDimensions mEtchDimens;
    private EtchOverlayImage mEtch;

    private byte[] mGzipImage;


    private enum TouchType {
        NONE,
        DRAW,
        SCROLL
    }

    private SecondBitmapDrawingStrategy mDrawingStrategy;
//    private ScrollStrategy mScrollStrategy;

    /**
     * Some kind of crazy because too long a path causes performance issues.
     */
    private int mLastBreakCount;

    /**
     * Using a constant height because the distance between two latitude points
     * is roughly the same anywhere (this is not true of longitude, so we'll let the width vary)
     */
    public static final int IMAGE_HEIGHT_PIXELS = 1024;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mDrawingBrush = new DrawingBrush();
    }

    public static Bitmap createBitmap(int width, int height) {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    public void setupFromEtch(EtchOverlayImage etch) {
        if (mInitialized) {
            throw new IllegalStateException("Should only initialize once.");
        }
        mEtch = etch;

        mInitialized = true;
    }

    private void setup() {
        mAspectRatio = mEtch.getAspectRatio();
        int width = RectangleUtils.calcWidthWithAspectRatio(IMAGE_HEIGHT_PIXELS, mAspectRatio);
        mEtchDimens = new RectangleDimensions(width, IMAGE_HEIGHT_PIXELS);
        mCanvasBitmap = createBitmap(width, IMAGE_HEIGHT_PIXELS);

        mDrawCanvas = new Canvas(mCanvasBitmap);

        mDrawingStrategy = new SecondBitmapDrawingStrategy(
                this.getContext(),
                mDrawCanvas,
                mCanvasBitmap,
                mDrawingBrush
        );

        updateScaleAndPosition();
    }

    public void onMapFinishedChanging() {
        setup();
        updateScaleAndPosition();
        ensureImageSet();
    }

    public void updateScaleAndPosition() {
        if (mDrawingStrategy == null) {
            return;
        }

        RectangleDimensions dimensions = mEtch.getCurrentScreenDimensions();
        float scale = ((float) dimensions.height) / IMAGE_HEIGHT_PIXELS;
        logger.i("Scale is %s", scale);
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        // Not sure how scaling will effect drawing. Might not need it if etches fit on all
        // screen sizes
//        mDrawCanvas.setMatrix(matrix);

        ScrollInfo scrollInfo = new ScrollInfo();
        Point etchOrigin = mEtch.currentOriginPoint();
        float x = this.getX();
        float y = this.getY();
        scrollInfo.x = etchOrigin.x - x;
        scrollInfo.y = etchOrigin.y - y;
        logger.i("Setting scroll offset to (%s, %s)", scrollInfo.x, scrollInfo.y);
        mDrawingStrategy.setScrollInfo(scrollInfo);
        mDrawingStrategy.sizeChanged(
                this.getWidth(), this.getHeight(),
                this.getWidth(), this.getHeight()
        );
    }

    //view assigned size
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        logger.i("Sized chaned to (%s, %s) from (%s, %s)", w, h, oldw, oldh);
        super.onSizeChanged(w, h, oldw, oldh);
        if (mDrawingStrategy != null) {
            mDrawingStrategy.sizeChanged(w, h, oldw, oldh);
        }
    }

    //draw view
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDrawingStrategy != null) {
            mDrawingStrategy.draw(canvas);
        }
    }

    //respond to touch interaction
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        mDrawingStrategy.touchEvent(event);
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            // we can safely always pass the action down to
//            // the drawing because it just preps stuff
//            mDrawingStrategy.touchEvent(event);
//            mScrollStrategy.touchEvent(event);
//        }
//
//        if (mTouchType == TouchType.NONE) {
//            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
//                mDrawingStrategy.touchEvent(event);
//                if (mDrawingStrategy.isDrawing()) {
//                    mTouchType = TouchType.DRAW;
//                }
//            }
//            else if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
//                mTouchType = TouchType.SCROLL;
//                mScrollStrategy.touchEvent(event);
//            }
//        }
//        else if (mTouchType == TouchType.DRAW) {
//            mDrawingStrategy.touchEvent(event);
//        }
//        else if (mTouchType == TouchType.SCROLL) {
//            mScrollStrategy.touchEvent(event);
//        }
//
//        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
//            if (mTouchType == TouchType.NONE) {
//                // If we never figured out how to handle this action,
//                // it should be drawing (let's a quick tap draw a dot)
//                mDrawingStrategy.forceStartDrawing();
//            }
//            // let drawing strategy clean-up
//            mDrawingStrategy.touchEvent(event);
//
//            mScrollStrategy.touchEvent(event);
//
//            mTouchType = TouchType.NONE;
//        }

        invalidate();
        return true;
    }

    public DrawingBrush getDrawingBrush()  {
        return mDrawingBrush;
    }

    public void setCurrentImage(byte[] gzipImage) {
        synchronized (this) {
            mGzipImage = gzipImage;
        }
        ensureImageSet();
    }

    private void ensureImageSet() {
        if (mDrawCanvas == null) {
            return;
        }

        byte[] gzipImage = null;
        synchronized (this) {
            gzipImage = mGzipImage;
            mGzipImage = null;
        }
        if (gzipImage == null) {
            return;
        }

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
        mDrawingStrategy.flush();
        return mCanvasBitmap.copy(Bitmap.Config.ARGB_8888, false);
    }

    public byte[] getCurrentImage() {
        mDrawingStrategy.flush();
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

    public void undoLastDraw()  {
        mDrawingStrategy.undoLastDraw();
        invalidate();
    }

}