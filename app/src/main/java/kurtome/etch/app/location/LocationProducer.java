package kurtome.etch.app.location;

import android.app.Activity;
import android.location.Location;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;
import kurtome.etch.app.ObjectGraphUtils;

import javax.inject.Inject;

public class LocationProducer {
    @Inject Bus mEventBus;
    @Inject LocationFetchManager mLocationFetchManager;

    private LocationUpdatedEvent mLastEvent;

    public LocationProducer(Activity activity) {
        ObjectGraphUtils.inject(activity, this);
        mEventBus.register(this);
    }

    public void refreshLocation() {
        FetchLocationCommand command = new FetchLocationCommand(new LocationFetchListener() {
            @Override
            public void onLocationAcquired(Location location) {
                LocationUpdatedEvent locationUpdatedEvent = new LocationUpdatedEvent();
                locationUpdatedEvent.setLocation(location);
                mLastEvent = locationUpdatedEvent;
                mEventBus.post(locationUpdatedEvent);
            }

            @Override
            public void onLocationFailed(String message, Location bestUnacceptableLocation) {
            }
        });
        mLocationFetchManager.fetchLocation(command);
    }

    @Produce
    public LocationUpdatedEvent produce() {
        return mLastEvent;
    }

    @Subscribe
    public void refreshLocationRequested(RefreshLocationRequest requestEvent) {
        refreshLocation();
    }

    public void onDestroy() {
        mEventBus.unregister(this);
    }
}
