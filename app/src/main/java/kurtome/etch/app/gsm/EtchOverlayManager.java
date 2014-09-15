package kurtome.etch.app.gsm;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.api.client.util.Maps;
import com.google.common.base.Optional;
import kurtome.etch.app.coordinates.CoordinateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class EtchOverlayManager {
    private static final Logger logger = LoggerFactory.getLogger(EtchOverlayManager.class);

    private final GoogleMapFragment mGoogleMapFragment;
    private Map<String, EtchOverlayImage> mEtchesByLatLngId = Maps.newHashMap();
    private Map<String, EtchOverlayImage> mLoadingEtchesByLatLngId = Maps.newHashMap();
    private Runnable mLoadingChangedRunnable;

    public EtchOverlayManager(GoogleMapFragment googleMapFragment) {
        mGoogleMapFragment = googleMapFragment;
    }

    public void addEtch(LatLngBounds latLngBounds) {
        final EtchOverlayImage etchOverlayImage = new EtchOverlayImage(mGoogleMapFragment, latLngBounds);
        LatLng etchOrigin = CoordinateUtils.northWestCorner(latLngBounds);
        final String latLngId = latLngId(etchOrigin);

        mLoadingEtchesByLatLngId.put(latLngId, etchOverlayImage);
        etchOverlayImage.setFinishedLoadingRunnable(new Runnable() {
            @Override
            public void run() {
                mLoadingEtchesByLatLngId.remove(latLngId);
                onLoadingChanged();
            }
        });
        mEtchesByLatLngId.put(latLngId, etchOverlayImage);
        etchOverlayImage.fetchEtch();
    }

    private void onLoadingChanged() {
        if (mLoadingChangedRunnable != null) {
            mLoadingChangedRunnable.run();
        }
    }

    public Optional<EtchOverlayImage> getEtchAt(LatLng latLng) {
        String latLngId = latLngId(latLng);

        return Optional.fromNullable(mEtchesByLatLngId.get(latLngId));
    }

    public boolean isLoading() {
        return !mLoadingEtchesByLatLngId.isEmpty();
    }

    public boolean hasEtches() {
        return !mEtchesByLatLngId.isEmpty();
    }

    public void clearEtches() {
        for (EtchOverlayImage etchOverlayImage : mEtchesByLatLngId.values()) {
            etchOverlayImage.forceReleaseResources();
        }
        mEtchesByLatLngId.clear();
    }

    public void setLoadingChangedRunnable(Runnable runnable) {
        mLoadingChangedRunnable = runnable;
    }

    private String latLngId(LatLng latLng) {
        return String.format("%s,%s", latLng.latitude, latLng.longitude);
    }
}
