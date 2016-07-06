package projekt.substratum.fragments;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import projekt.substratum.LaunchActivity;
import projekt.substratum.R;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class SettingsFragment extends Fragment {

    String settingsPackageName = "com.android.settings";
    String settingsSubstratumDrawableName = "ic_settings_substratum";

    private boolean checkSettingsPackageSupport() {
        try {
            Resources res = getContext().getApplicationContext().getPackageManager()
                    .getResourcesForApplication(settingsPackageName);
            int substratum_icon = res.getIdentifier(settingsPackageName + ":drawable/" +
                    settingsSubstratumDrawableName, "drawable", settingsPackageName);
            if (substratum_icon != 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.e("SubstratumLogger", "Could not load drawable from Settings.apk.");
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.settings_fragment, null);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getContext());

        TextView subtext = (TextView) root.findViewById(R.id.hide_app_icon_subtext);

        Switch hideAppIcon = (Switch) root.findViewById(R.id.hide_app_icon);
        Switch systemUIRestart = (Switch) root.findViewById(R.id.restart_systemui);
        hideAppIcon.setEnabled(false);

        if (prefs.getBoolean("show_app_icon", true)) {
            hideAppIcon.setChecked(true);
        } else {
            hideAppIcon.setChecked(false);
        }

        if (prefs.getBoolean("systemui_recreate", true)) {
            systemUIRestart.setChecked(true);
        } else {
            systemUIRestart.setChecked(false);
        }

        hideAppIcon.setOnCheckedChangeListener(new CompoundButton
                .OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    prefs.edit().putBoolean("show_app_icon", true).apply();
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
                    prefs.edit().putBoolean("show_app_icon", false).apply();
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

        systemUIRestart.setOnCheckedChangeListener(new CompoundButton
                .OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    prefs.edit().putBoolean("systemui_recreate", true).apply();
                    Toast toast = Toast.makeText(getContext(), getString(R.string
                                    .restart_systemui_toast_enabled),
                            Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    prefs.edit().putBoolean("systemui_recreate", false).apply();
                    Toast toast = Toast.makeText(getContext(), getString(R.string
                                    .restart_systemui_toast_disabled),
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
        if (checkSettingsPackageSupport()) {
            subtext.setText(getString(R.string.hide_app_icon_supported));
            hideAppIcon.setEnabled(true);
        }
        return root;
    }
}