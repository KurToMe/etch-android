package kurtome.etch.app.dagger;

import android.content.Context;
import com.octo.android.robospice.JacksonGoogleHttpClientSpiceService;
import com.octo.android.robospice.SpiceManager;
import com.squareup.otto.Bus;
import dagger.Module;
import dagger.Provides;
import kurtome.etch.app.activity.MainActivity;
import kurtome.etch.app.colorpickerview.dialog.ColorPickerDialogFragment;
import kurtome.etch.app.drawing.DrawingFragment;
import kurtome.etch.app.gsm.GoogleMapFragment;

import javax.inject.Singleton;

@Module(
        injects = {
                MainActivity.class,
                DrawingFragment.class,
                GoogleMapFragment.class,
                ColorPickerDialogFragment.class
        }
)
public class MainModule {

    private final Context mContext;

    public MainModule(Context context) {
        mContext = context;
    }

    @Provides
    public SpiceManager provideSpiceManager() {
        return new SpiceManager(JacksonGoogleHttpClientSpiceService.class);
    }

    @Provides @Singleton
    public Bus provideEventBus() {
        // maybe should make one specific to posting location updates?
        return new Bus("main-etch-bus");
    }

}
