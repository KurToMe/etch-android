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

import java.lang.ref.WeakReference;

public class EtchOverlayImage {
    private static Logger logger = LoggerFactory.getLogger(EtchOverlayImage.class);

    private final RectangleDimensions mEtchSize;
    private GoogleMapFragment mMapFragment;
    private Coordinates mEtchCoordinates;
//    private Canvas mCanvas;
    private double mEtchAspectRatio;

    private Bitmap mDownloadingBitmap;
    private Bitmap mAlertBitmap;
    private Paint mOverlayPaint = new Paint();
    private LatLngBounds mLatLngBounds;

//    private final GroundOverlay mGroundOverlay;
    private final int mStatusIconSize;
    private final Point mStatusIconOffset;
//    private WeakReference<Bitmap> mEtchBitmap;

    private static final int ETCH_OVERLAY_HEIGHT_PX = 1024;
    private LatLng mOrigin;
    private OnBitmapUpdatedListener mOnBitmapUpdatedListener;
    private boolean mLoading;
    private boolean mReleased;

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
//        mEtchBitmap = new WeakReference<Bitmap>(Bitmap.createBitmap(etchSize.width, etchSize.height, Bitmap.Config.ARGB_8888));
//        mCanvas = new Canvas(mEtchBitmap.get());
//        mGroundOverlay = mMapFragment.getMap().addGroundOverlay(
//                new GroundOverlayOptions()
//                        .image(BitmapDescriptorFactory.fromBitmap(mEtchBitmap))
//                        .positionFromBounds(latLngBounds)
//                        .anchor(0, 0)
//        );

        mStatusIconSize = etchSize.width / 3;

        Bitmap downloadingBmp = BitmapFactory.decodeResource(mMapFragment.getResources(), R.drawable.downloading);
        mStatusIconOffset = new Point(
                (etchSize.width - mStatusIconSize) / 2,
                (etchSize.height - mStatusIconSize) / 2
        );
        mDownloadingBitmap = Bitmap.createScaledBitmap(downloadingBmp, mStatusIconSize, mStatusIconSize, false);

        Bitmap alertBmp = BitmapFactory.decodeResource(mMapFragment.getResources(), R.drawable.alert_icon);
        mAlertBitmap = Bitmap.createScaledBitmap(alertBmp, mStatusIconSize, mStatusIconSize, false);
    }

    public Coordinates getEtchCoordinates() {
        return mEtchCoordinates;
    }

    private void drawBorder(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStrokeWidth(2);
        paint.setColor(Color.GRAY);
        canvas.drawLine(1, 1, 1, mEtchSize.height - 1, paint); // to lower left
        canvas.drawLine(1, mEtchSize.height - 1, mEtchSize.width - 1, mEtchSize.height - 1, paint); // to lower right
        canvas.drawLine(mEtchSize.width - 1, mEtchSize.height - 1, mEtchSize.width - 1, 1, paint); // to upper right
        canvas.drawLine(mEtchSize.width - 1, 1, 1, 1, paint); // to upper left
    }

    public void fetchEtch() {
        mLoading = true;

        mMapFragment.spiceManager.execute(new GetEtchRequest(mEtchCoordinates), new RequestListener<Etch>() {
            @Override
            public void onRequestFailure(SpiceException e) {
                if (mReleased) {
                    return;
                }
                logger.error("Error getting etch for location {}.", mEtchCoordinates, e);
                mLoading = false;
                drawIconOverlay(mAlertBitmap);
            }

            @Override
            public void onRequestSuccess(Etch etch) {
                if (mReleased) {
                    return;
                }
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
        Bitmap bitmap = Bitmap.createBitmap(mEtchSize.width, mEtchSize.height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawPaint(mOverlayPaint);
        canvas.drawBitmap(iconBitmap, mStatusIconOffset.x, mStatusIconOffset.y, DrawingBrush.BASIC_PAINT);
        drawBorder(canvas);
        mMapFragment.onOverlayInvalidated();
        bitmapUpdated(bitmap);
    }


    private void drawEmptyEtch() {
        Bitmap bitmap = Bitmap.createBitmap(mEtchSize.width, mEtchSize.height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawBorder(canvas);
        mMapFragment.onOverlayInvalidated();
        bitmapUpdated(bitmap);
    }

    public void drawBitmap(Bitmap bitmap) {
        Bitmap etchBitmap = Bitmap.createBitmap(mEtchSize.width, mEtchSize.height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(etchBitmap);

        // The image will be opened in a canvas of height DrawingView.IMAGE_HEIGHT_PIXELS,
        // so make sure to correctly show how much of that height it takes up.
        // (this could differ if the etch was saved on server when the height constant was different)
        double heightPercentage = Double.valueOf(bitmap.getHeight()) / DrawingView.IMAGE_HEIGHT_PIXELS;
        int finalHeight = (int) Math.round(mEtchSize.height * heightPercentage);

        // if for some reason the incoming bitmap is wider than it should be,
        // the extra width will get chopped off
        // (we're being ok with that for now (since we're driving everything off the height))
        Optional<Integer> desiredHeight = Optional.of(finalHeight);
        CanvasUtils.drawBitmap(canvas, bitmap, desiredHeight);

        drawBorder(canvas);
        mMapFragment.onOverlayInvalidated();
        bitmapUpdated(etchBitmap);
    }

    private void bitmapUpdated(Bitmap bitmap) {
        if (mOnBitmapUpdatedListener != null) {
            mOnBitmapUpdatedListener.onBitmapUpdated(bitmap);
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

    public boolean isLoading() {
        return mLoading;
    }

    public void forceReleaseResources() {
        mAlertBitmap.recycle();
        mAlertBitmap = null;

        mDownloadingBitmap.recycle();
        mDownloadingBitmap = null;


        mReleased = true;
    }
}
