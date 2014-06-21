package kurtome.etch.app;

import android.app.Application;
import dagger.ObjectGraph;
import kurtome.etch.app.dagger.MainModule;

import javax.annotation.Nonnull;

public class ApplicationMain extends Application implements ObjectGraphUtils.ObjectGraphApplication {


    /* Application Lifecycle */
    @Override
    public void onCreate() {
        // Creates the dependency injection object graph
        _object_graph = ObjectGraph.create(new MainModule());
    }

    /* ObjectGraphApplication Contract */
    @Override
    public void inject(@Nonnull Object dependent) {
        _object_graph.inject(dependent);
    }

    /**
     * Application's object graph for handling dependency injection
     */
    private ObjectGraph _object_graph;

}
