package projekt.substratum;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import projekt.substratum.util.ReadOverlaysFile;
import projekt.substratum.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class RestoreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.finish();
        String final_commands = "";
        String[] commands = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/.substratum/overlays.xml", "5"};
        List<String> enabled_overlays = ReadOverlaysFile.main(commands);

        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/.substratum/overlays.xml");
        if (f.exists() && !f.isDirectory()) {
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string
                            .restore_activity_toast),
                    Toast.LENGTH_SHORT);
            toast.show();
            for (int i = 0; i < enabled_overlays.size(); i++) {
                if (i == 0) {
                    final_commands = final_commands + "om enable " + enabled_overlays.get(i);
                } else {
                    final_commands = final_commands + " && om enable " + enabled_overlays.get(i);
                }
                Log.d("RestoreActivity", "Restoring overlay \"" + enabled_overlays.get(i) + "\"");
            }
            Root.runCommand(final_commands);
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string
                            .restore_activity_toast_not_found),
                    Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}