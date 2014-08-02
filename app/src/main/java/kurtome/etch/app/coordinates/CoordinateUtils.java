package kurtome.etch.app.coordinates;

import kurtome.etch.app.domain.Coordinates;
import org.osmdroid.util.GeoPoint;

import java.math.BigDecimal;

public class CoordinateUtils {


    /**
     * 4 decimal places is about 30 ft precision,
     *  which is about as accurate as most phones can hope for nowadays.
     *
     * 3 decimal places is about 300 ft precision
      */
//    private static final double DECIMAL_DIGITS = 4;

    // 9.123456E6 -> 9123456
    private static final int MIN_INCREMENT = 500;

    /**
     * This must match server-side truncating to ensure we always
     * get the same coordinates.
     */
    public static int roundToMinIncrement(int partE6) {
        double d = Integer.valueOf(partE6).doubleValue() / MIN_INCREMENT;
        int truncated = Long.valueOf(Math.round(d)).intValue();
        int result = truncated * MIN_INCREMENT;
        return result;
    }

    public static int addEastE6(int longitudeE6, int degreesEastE6) {
        return longitudeE6 + degreesEastE6;
    }

    public static int addWestE6(int longitudeE6, int degreesWestE6) {
        return longitudeE6 - degreesWestE6;
    }

    public static GeoPoint incrementEast(GeoPoint coordinates, int times) {
        GeoPoint newGeoPoint = new GeoPoint(
                coordinates.getLatitudeE6(),
                addEastE6(coordinates.getLongitudeE6(), (MIN_INCREMENT * times))
        );
        return newGeoPoint;
    }

    public static int addSouthE6(int latitudeE6, int degreesSouthE6) {
        return latitudeE6 - degreesSouthE6;
    }

    public static int addNorthE6(int latitudeE6, int degreesNorthE6) {
        return latitudeE6 + degreesNorthE6;
    }

    public static GeoPoint incrementSouth(GeoPoint coordinates, int times) {
        GeoPoint newGeoPoint = new GeoPoint(
                addSouthE6(coordinates.getLatitudeE6(), (MIN_INCREMENT * times)),
                coordinates.getLongitudeE6()
        );
        return newGeoPoint;
    }

    public static GeoPoint roundToMinIncrement(GeoPoint point) {
        return new GeoPoint(
                roundToMinIncrement(point.getLatitudeE6()),
                roundToMinIncrement(point.getLongitudeE6())
        );
    }

    public static Coordinates convert(GeoPoint point) {
        Coordinates coordinates = new Coordinates();
        coordinates.setLatitudeE6(point.getLatitudeE6());
        coordinates.setLongitudeE6(point.getLongitudeE6());
        return coordinates;
    }

    public static GeoPoint getNorthWestPointStillInSameMinIncrement(GeoPoint point) {
        GeoPoint rounded = roundToMinIncrement(point);
        int degreesE6 = MIN_INCREMENT / 2;
        return new GeoPoint(
                addNorthE6(rounded.getLatitudeE6(), degreesE6),
                addWestE6(rounded.getLongitudeE6(), degreesE6)
        );
    }
}
