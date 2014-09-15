package kurtome.etch.app.gsm;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.common.base.Optional;
import com.octo.android.robospice.SpiceManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import kurtome.etch.app.ObjectGraphUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.coordinates.CoordinateUtils;
import kurtome.etch.app.drawing.RectangleUtils;
import kurtome.etch.app.util.NumberUtils;
import kurtome.etch.app.util.ObjUtils;
import kurtome.etch.app.util.RectangleDimensions;
import kurtome.etch.app.util.ViewUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class GoogleMapFragment extends Fragment {

    private static final Logger logger = LoggerFactory.getLogger(GoogleMapFragment.class);

//    public final ItemizedIconOverlay.OnItemGestureListener<OverlayItem> ON_ITEM_GESTURE_LISTENER =
//            new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
//                @Override
//                public boolean onItemSingleTapUp(int index, OverlayItem item) {
//                    if (item instanceof EtchOverlayItem) {
//                        goToSelectedEtch((EtchOverlayItem) item);
//                        return true;
//                    }
//                    return false;
//                }
//
//                private void goToSelectedEtch(EtchOverlayItem etchItem) {
//                    mLastSelectedEvent = new MapLocationSelectedEvent();
//                    mLastSelectedEvent.setEtchOverlayItem(etchItem);
//                    mEventBus.post(mLastSelectedEvent);
//                }
//
//                @Override
//                public boolean onItemLongPress(int index, OverlayItem item) {
//                    if (item instanceof EtchOverlayItem) {
//                        goToSelectedEtch((EtchOverlayItem) item);
//                        return true;
//                    }
//                    return false;
//                }
//            };

//    private ItemizedIconOverlay<OverlayItem> mCenterOverlay;
//    private ItemizedIconOverlay<OverlayItem> mEtchGridOverlay;
    private Activity mMainActivity;
    private MapFragment mGoogleMapFragment;
    private GoogleMap mGoogleMap;
    private MapView mGoogleMapView;
//    private List<EtchOverlayImage> mEtchOverlays;
    private EtchOverlayManager mEtchOverlayManager;
    private GroundOverlay mEtchGroundOverlay;
    private Location mMostRecentLocaction;
    private boolean mAnimatingCamera;

    public void onOverlayInvalidated() {
        mGoogleMapView.invalidate();
    }

    /**
     * http://www.maps.stamen.com
     */
    private static interface StamenMapTileNames {
        String TONER_LITE = "toner-lite";
        String WATERCOLOR = "watercolor";
    }

    private View mView;
//    private IMapController mMapController;
//    private ResourceProxy mResourceProxy;
    private Location mLocation;
    private MapLocationSelectedEvent mLastSelectedEvent;
    private RelativeLayout mLoadingLayout;
    private ProgressBar mLoadingProgress;
    private ImageView mLoadingAlertImage;
    private boolean mAccurateLocationFound;

    @Inject Bus mEventBus;
    @Inject SpiceManager spiceManager;

    public static GoogleMapFragment newInstance() {
        GoogleMapFragment fragment = new GoogleMapFragment();
        return fragment;
    }

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
        mEtchOverlayManager = new EtchOverlayManager(this, ETCH_GRID_SIZE);

        MapsInitializer.initialize(this.getActivity());


//        mMapView = ViewUtils.subViewById(mView, R.id.etch_map_view);
//        mMapController = mMapView.getController();
//
//        mLoadingLayout = ViewUtils.subViewById(mView, R.id.map_loader_overlay);
//        mLoadingProgress = ViewUtils.subViewById(mView, R.id.map_loader_progress);
//        mLoadingAlertImage = ViewUtils.subViewById(mView, R.id.map_loader_alert_img);
//        mLoadingAlertImage.setVisibility(View.INVISIBLE);

//
//        final ITileSource tileSource = createOsmFr();
//        mMapView.setTileSource(tileSource);
//
//        goToScene(MapScene.NORTH_AMERICA);

        // Call this method to turn off hardware acceleration at the View level.
        // setHardwareAccelerationOff();

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

        mAccurateLocationFound = true;
        mLocation = location;
        centerOnLocationForEtches();
//        mLoadingLayout.setVisibility(View.INVISIBLE);
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

//        mGoogleMapFragment = ActivityUtils.fragmentById(mMainActivity, R.id.google_map_fragment);
//        mGoogleMap = mGoogleMapFragment.getMap();
    }

