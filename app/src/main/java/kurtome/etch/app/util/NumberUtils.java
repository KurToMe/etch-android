package kurtome.etch.app.util;

public class NumberUtils {

    public static float absDiff(float x, float y) {
        return Math.abs(x - y);
    }

    public static boolean isWithinEpsilon(float x, float y, float epsilon) {
        float diff = absDiff(x, y);
        return diff < epsilon;
    }

    public static double absDiff(double x, double y) {
        return Math.abs(x - y);
    }

    public static boolean isWithinEpsilon(double x, double y, double epsilon) {
        double diff = absDiff(x, y);
        return diff < epsilon;
    }
}
