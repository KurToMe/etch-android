package kurtome.etch.app.domain;

public class SaveEtchCommand {
    private byte[] mImageGzip;
    private Coordinates mCoordinates;

    public byte[] getImageGzip() {
        return mImageGzip;
    }

    public void setImageGzip(byte[] imageGzip) {
        mImageGzip = imageGzip;
    }

    public void setCoordinates(Coordinates coordinates) {
        mCoordinates = coordinates;
    }

    public Coordinates getCoordinates() {
        return mCoordinates;
    }
}