//    public void goToScene(MapScene scene) {
//        mMapController.setCenter(scene.getCenter());
//        mMapController.setZoom(scene.getZoomLevel());
//    }


    private void refreshMap() {
//        mLoadingLayout.setVisibility(View.VISIBLE);
//        mLoadingProgress.setVisibility(View.VISIBLE);
//        mLoadingAlertImage.setVisibility(View.INVISIBLE);

        mLocation = null;

        mEtchOverlayManager.clearEtches();

        if (mEtchGroundOverlay != null) {
            mEtchGroundOverlay.remove();
            mEtchGroundOverlay = null;
        }

        mLocation = mMostRecentLocaction;
        centerOnLocationForEtches();
//        if (mEtchOverlays != null) {
//            for (EtchOverlayImage etch : mEtchOverlays) {
//                etch.remove();
//            }
//        }
//        mEtchOverlays = null;

//        mEventBus.post(new RefreshLocationRequest());


//        mGoogleMapView.invalidate();
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

//    private ITileSource createOsmFr() {
//        return new XYTileSource(
//                "openstreetmaps-fr",
//                ResourceProxy.string.unknown,
//                1,
//                19,
//                256,
//                ".png",
//                new String[]{
//                        "http://a.tile.openstreetmap.fr/osmfr/",
//                        "http://b.tile.openstreetmap.fr/osmfr/",
//                        "http://c.tile.openstreetmap.fr/osmfr/"
//                }
//        );
//    }

//    private ITileSource createStamenMapTileSource(String tileName) {
//        return new XYTileSource(
//                "maps-stamen-" + tileName,
//                ResourceProxy.string.unknown,
//                1,
//                18,
//                256,
//                ".png",
//                new String[]{
//                        "http://a.tile.stamen.com/" + tileName + "/",
//                        "http://b.tile.stamen.com/" + tileName + "/",
//                        "http://c.tile.stamen.com/" + tileName + "/",
//                        "http://d.tile.stamen.com/" + tileName + "/"
//                }
//        );
//    }

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

//    @Subscribe
//    public void updateLocation(final LocationFoundEvent event) {
//        if (event.getLocation().isPresent()) {
//            mAccurateLocationFound = true;
//            mLocation = event.getLocation().get();
//            centerOnLocationForEtches();
//            attemptAddOverlaysToMapBasedOnLocation();
//        }
//        else if (event.getRoughLocation().isPresent() && !mAccurateLocationFound) {
//            Location location = event.getRoughLocation().get();
//            LatLng center = CoordinateUtils.toLatLng(location);
//            int zoomLevel = 14;
//            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(center, zoomLevel);
//            mGoogleMap.animateCamera(update);
////            GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
////            mMapController.setCenter(point);
////            mMapController.setZoom(14);
//        }
//
//        if (event.isFinal()) {
//            if (mLocation == null) {
//                handleLocationLookupFailure();
//            }
//            else {
//                mLoadingLayout.setVisibility(View.INVISIBLE);
//            }
//        }
//    }

    private void handleLocationLookupFailure() {
        mLoadingProgress.setVisibility(View.INVISIBLE);
        mLoadingAlertImage.setVisibility(View.VISIBLE);
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

//        if (!isAspectRatioValid(calcEtchAspectRatio())) {
//            return;
//        }

        placeEtchOverlays();
    }

    private void placeEtchOverlays() {
        double latitude = mLocation.getLatitude();
        double longitude = mLocation.getLongitude();


        LatLng exactCenter = new LatLng(latitude, longitude);
        LatLng point = CoordinateUtils.roundToMinIncrementTowardNorthWest(exactCenter);
//
        int initialOffset = (-ETCH_GRID_SIZE / 2);
        int maxOffset = -initialOffset;

        LatLng west = CoordinateUtils.incrementEast(point, initialOffset);
        LatLng north = CoordinateUtils.incrementSouth(point, initialOffset);
        int etchBoundsOffset = maxOffset + 1;// plus one because bounds need to be outside the last etch
        LatLng northeast = CoordinateUtils.incrementEast(north, etchBoundsOffset);
        LatLng southwest = CoordinateUtils.incrementSouth(west, etchBoundsOffset);
        LatLngBounds bounds = new LatLngBounds(southwest, northeast);
//        LatLngBounds overlayBounds = mEtchOverlayManager.setBounds(bounds);

//        double etchLatitudeDegrees = CoordinateUtils.calculateLatitudeDegrees(bounds);
//        double etchLongitudeDegrees = CoordinateUtils.calculateLongitudeDegrees(bounds);
//        double overlayLatitudeDegrees = CoordinateUtils.calculateLatitudeDegrees(overlayBounds);
//        double overlayLongitudeDegrees = CoordinateUtils.calculateLongitudeDegrees(overlayBounds);
//        double widthRatio = etchLongitudeDegrees / overlayLongitudeDegrees;
//        double heightRatio = etchLatitudeDegrees / overlayLatitudeDegrees;
//        Bitmap bitmap = mEtchOverlayManager.getBitmap();
//        float xAnchor = (float) (bitmap.getWidth() * 0.5 * widthRatio);
//        float yAnchor = (float) (bitmap.getHeight() * 0.5 * heightRatio);
//        mEtchGroundOverlay = mGoogleMap.addGroundOverlay(new GroundOverlayOptions()
//                .image(BitmapDescriptorFactory.fromBitmap(bitmap))
//                .positionFromBounds(overlayBounds)
//        );
//        mEtchOverlayManager.setOnBitmapUpdatedListener(new OnBitmapUpdatedListener() {
//            @Override
//            public void onBitmapUpdated(Bitmap bitmap) {
//                mEtchGroundOverlay.setImage(BitmapDescriptorFactory.fromBitmap(bitmap));
//                syncLoadingState();
//            }
//        });

        for (int longOffset = initialOffset; longOffset <= maxOffset; longOffset++) {
            for (int latOffset = initialOffset; latOffset <= maxOffset; latOffset++) {
                // We want to start in upper-left/north-west corner, so each increment
                // should be in terms of amount east and south
                LatLng eastOffset = CoordinateUtils.incrementEast(point, longOffset);
                LatLng finalOffset = CoordinateUtils.incrementSouth(eastOffset, latOffset);

                addEtchGroundOverlay(finalOffset, latOffset + Math.abs(initialOffset), longOffset + Math.abs(initialOffset));
            }
        }
//        mEtchGridOverlay = new ItemizedIconOverlay<OverlayItem>(this.getActivity(), items, ON_ITEM_GESTURE_LISTENER);
//        mMapView.getOverlays().add(mEtchGridOverlay);

        mGoogleMapView.invalidate();
    }

    private Point etchGridUpperLeft() {
        final int etchSize = mGoogleMapView.getWidth() / ETCH_GRID_SIZE;
        final int etchGridHeight = etchSize * ETCH_GRID_SIZE;
        final int extraYPixels = mGoogleMapView.getHeight() - etchGridHeight;
        return new Point(0, extraYPixels / 2);
    }

//    private GeoPoint pixelPointOnMap(Point point) {
//        Projection projection = mMapView.getProjection();
//        IGeoPoint iGeo = projection.fromPixels(point.x, point.y);
//        if (iGeo instanceof GeoPoint) {
//            return (GeoPoint) iGeo;
//        }
//        else {
//            return new GeoPoint(iGeo.getLatitudeE6(), iGeo.getLongitudeE6());
//        }
//    }

//    private GeoPoint coerce(IGeoPoint iGeo) {
//        if (iGeo instanceof GeoPoint) {
//            return (GeoPoint) iGeo;
//        }
//        else {
//            return new GeoPoint(iGeo.getLatitudeE6(), iGeo.getLongitudeE6());
//        }
//    }

//    private RectangleDimensions calcEtchSize() {
//        Projection projection = mGoogleMap.getProjection();
//        LatLng geo = projection.fromScreenLocation(new Point(0, 0));
//        LatLng eastGeo = CoordinateUtils.incrementEast(geo, 1);
//        LatLng southGeo = CoordinateUtils.incrementSouth(geo, 1);
//        Point eastPoint = new Point(0, 0);
//        projection.toPixels(eastGeo, eastPoint);
//        Point southPoint = new Point(0, 0);
//        projection.toPixels(southGeo, southPoint);
//        return new RectangleDimensions(Math.abs(eastPoint.x), Math.abs(southPoint.y));
//    }
//

    private double calcEtchAspectRatio() {
        Projection projection = mGoogleMap.getProjection();
        LatLng geo = projection.fromScreenLocation(new Point(0, 0));


        // NOTE: this assumes we're relatively zoomed in and the aspect ratio
        //          will be about the same anywhere on the map
        LatLng southGeo = projection.fromScreenLocation(new Point(0, 100));
        double degreesDiff = Math.abs(southGeo.latitude - geo.latitude);
//        LatLng eastGeo = new LatLng(geo.latitude, geo.longitude);
        LatLng eastGeo = CoordinateUtils.moveEast(geo, degreesDiff);
//        LatLng southGeo = CoordinateUtils.incrementSouth(geo, 1);
        Point eastPoint = projection.toScreenLocation(eastGeo);
        Point southPoint = projection.toScreenLocation(southGeo);
        logger.debug("Calculating aspect ratio from dimensions {}x{}", eastPoint.x, southPoint.y);
        double aspectRatio = RectangleUtils.calculateAspectRatio(eastPoint.x, southPoint.y);
        return aspectRatio;
    }

    private RectangleDimensions calcSize(LatLngBounds bounds) {
        Projection projection = mGoogleMap.getProjection();

        LatLng geo = CoordinateUtils.northWestCorner(bounds);
        LatLng southGeo = bounds.southwest;
        LatLng eastGeo = bounds.northeast;

        Point originPoint = projection.toScreenLocation(geo);
        Point eastPoint = projection.toScreenLocation(eastGeo);
        Point southPoint = projection.toScreenLocation(southGeo);
        int x = eastPoint.x - originPoint.x;
        int y = southPoint.y - originPoint.y;
//        logger.debug("Calculating aspect ratio from dimensions {}x{}", x, y);
//        double aspectRatio = RectangleUtils.calculateAspectRatio(x, y);
        return new RectangleDimensions(x, y);
    }

    private boolean isAspectRatioValid(double aspectRatio) {
        return aspectRatio < 5 && aspectRatio > 0.2;
    }

    private void addEtchGroundOverlay(LatLng etchPoint, int row, int col) {
        LatLng eastGeo = CoordinateUtils.incrementEast(etchPoint, 1);
        LatLng southGeo = CoordinateUtils.incrementSouth(etchPoint, 1);

        LatLngBounds etchBounds = new LatLngBounds(southGeo, eastGeo);
        final RectangleDimensions etchSize = calcSize(etchBounds);
//        if (!isAspectRatioValid(etchAspectRatio)) {
//            throw new IllegalStateException("Aspect ratio outside expected bounds: " + etchAspectRatio);
//        }


//        EtchOverlayImage etchItem = new EtchOverlayImage(this, etchBounds, etchSize);

//        final Coordinates coordinates = CoordinateUtils.convert(etchPoint);
        mEtchOverlayManager.addEtch(etchBounds, row);
//        return etchItem;
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
//        forceMaxZoom();
//        double latitude = mLocation.getLatitude();
//        double longitude = mLocation.getLongitude();
//        GeoPoint userCenterPoint = new GeoPoint(latitude, longitude);
//        mMapController.setCenter(userCenterPoint);
//
//        BoundingBoxE6 boundingBox = new BoundingBoxE6(latitude, longitude, latitude, longitude);

//        mMapView.setScrollableAreaLimit(boundingBox);
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


    private void forceMaxZoom() {
        // Not sure if varying zoom levels will mess up the aspect ratios of the etch images.
        //  (because we're using the map projection to calculate the width ratio)
        // For now we'll just assume setting max zoom we'll keep things simple

//        int zoomLevel = mMapView.getMaxZoomLevel();
        // http://wiki.openstreetmap.org/wiki/Zoom_levels
//        mMapController.setZoom(zoomLevel);
    }

    public GoogleMap getMap() {
        return mGoogleMap;
    }

    @Produce
    public MapLocationSelectedEvent produce() {
        return mLastSelectedEvent;
    }

}
