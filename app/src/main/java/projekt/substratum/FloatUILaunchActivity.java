package projekt.substratum;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import projekt.substratum.config.References;
import projekt.substratum.services.SubstratumFloatInterface;

import static projekt.substratum.config.References.checkUsagePermissions;

public class FloatUILaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Settings.canDrawOverlays(getApplicationContext()) &&
                checkUsagePermissions(getApplicationContext())) {
            if (!References.isServiceRunning(SubstratumFloatInterface.class,
                    getApplicationContext())) {
                showFloatingHead();
            } else {
                hideFloatingHead();
            }
        } else {
            Toast toast = Toast.makeText(
                    getApplicationContext(),
                    getString(R.string.per_app_manual_grant),
                    Toast.LENGTH_LONG);
            toast.show();
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