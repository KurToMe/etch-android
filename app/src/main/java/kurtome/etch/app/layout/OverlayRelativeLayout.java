package kurtome.etch.app.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class OverlayRelativeLayout extends RelativeLayout {

    public OverlayRelativeLayout(Context context) {
        super(context);
    }

    public OverlayRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverlayRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        return super.onInterceptTouchEvent(ev);
        return true;
    }
}
