package projekt.substratum.fragments;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
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

public class SettingsFragment extends PreferenceFragmentCompat {

    private boolean checkSettingsPackageSupport() {
        try {
            Resources res = getContext().getApplicationContext().getPackageManager()
                    .getResourcesForApplication(References.settingsPackageName);
            int substratum_icon = res.getIdentifier(References.settingsPackageName + ":drawable/" +
                            References.settingsSubstratumDrawableName, "drawable",
                    References.settingsPackageName);
            return substratum_icon != 0;
        } catch (Exception e) {
            Log.e("SubstratumLogger", "Could not load drawable from Settings.apk.");
        }
        return false;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        if (References.checkOMS(getContext())) {
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
        aboutSubstratum.setIcon(getContext().getDrawable(R.mipmap.main_launcher));
        aboutSubstratum.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        try {
                            String sourceURL = getString(R.string.substratum_github);
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(sourceURL));
                            startActivity(i);
                        } catch (ActivityNotFoundException activityNotFoundException) {
                            //
                        }
                        return false;
                    }
                });

        Preference themePlatform = getPreferenceManager().findPreference
                ("theme_platform");
        if (References.checkOMS(getContext())) {
            if (References.checkOMSVersion(getContext()) == 3) {
                themePlatform.setSummary(getString(R.string.settings_about_oms_version_3));
            } else if (References.checkOMSVersion(getContext()) == 7) {
                themePlatform.setSummary(getString(R.string.settings_about_oms_version_7));
            }
        }
        themePlatform.setIcon(getContext().getDrawable(R.mipmap.projekt_icon));

        if (References.checkOMS(getContext())) {
            Preference aboutMasquerade = getPreferenceManager().findPreference
                    ("about_masquerade");
            aboutMasquerade.setIcon(getContext().getDrawable(R.mipmap.restore_launcher));
            aboutMasquerade.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            try {
                                String sourceURL = getString(R.string.masquerade_github);
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(sourceURL));
                                startActivity(i);
                            } catch (ActivityNotFoundException activityNotFoundException) {
                                //
                            }
                            return false;
                        }
                    });
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

            final CheckBoxPreference quick_apply_ui = (CheckBoxPreference)
                    getPreferenceManager().findPreference("quick_apply");
            if (prefs.getBoolean("quick_apply", false)) {
                quick_apply_ui.setChecked(true);
            } else {
                quick_apply_ui.setChecked(false);
            }
            quick_apply_ui.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            boolean isChecked = (Boolean) newValue;
                            if (isChecked) {
                                prefs.edit().putBoolean("quick_apply", true).apply();
                                quick_apply_ui.setChecked(true);
                                return true;
                            } else {
                                prefs.edit().putBoolean("quick_apply", false).apply();
                                quick_apply_ui.setChecked(false);
                                return false;
                            }
                        }
                    });

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

        final CheckBoxPreference alternate_drawer_design = (CheckBoxPreference)
                getPreferenceManager().findPreference("alternate_drawer_design");
        if (prefs.getBoolean("alternate_drawer_design", false)) {
            alternate_drawer_design.setChecked(true);
        } else {
            alternate_drawer_design.setChecked(false);
        }
        alternate_drawer_design.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            prefs.edit().putBoolean("alternate_drawer_design", true).apply();
                            alternate_drawer_design.setChecked(true);
                            Toast toast = Toast.makeText(getContext(), getString(R.string
                                            .substratum_restart_toast),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                            getActivity().recreate();
                        } else {
                            prefs.edit().putBoolean("alternate_drawer_design", false).apply();
                            alternate_drawer_design.setChecked(false);
                            Toast toast = Toast.makeText(getContext(), getString(R.string
                                            .substratum_restart_toast),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                            getActivity().recreate();
                        }
                        return false;
                    }
                });

        final CheckBoxPreference nougat_style_cards = (CheckBoxPreference)
                getPreferenceManager().findPreference("nougat_style_cards");
        if (prefs.getBoolean("nougat_style_cards", false)) {
            nougat_style_cards.setChecked(true);
        } else {
            nougat_style_cards.setChecked(false);
        }
        nougat_style_cards.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            prefs.edit().putBoolean("nougat_style_cards", true).apply();
                            nougat_style_cards.setChecked(true);
                        } else {
                            prefs.edit().putBoolean("nougat_style_cards", false).apply();
                            nougat_style_cards.setChecked(false);
                        }
                        return false;
                    }
                });

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