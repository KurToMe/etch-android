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

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import kurtome.etch.app.ObjectGraphUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.colorpickerview.event.ColorPickedEvent;
import kurtome.etch.app.colorpickerview.view.ColorPanelView;
import kurtome.etch.app.colorpickerview.view.ColorPickerView;
import kurtome.etch.app.colorpickerview.view.ColorPickerView.OnColorChangedListener;
import kurtome.etch.app.drawing.DrawingBrush;
import kurtome.etch.app.util.ViewUtils;

public class ColorPickerDialogFragment extends DialogFragment {

    private ColorPickerView mColorPicker;

    private ColorPanelView mOldColor;
    private ColorPanelView mNewColor;

    private ImageButton mAcceptColorButton;
    private ImageButton mDeclineColorButton;
    private ColorPickedEvent mColorPickedEvent;
    private DrawingBrush mDrawingBrush;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.dialog_color_picker, container);

        ObjectGraphUtils.inject(this);


        mColorPicker = (ColorPickerView) layout.findViewById(R.id.color_picker_view);
        mColorPicker.setAlphaSliderVisible(true);

        mOldColor = (ColorPanelView) layout.findViewById(R.id.color_panel_old);
        mNewColor = (ColorPanelView) layout.findViewById(R.id.color_panel_new);

        if (mDrawingBrush != null) {
            mOldColor.setColor(mDrawingBrush.getColor());
            mColorPicker.setColor(mDrawingBrush.getColor(), true);
        }

        mDeclineColorButton = ViewUtils.subViewById(layout, R.id.decline_color_btn);
        mDeclineColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                declineColor();
            }
        });

        mAcceptColorButton = ViewUtils.subViewById(layout, R.id.accept_color_btn);
        mAcceptColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptColor();
            }
        });

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


        getDialog().getWindow().requestFeature(STYLE_NO_TITLE);

        return layout;
    }

    private void declineColor() {
        dismiss();
    }

    private void acceptColor() {
        mDrawingBrush.setColor(mNewColor.getColor());
        dismiss();
    }

    private void colorChanged(int color) {
        mNewColor.setColor(color);
    }

    public void setDrawingBrush(DrawingBrush drawingBrush) {
        mDrawingBrush = drawingBrush;
    }
}
