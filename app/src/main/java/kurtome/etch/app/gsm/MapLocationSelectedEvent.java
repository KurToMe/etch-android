package kurtome.etch.app.gsm;

import kurtome.etch.app.domain.Coordinates;

public class MapLocationSelectedEvent {
    private EtchOverlayImage mEtchOverlayImage;

    public Coordinates getCoordinates() {
        return mEtchOverlayImage.getEtchCoordinates();
    }

    public EtchOverlayImage getEtchOverlayItem() {
        return mEtchOverlayImage;
    }

    public void setEtchOverlayImage(EtchOverlayImage etchOverlayImage) {
        mEtchOverlayImage = etchOverlayImage;
    }

    public double getEtchAspectRatio() {
        return mEtchOverlayImage.getAspectRatio();
    }


}
