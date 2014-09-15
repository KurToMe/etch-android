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
        Optional<RectangleDimensions> desiredSize = Optional.absent();
        drawBitmapFromGzip(canvas, gzipImage, desiredSize);
    }

    public static void drawBitmapFromGzip(Canvas canvas, byte[] gzipImage, Optional<RectangleDimensions> desiredSize) {
        if (gzipImage.length <= 0) {
            return;
        }

        Optional<byte[]> bytes = GzipUtils.unzip(gzipImage);
        if (bytes.isPresent()) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes.get(), 0, bytes.get().length);
            drawBitmapScalingBasedOnHeightThenCropping(canvas, bitmap, desiredSize);
        }
    }

    /**
     * @param desiredSize Scales the incoming bitmap so it's height matches incoming height.
     *                  Aspect ratio of the original bitmap is maintained
     */
    public static void drawBitmapScalingBasedOnHeightThenCropping(Canvas canvas, Bitmap bitmap, Optional<RectangleDimensions> desiredSize) {
        if (desiredSize.isPresent()) {
            int finalHeight = desiredSize.get().height;
            int finalWidth = RectangleUtils.calcWidthMaintainingRatio(
                    new RectangleDimensions(bitmap.getWidth(), bitmap.getHeight()),
                    finalHeight
            );
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, false);
            if (desiredSize.isPresent()) {
               scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, desiredSize.get().width, desiredSize.get().height);
            }
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
