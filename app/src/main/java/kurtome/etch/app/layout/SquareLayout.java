package kurtome.etch.app.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class SquareLayout extends LinearLayout {


    public SquareLayout(Context context) {
        super(context);
    }

    public SquareLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int size = computeSize(widthMode, widthSize, heightMode, heightSize);

        int finalMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        super.onMeasure(finalMeasureSpec, finalMeasureSpec);
    }

    private int computeSize(int widthMode, int widthSize, int heightMode, int heightSize) {
        if (widthMode == MeasureSpec.EXACTLY && widthSize > 0) {
            return widthSize;
        }
        else if (heightMode == MeasureSpec.EXACTLY && heightSize > 0) {
            return heightSize;
        }
        else {
            if (widthSize < heightSize) {
                return widthSize;
            }
            else {
                return heightSize;
            }
        }
    }
}
