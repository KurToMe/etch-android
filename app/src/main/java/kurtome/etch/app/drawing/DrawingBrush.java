package kurtome.etch.app.drawing;

import android.graphics.*;

public class DrawingBrush {

    public static final int MAX_ALPHA = 255;

    private Paint paint = createDefaultPaint();

    public static Paint createBasicPaint() {
        Paint paint = new Paint(Paint.DITHER_FLAG);
        paint.setAntiAlias(true);
        return paint;
    }

    private static Paint createDefaultPaint() {
        Paint paint = createBasicPaint();
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.DKGRAY);
        return paint;
    }

    public DrawingBrush() {
        updatePaint();
    }

    private void updatePaint() {
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
