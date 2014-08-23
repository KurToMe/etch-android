package kurtome.etch.app.openstreetmap;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import com.google.common.base.Optional;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import kurtome.etch.app.GzipUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.domain.Coordinates;
import kurtome.etch.app.domain.Etch;
import kurtome.etch.app.drawing.CanvasUtils;
import kurtome.etch.app.drawing.DrawingBrush;
import kurtome.etch.app.drawing.DrawingView;
import kurtome.etch.app.robospice.GetEtchRequest;
import kurtome.etch.app.util.RectangleDimensions;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.OverlayItem;

public class EtchOverlayItem extends OverlayItem {
    private static Logger logger = LoggerManager.getLogger();

    private MapFragment mMapFragment;
    private Coordinates mEtchCoordinates;
    private Canvas mCanvas;
    private RectangleDimensions mEtchSize;

    private Bitmap mDownloadingBitmap;
    private Bitmap mAlertBitmap;

    private Paint overlayPaint = new Paint();
    private final int mStatusIconSize;
    private final Point mStatusIconOffset;

    public EtchOverlayItem(MapFragment mapFragment, String aTitle, String aSnippet, GeoPoint aGeoPoint, RectangleDimensions etchSize) {
        super(aTitle, aSnippet, aGeoPoint);
        mMapFragment = mapFragment;
        mEtchSize = etchSize;
        overlayPaint.setColor(Color.BLACK);
        overlayPaint.setAlpha(100);

        Bitmap bitmap = Bitmap.createBitmap(etchSize.width, etchSize.height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(bitmap);
        setMarker(new BitmapDrawable(mMapFragment.getResources(), bitmap));

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

    public void setEtchCoordinates(Coordinates etchCoordinates) {
        mEtchCoordinates = etchCoordinates;
    }

    private void drawBorder() {
        Paint paint = new Paint();
        paint.setStrokeWidth(2);
        paint.setColor(Color.GRAY);
        mCanvas.drawLine(1, 1, 1, mEtchSize.height - 1, paint); // to lower left
        mCanvas.drawLine(1, mEtchSize.height - 1, mEtchSize.width - 1, mEtchSize.height - 1, paint); // to lower right
        mCanvas.drawLine(mEtchSize.width - 1, mEtchSize.height - 1, mEtchSize.width - 1, 1, paint); // to upper right
        mCanvas.drawLine(mEtchSize.width - 1, 1, 1, 1, paint); // to upper left
    }

    public void fetchEtch(final Coordinates coordinates) {
        drawIconOverlay(mDownloadingBitmap);

        mMapFragment.spiceManager.execute(new GetEtchRequest(coordinates), new RequestListener<Etch>() {
            @Override
            public void onRequestFailure(SpiceException e) {
                logger.e(e, "Error getting etch for location {}.", coordinates);
            }

            @Override
            public void onRequestSuccess(Etch etch) {
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
        mCanvas.drawPaint(overlayPaint);
        mCanvas.drawBitmap(iconBitmap, mStatusIconOffset.x, mStatusIconOffset.y, DrawingBrush.BASIC_PAINT);
        drawBorder();
        mMapFragment.onOverlayInvalidated();
    }

    private void drawEmptyEtch() {
        CanvasUtils.clearCanvas(mCanvas);
        drawBorder();
        mMapFragment.onOverlayInvalidated();
    }

    public void drawBitmap(Bitmap bitmap) {
        CanvasUtils.clearCanvas(mCanvas);

        // The image will be opened in a canvas of height DrawingView.IMAGE_HEIGHT_PIXELS,
        // so make sure to correctly show how much of that height it takes up.
        // (this could differ if the etch was saved when the height constant was different)
        double heightPercentage = Double.valueOf(bitmap.getHeight()) / DrawingView.IMAGE_HEIGHT_PIXELS;
        int finalHeight = (int) Math.round(mEtchSize.height * heightPercentage);

        // if for some reason the incoming bitmap is wider than it should be,
        // the extra width will get chopped off
        // (we're being ok with that for now (since we're driving everything off the height))
        Optional<Integer> desiredHeight = Optional.of(finalHeight);
        CanvasUtils.drawBitmap(mCanvas, bitmap, desiredHeight);

        mMapFragment.onOverlayInvalidated();
        drawBorder();
    }

    public RectangleDimensions getEtchSize() {
        return mEtchSize;
    }
}
