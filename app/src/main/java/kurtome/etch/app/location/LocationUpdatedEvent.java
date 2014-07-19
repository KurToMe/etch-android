package kurtome.etch.app.location;

import android.location.Location;

public class LocationUpdatedEvent {
    private Location mLocation;

    public Location getLocation() {
        return mLocation;
    }

    public void setLocation(Location location) {
        mLocation = location;
    }
}
