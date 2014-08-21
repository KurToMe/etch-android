package kurtome.etch.app.util;

public class Minutes {

    private Minutes() {
        // noooooope
    }

    public static long toMillis(long minutes) {
        return minutes * 60 * 1000;
    }

}
