package kurtome.etch.app.drawing;

import android.content.Context;
import android.graphics.*;

public class DrawingBrush {

    public static final int MAX_ALPHA = 255;

    public static final Paint BASIC_PAINT = DrawingBrush.createBasicPaint();
    private PorterDuff.Mode mMode;

    public static Paint createBasicPaint() {
        Paint paint = new Paint(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setAntiAlias(true);
        return paint;
    }

    public static Paint createBasicPaintWithColor(int color) {
        Paint paint = createBasicPaint();
        paint.setColor(color);
        return paint;
    }

    private Paint mPaint;

    public DrawingBrush() {
        mPaint = createBasicPaint();
        mPaint.setStrokeWidth(15);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setFilterBitmap(true);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setColor(Color.BLACK);

//        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.clean_gray_paper);
//        BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
//        mPaint.setShader(new LinearGradient());
//
//        mPaint.setStyle(Paint.Style.STROKE);
//        mPaint.setMaskFilter(new BlurMaskFilter(mPaint.getStrokeWidth() / 2, BlurMaskFilter.Blur.NORMAL));

        mMode = PorterDuff.Mode.SRC_OVER;
        mPaint.setXfermode(new PorterDuffXfermode(mMode));
    }

    public Paint getPaint() {
        return mPaint;
    }


    public int getColor() {
        return mPaint.getColor();
    }


    public void setColor(int color) {
        mPaint.setColor(color);
    }


    public void setStrokeWidth(int newStrokeWidth) {
        mPaint.setStrokeWidth(newStrokeWidth);
    }

    public PorterDuff.Mode getMode() {
        return mMode;
    }

    public void setMode(PorterDuff.Mode mode) {
        mMode = mode;
        mPaint.setXfermode(new PorterDuffXfermode(mode));
    }

    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    public int getAlpha() {
        return mPaint.getAlpha();
    }
}

