package kurtome.etch.app.location;

import android.app.Activity;
import android.location.Location;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import kurtome.etch.app.ObjectGraphUtils;

import javax.inject.Inject;

public class LocationProducer {
    @Inject Bus mEventBus;

    private LocationHelper mLocationHelper;

    private LocationUpdatedEvent mLastEvent;

    public LocationProducer(Activity activity) {
        ObjectGraphUtils.inject(activity, this);

        mEventBus.register(this);

        mLocationHelper = new LocationHelper(activity);
        mLocationHelper.setAccuracy(100f);
    }

    public void refreshLocation() {
        long timeoutMs = 3 * 60 * 1000;
        mLocationHelper.fetchLocation(timeoutMs, LocationHelper.Accuracy.FINE, new LocationHelper.LocationResponse() {
            @Override
            public void onLocationAcquired(Location location) {
                LocationUpdatedEvent locationUpdatedEvent = new LocationUpdatedEvent();
                locationUpdatedEvent.setLocation(location);
                mLastEvent = locationUpdatedEvent;
                mEventBus.post(locationUpdatedEvent);

            }
        });
    }

    @Produce
    public LocationUpdatedEvent produce() {
        return mLastEvent;
    }
}
