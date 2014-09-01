package kurtome.etch.app.drawing;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import kurtome.etch.app.R;
import kurtome.etch.app.util.ViewUtils;

public class BrushStrokeDialog extends AlertDialog {

    private static final int MIN_STROKE_WIDTH = 1;
    private static final int MAX_STROKE_WIDTH = 100;

    private DrawingBrush mDrawingBrush;
    private View mLayoutView;

    private SeekBar mStrokeWidthSeek;
    private TextView mStrokeWidthText;
    private ImageButton mAcceptButton;
    private ImageButton mDeclineButton;


    private Spinner mBrushModeSpinner;
    private int mStrokeWidth;
    private BrushMode mBrushMode;

    public BrushStrokeDialog(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayoutView = inflater.inflate(R.layout.brush_stroke_picker, null);
        setView(mLayoutView);


        mStrokeWidthText = (TextView) mLayoutView.findViewById(R.id.stroke_width_txt);
        mStrokeWidthSeek = (SeekBar) mLayoutView.findViewById(R.id.stroke_width_seek);
        mStrokeWidthSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setStrokeWidthFromPrecent(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        mBrushModeSpinner = ViewUtils.subViewById(mLayoutView, R.id.brush_mode_spinner);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context,
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


        mAcceptButton = ViewUtils.subViewById(mLayoutView, R.id.accept_stroke_btn);
        mAcceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accept();
            }
        });

        mDeclineButton = ViewUtils.subViewById(mLayoutView, R.id.decline_stroke_btn);
        mDeclineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decline();
            }
        });
    }

    private void decline() {
        dismiss();
    }

    private void accept() {
        mDrawingBrush.setMode(mBrushMode.porterDuff);
        mDrawingBrush.setStrokeWidth(mStrokeWidth);
        dismiss();
    }


    private void setStrokeWidthFromPrecent(int percent) {
        int newStrokeWidth = calcFromPercent(percent, MIN_STROKE_WIDTH, MAX_STROKE_WIDTH);
        setStrokeWidth(newStrokeWidth);
    }

    private void setStrokeWidth(int width) {
        mStrokeWidthText.setText(String.format("%spx", width));
        mStrokeWidth = width;
    }

    private int calcFromPercent(int percent, int min, int max) {
        double d = percent / 100.0;
        Long tempWidth = Math.round(max * d);

        if (tempWidth < min) {
            return min;
        }
        else if (tempWidth > max) {
            return max;
        }
        else {
            return tempWidth.intValue();
        }
    }

    private int calcProgress(float value, int max) {
        Integer progress = Math.round((value / max) * 100);
        if (progress < 0) {
            return 0;
        }
        else if (progress > 100) {
            return 100;
        }
        return progress;
    }

    public void setDrawingBrush(DrawingBrush drawingBrush) {
        mDrawingBrush = drawingBrush;
        setStrokeWidth(Math.round(mDrawingBrush.getPaint().getStrokeWidth()));
        mStrokeWidthSeek.setProgress(calcProgress(mStrokeWidth, MAX_STROKE_WIDTH));

        BrushMode mode = BrushMode.fromPorterDuff(drawingBrush.getMode());
        setBrushMode(mode);
    }

    private void setBrushMode(BrushMode mode) {
        mBrushMode = mode;
        mBrushModeSpinner.setSelection(mode.displayPosition);
    }
}
