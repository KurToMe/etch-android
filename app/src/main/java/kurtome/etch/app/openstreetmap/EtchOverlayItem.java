package kurtome.etch.app.openstreetmap;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import com.google.common.base.Optional;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import kurtome.etch.app.GzipUtils;
import kurtome.etch.app.domain.Coordinates;
import kurtome.etch.app.domain.Etch;
import kurtome.etch.app.drawing.CanvasUtils;
import kurtome.etch.app.robospice.GetEtchRequest;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.OverlayItem;

public class EtchOverlayItem extends OverlayItem {
    private static Logger logger = LoggerManager.getLogger();

    private MapFragment mMapFragment;
    private Coordinates mEtchCoordinates;
    private Canvas mCanvas;
    private int mEtchSize;

    public EtchOverlayItem(MapFragment mapFragment, String aTitle, String aSnippet, GeoPoint aGeoPoint) {
        super(aTitle, aSnippet, aGeoPoint);
        mMapFragment = mapFragment;
    }

    public Coordinates getEtchCoordinates() {
        return mEtchCoordinates;
    }

    public void setEtchCoordinates(Coordinates etchCoordinates) {
        mEtchCoordinates = etchCoordinates;
    }

    public void initializeMarker(int etchSize) {
        mEtchSize = etchSize;
        Bitmap bitmap = Bitmap.createBitmap(etchSize, etchSize, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(bitmap);
        setMarker(new BitmapDrawable(mMapFragment.getResources(), bitmap));
    }

    private void drawBorder(int etchSize) {
        Paint paint = new Paint();
        paint.setStrokeWidth(2);
        paint.setColor(Color.GRAY);
        mCanvas.drawLine(1, 1, 1, etchSize - 1, paint); // to lower left
        mCanvas.drawLine(1, etchSize - 1, etchSize - 1, etchSize - 1, paint); // to lower right
        mCanvas.drawLine(etchSize - 1, etchSize - 1, etchSize - 1, 1, paint); // to upper right
        mCanvas.drawLine(etchSize - 1, 1, 1, 1, paint); // to upper left
    }

    public void fetchEtch(final Coordinates coordinates) {
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
                        scaleAndSetBitmap(bitmap);
                    }
                }
                drawBorder(mEtchSize);
            }
        });
    }

    public void scaleAndSetBitmap(Bitmap bitmap) {
        CanvasUtils.clearCanvas(mCanvas);
        Optional<Integer> scaleSize = Optional.of(mEtchSize);
        CanvasUtils.drawBitmap(mCanvas, bitmap, scaleSize);
        mMapFragment.onOverlayInvalidated();
    }
}
