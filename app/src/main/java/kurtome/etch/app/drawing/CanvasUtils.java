package kurtome.etch.app.drawing;

import android.graphics.*;
import com.google.common.base.Optional;
import kurtome.etch.app.GzipUtils;
import kurtome.etch.app.util.RectangleDimensions;

public class CanvasUtils {

    public static void clearCanvas(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
    }

    public static void drawBitmapFromGzip(Canvas canvas, byte[] gzipImage) {
        Optional<Integer> scaleSize = Optional.absent();
        drawBitmapFromGzip(canvas, gzipImage, scaleSize);
    }

    public static void drawBitmapFromGzip(Canvas canvas, byte[] gzipImage, Optional<Integer> scaleSize) {
        if (gzipImage.length <= 0) {
            return;
        }

        Optional<byte[]> bytes = GzipUtils.unzip(gzipImage);
        if (bytes.isPresent()) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes.get(), 0, bytes.get().length);
            drawBitmap(canvas, bitmap, scaleSize);
        }
    }

    /**
     * @param desiredHeight Scales the incoing bitmap so it's height matches incoming height.
     *                  Aspect ratio of the original bitmap is maintained
     */
    public static void drawBitmap(Canvas canvas, Bitmap bitmap, Optional<Integer> desiredHeight) {
        if (desiredHeight.isPresent()) {
            int finalHeight = desiredHeight.get();
            int finalWidth = RectangleUtils.calcWidthMaintainingRatio(
                    new RectangleDimensions(bitmap.getWidth(), bitmap.getHeight()),
                    finalHeight
            );
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, false);
            drawBitmap(canvas, scaledBitmap);
        }
        else {
            drawBitmap(canvas, bitmap);
        }
    }

    public static void drawBitmap(Canvas canvas, Bitmap bitmap) {
        canvas.drawBitmap(bitmap, 0, 0, DrawingBrush.BASIC_PAINT);
    }

}
