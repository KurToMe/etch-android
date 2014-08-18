package kurtome.etch.app.openstreetmap;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.graphics.*;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import com.google.api.client.util.Lists;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
import com.octo.android.robospice.SpiceManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;
import kurtome.etch.app.ObjectGraphUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.coordinates.CoordinateUtils;
import kurtome.etch.app.domain.Coordinates;
import kurtome.etch.app.location.event.LocationFoundEvent;
import kurtome.etch.app.location.RefreshLocationRequest;
import kurtome.etch.app.openstreetmap.preset.MapScene;
import kurtome.etch.app.util.ViewUtils;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
//import org.osmdroid.views.overlay.ScaleBarOverlay;

import javax.inject.Inject;
import java.util.List;

public class MapFragment extends Fragment {

    private static final Logger logger = LoggerManager.getLogger();

    public final ItemizedIconOverlay.OnItemGestureListener<OverlayItem> ON_ITEM_GESTURE_LISTENER =
            new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                @Override
                public boolean onItemSingleTapUp(int index, OverlayItem item) {
                    if (item instanceof EtchOverlayItem) {
                        goToSelectedEtch((EtchOverlayItem) item);
                    }
                    return false;
                }

                private void goToSelectedEtch(EtchOverlayItem etchItem) {
                    mLastSelectedEvent = new MapLocationSelectedEvent();
                    mLastSelectedEvent.setEtchOverlayItem(etchItem);
                    mEventBus.post(mLastSelectedEvent);
                }

