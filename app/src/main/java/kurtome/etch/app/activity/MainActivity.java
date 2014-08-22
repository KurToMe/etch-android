package kurtome.etch.app.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import kurtome.etch.app.ObjectGraphUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.drawing.DrawingFragment;
import kurtome.etch.app.location.event.LocationFoundEvent;
import kurtome.etch.app.location.LocationProducer;
import kurtome.etch.app.openstreetmap.MapFragment;
import kurtome.etch.app.openstreetmap.MapLocationSelectedEvent;

import javax.inject.Inject;


public class MainActivity extends Activity {

    private static final Logger logger = LoggerManager.getLogger();

    private LocationProducer mLocationProducer;

    @Inject Bus mEventBus;

    private static final String DRAWING_ADDED_BACKSTACK = "drawing-added";
    private static final String DRAWING_FRAGMENT_TAG = "drawing-fragment-tag";

    private final MapFragment mMapFragment = new MapFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ObjectGraphUtils.inject(this);
        mEventBus.register(this);

        mLocationProducer = new LocationProducer(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, mMapFragment)
                    .commit();
            //goToDrawingFragment();
        }

        mLocationProducer.refreshLocation();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister everything so there are no memory leaks
        //  and to ensure only one of everything is registered
        mEventBus.unregister(this);
        mLocationProducer.onDestroy();
    }

    private boolean isFragmentVisible(String tag) {
        Fragment fragment = getFragmentManager().findFragmentByTag(tag);
        if (fragment != null) {
            return fragment.isVisible();
        }
        else {
            return false;
        }
    }

    private void goToDrawingFragment() {
        if (isFragmentVisible(DRAWING_FRAGMENT_TAG)) {
            return;
        }

        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.enter_slide_up, R.animator.fade_out, R.animator.fade_in, R.animator.exit_slide_down)
                .add(R.id.container, new DrawingFragment(), DRAWING_FRAGMENT_TAG)
                .hide(mMapFragment)
                .addToBackStack(DRAWING_ADDED_BACKSTACK)
                .commit()

        ;
    }

    @Subscribe
    public void mapLocationSelected(MapLocationSelectedEvent event) {
        goToDrawingFragment();
    }

    public void popToMap() {
        getFragmentManager().popBackStack(DRAWING_ADDED_BACKSTACK, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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
