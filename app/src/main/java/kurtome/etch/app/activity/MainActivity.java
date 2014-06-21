package kurtome.etch.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.noveogroup.android.log.Logger;
import com.noveogroup.android.log.LoggerManager;
import com.octo.android.robospice.SpiceManager;
import dagger.ObjectGraph;
import kurtome.etch.app.R;
import kurtome.etch.app.dagger.MainModule;
import kurtome.etch.app.drawing.DrawingFragment;

import javax.inject.Inject;


public class MainActivity extends Activity {

    private static final Logger logger = LoggerManager.getLogger();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new DrawingFragment())
                    .commit();
        }

    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automaticaylly handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }


}
