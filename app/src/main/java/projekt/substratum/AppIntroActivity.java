package projekt.substratum;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.stephentuso.welcome.WelcomeScreenHelper;

import projekt.substratum.util.AppIntro;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class AppIntroActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private WelcomeScreenHelper welcomeScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());

        if (prefs.getBoolean("first_run", true)) {
            loadAppIntro(savedInstanceState);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            this.finish();
        }
    }

    public void loadAppIntro(Bundle savedInstanceState) {
        welcomeScreen = new WelcomeScreenHelper(this, AppIntro.class);
        welcomeScreen.show(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WelcomeScreenHelper.DEFAULT_WELCOME_SCREEN_REQUEST) {
            if (resultCode == RESULT_OK) {
                prefs.edit().putBoolean("enable_swapping_overlays", false).apply();
                prefs.edit().putBoolean("first_run", false).apply();

                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                this.finish();
            } else {
                this.finish();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        welcomeScreen.onSaveInstanceState(outState);
    }
}