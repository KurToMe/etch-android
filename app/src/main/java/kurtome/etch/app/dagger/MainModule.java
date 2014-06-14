package kurtome.etch.app.dagger;

import com.octo.android.robospice.JacksonGoogleHttpClientSpiceService;
import com.octo.android.robospice.SpiceManager;
import dagger.Module;
import dagger.Provides;
import kurtome.etch.app.activity.MainActivity;

import javax.inject.Singleton;

@Module(
        injects = {
                MainActivity.class,
                MainActivity.PlaceholderFragment.class
        }
)
public class MainModule {

    private MainActivity mainActivity;

    public MainModule(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Provides @Singleton
    public MainActivity provideMainActivity() {
        return mainActivity;
    }

    @Provides @Singleton
    public SpiceManager provideSpiceManager() {
        return new SpiceManager(JacksonGoogleHttpClientSpiceService.class);
    }

}
