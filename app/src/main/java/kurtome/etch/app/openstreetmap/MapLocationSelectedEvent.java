package kurtome.etch.app.openstreetmap;

import kurtome.etch.app.domain.Coordinates;
import kurtome.etch.app.drawing.RectangleUtils;
import kurtome.etch.app.util.RectangleDimensions;

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

    public double getEtchAspectRatio() {
        return RectangleUtils.calculateAspectRatio(mEtchOverlayItem.getEtchSize());
    }


}
