package kurtome.etch.app.robospice;

import android.location.Location;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.jackson.JacksonFactory;
import com.octo.android.robospice.request.googlehttpclient.GoogleHttpClientSpiceRequest;
import com.octo.android.robospice.retry.DefaultRetryPolicy;
import kurtome.etch.app.domain.Coordinates;
import kurtome.etch.app.domain.Etch;
import org.apache.commons.io.IOUtils;

public class GetEtchRequest extends GoogleHttpClientSpiceRequest<Etch> {
    private final Coordinates mCoordinates;

    public static final String BASE_URL = "http://etch.herokuapp.com/json/etchE6";

    public GetEtchRequest(Coordinates coordinates) {
        super(Etch.class);
        mCoordinates = coordinates;
    }

    public static String getUrl(Coordinates coordinates) {
        String url = GetEtchRequest.BASE_URL + "/" + coordinates.getLatitudeE6() + "/" + coordinates.getLongitudeE6();
        return url;
    }

    @Override
    public Etch loadDataFromNetwork() throws Exception {
        GenericUrl genericUrl = new GenericUrl(getUrl(mCoordinates));

        HttpRequest request = getHttpRequestFactory()
                .buildGetRequest(genericUrl);

        HttpResponse response = request.execute();
        byte[] imageGzip = IOUtils.toByteArray(response.getContent());
        Etch etch = new Etch();
        etch.setGzipImage(imageGzip);
        return etch;
    }
}
