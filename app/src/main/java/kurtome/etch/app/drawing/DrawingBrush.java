package kurtome.etch.app.drawing;

import android.graphics.*;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Random;

public class DrawingBrush {

    public static final int MAX_ALPHA = 255;

    public static final Paint BASIC_PAINT = DrawingBrush.createBasicPaint();

    public static Paint createBasicPaint() {
        Paint paint = new Paint(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setAntiAlias(true);
        return paint;
    }

    private Paint paint;

    public DrawingBrush() {
        paint = createBasicPaint();
        paint.setStrokeWidth(3);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setColor(Color.DKGRAY);
//        paint.setAlpha(200);
        paint.setStyle(Paint.Style.STROKE);
        paint.setMaskFilter(new BlurMaskFilter(paint.getStrokeWidth() / 4, BlurMaskFilter.Blur.NORMAL));
    }

    public Paint getPaint() {
        return paint;
    }


    public int getColor() {
        return paint.getColor();
    }


    public void setColor(int color) {
        paint.setColor(color);
    }



}

