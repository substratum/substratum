package projekt.substratum;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import projekt.substratum.config.References;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());

        if (prefs.getBoolean("first_run", true)) {
            prefs.edit().putBoolean("show_app_icon", true).apply();
            prefs.edit().putBoolean("systemui_recreate", true).apply();
            prefs.edit().putBoolean("is_updating", false).apply();
            prefs.edit().putBoolean("substratum_oms", References.checkOMS()).apply();
            Intent intent = new Intent(this, SplashScreenActivityFirstLaunch.class);
            startActivity(intent);
            this.finish();
        } else {
            Intent intent = new Intent(this, SplashScreenActivity.class);
            startActivity(intent);
            this.finish();
        }
    }
}