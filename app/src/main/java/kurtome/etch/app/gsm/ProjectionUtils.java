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
        int x = eastPoint.x - originPoint.x;
        int y = southPoint.y - originPoint.y;
        double aspectRatio = RectangleUtils.calculateAspectRatio(x, y);
        return aspectRatio;
    }

    public static RectangleDimensions calcProjectedSize(Projection projection, LatLngBounds latLngBounds) {
        Point originPoint = projection.toScreenLocation(CoordinateUtils.northWestCorner(latLngBounds));
        Point eastPoint = projection.toScreenLocation(latLngBounds.northeast);
        Point southPoint = projection.toScreenLocation(latLngBounds.southwest);
        int width = eastPoint.x - originPoint.x;
        int height = southPoint.y - originPoint.y;
        return new RectangleDimensions(width, height);
    }

    public static LatLngBounds extendWidthToCreateSquareBounds(Projection projection, LatLngBounds srcBounds) {
        RectangleDimensions projectedBoundsPx = calcProjectedSize(projection, srcBounds);
        if (projectedBoundsPx.width > projectedBoundsPx.height) {
            throw new IllegalStateException("Width must be less than height.");
        }

        LatLng latLngOrigin = CoordinateUtils.northWestCorner(srcBounds);

        int squareProjectedSize = projectedBoundsPx.height;
        RectangleDimensions dimens = new RectangleDimensions(
                squareProjectedSize,
                squareProjectedSize
        );

        LatLngBounds bounds = calcBoundsFromOriginWithProjectedDimensions(
                projection,
                latLngOrigin,
                dimens
        );
        return bounds;
    }

    public static LatLngBounds calcBoundsFromOriginWithProjectedDimensions(
            Projection projection,
            LatLng origin,
            RectangleDimensions projectedDimensions
    ) {
        Point projectedOrigin = projection.toScreenLocation(origin);
        Point projectedSoutheastCorner = new Point(
                projectedOrigin.x + projectedDimensions.width,
                projectedOrigin.y + projectedDimensions.height
        );

        LatLng latLngSoutheast = projection.fromScreenLocation(projectedSoutheastCorner);

        LatLng northeast = new LatLng(origin.latitude, latLngSoutheast.longitude);
        LatLng southwest = new LatLng(latLngSoutheast.latitude, origin.longitude);
        LatLngBounds bounds = new LatLngBounds(southwest, northeast);
        return bounds;
    }

}
