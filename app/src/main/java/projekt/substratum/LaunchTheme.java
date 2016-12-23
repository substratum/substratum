package projekt.substratum;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;

import java.io.File;

import projekt.substratum.config.References;

public class LaunchTheme extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent currentIntent = getIntent();
        String theme_pid = currentIntent.getStringExtra("theme_pid");

        SharedPreferences prefs = this.getSharedPreferences(
                "substratum_state", Context.MODE_PRIVATE);
        if (!prefs.contains("is_updating")) prefs.edit()
                .putBoolean("is_updating", false).apply();
        if (!prefs.getBoolean("is_updating", true)) {
            // Process fail case if user uninstalls an app and goes back an activity
            if (References.isPackageInstalled(this, theme_pid)) {
                File checkSubstratumVerity = new File(this.getCacheDir()
                        .getAbsoluteFile() + "/SubstratumBuilder/" + theme_pid + "/substratum.xml");
                if (checkSubstratumVerity.exists()) {
                    References.launchTheme(this, theme_pid, null, false);
                } else {
                    Snackbar.make(findViewById(android.R.id.content),
                            getString(R.string.toast_needs_caching),
                            Snackbar.LENGTH_LONG)
                            .show();
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                }
            } else {
                Snackbar.make(findViewById(android.R.id.content),
                        getString(R.string.toast_uninstalled),
                        Snackbar.LENGTH_LONG)
                        .show();
            }
        } else {
            Snackbar.make(findViewById(android.R.id.content),
                    getString(R.string.background_updating_toast),
                    Snackbar.LENGTH_LONG)
                    .show();
        }
        finish();
    }
}