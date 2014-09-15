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
    private final int mOverlayBitmapWidth;
    private final int mOverlayBitmapHeight;
    private final LatLngBounds mOverlayBounds;
    private final boolean mEditable;
    private GoogleMapFragment mMapFragment;
    private Coordinates mEtchCoordinates;
    private double mEtchAspectRatio;

    private Paint mOverlayPaint = new Paint();

    private final int mStatusIconSize;
    private final Point mStatusIconOffset;

    /**
     * Currently assumes this is a power of two so it can be used for the sample size
     */
    private static final int ETCH_OVERLAY_DENSITY_RATIO = 2;

    /**
     * Must be power of two to work well with google map
     */
    private static final int ETCH_MAP_HEIGHT_PX = DrawingView.IMAGE_HEIGHT_PIXELS / ETCH_OVERLAY_DENSITY_RATIO;
    private LatLng mOrigin;
    private boolean mReleased;
    private GroundOverlay mGroundOverlay;
    private Runnable mFinishedLoadingRunnable;

    public EtchOverlayImage(GoogleMapFragment mapFragment, LatLngBounds latLngBounds, boolean editable) {
        mOrigin = CoordinateUtils.northWestCorner(latLngBounds);
        mEditable = editable;
        mMapFragment = mapFragment;
        mOverlayPaint.setColor(Color.BLACK);
        mOverlayPaint.setAlpha(100);
        LatLng point = CoordinateUtils.northWestCorner(latLngBounds);
        mEtchCoordinates = CoordinateUtils.convert(point);

        int heightPx = ETCH_MAP_HEIGHT_PX;
        mEtchAspectRatio = ProjectionUtils.calcAspectRatio(mapFragment.getMap().getProjection(), latLngBounds);
        int widthPx = RectangleUtils.calcWidthWithAspectRatio(heightPx, mEtchAspectRatio);

        mOverlayBitmapWidth = ETCH_MAP_HEIGHT_PX;
        mOverlayBitmapHeight = ETCH_MAP_HEIGHT_PX; // leaves extra width, but must be power of two so just make it square

        mEtchSize = new RectangleDimensions(widthPx, heightPx);

        mOverlayBounds = ProjectionUtils.extendWidthToCreateSquareBounds(
                mapFragment.getMap().getProjection(),
                latLngBounds
        );


        mStatusIconSize = mEtchSize.width / 3;

        mStatusIconOffset = new Point(
                (mEtchSize.width - mStatusIconSize) / 2,
                (mEtchSize.height - mStatusIconSize) / 2
        );

    }

    public Coordinates getEtchCoordinates() {
        return mEtchCoordinates;
    }

    private void drawBorder(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStrokeWidth(2);
        if (!mEditable) {
            paint.setColor(Color.GRAY);
            paint.setAlpha(150);
        }
        canvas.drawLine(1, 1, 1, mEtchSize.height - 1, paint); // to lower left
        canvas.drawLine(1, mEtchSize.height - 1, mEtchSize.width - 1, mEtchSize.height - 1, paint); // to lower right
        canvas.drawLine(mEtchSize.width - 1, mEtchSize.height - 1, mEtchSize.width - 1, 1, paint); // to upper right
        canvas.drawLine(mEtchSize.width - 1, 1, 1, 1, paint); // to upper left
    }

    public void fetchEtch() {
        mMapFragment.spiceManager.execute(new GetEtchRequest(mEtchCoordinates), new RequestListener<Etch>() {
            @Override
            public void onRequestFailure(SpiceException e) {
                if (mReleased) {
                    return;
                }
                logger.error("Error getting etch for location {}.", mEtchCoordinates, e);
                onFinishedLoading();

                Bitmap alertBmp = BitmapFactory.decodeResource(mMapFragment.getResources(), R.drawable.alert_icon);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(alertBmp, mStatusIconSize, mStatusIconSize, false);
                Bitmap overlayBitmap = drawIconOverlay(scaledBitmap);

                setGroundOverlayImage(overlayBitmap);
            }

            @Override
            public void onRequestSuccess(Etch etch) {
                if (mReleased) {
                    return;
                }
                onFinishedLoading();

                Bitmap overlayBitmap = getOverlayBitmap(etch);

                setGroundOverlayImage(overlayBitmap);
            }
        });
    }

    private void setGroundOverlayImage(Bitmap bitmap) {
        if (mGroundOverlay == null) {
            mGroundOverlay = mMapFragment.getMap().addGroundOverlay(
                    new GroundOverlayOptions()
                            .image(BitmapDescriptorFactory.fromBitmap(bitmap))
                            .positionFromBounds(mOverlayBounds)
                            .anchor(0, 0)
            );
        }
        else {
            mGroundOverlay.setImage(
                    BitmapDescriptorFactory.fromBitmap(bitmap)
            );
        }
    }

    private void onFinishedLoading() {
        if (mFinishedLoadingRunnable != null) {
            mFinishedLoadingRunnable.run();
        }
    }

    private Bitmap getOverlayBitmap(Etch etch) {
        if (etch.getGzipImage().length > 0) {
            Optional<byte[]> bytes = GzipUtils.unzip(etch.getGzipImage());
            if (bytes.isPresent()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = ETCH_OVERLAY_DENSITY_RATIO;
                int offset = 0;
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes.get(), offset, bytes.get().length, options);
                return createScaledEtchBitmap(bitmap, ETCH_MAP_HEIGHT_PX);
            }
            else {
                Bitmap alertBmp = BitmapFactory.decodeResource(mMapFragment.getResources(), R.drawable.alert_icon);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(alertBmp, mStatusIconSize, mStatusIconSize, false);
                return drawIconOverlay(scaledBitmap);
            }
        }
        else {
            return drawEmptyEtch();
        }
    }

    private Bitmap drawIconOverlay(Bitmap iconBitmap) {
        Bitmap bitmap = Bitmap.createBitmap(mOverlayBitmapWidth, mOverlayBitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawPaint(mOverlayPaint);
        canvas.drawBitmap(iconBitmap, mStatusIconOffset.x, mStatusIconOffset.y, DrawingBrush.BASIC_PAINT);
        drawBorder(canvas);
        return bitmap;
    }

    private Bitmap createOverlayBitmap() {
        return Bitmap.createBitmap(mOverlayBitmapWidth, mOverlayBitmapHeight, Bitmap.Config.ARGB_8888);
    }

    private Bitmap drawEmptyEtch() {
        Bitmap bitmap = createOverlayBitmap();
        Canvas canvas = new Canvas(bitmap);
        drawBorder(canvas);
        return bitmap;
    }

    public Bitmap createScaledEtchBitmap(Bitmap bitmap, int srcHeight) {
        Bitmap etchBitmap = createOverlayBitmap();
        Canvas canvas = new Canvas(etchBitmap);


        Optional<RectangleDimensions> desiredSize = calcScaleDimensions(bitmap, srcHeight);
        CanvasUtils.drawBitmapScalingBasedOnHeightThenCropping(canvas, bitmap, desiredSize);

        drawBorder(canvas);
        return etchBitmap;
    }

    public void setEtchBitmap(Bitmap bitmap) {
        Bitmap bitmapToDraw = createScaledEtchBitmap(bitmap, bitmap.getHeight());

        setGroundOverlayImage(bitmapToDraw);
    }

    private Optional<RectangleDimensions> calcScaleDimensions(Bitmap bitmap, int srcHeight) {
        if (bitmap.getHeight() == mEtchSize.height &&
                bitmap.getWidth() <= mEtchSize.width) {
            return Optional.absent();
        }

        // The image will be opened in a canvas of height DrawingView.IMAGE_HEIGHT_PIXELS,
        // so make sure to correctly show how much of that height it takes up.
        // (this could differ if the etch was saved on server when the height constant was different)
        double scaleRatio = Double.valueOf(bitmap.getHeight()) / srcHeight;

        // if for some reason the incoming bitmap is wider than it should be,
        // the extra width will get chopped off
        // (we're being ok with that for now (since we're driving everything off the height))
        int finalHeight = (int) Math.round(mEtchSize.height * scaleRatio);

        // Desired width is constant because we'll just crop the extra if there is any after scaling.
        // When scaling we'll keep the aspect ratio to prevent distortion
        int finalWidth = mEtchSize.width;

        RectangleDimensions desiredSize = new RectangleDimensions(finalWidth, finalHeight);
        return Optional.of(desiredSize);
    }

    public double getAspectRatio() {
        return mEtchAspectRatio;
    }

    public void setFinishedLoadingRunnable(Runnable runnable) {
        mFinishedLoadingRunnable = runnable;
    }

    public void forceReleaseResources() {
        if (mGroundOverlay != null) {
            mGroundOverlay.remove();
            mGroundOverlay = null;
        }

        mReleased = true;
    }

    public LatLng getEtchLatLng() {
        return mOrigin;
    }
}
