package kurtome.etch.app.drawing;

import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
import kurtome.etch.app.openstreetmap.MapLocationSelectedEvent;
import kurtome.etch.app.robospice.GetEtchRequest;
import kurtome.etch.app.robospice.SaveEtchRequest;

import javax.inject.Inject;

public class DrawingFragment extends Fragment {

    private static final Logger logger = LoggerManager.getLogger();

    private DrawingView mDrawingView;
    private DrawingBrush mDrawingBrush;
    private ImageButton mColorButton;
    private ImageButton mSaveEtchButton;
    private View mRootView;
    private TextView mLocationText;
    private Coordinates mCoordinates;

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

        final String image = mDrawingView.getCurrentImage();

        final SaveEtchCommand saveEtchCommand = new SaveEtchCommand();
        saveEtchCommand.setBase64Image(image);
        Coordinates coordinates = new Coordinates();
        coordinates.setLatitude(mCoordinates.getLatitude());
        coordinates.setLongitude(mCoordinates.getLongitude());
        saveEtchCommand.setCoordinates(coordinates);

        spiceManager.execute(new SaveEtchRequest(saveEtchCommand), new RequestListener<Void>() {

            @Override
            public void onRequestFailure(SpiceException e) {
                logger.e(e, "Error getting etch for location {}.", mCoordinates);
            }

            @Override
            public void onRequestSuccess(Void v) {
                logger.d("Saved etch {}.", saveEtchCommand);
            }
        });
    }


    @Subscribe
    public void updateLocation(final LocationUpdatedEvent event) {
//        mLocation = event.getLocation();
//
//        String text = mLocation.getLatitude() + " " + mLocation.getLongitude() + ", accuracy: " + mLocation.getAccuracy() + "m";
//        mLocationText.setText(text);
    }

    @Subscribe
    public void mapLocationSelected(MapLocationSelectedEvent event) {
        mCoordinates = event.getCoordinates();

        spiceManager.execute(new GetEtchRequest(mCoordinates), new RequestListener<Etch>() {

            @Override
            public void onRequestFailure(SpiceException e) {
                logger.e(e, "Error getting etch for location {}.", mCoordinates);
            }

            @Override
            public void onRequestSuccess(Etch etch) {
                mDrawingView.setCurrentImage(etch.getBase64Image());
            }
        });
    }
}
