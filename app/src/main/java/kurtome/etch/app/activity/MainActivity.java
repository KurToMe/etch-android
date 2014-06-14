package kurtome.etch.app.activity;

import android.app.Activity;
import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import dagger.ObjectGraph;
import kurtome.etch.app.R;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import kurtome.etch.app.colorpickerview.dialog.ColorPickerDialog;
import kurtome.etch.app.colorpickerview.view.ColorPickerView;
import kurtome.etch.app.dagger.MainModule;
import kurtome.etch.app.domain.Coordinates;
import kurtome.etch.app.domain.Etch;
import kurtome.etch.app.domain.SaveEtchCommand;
import kurtome.etch.app.drawing.DrawingBrush;
import kurtome.etch.app.drawing.DrawingView;
import kurtome.etch.app.location.LocationHelper;
import kurtome.etch.app.robospice.GetEtchRequest;
import kurtome.etch.app.robospice.SaveEtchRequest;

import javax.inject.Inject;


public class MainActivity extends Activity {

    private static final Logger logger = LoggerManager.getLogger();

    private ObjectGraph objectGraph;

    public MainActivity() {
        objectGraph = ObjectGraph.create(new MainModule(this));
        objectGraph.inject(this);
    }

    @Inject SpiceManager spiceManager;

    @Override
    protected void onStart() {
        super.onStart();
        spiceManager.start(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        spiceManager.shouldStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PlaceholderFragment fragment = new PlaceholderFragment();
        objectGraph.inject(fragment);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automaticaylly handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public static class PlaceholderFragment extends Fragment {

        private DrawingView drawingView;
        private DrawingBrush drawingBrush;
        private ImageButton colorButton;
        private ImageButton saveEtchButton;
        private View rootView;
        private TextView locationText;
        private LocationHelper locationHelper;
        private Location location;

        @Inject SpiceManager spiceManager;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_main, container, false);

            drawingView = (DrawingView) rootView.findViewById(R.id.drawing);
            drawingBrush = drawingView.getDrawingBrush();

            saveEtchButton = (ImageButton) rootView.findViewById(R.id.save_etch_btn);
            saveEtchButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveEtch();
                }
            });

            colorButton = (ImageButton) rootView.findViewById(R.id.color_chooser_button);
            colorButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    showColorDialog();
                }
            });

            locationText = (TextView) rootView.findViewById(R.id.location_txt);

            locationHelper = new LocationHelper(getActivity());
            locationHelper.setAccuracy(100f);
            long timeoutMs = 3 * 60 * 1000;
            locationHelper.fetchLocation(timeoutMs, LocationHelper.Accuracy.FINE, new LocationHelper.LocationResponse() {
                @Override
                public void onLocationAcquired(Location l) {
                    l.getAccuracy();
                    location = l;
                    updateLocation(l);
                }
            });

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
}
