package kurtome.etch.app.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.Window;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import kurtome.etch.app.ObjectGraphUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.drawing.DrawingFragment;
import kurtome.etch.app.gsm.GoogleMapFragment;
import kurtome.etch.app.gsm.MapLocationSelectedEvent;
import kurtome.etch.app.location.LocationProducer;

import javax.inject.Inject;


public class MainActivity extends Activity {

    private static final Logger logger = LoggerManager.getLogger();

    private LocationProducer mLocationProducer;

    @Inject Bus mEventBus;

    private static final String DRAWING_ADDED_BACKSTACK = "drawing-added";
    private static final String DRAWING_FRAGMENT_TAG = "drawing-fragment-tag";

//    private final MapFragment mMapFragment = new MapFragment();
    private final GoogleMapFragment mMapFragment = new GoogleMapFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        ObjectGraphUtils.inject(this);
        mEventBus.register(this);

        mLocationProducer = new LocationProducer(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, mMapFragment)
                    .commit();
        }

        mLocationProducer.refreshLocation();

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

    }

    @Override
    public boolean onNavigateUp() {
        if (isFragmentVisible(DRAWING_FRAGMENT_TAG)) {
            popToMap();
            return true;
        }

        return super.onNavigateUp();
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

    private void goToDrawingFragment(MapLocationSelectedEvent event) {
        if (isFragmentVisible(DRAWING_FRAGMENT_TAG)) {
            return;
        }

        DrawingFragment fragment = new DrawingFragment();
        fragment.setEtchData(event);

        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.enter_slide_up, R.animator.fade_out, R.animator.fade_in, R.animator.exit_slide_down)
                .add(R.id.container, fragment, DRAWING_FRAGMENT_TAG)
                .hide(mMapFragment)
                .addToBackStack(DRAWING_ADDED_BACKSTACK)
                .commit()
        ;
    }

    @Subscribe
    public void mapLocationSelected(MapLocationSelectedEvent event) {
        goToDrawingFragment(event);
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
