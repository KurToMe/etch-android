package kurtome.etch.app.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

/**
 * Stateful object for orchestrating a single location fetch request.
 *
 * Can be configured before initiating the fetch.
 */
public class LocationFetcher {

    private static final Logger logger = LoggerFactory.getLogger(LocationFetcher.class);

    @Inject LocationManager mLocationManager;
    @Inject LocationFetchProcessor mProcessor;
    @Inject Context mContext;

    private final Handler mHandler;
    private final LocationFetchListener mCallback;
    private final FetchLocationConfig mConfig;

    private boolean mFetchStarted = false;
    private boolean mFinished = false;
    private Location mBestAcceptableLocation;
    private Location mBestOverallLocation;

    private final Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            finish("Timeout.");
        }
    };

    private final Runnable mOptimizeFinishedRunnable = new Runnable() {
        @Override
        public void run() {
            finish("Optimization period over.");
        }
    };

    private void finish(String message) {
        mFinished = true;
        removeListeners();
        if (mBestAcceptableLocation != null) {
            mCallback.onLocationAcquired(mBestAcceptableLocation);
        }
        else {
            mCallback.onLocationFailed(message, mBestOverallLocation);
        }
    }

    private final LocationListener mListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            processLocation(location);
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

                mCallback.onLocationFailed("All providers disabled", mBestOverallLocation);
            }
        }
    };

    public LocationFetcher(FetchLocationConfig config, LocationFetchListener callback) {
        mConfig = config;
        mHandler = new Handler();
        mCallback = callback;
    }

    public void fetch() {
        if (mFetchStarted) {
            throw new IllegalStateException("This can only be used to fetch once.");
        }
        mFetchStarted = true;

        processCachedLocations();

        if (!mFinished) {
            if (mConfig.getTimeoutMillis() > 0) {
                mHandler.postDelayed(mTimeoutRunnable, mConfig.getTimeoutMillis());
            }

            tryRequestUpdatesFromProvider(LocationManager.GPS_PROVIDER);
            tryRequestUpdatesFromProvider(LocationManager.NETWORK_PROVIDER);
            tryRequestUpdatesFromProvider(LocationManager.PASSIVE_PROVIDER);
        }
    }

    private void tryRequestUpdatesFromProvider(String providerName) {
        try {
            mLocationManager.requestLocationUpdates(providerName, 0, 0, mListener);
        }
        catch (Exception e) {
            logger.error("Error requesting updates from location provider '{}'", e);
        }
    }

    private void processLocation(Location location) {
        if (location == null) {
            return;
        }

        mBestOverallLocation = mProcessor.getBetterLocation(mBestOverallLocation, location);
        if (mProcessor.isAcceptable(mConfig, location)) {
            if (mBestAcceptableLocation == null) {
                logger.debug("Found first acceptable location: {}", location);
                mHandler.postDelayed(mOptimizeFinishedRunnable, mConfig.getFetchOptimizationPeriodMillis());
            }
            else {
                logger.debug("Found acceptable location: {}, comparing to {}", location, mBestAcceptableLocation);
            }

            mBestAcceptableLocation = mProcessor.getBetterLocation(mBestAcceptableLocation, location);
        }
    }

    public void processCachedLocations() {
        List<String> providers = mLocationManager.getProviders(true);

        for (String provider : providers) {
            Location loc = mLocationManager.getLastKnownLocation(provider);
            processLocation(loc);
        }
    }

    private void removeListeners() {
        mHandler.removeCallbacks(mTimeoutRunnable);
        mHandler.removeCallbacks(mOptimizeFinishedRunnable);
        mLocationManager.removeUpdates(mListener);
    }
}
