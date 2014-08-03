package kurtome.etch.app.util;

import android.view.View;

public class ViewUtils {

    public static <T extends View> T subViewById(View view, int id) {
        return (T) view.findViewById(id);
    }

}
