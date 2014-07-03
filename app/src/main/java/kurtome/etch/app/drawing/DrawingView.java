package kurtome.etch.app.drawing;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;

public class DrawingView extends View {

    private Path drawPath;
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;
    private DrawingBrush currentBrush;

    private float lastX, lastY;

    public static final int IMAGE_SIZE_PIXELS = 1000;

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
        canvasBitmap = Bitmap.createBitmap(IMAGE_SIZE_PIXELS, IMAGE_SIZE_PIXELS, Bitmap.Config.ARGB_8888);

        drawCanvas = new Canvas(canvasBitmap);
    }

    //draw view
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(canvasBitmap, 0, 0, DrawingBrush.BASIC_PAINT);
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
                quadPathTo(touchX, touchY);
               break;
            case MotionEvent.ACTION_UP:
//                drawPath.lineTo(touchX, touchY);
                quadPathTo(touchX, touchY);
                drawCanvas.drawPath(drawPath, currentBrush.getPaint());
                drawPath = new Path();
                break;
            default:
                return false;
        }
        lastX = touchX;
        lastY = touchY;
        invalidate();
        return true;
    }

    private void quadPathTo(float touchX, float touchY) {
        final float x2 = (touchX + lastX) / 2;
        final float y2 = (touchY + lastY) / 2;
        drawPath.quadTo(x2, y2, touchX, touchY);
    }

    public DrawingBrush getDrawingBrush()  {
        return currentBrush;
    }


    public void setCurrentImage(String base64Image) {
        clearCanvas();
        if (StringUtils.isNotBlank(base64Image)) {
            drawEtchToCanvas(base64Image);
        }
        invalidate();
    }

    private void drawEtchToCanvas(String base64Image) {
        byte[] bytes = Base64.decodeBase64(base64Image);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        drawCanvas.drawBitmap(bitmap, 0, 0, DrawingBrush.BASIC_PAINT);
    }

    private void clearCanvas() {
        drawCanvas.drawColor(Color.WHITE);
    }

    public String getCurrentImage() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        canvasBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] bytes = stream.toByteArray();
        String base64 = Base64.encodeBase64String(bytes);
        return base64;
    }
}