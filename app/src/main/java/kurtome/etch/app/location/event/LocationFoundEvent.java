package kurtome.etch.app.location.event;

import android.location.Location;
import com.google.common.base.Optional;

public class LocationFoundEvent {
    private Location mLocation;
    private Location mRoughLocation;
    private boolean mIsFinal;

    public Optional<Location> getLocation() {
        return Optional.fromNullable(mLocation);
    }

    public void setLocation(Location location) {
        mLocation = location;
    }

    public boolean isFinal() {
        return mIsFinal;
    }

    public void setFinal(boolean isFinal) {
        mIsFinal = isFinal;
    }

    public Optional<Location> getRoughLocation() {
        return Optional.fromNullable(mRoughLocation);
    }

    public void setRoughLocation(Location roughLocation) {
        mRoughLocation = roughLocation;
    }
}
