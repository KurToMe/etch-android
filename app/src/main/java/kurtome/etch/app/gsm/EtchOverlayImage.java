package kurtome.etch.app.gsm;

import android.graphics.*;
import android.os.AsyncTask;
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
    private final LatLngBounds mEtchBounds;
    private final boolean mEditable;
    private GoogleMapFragment mMapFragment;
    private Coordinates mEtchCoordinates;

    private Paint mOverlayPaint = new Paint();

    private final int mStatusIconSize;
    private final Point mStatusIconOffset;
    private boolean mLoading;

    /**
     * Currently assumes this is a power of two so it can be used for the sample size
     */
    private static final int ETCH_OVERLAY_DENSITY_RATIO = 2;

    /**
     * Must be power of two to work well with google map
     */
    private static final int ETCH_MAP_HEIGHT_PX =
            DrawingView.IMAGE_HEIGHT_PIXELS / ETCH_OVERLAY_DENSITY_RATIO;

    private LatLng mOrigin;
    private boolean mReleased;
    private GroundOverlay mGroundOverlay;
    private Runnable mFinishedLoadingRunnable;

    public EtchOverlayImage(GoogleMapFragment mapFragment, LatLngBounds latLngBounds, boolean editable) {
        mEtchBounds = latLngBounds;
        mOrigin = CoordinateUtils.northWestCorner(latLngBounds);
        mEditable = editable;
        mMapFragment = mapFragment;
        mOverlayPaint.setColor(Color.BLACK);
        mOverlayPaint.setAlpha(100);
        LatLng point = CoordinateUtils.northWestCorner(latLngBounds);
        mEtchCoordinates = CoordinateUtils.convert(point);

        int heightPx = ETCH_MAP_HEIGHT_PX;
        int widthPx = RectangleUtils.calcWidthWithAspectRatio(heightPx, getAspectRatio());

        mOverlayBitmapHeight = ETCH_MAP_HEIGHT_PX;
        // leaves extra width, but must be power of two so just make it square
        mOverlayBitmapWidth = ETCH_MAP_HEIGHT_PX;

        mEtchSize = new RectangleDimensions(widthPx, heightPx);

        mOverlayBounds = ProjectionUtils.extendWidthToCreateSquareBounds(
                mapFragment.getMap().getProjection(),
                latLngBounds
        );


        mStatusIconSize = mEtchSize.width / 6;

//        mStatusIconOffset = new Point(
//                (mEtchSize.width - mStatusIconSize) / 2,
//                (mEtchSize.height - mStatusIconSize) / 2
//        );
        mStatusIconOffset = new Point(10, 10);

    }

    public Coordinates getEtchCoordinates() {
        return mEtchCoordinates;
    }

    private void drawBorder(Canvas canvas) {
        if (!mEditable) {
            return;
        }

//        Paint paint = new Paint();
//        paint.setStrokeWidth(2);
//        canvas.drawLine(1, 1, 1, mEtchSize.height - 1, paint); // to lower left
//        canvas.drawLine(1, mEtchSize.height - 1, mEtchSize.width - 1, mEtchSize.height - 1, paint); // to lower right
//        canvas.drawLine(mEtchSize.width - 1, mEtchSize.height - 1, mEtchSize.width - 1, 1, paint); // to upper right
//        canvas.drawLine(mEtchSize.width - 1, 1, 1, 1, paint); // to upper left
    }

    public void fetchEtch() {
        mLoading = true;

        new SetBitmapAsOverlayImage().execute( Optional.<Bitmap>absent() );

        mMapFragment.spiceManager.execute(new GetEtchRequest(mEtchCoordinates), new RequestListener<Etch>() {
            @Override
            public void onRequestFailure(SpiceException e) {
                if (mReleased) {
                    return;
                }
                logger.error("Error getting etch for location {}.", mEtchCoordinates, e);
                onFinishedLoading();

                new SetBitmapAsOverlayImage().execute( Optional.<Bitmap>absent() );
            }

            @Override
            public void onRequestSuccess(Etch etch) {
                if (mReleased) {
                    return;
                }
                onFinishedLoading();

                new SetEtchDataAsOverlayImage().execute(etch);
            }
        });
    }

    private class SetEtchDataAsOverlayImage extends AsyncTask<Etch, Void, Void> {

        @Override
        protected Void doInBackground(Etch... etches) {
            Etch etch = etches[0];

            final Bitmap overlayBitmap = getOverlayBitmap(etch);
            final BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(overlayBitmap);
            mMapFragment.getMapView().post(new Runnable() {
                @Override
                public void run() {
                    setGroundOverlayImage(bitmapDescriptor);
                }
            });
            return null;
        }
    }

    private class SetBitmapAsOverlayImage extends AsyncTask<Optional<Bitmap>, Void, Void> {

        @Override
        protected Void doInBackground(Optional<Bitmap>... etches) {
            Optional<Bitmap> bitmap = etches[0];

            final BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(createScaledEtchBitmap(
                    bitmap,
                    getStatusIconBitmap(),
                    ETCH_MAP_HEIGHT_PX
            ));
            mMapFragment.getMapView().post(new Runnable() {
                @Override
                public void run() {
                    setGroundOverlayImage(bitmapDescriptor);
                }
            });
            return null;
        }
    }

    private void setGroundOverlayImage(BitmapDescriptor bitmap) {
        if (mGroundOverlay == null) {
            mGroundOverlay = mMapFragment.getMap().addGroundOverlay(
                    new GroundOverlayOptions()
                            .image(bitmap)
                            .positionFromBounds(mOverlayBounds)
                            .anchor(0, 0)
                            .bearing(0)
            );
        }
        else {
            mGroundOverlay.setImage(bitmap);
        }
    }

    private void onFinishedLoading() {
        mLoading = false;
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
                Bitmap etchBitmap = BitmapFactory.decodeByteArray(
                        bytes.get(),
                        offset,
                        bytes.get().length,
                        options
                );
                Bitmap scaledEtchBitmap = createScaledEtchBitmap(
                        Optional.of(etchBitmap),
                        getStatusIconBitmap(),
                        ETCH_MAP_HEIGHT_PX
                );
                return scaledEtchBitmap;
            }
            else {
                Bitmap alertBmp = BitmapFactory.decodeResource(
                        mMapFragment.getResources(),
                        R.drawable.alert_icon
                );
                return createScaledEtchBitmap(
                        Optional.<Bitmap>absent(),
                        Optional.of(alertBmp),
                        ETCH_MAP_HEIGHT_PX
                );
            }
        }
        else {
            return createScaledEtchBitmap(
                    Optional.<Bitmap>absent(),
                    getStatusIconBitmap(),
                    ETCH_MAP_HEIGHT_PX
            );
        }
    }

    private Bitmap scaleStatusIcon(Bitmap iconBitmap) {
        return Bitmap.createScaledBitmap(iconBitmap, mStatusIconSize, mStatusIconSize, false);
    }


