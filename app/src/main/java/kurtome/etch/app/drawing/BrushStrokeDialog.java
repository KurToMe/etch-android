package kurtome.etch.app.drawing;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import kurtome.etch.app.R;

public class BrushStrokeDialog extends AlertDialog {

    private static final int MIN_STROKE_WIDTH = 1;
    private static final int MAX_STROKE_WIDTH = 80;
    public static final int MAX_OPACITY = 255;

    private DrawingBrush mDrawingBrush;
    private View mLayoutView;

    private SeekBar mStrokeWidthSeek;
    private TextView mStrokeWidthText;

    private SeekBar mStrokeOpacitySeek;
    private TextView mStrokeOpacityText;

    private RadioButton mStrokeNormalModeButton;
    private RadioButton mStrokeReplaceModeButton;
    private RadioButton mStrokeUnderModeButton;

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
                setStrokeWidth(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        mStrokeOpacityText = (TextView) mLayoutView.findViewById(R.id.stroke_opacity_txt);
        mStrokeOpacitySeek = (SeekBar) mLayoutView.findViewById(R.id.stroke_opacity_seek);
        mStrokeOpacitySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setStrokeOpacity(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        mStrokeNormalModeButton = (RadioButton) mLayoutView.findViewById(R.id.brush_mode_normal_radio);
        mStrokeNormalModeButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    setStrokeModeNormal();
                }
            }
        });

        mStrokeReplaceModeButton = (RadioButton) mLayoutView.findViewById(R.id.brush_mode_replace_radio);
        mStrokeReplaceModeButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    setStrokeModeReplace();
                }
            }
        });

        mStrokeUnderModeButton = (RadioButton) mLayoutView.findViewById(R.id.brush_mode_under_radio);
        mStrokeUnderModeButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    setStrokeModeUnder();
                }
            }
        });
    }

    private void setStrokeOpacity(int percent) {
        int newStrokeOpacity = calcFromPercent(percent, 0, MAX_OPACITY);
        mStrokeOpacityText.setText(String.format("%s%%", percent));
        mDrawingBrush.setAlpha(newStrokeOpacity);
    }

    private void setStrokeModeNormal() {
        mDrawingBrush.setMode(PorterDuff.Mode.SRC_OVER);
    }

    private void setStrokeModeReplace() {
        mDrawingBrush.setMode(PorterDuff.Mode.SRC);
    }

    private void setStrokeModeUnder() {
        mDrawingBrush.setMode(PorterDuff.Mode.DST_OVER);
    }


    private void setStrokeWidth(int percent) {
        int newStrokeWidth = calcFromPercent(percent, MIN_STROKE_WIDTH, MAX_STROKE_WIDTH);
        mStrokeWidthText.setText(String.format("%spx", newStrokeWidth));
        mDrawingBrush.setStrokeWidth(newStrokeWidth);
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

    private int calcProgress(float strokeWidth, int max) {
        Integer progress = Math.round((strokeWidth / max) * 100);
        if (progress < 0) {
            return 0;
        }
        else if (progress > 100) {
            return 100;
        }
        return progress;
    }

    private void check(RadioButton button) {
        if (!button.isChecked()) {
            button.setChecked(true);
        }
    }

    public void setDrawingBrush(DrawingBrush drawingBrush) {
        mDrawingBrush = drawingBrush;
        int progress = calcProgress(mDrawingBrush.getPaint().getStrokeWidth(), MAX_STROKE_WIDTH);
        mStrokeWidthSeek.setProgress(progress);

        int opacityProgress = calcProgress(mDrawingBrush.getAlpha(), MAX_OPACITY);
        mStrokeOpacitySeek.setProgress(opacityProgress);

//        mStrokeNormalModeButton.setChecked(false);
//        mStrokeReplaceModeButton.setChecked(false);
//        mStrokeUnderModeButton.setChecked(false);
        if (mDrawingBrush.getMode() == PorterDuff.Mode.SRC_OVER) {
            check(mStrokeNormalModeButton);
        }
        else if (mDrawingBrush.getMode() == PorterDuff.Mode.SRC) {
            mStrokeReplaceModeButton.setChecked(true);
        }
        else if (mDrawingBrush.getMode() == PorterDuff.Mode.DST_OVER) {
            mStrokeUnderModeButton.setChecked(true);
        }
    }
}
