package kurtome.etch.app.dagger;

import com.octo.android.robospice.JacksonGoogleHttpClientSpiceService;
import com.octo.android.robospice.SpiceManager;
import dagger.Module;
import dagger.Provides;
import kurtome.etch.app.activity.MainActivity;
import kurtome.etch.app.drawing.DrawingFragment;

import javax.inject.Singleton;

@Module(
        injects = {
                MainActivity.class,
                DrawingFragment.class
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

}
