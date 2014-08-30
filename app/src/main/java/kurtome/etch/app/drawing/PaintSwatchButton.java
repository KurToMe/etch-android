package kurtome.etch.app.drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.ImageButton;
import kurtome.etch.app.R;
import kurtome.etch.app.colorpickerview.drawable.AlphaPatternDrawable;

public class PaintSwatchButton extends ImageButton {

    private boolean mInitialized = false;
    private Canvas mMainCanvas;
    private Canvas mColorCanvas;
    private Bitmap mColorBitmap;
    private int mColorOffset;
    private Bitmap mAlphaBitmap;



    public PaintSwatchButton(Context context) {
        super(context);
        init();
    }

    public PaintSwatchButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PaintSwatchButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        if (mInitialized) {
            throw new IllegalStateException("Cannot init twice.");
        }

        mInitialized = true;

        int size = getResources().getDimensionPixelSize(R.dimen.action_icon_size);
        mColorOffset = getResources().getDimensionPixelSize(R.dimen.action_icon_content_margin);
        int colorSize = getResources().getDimensionPixelSize(R.dimen.action_icon_content_size);

        Bitmap mainBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        mMainCanvas = new Canvas(mainBitmap);
        setImageBitmap(mainBitmap);

        mColorBitmap = Bitmap.createBitmap(colorSize, colorSize, Bitmap.Config.ARGB_8888);
        mColorCanvas = new Canvas(mColorBitmap);

        mAlphaBitmap = AlphaPatternDrawable.createPatternBitmap(colorSize, colorSize);

        useColor(Color.TRANSPARENT);
    }

    public void setColor(int color) {
        useColor(color);
    }

    private void useColor(int color) {
        CanvasUtils.clearCanvas(mMainCanvas);
        CanvasUtils.clearCanvas(mColorCanvas);

        mColorCanvas.drawBitmap(mAlphaBitmap, 0, 0, DrawingBrush.BASIC_PAINT);
        mColorCanvas.drawColor(color);

        int borderSize = 3;
        mMainCanvas.drawRect(
                mColorOffset - borderSize,
                mColorOffset - borderSize,
                mColorBitmap.getWidth() + mColorOffset + borderSize,
                mColorBitmap.getHeight() + mColorOffset + borderSize,
                DrawingBrush.createBasicPaintWithColor(Color.WHITE)
        );
        mMainCanvas.drawBitmap(mColorBitmap, mColorOffset, mColorOffset, DrawingBrush.BASIC_PAINT);

        invalidate();
    }
}
