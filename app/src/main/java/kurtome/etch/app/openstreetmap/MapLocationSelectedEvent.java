package kurtome.etch.app.openstreetmap;

import kurtome.etch.app.domain.Coordinates;

public class MapLocationSelectedEvent {
    private Coordinates mCoordinates;

    public Coordinates getCoordinates() {
        return mCoordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        mCoordinates = coordinates;
    }
}
