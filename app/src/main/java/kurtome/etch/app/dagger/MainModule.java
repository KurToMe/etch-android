package kurtome.etch.app.dagger;

import com.octo.android.robospice.JacksonGoogleHttpClientSpiceService;
import com.octo.android.robospice.SpiceManager;
import com.squareup.otto.Bus;
import dagger.Module;
import dagger.Provides;
import kurtome.etch.app.activity.MainActivity;
import kurtome.etch.app.drawing.DrawingFragment;
import kurtome.etch.app.location.LocationProducer;
import kurtome.etch.app.openstreetmap.MapFragment;

import javax.inject.Singleton;

@Module(
        injects = {
                MainActivity.class,
                DrawingFragment.class,
                MapFragment.class,
                LocationProducer.class
        }
)
public class MainModule {

//    private MainActivity mainActivity;
//
//    public MainModule(MainActivity mainActivity) {
//        this.mainActivity = mainActivity;
//    }
//
//    @Provides
//    public MainActivity provideMainActivity() {
//        return mainActivity;
//    }


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
