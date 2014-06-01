package kurtome.etch.app.drawing;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import kurtome.etch.app.domain.Etch;

public class DrawingView extends View {

    private Path drawPath;
    private Paint canvasPaint;
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;
    private DrawingBrush currentBrush;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        drawPath = new Path();
        currentBrush = new DrawingBrush();
        canvasPaint = new Paint(Paint.DITHER_FLAG);


    }

    //view assigned size
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    //draw view
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(drawPath, currentBrush.getPaint());
    }

    //respond to touch interaction
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        float touchX = event.getX();
        float touchY = event.getY();
        //respond to down, move and up events
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                drawPath.lineTo(touchX, touchY);
                drawCanvas.drawPath(drawPath, currentBrush.getPaint());
                drawPath.reset();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    public DrawingBrush getDrawingBrush()  {
        return currentBrush;
    }

    public void setCurrentEtch(Etch etch) {
        byte[] bytes = Base64.decodeBase64(etch.getBase64Image());
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        drawCanvas.drawBitmap(bitmap, 0, 0, new Paint());
    }
}