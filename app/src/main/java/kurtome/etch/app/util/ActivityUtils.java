package kurtome.etch.app.util;

import android.app.Activity;
import android.app.Fragment;
import android.view.View;

public class ActivityUtils {

    private ActivityUtils() {
        // don't instantiate me bro
    }

    public static <T extends Fragment> T fragmentById(Activity activity, int id) {
        return (T) activity.getFragmentManager().findFragmentById(id);
    }

}
