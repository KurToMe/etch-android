package kurtome.etch.app.drawing;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
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
import kurtome.etch.app.location.LocationHelper;
import kurtome.etch.app.location.LocationUpdatedEvent;
import kurtome.etch.app.openstreetmap.MapFragment;
import kurtome.etch.app.openstreetmap.MapLocationSelectedEvent;
import kurtome.etch.app.robospice.GetEtchRequest;
import kurtome.etch.app.robospice.SaveEtchRequest;

import javax.inject.Inject;
import java.util.zip.GZIPInputStream;

public class DrawingFragment extends Fragment {

    private static final Logger logger = LoggerManager.getLogger();

    private DrawingView mDrawingView;
    private DrawingBrush mDrawingBrush;
    private ImageButton mColorButton;
    private ImageButton mSaveEtchButton;
    private View mRootView;
    private TextView mLocationText;
    private Coordinates mCoordinates;
    private MapFragment.EtchOverlayItem mEtchOverlayItem;
    private RelativeLayout mLoadingLayout;

    @Inject public SpiceManager spiceManager;
    @Inject public Bus mEventBus;

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


        mRootView = inflater.inflate(R.layout.fragment_main, container, false);

        mDrawingView = (DrawingView) mRootView.findViewById(R.id.drawing);
        mDrawingBrush = mDrawingView.getDrawingBrush();

        mLoadingLayout = (RelativeLayout) mRootView.findViewById(R.id.loadingPanel);

        mSaveEtchButton = (ImageButton) mRootView.findViewById(R.id.save_etch_btn);
        mSaveEtchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEtch();
            }
        });

        mColorButton = (ImageButton) mRootView.findViewById(R.id.color_chooser_button);
        mColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorDialog();
            }
        });

        mLocationText = (TextView) mRootView.findViewById(R.id.location_txt);
//        mLocationText.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                refreshLocation();
//            }
//        });


        logger.d("onCreateView {}", (spiceManager != null));

        // Do this last so everything is setup before handling events
        mEventBus.register(this);

        return mRootView;
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

    private void saveEtch() {
        if (mCoordinates == null) {
            logger.d("Unknown location, can't set etch.");
            return;
        }
        mLoadingLayout.setVisibility(View.VISIBLE);

        final byte[] image = mDrawingView.getCurrentImage();

        final SaveEtchCommand saveEtchCommand = new SaveEtchCommand();
        final Bitmap currentBitmap = mDrawingView.getCopyOfCurrentBitmap();
        saveEtchCommand.setCoordinates(mCoordinates);
        saveEtchCommand.setImageGzip(image);

        mSaveEtchButton.setEnabled(false);
        mSaveEtchButton.invalidate();
        spiceManager.execute(new SaveEtchRequest(saveEtchCommand), new RequestListener<Void>() {

            @Override
            public void onRequestFailure(SpiceException e) {
                mLoadingLayout.setVisibility(View.INVISIBLE);
                logger.e(e, "Error getting etch for location {}.", mCoordinates);
            }

            @Override
            public void onRequestSuccess(Void v) {
                mLoadingLayout.setVisibility(View.INVISIBLE);
                logger.d("Saved etch {}.", saveEtchCommand);
                mEtchOverlayItem.scaleAndSetBitmap(currentBitmap);
                mSaveEtchButton.setEnabled(true);
                mSaveEtchButton.invalidate();
            }
        });
    }


    public void updateLocation(final LocationUpdatedEvent event) {
//        mLocation = event.getLocation();
//
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
        mLoadingLayout.setVisibility(View.VISIBLE);
        mCoordinates = event.getCoordinates();
        mEtchOverlayItem = event.getEtchOverlayItem();
        String text = String.format("latitude: %s, longitude %s", format(mCoordinates.getLatitudeE6()), format(mCoordinates.getLongitudeE6()));
        mLocationText.setText(text);

        mSaveEtchButton.setEnabled(false);
        mSaveEtchButton.invalidate();
        spiceManager.execute(new GetEtchRequest(mCoordinates), new RequestListener<Etch>() {

            @Override
            public void onRequestFailure(SpiceException e) {
                logger.e(e, "Error getting etch for location {}.", mCoordinates);
                mLoadingLayout.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onRequestSuccess(Etch etch) {
                mDrawingView.setCurrentImage(etch.getGzipImage());
                mSaveEtchButton.setEnabled(true);
                mSaveEtchButton.invalidate();
                mLoadingLayout.setVisibility(View.INVISIBLE);
            }
        });
    }
}
