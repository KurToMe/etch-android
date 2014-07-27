package kurtome.etch.app;

import com.google.common.base.Optional;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class GzipUtils {

    private static final Logger logger = LoggerManager.getLogger();

    public static Optional<byte[]> unzip(byte[] rawBytes) {
        GZIPInputStream stream = null;
        try {
            stream = new GZIPInputStream(new ByteArrayInputStream(rawBytes));
            byte[] bytes = IOUtils.toByteArray(stream);
            return Optional.of(bytes);
        }
        catch (IOException e) {
            logger.e("Unable to unzip bytes",e);
            return Optional.absent();
        }
    }

}
