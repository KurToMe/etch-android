package kurtome.etch.app.coordinates;

import kurtome.etch.app.domain.Coordinates;
import org.osmdroid.util.GeoPoint;

import java.math.BigDecimal;

public class CoordinateUtils {


    /**
     * 4 decimal places is about 30 ft precision,
     *  which is about as accurate as most phones can hope for nowadays.
      */
    private static final double DECIMAL_DIGITS = 4;

    // 9.123456E6 -> 9123456
    private static final int MIN_INCREMENT = 100;

    /**
     * This must match server-side truncating to ensure we always
     * get the same coordinates.
     */
    public static int roundToMinIncrement(int partE6) {
        int truncated = Math.round(partE6 / MIN_INCREMENT);
        int result = truncated * MIN_INCREMENT;
        return result;
    }

    public static Coordinates incrementWest(Coordinates coordinates) {
        Coordinates newCoordinates = new Coordinates();
        coordinates.setLatitudeE6(coordinates.getLatitudeE6());
        coordinates.setLongitudeE6(coordinates.getLongitudeE6() - MIN_INCREMENT);
        return newCoordinates;
    }

    public static Coordinates incrementEast(Coordinates coordinates) {
        Coordinates newCoordinates = new Coordinates();
        coordinates.setLatitudeE6(coordinates.getLatitudeE6());
        coordinates.setLongitudeE6(coordinates.getLongitudeE6() + MIN_INCREMENT);
        return newCoordinates;
    }

    public static Coordinates incrementNorth(Coordinates coordinates) {
        Coordinates newCoordinates = new Coordinates();
        coordinates.setLatitudeE6(coordinates.getLatitudeE6() + MIN_INCREMENT);
        coordinates.setLongitudeE6(coordinates.getLongitudeE6());
        return newCoordinates;
    }

    public static Coordinates incrementSouth(Coordinates coordinates) {
        Coordinates newCoordinates = new Coordinates();
        coordinates.setLatitudeE6(coordinates.getLatitudeE6() - MIN_INCREMENT);
        coordinates.setLongitudeE6(coordinates.getLongitudeE6());
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

    public static GeoPoint roundToMinIncrement(GeoPoint point) {
        return new GeoPoint(
                roundToMinIncrement(point.getLatitudeE6()),
                roundToMinIncrement(point.getLongitudeE6())
        );
    }

    public static GeoPoint offset(GeoPoint point, int latOffset, int longOffset) {
        int latSign = point.getLatitudeE6() > 0 ? 1 : -1;
        int longSign = point.getLongitudeE6() > 0 ? 1 : -1;
        int latitudeE6 = point.getLatitudeE6() + (MIN_INCREMENT * latOffset * latSign);
        int longitudeE6 = point.getLongitudeE6() + (MIN_INCREMENT * longOffset * longSign);
        GeoPoint geoPoint = new GeoPoint(
                latitudeE6,
                longitudeE6
        );
        return geoPoint;
    }

    public static Coordinates convert(GeoPoint point) {
        Coordinates coordinates = new Coordinates();
        coordinates.setLatitudeE6(point.getLatitudeE6());
        coordinates.setLongitudeE6(point.getLongitudeE6());
        return coordinates;
    }
}
