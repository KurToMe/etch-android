package kurtome.etch.app.gsm;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.*;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.internal.jx;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.common.base.Optional;
import com.octo.android.robospice.SpiceManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;
import kurtome.etch.app.ObjectGraphUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.activity.MainActivity;
import kurtome.etch.app.coordinates.CoordinateUtils;
import kurtome.etch.app.drawing.DoneDrawingCommand;
import kurtome.etch.app.drawing.DrawingView;
import kurtome.etch.app.util.NumberUtils;
import kurtome.etch.app.util.ObjUtils;
import kurtome.etch.app.util.ViewUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class GoogleMapFragment extends Fragment {

    private static final Logger logger = LoggerFactory.getLogger(GoogleMapFragment.class);
    public static final int MIN_ZOOM_FOR_ETCHES = 15;
    public static final float DEFAULT_ZOOM = 18;

    public static final String LAST_LATITUDE = "last-latitude";
    public static final String LAST_LONGITUDE = "last-longitude";

    private ActionBarActivity mMainActivity;
    private GoogleMap mGoogleMap;
    private MapView mGoogleMapView;
    private EtchOverlayManager mEtchOverlayManager;
    private boolean mAnimatingCamera;

    private View mView;

    private Location mLocation;

    private MapModeChangedEvent.Mode mCurrentMode = MapModeChangedEvent.Mode.MAP;

    @Inject Bus mEventBus;
    @Inject SpiceManager spiceManager;
    private int mTransitionMillis;
    private boolean mZoomOutToastShown;
    private FloatingActionButton mDrawButton;
    private ProgressBar mProgressBar;
    private boolean mHasToastedNetworkDown;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        spiceManager.start(getActivity());
    }

    @Override
    public void onStop() {
        if (spiceManager.isStarted()) {
            spiceManager.shouldStop();
        }
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        mGoogleMapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        ObjectGraphUtils.inject(this);

        mView = inflater.inflate(R.layout.google_map_layout, container, false);

        mTransitionMillis = this.getResources().getInteger(R.integer.mode_transition_millis);

        mGoogleMapView = ViewUtils.subViewById(mView, R.id.google_map_view);
        mDrawButton = ViewUtils.subViewById(mView, R.id.draw_btn);
        mProgressBar = ViewUtils.subViewById(mView, R.id.map_loader_progress);
        mGoogleMapView.onCreate(savedInstanceState);
        mGoogleMap = mGoogleMapView.getMap();

        mGoogleMap.getUiSettings().setCompassEnabled(true);
        mGoogleMap.getUiSettings().setZoomControlsEnabled(false);

        mGoogleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                mGoogleMapView.post(new Runnable(){
                    public void run(){
                        removeEtchesFarFromLatLng(mGoogleMap.getCameraPosition().target);
                    }
                });
                cameraChanged();
            }
        });

        mGoogleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                useLocation(location);
            }
        });

        mGoogleMap.setMyLocationEnabled(true);
        mEtchOverlayManager = new EtchOverlayManager(this);
        mEtchOverlayManager.setLoadingChangedRunnable(new Runnable() {
            @Override
            public void run() {
                syncLoadingState();
            }
        });

        MapsInitializer.initialize(this.getActivity());


        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        if (prefs.contains(LAST_LATITUDE)) {
            float latitude = prefs.getFloat(LAST_LATITUDE, 0f);
            float longitude = prefs.getFloat(LAST_LONGITUDE, 0f);

            float newZoom = DEFAULT_ZOOM;
            LatLng center = new LatLng(latitude, longitude);
            CameraUpdate update = CameraUpdateFactory.newCameraPosition(new CameraPosition(
                    center, newZoom, 30, 0
            ));

            mAnimatingCamera = true;
            mGoogleMap.animateCamera(update, new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    cameraChanged();
                    mAnimatingCamera = false;
                }

                @Override
                public void onCancel() {

                    cameraChanged();
                    mAnimatingCamera = false;

                }
            });
        }

        mDrawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawButtonClicked();
            }
        });

        mEventBus.register(this);

        final Handler h = new Handler();


        return mView;
    }

    private void drawButtonClicked() {
        if (mLocation == null) {
            showToast("Where are you?\n(try turning on GPS and WiFi)");
            return;
        }

        if (mAnimatingCamera) {
            // ignore while animating.
            // This can help prevent double click craziness
            return;
        }

        LatLng phoneLocation = CoordinateUtils.toLatLng(mLocation);
        LatLng mapLocation = mGoogleMap.getCameraPosition().target;
        if (!isNearPosition(phoneLocation, mapLocation, 4)) {
            logger.info(
                    "{} is too far away from {}",
                    mapLocation,
                    mGoogleMap.getCameraPosition().target
            );
            showToast("Can't reach, maybe time for a walk?");
            return;
        }

        LatLng etchLatLng = CoordinateUtils.roundToMinIncrementTowardNorthWest(mapLocation);
        Optional<EtchOverlayImage> etch = mEtchOverlayManager.getEtchAt(etchLatLng);
        if (etch.isPresent()) {
            logger.error("No etch found at {}", etchLatLng);
            goToSelectedEtch(etch.get());
        }
    }

    private void cameraChanged() {
        CameraPosition cameraPosition = mGoogleMap.getCameraPosition();
        if (!mEtchOverlayManager.hasEtches()) {
            attemptAddOverlaysToMapBasedOnLocation();
        }
        else {
            if (cameraPosition.zoom < MIN_ZOOM_FOR_ETCHES) {
                if (!mZoomOutToastShown) {
                    mZoomOutToastShown = true;
                    String text = "Cannot display etches when zoomed out too far.";
                    showToast(text);
                }
                return;
            }

            if (mAnimatingCamera) {
                return;
            }
            placeEtchOverlaysNearLatLng(cameraPosition.target);
        }
    }

    private void showToast(String text) {
        Toast toast = Toast.makeText(
                getActivity(),
                text,
                Toast.LENGTH_LONG
        );
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void removeEtchesFarFromLatLng(LatLng latLng) {
        LatLngBounds bounds = CoordinateUtils.createBoundsEnclosingXIncrements(
                latLng,
                MAX_ETCH_GRID_SIZE
        );
       mEtchOverlayManager.removeEtchesOutsideOfBounds(bounds);
    }

    private void goToSelectedEtch(final EtchOverlayImage etchItem) {
        int paddingPx = 0;
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(
                etchItem.getEtchBounds(),
                DrawingView.IMAGE_HEIGHT_PIXELS,
                DrawingView.IMAGE_HEIGHT_PIXELS,
                paddingPx
        );

        MapLocationSelectedEvent mLastSelectedEvent = new MapLocationSelectedEvent();
        mLastSelectedEvent.setEtchOverlayImage(etchItem);
        mEventBus.post(mLastSelectedEvent);

        toggleMapControls(false);

        mAnimatingCamera = true;
        mDrawButton.setVisibility(View.INVISIBLE);
        mGoogleMap.animateCamera(update, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                mAnimatingCamera = false;
                goToDrawingMode(etchItem);
            }

            @Override
            public void onCancel() {
                mAnimatingCamera = false;
                goToDrawingMode(etchItem);
            }
        });
    }

    private void goToDrawingMode(EtchOverlayImage etchItem) {
        logger.info("Going to drawing mode.");
        toggleMapControls(false);
        mGoogleMap.setMyLocationEnabled(false);
        mDrawButton.setVisibility(View.INVISIBLE);

        mCurrentMode = MapModeChangedEvent.Mode.DRAWING;
        mEventBus.post(new MapModeChangedEvent(mCurrentMode));
    }

    private void leaveDrawingMode() {
        logger.info("Leaving drawing mode.");
        mCurrentMode = MapModeChangedEvent.Mode.MAP;
        toggleMapControls(true);
        mGoogleMap.setMyLocationEnabled(true);
        mDrawButton.setVisibility(View.VISIBLE);
    }

    private void toggleMapControls(boolean b) {
        setHasOptionsMenu(b);
        mGoogleMap.getUiSettings().setAllGesturesEnabled(b);
        mGoogleMap.getUiSettings().setCompassEnabled(b);
        mGoogleMap.getUiSettings().setIndoorLevelPickerEnabled(b);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(b);
//        mGoogleMap.getUiSettings().setZoomControlsEnabled(b);
    }

    private void useLocation(Location location) {
        boolean firstLocation = mLocation == null;

        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        prefs.edit()
                .putFloat(LAST_LATITUDE, (float) location.getLatitude())
                .putFloat(LAST_LONGITUDE, (float) location.getLongitude())
                .apply();

        mLocation = location;

        if (firstLocation) {
            centerOnLocationForEtches();
        }
    }

    private void syncLoadingState() {
        boolean loading = isLoading();
        if (loading) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
        else {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private boolean isLoading() {
        if (mLocation == null) {
            return true;
        }

        if (mEtchOverlayManager != null && mEtchOverlayManager.isLoading()) {
            return true;
        }

        return false;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mMainActivity = ObjUtils.cast(activity);
    }

    private void refreshMap() {
        mEtchOverlayManager.recreateExistingEtches();

        syncLoadingState();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mGoogleMapView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                attemptAddOverlaysToMapBasedOnLocation();
            }
        });

    }

    @Override
    public void onResume() {
        mGoogleMapView.onResume();
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.map, menu);
    }

    @Override
    public void onDestroy() {
        mEventBus.unregister(this);
        super.onDestroy();
    }

    public void onPause() {
        this.mGoogleMapView.onPause();
        super.onPause();
    }

    public void onDestroyView() {
        this.mGoogleMapView.onDestroy();
        super.onDestroyView();
    }

    public void onSaveInstanceState(Bundle outState) {
        if(outState != null) {
            outState.setClassLoader(MapFragment.class.getClassLoader());
        }

        super.onSaveInstanceState(outState);
        this.mGoogleMapView.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh_map) {
            refreshMap();
        }

        return super.onOptionsItemSelected(item);
    }

    private static final int MAX_ETCH_GRID_SIZE = 5;

    /**
     * Might only work with odd numbers right now, due to how we calculate offsets etc.
     */
    private static final int MIN_ETCH_GRID_SIZE = 3;

    private class AttemptAddOverlays implements Runnable {

        @Override
        public void run() {
            if (!isNetworkAvailable()) {
                if (!mHasToastedNetworkDown) {
                    mHasToastedNetworkDown = true;
                    showToast("No network connection, go find some signal.");
                }
            }

            if (mEtchOverlayManager.hasEtches()) {
                // already added
                return;
            }

            if (mLocation == null) {
                // don't know where we are
                return;
            }

            if (mView == null) {
                return;
            }

            if (mView.getWidth() <= 0) {
                // map isn't ready
                return;
            }

            if (mAnimatingCamera) {
                return;
            }

            if (mGoogleMap.getCameraPosition().zoom < MIN_ZOOM_FOR_ETCHES) {
                return;
            }

            LatLng latLng = CoordinateUtils.toLatLng(mLocation);
            placeEtchOverlaysNearLatLng(latLng);
        }
    }

    private void attemptAddOverlaysToMapBasedOnLocation() {
        this.mGoogleMapView.post(
                new AttemptAddOverlays()
        );
    }

    private void placeEtchOverlaysNearLatLng(LatLng latLng) {
        if (mAnimatingCamera) {
            throw new IllegalStateException("Can't place etches while animating.");
        }

        LatLng etchLatLng = CoordinateUtils.roundToMinIncrementTowardNorthWest(latLng);

        LatLngBounds bounds = mGoogleMap.getProjection().getVisibleRegion().latLngBounds;
        double latDegrees = CoordinateUtils.calculateLatitudeDegrees(bounds);
        int increments = CoordinateUtils.calculateIncrements(latDegrees);
        if (increments % 2 == 0) {
            increments += 1;
        }
        if (increments > MAX_ETCH_GRID_SIZE) {
            increments = MAX_ETCH_GRID_SIZE;
        }
        if (increments < MIN_ETCH_GRID_SIZE) {
            increments = MIN_ETCH_GRID_SIZE;
        }

        logger.info("Using grid size of {}.", increments);

        int initialOffset = (-increments / 2);
        int maxOffset = -initialOffset;

        for (int longOffset = initialOffset; longOffset <= maxOffset; longOffset++) {
            for (int latOffset = initialOffset; latOffset <= maxOffset; latOffset++) {
                // We want to start in upper-left/north-west corner, so each increment
                // should be in terms of amount east and south
                LatLng eastOffset = CoordinateUtils.incrementEast(etchLatLng, longOffset);
                LatLng finalOffset = CoordinateUtils.incrementSouth(eastOffset, latOffset);

                addEtchGroundOverlay(finalOffset);
            }
        }

        mGoogleMapView.invalidate();
    }

    private void addEtchGroundOverlay(LatLng etchPoint) {
        LatLng eastGeo = CoordinateUtils.incrementEast(etchPoint, 1);
        LatLng southGeo = CoordinateUtils.incrementSouth(etchPoint, 1);

        LatLngBounds etchBounds = new LatLngBounds(southGeo, eastGeo);
//        boolean editable = mEditableBounds.contains(etchPoint);
        mEtchOverlayManager.addEtch(etchBounds, true);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isAvailable = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        if (isAvailable) {
            mHasToastedNetworkDown = false;
        }
        return isAvailable;
    }

    private void centerOnLocationForEtches() {
        centerOnLocationForEtches(null);
    }
    private void centerOnLocationForEtches(final Runnable callback) {
        LatLng center = CoordinateUtils.toLatLng(mLocation);
        float zoomLevel = 17.75f;
        if (isNearPositionAndZoom(center, zoomLevel)) {
            attemptAddOverlaysToMapBasedOnLocation();
        }
        else {
            mAnimatingCamera = true;
            CameraPosition pos = new CameraPosition(center, zoomLevel, .75f, 0);
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(center, zoomLevel);
            mGoogleMap.animateCamera(update, new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    mAnimatingCamera = false;
                    attemptAddOverlaysToMapBasedOnLocation();
                    if (callback != null) {
                        callback.run();
                    }
                }

                @Override
                public void onCancel() {
                    mAnimatingCamera = false;
                    attemptAddOverlaysToMapBasedOnLocation();
                    if (callback != null) {
                        callback.run();
                    }
                }
            });
        }
    }

    private boolean isNearPositionAndZoom(LatLng latLng, float zoomLevel) {
        CameraPosition cameraPosition = mGoogleMap.getCameraPosition();
        if (!NumberUtils.isWithinEpsilon(zoomLevel, cameraPosition.zoom, 0.25)) {
            return false;
        }

        if (!isNearPosition(latLng, cameraPosition.target, 2)) {
            return false;
        }

        return true;
    }

    private boolean isNearPosition(LatLng latLng,  LatLng latLng2, int incrementsAway) {
        if (!NumberUtils.isWithinEpsilon(latLng.latitude, latLng2.latitude, CoordinateUtils.MIN_INCREMENT * incrementsAway)) {
            return false;
        }

        if (!NumberUtils.isWithinEpsilon(latLng.longitude, latLng2.longitude, CoordinateUtils.MIN_INCREMENT * incrementsAway)) {
            return false;
        }

        return true;
    }

    public GoogleMap getMap() {
        return mGoogleMap;
    }

    public MapView getMapView() {
        return mGoogleMapView;
    }

    @Produce
    public MapModeChangedEvent produceMapModeChanged() {
        return new MapModeChangedEvent(mCurrentMode);
    }

    @Subscribe
    public void handleDrawingComplete(DoneDrawingCommand cmd) {
        float newZoom = DEFAULT_ZOOM;
        LatLng center = mGoogleMap.getCameraPosition().target;

        CameraPosition pos = new CameraPosition(center, newZoom, 30, 0);
        CameraUpdate update = CameraUpdateFactory.newCameraPosition(pos);

        mEtchOverlayManager.showAllEtches();

        mAnimatingCamera = true;
        mGoogleMap.animateCamera(update, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                mAnimatingCamera = false;
                leaveDrawingMode();
            }

            @Override
            public void onCancel() {
                mAnimatingCamera = false;
                leaveDrawingMode();
            }
        });
    }
}
