package projekt.substratum;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

    public boolean has_modified_anything = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (has_modified_anything) {
                    Intent i = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (has_modified_anything) {
            Intent i = getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
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