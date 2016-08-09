package projekt.substratum.fragments;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.widget.Toast;

import projekt.substratum.BuildConfig;
import projekt.substratum.LauncherActivity;
import projekt.substratum.R;
import projekt.substratum.config.References;

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
        if (References.checkOMS()) {
            addPreferencesFromResource(R.xml.preference_fragment);
        } else {
            addPreferencesFromResource(R.xml.legacy_preference_fragment);
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getContext());

        Preference aboutSubstratum = getPreferenceManager().findPreference
                ("about_substratum");
        aboutSubstratum.setSummary(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE +
                ")");
        aboutSubstratum.setIcon(getResources().getDrawable(R.mipmap.main_launcher));

        if (References.checkOMS()) {
            Preference aboutMasquerade = getPreferenceManager().findPreference
                    ("about_masquerade");
            aboutMasquerade.setIcon(getResources().getDrawable(R.mipmap.restore_launcher));
            try {
                PackageInfo pinfo;
                pinfo = getContext().getPackageManager().getPackageInfo("masquerade.substratum", 0);
                String versionName = pinfo.versionName;
                int versionCode = pinfo.versionCode;
                aboutMasquerade.setSummary(
                        versionName + " (" + versionCode + ")");
            } catch (Exception e) {
                // Masquerade was not installed
            }
            final Preference masqueradeCheck = getPreferenceManager().findPreference
                    ("masquerade_check");
            masqueradeCheck.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            if (References.isPackageInstalled(getContext(),
                                    "masquerade.substratum")) {
                                Intent runCommand = new Intent();
                                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                runCommand.setAction("masquerade.substratum.COMMANDS");
                                runCommand.putExtra("substratum-check", "masquerade-ball");
                                getContext().sendBroadcast(runCommand);
                            } else {
                                Toast toast = Toast.makeText(getContext(), getString(R.string
                                                .masquerade_check_not_installed),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }
                            return false;
                        }
                    });

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
                                        .COMPONENT_ENABLED_STATE_ENABLED, PackageManager
                                        .DONT_KILL_APP);
                                Toast toast = Toast.makeText(getContext(), getString(R.string
                                                .hide_app_icon_toast_disabled),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                                hide_app_checkbox.setChecked(true);
                            } else {
                                prefs.edit().putBoolean("show_app_icon", false).apply();
                                PackageManager p = getContext().getPackageManager();
                                ComponentName componentName = new ComponentName(getContext(),
                                        LauncherActivity.class);
                                p.setComponentEnabledSetting(componentName, PackageManager
                                        .COMPONENT_ENABLED_STATE_DISABLED, PackageManager
                                        .DONT_KILL_APP);

                                Toast toast = Toast.makeText(getContext(), getString(R.string
                                                .hide_app_icon_toast_enabled),
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
            if (!References.getProp("ro.substratum.recreate").equals("true"))
                systemUIRestart.setVisible(false);

            final CheckBoxPreference manager_disabled_overlays = (CheckBoxPreference)
                    getPreferenceManager().findPreference("manager_disabled_overlays");
            if (prefs.getBoolean("manager_disabled_overlays", true)) {
                manager_disabled_overlays.setChecked(true);
            } else {
                manager_disabled_overlays.setChecked(false);
            }
            manager_disabled_overlays.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            boolean isChecked = (Boolean) newValue;
                            if (isChecked) {
                                prefs.edit().putBoolean("manager_disabled_overlays", true).apply();
                                manager_disabled_overlays.setChecked(true);
                                return true;
                            } else {
                                prefs.edit().putBoolean("manager_disabled_overlays", false).apply();
                                manager_disabled_overlays.setChecked(false);
                                return false;
                            }
                        }
                    });
        }

        final CheckBoxPreference vibrate_on_compiled = (CheckBoxPreference)
                getPreferenceManager().findPreference("vibrate_on_compiled");
        if (prefs.getBoolean("vibrate_on_compiled", true)) {
            vibrate_on_compiled.setChecked(true);
        } else {
            vibrate_on_compiled.setChecked(false);
        }
        vibrate_on_compiled.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            prefs.edit().putBoolean("vibrate_on_compiled", true).apply();
                            vibrate_on_compiled.setChecked(true);
                            return true;
                        } else {
                            prefs.edit().putBoolean("vibrate_on_compiled", false).apply();
                            vibrate_on_compiled.setChecked(false);
                            return false;
                        }
                    }
                });

        final CheckBoxPreference show_template_version = (CheckBoxPreference)
                getPreferenceManager().findPreference("show_template_version");
        if (prefs.getBoolean("show_template_version", true)) {
            show_template_version.setChecked(true);
        } else {
            show_template_version.setChecked(false);
        }
        show_template_version.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            prefs.edit().putBoolean("show_template_version", true).apply();
                            show_template_version.setChecked(true);
                        } else {
                            prefs.edit().putBoolean("show_template_version", false).apply();
                            show_template_version.setChecked(false);
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