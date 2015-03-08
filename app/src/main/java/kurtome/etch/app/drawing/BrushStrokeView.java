package kurtome.etch.app.drawing;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.View;
import android.widget.*;
import kurtome.etch.app.R;
import kurtome.etch.app.util.ViewUtils;
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

public class BrushStrokeView extends RelativeLayout {

    private static final int MIN_STROKE_WIDTH = 1;
    private static final int MAX_STROKE_WIDTH = 200;

    private DrawingBrush mDrawingBrush;
    private View mLayoutView;

    private DiscreteSeekBar mStrokeWidthSeek;
    private TextView mStrokeWidthText;
    private ImageButton mAcceptButton;
    private ImageButton mDeclineButton;


    private Spinner mBrushModeSpinner;
    private int mStrokeWidth;
    private BrushMode mBrushMode;

    public BrushStrokeView(Context context) {
        super(context);
    }

    public BrushStrokeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BrushStrokeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setup(DrawingBrush drawingBrush) {
        mDrawingBrush = drawingBrush;

        mLayoutView = this;


        mStrokeWidthText = ViewUtils.subViewById(mLayoutView, R.id.stroke_width_txt);
        mStrokeWidthSeek = ViewUtils.subViewById(mLayoutView, R.id.stroke_width_seek);
        mStrokeWidthSeek.setMin(MIN_STROKE_WIDTH);
        mStrokeWidthSeek.setMax(MAX_STROKE_WIDTH);
        mStrokeWidthSeek.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar discreteSeekBar, int value, boolean fromUser) {
                setStrokeWidthFrom(value);
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar discreteSeekBar) {
            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar discreteSeekBar) {
            }
        });

        mBrushModeSpinner = ViewUtils.subViewById(mLayoutView, R.id.brush_mode_spinner);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.brush_modes_array, // currently this and the enum BrushMode could get out of sync
                android.R.layout.simple_spinner_item
        );
        mBrushModeSpinner.setAdapter(adapter);
        mBrushModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                BrushMode mode = BrushMode.fromDisplayPosition(position);
                setBrushMode(mode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setStrokeWidth(Math.round(mDrawingBrush.getPaint().getStrokeWidth()));
        mStrokeWidthSeek.setProgress(mStrokeWidth);

        BrushMode mode = BrushMode.fromPorterDuff(mDrawingBrush.getMode());
        setBrushMode(mode);
    }

    private void setStrokeWidthFrom(int value) {
        setStrokeWidth(value);
    }

    private void setStrokeWidth(int width) {
        mStrokeWidthText.setText(String.format("%spx", width));
        mStrokeWidth = width;
    }

    private void setBrushMode(BrushMode mode) {
        mBrushMode = mode;
        mBrushModeSpinner.setSelection(mode.displayPosition);
    }

    public PorterDuff.Mode getPorterDuffMode() {
        return mBrushMode.porterDuff;
    }

    public int getStrokeWidth() {
        return mStrokeWidth;
    }
}
