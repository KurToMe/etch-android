package kurtome.etch.app.gsm;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.base.Optional;
import kurtome.etch.app.coordinates.CoordinateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
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

    public boolean hasEtchAtPoint(LatLng latLng) {
        String latLngId = latLngId(latLng);
        return mEtchesByLatLngId.containsKey(latLngId);
    }

    public void addEtch(LatLngBounds latLngBounds, boolean editable) {
        LatLng etchOrigin = CoordinateUtils.northWestCorner(latLngBounds);
        if (hasEtchAtPoint(etchOrigin)) {
            // don't add twice
            return;
        }

        final EtchOverlayImage etchOverlayImage = new EtchOverlayImage(mGoogleMapFragment, latLngBounds, editable);

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
        mLoadingEtchesByLatLngId.clear();
    }

    public void setLoadingChangedRunnable(Runnable runnable) {
        mLoadingChangedRunnable = runnable;
    }

    private String latLngId(LatLng latLng) {
        return String.format("%s,%s", latLng.latitude, latLng.longitude);
    }

    private LatLng latLngFromId(String latLngId) {
        String[] parts = latLngId.split(",");
        double latitude = Double.parseDouble(parts[0]);
        double longitude = Double.parseDouble(parts[1]);
        return new LatLng(latitude, longitude);
    }

    public void removeEtchesOutsideOfBounds(LatLngBounds bounds) {
        Iterator<Map.Entry<String, EtchOverlayImage>> iterator = mEtchesByLatLngId.entrySet().iterator();
        for (Map.Entry<String, EtchOverlayImage> entry = iterator.next(); iterator.hasNext(); entry = iterator.next()) {
            String latLngId = entry.getKey();
            LatLng latLng = latLngFromId(latLngId);
            boolean isOutside = !bounds.contains(latLng);
            if (isOutside) {
                EtchOverlayImage etchToRemove = entry.getValue();
                etchToRemove.forceReleaseResources();
                iterator.remove();
                mLoadingEtchesByLatLngId.remove(latLngId);
            }
        }
    }

    public void recreateExistingEtches() {
        List<EtchOverlayImage> etches = Lists.newArrayList(mEtchesByLatLngId.values());
        clearEtches();

        for (EtchOverlayImage etch : etches) {
            addEtch(etch.getEtchBounds(), etch.isEditable());
        }
    }

    public void showAllEtches() {
        for (EtchOverlayImage etch : mEtchesByLatLngId.values()) {
            etch.showOnMap();
        }
    }
}
