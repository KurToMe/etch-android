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
import kurtome.etch.app.robospice.GetEtchRequest;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.OverlayItem;

public class EtchOverlayItem extends OverlayItem {
    private static Logger logger = LoggerManager.getLogger();

    private MapFragment mMapFragment;
    private Coordinates mEtchCoordinates;
    private Canvas mCanvas;
    private int mEtchSize;

    private Bitmap mDownloadingBitmap;
    private Bitmap mAlertBitmap;

    private Paint overlayPaint = new Paint();
    private final int mIconPadding;

    public EtchOverlayItem(MapFragment mapFragment, String aTitle, String aSnippet, GeoPoint aGeoPoint, int etchSize) {
        super(aTitle, aSnippet, aGeoPoint);
        mMapFragment = mapFragment;
        mEtchSize = etchSize;
        overlayPaint.setColor(Color.BLACK);
        overlayPaint.setAlpha(100);

        Bitmap bitmap = Bitmap.createBitmap(etchSize, etchSize, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(bitmap);
        setMarker(new BitmapDrawable(mMapFragment.getResources(), bitmap));

        mIconPadding = etchSize / 2;

        Bitmap downloadingBmp = BitmapFactory.decodeResource(mMapFragment.getResources(), R.drawable.downloading);
        mDownloadingBitmap = Bitmap.createScaledBitmap(downloadingBmp, etchSize- mIconPadding, etchSize- mIconPadding, false);

        Bitmap alertBmp = BitmapFactory.decodeResource(mMapFragment.getResources(), R.drawable.alert_icon);
        mAlertBitmap = Bitmap.createScaledBitmap(alertBmp, etchSize- mIconPadding, etchSize- mIconPadding, false);
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
        mCanvas.drawLine(1, 1, 1, mEtchSize - 1, paint); // to lower left
        mCanvas.drawLine(1, mEtchSize - 1, mEtchSize - 1, mEtchSize - 1, paint); // to lower right
        mCanvas.drawLine(mEtchSize - 1, mEtchSize - 1, mEtchSize - 1, 1, paint); // to upper right
        mCanvas.drawLine(mEtchSize - 1, 1, 1, 1, paint); // to upper left
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
        mCanvas.drawBitmap(iconBitmap, mIconPadding/2, mIconPadding/2, DrawingBrush.BASIC_PAINT);
        // Always have a border
        drawBorder();
        mMapFragment.onOverlayInvalidated();
    }


    private void drawEmptyEtch() {
        // No etch drawn here yet
        CanvasUtils.clearCanvas(mCanvas);
        drawBorder();
        mMapFragment.onOverlayInvalidated();
    }

    public void drawBitmap(Bitmap bitmap) {
        CanvasUtils.clearCanvas(mCanvas);
        Optional<Integer> scaleSize = Optional.of(mEtchSize);
        CanvasUtils.drawBitmap(mCanvas, bitmap, scaleSize);
        mMapFragment.onOverlayInvalidated();
        drawBorder();
    }
}
