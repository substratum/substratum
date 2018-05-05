/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import java.io.File;
import java.util.List;

import projekt.substratum.BuildConfig;
import projekt.substratum.LauncherActivity;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.activities.launch.ManageSpaceActivity;
import projekt.substratum.adapters.fragments.settings.ValidatorError;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.util.helpers.TranslatorParser;
import projekt.substratum.util.helpers.ValidatorUtils;
import projekt.substratum.util.views.Lunchbar;
import projekt.substratum.util.views.SheetDialog;

import static projekt.substratum.common.Activities.launchActivityUrl;
import static projekt.substratum.common.Activities.launchExternalActivity;
import static projekt.substratum.common.Internal.SUPPORTED_ROMS_FILE;
import static projekt.substratum.common.Packages.validateResource;
import static projekt.substratum.common.References.ANDROMEDA_PACKAGE;
import static projekt.substratum.common.References.APP_THEME;
import static projekt.substratum.common.References.AUTO_THEME;
import static projekt.substratum.common.References.DARK_THEME;
import static projekt.substratum.common.References.DEFAULT_GRID_COUNT;
import static projekt.substratum.common.References.DEFAULT_THEME;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.References.MAX_PRIORITY;
import static projekt.substratum.common.References.MIN_PRIORITY;
import static projekt.substratum.common.References.SST_ADDON_PACKAGE;
import static projekt.substratum.common.Resources.SYSTEMUI;
import static projekt.substratum.common.Systems.checkAndromeda;
import static projekt.substratum.common.Systems.checkPackageSupport;
import static projekt.substratum.common.Systems.isNewSamsungDevice;
import static projekt.substratum.common.Systems.isNewSamsungDeviceAndromeda;
import static projekt.substratum.common.commands.FileOperations.delete;

public class SettingsFragment extends PreferenceFragmentCompat {

    public StringBuilder platformSummary;
    public Preference systemPlatform;
    public List<ValidatorError> errors;
    public Dialog dialog;
    public Context context;
    private SharedPreferences prefs;

    /**
     * Scroll up the ListView smoothly
     */
    public void scrollUp() {
        getListView().smoothScrollToPosition(0);
    }

