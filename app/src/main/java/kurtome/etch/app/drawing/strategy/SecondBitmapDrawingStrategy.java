package kurtome.etch.app.drawing.strategy;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import kurtome.etch.app.R;
import kurtome.etch.app.colorpickerview.drawable.AlphaPatternDrawable;
import kurtome.etch.app.drawing.CanvasUtils;
import kurtome.etch.app.drawing.DrawingBrush;
import kurtome.etch.app.drawing.ScrollInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Uses a second bitmap to blend before rendering so the blending
 * is always the same regardless of how/when the path is being drawn.
 *
 * (If you simply draw on the draw canvas it doesn't blend right
 *  with more complex PorterDuff modes, like SRC and OVERLAY)
 *
 */
public class SecondBitmapDrawingStrategy implements DrawingStrategy {

    private Path mDrawPath;
    private Canvas mDrawCanvas;
    private Bitmap mCanvasBitmap;

    private Canvas mSecondCanvas;
    private Bitmap mSecondBitmap;

    private Canvas mScrollCanvas;
    private Bitmap mScrollBitmap;

    private DrawingBrush mCurrentBrush;

    private float mLastX, mLastY;
    private float mFirstX, mFirstY;
    private boolean mIsDrawing;
    private ScrollInfo mScroll = new ScrollInfo();

    private static class PaintPath {
        public final Paint paint;
        public final Path path;

        private PaintPath(DrawingBrush brush, Path path) {
            this.paint = new Paint(brush.getPaint());
            this.path = path;
        }
    }

    private static final int MAX_PATH_UNDO_SIZE = 10;

    private List<PaintPath> mDrawnPathsQueue = Lists.newLinkedList();

    private final Bitmap mAlphaPatternBitmap;
    private final Paint mBackgroundPaint = DrawingBrush.createBasicPaint();

    public SecondBitmapDrawingStrategy(Context context, Canvas drawCanvas, Bitmap canvasBitmap, DrawingBrush drawingBrush) {
        this.mDrawCanvas = drawCanvas;
        this.mCanvasBitmap = canvasBitmap;

        this.mSecondBitmap = Bitmap.createBitmap(mCanvasBitmap);
        this.mSecondCanvas = new Canvas(mSecondBitmap);

        this.mScrollBitmap = Bitmap.createBitmap(mCanvasBitmap);
        this.mScrollCanvas = new Canvas(mScrollBitmap);
        this.mDrawPath = new Path();

        mCurrentBrush = drawingBrush;
        mBackgroundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
        mBackgroundPaint.setColor(context.getResources().getColor(R.color.dark_contrast));
        mBackgroundPaint.setAlpha(210);

        int alphaRectSizePx = 20;
        mAlphaPatternBitmap = AlphaPatternDrawable.createPatternBitmap(mSecondBitmap.getWidth(), mSecondBitmap.getHeight(), alphaRectSizePx);
    }

    @Override
    public void draw(Canvas canvas) {
        CanvasUtils.clearCanvas(mSecondCanvas);
        // let the second canvas do the blending on its bitmap
        mSecondCanvas.drawBitmap(mCanvasBitmap, 0, 0, DrawingBrush.BASIC_PAINT);

        for (PaintPath paintPath : mDrawnPathsQueue) {
            mSecondCanvas.drawPath(paintPath.path, paintPath.paint);
        }
        if (mDrawPath != null && mIsDrawing) {
            mSecondCanvas.drawPath(mDrawPath, mCurrentBrush.getPaint());
        }
        // This must be last to not change how the blending of the path and current etch look
//        mSecondCanvas.drawBitmap(mAlphaPatternBitmap, 0, 0, mBackgroundPaint);

        //mScrollCanvas.drawColor(Color.GRAY); // clear to a solid background color
        CanvasUtils.clearCanvas(mScrollCanvas);
        mScrollCanvas.drawPaint(mBackgroundPaint);
        CanvasUtils.clearRect(mScrollCanvas, new RectF(
                mScroll.x,
                mScroll.y,
                mScroll.x + mCanvasBitmap.getWidth(),
                mScroll.y + mCanvasBitmap.getHeight()
        ));
        mScrollCanvas.drawBitmap(mSecondBitmap, mScroll.x, mScroll.y, DrawingBrush.BASIC_PAINT);

        canvas.drawBitmap(mScrollBitmap, 0, 0, DrawingBrush.BASIC_PAINT);
    }

