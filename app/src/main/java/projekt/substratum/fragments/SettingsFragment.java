package projekt.substratum.fragments;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.widget.Toast;

import projekt.substratum.LauncherActivity;
import projekt.substratum.R;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class SettingsFragment extends PreferenceFragmentCompat {

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
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preference_fragment);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getContext());

        final CheckBoxPreference hide_app_checkbox = (CheckBoxPreference)
                getPreferenceManager().findPreference("hide_app_checkbox");
        hide_app_checkbox.setEnabled(false);
        if (prefs.getBoolean("show_app_icon", true)) {
            hide_app_checkbox.setChecked(true);
        } else {
            hide_app_checkbox.setChecked(false);
        }
        if (checkSettingsPackageSupport()) {
            hide_app_checkbox.setSummary(getString(R.string.hide_app_icon_supported));
            hide_app_checkbox.setEnabled(true);
        }
        hide_app_checkbox.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            prefs.edit().putBoolean("show_app_icon", true).apply();
                            PackageManager p = getContext().getPackageManager();
                            ComponentName componentName = new ComponentName(getContext(),
                                    LauncherActivity
                                            .class);
                            p.setComponentEnabledSetting(componentName, PackageManager
                                    .COMPONENT_ENABLED_STATE_DISABLED, PackageManager
                                    .DONT_KILL_APP);
                            Toast toast = Toast.makeText(getContext(), getString(R.string
                                            .hide_app_icon_toast_enabled),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                            hide_app_checkbox.setChecked(true);
                        } else {
                            prefs.edit().putBoolean("show_app_icon", false).apply();
                            PackageManager p = getContext().getPackageManager();
                            ComponentName componentName = new ComponentName(getContext(),
                                    LauncherActivity
                                            .class);
                            p.setComponentEnabledSetting(componentName, PackageManager
                                    .COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                            Toast toast = Toast.makeText(getContext(), getString(R.string
                                            .hide_app_icon_toast_disabled),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                            hide_app_checkbox.setChecked(false);
                        }
                        return false;
                    }
                });

        final CheckBoxPreference systemUIRestart = (CheckBoxPreference)
                getPreferenceManager().findPreference("restart_systemui");
        if (prefs.getBoolean("systemui_recreate", true)) {
            systemUIRestart.setChecked(true);
        } else {
            systemUIRestart.setChecked(false);
        }
        systemUIRestart.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            prefs.edit().putBoolean("systemui_recreate", true).apply();
                            Toast toast = Toast.makeText(getContext(), getString(R.string
                                            .restart_systemui_toast_enabled),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                            systemUIRestart.setChecked(true);
                        } else {
                            prefs.edit().putBoolean("systemui_recreate", false).apply();
                            Toast toast = Toast.makeText(getContext(), getString(R.string
                                            .restart_systemui_toast_disabled),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                            systemUIRestart.setChecked(false);
                        }
                        return false;
                    }
                });

        final CheckBoxPreference dynamic_actionbar = (CheckBoxPreference)
                getPreferenceManager().findPreference("dynamic_actionbar");
        if (prefs.getBoolean("dynamic_actionbar", true)) {
            dynamic_actionbar.setChecked(true);
        } else {
            dynamic_actionbar.setChecked(false);
        }
        dynamic_actionbar.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            prefs.edit().putBoolean("dynamic_actionbar", true).apply();
                            dynamic_actionbar.setChecked(true);
                        } else {
                            prefs.edit().putBoolean("dynamic_actionbar", false).apply();
                            dynamic_actionbar.setChecked(false);
                        }
                        return false;
                    }
                });

        final CheckBoxPreference dynamic_navbar = (CheckBoxPreference)
                getPreferenceManager().findPreference("dynamic_navbar");
        if (prefs.getBoolean("dynamic_navbar", true)) {
            dynamic_navbar.setChecked(true);
        } else {
            dynamic_navbar.setChecked(false);
        }
        dynamic_navbar.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            prefs.edit().putBoolean("dynamic_navbar", true).apply();
                            dynamic_navbar.setChecked(true);
                        } else {
                            prefs.edit().putBoolean("dynamic_navbar", false).apply();
                            dynamic_navbar.setChecked(false);
                        }
                        return false;
                    }
                });
    }
}