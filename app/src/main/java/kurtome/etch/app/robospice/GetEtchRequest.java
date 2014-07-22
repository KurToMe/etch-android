package kurtome.etch.app.robospice;

import android.location.Location;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.json.jackson.JacksonFactory;
import com.octo.android.robospice.request.googlehttpclient.GoogleHttpClientSpiceRequest;
import kurtome.etch.app.domain.Coordinates;
import kurtome.etch.app.domain.Etch;

public class GetEtchRequest extends GoogleHttpClientSpiceRequest<Etch> {
    private final Coordinates mCoordinates;

    public static final String BASE_URL = "http://etch.herokuapp.com/json/etch";

    public GetEtchRequest(Coordinates coordinates) {
        super(Etch.class);
        mCoordinates = coordinates;
    }


    @Override
    public Etch loadDataFromNetwork() throws Exception {
        GenericUrl genericUrl = new GenericUrl(BASE_URL);
        genericUrl.put("latitude", mCoordinates.getLatitude());
        genericUrl.put("longitude", mCoordinates.getLongitude());

        HttpRequest request = getHttpRequestFactory()
                .buildGetRequest(genericUrl);

        request.setParser( new JacksonFactory().createJsonObjectParser() );
        return request.execute().parseAs( getResultType() );
    }
}
