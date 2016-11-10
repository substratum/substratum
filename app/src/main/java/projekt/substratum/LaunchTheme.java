package projekt.substratum;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.io.File;

import projekt.substratum.config.References;

public class LaunchTheme extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent currentIntent = getIntent();
        String theme_name = currentIntent.getStringExtra("theme_name");
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
                    References.launchTheme(this, theme_name, theme_pid, null);
                } else {
                    Toast toast = Toast.makeText(this, this.getString(R.string.toast_needs_caching),
                            Toast.LENGTH_LONG);
                    toast.show();
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                }
            } else {
                Toast toast = Toast.makeText(this, this.getString(R.string.toast_uninstalled),
                        Toast.LENGTH_SHORT);
                toast.show();
            }
        } else {
            Toast toast = Toast.makeText(this, this.getString(R.string.background_updating_toast),
                    Toast.LENGTH_SHORT);
            toast.show();
        }
        finish();
    }
}