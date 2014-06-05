package kurtome.etch.app.domain;

import com.google.api.client.util.Key;

public class Etch {
    @Key("base64Image")
    private String base64Image;

    @Key("latitude")
    private double latitude;

    @Key("longitude")
    private double longitude;

    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
