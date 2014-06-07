package kurtome.etch.app.robospice;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson.JacksonFactory;
import com.octo.android.robospice.request.googlehttpclient.GoogleHttpClientSpiceRequest;
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
