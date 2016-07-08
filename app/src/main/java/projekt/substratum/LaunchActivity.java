package projekt.substratum;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());

        if (prefs.getBoolean("first_run", true)) {
            prefs.edit().putBoolean("show_app_icon", true).apply();
            prefs.edit().putBoolean("systemui_recreate", true).apply();
            prefs.edit().putBoolean("is_updating", false).apply();
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