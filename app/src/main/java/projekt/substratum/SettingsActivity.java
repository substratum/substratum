package projekt.substratum;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Created by Nicholas on 2016-03-31.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        getSupportActionBar().setTitle(getString(R.string.menu_settings));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());

        Switch show_installed_packages = (Switch) findViewById(R.id.show_installed_packages_only);
        if (prefs.getBoolean("show_installed_packages", true)) {
            show_installed_packages.setChecked(true);
        } else {
            show_installed_packages.setChecked(false);
        }
        show_installed_packages.setOnCheckedChangeListener(new CompoundButton
                .OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    prefs.edit().putBoolean("show_installed_packages", true).apply();
                } else {
                    prefs.edit().putBoolean("show_installed_packages", false).apply();
                }
            }
        });

        Button purgeAll = (Button) findViewById(R.id.purge);
        if (purgeAll != null) {
            purgeAll.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        String line;
                        Process nativeApp = Runtime.getRuntime().exec(
                                "om list");

                        OutputStream stdin = nativeApp.getOutputStream();
                        InputStream stderr = nativeApp.getErrorStream();
                        InputStream stdout = nativeApp.getInputStream();
                        stdin.write(("ls\n").getBytes());
                        stdin.write("exit\n".getBytes());
                        stdin.flush();
                        stdin.close();

                        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                        while ((line = br.readLine()) != null) {
                            if (line.contains("    ")) {
                                Log.d("Uninstalling", line.substring(8));
                                eu.chainfire.libsuperuser.Shell.SU.run(
                                        "pm uninstall " + line.substring(8));
                            }
                        }
                        br.close();
                        br = new BufferedReader(new InputStreamReader(stderr));
                        while ((line = br.readLine()) != null) {
                            Log.e("LayersBuilder", line);
                        }
                        br.close();
                        eu.chainfire.libsuperuser.Shell.SU.run("pkill com.android.systemui");
                        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string
                                        .purge_completion),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    } catch (IOException ioe) {
                    }
                }
            });
        }
    }
}