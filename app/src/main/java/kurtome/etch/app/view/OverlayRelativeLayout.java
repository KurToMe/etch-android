package kurtome.etch.app.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class OverlayRelativeLayout extends RelativeLayout {

    public OverlayRelativeLayout(Context context) {
        super(context);
    }

    public OverlayRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverlayRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getVisibility() == VISIBLE) {
            // eat events when overlaying
            return true;
        }
        else {
            return super.onTouchEvent(event);
        }

    }
}
