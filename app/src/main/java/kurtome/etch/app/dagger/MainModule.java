package kurtome.etch.app.dagger;

import android.content.Context;
import android.location.LocationManager;
import com.octo.android.robospice.JacksonGoogleHttpClientSpiceService;
import com.octo.android.robospice.SpiceManager;
import com.squareup.otto.Bus;
import dagger.Module;
import dagger.Provides;
import kurtome.etch.app.activity.MainActivity;
import kurtome.etch.app.colorpickerview.dialog.ColorPickerDialogFragment;
import kurtome.etch.app.drawing.DrawingFragment;
import kurtome.etch.app.location.LocationFetchManager;
import kurtome.etch.app.location.LocationFetchProcessor;
import kurtome.etch.app.location.LocationFetcher;
import kurtome.etch.app.location.LocationProducer;
import kurtome.etch.app.openstreetmap.MapFragment;
import kurtome.etch.app.util.ContextUtils;

import javax.inject.Singleton;

@Module(
        injects = {
                MainActivity.class,
                DrawingFragment.class,
                MapFragment.class,
                LocationProducer.class,
                LocationFetchManager.class,
                LocationFetcher.class,
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

    @Provides @Singleton
    public LocationManager provideLocationManager() {
        return ContextUtils.getSystemService(mContext, Context.LOCATION_SERVICE);
    }

    @Provides @Singleton
    public Context provideContext() {
        return mContext;
    }

    @Provides @Singleton
    public LocationFetchProcessor provideLocationFetchProcessor() {
        return new LocationFetchProcessor();
    }

}
