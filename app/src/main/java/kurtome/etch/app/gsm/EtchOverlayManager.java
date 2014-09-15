package kurtome.etch.app.gsm;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EtchOverlayManager {
    private static final Logger logger = LoggerFactory.getLogger(EtchOverlayManager.class);

    private final GoogleMapFragment mGoogleMapFragment;
    private List<EtchOverlayImage> mEtchOverlays = Lists.newArrayList();
    private List<EtchOverlayImage> mLoadingEtches = Lists.newArrayList();
    private Runnable mLoadingChangedRunnable;

    public EtchOverlayManager(GoogleMapFragment googleMapFragment) {
        mGoogleMapFragment = googleMapFragment;
    }

    public void addEtch(LatLngBounds latLngBounds) {
        final EtchOverlayImage etchOverlayImage = new EtchOverlayImage(mGoogleMapFragment, latLngBounds);
        mLoadingEtches.add(etchOverlayImage);
        etchOverlayImage.setFinishedLoadingRunnable(new Runnable() {
            @Override
            public void run() {
                mLoadingEtches.remove(etchOverlayImage);
                onLoadingChanged();
            }
        });
        mEtchOverlays.add(etchOverlayImage);
        etchOverlayImage.fetchEtch();
    }

    private void onLoadingChanged() {
        if (mLoadingChangedRunnable != null) {
            mLoadingChangedRunnable.run();
        }
    }

    public Optional<EtchOverlayImage> getEtchAt(LatLng latLng) {
        for (EtchOverlayImage etchOverlay : mEtchOverlays) {
            if (etchOverlay.getLatLngBounds().contains(latLng)) {
                return Optional.of(etchOverlay);
            }
        }
        return Optional.absent();
    }

    public boolean isLoading() {
        for (EtchOverlayImage etchOverlayImage : mEtchOverlays) {
            if (etchOverlayImage.isLoading()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEtches() {
        return !mEtchOverlays.isEmpty();
    }

    public void clearEtches() {
        for (EtchOverlayImage etchOverlayImage : mEtchOverlays) {
            etchOverlayImage.forceReleaseResources();
        }
        mEtchOverlays.clear();
    }

    public void setLoadingChangedRunnable(Runnable runnable) {
        mLoadingChangedRunnable = runnable;
    }
}
