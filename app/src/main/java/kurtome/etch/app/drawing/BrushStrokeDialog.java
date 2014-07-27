package kurtome.etch.app.drawing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import kurtome.etch.app.R;

public class BrushStrokeDialog extends AlertDialog {

    private static final int MIN_STROKE_WIDTH = 3;
    private static final int MAX_STROKE_WIDTH = 30;

    private SeekBar mStrokeWidthSeek;
    private TextView mStrokeWidthText;
    private DrawingBrush mDrawingBrush;
    private Switch mStrokeModeButton;

    public BrushStrokeDialog(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layoutView = inflater.inflate(R.layout.brush_stroke_picker, null);
        setView(layoutView);

        mStrokeWidthText = (TextView) layoutView.findViewById(R.id.stroke_width_txt);

        mStrokeWidthSeek = (SeekBar) layoutView.findViewById(R.id.stroke_width_seek);
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

        mStrokeModeButton = (Switch) layoutView.findViewById(R.id.stroke_mode_btn);
        mStrokeModeButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setStrokeMode(isChecked);
            }
        });
    }

    private void setStrokeMode(boolean isNormal) {
        PorterDuff.Mode mode = PorterDuff.Mode.SRC_OVER;
        if (!isNormal) {
            // Replace
            mode = PorterDuff.Mode.SRC;
        }
        mDrawingBrush.setMode(mode);
    }


    private void setStrokeWidth(int percent) {
        int newStrokeWidth = calcStrokeWidth(percent);
        mStrokeWidthText.setText(String.format("%spx", newStrokeWidth));
        mDrawingBrush.setStrokeWidth(newStrokeWidth);


        mStrokeModeButton.setChecked(mDrawingBrush.getMode() == PorterDuff.Mode.SRC_OVER);
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

    private int calcProgress(float strokeWidth) {
        Integer progress = Math.round((strokeWidth / MAX_STROKE_WIDTH) * 100);
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
        int progress = calcProgress(mDrawingBrush.getPaint().getStrokeWidth());
        mStrokeWidthSeek.setProgress(progress);
    }
}
