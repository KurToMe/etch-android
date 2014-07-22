package kurtome.etch.app.activity;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import kurtome.etch.app.ObjectGraphUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.drawing.DrawingFragment;
import kurtome.etch.app.location.LocationProducer;
import kurtome.etch.app.location.LocationUpdatedEvent;
import kurtome.etch.app.openstreetmap.MapFragment;
import kurtome.etch.app.openstreetmap.MapLocationSelectedEvent;

import javax.inject.Inject;


public class MainActivity extends Activity {

    private static final Logger logger = LoggerManager.getLogger();

    private LocationProducer mLocationProducer;
    private Location mLocation;

    @Inject Bus mEventBus;

    private static final String DRAWING_ADDED_BACKSTACK = "drawing-added";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ObjectGraphUtils.inject(this);
        mEventBus.register(this);

        mLocationProducer = new LocationProducer(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new MapFragment())
                    .commit();
            goToDrawingFragment();
        }

        mLocationProducer.refreshLocation();
    }

    private void goToDrawingFragment() {
        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.enter_slide_up, R.animator.exit_slide_down, R.animator.enter_slide_up, R.animator.exit_slide_down)
                .add(R.id.container, new DrawingFragment())
                .addToBackStack(DRAWING_ADDED_BACKSTACK)
                .commit();
    }

    @Subscribe
    public void deviceLocationChanged(LocationUpdatedEvent event) {
        if (mLocation == null) {
            // first location acquired to go map
            getFragmentManager().popBackStack(DRAWING_ADDED_BACKSTACK, 0);
        }
        mLocation = event.getLocation();

    }

    @Subscribe
    public void mapLocationSelected(MapLocationSelectedEvent event) {
        goToDrawingFragment();
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automaticaylly handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }


}
