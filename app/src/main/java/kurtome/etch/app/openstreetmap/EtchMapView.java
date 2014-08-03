package kurtome.etch.app.openstreetmap;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.views.MapView;

public class EtchMapView extends MapView {

    public EtchMapView(final Context context, final AttributeSet attrs) {
        super(context, 256, new DefaultResourceProxyImpl(context), null, null, attrs);
    }

//            @Override
//            public void scrollBy(int x, int y) {
//                // disable scrolling
//                // TODO - sometimes scrolling is still possible, need to do a better job preventing that
//                //super.scrollBy(x, y);
//            }

//            @Override
//            public boolean touchEvent(MotionEvent event) {
//                // disable scrolling
//                //return super.touchEvent(event);
//                boolean handledEvent = true;
//                return handledEvent;
//            }
}
