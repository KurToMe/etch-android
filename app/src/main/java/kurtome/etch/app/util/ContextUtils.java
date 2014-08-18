package kurtome.etch.app.util;

import android.content.Context;

public class ContextUtils {

    private ContextUtils() {
        // don't instantiate me bro
    }

    public static <T> T getSystemService(Context context, String serviceName) {
        return (T) context.getSystemService(serviceName);
    }

}
