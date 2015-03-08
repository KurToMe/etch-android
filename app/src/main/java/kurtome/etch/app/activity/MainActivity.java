package kurtome.etch.app.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Window;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import kurtome.etch.app.ObjectGraphUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.drawing.DoneDrawingCommand;
import kurtome.etch.app.drawing.DrawingFragment;
import kurtome.etch.app.gsm.GoogleMapFragment;
import kurtome.etch.app.gsm.MapLocationSelectedEvent;
import kurtome.etch.app.location.LocationProducer;

import javax.inject.Inject;


public class MainActivity extends ActionBarActivity {
    public static final String PREFS_NAME = "etch-prefs";

    private static final Logger logger = LoggerManager.getLogger();

    @Inject Bus mEventBus;

    private static final String DRAWING_ADDED_BACKSTACK = "drawing-added";
    private static final String DRAWING_FRAGMENT_TAG = "drawing-fragment-tag";

//    private final MapFragment mMapFragment = new MapFragment();
    private final GoogleMapFragment mMapFragment = new GoogleMapFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        ObjectGraphUtils.inject(this);
        mEventBus.register(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (getSupportFragmentManager().findFragmentByTag(DRAWING_FRAGMENT_TAG) == null) {
                    mEventBus.post(new DoneDrawingCommand());
                }
            }
        });


        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mMapFragment)
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setLogo(R.mipmap.ic_launcher);
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
    }

    private boolean isFragmentVisible(String tag) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
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

        getSupportFragmentManager().beginTransaction()
//                .setCustomAnimations(R.animator.fade_in, 0, R.animator.fade_in, 0)
                .add(R.id.container, fragment, DRAWING_FRAGMENT_TAG)
//                .hide(mMapFragment)
                .addToBackStack(DRAWING_ADDED_BACKSTACK)
                .commit()
        ;
    }

    @Subscribe
    public void mapLocationSelected(MapLocationSelectedEvent event) {
        goToDrawingFragment(event);
    }

    public void popToMap() {
        getSupportFragmentManager().popBackStack(
                DRAWING_ADDED_BACKSTACK,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
        );
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


    @Subscribe
    public void handleDrawingComplete(DoneDrawingCommand cmd) {
        this.popToMap();
    }

}
