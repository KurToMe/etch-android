package kurtome.etch.app.gsm;

import android.app.Activity;
import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.view.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.common.base.Optional;
import com.octo.android.robospice.SpiceManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import kurtome.etch.app.ObjectGraphUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.coordinates.CoordinateUtils;
import kurtome.etch.app.util.NumberUtils;
import kurtome.etch.app.util.ObjUtils;
import kurtome.etch.app.util.ViewUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class GoogleMapFragment extends Fragment {

    private static final Logger logger = LoggerFactory.getLogger(GoogleMapFragment.class);

    private Activity mMainActivity;
    private GoogleMap mGoogleMap;
    private MapView mGoogleMapView;
    private EtchOverlayManager mEtchOverlayManager;
    private GroundOverlay mEtchGroundOverlay;
    private Location mMostRecentLocaction;
    private boolean mAnimatingCamera;

    private View mView;
    private Location mLocation;
    private MapLocationSelectedEvent mLastSelectedEvent;

    @Inject Bus mEventBus;
    @Inject SpiceManager spiceManager;

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
        mGoogleMapView = ViewUtils.subViewById(mView, R.id.google_map_view);
        mGoogleMapView.onCreate(savedInstanceState);
        mGoogleMap = mGoogleMapView.getMap();

        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mapClick(latLng);
            }
        });

        mGoogleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                attemptAddOverlaysToMapBasedOnLocation();
            }
        });

        mGoogleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                mMostRecentLocaction = location;
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

        mEventBus.register(this);

        return mView;
    }

    private void mapClick(LatLng latLng) {
        if (mEtchOverlayManager == null) {
            return;
        }

        Optional<EtchOverlayImage> etch = mEtchOverlayManager.getEtchAt(latLng);
        if (etch.isPresent()) {
            goToSelectedEtch(etch.get());
        }
    }

    private void goToSelectedEtch(EtchOverlayImage etchItem) {
        mLastSelectedEvent = new MapLocationSelectedEvent();
        mLastSelectedEvent.setEtchOverlayImage(etchItem);
        mEventBus.post(mLastSelectedEvent);
    }

    private void useLocation(Location location) {
        if (mLocation != null) {
            return;
        }

        mLocation = location;
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
        mLocation = null;

        mEtchOverlayManager.clearEtches();

        if (mEtchGroundOverlay != null) {
            mEtchGroundOverlay.remove();
            mEtchGroundOverlay = null;
        }

        mLocation = mMostRecentLocaction;
        centerOnLocationForEtches();

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

    /**
     * Might only work with odd numbers right now, due to how we calculate offsets etc.
     */
    private static final int ETCH_GRID_SIZE = 3;

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

        placeEtchOverlays();
    }

    private void placeEtchOverlays() {
        double latitude = mLocation.getLatitude();
        double longitude = mLocation.getLongitude();

        LatLng exactCenter = new LatLng(latitude, longitude);
        LatLng point = CoordinateUtils.roundToMinIncrementTowardNorthWest(exactCenter);

        int initialOffset = (-ETCH_GRID_SIZE / 2);
        int maxOffset = -initialOffset;

        for (int longOffset = initialOffset; longOffset <= maxOffset; longOffset++) {
            for (int latOffset = initialOffset; latOffset <= maxOffset; latOffset++) {
                // We want to start in upper-left/north-west corner, so each increment
                // should be in terms of amount east and south
                LatLng eastOffset = CoordinateUtils.incrementEast(point, longOffset);
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
        mEtchOverlayManager.addEtch(etchBounds);
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
    public MapLocationSelectedEvent produce() {
        return mLastSelectedEvent;
    }

}
