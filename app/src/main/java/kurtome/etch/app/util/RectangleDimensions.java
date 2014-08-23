package kurtome.etch.app.util;

public class RectangleDimensions {
    public final int width;
    public final int height;

    public RectangleDimensions(int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("Width " + width + " is not positive.");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Height " + height + " is not positive.");
        }

        this.width = width;
        this.height = height;
    }

}
