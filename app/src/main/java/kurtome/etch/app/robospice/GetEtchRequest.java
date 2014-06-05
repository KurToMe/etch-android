package kurtome.etch.app.robospice;

import android.location.Location;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.json.jackson.JacksonFactory;
import com.octo.android.robospice.request.googlehttpclient.GoogleHttpClientSpiceRequest;
import kurtome.etch.app.domain.Etch;

public class GetEtchRequest extends GoogleHttpClientSpiceRequest<Etch> {
    private final Location location;

    public static final String BASE_URL = "http://etch.herokuapp.com/json/etch";

    public GetEtchRequest(Location location) {
        super(Etch.class);
        this.location = location;
    }


    @Override
    public Etch loadDataFromNetwork() throws Exception {
        GenericUrl genericUrl = new GenericUrl(BASE_URL);
        genericUrl.put("latitude", location.getLatitude());
        genericUrl.put("longitude", location.getLongitude());

        HttpRequest request = getHttpRequestFactory()
                .buildGetRequest(genericUrl);

        request.setParser( new JacksonFactory().createJsonObjectParser() );
        return request.execute().parseAs( getResultType() );
    }
}
