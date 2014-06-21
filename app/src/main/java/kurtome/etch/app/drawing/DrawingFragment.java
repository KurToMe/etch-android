package kurtome.etch.app.drawing;

import android.app.Activity;
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
import kurtome.etch.app.ObjectGraphUtils;
import kurtome.etch.app.R;
import kurtome.etch.app.activity.MainActivity;
import kurtome.etch.app.colorpickerview.dialog.ColorPickerDialog;
import kurtome.etch.app.colorpickerview.view.ColorPickerView;
import kurtome.etch.app.domain.Coordinates;
import kurtome.etch.app.domain.Etch;
import kurtome.etch.app.domain.SaveEtchCommand;
import kurtome.etch.app.location.LocationHelper;
import kurtome.etch.app.robospice.GetEtchRequest;
import kurtome.etch.app.robospice.SaveEtchRequest;

import javax.inject.Inject;

public class DrawingFragment extends Fragment {

    private static final Logger logger = LoggerManager.getLogger();

    private DrawingView drawingView;
    private DrawingBrush drawingBrush;
    private ImageButton colorButton;
    private ImageButton saveEtchButton;
    private View rootView;
    private TextView locationText;
    private LocationHelper locationHelper;
    private Location location;
    private boolean postInject = false;

    @Inject public SpiceManager spiceManager;

    @Override
    public void onStart() {
        super.onStart();
        spiceManager.start(getActivity());

        long timeoutMs = 3 * 60 * 1000;
        locationHelper.fetchLocation(timeoutMs, LocationHelper.Accuracy.FINE, new LocationHelper.LocationResponse() {
            @Override
            public void onLocationAcquired(Location l) {
                l.getAccuracy();
                location = l;
                updateLocation(l);
            }
        });
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

        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        drawingView = (DrawingView) rootView.findViewById(R.id.drawing);
        drawingBrush = drawingView.getDrawingBrush();

        saveEtchButton = (ImageButton) rootView.findViewById(R.id.save_etch_btn);
        saveEtchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEtch();
            }
        });

        colorButton = (ImageButton) rootView.findViewById(R.id.color_chooser_button);
        colorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorDialog();
            }
        });

        locationText = (TextView) rootView.findViewById(R.id.location_txt);

        locationHelper = new LocationHelper(getActivity());
        locationHelper.setAccuracy(100f);

        logger.d("onCreateView {}", (spiceManager != null));
        return rootView;
    }

    private void showColorDialog() {
        ColorPickerDialog dialog = new ColorPickerDialog(getActivity(), drawingBrush.getColor(), new ColorPickerView.OnColorChangedListener() {
            @Override
            public void onColorChanged(int newColor) {
                setColor(newColor);
            }
        });
        dialog.setAlphaSliderVisible(true);
        dialog.show();
    }

    private void setColor(int color) {
        drawingBrush.setColor(color);
    }

    private void saveEtch() {
        if (location == null) {
            logger.d("Unknown location, can't set etch.");
            return;
        }

        final String image = drawingView.getCurrentImage();

        final SaveEtchCommand saveEtchCommand = new SaveEtchCommand();
        saveEtchCommand.setBase64Image(image);
        Coordinates coordinates = new Coordinates();
        coordinates.setLatitude(location.getLatitude());
        coordinates.setLongitude(location.getLongitude());
        saveEtchCommand.setCoordinates(coordinates);

        spiceManager.execute(new SaveEtchRequest(saveEtchCommand), new RequestListener<Void>() {

            @Override
            public void onRequestFailure(SpiceException e) {
                logger.e(e, "Error getting etch for location {}.", location);
            }

            @Override
            public void onRequestSuccess(Void v) {
                logger.d("Saved etch {}.", saveEtchCommand);
            }
        });
    }

    private void updateLocation(final Location location) {
        String text = location.getLatitude() + " " + location.getLongitude() + ", accuracy: " + location.getAccuracy();
        locationText.setText(text);
        spiceManager.execute(new GetEtchRequest(location), new RequestListener<Etch>() {

            @Override
            public void onRequestFailure(SpiceException e) {
                logger.e(e, "Error getting etch for location {}.", location);
            }

            @Override
            public void onRequestSuccess(Etch etch) {
                drawingView.setCurrentImage(etch.getBase64Image());
            }
        });
    }

}
