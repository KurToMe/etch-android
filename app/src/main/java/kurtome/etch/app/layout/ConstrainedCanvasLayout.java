package kurtome.etch.app.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import kurtome.etch.app.drawing.DrawingView;

public class ConstrainedCanvasLayout extends LinearLayout {


    public ConstrainedCanvasLayout(Context context) {
        super(context);
    }

    public ConstrainedCanvasLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConstrainedCanvasLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int widthSpec = MeasureSpec.makeMeasureSpec(Math.min(widthSize, DrawingView.IMAGE_SIZE_PIXELS), MeasureSpec.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(Math.min(heightSize, DrawingView.IMAGE_SIZE_PIXELS), MeasureSpec.EXACTLY);
        super.onMeasure(widthSpec, heightSpec);
    }
}
