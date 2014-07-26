package kurtome.etch.app.openstreetmap;

import kurtome.etch.app.domain.Coordinates;

public class MapLocationSelectedEvent {
    private MapFragment.EtchOverlayItem mEtchOverlayItem;

    public Coordinates getCoordinates() {
        return mEtchOverlayItem.getEtchCoordinates();
    }

    public MapFragment.EtchOverlayItem getEtchOverlayItem() {
        return mEtchOverlayItem;
    }

    public void setEtchOverlayItem(MapFragment.EtchOverlayItem etchOverlayItem) {
        mEtchOverlayItem = etchOverlayItem;
    }
}
