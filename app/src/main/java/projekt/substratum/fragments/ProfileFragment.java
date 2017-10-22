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

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.Theming;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeInterfacerService;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.systems.ProfileItem;
import projekt.substratum.common.systems.ProfileManager;
import projekt.substratum.common.tabs.WallpaperManager;
import projekt.substratum.util.compilers.SubstratumBuilder;

import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.KEY_RETRIEVAL;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.STATUS_CHANGED;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;
import static projekt.substratum.common.References.VENDOR_DIR;
import static projekt.substratum.common.References.metadataEncryption;
import static projekt.substratum.common.References.metadataEncryptionValue;
import static projekt.substratum.common.platform.ThemeManager.STATE_ENABLED;
import static projekt.substratum.common.systems.ProfileManager.DAY_PROFILE;
import static projekt.substratum.common.systems.ProfileManager.DAY_PROFILE_HOUR;
import static projekt.substratum.common.systems.ProfileManager.DAY_PROFILE_MINUTE;
import static projekt.substratum.common.systems.ProfileManager.NIGHT_PROFILE;
import static projekt.substratum.common.systems.ProfileManager.NIGHT_PROFILE_HOUR;
import static projekt.substratum.common.systems.ProfileManager.NIGHT_PROFILE_MINUTE;
import static projekt.substratum.common.systems.ProfileManager.SCHEDULED_PROFILE_ENABLED;

public class ProfileFragment extends Fragment {

    private static final int THREAD_WAIT_DURATION = 500;
    public static int nightHour, nightMinute, dayHour, dayMinute;
    private Context mContext;
    private List<String> list;
    private ProgressBar headerProgress;
    private Spinner profile_selector, dayProfile, nightProfile;
    private EditText backup_name;
    private String backup_getText;
    private String to_be_run_commands;
    private ArrayAdapter<String> adapter;
    private List<List<String>> cannot_run_overlays;
    private StringBuilder dialog_message;
    private boolean dayNightEnabled;
    private ArrayList<CharSequence> selectedBackup;
    private boolean isWaiting;
    private FinishReceiver finishReceiver;

    public static void setNightProfileStart(final int hour, final int minute) {
        nightHour = hour;
        nightMinute = minute;
    }

    public static void setDayProfileStart(final int hour, final int minute) {
        dayHour = hour;
        dayMinute = minute;
    }

