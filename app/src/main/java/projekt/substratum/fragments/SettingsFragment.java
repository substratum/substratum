package projekt.substratum.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.util.ReadXMLFile;

/**
 * Created by Nicholas on 2016-03-31.
 */
public class SettingsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.settings_fragment, null);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getContext());

        Switch show_installed_packages = (Switch) root.findViewById(R.id
                .show_installed_packages_only);
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

        Button purgeAll = (Button) root.findViewById(R.id.purge);
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
                        Toast toast = Toast.makeText(getContext(), getString(R.string
                                        .purge_completion),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    } catch (IOException ioe) {
                    }
                }
            });
        }

        Button purgeAll2 = (Button) root.findViewById(R.id.purge2);
        if (purgeAll2 != null) {
            purgeAll2.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays.xml " +
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/.substratum/current_overlays.xml");

                    String final_commands = "";
                    String[] commands = {Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/.substratum/current_overlays.xml", "5"};
                    List<String> enabled_overlays = ReadXMLFile.main(commands);

                    File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/.substratum/current_overlays.xml");
                    if (f.exists() && !f.isDirectory()) {
                        Toast toast = Toast.makeText(getContext(), getString(R.string
                                        .disable_overlay_toast),
                                Toast.LENGTH_SHORT);
                        toast.show();
                        for (int i = 0; i < enabled_overlays.size(); i++) {
                            if (i == 0) {
                                final_commands = final_commands + "om disable " +
                                        enabled_overlays.get(i);
                            } else {
                                final_commands = final_commands + " && om disable " +
                                        enabled_overlays.get(i);
                            }
                            Log.d("Settings", "Disabling overlay \"" + enabled_overlays
                                    .get(i) + "\"");
                        }
                        if (final_commands.contains("com.android.systemui")) {
                            final_commands = final_commands + " && pkill com.android.systemui";
                        }
                        eu.chainfire.libsuperuser.Shell.SU.run(final_commands);
                    } else {
                        Toast toast = Toast.makeText(getContext(), getString(R.string
                                        .disable_overlay_toast_not_found),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            });
        }
        return root;
    }
}