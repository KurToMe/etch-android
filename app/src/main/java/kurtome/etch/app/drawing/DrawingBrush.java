package kurtome.etch.app.drawing;

import android.graphics.*;

public class DrawingBrush {

    public static final int MAX_ALPHA = 255;

    public static Paint createBasicPaint() {
        Paint paint = new Paint(Paint.DITHER_FLAG);
        paint.setAntiAlias(true);
        return paint;
    }

    private Paint paint;

    public DrawingBrush() {
        paint = createBasicPaint();
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(150);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.DKGRAY);
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
