package kurtome.etch.app.openstreetmap;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.*;
import com.google.api.client.util.Base64;
import com.google.api.client.util.Lists;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;
import kurtome.etch.app.ObjectGraphUtils;
import kurtome.etch.app.coordinates.CoordinateUtils;
import kurtome.etch.app.domain.Coordinates;
import kurtome.etch.app.domain.Etch;
import kurtome.etch.app.drawing.DrawingBrush;
import kurtome.etch.app.location.LocationUpdatedEvent;
import kurtome.etch.app.robospice.GetEtchRequest;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;

import javax.inject.Inject;
import java.util.List;

public class MapFragment extends Fragment {

    private static final Logger logger = LoggerManager.getLogger();

    public final ItemizedIconOverlay.OnItemGestureListener<OverlayItem> ON_ITEM_GESTURE_LISTENER = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
        @Override
        public boolean onItemSingleTapUp(int index, OverlayItem item) {
            if (item instanceof EtchOverlayItem) {
                goToSelectedEtch((EtchOverlayItem) item);
            }
            return false;
        }

        private void goToSelectedEtch(EtchOverlayItem etchItem) {
            GeoPoint point = etchItem.getPoint();
            mLastSelectedEvent = new MapLocationSelectedEvent();
            Coordinates coordinates = new Coordinates();
            coordinates.setLatitude(point.getLatitude());
            coordinates.setLongitude(point.getLongitude());
            mLastSelectedEvent.setCoordinates(coordinates);
            mEventBus.post(mLastSelectedEvent);
        }

        @Override
        public boolean onItemLongPress(int index, OverlayItem item) {
            return false;
        }
    };
    private ItemizedIconOverlay<OverlayItem> mCenterOverlay;

    /**
     * http://www.maps.stamen.com
     */
    private static interface StamenMapTileNames {
        String TONER_LITE = "toner-lite";
        String WATERCOLOR = "watercolor";
    }

    public static final String PREFS_NAME = "org.andnav.osm.prefs";
    public static final String PREFS_TILE_SOURCE = "tilesource";
    public static final String PREFS_SCROLL_X = "scrollX";
    public static final String PREFS_SCROLL_Y = "scrollY";
    public static final String PREFS_ZOOM_LEVEL = "zoomLevel";
    public static final String PREFS_SHOW_LOCATION = "showLocation";
    public static final String PREFS_SHOW_COMPASS = "showCompass";

    private static final int DIALOG_ABOUT_ID = 1;

    private static final int MENU_SAMPLES = Menu.FIRST + 1;
    private static final int MENU_ABOUT = MENU_SAMPLES + 1;

    private static final int MENU_LAST_ID = MENU_ABOUT + 1; // Always set to last unused id

    private SharedPreferences mPrefs;
    private MapView mMapView;
    private IMapController mMapController;
    //    private MyLocationNewOverlay mLocationOverlay;
//    private CompassOverlay mCompassOverlay;
//    private MinimapOverlay mMinimapOverlay;
    private ScaleBarOverlay mScaleBarOverlay;
    //    private RotationGestureOverlay mRotationGestureOverlay;
    private ResourceProxy mResourceProxy;
    private Location mLocation;
    private OverlayItem mCenterOverlayItem;
    private MapLocationSelectedEvent mLastSelectedEvent;

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
        ObjectGraphUtils.inject(this);
        mEventBus.register(this);

        mResourceProxy = new ResourceProxyImpl(inflater.getContext().getApplicationContext());
//        mMapView = new MapView(inflater.getContext(), 256, mResourceProxy);
        mMapView = new MapView(inflater.getContext(), 256, mResourceProxy) {
            @Override
            public void scrollBy(int x, int y) {
                // disable scrolling
                //super.scrollBy(x, y);
            }
        };
        mMapController = mMapView.getController();

        // Call this method to turn off hardware acceleration at the View level.
        // setHardwareAccelerationOff();

        return mMapView;
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

        final Context context = this.getActivity();
        final DisplayMetrics dm = context.getResources().getDisplayMetrics();
        // mResourceProxy = new ResourceProxyImpl(getActivity().getApplicationContext());

        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

//        this.mCompassOverlay = new CompassOverlay(
//                context,
//                new InternalCompassOrientationProvider(context),
//                mMapView
//        );
//        this.mLocationOverlay = new MyLocationNewOverlay(
//                context,
//                new GpsMyLocationProvider(context),
//                mMapView
//        );

