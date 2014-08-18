package kurtome.etch.app.location;

import android.location.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationFetchProcessor {

    private static final Logger logger = LoggerFactory.getLogger(LocationFetchProcessor.class);

    public Location getBetterLocation(Location location1, Location location2) {
        if (location1 == null) {
            return location2;
        }
        if (location2 == null) {
            return location1;
        }

        logger.debug("Comparing locations: [{}], [{}]", location1, location2);

        if (location1.getAccuracy() < location2.getAccuracy()) {
            return location1;
        }

        return location1;
    }

    public boolean isAcceptable(FetchLocationConfig config, Location location) {
        if (location.getTime() < config.getMaxAgeEpoch()) {
            return false;
        }

        if (location.getAccuracy() > config.getMinAccuracyMeters()) {
            return false;
        }

        return true;
    }

}
