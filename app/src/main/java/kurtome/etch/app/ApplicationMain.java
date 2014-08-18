package kurtome.etch.app;

import android.app.Application;
import dagger.ObjectGraph;
import kurtome.etch.app.dagger.MainModule;

import javax.annotation.Nonnull;

public class ApplicationMain extends Application implements ObjectGraphUtils.ObjectGraphApplication {

    /**
     * Application's object graph for handling dependency injection
     */
    private ObjectGraph mObjectGraph;

    /* Application Lifecycle */
    @Override
    public void onCreate() {
        // Creates the dependency injection object graph
        mObjectGraph = ObjectGraph.create(new MainModule(this));
    }

    /* ObjectGraphApplication Contract */
    @Override
    public void inject(@Nonnull Object dependent) {
        mObjectGraph.inject(dependent);
    }


}
