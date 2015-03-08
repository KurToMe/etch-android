/*
 * Copyright (C) 2010 Daniel Nilsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kurtome.etch.app.colorpickerview.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import kurtome.etch.app.R;
import kurtome.etch.app.colorpickerview.event.ColorPickedEvent;
import kurtome.etch.app.colorpickerview.view.ColorPanelView;
import kurtome.etch.app.colorpickerview.view.ColorPickerView;
import kurtome.etch.app.colorpickerview.view.ColorPickerView.OnColorChangedListener;
import kurtome.etch.app.drawing.DrawingBrush;

public class ColorPickerDialogView extends LinearLayout {

    private ColorPickerView mColorPicker;

    private ColorPanelView mOldColor;
    private ColorPanelView mNewColor;

    private ImageButton mAcceptColorButton;
    private ImageButton mDeclineColorButton;
    private ColorPickedEvent mColorPickedEvent;
    private DrawingBrush mDrawingBrush;
    private Runnable mDismissCallback;

    public ColorPickerDialogView(Context context) {
        super(context);
    }

    public ColorPickerDialogView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorPickerDialogView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setup(DrawingBrush drawingBrush) {
        mDrawingBrush = drawingBrush;
        View layout = this;

        mColorPicker = (ColorPickerView) layout.findViewById(R.id.color_picker_view);
        mColorPicker.setAlphaSliderVisible(true);

        mOldColor = (ColorPanelView) layout.findViewById(R.id.color_panel_old);
        mNewColor = (ColorPanelView) layout.findViewById(R.id.color_panel_new);

        if (mDrawingBrush != null) {
            mOldColor.setColor(mDrawingBrush.getColor());
            mNewColor.setColor(mDrawingBrush.getColor());
            mColorPicker.setColor(mDrawingBrush.getColor(), true);
        }

        LinearLayout parent = (LinearLayout) mOldColor.getParent();
        parent.setPadding(
                Math.round(mColorPicker.getDrawingOffset()),
                0,
                Math.round(mColorPicker.getDrawingOffset()),
                0
        );

        mColorPicker.setOnColorChangedListener(new OnColorChangedListener() {
            @Override
            public void onColorChanged(int newColor) {
                colorChanged(newColor);
            }
        });
    }

    private void colorChanged(int color) {
        mNewColor.setColor(color);
    }

    public int getNewColor() {
        return mNewColor.getColor();
    }
}
