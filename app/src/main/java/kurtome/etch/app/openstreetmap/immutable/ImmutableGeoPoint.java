package kurtome.etch.app.openstreetmap.immutable;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;

public class ImmutableGeoPoint implements IGeoPoint{

    private final GeoPoint point;

    public ImmutableGeoPoint (int latitudeE6, int longitudeE6) {
        this(new GeoPoint(latitudeE6, longitudeE6));
    }

    public ImmutableGeoPoint (double latitude, double longitude) {
        this(new GeoPoint(latitude, longitude));
    }

    public ImmutableGeoPoint (GeoPoint point) {
        this.point = point;
    }

    @Override
    public int getLatitudeE6() {
        return point.getLatitudeE6();
    }

    @Override
    public int getLongitudeE6() {
        return point.getLongitudeE6();
    }

    @Override
    public double getLatitude() {
        return point.getLatitude();
    }

    @Override
    public double getLongitude() {
        return point.getLongitude();
    }
}
