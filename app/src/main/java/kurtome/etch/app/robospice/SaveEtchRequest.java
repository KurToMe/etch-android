package kurtome.etch.app.robospice;

import android.location.Location;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.Json;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.GenericData;
import com.octo.android.robospice.request.SpiceRequest;
import com.octo.android.robospice.request.googlehttpclient.GoogleHttpClientSpiceRequest;
import kurtome.etch.app.domain.Etch;
import kurtome.etch.app.domain.SaveEtchCommand;

public class SaveEtchRequest extends GoogleHttpClientSpiceRequest<Void> {
    private SaveEtchCommand command;


    public SaveEtchRequest(SaveEtchCommand saveEtchCommand) {
        super(Void.class);
        this.command = saveEtchCommand;
    }

    @Override
    public Void loadDataFromNetwork() throws Exception {
        GenericUrl genericUrl = new GenericUrl(GetEtchRequest.BASE_URL);

        JsonHttpContent json = new JsonHttpContent(new JacksonFactory(), command);
        HttpRequest request = getHttpRequestFactory()
                .buildPostRequest(genericUrl, json);

        request.execute();

        return null;
    }
}