    @SuppressLint("StringFormatMatches")
    @Override
    public void onCreatePreferences(
            Bundle bundle,
            String s) {
        context = getContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean isSamsung = Systems.isSamsungDevice(context);
        boolean isOMS = Systems.checkOMS(context);
        boolean hasThemeInterfacer = Systems.checkThemeInterfacer(context);
        boolean hasAndromeda = checkAndromeda(context);

        // Initialize the XML file
        addPreferencesFromResource(R.xml.preference_fragment);

        // About Substratum
        StringBuilder sb = new StringBuilder();
        Preference aboutSubstratum = getPreferenceManager().findPreference("about_substratum");
        sb.append(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ')');
        if (BuildConfig.DEBUG) sb.append(" - " + BuildConfig.GIT_HASH);
        aboutSubstratum.setSummary(sb.toString());
        aboutSubstratum.setIcon(context.getDrawable(R.mipmap.main_launcher));
        aboutSubstratum.setOnPreferenceClickListener(
                preference -> {
                    SheetDialog sheetDialog = new SheetDialog(context);
                    View sheetView =
                            View.inflate(context, R.layout.about_substratum_sheet_dialog, null);
                    LinearLayout translatorsView = sheetView.findViewById(R.id.translators);
                    LinearLayout contributorsView = sheetView.findViewById(R.id.contributors);
                    LinearLayout layersView = sheetView.findViewById(R.id.layers_contributors);
                    LinearLayout githubSourceView = sheetView.findViewById(R.id.github_source);
                    LinearLayout teamView = sheetView.findViewById(R.id.team);
                    translatorsView.setOnClickListener(v -> {
                        new TranslatorParser.TranslatorContributionDialog(this).execute();
                        sheetDialog.cancel();
                    });
                    githubSourceView.setOnClickListener(v -> {
                        try {
                            String sourceURL;
                            if (BuildConfig.DEBUG) {
                                sourceURL = getString(R.string.substratum_github_commits);
                            } else {
                                sourceURL = getString(R.string.substratum_github);
                            }
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(sourceURL));
                            startActivity(i);
                        } catch (ActivityNotFoundException activityNotFoundException) {
                            if (getActivity() != null) {
                                Lunchbar.make(References.getView(getActivity()),
                                        getString(R.string.activity_missing_toast),
                                        Snackbar.LENGTH_LONG)
                                        .show();
                            }
                        }
                        sheetDialog.cancel();
                    });
                    contributorsView.setOnClickListener(v -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setItems(
                                getResources().getStringArray(R.array.substratum_contributors),
                                (dialog, item) -> {
                                });
                        builder.setPositiveButton(
                                android.R.string.ok, (dialog, which) -> dialog.cancel());
                        AlertDialog alert = builder.create();
                        alert.show();
                        sheetDialog.cancel();
                    });
                    layersView.setOnClickListener(v -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setItems(
                                getResources().getStringArray(R.array.layers_contributors),
                                (dialog, item) -> {
                                });
                        builder.setPositiveButton(
                                android.R.string.ok, (dialog, which) -> dialog.cancel());
                        AlertDialog alert = builder.create();
                        alert.show();
                        sheetDialog.cancel();
                    });
                    teamView.setOnClickListener(v -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        View teamViewLayout = View.inflate(context, R.layout.team_dialog, null);
                        CardView nicholasCard = teamViewLayout.findViewById(R.id.nicholas_card);
                        CardView sykoCard = teamViewLayout.findViewById(R.id.syko_card);
                        CardView ivanCard = teamViewLayout.findViewById(R.id.ivan_card);
                        CardView surgeCard = teamViewLayout.findViewById(R.id.surge_card);
                        CardView georgeCard = teamViewLayout.findViewById(R.id.george_card);
                        CardView nathanCard = teamViewLayout.findViewById(R.id.nathan_card);
                        CardView charCard = teamViewLayout.findViewById(R.id.char_card);
                        CardView harshCard = teamViewLayout.findViewById(R.id.harsh_card);

                        nicholasCard.setOnClickListener(v2 ->
                                launchActivityUrl(context, R.string.team_nicholas_link));
                        sykoCard.setOnClickListener(v2 ->
                                launchActivityUrl(context, R.string.team_syko_link));
                        ivanCard.setOnClickListener(v2 ->
                                launchActivityUrl(context, R.string.team_ivan_link));
                        surgeCard.setOnClickListener(v2 ->
                                launchActivityUrl(context, R.string.team_surge_link));
                        georgeCard.setOnClickListener(v2 ->
                                launchActivityUrl(context, R.string.team_george_link));
                        nathanCard.setOnClickListener(v2 ->
                                launchActivityUrl(context, R.string.team_nathan_link));
                        charCard.setOnClickListener(v2 ->
                                launchActivityUrl(context, R.string.team_char_link));
                        harshCard.setOnClickListener(v2 ->
                                launchActivityUrl(context, R.string.team_harsh_link));

                        builder.setView(teamViewLayout);
                        builder.setPositiveButton(
                                android.R.string.ok, (dialog, which) -> dialog.cancel());

                        AlertDialog alert = builder.create();
                        alert.show();
                        sheetDialog.cancel();
                    });
                    sheetDialog.setContentView(sheetView);
                    sheetDialog.show();
                    return false;
                });

        // System Platform
        systemPlatform = getPreferenceManager().findPreference("system_platform");
        if (isOMS && !hasAndromeda) {
            new ValidatorUtils.checkROMSupportList(this).execute(
                    getString(R.string.supported_roms_url),
                    SUPPORTED_ROMS_FILE);
        }
        platformSummary = new StringBuilder();
        platformSummary.append(String.format("%s %s (%s)\n", getString(R.string.android),
                Systems.getProp("ro.build.version.release"), Build.ID));
        platformSummary.append(String.format("%s %s (%s)\n",
                getString(R.string.device), Build.MODEL, Build.DEVICE));
        platformSummary.append(String.format("%s ", getString(R.string
                .settings_about_oms_rro_version)));
        platformSummary.append((isOMS ?
                (Systems.checkOreo() ? getString(R.string.settings_about_oms_version_do) :
                        getString(R.string.settings_about_oms_version_7)) :
                getString(R.string.settings_about_rro_version_2)));
        systemPlatform.setSummary(platformSummary);
        systemPlatform.setIcon(Packages.getAppIcon(context, SYSTEMUI));

        // System Status
        Preference systemStatus = getPreferenceManager().findPreference("system_status");
        boolean full_oms = isOMS && Systems.checkSubstratumFeature(context);
        boolean interfacer = hasThemeInterfacer && !isSamsung;
        boolean system_service = Systems.checkSubstratumService(getContext()) && !isSamsung;
        boolean verified = !checkPackageSupport(context, false);
        boolean certified = verified;
        if (isOMS && interfacer || system_service) {
            certified = verified && full_oms;
        }
        systemStatus.setSummary((interfacer
                ? getString(R.string.settings_system_status_rootless)
                : (system_service ?
                getString(R.string.settings_system_status_ss) :
                (isSamsung ?
                        (isNewSamsungDeviceAndromeda(context) ?
                                getString(R.string.settings_system_status_samsung_andromeda) :
                                getString(R.string.settings_system_status_samsung)) :
                        hasAndromeda ?
                                getString(R.string.settings_system_status_andromeda) :
                                getString(R.string.settings_system_status_rooted))))
                + " (" + (certified ? getString(R.string.settings_system_status_certified) :
                getString(R.string.settings_system_status_uncertified)) + ')'
        );
        systemStatus.setIcon(certified ?
                context.getDrawable(R.drawable.system_status_certified)
                : context.getDrawable(R.drawable.system_status_uncertified));
        if (BuildConfig.DEBUG && isOMS && (interfacer || system_service) && !hasAndromeda) {
            systemStatus.setOnPreferenceClickListener(preference -> {
                if (References.isNetworkAvailable(context)) {
                    new ValidatorUtils.downloadRepositoryList(this).execute("");
                } else if (getView() != null) {
                    Lunchbar.make(getView(),
                            getString(R.string.resource_needs_internet),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
                return false;
            });
        }

        // Force English
        CheckBoxPreference forceEnglish = (CheckBoxPreference)
                getPreferenceManager().findPreference("force_english_locale");
        forceEnglish.setChecked(prefs.getBoolean("force_english_locale", false));
        forceEnglish.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    forceEnglish.setChecked(true);
                    prefs.edit().putBoolean("force_english_locale", (Boolean) newValue).apply();
                    Snackbar lunchbar = Lunchbar.make(getView(),
                            getString(R.string.locale_restart_message),
                            Snackbar.LENGTH_LONG);
                    lunchbar.setAction(getString(R.string.restart), v ->
                            new Handler().postDelayed(() ->
                                    Substratum.restartSubstratum(context), 0));
                    lunchbar.show();
                    return false;
                }
        );

        // Notify on Compiled
        CheckBoxPreference vibrate_on_compiled = (CheckBoxPreference)
                getPreferenceManager().findPreference("vibrate_on_compiled");
        Preference manage_notifications =
                getPreferenceManager().findPreference("manage_notifications");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrate_on_compiled.setVisible(false);
            manage_notifications.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                startActivity(intent);
                return false;
            });
        } else {
            if (manage_notifications != null) manage_notifications.setVisible(false);
            vibrate_on_compiled.setChecked(prefs.getBoolean("vibrate_on_compiled", true));
            vibrate_on_compiled.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        prefs.edit().putBoolean("vibrate_on_compiled", (Boolean) newValue).apply();
                        vibrate_on_compiled.setChecked((Boolean) newValue);
                        return false;
                    });
        }

        // App Theme
        Preference app_theme = getPreferenceManager().findPreference("app_theme");
        String selectedTheme;
        switch (prefs.getString(APP_THEME, DEFAULT_THEME)) {
            case AUTO_THEME:
                selectedTheme =
                        String.format(getString(R.string.app_theme_text),
                                getString(R.string.app_theme_auto));
                break;
            case DARK_THEME:
                selectedTheme =
                        String.format(getString(R.string.app_theme_text),
                                getString(R.string.app_theme_dark));
                break;
            default:
                selectedTheme =
                        String.format(getString(R.string.app_theme_text),
                                getString(R.string.app_theme_disabled));
                break;
        }
        app_theme.setSummary(selectedTheme);
        app_theme.setOnPreferenceClickListener(preference -> {
            SheetDialog sheetDialog = new SheetDialog(context);
            View sheetView = View.inflate(context, R.layout.app_theme_sheet_dialog, null);
            LinearLayout disabled = sheetView.findViewById(R.id.disabled);
            LinearLayout auto = sheetView.findViewById(R.id.automatic);
            LinearLayout dark = sheetView.findViewById(R.id.dark);
            disabled.setOnClickListener(view -> {
                prefs.edit().putString(APP_THEME, DEFAULT_THEME).apply();
                app_theme.setSummary(String.format(getString(R.string.app_theme_text),
                        getString(R.string.app_theme_disabled)));
                sheetDialog.dismiss();
                Snackbar lunchbar = Lunchbar.make(getView(),
                        getString(R.string.app_theme_change),
                        Snackbar.LENGTH_LONG);
                lunchbar.setAction(getString(R.string.restart), v ->
                        new Handler().postDelayed(() -> Substratum.restartSubstratum(context), 0));
                lunchbar.show();
            });
            auto.setOnClickListener(view -> {
                prefs.edit().putString(APP_THEME, AUTO_THEME).apply();
                app_theme.setSummary(String.format(getString(R.string.app_theme_text),
                        getString(R.string.app_theme_auto)));
                sheetDialog.dismiss();
                Snackbar lunchbar = Lunchbar.make(getView(),
                        getString(R.string.app_theme_change),
                        Snackbar.LENGTH_LONG);
                lunchbar.setAction(getString(R.string.restart), v ->
                        new Handler().postDelayed(() -> Substratum.restartSubstratum(context), 0));
                lunchbar.show();
            });
            dark.setOnClickListener(view -> {
                prefs.edit().putString(APP_THEME, DARK_THEME).apply();
                app_theme.setSummary(String.format(getString(R.string.app_theme_text),
                        getString(R.string.app_theme_dark)));
                sheetDialog.dismiss();
                Snackbar lunchbar = Lunchbar.make(getView(),
                        getString(R.string.app_theme_change),
                        Snackbar.LENGTH_LONG);
                lunchbar.setAction(getString(R.string.restart), v ->
                        new Handler().postDelayed(() -> Substratum.restartSubstratum(context), 0));
                lunchbar.show();
            });
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
            return false;
        });

        // Lite Mode
        CheckBoxPreference lite_mode =
                (CheckBoxPreference) getPreferenceManager().findPreference("lite_mode");
        lite_mode.setChecked(prefs.getBoolean("lite_mode", false));
        lite_mode.setOnPreferenceClickListener(preference -> {
            prefs.edit().putBoolean("lite_mode", lite_mode.isChecked()).apply();
            return false;
        });

        // Grid Style Cards Count
        Preference grid_style_cards_count =
                getPreferenceManager().findPreference("grid_style_cards_count");
        String toFormat =
                String.format(getString(R.string.grid_size_text),
                        DEFAULT_GRID_COUNT,
                        prefs.getInt("grid_style_cards_count", DEFAULT_GRID_COUNT));
        grid_style_cards_count.setSummary(toFormat);
        grid_style_cards_count.setOnPreferenceClickListener(
                preference -> {
                    AlertDialog.Builder d = new AlertDialog.Builder(context);
                    d.setTitle(getString(R.string.grid_size_title));

                    NumberPicker numberPicker = new NumberPicker(context);
                    // Maximum overlay priority count
                    numberPicker.setMaxValue(References.MAX_GRID_COUNT);
                    // Minimum overlay priority count
                    numberPicker.setMinValue(References.MIN_GRID_COUNT);
                    // Set the value to the current chosen priority by the user
                    numberPicker.setValue(
                            prefs.getInt("grid_style_cards_count", DEFAULT_GRID_COUNT));
                    // Do not show the soft keyboard
                    numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                    // Do not wrap selector wheel
                    numberPicker.setWrapSelectorWheel(false);

                    d.setView(numberPicker);
                    d.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        Integer new_count = numberPicker.getValue();
                        prefs.edit().putInt(
                                "grid_style_cards_count", new_count).apply();
                        switch (new_count) {
                            case 1:
                                prefs.edit().putBoolean("grid_layout", false).apply();
                                break;
                            default:
                                prefs.edit().putBoolean("grid_layout", true).apply();
                                break;
                        }

                        grid_style_cards_count.setSummary(
                                String.format(
                                        getString(R.string.grid_size_text),
                                        DEFAULT_GRID_COUNT,
                                        new_count));
                    });
                    d.setNegativeButton(android.R.string.cancel, (dialogInterface, i) ->
                            dialogInterface.cancel());
                    d.show();
                    return false;
                }
        );

        // Manage Space Activity
        Preference manageSpace = getPreferenceManager().findPreference("manage_space");
        manageSpace.setOnPreferenceClickListener(
                preference -> {
                    try {
                        startActivity(new Intent(getActivity(), ManageSpaceActivity.class));
                    } catch (ActivityNotFoundException ignored) {
                    }
                    return false;
                });

        // Auto-save LogChar
        CheckBoxPreference autosave_logchar = (CheckBoxPreference)
                getPreferenceManager().findPreference("autosave_logchar");
        autosave_logchar.setChecked(prefs.getBoolean("autosave_logchar", true));
        autosave_logchar.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    boolean isChecked = (Boolean) newValue;
                    if (isChecked) {
                        prefs.edit().putBoolean("autosave_logchar", true).apply();
                        autosave_logchar.setChecked(true);
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(R.string.logchar_dialog_title_delete_title);
                        builder.setMessage(R.string.logchar_dialog_title_delete_content);
                        builder.setNegativeButton(R.string.dialog_cancel,
                                (dialog, id) -> dialog.dismiss());
                        builder.setPositiveButton(R.string.break_compilation_dialog_continue,
                                (dialog, id) -> {
                                    prefs.edit().putBoolean("autosave_logchar", false).apply();
                                    delete(context,
                                            new File(Environment.getExternalStorageDirectory() +
                                                    File.separator + "substratum" + File.separator +
                                                    "LogCharReports").getAbsolutePath());
                                    autosave_logchar.setChecked(false);
                                });
                        builder.show();
                    }
                    return false;
                }
        );

        // Alert if an overlay has been found in a theme after a new app has been installed
        CheckBoxPreference overlay_alert =
                (CheckBoxPreference) getPreferenceManager().findPreference("overlay_alert");
        overlay_alert.setChecked(prefs.getBoolean("overlay_alert", false));
        overlay_alert.setOnPreferenceChangeListener((preference, newValue) -> {
            prefs.edit().putBoolean("overlay_alert", (Boolean) newValue).apply();
            return false;
        });

        // Legacy compiler
        CheckBoxPreference debugTheme =
                (CheckBoxPreference) getPreferenceManager().findPreference("theme_debug");
        debugTheme.setChecked(prefs.getBoolean("theme_debug", false));
        debugTheme.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    boolean isChecked = (Boolean) newValue;
                    if (isChecked) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(R.string.break_compilation_dialog_title);
                        builder.setMessage(R.string.break_compilation_dialog_content);
                        builder.setNegativeButton(R.string.dialog_cancel,
                                (dialog, id) -> dialog.dismiss());
                        builder.setPositiveButton(R.string.break_compilation_dialog_continue,
                                (dialog, id) -> {
                                    prefs.edit().putBoolean("theme_debug", true).apply();
                                    debugTheme.setChecked(true);
                                });
                        builder.show();
                    } else {
                        prefs.edit().putBoolean("theme_debug", false).apply();
                        debugTheme.setChecked(false);
                    }
                    return false;
                }
        );

        // Set the theme system specific preferences without loading through the code
        Preference aboutAndromeda = getPreferenceManager().findPreference("about_andromeda");
        Preference aboutSamsung = getPreferenceManager().findPreference("about_samsung");
        Preference aboutInterfacer = getPreferenceManager().findPreference("about_interfacer");
        CheckBoxPreference showDangerousSamsung = (CheckBoxPreference)
                getPreferenceManager().findPreference("show_dangerous_samsung_overlays");
        Preference priority_switcher =
                getPreferenceManager().findPreference("legacy_priority_switcher");
        CheckBoxPreference crashReceiver = (CheckBoxPreference)
                getPreferenceManager().findPreference("crash_receiver");
        CheckBoxPreference overlay_updater =
                (CheckBoxPreference) getPreferenceManager().findPreference("overlay_updater");
        CheckBoxPreference theme_updater = (CheckBoxPreference)
                getPreferenceManager().findPreference("theme_updater");
        CheckBoxPreference hide_app_checkbox = (CheckBoxPreference)
                getPreferenceManager().findPreference("hide_app_checkbox");
        CheckBoxPreference sungstromeda_mode = (CheckBoxPreference)
                getPreferenceManager().findPreference("sungstromeda_mode");

        hide_app_checkbox.setVisible(false);
        sungstromeda_mode.setVisible(false);

        if (isNewSamsungDevice() && checkAndromeda(context)) {
            sungstromeda_mode.setVisible(true);
            sungstromeda_mode.setChecked(prefs.getBoolean("sungstromeda_mode", true));
            sungstromeda_mode.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        prefs.edit().putBoolean("sungstromeda_mode",
                                (Boolean) newValue).apply();
                        sungstromeda_mode.setChecked((Boolean) newValue);
                        new Handler().postDelayed(() ->
                                Substratum.restartSubstratum(context), 1000L);
                        return false;
                    });
        }

        if (isOMS) {
            aboutSamsung.setVisible(false);
            showDangerousSamsung.setVisible(false);
            priority_switcher.setVisible(false);

            // Crash Receiver should only show if the app is running in debug mode
            if (BuildConfig.DEBUG) {
                crashReceiver.setChecked(prefs.getBoolean("crash_receiver", true));
                crashReceiver.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean isChecked = (Boolean) newValue;
                    if (!isChecked) {
                        AlertDialog.Builder builder =
                                new AlertDialog.Builder(context);
                        builder.setTitle(R.string.theme_safety_dialog_title);
                        builder.setMessage(R.string.theme_safety_dialog_content);
                        builder.setNegativeButton(R.string.dialog_cancel,
                                (dialog, id) -> dialog.dismiss());
                        builder.setPositiveButton(
                                R.string.break_compilation_dialog_continue,
                                (dialog, id) -> {
                                    crashReceiver.setChecked(false);
                                    prefs.edit().putBoolean("crash_receiver", false).apply();
                                });
                        builder.show();
                    } else {
                        crashReceiver.setChecked(true);
                        prefs.edit().putBoolean("crash_receiver", true).apply();
                    }
                    return false;
                });
                if (!Systems.checkSubstratumFeature(context)) {
                    crashReceiver.setVisible(false);
                }
            }

            // About Andromeda
            if (Systems.isAndromedaDevice(context)) {
                aboutAndromeda.setIcon(Packages.getAppIcon(context, ANDROMEDA_PACKAGE));
                try {
                    PackageInfo info =
                            context.getPackageManager().getPackageInfo(ANDROMEDA_PACKAGE, 0);
                    aboutAndromeda.setSummary(info.versionName + " (" + info.versionCode + ')');
                } catch (Exception ignored) {
                }
                aboutAndromeda.setOnPreferenceClickListener(preference -> {
                    launchExternalActivity(context, ANDROMEDA_PACKAGE,
                            Packages.getAppVersionCode(context, ANDROMEDA_PACKAGE) > 19
                                    ? "activities.InfoActivity" : "InfoActivity");
                    return false;
                });
            } else {
                aboutAndromeda.setVisible(false);
            }

            // About Interfacer
            if (hasThemeInterfacer) {
                aboutInterfacer.setIcon(Packages.getAppIcon(context, INTERFACER_PACKAGE));
                aboutInterfacer.setOnPreferenceClickListener(
                        preference -> {
                            try {
                                int sourceURL;
                                if (BuildConfig.DEBUG) {
                                    sourceURL = R.string.interfacer_github_commits;
                                } else {
                                    sourceURL = R.string.interfacer_github;
                                }
                                launchActivityUrl(context, sourceURL);
                            } catch (ActivityNotFoundException activityNotFoundException) {
                                if (getActivity() != null) {
                                    Lunchbar.make(References.getView(getActivity()),
                                            getString(R.string.activity_missing_toast),
                                            Snackbar.LENGTH_LONG)
                                            .show();
                                }
                            }
                            return false;
                        });
                try {
                    PackageInfo pInfo = Systems.getThemeInterfacerPackage(context);
                    if (pInfo != null) {
                        aboutInterfacer.setSummary(
                                pInfo.versionName + " (" + pInfo.versionCode + ')');
                    }
                } catch (Exception e) {
                    // Suppress exception
                }
            } else {
                aboutInterfacer.setVisible(false);
            }

            // Check if the system actually supports the latest Interfacer and the permission
            // found at:
            // https://github.com/substratum/interfacer/blob/n-rootless/projekt.substratum.theme.xml
            if (!Systems.checkSubstratumFeature(context)) {
                aboutInterfacer.setVisible(false);
            }

            // Hide App in Launcher
            hide_app_checkbox.setChecked(prefs.getBoolean("show_app_icon", true));
            if (validateResource(context,
                    References.settingsPackageName,
                    References.settingsSubstratumDrawableName,
                    "drawable")) {
                hide_app_checkbox.setSummary(getString(R.string.hide_app_icon_supported));
                hide_app_checkbox.setVisible(true);
            }
            hide_app_checkbox.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            prefs.edit().putBoolean("show_app_icon", true).apply();
                            PackageManager p = context.getPackageManager();
                            ComponentName componentName = new ComponentName(context,
                                    LauncherActivity.class);
                            p.setComponentEnabledSetting(
                                    componentName,
                                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                    PackageManager.DONT_KILL_APP);
                            if (getView() != null) {
                                Lunchbar.make(getView(),
                                        getString(R.string.hide_app_icon_toast_disabled),
                                        Snackbar.LENGTH_LONG)
                                        .show();
                            }
                            hide_app_checkbox.setChecked(true);
                        } else {
                            prefs.edit().putBoolean("show_app_icon", false).apply();
                            PackageManager p = context.getPackageManager();
                            ComponentName componentName = new ComponentName(context,
                                    LauncherActivity.class);
                            p.setComponentEnabledSetting(componentName,
                                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                    PackageManager.DONT_KILL_APP);
                            if (getView() != null) {
                                Lunchbar.make(getView(),
                                        getString(R.string.hide_app_icon_toast_enabled),
                                        Snackbar.LENGTH_LONG)
                                        .show();
                            }
                            hide_app_checkbox.setChecked(false);
                        }
                        return false;
                    }
            );

            // Optional toggle to allow the target's overlays to automatically update
            overlay_updater.setChecked(prefs.getBoolean("overlay_updater", false));
            overlay_updater.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        prefs.edit().putBoolean("overlay_updater", (Boolean) newValue).apply();
                        overlay_updater.setChecked(false);
                        return false;
                    });

            // Optional toggle to allow the theme overlays to automatically update
            theme_updater.setChecked(prefs.getBoolean("theme_updater", false));
            theme_updater.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle(R.string.settings_theme_auto_updater_dialog_title);
                            builder.setMessage(R.string.settings_theme_auto_updater_dialog_text);
                            builder.setNegativeButton(R.string.dialog_cancel,
                                    (dialog, id) -> dialog.dismiss());
                            builder.setPositiveButton(R.string.break_compilation_dialog_continue,
                                    (dialog, id) -> {
                                        prefs.edit().putBoolean("theme_updater", true).apply();
                                        theme_updater.setChecked(true);
                                    });
                            builder.show();
                        } else {
                            prefs.edit().putBoolean("theme_updater", false).apply();
                            theme_updater.setChecked(false);
                        }
                        return false;
                    }
            );
        } else {
            aboutAndromeda.setVisible(Systems.isNewSamsungDeviceAndromeda(context));
            aboutInterfacer.setVisible(false);
            crashReceiver.setVisible(false);
            overlay_updater.setVisible(false);
            theme_updater.setVisible(false);

            // Sungstromeda mode
            if (isNewSamsungDeviceAndromeda(context)) {
                aboutAndromeda.setIcon(Packages.getAppIcon(context, ANDROMEDA_PACKAGE));
                sungstromeda_mode.setVisible(true);
                try {
                    PackageInfo info =
                            context.getPackageManager().getPackageInfo(ANDROMEDA_PACKAGE, 0);
                    aboutAndromeda.setSummary(info.versionName + " (" + info.versionCode + ')');
                } catch (Exception ignored) {
                }
                aboutAndromeda.setOnPreferenceClickListener(preference -> {
                    launchExternalActivity(context, ANDROMEDA_PACKAGE,
                            Packages.getAppVersionCode(context, ANDROMEDA_PACKAGE) > 19
                                    ? "activities.InfoActivity" : "InfoActivity");
                    return false;
                });
            }

            // About Samsung
            if (isSamsung && !Systems.isNewSamsungDeviceAndromeda(context)) {
                aboutSamsung.setVisible(true);
                aboutSamsung.setIcon(Packages.getAppIcon(context, SST_ADDON_PACKAGE));
                try {
                    PackageInfo info = context.getPackageManager().getPackageInfo(SST_ADDON_PACKAGE, 0);
                    aboutSamsung.setSummary(info.versionName + " (" + info.versionCode + ')');
                } catch (Exception ignored) {
                }
                showDangerousSamsung.setChecked(
                        prefs.getBoolean("show_dangerous_samsung_overlays", false));
                showDangerousSamsung.setOnPreferenceChangeListener(
                        (preference, newValue) -> {
                            boolean isChecked = (Boolean) newValue;
                            if (isChecked) {
                                AlertDialog.Builder builder =
                                        new AlertDialog.Builder(context);
                                builder.setTitle(
                                        R.string.settings_samsung_show_dangerous_overlays_warning_title);
                                builder.setMessage(
                                        R.string.settings_samsung_show_dangerous_overlays_warning);
                                builder.setNegativeButton(R.string.dialog_cancel,
                                        (dialog, id) -> dialog.dismiss());
                                builder.setPositiveButton(
                                        R.string.break_compilation_dialog_continue,
                                        (dialog, id) -> {
                                            prefs.edit().putBoolean(
                                                    "show_dangerous_samsung_overlays", true).apply();
                                            showDangerousSamsung.setChecked(true);
                                        });
                                builder.show();
                            } else {
                                prefs.edit().putBoolean(
                                        "show_dangerous_samsung_overlays", false).apply();
                                showDangerousSamsung.setChecked(false);
                            }
                            return false;
                        }
                );
            } else {
                aboutSamsung.setVisible(false);
            }

            @SuppressLint("StringFormatMatches") String formatted =
                    String.format(getString(R.string.legacy_preference_priority_text),
                            References.DEFAULT_PRIORITY,
                            prefs.getInt("legacy_overlay_priority", References.DEFAULT_PRIORITY));
            priority_switcher.setSummary(formatted);
            priority_switcher.setOnPreferenceClickListener(
                    preference -> {
                        AlertDialog.Builder d = new AlertDialog.Builder(context);
                        d.setTitle(getString(R.string.legacy_preference_priority_title));

                        NumberPicker numberPicker = new NumberPicker(context);
                        // Maximum overlay priority count
                        numberPicker.setMaxValue(MAX_PRIORITY);
                        // Minimum overlay priority count
                        numberPicker.setMinValue(MIN_PRIORITY);
                        // Set the value to the current chosen priority by the user
                        numberPicker.setValue(prefs.getInt("legacy_overlay_priority",
                                References.DEFAULT_PRIORITY));
                        // Do not wrap selector wheel
                        numberPicker.setWrapSelectorWheel(false);

                        d.setView(numberPicker);
                        d.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                            Integer new_priority = numberPicker.getValue();
                            prefs.edit().putInt(
                                    "legacy_overlay_priority", new_priority).apply();
                            priority_switcher.setSummary(
                                    String.format(
                                            getString(R.string
                                                    .legacy_preference_priority_text),
                                            References.DEFAULT_PRIORITY,
                                            new_priority));
                        });
                        d.setNegativeButton(android.R.string.cancel, (di, i) -> di.cancel());
                        d.show();
                        return false;
                    }
            );
        }
    }
}