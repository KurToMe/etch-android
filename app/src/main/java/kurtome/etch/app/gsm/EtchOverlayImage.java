package kurtome.etch.app.gsm;

import android.graphics.*;
import com.google.android.gms.maps.model.*;
import com.google.common.base.Optional;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import kurtome.etch.app.GzipUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.coordinates.CoordinateUtils;
import kurtome.etch.app.domain.Coordinates;
import kurtome.etch.app.domain.Etch;
import kurtome.etch.app.drawing.CanvasUtils;
import kurtome.etch.app.drawing.DrawingBrush;
import kurtome.etch.app.drawing.DrawingView;
import kurtome.etch.app.drawing.RectangleUtils;
import kurtome.etch.app.robospice.GetEtchRequest;
import kurtome.etch.app.util.RectangleDimensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EtchOverlayImage {
    private static Logger logger = LoggerFactory.getLogger(EtchOverlayImage.class);

    private final RectangleDimensions mEtchSize;
    private GoogleMapFragment mMapFragment;
    private Coordinates mEtchCoordinates;
    private Canvas mCanvas;
    private double mEtchAspectRatio;

    private Bitmap mDownloadingBitmap;
    private Bitmap mAlertBitmap;
    private Paint mOverlayPaint = new Paint();
    private LatLngBounds mLatLngBounds;

//    private final GroundOverlay mGroundOverlay;
    private final int mStatusIconSize;
    private final Point mStatusIconOffset;
    private final Bitmap mEtchBitmap;

    private static final int ETCH_OVERLAY_HEIGHT_PX = 1024;
    private LatLng mOrigin;
    private OnBitmapUpdatedListener mOnBitmapUpdatedListener;
    private boolean mLoading;

    public EtchOverlayImage(GoogleMapFragment mapFragment, LatLngBounds latLngBounds, RectangleDimensions etchSize) {
        mMapFragment = mapFragment;
        mEtchSize = etchSize;
        mEtchAspectRatio = RectangleUtils.calculateAspectRatio(etchSize);
        mOverlayPaint.setColor(Color.BLACK);
        mOverlayPaint.setAlpha(100);
        mLatLngBounds = latLngBounds;
        LatLng point = CoordinateUtils.northWestCorner(latLngBounds);
        mEtchCoordinates = CoordinateUtils.convert(point);

//        int heightPx = ETCH_OVERLAY_HEIGHT_PX;
//        int width = RectangleUtils.calcWidthWithAspectRatio(heightPx, mEtchAspectRatio);
        mEtchBitmap = Bitmap.createBitmap(etchSize.width, etchSize.height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mEtchBitmap);
//        mGroundOverlay = mMapFragment.getMap().addGroundOverlay(
//                new GroundOverlayOptions()
//                        .image(BitmapDescriptorFactory.fromBitmap(mEtchBitmap))
//                        .positionFromBounds(latLngBounds)
//                        .anchor(0, 0)
//        );

        mStatusIconSize = mEtchBitmap.getWidth() / 3;

        Bitmap downloadingBmp = BitmapFactory.decodeResource(mMapFragment.getResources(), R.drawable.downloading);
        mStatusIconOffset = new Point(
                (mEtchBitmap.getWidth() - mStatusIconSize) / 2,
                (mEtchBitmap.getHeight() - mStatusIconSize) / 2
        );
        mDownloadingBitmap = Bitmap.createScaledBitmap(downloadingBmp, mStatusIconSize, mStatusIconSize, false);

        Bitmap alertBmp = BitmapFactory.decodeResource(mMapFragment.getResources(), R.drawable.alert_icon);
        mAlertBitmap = Bitmap.createScaledBitmap(alertBmp, mStatusIconSize, mStatusIconSize, false);
    }

    public Coordinates getEtchCoordinates() {
        return mEtchCoordinates;
    }

    private void drawBorder() {
        Paint paint = new Paint();
        paint.setStrokeWidth(2);
        paint.setColor(Color.GRAY);
        mCanvas.drawLine(1, 1, 1, mEtchBitmap.getHeight() - 1, paint); // to lower left
        mCanvas.drawLine(1, mEtchBitmap.getHeight() - 1, mEtchBitmap.getWidth() - 1, mEtchBitmap.getHeight() - 1, paint); // to lower right
        mCanvas.drawLine(mEtchBitmap.getWidth() - 1, mEtchBitmap.getHeight() - 1, mEtchBitmap.getWidth() - 1, 1, paint); // to upper right
        mCanvas.drawLine(mEtchBitmap.getWidth() - 1, 1, 1, 1, paint); // to upper left
    }

    public void fetchEtch() {
        mLoading = true;
        drawIconOverlay(mDownloadingBitmap);

        mMapFragment.spiceManager.execute(new GetEtchRequest(mEtchCoordinates), new RequestListener<Etch>() {
            @Override
            public void onRequestFailure(SpiceException e) {
                logger.error("Error getting etch for location {}.", mEtchCoordinates, e);
                mLoading = false;
                drawIconOverlay(mAlertBitmap);
            }

            @Override
            public void onRequestSuccess(Etch etch) {
                mLoading = false;
                if (etch.getGzipImage().length > 0) {
                    Optional<byte[]> bytes = GzipUtils.unzip(etch.getGzipImage());
                    if (bytes.isPresent()) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes.get(), 0, bytes.get().length);
                        drawBitmap(bitmap);
                    }
                    else {
                        drawIconOverlay(mAlertBitmap);
                    }
                }
                else {
                    drawEmptyEtch();
                }
            }
        });
    }

    private void drawIconOverlay(Bitmap iconBitmap) {
        mCanvas.drawPaint(mOverlayPaint);
        mCanvas.drawBitmap(iconBitmap, mStatusIconOffset.x, mStatusIconOffset.y, DrawingBrush.BASIC_PAINT);
        drawBorder();
        syncOverlayFromCanvas();
        mMapFragment.onOverlayInvalidated();
        bitmapUpdated();
    }

    private void syncOverlayFromCanvas() {
//        mGroundOverlay.setImage(BitmapDescriptorFactory.fromBitmap(mEtchBitmap));
    }

    private void drawEmptyEtch() {
        CanvasUtils.clearCanvas(mCanvas);
        drawBorder();
        syncOverlayFromCanvas();
        mMapFragment.onOverlayInvalidated();
        bitmapUpdated();
    }

    public void drawBitmap(Bitmap bitmap) {
        CanvasUtils.clearCanvas(mCanvas);

        // The image will be opened in a canvas of height DrawingView.IMAGE_HEIGHT_PIXELS,
        // so make sure to correctly show how much of that height it takes up.
        // (this could differ if the etch was saved on server when the height constant was different)
        double heightPercentage = Double.valueOf(bitmap.getHeight()) / DrawingView.IMAGE_HEIGHT_PIXELS;
        int finalHeight = (int) Math.round(mEtchBitmap.getHeight() * heightPercentage);

        // if for some reason the incoming bitmap is wider than it should be,
        // the extra width will get chopped off
        // (we're being ok with that for now (since we're driving everything off the height))
        Optional<Integer> desiredHeight = Optional.of(finalHeight);
        CanvasUtils.drawBitmap(mCanvas, bitmap, desiredHeight);

        drawBorder();
        syncOverlayFromCanvas();
        mMapFragment.onOverlayInvalidated();
        bitmapUpdated();
    }

    private void bitmapUpdated() {
        if (mOnBitmapUpdatedListener != null) {
            mOnBitmapUpdatedListener.onBitmapUpdated(mEtchBitmap);
        }
    }

    public void remove() {
//        mGroundOverlay.remove();
    }

    public LatLngBounds getLatLngBounds() {
        return mLatLngBounds;
    }

    public double getAspectRatio() {
        return mEtchAspectRatio;
    }

    public LatLng getOrigin() {
        return mOrigin;
    }

    public void setOnBitmapUpdatedListener(OnBitmapUpdatedListener onBitmapUpdatedListener) {
        mOnBitmapUpdatedListener = onBitmapUpdatedListener;
    }

    public Bitmap getBitmap() {
        return mEtchBitmap;
    }

    public boolean isLoading() {
        return mLoading;
    }
}
