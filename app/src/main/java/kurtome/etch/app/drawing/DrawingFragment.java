package kurtome.etch.app.drawing;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import kurtome.etch.app.ObjectGraphUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.activity.MainActivity;
import kurtome.etch.app.colorpickerview.dialog.ColorPickerDialog;
import kurtome.etch.app.colorpickerview.view.ColorPickerView;
import kurtome.etch.app.domain.Coordinates;
import kurtome.etch.app.domain.Etch;
import kurtome.etch.app.domain.SaveEtchCommand;
import kurtome.etch.app.openstreetmap.EtchOverlayItem;
import kurtome.etch.app.openstreetmap.MapLocationSelectedEvent;
import kurtome.etch.app.robospice.GetEtchRequest;
import kurtome.etch.app.robospice.SaveEtchRequest;
import kurtome.etch.app.util.Obj;
import kurtome.etch.app.util.ViewUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class DrawingFragment extends Fragment {

    private static final Logger logger = LoggerFactory.getLogger(DrawingFragment.class);

    private DrawingView mDrawingView;
    private DrawingBrush mDrawingBrush;
    private View mRootView;
    private Coordinates mCoordinates;
    private EtchOverlayItem mEtchOverlayItem;
    private RelativeLayout mLoadingLayout;
    private ImageView mLoadingAlertImage;
    private ProgressBar mLoadingProgress;
    private MainActivity mMainActivity;

    @Inject SpiceManager spiceManager;
    @Inject Bus mEventBus;
    private boolean mReadyToSave;

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mMainActivity = Obj.cast(activity);

        int visibility = View.SYSTEM_UI_FLAG_LOW_PROFILE;
        mMainActivity.getWindow()
                .getDecorView()
                .setSystemUiVisibility(visibility);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.drawing, menu);
        menu.findItem(R.id.save_etch_action).setEnabled(mReadyToSave);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.save_etch_action) {
            saveEtch();
            return true;
        }

        if (item.getItemId() == R.id.color_chooser_action) {
            showColorDialog();
            return true;
        }

        if (item.getItemId() == R.id.brush_options_action) {
            showBrushStrokePicker();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ObjectGraphUtils.inject(this);

        mRootView = inflater.inflate(R.layout.drawing_layout, container, false);

        mDrawingView = ViewUtils.subViewById(mRootView, R.id.drawing);
        mDrawingBrush = mDrawingView.getDrawingBrush();

        mLoadingLayout = ViewUtils.subViewById(mRootView, R.id.drawing_loader_overlay);
        mLoadingAlertImage = ViewUtils.subViewById(mRootView, R.id.drawing_loader_alert_img);
        mLoadingProgress = ViewUtils.subViewById(mRootView, R.id.drawing_loader_progress);

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


    private void startLoading() {
        mLoadingProgress.setVisibility(View.VISIBLE);
        mLoadingAlertImage.setVisibility(View.INVISIBLE);
        showLoader();
        mReadyToSave = false;
        getActivity().invalidateOptionsMenu();
    }

    private void endLoading() {
        hideLoader();
        mReadyToSave = true;
        getActivity().invalidateOptionsMenu();
    }

    private void endLoadingWithError() {
        mLoadingProgress.setVisibility(View.INVISIBLE);
        mLoadingAlertImage.setVisibility(View.VISIBLE);
        mReadyToSave = true;
    }

    private void saveEtch() {
        if (!mReadyToSave) {
            return;
        }

        startLoading();

        if (mCoordinates == null) {
            logger.debug("Unknown location, can't set etch.");
            return;
        }

        final byte[] image = mDrawingView.getCurrentImage();

        final SaveEtchCommand saveEtchCommand = new SaveEtchCommand();
        final Bitmap currentBitmap = mDrawingView.getCopyOfCurrentBitmap();
        saveEtchCommand.setCoordinates(mCoordinates);
        saveEtchCommand.setImageGzip(image);


        spiceManager.execute(new SaveEtchRequest(saveEtchCommand), new RequestListener<Void>() {

            @Override
            public void onRequestFailure(SpiceException e) {
                logger.error("Error getting etch for location {}.", mCoordinates, e);
                endLoadingWithError();
            }

            @Override
            public void onRequestSuccess(Void v) {
                logger.debug("Saved etch {}.", saveEtchCommand);
                mEtchOverlayItem.scaleAndSetBitmap(currentBitmap);
                endLoading();
                mMainActivity.popToMap();
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

        startLoading();
        spiceManager.execute(new GetEtchRequest(mCoordinates), new RequestListener<Etch>() {

            @Override
            public void onRequestFailure(SpiceException e) {
                logger.error("Error getting etch for location {}.", mCoordinates, e);
                endLoadingWithError();
            }

            @Override
            public void onRequestSuccess(Etch etch) {
                mDrawingView.setCurrentImage(etch.getGzipImage());
                endLoading();
            }
        });
    }
}
