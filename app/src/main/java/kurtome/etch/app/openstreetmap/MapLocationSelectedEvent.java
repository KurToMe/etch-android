package kurtome.etch.app.openstreetmap;

import kurtome.etch.app.domain.Coordinates;

public class MapLocationSelectedEvent {
    private EtchOverlayItem mEtchOverlayItem;

    public Coordinates getCoordinates() {
        return mEtchOverlayItem.getEtchCoordinates();
    }

    public EtchOverlayItem getEtchOverlayItem() {
        return mEtchOverlayItem;
    }

    public void setEtchOverlayItem(EtchOverlayItem etchOverlayItem) {
        mEtchOverlayItem = etchOverlayItem;
    }
}
