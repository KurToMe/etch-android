package kurtome.etch.app.drawing;

import kurtome.etch.app.util.RectangleDimensions;

public class RectangleUtils {

    private RectangleUtils() {
        // don't instantiate me bro
    }
    public static double calculateAspectRatio(RectangleDimensions dimensions) {
        return calculateAspectRatio(
                dimensions.width,
                dimensions.height
        );
    }

    public static int calcWidthWithAspectRatio(int height, double aspectRatio) {
        int width = (int) Math.round(aspectRatio * height);
        return width;
    }

    public static double calculateAspectRatio(int width, int height) {
        double aspectRatio = Double.valueOf(width) / height;
        return aspectRatio;
    }

    public static int calcWidthMaintainingRatio(RectangleDimensions dimensions, int newHeight) {
        double aspectRatio = calculateAspectRatio(dimensions);
        return calcWidthWithAspectRatio(newHeight, aspectRatio);
    }

}
