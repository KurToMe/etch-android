package kurtome.etch.app.location;

public class FetchLocationCommand {

    /**
     * Minimum accuracy to be accepted as successful.
     *
     * Note: This is considered secondary to maxAgeMillis
     */
    private float mMinAccuracyMeters = 100;

    /**
     * Numbers of millis before fetch start time to use as the
     * cutoff point for acceptance
     */
    private long mMaxAgeMillis = 2 * 60 * 1000;

    /**
     * How long after fetch starts to fail with timeout
     */
    private long mTimeoutMillis = 60 * 1000;

    /**
     * How long to keep looking for a more accurate location after
     * an acceptable location is found
     */
    private long mMinFetchOptimizationMillis = 3 * 1000;

    private final LocationFetchListener mCallback;

    public FetchLocationCommand(LocationFetchListener callback) {
        mCallback = callback;
    }

    public LocationFetchListener getCallback() {
        return mCallback;
    }

    public float getMinAccuracyMeters() {
        return mMinAccuracyMeters;
    }

    public void setMinAccuracyMeters(float minAccuracyMeters) {
        mMinAccuracyMeters = minAccuracyMeters;
    }

    public long getMaxAgeMillis() {
        return mMaxAgeMillis;
    }

    public void setMaxAgeMillis(long maxAgeMillis) {
        mMaxAgeMillis = maxAgeMillis;
    }

    public long getTimeoutMillis() {
        return mTimeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        mTimeoutMillis = timeoutMillis;
    }

    public long getMinFetchOptimizationMillis() {
        return mMinFetchOptimizationMillis;
    }

    public void setMinFetchOptimizationMillis(long minFetchOptimizationMillis) {
        mMinFetchOptimizationMillis = minFetchOptimizationMillis;
    }
}