    public void RefreshSpinner() {
        this.list.clear();
        this.list.add(this.getResources().getString(R.string.spinner_default_item));

        // Now lets add all the located profiles
        final File f = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/");
        final File[] files = f.listFiles();
        if (files != null) {
            for (final File inFile : files) {
                if (inFile.isDirectory()) {
                    this.list.add(inFile.getName());
                }
            }
        }
        this.adapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ViewGroup root = (ViewGroup) inflater.inflate(R.layout.profile_fragment, container, false);

        this.mContext = this.getContext();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        this.headerProgress = root.findViewById(R.id.header_loading_bar);
        this.headerProgress.setVisibility(View.GONE);

        // Create a user viewable directory for profiles
        final File directory = new File(
                Environment.getExternalStorageDirectory(), "/substratum/");
        if (!directory.exists() && !directory.mkdirs()) {
            Log.e(References.SUBSTRATUM_LOG, "Could not create Substratum directory...");
        }
        final File directory2 = new File(
                Environment.getExternalStorageDirectory(), "/substratum/profiles");
        if (!directory2.exists() && directory2.mkdirs()) {
            Log.e(References.SUBSTRATUM_LOG, "Could not create profile directory...");
        }

        // Handle Backups
        this.backup_name = root.findViewById(R.id.edittext);

        // Restrict whitespace for profile name
        final InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (Character.isWhitespace(source.charAt(i))) {
                    Toast.makeText(this.mContext,
                            R.string.profile_edittext_whitespace_warning_toast,
                            Toast.LENGTH_LONG)
                            .show();
                    return "";
                }
            }
            return null;
        };
        this.backup_name.setFilters(new InputFilter[]{filter});
        this.backup_name.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                final InputMethodManager imm = (InputMethodManager)
                        this.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(this.backup_name.getWindowToken(),
                            InputMethodManager.RESULT_UNCHANGED_SHOWN);
                }
            }
        });

        final Button backupButton = root.findViewById(R.id.backupButton);
        backupButton.setOnClickListener(v -> {
            if (this.backup_name.getText().length() > 0) {
                this.selectedBackup = new ArrayList<>();
                final CharSequence[] items;
                if (Systems.checkOMS(this.mContext) ||
                        projekt.substratum.common.Resources.isFontsSupported()) {
                    items = new CharSequence[]{
                            this.getString(R.string.profile_boot_animation),
                            this.getString(R.string.profile_font),
                            this.getString(R.string.profile_overlay),
                            this.getString(R.string.profile_sound),
                            this.getString(R.string.profile_wallpaper)};
                } else {
                    items = new CharSequence[]{
                            this.getString(R.string.profile_boot_animation),
                            this.getString(R.string.profile_overlay),
                            this.getString(R.string.profile_sound),
                            this.getString(R.string.profile_wallpaper)};
                }

                final AlertDialog dialog = new AlertDialog.Builder(this.mContext)
                        .setTitle(R.string.profile_dialog_title)
                        .setMultiChoiceItems(items, null, (dialog1, which, isChecked) -> {
                            if (isChecked) {
                                if (items[which].equals(this.getString(R.string.profile_boot_animation))
                                        && Systems.getDeviceEncryptionStatus(this.mContext) > 1
                                        && Systems.checkThemeInterfacer(this.mContext)) {
                                    AlertDialog dialog2 = new AlertDialog.Builder(this.mContext)
                                            .setTitle(R.string.root_required_title)
                                            .setMessage(R.string
                                                    .root_required_boot_animation_profile)
                                            .setPositiveButton(android.R.string.ok, null)
                                            .create();
                                    dialog2.show();
                                }
                                this.selectedBackup.add(items[which]);
                            } else if (this.selectedBackup.contains(items[which])) {
                                this.selectedBackup.remove(items[which]);
                            }
                        })
                        .setPositiveButton(R.string.profile_dialog_ok, null)
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .create();

                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                    if (!this.selectedBackup.isEmpty()) {
                        this.backup_getText = this.backup_name.getText().toString();
                        final BackupFunction backupFunction = new BackupFunction(this);
                        backupFunction.execute();
                        Log.d(References.SUBSTRATUM_LOG, this.selectedBackup.toString());
                        dialog.dismiss();
                        this.backup_name.getText().clear();
                    } else {
                        Toast.makeText(this.mContext, R.string.profile_no_selection_warning,
                                Toast.LENGTH_LONG).show();
                    }
                });

                final InputMethodManager imm = (InputMethodManager)
                        this.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(backupButton.getWindowToken(),
                            InputMethodManager.RESULT_UNCHANGED_SHOWN);
                }
            } else {
                if (this.getView() != null) {
                    Lunchbar.make(this.getView(),
                            this.getString(R.string.profile_edittext_empty_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            }
        });

        // Handle Restores

        this.profile_selector = root.findViewById(R.id.restore_spinner);

        this.list = new ArrayList<>();
        this.adapter = new ArrayAdapter<>(this.mContext, android.R.layout.simple_spinner_item, this.list);
        this.RefreshSpinner();

        // Specify the layout to use when the list of choices appears
        this.adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.profile_selector.setAdapter(this.adapter);

        final ImageButton imageButton = root.findViewById(R.id.remove_profile);
        imageButton.setOnClickListener(v -> {
            if (this.profile_selector.getSelectedItemPosition() > 0) {
                final String formatted = String.format(this.getString(R.string.delete_dialog_text),
                        this.profile_selector.getSelectedItem());
                new AlertDialog.Builder(this.mContext)
                        .setTitle(this.getString(R.string.delete_dialog_title))
                        .setMessage(formatted)
                        .setCancelable(false)
                        .setPositiveButton(this.getString(R.string.delete_dialog_okay),
                                (dialog, which) -> {
                                    File f1 = new File(Environment
                                            .getExternalStorageDirectory().getAbsolutePath()
                                            + "/substratum/profiles/" + this.profile_selector
                                            .getSelectedItem() + "" +
                                            ".substratum");
                                    boolean deleted = f1.delete();
                                    if (!deleted)
                                        Log.e(References.SUBSTRATUM_LOG,
                                                "Could not delete profile directory.");
                                    FileOperations.delete(this.mContext,
                                            Environment.getExternalStorageDirectory()
                                                    .getAbsolutePath() +
                                                    "/substratum/profiles/" +
                                                    this.profile_selector.getSelectedItem());
                                    this.RefreshSpinner();
                                })
                        .setNegativeButton(this.getString(R.string.dialog_cancel),
                                (dialog, which) -> dialog.cancel())
                        .create()
                        .show();
            } else {
                if (this.getView() != null) {
                    Lunchbar.make(this.getView(),
                            this.getString(R.string.profile_delete_button_none_selected_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            }
        });

        final Button restoreButton = root.findViewById(R.id.restoreButton);
        restoreButton.setOnClickListener(v -> {
            if (this.profile_selector.getSelectedItemPosition() > 0) {
                final RestoreFunction restoreFunction = new RestoreFunction(this);
                restoreFunction.execute(this.profile_selector.getSelectedItem().toString());
            } else {
                if (this.getView() != null) {
                    Lunchbar.make(this.getView(),
                            this.getString(R.string.restore_button_none_selected_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            }
        });

        final CardView scheduledProfileCard = root.findViewById(R.id.cardListView3);
        if (Systems.checkOMS(this.mContext) && Systems.checkThemeInterfacer(this.getContext
                ())) {
            final ExpandableLayout scheduledProfileLayout = root.findViewById(
                    R.id.scheduled_profile_card_content_container);
            final Switch dayNightSwitch = root.findViewById(R.id.profile_switch);
            dayNightSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) {
                    scheduledProfileLayout.expand();
                } else {
                    scheduledProfileLayout.collapse();
                }
                this.dayNightEnabled = b;
            });

            final FragmentManager fm = this.getActivity().getSupportFragmentManager();
            final Button startTime = root.findViewById(R.id.night_start_time);
            startTime.setOnClickListener(view -> {
                final DialogFragment timePickerFragment = new TimePickerFragment();
                if (startTime.getText().equals(this.getResources().getString(R.string.start_time))) {
                    TimePickerFragment.setFlag(TimePickerFragment.FLAG_START_TIME);
                } else {
                    TimePickerFragment.setFlag(
                            TimePickerFragment.FLAG_START_TIME | TimePickerFragment.FLAG_GET_VALUE);
                }
                timePickerFragment.show(fm, "TimePicker");
            });

            final Button endTime = root.findViewById(R.id.night_end_time);
            endTime.setOnClickListener(view -> {
                final DialogFragment timePickerFragment = new TimePickerFragment();
                if (endTime.getText().equals(this.getResources().getString(R.string.end_time))) {
                    TimePickerFragment.setFlag(TimePickerFragment.FLAG_END_TIME);
                } else {
                    TimePickerFragment.setFlag(
                            TimePickerFragment.FLAG_END_TIME | TimePickerFragment.FLAG_GET_VALUE);
                }
                timePickerFragment.show(fm, "TimePicker");
            });

            this.dayProfile = root.findViewById(R.id.day_spinner);
            this.dayProfile.setAdapter(this.adapter);
            this.nightProfile = root.findViewById(R.id.night_spinner);
            this.nightProfile.setAdapter(this.adapter);

            if (prefs.getBoolean(SCHEDULED_PROFILE_ENABLED, false)) {
                final String day = prefs.getString(DAY_PROFILE, this.getResources()
                        .getString(R.string.spinner_default_item));
                final String night = prefs.getString(NIGHT_PROFILE, this.getResources()
                        .getString(R.string.spinner_default_item));
                dayHour = prefs.getInt(DAY_PROFILE_HOUR, 0);
                dayMinute = prefs.getInt(DAY_PROFILE_MINUTE, 0);
                nightHour = prefs.getInt(NIGHT_PROFILE_HOUR, 0);
                nightMinute = prefs.getInt(NIGHT_PROFILE_MINUTE, 0);
                this.dayNightEnabled = true;

                scheduledProfileLayout.expand(false);
                dayNightSwitch.setChecked(true);
                startTime.setText(References.parseTime(this.getActivity(), nightHour, nightMinute));
                endTime.setText(References.parseTime(this.getActivity(), dayHour, dayMinute));
                this.dayProfile.setSelection(this.adapter.getPosition(day));
                this.nightProfile.setSelection(this.adapter.getPosition(night));
            }

            final Button applyScheduledProfileButton = root.findViewById(
                    R.id.apply_schedule_button);
            applyScheduledProfileButton.setOnClickListener(view -> {
                if (this.dayNightEnabled) {
                    if (this.dayProfile.getSelectedItemPosition() > 0 &&
                            this.nightProfile.getSelectedItemPosition() > 0) {
                        if (!startTime.getText().equals(this.getResources()
                                .getString(R.string.start_time)) && !endTime.getText()
                                .equals(this.getResources().getString(R.string.end_time))) {
                            if (dayHour != nightHour || dayMinute != nightMinute) {
                                ProfileManager.enableScheduledProfile(this.getActivity(),
                                        this.dayProfile.getSelectedItem().toString(), dayHour, dayMinute,
                                        this.nightProfile.getSelectedItem().toString(), nightHour,
                                        nightMinute);
                                if (this.getView() != null) {
                                    Lunchbar.make(this.getView(),
                                            R.string.scheduled_profile_apply_success,
                                            Lunchbar.LENGTH_LONG)
                                            .show();
                                }
                            } else {
                                if (this.getView() != null) {
                                    Lunchbar.make(this.getView(), R.string.time_equal_warning,
                                            Lunchbar.LENGTH_LONG)
                                            .show();
                                }
                            }
                        } else {
                            if (this.getView() != null) {
                                Lunchbar.make(this.getView(), R.string.time_empty_warning,
                                        Lunchbar.LENGTH_LONG)
                                        .show();
                            }
                        }
                    } else {
                        if (this.getView() != null) {
                            Lunchbar.make(this.getView(), R.string.profile_empty_warning,
                                    Lunchbar.LENGTH_LONG)
                                    .show();
                        }
                    }
                } else {
                    ProfileManager.disableScheduledProfile(this.getActivity());
                    if (this.getView() != null) {
                        Lunchbar.make(this.getView(),
                                R.string.scheduled_profile_disable_success,
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                }
            });
        } else {
            scheduledProfileCard.setVisibility(View.GONE);
        }
        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            this.mContext.getApplicationContext().unregisterReceiver(this.finishReceiver);
        } catch (final IllegalArgumentException e) {
            // Unregistered already
        }
    }

    private static class BackupFunction extends AsyncTask<String, Integer, String> {

        private final WeakReference<ProfileFragment> ref;

        BackupFunction(final ProfileFragment profileFragment) {
            super();
            this.ref = new WeakReference<>(profileFragment);
        }

        @Override
        protected void onPreExecute() {
            final ProfileFragment profileFragment = this.ref.get();
            if (profileFragment != null) {
                profileFragment.headerProgress.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            super.onPostExecute(result);
            final ProfileFragment profileFragment = this.ref.get();
            if (profileFragment != null) {
                profileFragment.headerProgress.setVisibility(View.GONE);
                if (Systems.checkOMS(profileFragment.mContext)) {
                    final String directory_parse = String.format(
                            profileFragment.getString(R.string.toast_backup_success),
                            profileFragment.backup_getText);
                    if (profileFragment.getView() != null) {
                        Lunchbar.make(profileFragment.getView(),
                                directory_parse,
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                } else {
                    final String directory_parse = String.format(
                            profileFragment.getString(R.string.toast_backup_success),
                            profileFragment.backup_getText + "/");
                    if (profileFragment.getView() != null) {
                        Lunchbar.make(profileFragment.getView(),
                                directory_parse,
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                }
                profileFragment.RefreshSpinner();
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final ProfileFragment profileFragment = this.ref.get();
            if (profileFragment != null) {
                final String uid =
                        Environment.getExternalStorageDirectory().getAbsolutePath().split("/")[3];
                final File nomediaFile = new File(Environment.getExternalStorageDirectory() +
                        "/substratum/.nomedia");
                try {
                    if (!nomediaFile.createNewFile()) {
                        Log.d(References.SUBSTRATUM_LOG, "Could not create .nomedia file or" +
                                " file already exist!");
                    }
                } catch (final IOException e) {
                    Log.d(References.SUBSTRATUM_LOG, "Could not create .nomedia file!");
                    e.printStackTrace();
                }

                if (Systems.checkOMS(profileFragment.mContext)) {
                    final File profileDir = new File(Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                            "/substratum/profiles/" + profileFragment.backup_getText + "/");
                    if (profileDir.exists()) {
                        FileOperations.delete(profileFragment.mContext,
                                Environment.getExternalStorageDirectory().getAbsolutePath() +
                                        "/substratum/profiles/" + profileFragment.backup_getText);
                        if (!profileDir.mkdir())
                            Log.e(References.SUBSTRATUM_LOG, "Could not create profile directory.");
                    } else {
                        if (!profileDir.mkdir())
                            Log.e(References.SUBSTRATUM_LOG, "Could not create profile directory.");
                    }

                    final File profileFile = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/substratum/profiles/" + profileFragment.backup_getText +
                            "/" + "overlays.xml");
                    if (profileFile.exists()) {
                        FileOperations.delete(
                                profileFragment.mContext,
                                profileFile.getAbsolutePath());
                    }

                    if (profileFragment.selectedBackup.contains(
                            profileFragment.getString(R.string.profile_overlay))) {
                        ProfileManager.writeProfileState(
                                profileFragment.mContext,
                                profileFragment.backup_getText);
                    }

                    // Backup the entire /data/system/theme/ folder
                    FileOperations.copyDir(profileFragment.mContext, "/data/system/theme",
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/substratum/profiles/" + profileFragment.backup_getText +
                                    "/theme");

                    // Delete the items user don't want to backup
                    if (!profileFragment.selectedBackup.contains(
                            profileFragment.getString(R.string.profile_boot_animation))) {
                        final File bootanimation = new File(profileDir, "theme/bootanimation.zip");
                        if (bootanimation.exists()) {
                            FileOperations.delete(profileFragment.mContext,
                                    bootanimation.getAbsolutePath());
                        }
                    }
                    if (!profileFragment.selectedBackup.contains(
                            profileFragment.getString(R.string.profile_font))) {
                        final File fonts = new File(profileDir, "theme/fonts");
                        if (fonts.exists()) {
                            FileOperations.delete(profileFragment.mContext,
                                    fonts.getAbsolutePath());
                        }
                    }
                    if (!profileFragment.selectedBackup.contains(
                            profileFragment.getString(R.string.profile_sound))) {
                        final File sounds = new File(profileDir, "theme/audio");
                        if (sounds.exists()) {
                            FileOperations.delete(profileFragment.mContext,
                                    sounds.getAbsolutePath());
                        }
                    }

                    // Backup wallpapers if wanted
                    if (profileFragment.selectedBackup.contains(
                            profileFragment.getString(R.string.profile_wallpaper))) {
                        FileOperations.copy(profileFragment.mContext,
                                "/data/system/users/" + uid + "/wallpaper",
                                Environment.getExternalStorageDirectory().getAbsolutePath()
                                        + "/substratum/profiles/" + profileFragment.backup_getText +
                                        "/wallpaper.png");
                        FileOperations.copy(profileFragment.mContext, "/data/system/users/" +
                                        uid + "/wallpaper_lock",
                                Environment.getExternalStorageDirectory().getAbsolutePath()
                                        + "/substratum/profiles/" + profileFragment.backup_getText +
                                        "/wallpaper_lock.png");
                    }

                    // Backup system bootanimation if encrypted
                    if (Systems.getDeviceEncryptionStatus(profileFragment.mContext) > 1 &&
                            profileFragment.selectedBackup.contains(
                                    profileFragment.getString(R.string.profile_boot_animation))) {
                        FileOperations.copy(profileFragment.mContext,
                                "/system/media/bootanimation.zip",
                                Environment.getExternalStorageDirectory().getAbsolutePath()
                                        + "/substratum/profiles/" + profileFragment.backup_getText +
                                        "/bootanimation.zip");
                    }

                    // Clear theme profile folder if empty
                    final File profileThemeFolder = new File(profileDir, "theme");
                    if (profileThemeFolder.list() != null) {
                        if (profileThemeFolder.list().length == 0) {
                            Log.d(References.SUBSTRATUM_LOG,
                                    "Profile theme directory is empty! delete " +
                                            (profileThemeFolder.delete() ? "success" : "failed"));
                        }
                    }
                } else {
                    final String current_directory;
                    if (projekt.substratum.common.Resources.inNexusFilter()) {
                        current_directory = "/system/overlay/";
                    } else {
                        current_directory = "/system/vendor/overlay/";
                    }
                    final File file = new File(current_directory);
                    if (file.exists()) {
                        FileOperations.mountRW();
                        if (profileFragment.selectedBackup.contains(
                                profileFragment.getString(R.string.profile_overlay))) {
                            FileOperations.copyDir(profileFragment.mContext, current_directory,
                                    Environment.getExternalStorageDirectory().getAbsolutePath() +
                                            "/substratum/profiles/");
                            final File oldFolder = new File(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/substratum/profiles/overlay");
                            final File newFolder = new File(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/substratum/profiles/" +
                                    profileFragment.backup_getText);
                            final boolean success = oldFolder.renameTo(newFolder);
                            if (!success)
                                Log.e(References.SUBSTRATUM_LOG,
                                        "Could not move profile directory...");
                        }

                        if (profileFragment.selectedBackup.contains(
                                profileFragment.getString(R.string.profile_sound))) {
                            // Now begin backing up sounds
                            FileOperations.copyDir(profileFragment.mContext,
                                    "/data/system/theme/audio/",
                                    Environment.getExternalStorageDirectory()
                                            .getAbsolutePath() + "/substratum/profiles/"
                                            + profileFragment.backup_getText);
                        }
                        FileOperations.mountRO();

                        // Don't forget the wallpaper if wanted
                        if (profileFragment.selectedBackup.contains(
                                profileFragment.getString(R.string.profile_wallpaper))) {
                            final File homeWall = new File("/data/system/users/" + uid + "/wallpaper");
                            final File lockWall = new File("/data/system/users/" + uid +
                                    "/wallpaper_lock");
                            if (homeWall.exists()) {
                                FileOperations.copy(profileFragment.mContext, homeWall
                                                .getAbsolutePath(),
                                        Environment.getExternalStorageDirectory().getAbsolutePath()
                                                + "/substratum/profiles/" +
                                                profileFragment.backup_getText +
                                                "/wallpaper.png");
                            }
                            if (lockWall.exists()) {
                                FileOperations.copy(profileFragment.mContext,
                                        lockWall.getAbsolutePath(),
                                        Environment.getExternalStorageDirectory().getAbsolutePath()
                                                + "/substratum/profiles/" +
                                                profileFragment.backup_getText +
                                                "/wallpaper_lock.png");
                            }
                        }

                        // And bootanimation if wanted
                        if (profileFragment.selectedBackup.contains(
                                profileFragment.getString(R.string.profile_boot_animation))) {
                            FileOperations.copy(profileFragment.mContext,
                                    "/system/media/bootanimation.zip",
                                    Environment.getExternalStorageDirectory().getAbsolutePath()
                                            + "/substratum/profiles/" +
                                            profileFragment.backup_getText);
                        }
                    } else {
                        if (profileFragment.getView() != null) {
                            Lunchbar.make(profileFragment.getView(),
                                    profileFragment.getString(R.string.backup_no_overlays),
                                    Lunchbar.LENGTH_LONG)
                                    .show();
                        }
                    }
                }
            }
            return null;
        }
    }

    private static class RestoreFunction extends AsyncTask<String, Integer, String> {
        List<String> system = new ArrayList<>(); // All installed overlays
        ArrayList<String> to_be_run = new ArrayList<>(); // Overlays going to be enabled
        String profile_name;

        private final WeakReference<ProfileFragment> ref;

        RestoreFunction(final ProfileFragment profileFragment) {
            super();
            this.ref = new WeakReference<>(profileFragment);
        }

        @Override
        protected void onPreExecute() {
            final ProfileFragment profileFragment = this.ref.get();
            if (profileFragment != null) {
                profileFragment.headerProgress.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            super.onPostExecute(result);
            final ProfileFragment profileFragment = this.ref.get();
            if (profileFragment != null) {
                if (Systems.checkOMS(profileFragment.mContext)) {
                    if (profileFragment.cannot_run_overlays.size() > 0) {
                        new AlertDialog.Builder(profileFragment.mContext)
                                .setTitle(profileFragment.getString(R.string.restore_dialog_title))
                                .setMessage(profileFragment.dialog_message)
                                .setCancelable(false)
                                .setPositiveButton(
                                        profileFragment.getString(R.string.restore_dialog_okay),
                                        (dialog, which) -> {
                                            dialog.dismiss();
                                            // Continue restore process (compile missing overlays
                                            // and
                                            // enable)
                                            new ContinueRestore(
                                                    profileFragment,
                                                    this.profile_name,
                                                    profileFragment.cannot_run_overlays,
                                                    this.to_be_run)
                                                    .execute();
                                        })
                                .setNegativeButton(
                                        profileFragment.getString(R.string.dialog_cancel),
                                        (dialog, which) ->
                                                profileFragment.headerProgress.
                                                        setVisibility(View.GONE))
                                .create().show();
                    } else {
                        // Continue restore process (enable)
                        new ContinueRestore(profileFragment, this.profile_name, this.to_be_run).execute();
                    }
                } else {
                    final String current_directory;
                    if (projekt.substratum.common.Resources.inNexusFilter()) {
                        current_directory = PIXEL_NEXUS_DIR;
                    } else {
                        current_directory = LEGACY_NEXUS_DIR;
                    }
                    final File file = new File(current_directory);
                    if (file.exists()) {
                        // Delete destination overlays
                        FileOperations.mountRW();
                        FileOperations.delete(profileFragment.mContext, current_directory);
                        FileOperations.delete(profileFragment.mContext, "/data/system/theme/");
                        FileOperations.createNewFolder(profileFragment.mContext, current_directory);
                        FileOperations.createNewFolder(profileFragment.mContext,
                                "/data/system/theme/");
                        FileOperations.setPermissions(755, "/data/system/theme/");

                        final File profile_apk_files = new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/substratum/profiles/" +
                                profileFragment.profile_selector.getSelectedItem() + "/");
                        final String[] located_files = profile_apk_files.list();
                        for (final String found : located_files) {
                            if (!"audio".equals(found)) {
                                FileOperations.copyDir(profileFragment.mContext, Environment
                                        .getExternalStorageDirectory()
                                        .getAbsolutePath() +
                                        "/substratum/profiles/" +
                                        profileFragment.profile_selector.getSelectedItem() +
                                        "/" + found, current_directory);
                            } else {
                                FileOperations.copyDir(profileFragment.mContext, Environment
                                        .getExternalStorageDirectory()
                                        .getAbsolutePath() +
                                        "/substratum/profiles/" +
                                        profileFragment.profile_selector.getSelectedItem() +
                                        "/" + found + "/", "/data/system/theme/audio/");
                                FileOperations.setPermissionsRecursively(644,
                                        "/data/system/theme/audio/");
                                FileOperations.setPermissions(755, "/data/system/theme/audio/");
                            }
                        }
                        FileOperations.setPermissionsRecursively(644, current_directory);
                        FileOperations.setPermissions(755, current_directory);
                        FileOperations.setContext(current_directory);
                        FileOperations.mountRO();
                    } else {
                        final String vendor_location = LEGACY_NEXUS_DIR;
                        final String vendor_partition = VENDOR_DIR;
                        final String vendor_symlink = PIXEL_NEXUS_DIR;
                        final String current_vendor =
                                ((projekt.substratum.common.Resources.inNexusFilter()) ?
                                        vendor_partition :
                                        vendor_location);
                        FileOperations.mountRW();
                        final File vendor = new File(current_vendor);
                        if (!vendor.exists()) {
                            if (current_vendor.equals(vendor_location)) {
                                FileOperations.createNewFolder(current_vendor);
                            } else {
                                FileOperations.mountRWVendor();
                                FileOperations.createNewFolder(vendor_symlink);
                                FileOperations.symlink(vendor_symlink, "/vendor");
                                FileOperations.setPermissions(755, vendor_partition);
                                FileOperations.mountROVendor();
                            }
                        }

                        FileOperations.delete(profileFragment.mContext, "/data/system/theme/");
                        FileOperations.createNewFolder(profileFragment.mContext,
                                "/data/system/theme/");

                        final File profile_apk_files = new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/substratum/profiles/" +
                                profileFragment.profile_selector.getSelectedItem() + "/");
                        final String[] located_files = profile_apk_files.list();
                        for (final String found : located_files) {
                            if (!"audio".equals(found)) {
                                FileOperations.copyDir(profileFragment.mContext, Environment
                                        .getExternalStorageDirectory()
                                        .getAbsolutePath() +
                                        "/substratum/profiles/" +
                                        profileFragment.profile_selector.getSelectedItem() +
                                        "/" + found, current_directory);
                            } else {
                                FileOperations.setPermissions(755, "/data/system/theme/");
                                FileOperations.copyDir(profileFragment.mContext, Environment
                                        .getExternalStorageDirectory()
                                        .getAbsolutePath() +
                                        "/substratum/profiles/" +
                                        profileFragment.profile_selector.getSelectedItem() +
                                        "/" + found + "/", "/data/system/theme/audio/");
                                FileOperations.setPermissionsRecursively(644,
                                        "/data/system/theme/audio/");
                                FileOperations.setPermissions(755, "/data/system/theme/audio/");
                            }
                        }
                        FileOperations.setPermissionsRecursively(644, current_directory);
                        FileOperations.setPermissions(755, current_directory);
                        FileOperations.setContext(current_directory);
                        FileOperations.mountRO();

                        // Restore wallpaper
                        new ContinueRestore(profileFragment).execute();
                    }
                    final AlertDialog.Builder alertDialogBuilder =
                            new AlertDialog.Builder(profileFragment.mContext);
                    alertDialogBuilder
                            .setTitle(profileFragment.getString(
                                    R.string.legacy_dialog_soft_reboot_title));
                    alertDialogBuilder
                            .setMessage(profileFragment.getString(
                                    R.string.legacy_dialog_soft_reboot_text));
                    alertDialogBuilder
                            .setPositiveButton(
                                    android.R.string.ok, (dialog, id) -> ElevatedCommands.reboot());
                    alertDialogBuilder.setCancelable(false);
                    final AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
                profileFragment.headerProgress.setVisibility(View.GONE);
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final ProfileFragment profileFragment = this.ref.get();
            if (profileFragment != null) {
                if (Systems.checkOMS(profileFragment.mContext)) {  // RRO doesn't need this
                    this.profile_name = sUrl[0];
                    profileFragment.cannot_run_overlays = new ArrayList<>();
                    profileFragment.dialog_message = new StringBuilder();
                    profileFragment.to_be_run_commands = "";

                    final File overlays = new File(
                            Environment.getExternalStorageDirectory().getAbsolutePath()
                                    + "/substratum/profiles/" + this.profile_name + "/overlay_state" +
                                    ".xml");

                    if (overlays.exists()) {
                        final List<List<String>> profile =
                                ProfileManager.readProfileStatePackageWithTargetPackage(
                                        this.profile_name, STATE_ENABLED);
                        this.system = ThemeManager.listAllOverlays(profileFragment.mContext);

                        // Now process the overlays to be enabled
                        for (int i = 0, size = profile.size(); i < size; i++) {
                            final String packageName = profile.get(i).get(0);
                            final String targetPackage = profile.get(i).get(1);
                            if (Packages.isPackageInstalled(profileFragment.mContext,
                                    targetPackage)) {
                                if (this.system.contains(packageName)) {
                                    this.to_be_run.add(packageName);
                                } else {
                                    profileFragment.cannot_run_overlays.add(profile.get(i));
                                }
                            }
                        }

                        // Parse non-exist profile overlay packages
                        for (int i = 0; i < profileFragment.cannot_run_overlays.size(); i++) {
                            final String packageName = profileFragment.cannot_run_overlays.get(i).get(0);
                            final String targetPackage =
                                    profileFragment.cannot_run_overlays.get(i).get(1);
                            final String packageDetail = packageName.replace(targetPackage + ".", "");
                            final String detailSplit = Arrays.toString(packageDetail.split("\\."))
                                    .replace("[", "")
                                    .replace("]", "")
                                    .replace(",", " ");

                            if (profileFragment.dialog_message.length() == 0) {
                                profileFragment.dialog_message
                                        .append("\u2022 ")
                                        .append(targetPackage)
                                        .append(" (")
                                        .append(detailSplit)
                                        .append(")");
                            } else {
                                profileFragment.dialog_message
                                        .append("\n")
                                        .append("\u2022 ")
                                        .append(targetPackage)
                                        .append(" (")
                                        .append(detailSplit)
                                        .append(")");
                            }
                        }
                    }
                } else {
                    final String profile_name = sUrl[0];
                    profileFragment.to_be_run_commands += " && mount -o rw,remount /system";
                    profileFragment.to_be_run_commands = profileFragment.to_be_run_commands +
                            " && mv -f /system/media/bootanimation.zip" +
                            " /system/media/bootanimation-backup.zip";
                    profileFragment.to_be_run_commands =
                            profileFragment.to_be_run_commands + " && cp -f " +
                                    Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/substratum/profiles/" + profile_name + "/ /system/media/";
                    profileFragment.to_be_run_commands = profileFragment.to_be_run_commands +
                            " && chmod 644 /system/media/bootanimation.zip" +
                            " && mount -o ro,remount /system";
                }
            }
            return null;
        }
    }

    private static class ContinueRestore extends AsyncTask<Void, String, Void> {
        private final String TAG = "ContinueRestore";
        private String profileName;
        private List<List<String>> toBeCompiled;
        private ArrayList<String> toBeRun;
        private ProgressDialog progressDialog;
        private LocalBroadcastManager localBroadcastManager;
        private KeyRetrieval keyRetrieval;
        private Intent securityIntent;
        private Cipher cipher;
        private final Handler handler = new Handler();
        private final WeakReference<ProfileFragment> ref;
        private final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(ContinueRestore.this.TAG, "Waiting for encryption key handshake approval...");
                if (ContinueRestore.this.securityIntent != null) {
                    Log.d(ContinueRestore.this.TAG, "Encryption key handshake approved!");
                    ContinueRestore.this.handler.removeCallbacks(ContinueRestore.this.runnable);
                } else {
                    Log.d(ContinueRestore.this.TAG, "Encryption key still null...");
                    try {
                        Thread.sleep(500);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    ContinueRestore.this.handler.postDelayed(this, 100);
                }
            }
        };

        ContinueRestore(final ProfileFragment profileFragment) {
            super();
            this.ref = new WeakReference<>(profileFragment);
        }

        ContinueRestore(final ProfileFragment profileFragment,
                        final String profileName,
                        final ArrayList<String> tobeRun) {
            super();
            this.ref = new WeakReference<>(profileFragment);
            this.profileName = profileName;
            this.toBeRun = tobeRun;
        }

        ContinueRestore(final ProfileFragment profileFragment,
                        final String profileName,
                        final List<List<String>> toBeCompiled,
                        final ArrayList<String> toBeRun) {
            super();
            this.ref = new WeakReference<>(profileFragment);
            this.profileName = profileName;
            this.toBeCompiled = toBeCompiled;
            this.toBeRun = toBeRun;
        }

        @Override
        protected void onPreExecute() {
            final ProfileFragment profileFragment = this.ref.get();
            if (profileFragment != null) {
                this.progressDialog = new ProgressDialog(profileFragment.mContext);
                this.progressDialog.setIndeterminate(true);
                this.progressDialog.setCancelable(false);
                this.progressDialog.setMessage(
                        profileFragment.getString(R.string.profile_restoration_message));
                this.progressDialog.show();
                if (this.progressDialog.getWindow() != null) this.progressDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                final File directory = new File(EXTERNAL_STORAGE_CACHE);
                if (!directory.exists()) {
                    FileOperations.createNewFolder(EXTERNAL_STORAGE_CACHE);
                }
                if (this.toBeCompiled != null) {
                    if (Systems.checkThemeInterfacer(profileFragment.mContext) &&
                            !Systems.isBinderInterfacer(profileFragment.mContext)) {
                        if (profileFragment.finishReceiver == null)
                            profileFragment.finishReceiver = new FinishReceiver(profileFragment);
                        final IntentFilter filter = new IntentFilter(STATUS_CHANGED);
                        profileFragment.mContext.getApplicationContext().registerReceiver(
                                profileFragment.finishReceiver, filter);
                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(final String... progress) {
            this.progressDialog.setMessage(progress[0]);
        }

        @Override
        protected Void doInBackground(final Void... params) {
            final ProfileFragment profileFragment = this.ref.get();
            if (profileFragment != null) {
                if (this.toBeCompiled != null) {
                    final Map<String, ProfileItem> items =
                            ProfileManager.readProfileState(this.profileName, STATE_ENABLED);

                    String prevTheme = "";
                    for (int i = 0; i < this.toBeCompiled.size(); i++) {
                        final String compilePackage = this.toBeCompiled.get(i).get(0);
                        final ProfileItem currentItem = items.get(compilePackage);

                        @SuppressLint("StringFormatMatches") final
                        // Seems like there's a bug with lint according to
                                // https://stackoverflow.com/questions/23960019/
                                // lint-gives-wrong-format-type-when-using-long-values-in-strings
                                // -xml

                                String format = String.format(
                                profileFragment.getString(R.string.profile_compile_progress),
                                i + 1,
                                this.toBeCompiled.size(),
                                compilePackage);
                        this.publishProgress(format);

                        final String theme = currentItem.getParentTheme();

                        Boolean encrypted = false;
                        final String encrypt_check =
                                Packages.getOverlayMetadata(
                                        profileFragment.mContext, theme, metadataEncryption);

                        if (encrypt_check != null && encrypt_check.equals
                                (metadataEncryptionValue) &&
                                !theme.equals(prevTheme)) {
                            prevTheme = theme;
                            Log.d(this.TAG, "This overlay for " +
                                    Packages.getPackageName(profileFragment.mContext, theme) +
                                    " is encrypted, passing handshake to the theme package...");
                            encrypted = true;

                            Theming.getThemeKeys(profileFragment.mContext, theme);

                            this.keyRetrieval = new KeyRetrieval();
                            final IntentFilter if1 = new IntentFilter(KEY_RETRIEVAL);
                            this.localBroadcastManager = LocalBroadcastManager.getInstance(
                                    profileFragment.mContext);
                            this.localBroadcastManager.registerReceiver(this.keyRetrieval, if1);

                            int counter = 0;
                            this.handler.postDelayed(this.runnable, 100);
                            while (this.securityIntent == null && counter < 5) {
                                try {
                                    Thread.sleep(500);
                                } catch (final InterruptedException e) {
                                    e.printStackTrace();
                                }
                                counter++;
                            }
                            if (counter > 5) {
                                Log.e(this.TAG, "Could not receive handshake in time...");
                                return null;
                            }

                            if (this.securityIntent != null) {
                                try {
                                    final byte[] encryption_key =
                                            this.securityIntent.getByteArrayExtra("encryption_key");
                                    final byte[] iv_encrypt_key =
                                            this.securityIntent.getByteArrayExtra("iv_encrypt_key");

                                    this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                    this.cipher.init(
                                            Cipher.DECRYPT_MODE,
                                            new SecretKeySpec(encryption_key, "AES"),
                                            new IvParameterSpec(iv_encrypt_key)
                                    );
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            }
                        }

                        final AssetManager themeAssetManager;
                        Resources themeResources = null;
                        try {
                            themeResources = profileFragment.mContext.getPackageManager()
                                    .getResourcesForApplication(theme);
                        } catch (final PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                        assert themeResources != null;
                        themeAssetManager = themeResources.getAssets();

                        final String target = currentItem.getTargetPackage();
                        final String type1a = currentItem.getType1a();
                        final String type1b = currentItem.getType1b();
                        final String type1c = currentItem.getType1c();
                        final String type2 = currentItem.getType2();
                        final String type3 = currentItem.getType3();
                        final String type4 = currentItem.getType4();

                        final String type1aDir = "overlays/" + target + "/type1a_" + type1a +
                                (encrypted ? ".xml.enc" : ".xml");
                        final String type1bDir = "overlays/" + target + "/type1b_" + type1b +
                                (encrypted ? ".xml.enc" : ".xml");
                        final String type1cDir = "overlays/" + target + "/type1c_" + type1c +
                                (encrypted ? ".xml.enc" : ".xml");

                        final String additional_variant = (type2.length() > 0 ? type2 : null);
                        final String base_variant = (type3.length() > 0 ? type3 : null);

                        // Prenotions
                        final String suffix = (type3.length() != 0 ?
                                "/type3_" + type3 : "/res");
                        final String workingDirectory =
                                profileFragment.mContext.getCacheDir().getAbsolutePath() +
                                        SUBSTRATUM_BUILDER_CACHE.substring(0,
                                                SUBSTRATUM_BUILDER_CACHE.length() - 1);
                        final File created = new File(workingDirectory);
                        if (created.exists()) {
                            FileOperations.delete(
                                    profileFragment.mContext, created.getAbsolutePath());
                        }
                        FileOperations.createNewFolder(
                                profileFragment.mContext, created.getAbsolutePath());

                        // Handle the resource folder
                        final String listDir = "overlays/" + target + suffix;
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                listDir,
                                workingDirectory + suffix,
                                listDir,
                                (encrypted ? this.cipher : null));

                        // Handle the type1s
                        if (type1a.length() > 0) {
                            FileOperations.copyFileOrDir(
                                    themeAssetManager,
                                    type1aDir,
                                    workingDirectory + suffix + "/values/type1a.xml",
                                    type1aDir,
                                    (encrypted ? this.cipher : null));
                        }
                        if (type1b.length() > 0) {
                            FileOperations.copyFileOrDir(
                                    themeAssetManager,
                                    type1bDir,
                                    workingDirectory + suffix + "/values/type1b.xml",
                                    type1bDir,
                                    (encrypted ? this.cipher : null));
                        }
                        if (type1c.length() > 0) {
                            FileOperations.copyFileOrDir(
                                    themeAssetManager,
                                    type1cDir,
                                    workingDirectory + suffix + "/values/type1c.xml",
                                    type1cDir,
                                    (encrypted ? this.cipher : null));
                        }

                        final SubstratumBuilder sb = new SubstratumBuilder();
                        sb.beginAction(
                                profileFragment.mContext,
                                target,
                                Packages.getPackageName(profileFragment.mContext, theme),
                                compilePackage,
                                additional_variant,
                                base_variant,
                                Packages.getAppVersion(profileFragment.mContext,
                                        currentItem.getParentTheme()),
                                Systems.checkOMS(profileFragment.mContext),
                                theme,
                                suffix,
                                type1a,
                                type1b,
                                type1c,
                                type2,
                                type3,
                                type4,
                                compilePackage,
                                false
                        );
                        if (Systems.checkThemeInterfacer(profileFragment.mContext) &&
                                !Systems.isBinderInterfacer(profileFragment.mContext)) {
                            // Thread wait
                            profileFragment.isWaiting = true;
                            do {
                                try {
                                    Thread.sleep(THREAD_WAIT_DURATION);
                                } catch (final InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            } while (profileFragment.isWaiting);
                        }

                        // Add current package to enable queue
                        this.toBeRun.add(compilePackage);
                    }
                }

                this.publishProgress(profileFragment.getString(R.string.profile_compile_processing));
                if (this.profileName != null && this.toBeRun != null) this.continueProcess();
                this.continueProcessWallpaper();
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            final ProfileFragment profileFragment = this.ref.get();
            if (profileFragment != null) {
                try {
                    this.progressDialog.dismiss();
                    profileFragment.mContext.getApplicationContext().unregisterReceiver(
                            profileFragment.finishReceiver);
                } catch (final IllegalArgumentException e) {
                    // detached already
                }
            }
        }

        void continueProcess() {
            final ProfileFragment profileFragment = this.ref.get();
            if (profileFragment != null) {

                final File theme = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/" + this.profileName + "/theme");

                // Encrypted devices boot Animation
                final File bootanimation = new File(theme, "bootanimation.zip");
                if (bootanimation.exists() &&
                        Systems.getDeviceEncryptionStatus(profileFragment.mContext) > 1) {
                    FileOperations.mountRW();
                    FileOperations.move(profileFragment.mContext, "/system/media/bootanimation.zip",
                            "/system/madia/bootanimation-backup.zip");
                    FileOperations.copy(profileFragment.mContext, bootanimation.getAbsolutePath(),
                            "/system/media/bootanimation.zip");
                    FileOperations.setPermissions(644, "/system/media/bootanimation.zip");
                    FileOperations.mountRO();
                }

                if (Systems.checkThemeInterfacer(profileFragment.mContext)) {
                    final ArrayList<String> toBeDisabled =
                            new ArrayList<>(ThemeManager.listOverlays(
                                    profileFragment.mContext, STATE_ENABLED));
                    final boolean shouldRestartUi = ThemeManager.shouldRestartUI(
                            profileFragment.mContext, toBeDisabled)
                            || ThemeManager.shouldRestartUI(profileFragment.mContext, this.toBeRun);
                    ThemeInterfacerService.applyProfile(
                            profileFragment.mContext,
                            this.profileName,
                            toBeDisabled,
                            this.toBeRun,
                            shouldRestartUi);
                } else {
                    // Restore the whole backed up profile back to /data/system/theme/
                    if (theme.exists()) {
                        FileOperations.delete(profileFragment.mContext, "/data/system/theme",
                                false);
                        FileOperations.copyDir(profileFragment.mContext, theme.getAbsolutePath(),
                                "/data/system/theme");
                        FileOperations.setPermissionsRecursively(644, "/data/system/theme/audio");
                        FileOperations.setPermissions(755, "/data/system/theme/audio");
                        FileOperations.setPermissions(755, "/data/system/theme/audio/alarms");
                        FileOperations.setPermissions(755,
                                "/data/system/theme/audio/notifications");

                        FileOperations.setPermissions(755, "/data/system/theme/audio/ringtones");
                        FileOperations.setPermissions(755, "/data/system/theme/audio/ringtones");
                        FileOperations.setPermissionsRecursively(644, "/data/system/theme/fonts/");
                        FileOperations.setPermissions(755, "/data/system/theme/fonts/");
                        FileOperations.setContext("/data/system/theme");
                    }

                    ThemeManager.disableAllThemeOverlays(profileFragment.mContext);
                    ThemeManager.enableOverlay(profileFragment.mContext, this.toBeRun);
                }
            }
        }

        void continueProcessWallpaper() {
            final ProfileFragment profileFragment = this.ref.get();
            if (profileFragment != null) {
                final String homeWallPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/" + this.profileName + "/wallpaper.png";
                final String lockWallPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/" + this.profileName + "/wallpaper_lock.png";
                final File homeWall = new File(homeWallPath);
                final File lockWall = new File(lockWallPath);
                if (homeWall.exists() || lockWall.exists()) {
                    try {
                        WallpaperManager.setWallpaper(
                                profileFragment.mContext, homeWallPath, "home");
                        WallpaperManager.setWallpaper(
                                profileFragment.mContext, lockWallPath, "lock");
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        class KeyRetrieval extends BroadcastReceiver {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                ContinueRestore.this.securityIntent = intent;
            }
        }
    }

    static class FinishReceiver extends BroadcastReceiver {

        private final WeakReference<ProfileFragment> ref;

        FinishReceiver(final ProfileFragment profileFragment) {
            super();
            this.ref = new WeakReference<>(profileFragment);
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String PRIMARY_COMMAND_KEY = "primary_command_key";
            final String COMMAND_VALUE_JOB_COMPLETE = "job_complete";
            final String command = intent.getStringExtra(PRIMARY_COMMAND_KEY);

            if (command.equals(COMMAND_VALUE_JOB_COMPLETE)) {
                final ProfileFragment profileFragment = this.ref.get();
                if (profileFragment != null) {
                    profileFragment.isWaiting = false;
                }
            }
        }
    }
}