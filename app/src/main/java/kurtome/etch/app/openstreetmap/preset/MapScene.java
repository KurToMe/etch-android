package kurtome.etch.app.openstreetmap.preset;

import kurtome.etch.app.openstreetmap.immutable.ImmutableGeoPoint;
import org.osmdroid.api.IGeoPoint;

public interface MapScene {
    public static MapScene NORTH_AMERICA = new MapScene() {
        @Override
        public IGeoPoint getCenter() {
            return new ImmutableGeoPoint(65.21, -106.66);
        }

        @Override
        public int getZoomLevel() {
            return 4;
        }
    };


    IGeoPoint getCenter();
    int getZoomLevel();
}