//        mMinimapOverlay = new MinimapOverlay(context, mMapView.getTileRequestCompleteHandler());
//        mMinimapOverlay.setWidth(dm.widthPixels / 5);
//        mMinimapOverlay.setHeight(dm.heightPixels / 5);
//        mMinimapOverlay.setZoomDifference(5);

        mScaleBarOverlay = new ScaleBarOverlay(context);
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);

//        mRotationGestureOverlay = new RotationGestureOverlay(context, mMapView);
//		mRotationGestureOverlay.setEnabled(false);

        mMapView.setBuiltInZoomControls(false);
        mMapView.setMultiTouchControls(false);
//        mMapView.getOverlays().add(this.mLocationOverlay);
//        mMapView.getOverlays().add(this.mCompassOverlay);
//        mMapView.getOverlays().add(this.mMinimapOverlay);
        mMapView.getOverlays().add(this.mScaleBarOverlay);
//        mMapView.getOverlays().add(this.mRotationGestureOverlay);

        mMapView.scrollTo(mPrefs.getInt(PREFS_SCROLL_X, 0), mPrefs.getInt(PREFS_SCROLL_Y, 0));

//        mLocationOverlay.enableMyLocation();
//        mCompassOverlay.enableCompass();

        forceMaxZoom();

        setHasOptionsMenu(false);

//        mMapView.onTouchEvent()
    }

    @Override
    public void onPause() {
        final SharedPreferences.Editor edit = mPrefs.edit();
        edit.putString(PREFS_TILE_SOURCE, mMapView.getTileProvider().getTileSource().name());
        edit.putInt(PREFS_SCROLL_X, mMapView.getScrollX());
        edit.putInt(PREFS_SCROLL_Y, mMapView.getScrollY());
        edit.putInt(PREFS_ZOOM_LEVEL, mMapView.getZoomLevel());
//        edit.putBoolean(PREFS_SHOW_LOCATION, mLocationOverlay.isMyLocationEnabled());
//        edit.putBoolean(PREFS_SHOW_COMPASS, mCompassOverlay.isCompassEnabled());
        edit.commit();

//        this.mLocationOverlay.disableMyLocation();
//        this.mCompassOverlay.disableCompass();

        super.onPause();
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

        final ITileSource tileSource = createOsmFr();
        mMapView.setTileSource(tileSource);
        forceMaxZoom(); // differs by tile source
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Put overlay items first
        mMapView.getOverlayManager().onCreateOptionsMenu(menu, MENU_LAST_ID, mMapView);

//        // Put samples next
//		SubMenu samplesSubMenu = menu.addSubMenu(0, MENU_SAMPLES, Menu.NONE, R.string.samples)
//				.setIcon(android.R.drawable.ic_menu_gallery);
//		SampleFactory sampleFactory = SampleFactory.getInstance();
//		for (int a = 0; a < sampleFactory.count(); a++) {
//			final BaseSampleFragment f = sampleFactory.getSample(a);
//			samplesSubMenu.add(f.getSampleTitle()).setOnMenuItemClickListener(
//					new MenuItem.OnMenuItemClickListener() {
//						@Override
//						public boolean onMenuItemClick(MenuItem item) {
//							startSampleFragment(f);
//							return true;
//						}
//					});
//		}

//        // Put "About" menu item last
//        menu.add(0, MENU_ABOUT, Menu.CATEGORY_SECONDARY, R.string.about).setIcon(
//                android.R.drawable.ic_menu_info_details);

        super.onCreateOptionsMenu(menu, inflater);
    }

