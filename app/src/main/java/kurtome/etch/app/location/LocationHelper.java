package kurtome.etch.app.location;


import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import java.util.List;


/**
 * @brief This class is used to fetch the user's current location using either
 * cached location (if available) or requests it using the
 * LocationListener if not
 * <p/>
 * Originally from: https://gist.github.com/scruffyfox/5813346
 */
public class LocationHelper {
    /**
     * Message ID: Used when a provider has been disabled
     */
    public static final int MESSAGE_PROVIDER_DISABLED = 0;
    /**
     * Message ID: Used when the search has timed out
     */
    public static final int MESSAGE_TIMEOUT = 1;
    /**
     * Message ID: Used when the user cancels the request
     */
    public static final int MESSAGE_FORCED_CANCEL = 2;

    private final Context mContext;
    private final LocationManager mLocationManager;
    private LocationResponse mCallback = null;
    private Handler mTimeoutHandler;
    private float mAccuracyMeters = 30.0f;

    private LocationListener createListener() {
        return new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    if (mCallback != null) {
                        handleLocationChanged(location);
                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }

            @Override
            public void onProviderEnabled(String provider) { }

            @Override
            public void onProviderDisabled(String provider) {
                List<String> providers = mLocationManager.getProviders(true);
                boolean allOn = false;

                for (int i = providers.size() - 1; i >= 0; i--) {
                    allOn |= Settings.Secure.isLocationProviderEnabled(mContext.getContentResolver(), providers.get(i));
                }

                if (!allOn) {
                    removeListeners();

                    if (mCallback != null) {
                        mCallback.onLocationFailed("All providers disabled", MESSAGE_PROVIDER_DISABLED);
                    }
                }
            }
        };
    }

    private void handleLocationChanged(Location location) {
        if (location == null) {
            return;
        }

        mCallback.onLocationChanged(location);

        long maxAge = System.currentTimeMillis() - (30 * 1000);
        Location vetted = vetLocation(null, maxAge, location);
        if (!hasAcquired && vetted != null) {
            removeListeners();
            mCallback.onLocationAcquired(vetted);
            hasAcquired = true;
        }
    }

    private LocationListener mListener;

    private final Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            removeListeners();

            if (mCallback != null) {
                mCallback.onLocationFailed("Timeout", MESSAGE_TIMEOUT);
                mCallback.onTimeout();
            }
        }
    };

    private void removeListeners() {
        if (mTimeoutHandler != null) {
            mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
            mTimeoutHandler = null;
        }

        if (mListener != null) {
            mLocationManager.removeUpdates(mListener);
            mListener = null;
        }
    }

    /**
     * Determines the accuracy of the fetch
     */
    public enum Accuracy {
        /**
         * Get the location as close to the real point as possible
         */
        FINE,

        /**
         * Get the location by any means
         */
        COARSE
    }

    /**
     * Default Constructor
     *
     * @param context The application/activity context to use
     */
    public LocationHelper(Context context) {
        mContext = context;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Sets the desired accuracy for the fetch
     *
     * @param accuracy The new accuracy
     */
    public void setAccuracy(float accuracy) {
        mAccuracyMeters = accuracy;
    }

    /**
     * Gets the current set desired accuracy for a fetch
     *
     * @return The accuracy in meters
     */
    public float getAccuracy() {
        return mAccuracyMeters;
    }

    /**
     * Fetches the location using Fine accuracy. Note: if the response returns
     * location fetch failed, use the helper to get the cached location, then
     * finally fail if that is null
     *
     * @param timeout  The time out for the request in MS
     * @param callback The callback for the request
     */
    public void fetchLocation(long timeout, LocationResponse callback) {
        fetchLocation(timeout, Accuracy.FINE, callback);
    }

    /**
     * Fetches the location
     *
     * @param timeout  The time out for the request in MS
     * @param accuracy The accuracy of the fetch
     * @param callback The callback for the request
     */
    public void fetchLocation(long timeout, Accuracy accuracy, LocationResponse callback) {
        resetEverythingThatMatters();

        mCallback = callback;
        mCallback.onRequest();

        // Try to get the cache location first
        Location userLocation = getCachedLocation();
        handleLocationChanged(userLocation);

        if (!hasAcquired) {
            if (timeout > 0) {
                mTimeoutHandler.postDelayed(mTimeoutRunnable, timeout);
            }

            try {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mListener);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            try {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mListener);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            try {
                mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, mListener);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
        }
    }

    private void resetEverythingThatMatters() {
        hasAcquired = false;
        removeListeners();
        mTimeoutHandler = new Handler();
        mListener = createListener();
    }

    /**
     * Gets the cached location
     *
     * @return The location, or null if one was not retrieved
     */
    public Location getCachedLocation() {
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;

        final long now = System.currentTimeMillis();
        final long maxAgeMillis = now - (30 * 1000);

        for (int i = providers.size() - 1; i >= 0; i--) {
            Location loc = mLocationManager.getLastKnownLocation(providers.get(i));
            Location vetted = vetLocation(bestLocation, maxAgeMillis, loc);
            if (vetted != null) {
                bestLocation = vetted;
            }
        }

        return bestLocation;
    }

    private Location vetLocation(Location bestLocation, long maxAgeMillis, Location loc) {
        if (loc == null) {
            return null;
        }

        if (loc.getTime() < maxAgeMillis) {
            return null;
        }

        if (loc.getAccuracy() > mAccuracyMeters) {
            return null;
        }

        if (bestLocation == null) {
            return loc;
        }

        if (loc.getAccuracy() < bestLocation.getAccuracy()) {
            return loc;
        }

        return null;
    }

    private boolean hasAcquired = false;

    /**
     * @brief The location response for the callback of the LocationHelper
     */
    public static abstract class LocationResponse {
        /**
         * Called when the request was initiated
         */
        public void onRequest() {
        }

        /**
         * Called when the location changes
         *
         * @param l The new location
         */
        public void onLocationChanged(Location l) {
        }

        /**
         * Called when the location was aquired
         *
         * @param l The location recieved
         */
        public abstract void onLocationAcquired(Location l);

        /**
         * Called when the request timed out
         */
        public void onTimeout() {
        }

        /**
         * Called when the request failed
         *
         * @param message   The message
         * @param messageId The ID of the message
         */
        public void onLocationFailed(String message, int messageId) {
        }
    }
}

