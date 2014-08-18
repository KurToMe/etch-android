package kurtome.etch.app.location;


import android.content.Context;
import android.location.LocationManager;
import kurtome.etch.app.ObjectGraphUtils;

import javax.inject.Inject;

public class LocationFetchManager {

    @Inject LocationManager mLocationManager;
    @Inject LocationFetchProcessor mProcessor;
    @Inject Context mContext;

    public void fetchLocation(FetchLocationCommand command) {
        FetchLocationConfig config = new FetchLocationConfig(command);
        LocationFetcher fetcher = new LocationFetcher(config, command.getCallback());

        ObjectGraphUtils.inject(mContext, fetcher);

        fetcher.fetch();
    }

}

