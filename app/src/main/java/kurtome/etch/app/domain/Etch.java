package kurtome.etch.app.domain;

import com.google.api.client.util.Key;

public class Etch {
    private byte[] gzipImage;

    public byte[] getGzipImage() {
        return gzipImage;
    }

    public void setGzipImage(byte[] gzipImage) {
        this.gzipImage = gzipImage;
    }
}
