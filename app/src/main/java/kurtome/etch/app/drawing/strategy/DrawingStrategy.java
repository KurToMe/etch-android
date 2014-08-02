package kurtome.etch.app.drawing.strategy;

import android.graphics.Canvas;
import android.view.MotionEvent;
import kurtome.etch.app.drawing.DrawingBrush;

public interface DrawingStrategy {

    void onDraw(Canvas canvas);

    boolean onTouchEvent(MotionEvent event);

    DrawingBrush getBrush();

}
