package kurtome.etch.app.robospice;

import android.app.Application;
import com.octo.android.robospice.SpiceService;
import com.octo.android.robospice.persistence.CacheManager;
import com.octo.android.robospice.persistence.binary.InFileBitmapObjectPersister;
import com.octo.android.robospice.persistence.exception.CacheCreationException;
import com.octo.android.robospice.persistence.memory.LruCacheBitmapObjectPersister;
import com.octo.android.robospice.persistence.string.InFileStringObjectPersister;

public class EtchSpiceService extends SpiceService {

    @Override
    public CacheManager createCacheManager(Application application) throws CacheCreationException {
        CacheManager cacheManager = new CacheManager();

        InFileStringObjectPersister inFileStringObjectPersister = new InFileStringObjectPersister(application);
        InFileBitmapObjectPersister inFileBitmapObjectPersister = new InFileBitmapObjectPersister(application);

        cacheManager.addPersister(inFileStringObjectPersister);
        cacheManager.addPersister(inFileBitmapObjectPersister);

        return cacheManager;
    }

}
