package kurtome.etch.app.util;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

public class ActivityUtils {

    private ActivityUtils() {
        // don't instantiate me bro
    }

    public static <T extends Fragment> T fragmentById(FragmentActivity activity, int id) {
        return (T) activity.getSupportFragmentManager().findFragmentById(id);
    }

}
