package kurtome.etch.app.location;

import android.location.Location;

public interface LocationFetchListener {

    /**
     * Called when successfully found an acceptable location was acquired
     */
    public abstract void onLocationAcquired(Location location);

    /**
     * Called when unable to find acceptable location
     */
    public void onLocationFailed(String message, Location bestUnacceptableLocation);
}
