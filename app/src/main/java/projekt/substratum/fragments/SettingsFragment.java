package projekt.substratum.fragments;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.File;

import projekt.substratum.BuildConfig;
import projekt.substratum.LauncherActivity;
import projekt.substratum.R;
import projekt.substratum.config.References;
import projekt.substratum.util.AOPTCheck;

public class SettingsFragment extends PreferenceFragmentCompat {

    private ProgressDialog mProgressDialog;

    private boolean checkSettingsPackageSupport() {
        try {
            Resources res = getContext().getApplicationContext().getPackageManager()
                    .getResourcesForApplication(References.settingsPackageName);
            int substratum_icon = res.getIdentifier(References.settingsPackageName + ":drawable/" +
                            References.settingsSubstratumDrawableName, "drawable",
                    References.settingsPackageName);
            return substratum_icon != 0;
        } catch (Exception e) {
            Log.e(References.SUBSTRATUM_LOG, "Could not load drawable from Settings.apk.");
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
                preference -> {
                    try {
                        String sourceURL = getString(R.string.substratum_github);
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(sourceURL));
                        startActivity(i);
                    } catch (ActivityNotFoundException activityNotFoundException) {
                        //
                    }
                    return false;
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

        final Preference aoptSwitcher = getPreferenceManager().findPreference
                ("aopt_switcher");
        if (prefs.getString("compiler", "aapt").equals("aapt")) {
            aoptSwitcher.setSummary(R.string.settings_aapt);
        } else {
            aoptSwitcher.setSummary(R.string.settings_aopt);
        }
        aoptSwitcher.setOnPreferenceClickListener(
                preference -> {
                    final AlertDialog.Builder builderSingle = new AlertDialog.Builder
                            (getContext());
                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(),
                            R.layout.dialog_listview);

                    arrayAdapter.add(getString(R.string.settings_aapt));
                    arrayAdapter.add(getString(R.string.settings_aopt));

                    builderSingle.setNegativeButton(
                            android.R.string.cancel,
                            (dialog, which) -> dialog.dismiss());

                    builderSingle.setAdapter(arrayAdapter, (dialog, which) -> {
                        SharedPreferences prefs1 =
                                PreferenceManager.getDefaultSharedPreferences(getContext());
                        switch (which) {
                            case 0:
                                prefs1.edit().remove("compiler").apply();
                                prefs1.edit().putString("compiler", "aapt").apply();
                                aoptSwitcher.setSummary(R.string.settings_aapt);
                                new AOPTCheck().injectAOPT(getContext(), true);
                                break;
                            case 1:
                                prefs1.edit().remove("compiler").apply();
                                prefs1.edit().putString("compiler", "aopt").apply();
                                aoptSwitcher.setSummary(R.string.settings_aopt);
                                new AOPTCheck().injectAOPT(getContext(), true);
                                break;
                        }
                    });
                    builderSingle.show();
                    return false;
                });

        if (References.checkOMS(getContext())) {
            Preference aboutMasquerade = getPreferenceManager().findPreference
                    ("about_masquerade");
            aboutMasquerade.setIcon(getContext().getDrawable(R.mipmap.restore_launcher));
            aboutMasquerade.setOnPreferenceClickListener(
                    preference -> {
                        try {
                            String sourceURL = getString(R.string.masquerade_github);
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(sourceURL));
                            startActivity(i);
                        } catch (ActivityNotFoundException activityNotFoundException) {
                            //
                        }
                        return false;
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
                    preference -> {
                        if (References.isPackageInstalled(getContext(),
                                "masquerade.substratum")) {
                            Intent runCommand = new Intent();
                            runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                            runCommand.setAction("masquerade.substratum.COMMANDS");
                            runCommand.putExtra("substratum-check", "masquerade-ball");
                            getContext().sendBroadcast(runCommand);
                        } else {
                            Snackbar.make(getView(),
                                    getString(R.string.masquerade_check_not_installed),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        return false;
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
                    (preference, newValue) -> {
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
                            Snackbar.make(getView(),
                                    getString(R.string.hide_app_icon_toast_disabled),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                            hide_app_checkbox.setChecked(true);
                        } else {
                            prefs.edit().putBoolean("show_app_icon", false).apply();
                            PackageManager p = getContext().getPackageManager();
                            ComponentName componentName = new ComponentName(getContext(),
                                    LauncherActivity.class);
                            p.setComponentEnabledSetting(componentName, PackageManager
                                    .COMPONENT_ENABLED_STATE_DISABLED, PackageManager
                                    .DONT_KILL_APP);

                            Snackbar.make(getView(),
                                    getString(R.string.hide_app_icon_toast_enabled),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                            hide_app_checkbox.setChecked(false);
                        }
                        return false;
                    });

            final CheckBoxPreference systemUIRestart = (CheckBoxPreference)
                    getPreferenceManager().findPreference("restart_systemui");
            if (prefs.getBoolean("systemui_recreate", true)) {
                systemUIRestart.setChecked(true);
            } else {
                systemUIRestart.setChecked(false);
            }
            systemUIRestart.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            prefs.edit().putBoolean("systemui_recreate", true).apply();
                            Snackbar.make(getView(),
                                    getString(R.string.restart_systemui_toast_enabled),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                            systemUIRestart.setChecked(true);
                        } else {
                            prefs.edit().putBoolean("systemui_recreate", false).apply();
                            Snackbar.make(getView(),
                                    getString(R.string.restart_systemui_toast_disabled),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                            systemUIRestart.setChecked(false);
                        }
                        return false;
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
                    (preference, newValue) -> {
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
                    });
        }

        final Preference purgeCache = getPreferenceManager().findPreference("purge_cache");
        purgeCache.setOnPreferenceClickListener(
                preference -> {
                    new deleteCache().execute("");
                    return false;
                });

        final CheckBoxPreference show_outdated_themes = (CheckBoxPreference)
                getPreferenceManager().findPreference("show_outdated_themes");
        if (prefs.getBoolean("display_old_themes", true)) {
            show_outdated_themes.setChecked(true);
        } else {
            show_outdated_themes.setChecked(false);
        }
        show_outdated_themes.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    boolean isChecked = (Boolean) newValue;
                    if (isChecked) {
                        prefs.edit().putBoolean("display_old_themes", true).apply();
                        show_outdated_themes.setChecked(true);
                    } else {
                        prefs.edit().putBoolean("display_old_themes", false).apply();
                        show_outdated_themes.setChecked(false);
                    }
                    return false;
                });

        final CheckBoxPreference alternate_drawer_design = (CheckBoxPreference)
                getPreferenceManager().findPreference("alternate_drawer_design");
        if (prefs.getBoolean("alternate_drawer_design", false)) {
            alternate_drawer_design.setChecked(true);
        } else {
            alternate_drawer_design.setChecked(false);
        }
        alternate_drawer_design.setOnPreferenceChangeListener(
                (preference, newValue) -> {
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
                });

        final CheckBoxPreference nougat_style_cards = (CheckBoxPreference)
                getPreferenceManager().findPreference("nougat_style_cards");
        if (prefs.getBoolean("nougat_style_cards", false)) {
            nougat_style_cards.setChecked(true);
        } else {
            nougat_style_cards.setChecked(false);
        }
        nougat_style_cards.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    boolean isChecked = (Boolean) newValue;
                    if (isChecked) {
                        prefs.edit().putBoolean("nougat_style_cards", true).apply();
                        nougat_style_cards.setChecked(true);
                    } else {
                        prefs.edit().putBoolean("nougat_style_cards", false).apply();
                        nougat_style_cards.setChecked(false);
                    }
                    return false;
                });

        final CheckBoxPreference vibrate_on_compiled = (CheckBoxPreference)
                getPreferenceManager().findPreference("vibrate_on_compiled");
        if (prefs.getBoolean("vibrate_on_compiled", true)) {
            vibrate_on_compiled.setChecked(true);
        } else {
            vibrate_on_compiled.setChecked(false);
        }
        vibrate_on_compiled.setOnPreferenceChangeListener(
                (preference, newValue) -> {
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
                });

        final CheckBoxPreference show_template_version = (CheckBoxPreference)
                getPreferenceManager().findPreference("show_template_version");
        if (prefs.getBoolean("show_template_version", true)) {
            show_template_version.setChecked(true);
        } else {
            show_template_version.setChecked(false);
        }
        show_template_version.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    boolean isChecked = (Boolean) newValue;
                    if (isChecked) {
                        prefs.edit().putBoolean("show_template_version", true).apply();
                        show_template_version.setChecked(true);
                    } else {
                        prefs.edit().putBoolean("show_template_version", false).apply();
                        show_template_version.setChecked(false);
                    }
                    return false;
                });

        final CheckBoxPreference dynamic_actionbar = (CheckBoxPreference)
                getPreferenceManager().findPreference("dynamic_actionbar");
        if (prefs.getBoolean("dynamic_actionbar", true)) {
            dynamic_actionbar.setChecked(true);
        } else {
            dynamic_actionbar.setChecked(false);
        }
        dynamic_actionbar.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    boolean isChecked = (Boolean) newValue;
                    if (isChecked) {
                        prefs.edit().putBoolean("dynamic_actionbar", true).apply();
                        dynamic_actionbar.setChecked(true);
                    } else {
                        prefs.edit().putBoolean("dynamic_actionbar", false).apply();
                        dynamic_actionbar.setChecked(false);
                    }
                    return false;
                });

        final CheckBoxPreference dynamic_navbar = (CheckBoxPreference)
                getPreferenceManager().findPreference("dynamic_navbar");
        if (prefs.getBoolean("dynamic_navbar", true)) {
            dynamic_navbar.setChecked(true);
        } else {
            dynamic_navbar.setChecked(false);
        }
        dynamic_navbar.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    boolean isChecked = (Boolean) newValue;
                    if (isChecked) {
                        prefs.edit().putBoolean("dynamic_navbar", true).apply();
                        dynamic_navbar.setChecked(true);
                    } else {
                        prefs.edit().putBoolean("dynamic_navbar", false).apply();
                        dynamic_navbar.setChecked(false);
                    }
                    return false;
                });
    }

    public class deleteCache extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(getContext());
            mProgressDialog.setMessage(getString(R.string.substratum_cache_clear_initial_toast));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            // Clear the notification of building theme if shown
            NotificationManager manager = (NotificationManager)
                    getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(References.notification_id);
        }

        @Override
        protected void onPostExecute(String result) {
            // Since the cache is invalidated, better relaunch the app now
            mProgressDialog.cancel();
            getActivity().finish();
            startActivity(getActivity().getIntent());
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Delete the directory
            try {
                File dir = new File(getContext().getCacheDir().getAbsolutePath() +
                        "/SubstratumBuilder/");
                deleteDir(dir);
            } catch (Exception e) {
                // Suppress warning
            }
            // Reset the flag for is_updating
            SharedPreferences prefsPrivate =
                    getContext().getSharedPreferences("substratum_state",
                            Context.MODE_PRIVATE);
            prefsPrivate.edit().remove("is_updating").apply();
            return null;
        }

        boolean deleteDir(File dir) {
            if (dir != null && dir.isDirectory()) {
                String[] children = dir.list();
                for (String aChildren : children) {
                    boolean success = deleteDir(new File(dir, aChildren));
                    if (!success) {
                        return false;
                    }
                }
                return dir.delete();
            } else
                return dir != null && dir.isFile() && dir.delete();
        }
    }
}