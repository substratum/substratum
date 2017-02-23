package projekt.substratum;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import projekt.substratum.config.References;
import projekt.substratum.services.SubstratumFloatInterface;

public class FloatUILaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!References.isServiceRunning(SubstratumFloatInterface.class,
                getApplicationContext())) {
            showFloatingHead();
        } else {
            hideFloatingHead();
        }
        this.finish();
    }

    public void showFloatingHead() {
        getApplicationContext().startService(new Intent(getApplicationContext(),
                SubstratumFloatInterface.class));
    }

    private void hideFloatingHead() {
        stopService(new Intent(getApplicationContext(),
                SubstratumFloatInterface.class));
    }
}