    @Override
    public boolean touchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        // NOTE: try not to create new objects in here, this is called A LOT

        //respond to down, move and up events
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = touchX;
                mLastY = touchY;
                mFirstX = touchX;
                mFirstY = touchY;
                float x = touchX - mScroll.x;
                float y = touchY - mScroll.y;
                mDrawPath.moveTo(x-1, y-1);
                mDrawPath.lineTo(x, y); // immediately create one px line so a single tap will make a dot
                mIsDrawing = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mIsDrawing) {
                    if (Math.abs(touchX - mFirstX) > 4 || Math.abs(touchY - mLastY) > 4) {
                        // Store restore state just before beginning new path
                        mIsDrawing = true;
                    }
                }
                updatePathForMotion(event);
                break;
            case MotionEvent.ACTION_UP:
                updatePathForMotion(event);
                if (mIsDrawing) {
                    mDrawnPathsQueue.add(new PaintPath(mCurrentBrush, mDrawPath));
                    if (mDrawnPathsQueue.size() > MAX_PATH_UNDO_SIZE) {
                        PaintPath oldestPath = Iterables.getFirst(mDrawnPathsQueue, null);
                        mDrawCanvas.drawPath(oldestPath.path, oldestPath.paint);
                        mDrawnPathsQueue.remove(oldestPath);
                    }
                }
                mIsDrawing = false;
                mDrawPath = new Path();
                break;
            default:
                return false;
        }

        mLastX = touchX;
        mLastY = touchY;
        return true;
    }


    private void updatePathForMotion(MotionEvent event) {
        quadThroughMotionCoords(event);
    }

    private void quadThroughMotionCoords(MotionEvent event) {
        int historySize = event.getHistorySize();
        float historicalLastX = mLastX - mScroll.x;
        float historicalLastY = mLastY - mScroll.y;
        for (int historyPos = 0; historyPos < historySize; historyPos++) {
            float x = event.getHistoricalX(historyPos) - mScroll.x;
            float y = event.getHistoricalY(historyPos) - mScroll.y;
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

    public void setScrollInfo(ScrollInfo scrollInfo) {
        this.mScroll = scrollInfo;
    }

    @Override
    public boolean isDrawing() {
        return mIsDrawing;
    }

    @Override
    public void sizeChanged(int width, int height, int oldWidth, int oldHeight) {
        this.mScrollBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.mScrollCanvas = new Canvas(mScrollBitmap);

//        int extraWidth = width - mCanvasBitmap.getWidth();
//        if (extraWidth > 0) {
//            this.mScroll.x = extraWidth / 2;
//        }
//
//        int extraHeight = height - mCanvasBitmap.getHeight();
//        if (extraHeight > 0) {
//            this.mScroll.y = extraHeight / 2;
//        }
    }

    @Override
    public DrawingBrush getBrush() {
        return mCurrentBrush;
    }

    @Override
    public void undoLastDraw() {
        int savedPathsSize = mDrawnPathsQueue.size();
        if (savedPathsSize > 0) {
            mDrawnPathsQueue.remove(savedPathsSize - 1);
        }
    }

    @Override
    public void flush() {
        for (PaintPath paintPath : mDrawnPathsQueue) {
            mDrawCanvas.drawPath(paintPath.path, paintPath.paint);
        }
        mDrawnPathsQueue.clear();
    }

    @Override
    public void forceStartDrawing() {
        mIsDrawing = true;
    }
}
