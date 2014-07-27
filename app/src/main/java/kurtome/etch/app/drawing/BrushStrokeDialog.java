package kurtome.etch.app.drawing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import kurtome.etch.app.R;

public class BrushStrokeDialog extends AlertDialog {

    private static final int MIN_STROKE_WIDTH = 3;
    private static final int MAX_STROKE_WIDTH = 30;

    private SeekBar mStrokeWidthSeek;
    private TextView mStrokeWidthText;
    private DrawingBrush mDrawingBrush;

    public BrushStrokeDialog(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.brush_stroke_picker, null);
        setView(layout);

        mStrokeWidthText = (TextView) layout.findViewById(R.id.stroke_width_txt);

        mStrokeWidthSeek = (SeekBar) layout.findViewById(R.id.stroke_width_seek);
        mStrokeWidthSeek.setMax(100); // to use as a percent
        mStrokeWidthSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setStrokeWidth(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }


    private void setStrokeWidth(int percent) {
        int newStrokeWidth = calcStrokeWidth(percent);
        mStrokeWidthText.setText(String.format("%spx", newStrokeWidth));
        mDrawingBrush.setStrokeWidth(newStrokeWidth);
    }

    private int calcStrokeWidth(int percent) {
        double d = percent / 100.0;
        Long tempWidth = Math.round(MAX_STROKE_WIDTH * d);

        if (tempWidth < MIN_STROKE_WIDTH) {
            return MIN_STROKE_WIDTH;
        }
        else if (tempWidth > MAX_STROKE_WIDTH) {
            return MAX_STROKE_WIDTH;
        }
        else {
            return tempWidth.intValue();
        }
    }

    public void setDrawingBrush(DrawingBrush drawingBrush) {
        mDrawingBrush = drawingBrush;
    }
}