                @Override
                public boolean onItemLongPress(int index, OverlayItem item) {
                    return false;
                }
            };

    private ItemizedIconOverlay<OverlayItem> mCenterOverlay;
    private ItemizedIconOverlay<OverlayItem> mEtchGridOverlay;

    public void onOverlayInvalidated() {
        mMapView.invalidate();
    }

    /**
     * http://www.maps.stamen.com
     */
    private static interface StamenMapTileNames {
        String TONER_LITE = "toner-lite";
        String WATERCOLOR = "watercolor";
    }

    public static final String PREFS_NAME = "org.andnav.osm.prefs";

    private static final int DIALOG_ABOUT_ID = 1;

    private static final int MENU_SAMPLES = Menu.FIRST + 1;
    private static final int MENU_ABOUT = MENU_SAMPLES + 1;

    private static final int MENU_LAST_ID = MENU_ABOUT + 1; // Always set to last unused id

    private View mView;
    private MapView mMapView;
    private IMapController mMapController;
    private ResourceProxy mResourceProxy;
    private Location mLocation;
    private MapLocationSelectedEvent mLastSelectedEvent;
    private RelativeLayout mLoadingLayout;
    private ImageButton mRefreshButton;

    @Inject Bus mEventBus;
    @Inject SpiceManager spiceManager;

    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        ObjectGraphUtils.inject(this);
        mEventBus.register(this);

        mView = inflater.inflate(R.layout.map_layout, container, false);
        mMapView = ViewUtils.subViewById(mView, R.id.etch_map_view);
        mMapController = mMapView.getController();

        mLoadingLayout = ViewUtils.subViewById(mView, R.id.map_loader);

        mRefreshButton = ViewUtils.subViewById(mView, R.id.refresh_map_btn);
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshMap();
            }
        });

        final ITileSource tileSource = createOsmFr();
        mMapView.setTileSource(tileSource);

        goToScene(MapScene.NORTH_AMERICA);

        // Call this method to turn off hardware acceleration at the View level.
        // setHardwareAccelerationOff();

        return mView;
    }

    public void goToScene(MapScene scene) {
        mMapController.setCenter(scene.getCenter());
        mMapController.setZoom(scene.getZoomLevel());
    }


    private void refreshMap() {
        mLoadingLayout.setVisibility(View.VISIBLE);

        mLocation = null;

        mMapView.getOverlays().remove(mEtchGridOverlay);
        mEtchGridOverlay = null;

        mMapView.getOverlays().remove(mCenterOverlay);
        mCenterOverlay = null;

        mEventBus.post(new RefreshLocationRequest());
        mMapView.invalidate();
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setHardwareAccelerationOff() {
        // Turn off hardware acceleration here, or in manifest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            mMapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mMapView.setBuiltInZoomControls(false);
//        mMapView.setMultiTouchControls(false);


        setHasOptionsMenu(false);

        mMapView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                attemptAddOverlaysToMapBasedOnLocation();
            }
        });
    }

    private ITileSource createOsmFr() {
        return new XYTileSource(
                "openstreetmaps-fr",
                ResourceProxy.string.unknown,
                1,
                19,
                256,
                ".png",
                new String[]{
                        "http://a.tile.openstreetmap.fr/osmfr/",
                        "http://b.tile.openstreetmap.fr/osmfr/",
                        "http://c.tile.openstreetmap.fr/osmfr/"
                }
        );
    }

    private ITileSource createStamenMapTileSource(String tileName) {
        return new XYTileSource(
                "maps-stamen-" + tileName,
                ResourceProxy.string.unknown,
                1,
                18,
                256,
                ".png",
                new String[]{
                        "http://a.tile.stamen.com/" + tileName + "/",
                        "http://b.tile.stamen.com/" + tileName + "/",
                        "http://c.tile.stamen.com/" + tileName + "/",
                        "http://d.tile.stamen.com/" + tileName + "/"
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();

//        final ITileSource tileSource = createOsmFr();
//        mMapView.setTileSource(tileSource);
// //        forceMaxZoom(); // differs by tile source
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Put overlay items first
        mMapView.getOverlayManager().onCreateOptionsMenu(menu, MENU_LAST_ID, mMapView);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu pMenu) {
        mMapView.getOverlayManager().onPrepareOptionsMenu(pMenu, MENU_LAST_ID, mMapView);
        super.onPrepareOptionsMenu(pMenu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mEventBus.unregister(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mMapView.getOverlayManager().onOptionsItemSelected(item, MENU_LAST_ID, mMapView))
            return true;

        switch (item.getItemId()) {
            case MENU_ABOUT:
                getActivity().showDialog(DIALOG_ABOUT_ID);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    public void updateLocation(final LocationFoundEvent event) {
        if (event.getLocation().isPresent()) {
            mLocation = event.getLocation().get();
            attemptAddOverlaysToMapBasedOnLocation();
        }
        else if (event.getRoughLocation().isPresent()) {
            Location location = event.getRoughLocation().get();
            GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
            mMapController.setCenter(point);
            mMapController.setZoom(16);
        }

        if (event.isFinal()) {
            mLoadingLayout.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Might only work with odd numbers right now, due to how we calculate offsets etc.
     */
    private static final int ETCH_GRID_SIZE = 3;

    private void attemptAddOverlaysToMapBasedOnLocation() {
        if (mEtchGridOverlay != null) {
            // already added
            return;
        }

        if (mLocation == null) {
            // don't know where we are
            return;
        }

        if (mMapView == null) {
            return;
        }

        if (mMapView.getWidth() <= 0) {
            // map isn't ready
            return;
        }


        centerOnLocation();
        placeEtchOverlays();
    }

    private void placeEtchOverlays() {
        double latitude = mLocation.getLatitude();
        double longitude = mLocation.getLongitude();


        List<OverlayItem> items = Lists.newArrayList();
        GeoPoint exactCenter = new GeoPoint(latitude, longitude);
        GeoPoint point = CoordinateUtils.roundToMinIncrement(exactCenter);

        int initialOffset = (-ETCH_GRID_SIZE / 2);
        int maxOffset = -initialOffset;
        for (int longOffset = initialOffset; longOffset <= maxOffset; longOffset++) {
            for (int latOffset = initialOffset; latOffset <= maxOffset; latOffset++) {
                // We want to start in upper-left/north-west corner, so each increment
                // should be in terms of amount east and south
                GeoPoint eastOffset = CoordinateUtils.incrementEast(point, longOffset);
                GeoPoint finalOffset = CoordinateUtils.incrementSouth(eastOffset, latOffset);

                // Upper left (offset -1, -1) should be 0, 0 on the grid of etches
                int etchGridX = longOffset + -initialOffset;
                int etchGridY = latOffset + -initialOffset;

                EtchOverlayItem etchItem = getEtchOverlayItem(finalOffset, etchGridX, etchGridY);
                etchItem.setEtchCoordinates(CoordinateUtils.convert(finalOffset));
                items.add(etchItem);
            }
        }
        mEtchGridOverlay = new ItemizedIconOverlay<OverlayItem>(this.getActivity(), items, ON_ITEM_GESTURE_LISTENER);
        mMapView.getOverlays().add(mEtchGridOverlay);

    }

    private Point etchGridUpperLeft() {
        final int etchSize = mMapView.getWidth() / ETCH_GRID_SIZE;
        final int etchGridHeight = etchSize * ETCH_GRID_SIZE;
        final int extraYPixels = mMapView.getHeight() - etchGridHeight;
        return new Point(0, extraYPixels / 2);
    }

    private GeoPoint pixelPointOnMap(Point point) {
        Projection projection = mMapView.getProjection();
        IGeoPoint iGeo = projection.fromPixels(point.x, point.y);
        if (iGeo instanceof GeoPoint) {
            return (GeoPoint) iGeo;
        }
        else {
            return new GeoPoint(iGeo.getLatitudeE6(), iGeo.getLongitudeE6());
        }
    }

    private GeoPoint coerce(IGeoPoint iGeo) {
        if (iGeo instanceof GeoPoint) {
            return (GeoPoint) iGeo;
        }
        else {
            return new GeoPoint(iGeo.getLatitudeE6(), iGeo.getLongitudeE6());
        }
    }

    private int calcEtchSize() {
        Projection projection = mMapView.getProjection();
        GeoPoint geo = coerce(projection.fromPixels(0, 0));
        GeoPoint eastGeo = CoordinateUtils.incrementEast(geo, 1);
        Point pointToUse = new Point(0, 0);
        projection.toPixels(eastGeo, pointToUse);
        return pointToUse.x;
//        return mMapView.getWidth() / ETCH_GRID_SIZE;
    }

    private EtchOverlayItem getEtchOverlayItem(GeoPoint etchPoint, int etchGridX, int etchGridY) {
        final int etchSize = calcEtchSize();

//        Point etchGridOrigin = etchGridUpperLeft();
//        Point upperLeft = new Point(etchGridOrigin.x + (etchSize * etchGridX), etchGridOrigin.y + (etchSize * etchGridY));

//        GeoPoint upperLeftGeo = pixelPointOnMap(upperLeft);
//        GeoPoint upperLeftGeo = CoordinateUtils.getNorthWestPointStillInSameMinIncrement(etchPoint);
        EtchOverlayItem etchItem = new EtchOverlayItem(this, "Etch", "Etch", etchPoint);
        etchItem.setMarkerHotspot(OverlayItem.HotspotPlace.CENTER);

        etchItem.initializeMarker(etchSize);

        final Coordinates coordinates = CoordinateUtils.convert(etchPoint);
        etchItem.fetchEtch(coordinates);

        return etchItem;
    }

    private void centerOnLocation() {
        forceMaxZoom();
        double latitude = mLocation.getLatitude();
        double longitude = mLocation.getLongitude();
        GeoPoint userCenterPoint = new GeoPoint(latitude, longitude);
        mMapController.setCenter(userCenterPoint);

        BoundingBoxE6 boundingBox = new BoundingBoxE6(latitude, longitude, latitude, longitude);

        mMapView.setScrollableAreaLimit(boundingBox);

        addCenterOverlay(userCenterPoint);
    }

    private void addCenterOverlay(GeoPoint userCenterPoint) {
        List<OverlayItem> items = Lists.newArrayList();
        OverlayItem centerItem = new OverlayItem("Center", "Center", userCenterPoint);
        int size = 40;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        centerItem.setMarker(getResources().getDrawable(R.drawable.marker));

        items.add(centerItem);

        mCenterOverlay = new ItemizedIconOverlay<OverlayItem>(this.getActivity(), items, ON_ITEM_GESTURE_LISTENER);

        mMapView.getOverlays().add(mCenterOverlay);
    }

    private void forceMaxZoom() {
        int zoomLevel = mMapView.getMaxZoomLevel();
        // http://wiki.openstreetmap.org/wiki/Zoom_levels
        mMapController.setZoom(zoomLevel);

        // Make sure there is no way to zoom out
//        mMapView.setMinZoomLevel(zoomLevel);
    }


    @Produce
    public MapLocationSelectedEvent produce() {
        return mLastSelectedEvent;
    }

}
