package kurtome.etch.app.gsm;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.*;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import kurtome.etch.app.coordinates.CoordinateUtils;
import kurtome.etch.app.drawing.DrawingBrush;
import kurtome.etch.app.drawing.RectangleUtils;
import kurtome.etch.app.util.RectangleDimensions;
import kurtome.etch.app.util.Twos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EtchOverlayManager {
    private static final Logger logger = LoggerFactory.getLogger(EtchOverlayManager.class);

    private final GoogleMapFragment mGoogleMapFragment;
    private Canvas mCanvas;
    private Bitmap mBitmap;
    private GoogleMap mGoogleMap;
    private LatLngBounds mGridBounds;
    private GroundOverlay mGroundOverlay;
    private int mOverlayBitmapWidth;
    private int mOverlayBitmapHeight;
    private List<EtchOverlayImage> mEtchOverlays = Lists.newArrayList();
    private int mGridSize;

    private static final int ETCH_HEIGHT_PX = 500;

    private LatLng mOrigin;
    private OnBitmapUpdatedListener mOnBitmapUpdatedListener;
    private LatLngBounds mOverlayBounds;

    public EtchOverlayManager(GoogleMapFragment googleMapFragment, int gridSize) {
        mGoogleMapFragment = googleMapFragment;
        mGoogleMap = googleMapFragment.getMap();
        mGridSize = gridSize;
    }

    public LatLngBounds setBounds(LatLngBounds gridBounds) {
        mGridBounds = gridBounds;

        Projection projection = mGoogleMap.getProjection();
        mOrigin = CoordinateUtils.northWestCorner(gridBounds);

        Point originPoint = projection.toScreenLocation(mOrigin);
        Point eastPoint = projection.toScreenLocation(gridBounds.northeast);
        Point southPoint = projection.toScreenLocation(gridBounds.southwest);
        int gridProjectedWidth = eastPoint.x - originPoint.x;
        int gridProjectedHeight = southPoint.y - originPoint.y;

        int etchBitmapGridHeight = mGridSize * ETCH_HEIGHT_PX;
        int etchBitmapGridWidth = etchBitmapGridHeight;

        // For some reason google map GroundOverlays must use an image
        // with power of two dimensions
        mOverlayBitmapWidth = Twos.firstLargerPowerOfTwo(etchBitmapGridWidth);
        mOverlayBitmapHeight = Twos.firstLargerPowerOfTwo(etchBitmapGridHeight);
        double widthRatio = (mOverlayBitmapWidth * 1.0) / etchBitmapGridWidth;
        double heightRatio = (mOverlayBitmapHeight * 1.0) / etchBitmapGridHeight;

        logger.debug("Creating grid bitmap of size {}x{}", mOverlayBitmapWidth, mOverlayBitmapHeight);
        mBitmap = Bitmap.createBitmap(mOverlayBitmapWidth, mOverlayBitmapHeight, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
//        mGroundOverlay = mGoogleMap.addGroundOverlay(
//                new GroundOverlayOptions()
//                    .anchor(0, 0)
//                    .position(mOrigin ,mOverlayBitmapWidth, mOverlayBitmapHeight)
//                    .image(BitmapDescriptorFactory.fromBitmap(mBitmap))
//        );

        int overlaySquareDimens = (int) (gridProjectedHeight * heightRatio);
        Point overlaySoutheastPoint = new Point(
                originPoint.x + overlaySquareDimens,
                originPoint.y + overlaySquareDimens
        );
//        int gridProjectedWidth = originPoint.x - overlaySoutheastPoint.x;
//        double gridProjectedWidth =
        LatLng overlaySoutheast = projection.fromScreenLocation(overlaySoutheastPoint);

//        double widthRatio = mOverlayBitmapWidth * 1.0 / mEtchBitmapGridWidth;
//        double heightRatio = mOverlayBitmapHeight * 1.0 / mEtchBitmapGridHeight;
//        double latDegrees = CoordinateUtils.calculateLatitudeDegrees(mGridBounds);
//        double longDegrees = CoordinateUtils.calculateLongitudeDegrees(mGridBounds);
//        double finalLat = CoordinateUtils.addEast(mOrigin.longitude, longDegrees*widthRatio);
//        double finalLong = CoordinateUtils.addSouth(mOrigin.latitude, latDegrees*heightRatio);
        LatLng northeast = new LatLng(mOrigin.latitude, overlaySoutheast.longitude);
        LatLng southwest = new LatLng(overlaySoutheast.latitude, mOrigin.longitude);
        mOverlayBounds = new LatLngBounds(southwest, northeast);
        return mOverlayBounds;
    }

    public void addEtch(LatLngBounds latLngBounds, final int etchGridRow) {
        if (mCanvas == null) {
            throw new IllegalStateException("Can't add etch when canvas not setup.");
        }

//        double widthRatio = CoordinateUtils.calculateLongitudeRatio(latLngBounds, mGridBounds);
        LatLng etchOrigin = CoordinateUtils.northWestCorner(latLngBounds);
        double degreesEast = CoordinateUtils.calculateDegreesEast(mOrigin, etchOrigin);
        double totalLongitudeDegrees = CoordinateUtils.calculateLongitudeDegrees(mOverlayBounds);
        double widthStartRatio = degreesEast / totalLongitudeDegrees ;

        final int xOffset = (int) Math.round(mOverlayBitmapWidth * widthStartRatio);
        final int yOffset = etchGridRow * ETCH_HEIGHT_PX;

        double aspectRatio = calcAspectRatio(latLngBounds);

        int heightPx = ETCH_HEIGHT_PX;
        int width = RectangleUtils.calcWidthWithAspectRatio(heightPx, aspectRatio);
        RectangleDimensions etchSize = new RectangleDimensions(width, ETCH_HEIGHT_PX);
        EtchOverlayImage etchOverlayImage = new EtchOverlayImage(mGoogleMapFragment, latLngBounds, etchSize);
        mEtchOverlays.add(etchOverlayImage);
        etchOverlayImage.fetchEtch();

        etchOverlayImage.setOnBitmapUpdatedListener(new OnBitmapUpdatedListener() {
            @Override
            public void onBitmapUpdated(Bitmap bitmap) {
                mCanvas.drawBitmap(bitmap, xOffset, yOffset, DrawingBrush.BASIC_PAINT);
                if (mOnBitmapUpdatedListener != null) {
                    mOnBitmapUpdatedListener.onBitmapUpdated(mBitmap);
                }
            }
        });
    }

    private double calcAspectRatio(LatLngBounds bounds) {
        Projection projection = mGoogleMap.getProjection();

        LatLng geo = CoordinateUtils.northWestCorner(bounds);
        LatLng southGeo = bounds.southwest;
        LatLng eastGeo = bounds.northeast;

        Point originPoint = projection.toScreenLocation(geo);
        Point eastPoint = projection.toScreenLocation(eastGeo);
        Point southPoint = projection.toScreenLocation(southGeo);
        int x = eastPoint.x - originPoint.x;
        int y = southPoint.y - originPoint.y;
        logger.debug("Calculating aspect ratio from dimensions {}x{}", x, y);
        double aspectRatio = RectangleUtils.calculateAspectRatio(x, y);
        return aspectRatio;
    }

    public Optional<EtchOverlayImage> getEtchAt(LatLng latLng) {
        for (EtchOverlayImage etchOverlay : mEtchOverlays) {
            if (etchOverlay.getLatLngBounds().contains(latLng)) {
                return Optional.of(etchOverlay);
            }
        }
        return Optional.absent();
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setOnBitmapUpdatedListener(OnBitmapUpdatedListener onBitmapUpdatedListener) {
        mOnBitmapUpdatedListener = onBitmapUpdatedListener;
    }

    public boolean isLoading() {
        for (EtchOverlayImage etchOverlayImage : mEtchOverlays) {
            if (etchOverlayImage.isLoading()) {
                return true;
            }
        }
        return false;
    }
}
