package kurtome.etch.app.drawing;

import android.graphics.Color;
import android.graphics.Paint;

public class DrawingBrush {

    public static final int MAX_ALPHA = 255;

    private Paint paint = new Paint(Paint.DITHER_FLAG);

    private int color = Color.DKGRAY;
    private int alpha = 200;
    private int strokeWidth = 10;

    public DrawingBrush() {
        updatePaint();
    }

    private void updatePaint() {
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(alpha);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public Paint getPaint() {
        return paint;
    }

    public int getAlpha() {
        return Math.round(((float) alpha / MAX_ALPHA) * 100);
    }

    public void setAlpha(int newAlpha) {
        this.paint = new Paint(paint);
        alpha = Math.round(((float) newAlpha / 100) * MAX_ALPHA);
        paint.setAlpha(alpha);
    }

    public void setColor(int color) {
        this.paint = new Paint(paint);
        this.color = color;
        paint.setColor(color);
        paint.setAlpha(alpha);
    }
}
