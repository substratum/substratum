/*
 * Copyright (c) 2016-2017 Projekt Substratum
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Lunchbar;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.BuildConfig;
import projekt.substratum.LauncherActivity;
import projekt.substratum.R;
import projekt.substratum.activities.launch.ManageSpaceActivity;
import projekt.substratum.adapters.fragments.settings.Repository;
import projekt.substratum.adapters.fragments.settings.ValidatorAdapter;
import projekt.substratum.adapters.fragments.settings.ValidatorError;
import projekt.substratum.adapters.fragments.settings.ValidatorFilter;
import projekt.substratum.adapters.fragments.settings.ValidatorInfo;
import projekt.substratum.common.References;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.systems.Validator;
import projekt.substratum.util.files.FileDownloader;
import projekt.substratum.util.injectors.CheckBinaries;
import projekt.substratum.util.readers.ReadFilterFile;
import projekt.substratum.util.readers.ReadRepositoriesFile;
import projekt.substratum.util.readers.ReadResourcesFile;
import projekt.substratum.util.views.SheetDialog;

import static android.content.Context.NOTIFICATION_SERVICE;
import static projekt.substratum.common.ActivityManagement.launchExternalActivity;
import static projekt.substratum.common.References.ANDROMEDA_PACKAGE;
import static projekt.substratum.common.References.HIDDEN_CACHING_MODE_TAP_COUNT;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.References.INTERFACER_SERVICE;
import static projekt.substratum.common.References.SST_ADDON_PACKAGE;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;
import static projekt.substratum.common.References.SUBSTRATUM_VALIDATOR;
import static projekt.substratum.common.References.validateResource;
import static projekt.substratum.common.commands.FileOperations.delete;
import static projekt.substratum.common.systems.Validator.VALIDATE_WITH_LOGS;

public class SettingsFragment extends PreferenceFragmentCompat {

    private ProgressDialog mProgressDialog;
    private StringBuilder platformSummary;
    private Preference systemPlatform;
    private ArrayList<ValidatorError> errors;
    private Dialog dialog;
    private int tapCount = 0;
    private ArrayList<Integer> packageCounters;
    private ArrayList<Integer> packageCountersErrored;
    private Context mContext;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        mContext = getContext();

        if (References.checkOMS(mContext)) {
            addPreferencesFromResource(R.xml.preference_fragment);
        } else {
            addPreferencesFromResource(R.xml.legacy_preference_fragment);
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        StringBuilder sb = new StringBuilder();

        Preference aboutSubstratum = getPreferenceManager().findPreference
                ("about_substratum");
        sb.append(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        if (BuildConfig.DEBUG) {
            sb.append(" - " + BuildConfig.GIT_HASH);
        }
        aboutSubstratum.setSummary(sb.toString());
        aboutSubstratum.setIcon(mContext.getDrawable(R.mipmap.main_launcher));
        aboutSubstratum.setOnPreferenceClickListener(
                preference -> {
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
                        Lunchbar.make(getActivity().findViewById(android.R.id.content),
                                getString(R.string.activity_missing_toast),
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                    return false;
                });

        systemPlatform = getPreferenceManager().findPreference("system_platform");

        if (References.checkOMS(mContext)) {
            new checkROMSupportList(this).execute(
                    getString(R.string.supported_roms_url),
                    "supported_roms.xml");
        }

        platformSummary = new StringBuilder();
        platformSummary.append(String.format("%s %s (%s)\n", getString(R.string.android),
                References.getProp("ro.build.version.release"), Build.ID));
        platformSummary.append(String.format("%s %s (%s)\n",
                getString(R.string.device), Build.MODEL, Build.DEVICE));
        platformSummary.append(String.format("%s ", getString(R.string
                .settings_about_oms_rro_version)));
        platformSummary.append(((References.checkOMS(mContext)) ?
                (References.checkOreo() ? getString(R.string.settings_about_oms_version_do) :
                        getString(R.string.settings_about_oms_version_7)) :
                getString(R.string.settings_about_rro_version_2)));
        systemPlatform.setSummary(platformSummary);
        systemPlatform.setIcon(References.grabAppIcon(mContext, "com.android.systemui"));

        Preference systemStatus = getPreferenceManager().findPreference("system_status");
        boolean full_oms = References.checkOMS(mContext) &&
                References.checkSubstratumFeature(mContext);
        boolean interfacer = References.checkThemeInterfacer(mContext) &&
                !References.isSamsung(mContext);
        boolean verified = prefs.getBoolean("complexion", true);
        boolean certified = verified;
        if (References.checkOMS(mContext)) {
            if (interfacer) {
                certified = verified && full_oms;
            } else {
                certified = verified;
            }
        }

        systemStatus.setSummary((interfacer
                ? getString(R.string.settings_system_status_rootless)
                : (References.isSamsung(mContext) ?
                getString(R.string.settings_system_status_samsung) :
                (References.checkAndromeda(mContext) ?
                        getString(R.string.settings_system_status_andromeda) :
                        getString(R.string.settings_system_status_rooted)))
                + " (" + (certified ? getString(R.string.settings_system_status_certified) :
                getString(R.string.settings_system_status_uncertified)) + ")"
        ));
        systemStatus.setIcon(certified ?
                mContext.getDrawable(R.drawable.system_status_certified)
                : mContext.getDrawable(R.drawable.system_status_uncertified));
        if (BuildConfig.DEBUG &&
                References.checkOMS(mContext) &&
                !References.checkAndromeda(mContext)) {
            systemStatus.setOnPreferenceClickListener(preference -> {
                if (References.isNetworkAvailable(mContext)) {
                    new downloadRepositoryList(this).execute("");
                } else if (getView() != null) {
                    Lunchbar.make(getView(),
                            R.string.resource_needs_internet,
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
                return false;
            });
        }

        final Preference aboutSamsung = getPreferenceManager().findPreference("about_samsung");
        final CheckBoxPreference showDangerousSamsung = (CheckBoxPreference)
                getPreferenceManager().findPreference("show_dangerous_samsung_overlays");

        if (References.isSamsung(mContext)) {
            aboutSamsung.setVisible(true);
            aboutSamsung.setIcon(References.grabAppIcon(mContext, SST_ADDON_PACKAGE));
            try {
                PackageInfo info = mContext.getPackageManager()
                        .getPackageInfo(SST_ADDON_PACKAGE, 0);
                String versionName = info.versionName;
                int versionCode = info.versionCode;
                aboutSamsung.setSummary(versionName + " (" + versionCode + ")");
            } catch (Exception e) {
                // Suppress exception
            }
            aboutSamsung.setOnPreferenceClickListener(preference -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(
                            new ComponentName(SST_ADDON_PACKAGE,
                                    "projekt.knox.theme.system.MainActivity"));
                    startActivity(intent);
                } catch (Exception e) {
                    Lunchbar.make(getActivity().findViewById(android.R.id.content),
                            getString(R.string.activity_missing_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
                return false;
            });

            boolean dangerous_samsung_overlays =
                    prefs.getBoolean("show_dangerous_samsung_overlays", false);
            if (dangerous_samsung_overlays) {
                showDangerousSamsung.setChecked(true);
            } else {
                showDangerousSamsung.setChecked(false);
            }
            showDangerousSamsung.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            final AlertDialog.Builder builder =
                                    new AlertDialog.Builder(mContext);
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
                            prefs.edit().putBoolean("show_dangerous_samsung_overlays", false)
                                    .apply();
                            showDangerousSamsung.setChecked(false);
                        }
                        return false;
                    });
        } else if (!References.checkOMS(mContext)) {
            aboutSamsung.setVisible(false);
            showDangerousSamsung.setVisible(false);
        }

        final CheckBoxPreference forceEnglish = (CheckBoxPreference)
                getPreferenceManager().findPreference("force_english_locale");
        boolean force = prefs.getBoolean("force_english", false);
        if (force) {
            forceEnglish.setChecked(true);
        } else {
            forceEnglish.setChecked(false);
        }
        forceEnglish.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    boolean isChecked = (Boolean) newValue;
                    if (isChecked) {
                        forceEnglish.setChecked(true);
                        Toast.makeText(mContext,
                                getString(R.string.settings_force_english_toast_success),
                                Toast.LENGTH_SHORT).show();
                        prefs.edit().putBoolean("force_english", true).apply();
                        getActivity().recreate();
                    } else {
                        forceEnglish.setChecked(false);
                        Toast.makeText(mContext,
                                getString(R.string.settings_force_english_toast_reverted),
                                Toast.LENGTH_SHORT).show();
                        prefs.edit().putBoolean("force_english", false).apply();
                        getActivity().recreate();
                    }
                    return false;
                });

        final Preference purgeCache = getPreferenceManager().findPreference("purge_cache");
        purgeCache.setOnPreferenceClickListener(
                preference -> {
                    new deleteCache(this).execute("");
                    return false;
                });
        if (!References.isCachingEnabled(mContext)) purgeCache.setVisible(false);

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
                        Toast.makeText(mContext,
                                getString(R.string.substratum_restart_toast),
                                Toast.LENGTH_SHORT).show();
                        getActivity().recreate();
                    } else {
                        prefs.edit().putBoolean("alternate_drawer_design", false).apply();
                        alternate_drawer_design.setChecked(false);
                        Toast.makeText(mContext,
                                getString(R.string.substratum_restart_toast),
                                Toast.LENGTH_SHORT).show();
                        getActivity().recreate();
                    }
                    return false;
                });

        final CheckBoxPreference nougat_style_cards = (CheckBoxPreference)
                getPreferenceManager().findPreference("nougat_style_cards");
        if (prefs.getBoolean("nougat_style_cards", true)) {
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
        final Preference manage_notifications =
                getPreferenceManager().findPreference("manage_notifications");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrate_on_compiled.setVisible(false);
            manage_notifications.setOnPreferenceClickListener(preference -> {
                NotificationManager notificationManager =
                        (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
                List<NotificationChannel> channels = null;
                if (notificationManager != null) {
                    channels = notificationManager.getNotificationChannels();
                }
                CharSequence[] channelNames = new CharSequence[0];
                if (channels != null) {
                    channelNames = new CharSequence[channels.size()];
                    for (int i = 0; i < channels.size(); i++) {
                        channelNames[i] = channels.get(i).getName();
                    }
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.manage_notification_channel_dialog_title);
                builder.setNegativeButton(android.R.string.cancel, (dialog, i) -> dialog.cancel());
                List<NotificationChannel> finalChannels = channels;
                builder.setItems(channelNames, (dialog, i) -> {
                    Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, mContext.getPackageName());
                    if (finalChannels != null) {
                        intent.putExtra(Settings.EXTRA_CHANNEL_ID, finalChannels.get(i).getId());
                    }
                    startActivity(intent);
                });
                builder.create().show();
                return false;
            });
        } else {
            manage_notifications.setVisible(false);
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
        }

        final CheckBoxPreference show_template_version = (CheckBoxPreference)
                getPreferenceManager().findPreference("show_template_version");
        if (prefs.getBoolean("show_template_version", false)) {
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
        if (prefs.getInt("grid_style_cards_count", References.DEFAULT_GRID_COUNT) > 1) {
            show_template_version.setVisible(false);
        }

        Preference grid_style_cards_count =
                getPreferenceManager().findPreference("grid_style_cards_count");
        String toFormat =
                String.format(getString(R.string.grid_size_text),
                        prefs.getInt("grid_style_cards_count", References.DEFAULT_GRID_COUNT));
        grid_style_cards_count.setSummary(toFormat);
        grid_style_cards_count.setOnPreferenceClickListener(
                preference -> {
                    AlertDialog.Builder d = new AlertDialog.Builder(mContext);
                    d.setTitle(getString(R.string.grid_size_title));

                    NumberPicker numberPicker = new NumberPicker(mContext);
                    // Maximum overlay priority count
                    numberPicker.setMaxValue(References.MAX_GRID_COUNT);
                    // Minimum overlay priority count
                    numberPicker.setMinValue(References.MIN_GRID_COUNT);
                    // Set the value to the current chosen priority by the user
                    numberPicker.setValue(prefs.getInt("grid_style_cards_count",
                            References.DEFAULT_GRID_COUNT));
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
                                show_template_version.setVisible(true);
                                break;
                            default:
                                prefs.edit().putBoolean("grid_layout", true).apply();
                                show_template_version.setVisible(false);
                        }

                        grid_style_cards_count.setSummary(
                                String.format(
                                        getString(R.string.grid_size_text),
                                        new_count));
                    });
                    d.setNegativeButton(android.R.string.cancel, (dialogInterface, i) ->
                            dialogInterface.cancel());
                    d.show();
                    return false;
                });

        final CheckBoxPreference themeCaching = (CheckBoxPreference)
                getPreferenceManager().findPreference("theme_caching");
        boolean to_show = prefs.getBoolean("caching_enabled", false);
        themeCaching.setChecked(to_show);
        themeCaching.setVisible(to_show);
        themeCaching.setOnPreferenceChangeListener(((preference, newValue) -> {
            boolean isChecked = (Boolean) newValue;
            prefs.edit().putBoolean("caching_enabled", isChecked).apply();
            purgeCache.setVisible(isChecked);
            new deleteCache(this).execute();
            return true;
        }));

        // These should run if the app is running in debug mode
        final Preference aoptSwitcher = getPreferenceManager().findPreference
                ("aopt_switcher");
        final CheckBoxPreference crashReceiver = (CheckBoxPreference)
                getPreferenceManager().findPreference("crash_receiver");

        if (BuildConfig.DEBUG) {
            if (prefs.getString("compiler", "aapt").equals("aapt")) {
                aoptSwitcher.setSummary(R.string.settings_aapt);
            } else {
                aoptSwitcher.setSummary(R.string.settings_aopt);
            }
            aoptSwitcher.setOnPreferenceClickListener(
                    preference -> {
                        SheetDialog sheetDialog =
                                new SheetDialog(mContext);
                        View sheetView = View.inflate(mContext,
                                R.layout.aopt_sheet_dialog, null);

                        LinearLayout aapt = sheetView.findViewById(R.id.aapt);
                        LinearLayout aopt = sheetView.findViewById(R.id.aopt);
                        aapt.setOnClickListener(v -> {
                            prefs.edit().remove("compiler").apply();
                            prefs.edit().putString("compiler", "aapt").apply();
                            prefs.edit().putBoolean("aopt_debug", false).apply();
                            aoptSwitcher.setSummary(R.string.settings_aapt);
                            CheckBinaries.install(mContext, true);
                            sheetDialog.hide();
                        });
                        aopt.setOnClickListener(v -> {
                            prefs.edit().remove("compiler").apply();
                            prefs.edit().putString("compiler", "aopt").apply();
                            prefs.edit().putBoolean("aopt_debug", true).apply();
                            aoptSwitcher.setSummary(R.string.settings_aopt);
                            CheckBinaries.install(mContext, true);
                            sheetDialog.hide();
                        });
                        sheetDialog.setContentView(sheetView);
                        sheetDialog.show();
                        return false;
                    });

            if (References.checkOMS(mContext)) {
                crashReceiver.setChecked(prefs.getBoolean("crash_receiver", true));
                crashReceiver.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean isChecked = (Boolean) newValue;
                    if (!isChecked) {
                        final AlertDialog.Builder builder =
                                new AlertDialog.Builder(mContext);
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
                if (!References.checkSubstratumFeature(mContext)) {
                    crashReceiver.setVisible(false);
                }
            }
        } else {
            if (References.checkOMS(mContext)) {
                crashReceiver.setVisible(false);
            }
            aoptSwitcher.setVisible(false);
        }

        // Hidden caching mode option
        if (!themeCaching.isVisible()) {
            systemPlatform.setOnPreferenceClickListener(preference -> {
                if (!References.isSamsung(mContext)) {
                    tapCount++;
                    if (tapCount == 1) {
                        new Handler().postDelayed(() -> tapCount = 0, 2000);
                    } else if (tapCount == HIDDEN_CACHING_MODE_TAP_COUNT) {
                        themeCaching.setVisible(true);
                        tapCount = 0;
                        if (getView() != null) {
                            Lunchbar.make(getView(),
                                    R.string.settings_theme_caching_found_snackbar,
                                    Lunchbar.LENGTH_LONG)
                                    .show();
                        }
                        systemPlatform.setOnPreferenceClickListener(null);
                    }
                }
                return false;
            });
        }

        // Manage Space Activity
        Preference manageSpace = getPreferenceManager().findPreference("manage_space");
        manageSpace.setOnPreferenceClickListener(
                preference -> {
                    try {
                        startActivity(new Intent(getActivity(), ManageSpaceActivity.class));
                    } catch (ActivityNotFoundException activityNotFoundException) {
                        // Suppress warning
                    }
                    return false;
                });

        final CheckBoxPreference autosave_logchar = (CheckBoxPreference)
                getPreferenceManager().findPreference("autosave_logchar");
        Boolean save_logchar = prefs.getBoolean("autosave_logchar", true);
        if (save_logchar) {
            autosave_logchar.setChecked(true);
        } else {
            autosave_logchar.setChecked(false);
        }
        autosave_logchar.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    boolean isChecked = (Boolean) newValue;
                    if (isChecked) {
                        prefs.edit().putBoolean("autosave_logchar", true).apply();
                        autosave_logchar.setChecked(true);
                    } else {
                        final AlertDialog.Builder builder = new AlertDialog.Builder
                                (mContext);
                        builder.setTitle(R.string.logchar_dialog_title_delete_title);
                        builder.setMessage(R.string.logchar_dialog_title_delete_content);
                        builder.setNegativeButton(R.string.dialog_cancel,
                                (dialog, id) -> dialog.dismiss());
                        builder.setPositiveButton(R.string.break_compilation_dialog_continue,
                                (dialog, id) -> {
                                    prefs.edit().putBoolean("autosave_logchar", false).apply();
                                    delete(mContext,
                                            new File(Environment.getExternalStorageDirectory() +
                                                    File.separator + "substratum" + File.separator +
                                                    "LogCharReports").getAbsolutePath());
                                    autosave_logchar.setChecked(false);
                                });
                        builder.show();
                    }
                    return false;
                });

        final CheckBoxPreference overlay_alert = (CheckBoxPreference)
                getPreferenceManager().findPreference("overlay_alert");
        Boolean alert_show = prefs.getBoolean("overlay_alert", false);
        if (alert_show) {
            overlay_alert.setChecked(true);
        } else {
            overlay_alert.setChecked(false);
        }
        overlay_alert.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    boolean isChecked = (Boolean) newValue;
                    if (isChecked) {
                        prefs.edit().putBoolean("overlay_alert", true).apply();
                        overlay_alert.setChecked(true);
                        return true;
                    } else {
                        prefs.edit().putBoolean("overlay_alert", false).apply();
                        overlay_alert.setChecked(false);
                        return false;
                    }
                });


        if (!References.checkOMS(mContext)) {
            Preference priority_switcher =
                    getPreferenceManager().findPreference("legacy_priority_switcher");
            String formatted =
                    String.format(getString(R.string.legacy_preference_priority_text),
                            prefs.getInt("legacy_overlay_priority", References.DEFAULT_PRIORITY));
            priority_switcher.setSummary(formatted);
            priority_switcher.setOnPreferenceClickListener(
                    preference -> {
                        AlertDialog.Builder d = new AlertDialog.Builder(mContext);
                        d.setTitle(getString(R.string.legacy_preference_priority_title));

                        NumberPicker numberPicker = new NumberPicker(mContext);
                        // Maximum overlay priority count
                        numberPicker.setMaxValue(999);
                        // Minimum overlay priority count
                        numberPicker.setMinValue(1);
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
                                            getString(R.string.legacy_preference_priority_text),
                                            new_priority));
                        });
                        d.setNegativeButton(android.R.string.cancel, (dialogInterface, i) ->
                                dialogInterface.cancel());
                        d.show();
                        return false;
                    });
        }

        // Finally, these functions will only work on OMS ROMs
        if (References.checkOMS(mContext)) {
            Preference aboutAndromeda = getPreferenceManager().findPreference("about_andromeda");
            if (References.isAndromedaDevice(mContext)) {
                aboutAndromeda.setIcon(References.grabAppIcon(mContext, ANDROMEDA_PACKAGE));
                try {
                    PackageInfo info = mContext.getPackageManager()
                            .getPackageInfo(ANDROMEDA_PACKAGE, 0);
                    String versionName = info.versionName;
                    int versionCode = info.versionCode;
                    aboutAndromeda.setSummary(versionName + " (" + versionCode + ")");
                } catch (Exception e) {
                    // Suppress exception
                }
                aboutAndromeda.setOnPreferenceClickListener(preference -> {
                    launchExternalActivity(getContext(), ANDROMEDA_PACKAGE, "InfoActivity");
                    return false;
                });
            } else {
                aboutAndromeda.setVisible(false);
            }

            Preference aboutInterfacer = getPreferenceManager().findPreference("about_interfacer");
            aboutInterfacer.setIcon(References.grabAppIcon(mContext, INTERFACER_PACKAGE));
            aboutInterfacer.setOnPreferenceClickListener(
                    preference -> {
                        try {
                            String sourceURL;
                            if (BuildConfig.DEBUG) {
                                sourceURL = getString(R.string.interfacer_github_commits);
                            } else {
                                sourceURL = getString(R.string.interfacer_github);
                            }
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(sourceURL));
                            startActivity(i);
                        } catch (ActivityNotFoundException activityNotFoundException) {
                            Lunchbar.make(getActivity().findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Lunchbar.LENGTH_LONG)
                                    .show();
                        }
                        return false;
                    });
            try {
                PackageInfo pInfo = References.getThemeInterfacerPackage(mContext);
                assert pInfo != null;
                String versionName = pInfo.versionName;
                int versionCode = pInfo.versionCode;
                aboutInterfacer.setSummary(versionName + " (" + versionCode + ")");
            } catch (Exception e) {
                // Suppress exception
            }

            if (!References.checkSubstratumFeature(mContext)) {
                aboutInterfacer.setVisible(false);
            }

            final CheckBoxPreference hide_app_checkbox = (CheckBoxPreference)
                    getPreferenceManager().findPreference("hide_app_checkbox");
            hide_app_checkbox.setVisible(false);
            if (prefs.getBoolean("show_app_icon", true)) {
                hide_app_checkbox.setChecked(true);
            } else {
                hide_app_checkbox.setChecked(false);
            }
            if (validateResource(mContext,
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
                            PackageManager p = mContext.getPackageManager();
                            ComponentName componentName = new ComponentName(mContext,
                                    LauncherActivity.class);
                            p.setComponentEnabledSetting(
                                    componentName,
                                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                    PackageManager.DONT_KILL_APP);
                            if (getView() != null) {
                                Lunchbar.make(getView(),
                                        getString(R.string.hide_app_icon_toast_disabled),
                                        Lunchbar.LENGTH_LONG)
                                        .show();
                            }
                            hide_app_checkbox.setChecked(true);
                        } else {
                            prefs.edit().putBoolean("show_app_icon", false).apply();
                            PackageManager p = mContext.getPackageManager();
                            ComponentName componentName = new ComponentName(mContext,
                                    LauncherActivity.class);
                            p.setComponentEnabledSetting(componentName,
                                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                    PackageManager.DONT_KILL_APP);
                            if (getView() != null) {
                                Lunchbar.make(getView(),
                                        getString(R.string.hide_app_icon_toast_enabled),
                                        Lunchbar.LENGTH_LONG)
                                        .show();
                            }
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
                            if (getView() != null) {
                                Lunchbar.make(getView(),
                                        getString(R.string.restart_systemui_toast_enabled),
                                        Lunchbar.LENGTH_LONG)
                                        .show();
                            }
                            systemUIRestart.setChecked(true);
                        } else {
                            prefs.edit().putBoolean("systemui_recreate", false).apply();
                            if (getView() != null) {
                                Lunchbar.make(getView(),
                                        getString(R.string.restart_systemui_toast_disabled),
                                        Lunchbar.LENGTH_LONG)
                                        .show();
                            }
                            systemUIRestart.setChecked(false);
                        }
                        return false;
                    });
            if (!References.getProp("ro.substratum.recreate").equals("true"))
                systemUIRestart.setVisible(false);

            final CheckBoxPreference restartSystemUI = (CheckBoxPreference)
                    getPreferenceManager().findPreference("opt_in_sysui_restart");
            Boolean restartSysUI = prefs.getBoolean("opt_in_sysui_restart", true);
            if (restartSysUI) {
                restartSystemUI.setChecked(true);
            } else {
                restartSystemUI.setChecked(false);
            }
            if (References.isAndromedaDevice(mContext)) {
                restartSystemUI.setVisible(false);
            }
            restartSystemUI.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            prefs.edit().putBoolean("opt_in_sysui_restart", true).apply();
                            restartSystemUI.setChecked(true);
                        } else {
                            final AlertDialog.Builder builder = new AlertDialog.Builder
                                    (mContext);
                            builder.setTitle(R.string.auto_reload_sysui_dialog_title);
                            builder.setMessage(R.string.auto_reload_sysui_dialog_text);
                            builder.setNegativeButton(R.string.dialog_cancel,
                                    (dialog, id) -> dialog.dismiss());
                            builder.setPositiveButton(R.string.break_compilation_dialog_continue,
                                    (dialog, id) -> {
                                        prefs.edit().putBoolean("opt_in_sysui_restart", false)
                                                .apply();
                                        restartSystemUI.setChecked(false);
                                    });
                            builder.show();
                        }
                        return false;
                    });

            final CheckBoxPreference overlay_updater = (CheckBoxPreference)
                    getPreferenceManager().findPreference("overlay_updater");
            Boolean overlay_show = prefs.getBoolean("overlay_updater", false);
            if (overlay_show) {
                overlay_updater.setChecked(true);
            } else {
                overlay_updater.setChecked(false);
            }
            overlay_updater.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            prefs.edit().putBoolean("overlay_updater", true).apply();
                            overlay_updater.setChecked(true);
                            return true;
                        } else {
                            prefs.edit().putBoolean("overlay_updater", false).apply();
                            overlay_updater.setChecked(false);
                            return false;
                        }
                    });

            final CheckBoxPreference theme_updater = (CheckBoxPreference)
                    getPreferenceManager().findPreference("theme_updater");
            Boolean theme_show = prefs.getBoolean("theme_updater", false);
            if (theme_show) {
                theme_updater.setChecked(true);
            } else {
                theme_updater.setChecked(false);
            }
            theme_updater.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            final AlertDialog.Builder builder = new AlertDialog.Builder
                                    (mContext);
                            builder.setTitle(R.string.settings_theme_auto_updater_dialog_title);
                            builder.setMessage(R.string.settings_theme_auto_updater_dialog_text);
                            builder.setNegativeButton(R.string.dialog_cancel,
                                    (dialog, id) -> dialog.dismiss());
                            builder.setPositiveButton(R.string.break_compilation_dialog_continue,
                                    (dialog, id) -> {
                                        prefs.edit()
                                                .putBoolean("theme_updater", true).apply();
                                        theme_updater.setChecked(true);
                                    });
                            builder.show();
                        } else {
                            prefs.edit().putBoolean("theme_updater", false).apply();
                            theme_updater.setChecked(false);
                        }
                        return false;
                    });

            final CheckBoxPreference debugTheme = (CheckBoxPreference)
                    getPreferenceManager().findPreference("theme_debug");
            debugTheme.setChecked(prefs.getBoolean("theme_debug", false));
            debugTheme.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        boolean isChecked = (Boolean) newValue;
                        if (isChecked) {
                            final AlertDialog.Builder builder = new AlertDialog.Builder
                                    (mContext);
                            builder.setTitle(R.string.break_compilation_dialog_title);
                            builder.setMessage(R.string.break_compilation_dialog_content);
                            builder.setNegativeButton(R.string.dialog_cancel,
                                    (dialog, id) -> dialog.dismiss());
                            builder.setPositiveButton(R.string.break_compilation_dialog_continue,
                                    (dialog, id) -> {
                                        prefs.edit()
                                                .putBoolean("theme_debug", true).apply();
                                        debugTheme.setChecked(true);
                                    });
                            builder.show();
                        } else {
                            prefs.edit().putBoolean("theme_debug", false).apply();
                            debugTheme.setChecked(false);
                        }
                        return false;
                    });

            final Preference restartInterfacer = getPreferenceManager()
                    .findPreference("force_stop_interfacer");
            restartInterfacer.setOnPreferenceClickListener(preference -> {
                ThemeManager.forceStopService(mContext);
                if (getView() != null) {
                    Lunchbar.make(getView(),
                            R.string.settings_force_stop_interfacer_snackbar,
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
                return false;
            });
            try {
                Context interfacerContext = mContext.createPackageContext(INTERFACER_PACKAGE,
                        Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
                ClassLoader interfacerClassLoader = interfacerContext.getClassLoader();
                Class<?> cls = Class.forName(INTERFACER_SERVICE, true, interfacerClassLoader);
                cls.getDeclaredMethod("forceStopService");
                restartInterfacer.setVisible(true);
            } catch (Exception ex) {
                restartInterfacer.setVisible(false);
            }
        }
    }

    private static class deleteCache extends AsyncTask<String, Integer, String> {
        private WeakReference<SettingsFragment> ref;

        deleteCache(SettingsFragment settingsFragment) {
            ref = new WeakReference<>(settingsFragment);
        }

        @Override
        protected void onPreExecute() {
            SettingsFragment settingsFragment = ref.get();
            if (settingsFragment != null) {
                settingsFragment.mProgressDialog = new ProgressDialog(settingsFragment.mContext);
                settingsFragment.mProgressDialog.setMessage(
                        settingsFragment.getString(R.string.substratum_cache_clear_initial_toast));
                settingsFragment.mProgressDialog.setIndeterminate(true);
                settingsFragment.mProgressDialog.setCancelable(false);
                settingsFragment.mProgressDialog.show();
                References.clearAllNotifications(settingsFragment.mContext);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            SettingsFragment settingsFragment = ref.get();
            if (settingsFragment != null) {
                settingsFragment.mProgressDialog.cancel();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            SettingsFragment settingsFragment = ref.get();
            if (settingsFragment != null) {
                // Delete the directory
                try {
                    File dir = new File(
                            settingsFragment.mContext.
                                    getCacheDir().getAbsolutePath() + SUBSTRATUM_BUILDER_CACHE);
                    deleteDir(dir);
                } catch (Exception e) {
                    // Suppress warning
                }
                // Reset the flag for is_updating
                SharedPreferences prefsPrivate =
                        settingsFragment.mContext.
                                getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
                prefsPrivate.edit().remove("is_updating").apply();
            }
            return null;
        }

        boolean deleteDir(File dir) {
            if (dir != null && dir.isDirectory()) {
                String[] children = dir.list();
                for (String aChildren : children) {
                    boolean success = deleteDir(new File(dir, aChildren));
                    if (!success) return false;
                }
                return dir.delete();
            } else {
                return dir != null && dir.isFile() && dir.delete();
            }
        }
    }

    private static class checkROMSupportList extends AsyncTask<String, Integer, String> {

        private WeakReference<SettingsFragment> ref;

        checkROMSupportList(SettingsFragment settingsFragment) {
            ref = new WeakReference<>(settingsFragment);
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            SettingsFragment settingsFragment = ref.get();
            if (settingsFragment != null) {
                if (!References.checkThemeInterfacer(settingsFragment.mContext)) {
                    return;
                }

                if (!References.isNetworkAvailable(settingsFragment.mContext)) {
                    settingsFragment.platformSummary.append("\n")
                            .append(settingsFragment.getString(R.string.rom_status))
                            .append(" ")
                            .append(settingsFragment.getString(R.string.rom_status_network));
                    settingsFragment.systemPlatform.setSummary(
                            settingsFragment.platformSummary.toString());
                    return;
                }

                if (result.length() > 0) {
                    String supportedRom = String.format(
                            settingsFragment.getString(R.string.rom_status_supported), result);
                    settingsFragment.platformSummary.append("\n")
                            .append(settingsFragment.getString(R.string.rom_status))
                            .append(" ")
                            .append(supportedRom);
                    settingsFragment.systemPlatform.setSummary(
                            settingsFragment.platformSummary.toString());
                    return;
                }

                if (result.equals("")) {
                    settingsFragment.platformSummary.append("\n")
                            .append(settingsFragment.getString(R.string.rom_status))
                            .append(" ")
                            .append(settingsFragment.getString(R.string.rom_status_unsupported));
                    settingsFragment.systemPlatform.setSummary(
                            settingsFragment.platformSummary.toString());
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            SettingsFragment settingsFragment = ref.get();
            if (settingsFragment != null) {
                return References.checkFirmwareSupport(settingsFragment.mContext, sUrl[0], sUrl[1]);
            }
            return null;
        }
    }

    private static class downloadRepositoryList extends AsyncTask<String, Integer,
            ArrayList<String>> {

        private WeakReference<SettingsFragment> ref;

        downloadRepositoryList(SettingsFragment settingsFragment) {
            ref = new WeakReference<>(settingsFragment);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            SettingsFragment settingsFragment = ref.get();
            if (settingsFragment != null) {
                settingsFragment.dialog = new Dialog(settingsFragment.getActivity());
                settingsFragment.dialog.setContentView(R.layout.validator_dialog);
                settingsFragment.dialog.setCancelable(false);
                settingsFragment.dialog.show();
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            super.onPostExecute(result);
            SettingsFragment settingsFragment = ref.get();
            if (settingsFragment != null) {
                ArrayList<String> erroredPackages = new ArrayList<>();
                for (int x = 0; x < settingsFragment.errors.size(); x++) {
                    ValidatorError error = settingsFragment.errors.get(x);
                    erroredPackages.add(error.getPackageName());
                }

                settingsFragment.dialog.dismiss();
                Dialog dialog2 = new Dialog(settingsFragment.mContext);
                dialog2.setContentView(R.layout.validator_dialog_inner);
                RecyclerView recyclerView = dialog2.findViewById(R.id.recycler_view);
                ArrayList<ValidatorInfo> validatorInfos = new ArrayList<>();
                for (int i = 0; i < result.size(); i++) {
                    boolean validated = !erroredPackages.contains(result.get(i));
                    ValidatorInfo validatorInfo = new ValidatorInfo(
                            settingsFragment.mContext,
                            result.get(i),
                            validated,
                            result.get(i).endsWith(".common"));

                    validatorInfo.setPercentage(
                            settingsFragment.packageCounters.get(i) -
                                    settingsFragment.packageCountersErrored.get(i),
                            settingsFragment.packageCounters.get(i));

                    for (int x = 0; x < settingsFragment.errors.size(); x++) {
                        if (result.get(i).equals(settingsFragment.errors.get(x).getPackageName())) {
                            validatorInfo.setPackageError(settingsFragment.errors.get(x));
                            break;
                        }
                    }
                    validatorInfos.add(validatorInfo);
                }
                ValidatorAdapter validatorAdapter = new ValidatorAdapter(validatorInfos);
                recyclerView.setAdapter(validatorAdapter);
                RecyclerView.LayoutManager layoutManager =
                        new LinearLayoutManager(settingsFragment.mContext);
                recyclerView.setLayoutManager(layoutManager);

                Button button = dialog2.findViewById(R.id.button_done);
                button.setOnClickListener(v -> dialog2.dismiss());

                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                //noinspection ConstantConditions
                layoutParams.copyFrom(dialog2.getWindow().getAttributes());
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
                dialog2.getWindow().setAttributes(layoutParams);
                dialog2.show();
            }
        }

        @Override
        protected ArrayList<String> doInBackground(String... sUrl) {
            // First, we have to download the repository list into the cache
            SettingsFragment settingsFragment = ref.get();
            ArrayList<String> packages = new ArrayList<>();
            if (settingsFragment != null) {
                FileDownloader.init(
                        settingsFragment.mContext,
                        settingsFragment.getString(R.string.validator_url),
                        "repository_names.xml", "ValidatorCache");

                FileDownloader.init(
                        settingsFragment.mContext,
                        settingsFragment.getString(R.string.validator_whitelist_url),
                        "resource_whitelist.xml", "ValidatorCache");

                ArrayList<Repository> repositories =
                        ReadRepositoriesFile.main(
                                settingsFragment.mContext.getCacheDir().getAbsolutePath() +
                                        "/ValidatorCache/repository_names.xml");

                ArrayList<ValidatorFilter> whitelist =
                        ReadFilterFile.main(
                                settingsFragment.mContext.getCacheDir().getAbsolutePath() +
                                        "/ValidatorCache/resource_whitelist.xml");

                settingsFragment.packageCounters = new ArrayList<>();
                settingsFragment.packageCountersErrored = new ArrayList<>();
                settingsFragment.errors = new ArrayList<>();
                for (int i = 0; i < repositories.size(); i++) {
                    Repository repository = repositories.get(i);
                    // Now we have to check all the packages
                    String packageName = repository.getPackageName();
                    ValidatorError validatorError = new ValidatorError(packageName);
                    Boolean has_errored = false;

                    int resource_counter = 0;
                    int resource_counter_errored = 0;

                    String tempPackageName = (packageName.endsWith(".common") ?
                            packageName.substring(0, packageName.length() - 7) :
                            packageName);

                    if (References.isPackageInstalled(settingsFragment.mContext, tempPackageName)) {
                        packages.add(packageName);

                        // Check if there's a bools commit check
                        if (repository.getBools() != null) {
                            FileDownloader.init(settingsFragment.mContext, repository.getBools(),
                                    tempPackageName + ".bools.xml", "ValidatorCache");
                            ArrayList<String> bools =
                                    ReadResourcesFile.main(
                                            settingsFragment.mContext.
                                                    getCacheDir().getAbsolutePath() +
                                                    "/ValidatorCache/" + tempPackageName +
                                                    ".bools.xml",
                                            "bool");
                            for (int j = 0; j < bools.size(); j++) {
                                boolean validated = Validator.checkResourceObject(
                                        settingsFragment.mContext,
                                        tempPackageName,
                                        "bool",
                                        bools.get(j));
                                if (validated) {
                                    if (VALIDATE_WITH_LOGS)
                                        Log.d("BoolCheck", "Resource exists: " + bools.get(j));
                                } else {
                                    boolean bypassed = false;
                                    for (int x = 0; x < whitelist.size(); x++) {
                                        String currentPackage = whitelist.get(x).getPackageName();
                                        ArrayList<String> currentWhitelist = whitelist.get(x)
                                                .getFilter();
                                        if (currentPackage.equals(packageName)) {
                                            if (currentWhitelist.contains(bools.get(j))) {
                                                if (VALIDATE_WITH_LOGS)
                                                    Log.d("BoolCheck",
                                                            "Resource bypassed using filter: " +
                                                                    bools.get(j));
                                                bypassed = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!bypassed) {
                                        if (VALIDATE_WITH_LOGS)
                                            Log.e("BoolCheck",
                                                    "Resource does not exist: " + bools.get(j));
                                        has_errored = true;
                                        validatorError.addBoolError(
                                                "{" + settingsFragment.getString(
                                                        R.string.resource_boolean) + "} " +
                                                        bools.get(j));
                                        resource_counter_errored++;
                                    }
                                }
                                resource_counter++;
                            }
                        }
                        // Then go through the entire list of colors
                        if (repository.getColors() != null) {
                            FileDownloader.init(settingsFragment.mContext, repository.getColors(),
                                    tempPackageName + ".colors.xml", "ValidatorCache");
                            ArrayList<String> colors = ReadResourcesFile.main(
                                    settingsFragment.mContext
                                            .getCacheDir().getAbsolutePath() +
                                            "/ValidatorCache/" + tempPackageName + ".colors.xml",
                                    "color");
                            for (int j = 0; j < colors.size(); j++) {
                                boolean validated = Validator.checkResourceObject(
                                        settingsFragment.mContext,
                                        tempPackageName,
                                        "color",
                                        colors.get(j));
                                if (validated) {
                                    if (VALIDATE_WITH_LOGS)
                                        Log.d("ColorCheck", "Resource exists: " + colors.get(j));
                                } else {
                                    boolean bypassed = false;
                                    for (int x = 0; x < whitelist.size(); x++) {
                                        String currentPackage = whitelist.get(x).getPackageName();
                                        ArrayList<String> currentWhitelist = whitelist.get(x)
                                                .getFilter();
                                        if (currentPackage.equals(packageName)) {
                                            if (currentWhitelist.contains(colors.get(j))) {
                                                if (VALIDATE_WITH_LOGS)
                                                    Log.d("ColorCheck",
                                                            "Resource bypassed using filter: " +
                                                                    colors.get(j));
                                                bypassed = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!bypassed) {
                                        if (VALIDATE_WITH_LOGS)
                                            Log.e("ColorCheck",
                                                    "Resource does not exist: " + colors.get(j));
                                        has_errored = true;
                                        validatorError.addBoolError(
                                                "{" + settingsFragment.getString(
                                                        R.string.resource_color) + "} " +
                                                        colors.get(j));
                                        resource_counter_errored++;
                                    }
                                }
                                resource_counter++;
                            }
                        }
                        // Next, dimensions may need to be exposed
                        if (repository.getDimens() != null) {
                            FileDownloader.init(settingsFragment.mContext, repository.getDimens(),
                                    tempPackageName + ".dimens.xml", "ValidatorCache");
                            ArrayList<String> dimens = ReadResourcesFile.main(
                                    settingsFragment.mContext.getCacheDir().getAbsolutePath() +
                                            "/ValidatorCache/" + tempPackageName +
                                            ".dimens.xml", "dimen");
                            for (int j = 0; j < dimens.size(); j++) {
                                boolean validated = Validator.checkResourceObject(
                                        settingsFragment.mContext,
                                        tempPackageName,
                                        "dimen",
                                        dimens.get(j));
                                if (validated) {
                                    if (VALIDATE_WITH_LOGS)
                                        Log.d("DimenCheck", "Resource exists: " + dimens.get(j));
                                } else {
                                    boolean bypassed = false;
                                    for (int x = 0; x < whitelist.size(); x++) {
                                        String currentPackage = whitelist.get(x).getPackageName();
                                        ArrayList<String> currentWhitelist = whitelist.get(x)
                                                .getFilter();
                                        if (currentPackage.equals(packageName)) {
                                            if (currentWhitelist.contains(dimens.get(j))) {
                                                if (VALIDATE_WITH_LOGS)
                                                    Log.d("DimenCheck",
                                                            "Resource bypassed using filter: " +
                                                                    dimens.get(j));
                                                bypassed = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!bypassed) {
                                        if (VALIDATE_WITH_LOGS)
                                            Log.e("DimenCheck",
                                                    "Resource does not exist: " + dimens.get(j));
                                        has_errored = true;
                                        validatorError.addBoolError(
                                                "{" + settingsFragment.getString(
                                                        R.string.resource_dimension) + "}" +
                                                        " " +
                                                        dimens.get(j));
                                        resource_counter_errored++;
                                    }
                                }
                                resource_counter++;
                            }
                        }
                        // Finally, check if styles are exposed
                        if (repository.getStyles() != null) {
                            FileDownloader.init(settingsFragment.mContext, repository.getStyles(),
                                    tempPackageName + ".styles.xml", "ValidatorCache");
                            ArrayList<String> styles = ReadResourcesFile.main(
                                    settingsFragment.mContext
                                            .getCacheDir().getAbsolutePath() +
                                            "/ValidatorCache/" + tempPackageName + ".styles.xml",
                                    "style");
                            for (int j = 0; j < styles.size(); j++) {
                                boolean validated = Validator.checkResourceObject(
                                        settingsFragment.mContext,
                                        tempPackageName,
                                        "style",
                                        styles.get(j));
                                if (validated) {
                                    if (VALIDATE_WITH_LOGS)
                                        Log.d("StyleCheck", "Resource exists: " + styles.get(j));
                                } else {
                                    boolean bypassed = false;
                                    for (int x = 0; x < whitelist.size(); x++) {
                                        String currentPackage = whitelist.get(x).getPackageName();
                                        ArrayList<String> currentWhitelist = whitelist.get(x)
                                                .getFilter();
                                        if (currentPackage.equals(packageName)) {
                                            if (currentWhitelist.contains(styles.get(j))) {
                                                if (VALIDATE_WITH_LOGS)
                                                    Log.d("StyleCheck",
                                                            "Resource bypassed using filter: " +
                                                                    styles.get(j));
                                                bypassed = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!bypassed) {
                                        if (VALIDATE_WITH_LOGS)
                                            Log.e("StyleCheck",
                                                    "Resource does not exist: " + styles.get(j));
                                        has_errored = true;
                                        validatorError.addBoolError(
                                                "{" + settingsFragment.getString(
                                                        R.string.resource_style) + "} " +
                                                        styles.get(j));
                                        resource_counter_errored++;
                                    }
                                }
                                resource_counter++;
                            }
                        }
                        settingsFragment.packageCounters.add(resource_counter);
                        settingsFragment.packageCountersErrored.add(resource_counter_errored);
                    } else if (VALIDATE_WITH_LOGS)
                        Log.d(SUBSTRATUM_VALIDATOR,
                                "This device does not come built-in with '" + packageName + "', " +
                                        "skipping resource verification...");
                    if (has_errored) settingsFragment.errors.add(validatorError);
                }
            }
            return packages;
        }
    }
}