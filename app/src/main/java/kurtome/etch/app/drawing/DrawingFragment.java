package kurtome.etch.app.drawing;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import kurtome.etch.app.ObjectGraphUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.colorpickerview.dialog.ColorPickerDialog;
import kurtome.etch.app.colorpickerview.view.ColorPickerView;
import kurtome.etch.app.domain.Coordinates;
import kurtome.etch.app.domain.Etch;
import kurtome.etch.app.domain.SaveEtchCommand;
import kurtome.etch.app.openstreetmap.EtchOverlayItem;
import kurtome.etch.app.openstreetmap.MapLocationSelectedEvent;
import kurtome.etch.app.robospice.GetEtchRequest;
import kurtome.etch.app.robospice.SaveEtchRequest;
import kurtome.etch.app.util.ViewUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class DrawingFragment extends Fragment {

    private static final Logger logger = LoggerFactory.getLogger(DrawingFragment.class);

    private DrawingView mDrawingView;
    private DrawingBrush mDrawingBrush;
    private ImageButton mColorButton;
    private ImageButton mBrushStrokeButton;
    private ImageButton mSaveEtchButton;
    private View mRootView;
    private TextView mLocationText;
    private Coordinates mCoordinates;
    private EtchOverlayItem mEtchOverlayItem;
    private RelativeLayout mLoadingLayout;
    private ImageView mLoadingAlertImage;
    private ProgressBar mLoadingProgress;

    @Inject SpiceManager spiceManager;
    @Inject Bus mEventBus;

    @Override
    public void onStart() {
        super.onStart();
        spiceManager.start(getActivity());
    }

    @Override
    public void onStop() {
        if (spiceManager.isStarted()) {
            spiceManager.shouldStop();
        }
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ObjectGraphUtils.inject(this);

        mRootView = inflater.inflate(R.layout.drawing_layout, container, false);

        mDrawingView = ViewUtils.subViewById(mRootView, R.id.drawing);
        mDrawingBrush = mDrawingView.getDrawingBrush();

        mLoadingLayout = ViewUtils.subViewById(mRootView, R.id.drawing_loader_overlay);
        mLoadingAlertImage = ViewUtils.subViewById(mRootView, R.id.drawing_loader_progress);
        mLoadingProgress = ViewUtils.subViewById(mRootView, R.id.drawing_loader_progress);

        mSaveEtchButton = ViewUtils.subViewById(mRootView, R.id.save_etch_btn);
        mSaveEtchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEtch();
            }
        });

        mBrushStrokeButton = ViewUtils.subViewById(mRootView, R.id.brush_stroke_btn);
        mBrushStrokeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBrushStrokePicker();
            }
        });

        mColorButton = ViewUtils.subViewById(mRootView, R.id.color_chooser_button);
        mColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorDialog();
            }
        });

        mLocationText = ViewUtils.subViewById(mRootView, R.id.location_txt);

        logger.debug("onCreateView {}", (spiceManager != null));

        // Do this last so everything is setup before handling events
        mEventBus.register(this);

        return mRootView;
    }

    private void showBrushStrokePicker() {
        BrushStrokeDialog brushStrokeDialog = new BrushStrokeDialog(getActivity());
        brushStrokeDialog.setDrawingBrush(mDrawingBrush);
        brushStrokeDialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mEventBus.unregister(this);
    }

    private void showColorDialog() {
        ColorPickerDialog dialog = new ColorPickerDialog(getActivity(), mDrawingBrush.getColor(), new ColorPickerView.OnColorChangedListener() {
            @Override
            public void onColorChanged(int newColor) {
                setColor(newColor);
            }
        });
        dialog.setAlphaSliderVisible(true);
        dialog.show();
    }

    private void setColor(int color) {
        mDrawingBrush.setColor(color);
    }


    private void showLoader() {
        mLoadingLayout.setVisibility(View.VISIBLE);
    }

    private void hideLoader() {
        mLoadingLayout.setVisibility(View.INVISIBLE);
    }

    private void setSaveEnabled(boolean enabled) {
        mSaveEtchButton.setEnabled(enabled);
        mSaveEtchButton.invalidate();
    }

    private void startLoading() {
        showLoader();
        setSaveEnabled(false);
    }

    private void endLoading() {
        hideLoader();
        setSaveEnabled(true);
    }

    private void saveEtch() {
        if (mCoordinates == null) {
            logger.debug("Unknown location, can't set etch.");
            return;
        }

        final byte[] image = mDrawingView.getCurrentImage();

        final SaveEtchCommand saveEtchCommand = new SaveEtchCommand();
        final Bitmap currentBitmap = mDrawingView.getCopyOfCurrentBitmap();
        saveEtchCommand.setCoordinates(mCoordinates);
        saveEtchCommand.setImageGzip(image);

        startLoading();

        spiceManager.execute(new SaveEtchRequest(saveEtchCommand), new RequestListener<Void>() {

            @Override
            public void onRequestFailure(SpiceException e) {
                logger.error("Error getting etch for location {}.", mCoordinates, e);
                endLoading();
            }

            @Override
            public void onRequestSuccess(Void v) {
                logger.debug("Saved etch {}.", saveEtchCommand);

                mEtchOverlayItem.scaleAndSetBitmap(currentBitmap);

                endLoading();
            }
        });
    }


    private String format(int e6CooridnatePart) {
        String s = String.valueOf(e6CooridnatePart);
        int decimalPlace = 2;
        if (s.startsWith("-")) {
            decimalPlace = 3;
        }
        return s.substring(0, decimalPlace) + "." + s.substring(decimalPlace);
    }


    @Subscribe
    public void mapLocationSelected(MapLocationSelectedEvent event) {
        mCoordinates = event.getCoordinates();
        mEtchOverlayItem = event.getEtchOverlayItem();
        String text = String.format("latitude: %s, longitude %s", format(mCoordinates.getLatitudeE6()), format(mCoordinates.getLongitudeE6()));
        mLocationText.setText(text);

        startLoading();
        spiceManager.execute(new GetEtchRequest(mCoordinates), new RequestListener<Etch>() {

            @Override
            public void onRequestFailure(SpiceException e) {
                logger.error("Error getting etch for location {}.", mCoordinates, e);
                endLoading();
            }

            @Override
            public void onRequestSuccess(Etch etch) {
                mDrawingView.setCurrentImage(etch.getGzipImage());
                endLoading();
            }
        });
    }
}
