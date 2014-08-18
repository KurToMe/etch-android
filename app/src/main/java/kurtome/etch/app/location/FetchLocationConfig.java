package kurtome.etch.app.location;

public class FetchLocationConfig {

    private final float mMinAccuracyMeters;
    private final long mMaxAgeMillis;
    private final long mTimeoutMillis;
    private final long mMinFetchOptimizationMillis;
    private final long mMaxAgeEpoch;

    public FetchLocationConfig(FetchLocationCommand command) {
        mMinAccuracyMeters = command.getMinAccuracyMeters();
        mMaxAgeMillis = command.getMaxAgeMillis();
        mTimeoutMillis = command.getTimeoutMillis();
        mMinFetchOptimizationMillis = command.getMinFetchOptimizationMillis();
        mMaxAgeEpoch = System.currentTimeMillis() - command.getMaxAgeMillis();
    }

    public float getMinAccuracyMeters() {
        return mMinAccuracyMeters;
    }

    public long getMaxAgeMillis() {
        return mMaxAgeMillis;
    }

    public long getMaxAgeEpoch() {
        return mMaxAgeEpoch;
    }

    public long getTimeoutMillis() {
        return mTimeoutMillis;
    }

    public long getFetchOptimizationPeriodMillis() {
        return mMinFetchOptimizationMillis;
    }

}
