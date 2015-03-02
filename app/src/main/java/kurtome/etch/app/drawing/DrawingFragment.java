package kurtome.etch.app.drawing;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.*;
import android.widget.*;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import kurtome.etch.app.ObjectGraphUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.activity.MainActivity;
import kurtome.etch.app.colorpickerview.dialog.ColorPickerDialogFragment;
import kurtome.etch.app.domain.Coordinates;
import kurtome.etch.app.domain.Etch;
import kurtome.etch.app.domain.SaveEtchCommand;
import kurtome.etch.app.gsm.EtchOverlayImage;
import kurtome.etch.app.gsm.MapLocationSelectedEvent;
import kurtome.etch.app.gsm.MapModeChangedEvent;
import kurtome.etch.app.robospice.GetEtchRequest;
import kurtome.etch.app.robospice.SaveEtchRequest;
import kurtome.etch.app.util.ObjUtils;
import kurtome.etch.app.util.ViewUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class DrawingFragment extends Fragment {

    private static final Logger logger = LoggerFactory.getLogger(DrawingFragment.class);
    public static final String BRUSH_STROKE_WIDTH = "brush-stroke-width";
    public static final String BRUSH_COLOR = "brush-color";

    private DrawingView mDrawingView;
    private DrawingBrush mDrawingBrush;
    private Coordinates mCoordinates;
    private EtchOverlayImage mEtchOverlayItem;
    private RelativeLayout mLoadingLayout;
    private ImageView mLoadingAlertImage;
    private ProgressBar mLoadingProgress;
    private MainActivity mMainActivity;
    private ImageButton mStrokeOptionButton;
    private ImageButton mUndoButton;
    private PaintSwatchButton mColorSwatchButton;

    private static final String COLOR_PICKER_FRAGMENT_TAG = "COLOR_PICKER_FRAGMENT_TAG";

    @Inject SpiceManager spiceManager;
    @Inject Bus mEventBus;

    private boolean mReadyToSave;
    private ActionBar mActionBar;
    private MapModeChangedEvent.Mode mMapMode = MapModeChangedEvent.Mode.MAP;

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

        mMainActivity = ObjUtils.cast(activity);

//        mMainActivity.getWindow().getDecorView().setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LOW_PROFILE
//        );
        mActionBar = mMainActivity.getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMainActivity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_VISIBLE
        );
        mActionBar.setDisplayHomeAsUpEnabled(false);
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

        return super.onOptionsItemSelected(item);
    }

    public void setEtchData(MapLocationSelectedEvent event) {
        if (mEtchOverlayItem != null) {
            throw new RuntimeException("Can only set etch data once.");
        }

        mCoordinates = event.getCoordinates();
        mEtchOverlayItem = event.getEtchOverlayItem();
    }

    @Subscribe
    public void handleMapModeChanged(MapModeChangedEvent event) {
        mMapMode = event.mode;

        if (mMapMode == MapModeChangedEvent.Mode.DRAWING) {
            mEtchOverlayItem.getAspectRatio();
        }
        updateVisibility();
    }

    private void updateVisibility() {
        // Should be invisible until the map finishes transitioning completely
        if (this.getView() != null) {
            if (mMapMode == MapModeChangedEvent.Mode.DRAWING) {
                mEtchOverlayItem.hideFromMap();
                mDrawingView.onMapFinishedChanging();
                this.getView().setVisibility(View.VISIBLE);
            }
            else {
                this.getView().setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ObjectGraphUtils.inject(this);
        mEventBus.register(this);

        if (mEtchOverlayItem == null) {
            throw new RuntimeException("Can't setup without etch data.");
        }

        View rootView = inflater.inflate(R.layout.drawing_layout, container, false);
        rootView.setVisibility(View.INVISIBLE);

        mDrawingView = ViewUtils.subViewById(rootView, R.id.drawing);
        mDrawingBrush = mDrawingView.getDrawingBrush();
        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        mDrawingBrush.setColor(prefs.getInt(BRUSH_COLOR, mDrawingBrush.getColor()));
        mDrawingBrush.setAlpha(255); // don't start with see through color ever
        mDrawingBrush.setStrokeWidth(prefs.getFloat(BRUSH_STROKE_WIDTH, mDrawingBrush.getStrokeWidth()));

        mDrawingView.setupFromEtch(mEtchOverlayItem);

        mLoadingLayout = ViewUtils.subViewById(rootView, R.id.drawing_loader_overlay);
        mLoadingAlertImage = ViewUtils.subViewById(rootView, R.id.drawing_loader_alert_img);
        mLoadingProgress = ViewUtils.subViewById(rootView, R.id.drawing_loader_progress);

        logger.debug("onCreateView {}", (spiceManager != null));


        mColorSwatchButton = ViewUtils.subViewById(rootView, R.id.color_swatch_action_btn);
        mColorSwatchButton.setColor(mDrawingBrush.getColor());
        mColorSwatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorDialog();
            }
        });

        mStrokeOptionButton = ViewUtils.subViewById(rootView, R.id.brush_options_action);
        mStrokeOptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBrushStrokePicker();
            }
        });

        mUndoButton = ViewUtils.subViewById(rootView, R.id.drawing_undo_action);
        mUndoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undoLastDraw();
            }
        });

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

        return rootView;
    }

    private void undoLastDraw() {
        mDrawingView.undoLastDraw();
    }

    private void showBrushStrokePicker() {
        BrushStrokeDialog brushStrokeDialog = new BrushStrokeDialog(getActivity());
        brushStrokeDialog.setDrawingBrush(mDrawingBrush);
        brushStrokeDialog.show();
        brushStrokeDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                syncBrushChanges();
            }
        });
    }

    private void syncBrushChanges() {
        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);

        prefs.edit()
                .putInt(BRUSH_COLOR, mDrawingBrush.getColor())
                .putFloat(BRUSH_STROKE_WIDTH, mDrawingBrush.getStrokeWidth())
                .apply();

        mColorSwatchButton.setColor(mDrawingBrush.getColor());
    }

    @Override
    public void onDestroy() {
        mEventBus.unregister(this);
        super.onDestroy();
    }

    private void showColorDialog() {
        ColorPickerDialogFragment dialog = new ColorPickerDialogFragment();

        dialog.setDrawingBrush(mDrawingBrush);

        dialog.show(
                mMainActivity.getFragmentManager(),
                COLOR_PICKER_FRAGMENT_TAG
        );
        dialog.setOnDismissCallback(new Runnable() {
            @Override
            public void run() {
                syncBrushChanges();
            }
        });
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
                mEtchOverlayItem.setEtchBitmap(currentBitmap);
                endLoading();
                mEventBus.post(new DoneDrawingCommand());
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

}
