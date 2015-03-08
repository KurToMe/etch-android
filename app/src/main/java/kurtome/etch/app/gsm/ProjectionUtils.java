package kurtome.etch.app.gsm;

import android.graphics.Point;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import kurtome.etch.app.coordinates.CoordinateUtils;
import kurtome.etch.app.drawing.RectangleUtils;
import kurtome.etch.app.util.RectangleDimensions;

public class ProjectionUtils {
    private ProjectionUtils() {
        // don't instantiate me bro
    }

    public static double calcAspectRatio(Projection projection, LatLngBounds latLngBounds) {
        LatLng geo = CoordinateUtils.northWestCorner(latLngBounds);
        LatLng southGeo = latLngBounds.southwest;
        LatLng eastGeo = latLngBounds.northeast;

        Point originPoint = projection.toScreenLocation(geo);
        Point eastPoint = projection.toScreenLocation(eastGeo);
        Point southPoint = projection.toScreenLocation(southGeo);
//        int x = eastPoint.x - originPoint.x;
//        int y = southPoint.y - originPoint.y;
        int x = distance(originPoint, eastPoint);
        int y = distance(originPoint, southPoint);
        double aspectRatio = RectangleUtils.calculateAspectRatio(x, y);
        return aspectRatio;
    }

    private static int distance(Point p1, Point p2) {
        int distance = (int) Math.sqrt((p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y));
        return distance;
    }

    private static double slope(Point p1, Point p2) {
        double slope = ((p2.y - p1.y)*1.0) /  (p2.x - p1.x);
        return slope;
    }

    public static Point calcOrigin(Projection projection, LatLngBounds latLngBounds) {
        Point originPoint = projection.toScreenLocation(CoordinateUtils.northWestCorner(latLngBounds));
        return originPoint;
    }

    public static RectangleDimensions calcProjectedSize(Projection projection, LatLngBounds latLngBounds) {
        Point originPoint = projection.toScreenLocation(CoordinateUtils.northWestCorner(latLngBounds));
        Point eastPoint = projection.toScreenLocation(latLngBounds.northeast);
        Point southPoint = projection.toScreenLocation(latLngBounds.southwest);
        int width = distance(originPoint, eastPoint);
        int height = distance(originPoint, southPoint);
        return new RectangleDimensions(width, height);
    }

    public static boolean isComputableInCurrentView(Projection projection, LatLngBounds latLngBounds) {
        Point originPoint = projection.toScreenLocation(CoordinateUtils.northWestCorner(latLngBounds));
        Point eastPoint = projection.toScreenLocation(latLngBounds.northeast);
        Point southPoint = projection.toScreenLocation(latLngBounds.southwest);
        int width = distance(originPoint, eastPoint);
        int height = distance(originPoint, southPoint);

        if (width < 0) {
            return false;
        }
        if (height < 0) {
            return false;
        }
        if (width > height) {
            return false;
        }
        return true;
    }

    public static LatLngBounds extendWidthToCreateSquareBounds(Projection projection, LatLngBounds srcBounds) {
        RectangleDimensions projectedBoundsPx = calcProjectedSize(projection, srcBounds);
        if (projectedBoundsPx.width > projectedBoundsPx.height) {
            throw new IllegalStateException("Width must be less than height.");
        }

        double aspectRatio = RectangleUtils.calculateAspectRatio(projectedBoundsPx);
        double longitudeDegrees = CoordinateUtils.calculateLongitudeDegrees(srcBounds);
        LatLng origin = CoordinateUtils.northWestCorner(srcBounds);
        LatLng northeast = CoordinateUtils.moveEast(origin, (longitudeDegrees / aspectRatio));
        LatLng southwest = srcBounds.southwest;
        LatLngBounds bounds = new LatLngBounds(southwest, northeast);
        return bounds;
    }

}
