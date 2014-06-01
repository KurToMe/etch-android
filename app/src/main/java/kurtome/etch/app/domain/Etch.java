package kurtome.etch.app.domain;

import com.google.api.client.util.Key;

public class Etch {
    @Key("base64Image")
    private String _base64Image;

    @Key("latitude")
    private double _latitude;

    @Key("longitude")
    private double _longitude;

    public String getBase64Image() {
        return _base64Image;
    }

    public double getLatitude() {
        return _latitude;
    }

    public double getLongitude() {
        return _longitude;
    }
}
