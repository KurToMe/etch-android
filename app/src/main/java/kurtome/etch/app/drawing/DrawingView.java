package kurtome.etch.app.drawing;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import kurtome.etch.app.domain.Etch;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;

public class DrawingView extends View {

    private Path drawPath;
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
        canvas.drawBitmap(canvasBitmap, 0, 0, currentBrush.getPaint());
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
                drawPath = new Path();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    public void setPaintColor(int color) {
        currentBrush.setColor(color);
    }


    public DrawingBrush getDrawingBrush()  {
        return currentBrush;
    }

    public void setCurrentImage(String base64Image) {
        if (StringUtils.isNotBlank(base64Image)) {
            drawEtchToCanvas(base64Image);
        }
    }

    private void drawEtchToCanvas(String base64Image) {
        byte[] bytes = Base64.decodeBase64(base64Image);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        drawCanvas.drawBitmap(bitmap, 0, 0, new Paint(Paint.DITHER_FLAG));
        invalidate();
    }

    public String getCurrentImage() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        canvasBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] bytes = stream.toByteArray();
        String base64 = Base64.encodeBase64String(bytes);
        return base64;
    }
}