//    private Bitmap getBitmapWithIcon(Bitmap iconBitmap, Optional<Bitmap> etchBitmap) {
//        Bitmap bitmap = Bitmap.createBitmap(mOverlayBitmapWidth, mOverlayBitmapHeight, Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(bitmap);
//        Bitmap scaledBitmap = scaleStatusIcon(iconBitmap);
//        drawStatusIconToCanvas(canvas, scaledBitmap);
//        drawBorder(canvas);
//        return bitmap;
//    }

    private void drawStatusIconToCanvas(Canvas canvas, Bitmap iconBitmap) {
        Bitmap scaledBitmap = scaleStatusIcon(iconBitmap);
        canvas.drawRect(
                new Rect(mStatusIconOffset.x, mStatusIconOffset.y, mStatusIconOffset.x + mStatusIconSize, mStatusIconOffset.y + mStatusIconSize),
                mOverlayPaint
        );
        canvas.drawBitmap(scaledBitmap, mStatusIconOffset.x, mStatusIconOffset.y, DrawingBrush.BASIC_PAINT);
    }


    private Bitmap createOverlayBitmap() {
        return Bitmap.createBitmap(mOverlayBitmapWidth, mOverlayBitmapHeight, Bitmap.Config.ARGB_8888);
    }

//    private Bitmap drawEmptyEtch() {
//        Bitmap bitmap = createOverlayBitmap();
//        Canvas canvas = new Canvas(bitmap);
//        drawBorder(canvas);
//        return bitmap;
//    }

    public Bitmap createScaledEtchBitmap(Optional<Bitmap> bitmap, Optional<Bitmap> statusBitmap, int srcHeight) {
        Bitmap canvasBitmap = createOverlayBitmap();
        Canvas canvas = new Canvas(canvasBitmap);

//        if (statusBitmap.isPresent()) {
//            drawStatusIconToCanvas(canvas, statusBitmap.get());
//        }

        if (bitmap.isPresent()) {
            Optional<RectangleDimensions> desiredSize = calcScaleDimensions(bitmap.get(), srcHeight);
            CanvasUtils.drawBitmapScaledBitmap(canvas, bitmap.get(), desiredSize);
//            CanvasUtils.drawBitmap(canvas, bitmap.get());
            logger.info(
                    "source bitmap size {}x{}. canvas bitmap size {}x{}. borders size {}x{}",
                    bitmap.get().getWidth(),
                    bitmap.get().getHeight(),
                    canvasBitmap.getWidth(),
                    canvasBitmap.getHeight(),
                    mEtchSize.width,
                    mEtchSize.height
            );
        }

        drawBorder(canvas);
        return canvasBitmap;
    }

    public Optional<Bitmap> getStatusIconBitmap() {
//        if (mLoading) {
//            Bitmap editBitmap = BitmapFactory.decodeResource(mMapFragment.getResources(), R.drawable.ic_action_file_download);
//            return Optional.of(editBitmap);
//        }
//
//        if (mEditable) {
//            Bitmap editBitmap = BitmapFactory.decodeResource(mMapFragment.getResources(), R.drawable.ic_action_create);
//            return Optional.of(editBitmap);
//        }

        return Optional.absent();
    }

    public void setEtchBitmap(Bitmap bitmap) {
        new SetBitmapAsOverlayImage().execute( Optional.of(bitmap) );
    }

    private Optional<RectangleDimensions> calcScaleDimensions(Bitmap bitmap, int srcHeight) {
//        if (bitmap.getHeight() == mEtchSize.height &&
//                bitmap.getWidth() <= mEtchSize.width) {
//            return Optional.absent();
//        }

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
//        return Optional.absent();
    }

    public double getAspectRatio() {
        return ProjectionUtils.calcAspectRatio(
                mMapFragment.getMap().getProjection(),
                mEtchBounds
        );
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

    public void hideFromMap() {
        mGroundOverlay.setVisible(false);
    }

    public void showOnMap() {
        mGroundOverlay.setVisible(true);
    }

    public LatLng getEtchLatLng() {
        return mOrigin;
    }

    public LatLngBounds getEtchBounds() {
        return mEtchBounds;
    }

    public boolean isEditable() {
        return mEditable;
    }

    public RectangleDimensions getCurrentScreenDimensions() {
        return ProjectionUtils.calcProjectedSize(
                mMapFragment.getMap().getProjection(),
                mEtchBounds
        );
    }

    public Point currentOriginPoint() {
        return mMapFragment.getMap().getProjection().toScreenLocation(
                mGroundOverlay.getPosition()
        );
//        return ProjectionUtils.calcOrigin(
//                mEtchBounds
//        );
    }
}
