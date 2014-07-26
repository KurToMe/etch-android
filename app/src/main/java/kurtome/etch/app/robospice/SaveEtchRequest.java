package kurtome.etch.app.robospice;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.octo.android.robospice.request.googlehttpclient.GoogleHttpClientSpiceRequest;
import kurtome.etch.app.domain.Coordinates;
import kurtome.etch.app.domain.SaveEtchCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveEtchRequest extends GoogleHttpClientSpiceRequest<Void> {
    private static final Logger logger = LoggerFactory.getLogger(SaveEtchRequest.class);
    private SaveEtchCommand command;


    public SaveEtchRequest(SaveEtchCommand saveEtchCommand) {
        super(Void.class);
        this.command = saveEtchCommand;
    }

    @Override
    public Void loadDataFromNetwork() throws Exception {
        GenericUrl genericUrl = new GenericUrl(GetEtchRequest.getUrl(command.getCoordinates()));

        logger.i(
                "Saving etch to {},{} with content of length {}",
                command.getCoordinates().getLatitudeE6(),
                command.getCoordinates().getLongitudeE6(),
                command.getImageGzip().length
        );


        ByteArrayContent content = new ByteArrayContent("application/gzip", command.getImageGzip());
        HttpRequest request = getHttpRequestFactory()
                .buildPostRequest(genericUrl, content);

        request.execute();

        return null;
    }
}
