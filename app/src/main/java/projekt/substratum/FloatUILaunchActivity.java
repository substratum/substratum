package projekt.substratum;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import projekt.substratum.config.References;
import projekt.substratum.services.FloatUiTile;
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        prefs.edit().putInt("float_tile", Tile.STATE_ACTIVE).apply();
        FloatUiTile.requestListeningState(getApplicationContext(),
                new ComponentName(getApplicationContext(), FloatUiTile.class));
        getApplicationContext().startService(new Intent(getApplicationContext(),
                SubstratumFloatInterface.class));
    }

    private void hideFloatingHead() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        prefs.edit().putInt("float_tile", Tile.STATE_INACTIVE).apply();
        FloatUiTile.requestListeningState(getApplicationContext(),
                new ComponentName(getApplicationContext(), FloatUiTile.class));
        stopService(new Intent(getApplicationContext(),
                SubstratumFloatInterface.class));
    }
}
