package kurtome.etch.app.drawing.strategy;

import android.graphics.Canvas;
import android.view.MotionEvent;
import kurtome.etch.app.drawing.DrawingBrush;

public interface DrawingStrategy {

    void draw(Canvas canvas);

    boolean touchEvent(MotionEvent event);

    DrawingBrush getBrush();

    boolean isDrawing();

    void sizeChanged(int width, int height, int oldWidth, int oldHeight);

    void undoLastDraw();

    void flush();

    void forceStartDrawing();
}
