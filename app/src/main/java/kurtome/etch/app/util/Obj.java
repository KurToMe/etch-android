package kurtome.etch.app.util;

public class Obj {
    private Obj() {
        // none of these
    }

    public static <T> T cast(Object obj) {
        return (T) obj;
    }

}