//	protected void startSampleFragment(Fragment fragment) {
//		FragmentManager fm = getFragmentManager();
//		fm.beginTransaction().hide(this).add(android.R.id.content, fragment, "SampleFragment")
//				.addToBackStack(null).commit();
//	}

    @Override
    public void onPrepareOptionsMenu(final Menu pMenu) {
        mMapView.getOverlayManager().onPrepareOptionsMenu(pMenu, MENU_LAST_ID, mMapView);
        super.onPrepareOptionsMenu(pMenu);
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
    public void updateLocation(final LocationUpdatedEvent event) {
        mLocation = event.getLocation();

        centerOnLocation();
        placeEtchOverlays();
    }

    private static class EtchOverlayItem extends OverlayItem {

        public EtchOverlayItem(String aTitle, String aSnippet, GeoPoint aGeoPoint) {
            super(aTitle, aSnippet, aGeoPoint);
        }

    }

    private void placeEtchOverlays() {
        double latitude = mLocation.getLatitude();
        double longitude = mLocation.getLongitude();


        List<OverlayItem> items = Lists.newArrayList();
        GeoPoint exactCenter = new GeoPoint(latitude, longitude);
        GeoPoint point = CoordinateUtils.truncate(exactCenter);

        for (int latOffset = -4; latOffset <= 4; latOffset++) {
            for (int longOffset = -4; longOffset <= 4; longOffset++) {
                EtchOverlayItem etchItem = getEtchOverlayItem(CoordinateUtils.offset(point, latOffset, longOffset));
                items.add(etchItem);
            }
        }
        mMapView.getOverlays().add(new ItemizedIconOverlay<OverlayItem>(this.getActivity(), items, ON_ITEM_GESTURE_LISTENER));

    }

    private EtchOverlayItem getEtchOverlayItem(GeoPoint point) {
        EtchOverlayItem etchItem = new EtchOverlayItem("Etch", "Etch", point);
        etchItem.setMarkerHotspot(OverlayItem.HotspotPlace.UPPER_LEFT_CORNER);

        Projection projection = mMapView.getProjection();
        Point upperLeft = projection.toPixels(point, null);
        Point upperRight = projection.toPixels(CoordinateUtils.incrementEast(point), null);
        GeoPoint lowerLeftGeo = CoordinateUtils.incrementSouth(point);
        Point lowerLeft = projection.toPixels(lowerLeftGeo, null);
        Point lowerRight = projection.toPixels(CoordinateUtils.incrementEast(lowerLeftGeo), null);

        final int size = Math.abs(upperLeft.x - upperRight.x);

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStrokeWidth(1);
        paint.setColor(Color.GRAY);
        canvas.drawLine(1, 1, 1, size - 1, paint); // to lower left
        canvas.drawLine(1, size - 1, size - 1, size - 1, paint); // to lower right
        canvas.drawLine(size - 1, size - 1, size - 1, 1, paint); // to upper right
        canvas.drawLine(size - 1, 1, 1, 1, paint); // to upper left
        etchItem.setMarker(new BitmapDrawable(getResources(), bitmap));

        final Coordinates coordinates = CoordinateUtils.convert(point);
        spiceManager.execute(new GetEtchRequest(coordinates), new RequestListener<Etch>() {

            @Override
            public void onRequestFailure(SpiceException e) {
                logger.e(e, "Error getting etch for location {}.", coordinates);
            }

            @Override
            public void onRequestSuccess(Etch etch) {
                if (etch.getBase64Image() != null) {
                    byte[] bytes = Base64.decodeBase64(etch.getBase64Image());
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    canvas.drawBitmap(Bitmap.createScaledBitmap(bitmap, size, size, false), 0, 0, DrawingBrush.BASIC_PAINT);
                    mMapView.invalidate();
                }
            }
        });

        return etchItem;
    }

    private void centerOnLocation() {
        double latitude = mLocation.getLatitude();
        double longitude = mLocation.getLongitude();
        GeoPoint point = new GeoPoint(latitude, longitude);
        mMapController.setCenter(point);

        BoundingBoxE6 boundingBox = new BoundingBoxE6(latitude, longitude, latitude, longitude);

        mMapView.setScrollableAreaLimit(boundingBox);

//        addCenterOverlay(point);
    }

    private void addCenterOverlay(GeoPoint point) {
        List<OverlayItem> items = Lists.newArrayList();
        mCenterOverlayItem = new OverlayItem("Center", "Center", point);
        items.add(mCenterOverlayItem);
        mCenterOverlay = new ItemizedIconOverlay<OverlayItem>(this.getActivity(), items, ON_ITEM_GESTURE_LISTENER);
        mMapView.getOverlays().add(mCenterOverlay);
    }

    private void forceMaxZoom() {
        int zoomLevel = mMapView.getMaxZoomLevel();
        // http://wiki.openstreetmap.org/wiki/Zoom_levels
        mMapController.setZoom(zoomLevel);

        // Make sure there is no way to zoom out
        mMapView.setMinZoomLevel(zoomLevel);
    }


    @Produce
    public MapLocationSelectedEvent produce() {
        return mLastSelectedEvent;
    }


    // @Override
    // public boolean onTrackballEvent(final MotionEvent event) {
    // return this.mMapView.onTrackballEvent(event);
    // }

}
