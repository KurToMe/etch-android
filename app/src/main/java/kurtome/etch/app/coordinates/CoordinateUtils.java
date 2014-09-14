package kurtome.etch.app.coordinates;

import android.location.Location;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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

    public static int toE6(double d) {
        return (int) Math.round(d * Math.pow(10, 6));
    }

    public static double fromE6(int i) {
        return ((double) i)  / Math.pow(10, 6);
    }

    public static int roundToMinIncrement(double part) {
        return roundToMinIncrement(toE6(part));
    }

    public static double addEast(double longitude, double degreesEast) {
        return longitude + degreesEast;
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

    public static LatLng incrementEast(LatLng coordinates, int times) {
        LatLng newGeoPoint = new LatLng(
                coordinates.latitude,
                fromE6(addEastE6(toE6(coordinates.longitude), (MIN_INCREMENT * times)))
        );
        return newGeoPoint;
    }

    public static LatLng moveEast(LatLng geo, double degrees) {
        return new LatLng(
                geo.latitude,
                addEast(geo.longitude, degrees)
        );
    }

//    public static LatLng incrementEast(LatLng coordinates, int times) {
//        LatLng newGeoPoint = new LatLng(
//                coordinates.latitude,
//                addEastE6(coordinates.longitude, (MIN_INCREMENT * times))
//        );
//        return newGeoPoint;
//    }


    public static double addSouth(double latitude, double degreesSouth) {
        return latitude - degreesSouth;
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

    public static LatLng incrementSouth(LatLng coordinates, int times) {
        LatLng newGeoPoint = new LatLng(
                fromE6(addSouthE6(toE6(coordinates.latitude), (MIN_INCREMENT * times))),
                coordinates.longitude
        );
        return newGeoPoint;
    }

    public static GeoPoint roundToMinIncrementTowardNorthWest(GeoPoint point) {
        GeoPoint latLng = new GeoPoint(
                roundToMinIncrement(point.getLatitudeE6()),
                roundToMinIncrement(point.getLongitudeE6())
        );
        if (isEast(point, latLng)) {
            latLng = incrementEast(latLng, -1);
        }
        if (isSouth(point, latLng)) {
            latLng = incrementSouth(latLng, -1);
        }
        return latLng;
    }

    public static LatLng roundToMinIncrementTowardNorthWest(LatLng point) {
        LatLng latLng = new LatLng(
                fromE6(roundToMinIncrement(point.latitude)),
                fromE6(roundToMinIncrement(point.longitude))
        );
        if (isEast(point, latLng)) {
            latLng = incrementEast(latLng, -1);
        }
        if (isSouth(point, latLng)) {
            latLng = incrementSouth(latLng, -1);
        }
        return latLng;
    }

    private static boolean isSouth(LatLng origin, LatLng point) {
        return origin.latitude > point.latitude;
    }

    private static boolean isEast(LatLng origin, LatLng point) {
        return origin.longitude < point.longitude;
    }

    private static boolean isSouth(GeoPoint origin, GeoPoint point) {
        return origin.getLatitudeE6() > point.getLatitudeE6();
    }

    private static boolean isEast(GeoPoint origin, GeoPoint point) {
        return origin.getLongitudeE6() < point.getLongitudeE6();
    }

    public static Coordinates convert(GeoPoint point) {
        Coordinates coordinates = new Coordinates();
        coordinates.setLatitudeE6(point.getLatitudeE6());
        coordinates.setLongitudeE6(point.getLongitudeE6());
        return coordinates;
    }

    public static Coordinates convert(LatLng point) {
        Coordinates coordinates = new Coordinates();
        coordinates.setLatitudeE6(toE6(point.latitude));
        coordinates.setLongitudeE6(toE6(point.longitude));
        return coordinates;
    }

//    public static GeoPoint getNorthWestPointStillInSameMinIncrement(GeoPoint point) {
//        GeoPoint rounded = roundToMinIncrement(point);
//        int degreesE6 = MIN_INCREMENT / 2;
//        return new GeoPoint(
//                addNorthE6(rounded.getLatitudeE6(), degreesE6),
//                addWestE6(rounded.getLongitudeE6(), degreesE6)
//        );
//    }

    public static LatLng toLatLng(Location location) {
        return new LatLng(
                location.getLatitude(),
                location.getLongitude()
        );
    }

    public static LatLng northWestCorner(LatLngBounds latLngBounds) {
        return new LatLng(
                latLngBounds.northeast.latitude,
                latLngBounds.southwest.longitude
        );
    }

    public static double calculateLongitudeRatio(LatLngBounds numerator, LatLngBounds denom) {
        double numeratorLongitude = calculateLongitudeDegrees(numerator);
        double denomLongitude = calculateLongitudeDegrees(denom);
        return numeratorLongitude / denomLongitude;
    }

    public static double calculateDegreesEast(LatLng origin, LatLng point) {
        return point.longitude - origin.longitude;
    }

    public static double calculateLongitudeDegrees(LatLngBounds bounds) {
        return diff(bounds.southwest.longitude, bounds.northeast.longitude);
    }

    public static double calculateLatitudeDegrees(LatLngBounds bounds) {
        return diff(bounds.southwest.latitude, bounds.northeast.latitude);
    }

    private static double diff(double val1, double val2) {
        return Math.abs(Math.abs(val1) - Math.abs(val2));
    }

}
