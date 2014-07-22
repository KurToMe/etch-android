package kurtome.etch.app.coordinates;

import kurtome.etch.app.domain.Coordinates;
import org.osmdroid.util.GeoPoint;

public class CoordinateUtils {


    /**
     * 4 decimal places is about 30 ft precision,
     *  which is about as accurate as most phones can hope for nowadays.
      */
    private static final double DECIMAL_DIGITS = 4;

    private static final double MIN_INCREMENT = 1.0 * (Math.pow(10, -DECIMAL_DIGITS));

    /**
     * This must match server-side truncating to ensure we always
     * get the same coordinates.
     */
    public static double truncate(double d) {
        double multiplier = Math.pow(10, DECIMAL_DIGITS);
        return Math.floor(d * multiplier) / multiplier;
    }

    public static Coordinates incrementWest(Coordinates coordinates) {
        Coordinates newCoordinates = new Coordinates();
        coordinates.setLatitude(coordinates.getLatitude());
        coordinates.setLongitude(coordinates.getLongitude() - MIN_INCREMENT);
        return newCoordinates;
    }

    public static Coordinates incrementEast(Coordinates coordinates) {
        Coordinates newCoordinates = new Coordinates();
        coordinates.setLatitude(coordinates.getLatitude());
        coordinates.setLongitude(coordinates.getLongitude() + MIN_INCREMENT);
        return newCoordinates;
    }

    public static Coordinates incrementNorth(Coordinates coordinates) {
        Coordinates newCoordinates = new Coordinates();
        coordinates.setLatitude(coordinates.getLatitude() + MIN_INCREMENT);
        coordinates.setLongitude(coordinates.getLongitude());
        return newCoordinates;
    }

    public static Coordinates incrementSouth(Coordinates coordinates) {
        Coordinates newCoordinates = new Coordinates();
        coordinates.setLatitude(coordinates.getLatitude() - MIN_INCREMENT);
        coordinates.setLongitude(coordinates.getLongitude());
        return newCoordinates;
    }

    public static GeoPoint incrementWest(GeoPoint coordinates) {
        GeoPoint newGeoPoint = new GeoPoint(
                coordinates.getLatitude(),
                coordinates.getLongitude() - MIN_INCREMENT
        );
        return newGeoPoint;
    }

    public static GeoPoint incrementEast(GeoPoint coordinates) {
        GeoPoint newGeoPoint = new GeoPoint(
                coordinates.getLatitude(),
                coordinates.getLongitude() + MIN_INCREMENT
        );
        return newGeoPoint;
    }

    public static GeoPoint incrementNorth(GeoPoint coordinates) {
        GeoPoint newGeoPoint = new GeoPoint(
                coordinates.getLatitude() + MIN_INCREMENT,
                coordinates.getLongitude()
        );
        return newGeoPoint;
    }

    public static GeoPoint incrementSouth(GeoPoint coordinates) {
        GeoPoint newGeoPoint = new GeoPoint(
                coordinates.getLatitude() - MIN_INCREMENT,
                coordinates.getLongitude()
        );
        return newGeoPoint;
    }

    public static GeoPoint truncate(GeoPoint point) {
        return new GeoPoint(
                truncate(point.getLatitude()),
                truncate(point.getLongitude())
        );
    }

    public static GeoPoint offset(GeoPoint point, int latOffset, int longOffset) {
        return new GeoPoint(
                point.getLatitude() + (MIN_INCREMENT * latOffset),
                point.getLongitude() + (MIN_INCREMENT * longOffset)
        );
    }

    public static Coordinates convert(GeoPoint point) {
        Coordinates coordinates = new Coordinates();
        coordinates.setLatitude(point.getLatitude());
        coordinates.setLongitude(point.getLongitude());
        return coordinates;
    }
}
