package kurtome.etch.app.gsm;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
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
    public static final int MIN_ZOOM_FOR_ETCHES = 16;
    public static final float DEFAULT_ZOOM = 18;

    public static final String LAST_LATITUDE = "last-latitude";
    public static final String LAST_LONGITUDE = "last-longitude";

    private Activity mMainActivity;
    private GoogleMap mGoogleMap;
    private MapView mGoogleMapView;
    private EtchOverlayManager mEtchOverlayManager;
    private boolean mAnimatingCamera;

    private View mView;

    private Location mLocation;
    private LatLngBounds mEditableBounds;

    private MapLocationSelectedEvent mLastSelectedEvent;
    private MapModeChangedEvent.Mode mCurrentMode = MapModeChangedEvent.Mode.MAP;

    @Inject Bus mEventBus;
    @Inject SpiceManager spiceManager;
    private int mTransitionMillis;
    private boolean mZoomOutToastShown;

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
        super.onLowMemory();
        mGoogleMapView.onLowMemory();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        ObjectGraphUtils.inject(this);

        mView = inflater.inflate(R.layout.google_map_layout, container, false);

        mTransitionMillis = this.getResources().getInteger(R.integer.mode_transition_millis);

        mGoogleMapView = ViewUtils.subViewById(mView, R.id.google_map_view);
        mGoogleMapView.onCreate(savedInstanceState);
        mGoogleMap = mGoogleMapView.getMap();

        mGoogleMap.getUiSettings().setCompassEnabled(true);
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);

        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {
                mapClick(latLng);
            }
        });

        mGoogleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                cameraChanged(cameraPosition);
            }
        });

        mGoogleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if (mLocation == null) {
                    useLocation(location);
                }
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
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(center, newZoom);

            mGoogleMap.animateCamera(update);
        }

        mEventBus.register(this);

        showToast("Click on etches nearby to start drawing.");

        return mView;
    }

    private void cameraChanged(CameraPosition cameraPosition) {
        removeEtchesFarFromLatLng(cameraPosition.target);
        if (!mEtchOverlayManager.hasEtches()) {
            attemptAddOverlaysToMapBasedOnLocation();
        }
        else {
//            if (cameraPosition.zoom < MIN_ZOOM_FOR_ETCHES) {
//                if (!mZoomOutToastShown) {
//                    mZoomOutToastShown = true;
//                    String text = "Cannot display etches when zoomed out too far.";
//                    showToast(text);
//                }
//                return;
//            }

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
        toast.setGravity(Gravity.BOTTOM, 0, 20);
        toast.show();
    }

    private void removeEtchesFarFromLatLng(LatLng latLng) {
        LatLngBounds bounds = CoordinateUtils.createBoundsEnclosingXIncrements(
                latLng,
                MAX_ETCH_GRID_SIZE
        );
        mEtchOverlayManager.removeEtchesOutsideOfBounds(bounds);
    }

    private void mapClick(LatLng latLng) {
        if (mEtchOverlayManager == null) {
            return;
        }

        LatLng etchLatLng = CoordinateUtils.roundToMinIncrementTowardNorthWest(latLng);
        Optional<EtchOverlayImage> etch = mEtchOverlayManager.getEtchAt(etchLatLng);
        if (etch.isPresent() && mEditableBounds.contains(etch.get().getEtchLatLng())) {
            goToSelectedEtch(etch.get());
        }
    }

    private void goToSelectedEtch(final EtchOverlayImage etchItem) {
        int paddingPx = 0;
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(
                etchItem.getEtchBounds(),
                DrawingView.IMAGE_HEIGHT_PIXELS,
                DrawingView.IMAGE_HEIGHT_PIXELS,
                paddingPx
        );

        mLastSelectedEvent = new MapLocationSelectedEvent();
        mLastSelectedEvent.setEtchOverlayImage(etchItem);
        mEventBus.post(mLastSelectedEvent);

        toggleMapControls(false);

        mGoogleMap.animateCamera(update, mTransitionMillis, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                goToDrawingMode(etchItem);
            }

            @Override
            public void onCancel() {
                goToDrawingMode(etchItem);
            }
        });

    }

    private void goToDrawingMode(EtchOverlayImage etchItem) {
        toggleMapControls(false);

        mCurrentMode = MapModeChangedEvent.Mode.DRAWING;
        mEventBus.post(new MapModeChangedEvent(mCurrentMode));
    }

    private void leaveDrawingMode() {
        mCurrentMode = MapModeChangedEvent.Mode.MAP;
        toggleMapControls(true);
    }

    private void toggleMapControls(boolean b) {
        setHasOptionsMenu(b);
        mGoogleMap.getUiSettings().setAllGesturesEnabled(b);
        mGoogleMap.getUiSettings().setCompassEnabled(b);
        mGoogleMap.getUiSettings().setIndoorLevelPickerEnabled(b);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(b);
        mGoogleMap.getUiSettings().setZoomControlsEnabled(b);
        mGoogleMap.setMyLocationEnabled(b);
    }

    private void useLocation(Location location) {
        if (mLocation != null) {
            return;
        }

        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        prefs.edit()
                .putFloat(LAST_LATITUDE, (float) location.getLatitude())
                .putFloat(LAST_LONGITUDE, (float) location.getLongitude())
                .apply();

        mLocation = location;

        LatLng latLng = CoordinateUtils.toLatLng(location);

        mEditableBounds = CoordinateUtils.createBoundsEnclosingXIncrements(latLng, 1);

        centerOnLocationForEtches();
        syncLoadingState();
    }

    private void syncLoadingState() {
        boolean loading = isLoading();
        mMainActivity.setProgressBarIndeterminateVisibility(loading);
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
        mMainActivity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_VISIBLE
        );
        mMainActivity.getActionBar().setDisplayHomeAsUpEnabled(false);
        mMainActivity.setProgressBarIndeterminateVisibility(true);

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
        toggleMapControls(true);
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.map, menu);
    }


    @Override
    public void onDestroy() {
        mGoogleMapView.onDestroy();
        mEventBus.unregister(this);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh_map) {
            refreshMap();
        }

        return super.onOptionsItemSelected(item);
    }

    private static final int MAX_ETCH_GRID_SIZE = 7;

    /**
     * Might only work with odd numbers right now, due to how we calculate offsets etc.
     */
    private static final int MIN_ETCH_GRID_SIZE = 3;

    private void attemptAddOverlaysToMapBasedOnLocation() {
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
        boolean editable = mEditableBounds.contains(etchPoint);
        mEtchOverlayManager.addEtch(etchBounds, editable);
    }

    private void centerOnLocationForEtches() {
        LatLng center = CoordinateUtils.toLatLng(mLocation);
        float zoomLevel = 17.75f;
        if (isNearPositionAndZoom(center, zoomLevel)) {
            attemptAddOverlaysToMapBasedOnLocation();
        }
        else {
            mAnimatingCamera = true;
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(center, zoomLevel);
            mGoogleMap.animateCamera(update, new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    mAnimatingCamera = false;
                    attemptAddOverlaysToMapBasedOnLocation();
                }

                @Override
                public void onCancel() {
                    mAnimatingCamera = false;
                    attemptAddOverlaysToMapBasedOnLocation();
                }
            });
        }
    }

    private boolean isNearPositionAndZoom(LatLng latLng, float zoomLevel) {
        CameraPosition cameraPosition = mGoogleMap.getCameraPosition();
        if (!NumberUtils.isWithinEpsilon(zoomLevel, cameraPosition.zoom, 0.25)) {
            return false;
        }

        if (!NumberUtils.isWithinEpsilon(latLng.latitude, cameraPosition.target.latitude, CoordinateUtils.MIN_INCREMENT)) {
            return false;
        }

        if (!NumberUtils.isWithinEpsilon(latLng.longitude, cameraPosition.target.longitude, CoordinateUtils.MIN_INCREMENT)) {
            return false;
        }

        return true;
    }

    public GoogleMap getMap() {
        return mGoogleMap;
    }

    @Produce
    public MapModeChangedEvent produceMapModeChanged() {
        return new MapModeChangedEvent(mCurrentMode);
    }

    @Produce
    public MapLocationSelectedEvent produceMapLocationSelected() {
        return mLastSelectedEvent;
    }

    @Subscribe
    public void handleDrawingComplete(DoneDrawingCommand cmd) {
        float newZoom = DEFAULT_ZOOM;
        LatLng center = mGoogleMap.getCameraPosition().target;
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(center, newZoom);

        mEtchOverlayManager.showAllEtches();

        mGoogleMap.animateCamera(update, mTransitionMillis, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                leaveDrawingMode();
            }

            @Override
            public void onCancel() {
                leaveDrawingMode();
            }
        });
    }
}
