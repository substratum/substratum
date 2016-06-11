package projekt.substratum.fragments;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import projekt.substratum.LaunchActivity;
import projekt.substratum.R;

/**
 * Created by Nicholas on 2016-03-31.
 */
public class SettingsFragment extends Fragment {

    private static String getProp(String propName) {
        Process p;
        String result = "";
        try {
            p = new ProcessBuilder("/system/bin/getprop",
                    propName).redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = br.readLine()) != null) {
                result = line;
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.settings_fragment, null);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getContext());

        Switch hideAppIcon = (Switch) root.findViewById(R.id.hide_app_icon);
        hideAppIcon.setEnabled(false);

        if (prefs.getBoolean("hide_app_icon", true)) {
            hideAppIcon.setChecked(false);
        } else {
            hideAppIcon.setChecked(true);
        }

        hideAppIcon.setOnCheckedChangeListener(new CompoundButton
                .OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    prefs.edit().putBoolean("hide_app_icon", true).apply();
                    PackageManager p = getContext().getPackageManager();
                    ComponentName componentName = new ComponentName(getContext(), LaunchActivity
                            .class);
                    p.setComponentEnabledSetting(componentName, PackageManager
                            .COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    Toast toast = Toast.makeText(getContext(), getString(R.string
                                    .hide_app_icon_toast_enabled),
                            Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    prefs.edit().putBoolean("hide_app_icon", false).apply();
                    PackageManager p = getContext().getPackageManager();
                    ComponentName componentName = new ComponentName(getContext(), LaunchActivity
                            .class);
                    p.setComponentEnabledSetting(componentName, PackageManager
                            .COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    Toast toast = Toast.makeText(getContext(), getString(R.string
                                    .hide_app_icon_toast_disabled),
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
        if (getProp("ro.substratum.settings") != "") {
            hideAppIcon.setEnabled(true);
        }
        return root;
    }
}