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
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
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
import projekt.substratum.Substratum;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.Theming;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.SubstratumService;
import projekt.substratum.common.platform.ThemeInterfacerService;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.systems.ProfileItem;
import projekt.substratum.common.systems.ProfileManager;
import projekt.substratum.databinding.ProfileFragmentBinding;
import projekt.substratum.tabs.WallpapersManager;
import projekt.substratum.util.compilers.SubstratumBuilder;
import projekt.substratum.util.views.Lunchbar;

import static projekt.substratum.common.Internal.ALARM_THEME_DIRECTORY;
import static projekt.substratum.common.Internal.AUDIO_THEME_DIRECTORY;
import static projekt.substratum.common.Internal.BOOTANIMATION;
import static projekt.substratum.common.Internal.BOOTANIMATION_BU_LOCATION;
import static projekt.substratum.common.Internal.BOOTANIMATION_LOCATION;
import static projekt.substratum.common.Internal.CIPHER_ALGORITHM;
import static projekt.substratum.common.Internal.ENCRYPTED_FILE_EXTENSION;
import static projekt.substratum.common.Internal.ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.FONTS_THEME_DIRECTORY;
import static projekt.substratum.common.Internal.HIDDEN_FOLDER;
import static projekt.substratum.common.Internal.HOME_WALLPAPER;
import static projekt.substratum.common.Internal.IV_ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.LOCK_WALL;
import static projekt.substratum.common.Internal.LOCK_WALLPAPER;
import static projekt.substratum.common.Internal.LOCK_WALLPAPER_FILE_NAME;
import static projekt.substratum.common.Internal.MAIN_FOLDER;
import static projekt.substratum.common.Internal.NOTIF_THEME_DIRECTORY;
import static projekt.substratum.common.Internal.NO_MEDIA;
import static projekt.substratum.common.Internal.OVERLAY_DIR;
import static projekt.substratum.common.Internal.OVERLAY_STATE_FILE;
import static projekt.substratum.common.Internal.PROFILE_AUDIO;
import static projekt.substratum.common.Internal.PROFILE_BOOTANIMATIONS;
import static projekt.substratum.common.Internal.PROFILE_DIRECTORY;
import static projekt.substratum.common.Internal.PROFILE_FONTS;
import static projekt.substratum.common.Internal.RINGTONE_THEME_DIRECTORY;
import static projekt.substratum.common.Internal.SECRET_KEY_SPEC;
import static projekt.substratum.common.Internal.SPECIAL_SNOWFLAKE_DELAY;
import static projekt.substratum.common.Internal.SYSTEM_OVERLAY;
import static projekt.substratum.common.Internal.SYSTEM_VENDOR_OVERLAY;
import static projekt.substratum.common.Internal.THEME_644;
import static projekt.substratum.common.Internal.THEME_755;
import static projekt.substratum.common.Internal.THEME_DIR;
import static projekt.substratum.common.Internal.THEME_DIRECTORY;
import static projekt.substratum.common.Internal.USERS_DIR;
import static projekt.substratum.common.Internal.WALLPAPER_DIR;
import static projekt.substratum.common.Internal.WALLPAPER_FILE_NAME;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.KEY_RETRIEVAL;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
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

    public static int nightHour;
    public static int nightMinute;
    public static int dayHour;
    public static int dayMinute;
    ProgressBar headerProgress;
    EditText backupName;
    Button backupButton;
    Spinner profileSelector;
    ImageButton imageButton;
    Button restoreButton;
    CardView scheduledProfileCard;
    ExpandableLayout scheduledProfileLayout;
    Switch dayNightSwitch;
    Button startTime;
    Button endTime;
    Spinner dayProfile;
    Spinner nightProfile;
    Button applyScheduledProfileButton;
    private Context context;
    private List<String> list;
    private String backupGetText;
    private String toBeRunCommands;
    private ArrayAdapter<String> adapter;
    private List<List<String>> cannotRunOverlays;
    private StringBuilder dialogMessage;
    private boolean dayNightEnabled;
    private ArrayList<CharSequence> selectedBackup;
    private ArrayList<String> lateInstall;

    /**
     * Set Night Profile Start
     *
     * @param hour   Hour
     * @param minute Minute
     */
    public static void setNightProfileStart(int hour, int minute) {
        nightHour = hour;
        nightMinute = minute;
    }

    /**
     * Set Day Profile Start
     *
     * @param hour   Hour
     * @param minute Minute
     */
    public static void setDayProfileStart(int hour, int minute) {
        dayHour = hour;
        dayMinute = minute;
    }

    /**
     * Refresh the spinners
     */
    private void refreshSpinner() {
        list.clear();
        list.add(getResources().getString(R.string.spinner_default_item));

        // Now lets add all the located profiles
        File f = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() + PROFILE_DIRECTORY);
        File[] files = f.listFiles();
        if (files != null) {
            for (File inFile : files) {
                if (inFile.isDirectory()) {
                    list.add(inFile.getName());
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        ProfileFragmentBinding viewBinding = DataBindingUtil
                .inflate(inflater, R.layout.profile_fragment, container, false);
        View view = viewBinding.getRoot();
        headerProgress = viewBinding.headerLoadingBar;
        backupName = viewBinding.backupCardProfileNameEntry;
        backupButton = viewBinding.backupCardActionButton;
        profileSelector = viewBinding.restoreCardProfileSelectSpinner;
        imageButton = viewBinding.restoreCardDeleteButton;
        restoreButton = viewBinding.restoreCardActionButton;
        scheduledProfileCard = viewBinding.scheduledProfilesCard;
        scheduledProfileLayout = viewBinding.scheduledProfileCardContentContainer;
        dayNightSwitch = viewBinding.scheduledProfilesEnableSwitch;
        startTime = viewBinding.nightStartTime;
        endTime = viewBinding.nightEndTime;
        dayProfile = viewBinding.daySpinner;
        nightProfile = viewBinding.nightSpinner;
        applyScheduledProfileButton = viewBinding.applyScheduleButton;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        headerProgress.setVisibility(View.GONE);

        // Create a user viewable directory for profiles
        File directory = new File(Environment.getExternalStorageDirectory(), MAIN_FOLDER);
        if (!directory.exists() && !directory.mkdirs()) {
            Log.e(References.SUBSTRATUM_LOG, "Could not create Substratum directory...");
        }
        File directory2 = new File(
                Environment.getExternalStorageDirectory(), PROFILE_DIRECTORY);
        if (!directory2.exists() && directory2.mkdirs()) {
            Log.e(References.SUBSTRATUM_LOG, "Could not create profile directory...");
        }

        // Restrict whitespace for profile name
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (Character.isWhitespace(source.charAt(i))) {
                    Toast.makeText(context,
                            R.string.profile_edittext_whitespace_warning_toast,
                            Toast.LENGTH_LONG)
                            .show();
                    return "";
                }
            }
            return null;
        };
        backupName.setFilters(new InputFilter[]{filter});
        backupName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                assert getActivity() != null;
                InputMethodManager imm = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(backupName.getWindowToken(),
                            InputMethodManager.RESULT_UNCHANGED_SHOWN);
                }
            }
        });

        backupButton.setOnClickListener(v -> {
            if (backupName.getText().length() > 0) {
                selectedBackup = new ArrayList<>();
                CharSequence[] items;
                if (Systems.checkOMS(context) ||
                        projekt.substratum.common.Resources.isFontsSupported(context)) {
                    items = new CharSequence[]{
                            getString(R.string.profile_boot_animation),
                            getString(R.string.profile_font),
                            getString(R.string.profile_overlay),
                            getString(R.string.profile_sound),
                            getString(R.string.profile_wallpaper)};
                } else {
                    items = new CharSequence[]{
                            getString(R.string.profile_boot_animation),
                            getString(R.string.profile_overlay),
                            getString(R.string.profile_sound),
                            getString(R.string.profile_wallpaper)};
                }

                AlertDialog dialog = new AlertDialog.Builder(context)
                        .setTitle(R.string.profile_dialog_title)
                        .setMultiChoiceItems(items, null, (dialog1, which, isChecked) -> {
                            if (isChecked) {
                                if (items[which].equals(getString(R.string
                                        .profile_boot_animation))
                                        && (Systems.getDeviceEncryptionStatus(context) > 1)
                                        && Systems.checkThemeInterfacer(context)) {
                                    AlertDialog dialog2 = new AlertDialog.Builder(context)
                                            .setTitle(R.string.root_required_title)
                                            .setMessage(R.string
                                                    .root_required_boot_animation_profile)
                                            .setPositiveButton(android.R.string.ok, null)
                                            .create();
                                    dialog2.show();
                                }
                                selectedBackup.add(items[which]);
                            } else selectedBackup.remove(items[which]);
                        })
                        .setPositiveButton(R.string.profile_dialog_ok, null)
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .create();

                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                    if (!selectedBackup.isEmpty()) {
                        backupGetText = backupName.getText().toString();
                        BackupFunction backupFunction = new BackupFunction(this);
                        backupFunction.execute();
                        Log.d(References.SUBSTRATUM_LOG, selectedBackup.toString());
                        dialog.dismiss();
                        backupName.getText().clear();
                    } else {
                        Toast.makeText(context, R.string.profile_no_selection_warning,
                                Toast.LENGTH_LONG).show();
                    }
                });

                assert getActivity() != null;
                InputMethodManager imm = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(backupButton.getWindowToken(),
                            InputMethodManager.RESULT_UNCHANGED_SHOWN);
                }
            } else {
                if (getView() != null) {
                    Lunchbar.make(getView(),
                            getString(R.string.profile_edittext_empty_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        });

        list = new ArrayList<>();
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item,
                list);
        refreshSpinner();

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileSelector.setAdapter(adapter);

        imageButton.setOnClickListener(v -> {
            if (profileSelector.getSelectedItemPosition() > 0) {
                String formatted = String.format(getString(R.string.delete_dialog_text),
                        profileSelector.getSelectedItem());
                new AlertDialog.Builder(context)
                        .setTitle(getString(R.string.delete_dialog_title))
                        .setMessage(formatted)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.delete_dialog_okay),
                                (dialog, which) -> {
                                    File f1 = new File(Environment
                                            .getExternalStorageDirectory().getAbsolutePath() +
                                            PROFILE_DIRECTORY + profileSelector
                                            .getSelectedItem() + HIDDEN_FOLDER);
                                    boolean deleted = f1.delete();
                                    if (!deleted)
                                        Log.e(References.SUBSTRATUM_LOG,
                                                "Could not delete profile directory.");
                                    FileOperations.delete(context,
                                            Environment.getExternalStorageDirectory()
                                                    .getAbsolutePath() +
                                                    PROFILE_DIRECTORY +
                                                    profileSelector.getSelectedItem());
                                    refreshSpinner();
                                })
                        .setNegativeButton(getString(R.string.dialog_cancel),
                                (dialog, which) -> dialog.cancel())
                        .create()
                        .show();
            } else {
                if (getView() != null) {
                    Lunchbar.make(getView(),
                            getString(R.string.profile_delete_button_none_selected_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        });

        restoreButton.setOnClickListener(v -> {
            if (profileSelector.getSelectedItemPosition() > 0) {
                RestoreFunction restoreFunction = new RestoreFunction(this);
                restoreFunction.execute(profileSelector.getSelectedItem().toString());
            } else {
                if (getView() != null) {
                    Lunchbar.make(getView(),
                            getString(R.string.restore_button_none_selected_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        });

        if (Systems.checkOMS(context) &&
                (Systems.checkThemeInterfacer(getContext()) ||
                        Systems.checkSubstratumService(getContext()))) {
            dayNightSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) {
                    scheduledProfileLayout.expand();
                } else {
                    scheduledProfileLayout.collapse();
                }
                dayNightEnabled = b;
            });

            assert getActivity() != null;
            FragmentManager fm = getActivity().getSupportFragmentManager();
            startTime.setOnClickListener(v -> {
                DialogFragment timePickerFragment = new TimePickerFragment();
                if (startTime.getText().equals(getResources().getString(R.string.start_time)
                )) {
                    TimePickerFragment.setFlag(TimePickerFragment.FLAG_START_TIME);
                } else {
                    TimePickerFragment.setFlag(
                            TimePickerFragment.FLAG_START_TIME | TimePickerFragment.FLAG_GET_VALUE);
                }
                timePickerFragment.show(fm, "TimePicker");
            });

            endTime.setOnClickListener(v -> {
                DialogFragment timePickerFragment = new TimePickerFragment();
                if (endTime.getText().equals(getResources().getString(R.string.end_time))) {
                    TimePickerFragment.setFlag(TimePickerFragment.FLAG_END_TIME);
                } else {
                    TimePickerFragment.setFlag(
                            TimePickerFragment.FLAG_END_TIME | TimePickerFragment.FLAG_GET_VALUE);
                }
                timePickerFragment.show(fm, "TimePicker");
            });

            dayProfile.setAdapter(adapter);
            nightProfile.setAdapter(adapter);

            if (prefs.getBoolean(SCHEDULED_PROFILE_ENABLED, false)) {
                String day = prefs.getString(DAY_PROFILE, getResources()
                        .getString(R.string.spinner_default_item));
                String night = prefs.getString(NIGHT_PROFILE, getResources()
                        .getString(R.string.spinner_default_item));
                dayHour = prefs.getInt(DAY_PROFILE_HOUR, 0);
                dayMinute = prefs.getInt(DAY_PROFILE_MINUTE, 0);
                nightHour = prefs.getInt(NIGHT_PROFILE_HOUR, 0);
                nightMinute = prefs.getInt(NIGHT_PROFILE_MINUTE, 0);
                dayNightEnabled = true;

                scheduledProfileLayout.expand(false);
                dayNightSwitch.setChecked(true);
                startTime.setText(References.parseTime(getActivity(), nightHour, nightMinute));
                endTime.setText(References.parseTime(getActivity(), dayHour, dayMinute));
                dayProfile.setSelection(adapter.getPosition(day));
                nightProfile.setSelection(adapter.getPosition(night));
            }

            applyScheduledProfileButton.setOnClickListener(v -> {
                if (dayNightEnabled) {
                    if ((dayProfile.getSelectedItemPosition() > 0) &&
                            (nightProfile.getSelectedItemPosition() > 0)) {
                        if (!startTime.getText().equals(getResources()
                                .getString(R.string.start_time)) && !endTime.getText()
                                .equals(getResources().getString(R.string.end_time))) {
                            if ((dayHour != nightHour) || (dayMinute != nightMinute)) {
                                ProfileManager.enableScheduledProfile(getActivity(),
                                        dayProfile.getSelectedItem().toString(), dayHour,
                                        dayMinute,
                                        nightProfile.getSelectedItem().toString(), nightHour,
                                        nightMinute);
                                if (getView() != null) {
                                    Lunchbar.make(getView(),
                                            getString(R.string.scheduled_profile_apply_success),
                                            Snackbar.LENGTH_LONG)
                                            .show();
                                }
                            } else {
                                if (getView() != null) {
                                    Lunchbar.make(getView(), getString(R.string.time_equal_warning),
                                            Snackbar.LENGTH_LONG)
                                            .show();
                                }
                            }
                        } else {
                            if (getView() != null) {
                                Lunchbar.make(getView(), getString(R.string.time_empty_warning),
                                        Snackbar.LENGTH_LONG)
                                        .show();
                            }
                        }
                    } else {
                        if (getView() != null) {
                            Lunchbar.make(getView(), getString(R.string.profile_empty_warning),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                    }
                } else {
                    ProfileManager.disableScheduledProfile(getActivity());
                    if (getView() != null) {
                        Lunchbar.make(getView(),
                                getString(R.string.scheduled_profile_disable_success),
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                }
            });
        } else {
            scheduledProfileCard.setVisibility(View.GONE);
        }
        return view;
    }

    /**
     * Backup function to restore a profile on the device
     */
    private static class BackupFunction extends AsyncTask<String, Integer, String> {

        private WeakReference<ProfileFragment> ref;

        BackupFunction(ProfileFragment profileFragment) {
            super();
            ref = new WeakReference<>(profileFragment);
        }

        @Override
        protected void onPreExecute() {
            ProfileFragment profileFragment = ref.get();
            if (profileFragment != null) {
                profileFragment.headerProgress.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ProfileFragment profileFragment = ref.get();
            if (profileFragment != null) {
                profileFragment.headerProgress.setVisibility(View.GONE);
                if (Systems.checkOMS(profileFragment.context)) {
                    String directory_parse = String.format(
                            profileFragment.getString(R.string.toast_backup_success),
                            profileFragment.backupGetText);
                    if (profileFragment.getView() != null) {
                        Lunchbar.make(profileFragment.getView(),
                                directory_parse,
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                } else {
                    String directory_parse = String.format(
                            profileFragment.getString(R.string.toast_backup_success),
                            profileFragment.backupGetText + '/');
                    if (profileFragment.getView() != null) {
                        Lunchbar.make(profileFragment.getView(),
                                directory_parse,
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                }
                profileFragment.refreshSpinner();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            ProfileFragment profileFragment = ref.get();
            if (profileFragment != null) {
                String uid =
                        Environment.getExternalStorageDirectory().getAbsolutePath().split("/")[3];
                File nomediaFile = new File(Environment.getExternalStorageDirectory() +
                        NO_MEDIA);
                try {
                    if (!nomediaFile.createNewFile()) {
                        Log.d(References.SUBSTRATUM_LOG, "Could not create .nomedia file or" +
                                " file already exist!");
                    }
                } catch (IOException e) {
                    Log.d(References.SUBSTRATUM_LOG, "Could not create .nomedia file!");
                    e.printStackTrace();
                }

                if (Systems.checkOMS(profileFragment.context)) {
                    File profileDir = new File(Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                            PROFILE_DIRECTORY + profileFragment.backupGetText + '/');
                    if (profileDir.exists()) {
                        FileOperations.delete(profileFragment.context,
                                Environment.getExternalStorageDirectory().getAbsolutePath() +
                                        PROFILE_DIRECTORY + profileFragment.backupGetText);
                        if (!profileDir.mkdir())
                            Log.e(References.SUBSTRATUM_LOG, "Could not create profile directory.");
                    } else {
                        if (!profileDir.mkdir())
                            Log.e(References.SUBSTRATUM_LOG, "Could not create profile directory.");
                    }

                    File profileFile = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            PROFILE_DIRECTORY + profileFragment.backupGetText +
                            '/' + OVERLAY_STATE_FILE);
                    if (profileFile.exists()) {
                        FileOperations.delete(
                                profileFragment.context,
                                profileFile.getAbsolutePath());
                    }

                    if (profileFragment.selectedBackup.contains(
                            profileFragment.getString(R.string.profile_overlay))) {
                        ProfileManager.writeProfileState(
                                profileFragment.context,
                                profileFragment.backupGetText);
                    }

                    // Backup the entire /data/system/theme/ folder
                    FileOperations.copyDir(profileFragment.context, THEME_DIRECTORY,
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    PROFILE_DIRECTORY + profileFragment.backupGetText + THEME_DIR);

                    // Delete the items user don't want to backup
                    if (!profileFragment.selectedBackup.contains(
                            profileFragment.getString(R.string.profile_boot_animation))) {
                        File bootanimation = new File(profileDir, PROFILE_BOOTANIMATIONS);
                        if (bootanimation.exists()) {
                            FileOperations.delete(profileFragment.context,
                                    bootanimation.getAbsolutePath());
                        }
                    }
                    if (!profileFragment.selectedBackup.contains(
                            profileFragment.getString(R.string.profile_font))) {
                        File fonts = new File(profileDir, PROFILE_FONTS);
                        if (fonts.exists()) {
                            FileOperations.delete(profileFragment.context,
                                    fonts.getAbsolutePath());
                        }
                    }
                    if (!profileFragment.selectedBackup.contains(
                            profileFragment.getString(R.string.profile_sound))) {
                        File sounds = new File(profileDir, PROFILE_AUDIO);
                        if (sounds.exists()) {
                            FileOperations.delete(profileFragment.context,
                                    sounds.getAbsolutePath());
                        }
                    }

                    // Backup wallpapers if wanted
                    if (profileFragment.selectedBackup.contains(
                            profileFragment.getString(R.string.profile_wallpaper))) {
                        FileOperations.copy(profileFragment.context,
                                USERS_DIR + uid + WALLPAPER_DIR,
                                Environment.getExternalStorageDirectory().getAbsolutePath()
                                        + PROFILE_DIRECTORY + profileFragment.backupGetText +
                                        WALLPAPER_FILE_NAME);
                        FileOperations.copy(profileFragment.context, USERS_DIR + uid + LOCK_WALL,
                                Environment.getExternalStorageDirectory().getAbsolutePath()
                                        + PROFILE_DIRECTORY + profileFragment.backupGetText +
                                        LOCK_WALLPAPER_FILE_NAME);
                    }

                    // Backup system boot animation if encrypted
                    if ((Systems.getDeviceEncryptionStatus(profileFragment.context) > 1) &&
                            profileFragment.selectedBackup.contains(
                                    profileFragment.getString(R.string.profile_boot_animation))) {
                        FileOperations.copy(profileFragment.context,
                                BOOTANIMATION_LOCATION,
                                Environment.getExternalStorageDirectory().getAbsolutePath()
                                        + PROFILE_DIRECTORY + profileFragment.backupGetText +
                                        "/" + BOOTANIMATION);
                    }

                    // Clear theme profile folder if empty
                    File profileThemeFolder = new File(profileDir, "theme");
                    if (profileThemeFolder.list() != null) {
                        if (profileThemeFolder.list().length == 0) {
                            Log.d(References.SUBSTRATUM_LOG,
                                    "Profile theme directory is empty! delete " +
                                            (profileThemeFolder.delete() ? "success" : "failed"));
                        }
                    }
                } else {
                    String current_directory;
                    if (projekt.substratum.common.Resources.inNexusFilter()) {
                        current_directory = SYSTEM_OVERLAY;
                    } else {
                        current_directory = SYSTEM_VENDOR_OVERLAY;
                    }
                    File file = new File(current_directory);
                    if (file.exists()) {
                        FileOperations.mountRW();
                        if (profileFragment.selectedBackup.contains(
                                profileFragment.getString(R.string.profile_overlay))) {
                            FileOperations.copyDir(profileFragment.context, current_directory,
                                    Environment.getExternalStorageDirectory().getAbsolutePath() +
                                            PROFILE_DIRECTORY);
                            File oldFolder = new File(Environment
                                    .getExternalStorageDirectory()
                                    .getAbsolutePath() + PROFILE_DIRECTORY + OVERLAY_DIR);
                            File newFolder = new File(Environment
                                    .getExternalStorageDirectory()
                                    .getAbsolutePath() + PROFILE_DIRECTORY +
                                    profileFragment.backupGetText);
                            boolean success = oldFolder.renameTo(newFolder);
                            if (!success)
                                Log.e(References.SUBSTRATUM_LOG,
                                        "Could not move profile directory...");
                        }

                        if (profileFragment.selectedBackup.contains(
                                profileFragment.getString(R.string.profile_sound))) {
                            // Now begin backing up sounds
                            FileOperations.copyDir(profileFragment.context,
                                    AUDIO_THEME_DIRECTORY,
                                    Environment.getExternalStorageDirectory()
                                            .getAbsolutePath() + PROFILE_DIRECTORY
                                            + profileFragment.backupGetText);
                        }
                        FileOperations.mountRO();

                        // Don't forget the wallpaper if wanted
                        if (profileFragment.selectedBackup.contains(
                                profileFragment.getString(R.string.profile_wallpaper))) {
                            File homeWall = new File(USERS_DIR + uid + WALLPAPER_DIR);
                            if (homeWall.exists()) {
                                FileOperations.copy(profileFragment.context, homeWall
                                                .getAbsolutePath(),
                                        Environment.getExternalStorageDirectory().getAbsolutePath()
                                                + PROFILE_DIRECTORY +
                                                profileFragment.backupGetText +
                                                WALLPAPER_FILE_NAME);
                            }
                            File lockWall = new File(USERS_DIR + uid +
                                    LOCK_WALLPAPER_FILE_NAME);
                            if (lockWall.exists()) {
                                FileOperations.copy(profileFragment.context,
                                        lockWall.getAbsolutePath(),
                                        Environment.getExternalStorageDirectory().getAbsolutePath()
                                                + PROFILE_DIRECTORY +
                                                profileFragment.backupGetText +
                                                LOCK_WALLPAPER_FILE_NAME);
                            }
                        }

                        // And boot animation if selected
                        if (profileFragment.selectedBackup.contains(
                                profileFragment.getString(R.string.profile_boot_animation))) {
                            FileOperations.copy(profileFragment.context,
                                    BOOTANIMATION_LOCATION,
                                    Environment.getExternalStorageDirectory().getAbsolutePath()
                                            + PROFILE_DIRECTORY +
                                            profileFragment.backupGetText);
                        }
                    } else {
                        if (profileFragment.getView() != null) {
                            Lunchbar.make(profileFragment.getView(),
                                    profileFragment.getString(R.string.backup_no_overlays),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                    }
                }
            }
            return null;
        }
    }

    /**
     * Restore function to restore a profile on the device
     */
    private static class RestoreFunction extends AsyncTask<String, Integer, String> {
        ArrayList<String> to_be_run = new ArrayList<>(); // Overlays going to be enabled
        List<String> system = new ArrayList<>(); // All installed overlays
        String profile_name;
        private WeakReference<ProfileFragment> ref;

        RestoreFunction(ProfileFragment profileFragment) {
            super();
            ref = new WeakReference<>(profileFragment);
        }

        @Override
        protected void onPreExecute() {
            ProfileFragment profileFragment = ref.get();
            if (profileFragment != null) {
                profileFragment.headerProgress.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ProfileFragment profileFragment = ref.get();
            if (profileFragment != null) {
                if (Systems.checkOMS(profileFragment.context)) {
                    if (!profileFragment.cannotRunOverlays.isEmpty()) {
                        new AlertDialog.Builder(profileFragment.context)
                                .setTitle(profileFragment.getString(R.string.restore_dialog_title))
                                .setMessage(profileFragment.dialogMessage)
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
                                                    profile_name,
                                                    profileFragment.cannotRunOverlays,
                                                    to_be_run)
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
                        new ContinueRestore(profileFragment, profile_name, to_be_run)
                                .execute();
                    }
                } else {
                    String current_directory;
                    if (projekt.substratum.common.Resources.inNexusFilter()) {
                        current_directory = PIXEL_NEXUS_DIR;
                    } else {
                        current_directory = LEGACY_NEXUS_DIR;
                    }
                    File file = new File(current_directory);
                    if (file.exists()) {
                        // Delete destination overlays
                        FileOperations.mountRW();
                        FileOperations.delete(profileFragment.context, current_directory);
                        FileOperations.delete(profileFragment.context, THEME_DIRECTORY);
                        FileOperations.createNewFolder(profileFragment.context, current_directory);
                        FileOperations.createNewFolder(profileFragment.context, THEME_DIRECTORY);
                        FileOperations.setPermissions(THEME_755, THEME_DIRECTORY);

                        File profile_apk_files = new File(Environment
                                .getExternalStorageDirectory()
                                .getAbsolutePath() + PROFILE_DIRECTORY +
                                profileFragment.profileSelector.getSelectedItem() + '/');
                        String[] located_files = profile_apk_files.list();
                        for (String found : located_files) {
                            if (!"audio".equals(found)) {
                                FileOperations.copyDir(profileFragment.context, Environment
                                        .getExternalStorageDirectory()
                                        .getAbsolutePath() +
                                        PROFILE_DIRECTORY +
                                        profileFragment.profileSelector.getSelectedItem() +
                                        '/' + found, current_directory);
                            } else {
                                FileOperations.copyDir(profileFragment.context, Environment
                                        .getExternalStorageDirectory()
                                        .getAbsolutePath() +
                                        PROFILE_DIRECTORY +
                                        profileFragment.profileSelector.getSelectedItem() +
                                        '/' + found + '/', AUDIO_THEME_DIRECTORY);
                                FileOperations.setPermissionsRecursively(THEME_644,
                                        AUDIO_THEME_DIRECTORY);
                                FileOperations.setPermissions(THEME_755, AUDIO_THEME_DIRECTORY);
                            }
                        }
                        FileOperations.setPermissionsRecursively(THEME_644, current_directory);
                        FileOperations.setPermissions(THEME_755, current_directory);
                        FileOperations.setSystemFileContext(current_directory);
                        FileOperations.mountRO();
                    } else {
                        String vendor_location = LEGACY_NEXUS_DIR;
                        String vendor_partition = VENDOR_DIR;
                        String current_vendor =
                                ((projekt.substratum.common.Resources.inNexusFilter()) ?
                                        vendor_partition : vendor_location);
                        FileOperations.mountRW();
                        File vendor = new File(current_vendor);
                        if (!vendor.exists()) {
                            if (current_vendor.equals(vendor_location)) {
                                FileOperations.createNewFolder(current_vendor);
                            } else {
                                FileOperations.mountRWVendor();
                                String vendor_symlink = PIXEL_NEXUS_DIR;
                                FileOperations.createNewFolder(vendor_symlink);
                                FileOperations.symlink(vendor_symlink, "/vendor");
                                FileOperations.setPermissions(755, vendor_partition);
                                FileOperations.mountROVendor();
                            }
                        }

                        FileOperations.delete(profileFragment.context, THEME_DIRECTORY);
                        FileOperations.createNewFolder(profileFragment.context, THEME_DIRECTORY);

                        File profile_apk_files = new File(Environment
                                .getExternalStorageDirectory()
                                .getAbsolutePath() + PROFILE_DIRECTORY +
                                profileFragment.profileSelector.getSelectedItem() + '/');
                        String[] located_files = profile_apk_files.list();
                        for (String found : located_files) {
                            if (!"audio".equals(found)) {
                                FileOperations.copyDir(profileFragment.context, Environment
                                        .getExternalStorageDirectory()
                                        .getAbsolutePath() +
                                        PROFILE_DIRECTORY +
                                        profileFragment.profileSelector.getSelectedItem() +
                                        '/' + found, current_directory);
                            } else {
                                FileOperations.setPermissions(755, THEME_DIRECTORY);
                                FileOperations.copyDir(profileFragment.context, Environment
                                        .getExternalStorageDirectory()
                                        .getAbsolutePath() +
                                        PROFILE_DIRECTORY +
                                        profileFragment.profileSelector.getSelectedItem() +
                                        '/' + found + '/', AUDIO_THEME_DIRECTORY);
                                FileOperations.setPermissionsRecursively(THEME_644,
                                        AUDIO_THEME_DIRECTORY);
                                FileOperations.setPermissions(THEME_755, AUDIO_THEME_DIRECTORY);
                            }
                        }
                        FileOperations.setPermissionsRecursively(THEME_644, current_directory);
                        FileOperations.setPermissions(THEME_755, current_directory);
                        FileOperations.setSystemFileContext(current_directory);
                        FileOperations.mountRO();

                        // Restore wallpaper
                        new ContinueRestore(profileFragment).execute();
                    }
                    AlertDialog.Builder alertDialogBuilder =
                            new AlertDialog.Builder(profileFragment.context);
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
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
                profileFragment.headerProgress.setVisibility(View.GONE);
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            ProfileFragment profileFragment = ref.get();
            if (profileFragment != null) {
                if (Systems.checkOMS(profileFragment.context)) {  // RRO doesn't need this
                    profile_name = sUrl[0];
                    profileFragment.cannotRunOverlays = new ArrayList<>();
                    profileFragment.dialogMessage = new StringBuilder();
                    profileFragment.toBeRunCommands = "";

                    File overlays = new File(
                            Environment.getExternalStorageDirectory().getAbsolutePath()
                                    + PROFILE_DIRECTORY + profile_name + "/overlay_state.xml");

                    if (overlays.exists()) {
                        List<List<String>> profile =
                                ProfileManager.readProfileStatePackageWithTargetPackage(
                                        profile_name, STATE_ENABLED);
                        system = ThemeManager.listAllOverlays(profileFragment.context);

                        // Now process the overlays to be enabled
                        for (int i = 0, size = profile.size(); i < size; i++) {
                            String packageName = profile.get(i).get(0);
                            String targetPackage = profile.get(i).get(1);
                            if (Packages.isPackageInstalled(profileFragment.context,
                                    targetPackage)) {
                                if (system.contains(packageName)) {
                                    to_be_run.add(packageName);
                                } else {
                                    profileFragment.cannotRunOverlays.add(profile.get(i));
                                }
                            }
                        }

                        // Parse non-exist profile overlay packages
                        for (int i = 0; i < profileFragment.cannotRunOverlays.size(); i++) {
                            String packageName = profileFragment.cannotRunOverlays.get(i)
                                    .get(0);
                            String targetPackage =
                                    profileFragment.cannotRunOverlays.get(i).get(1);
                            String packageDetail = packageName.replace(targetPackage + '.',
                                    "");
                            String detailSplit = Arrays.toString(packageDetail.split("\\."))
                                    .replace("[", "")
                                    .replace("]", "")
                                    .replace(",", " ");

                            if (profileFragment.dialogMessage.length() == 0) {
                                profileFragment.dialogMessage
                                        .append("\u2022 ")
                                        .append(targetPackage)
                                        .append(" (")
                                        .append(detailSplit)
                                        .append(')');
                            } else {
                                profileFragment.dialogMessage
                                        .append('\n')
                                        .append("\u2022 ")
                                        .append(targetPackage)
                                        .append(" (")
                                        .append(detailSplit)
                                        .append(')');
                            }
                        }
                    }
                } else {
                    String profile_name = sUrl[0];
                    profileFragment.toBeRunCommands += " && mount -o rw,remount /system";
                    profileFragment.toBeRunCommands = profileFragment.toBeRunCommands +
                            " && mv -f " + BOOTANIMATION_LOCATION + " " + BOOTANIMATION_BU_LOCATION;
                    profileFragment.toBeRunCommands =
                            profileFragment.toBeRunCommands + " && cp -f " +
                                    Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    PROFILE_DIRECTORY + profile_name + "/ /system/media/";
                    profileFragment.toBeRunCommands = profileFragment.toBeRunCommands +
                            " && chmod 644 " + BOOTANIMATION_LOCATION +
                            " && mount -o ro,remount /system";
                }
            }
            return null;
        }
    }

    /**
     * Continue the restore when the profile pauses
     */
    private static class ContinueRestore extends AsyncTask<Void, String, Void> {
        private static String TAG = "ContinueRestore";
        private Handler handler = new Handler();
        private WeakReference<ProfileFragment> ref;
        private String profileName;
        private List<List<String>> toBeCompiled;
        private ArrayList<String> toBeRun;
        private ProgressDialog progressDialog;
        private LocalBroadcastManager localBroadcastManager;
        private KeyRetrieval keyRetrieval;
        private Intent securityIntent;
        private Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Waiting for encryption key handshake approval...");
                if (securityIntent != null) {
                    Log.d(TAG, "Encryption key handshake approved!");
                    handler.removeCallbacks(runnable);
                } else {
                    Log.d(TAG, "Encryption key still null...");
                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.postDelayed(this, 100L);
                }
            }
        };
        private Cipher cipher;
        private boolean needToWait;

        // Restore wallpaper
        ContinueRestore(ProfileFragment profileFragment) {
            super();
            ref = new WeakReference<>(profileFragment);
        }

        // All is well, continue enabling profile
        ContinueRestore(ProfileFragment profileFragment,
                        String profileName,
                        ArrayList<String> tobeRun) {
            super();
            ref = new WeakReference<>(profileFragment);
            this.profileName = profileName;
            toBeRun = tobeRun;
        }

        // Go here to compile some before enabling profile
        ContinueRestore(ProfileFragment profileFragment,
                        String profileName,
                        List<List<String>> toBeCompiled,
                        ArrayList<String> toBeRun) {
            super();
            ref = new WeakReference<>(profileFragment);
            this.profileName = profileName;
            this.toBeCompiled = toBeCompiled;
            this.toBeRun = toBeRun;
        }

        @Override
        protected void onPreExecute() {
            ProfileFragment profileFragment = ref.get();
            if (profileFragment != null) {
                progressDialog = new ProgressDialog(profileFragment.context);
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(false);
                progressDialog.setMessage(
                        profileFragment.getString(R.string.profile_restoration_message));
                progressDialog.show();
                if (progressDialog.getWindow() != null)
                    progressDialog.getWindow().addFlags(
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                File directory = new File(EXTERNAL_STORAGE_CACHE);
                if (!directory.exists()) {
                    FileOperations.createNewFolder(EXTERNAL_STORAGE_CACHE);
                }
                if (toBeCompiled != null) {
                    needToWait = Substratum.needToWaitInstall();
                    if (needToWait) {
                        Substratum.getInstance().registerFinishReceiver();
                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            progressDialog.setMessage(progress[0]);
        }

        @Override
        protected Void doInBackground(Void... params) {
            ProfileFragment profileFragment = ref.get();
            if (profileFragment != null) {
                profileFragment.lateInstall = new ArrayList<>();
                if (toBeCompiled != null) {
                    Map<String, ProfileItem> items =
                            ProfileManager.readProfileState(profileName, STATE_ENABLED);

                    String prevTheme = "";
                    for (int i = 0; i < toBeCompiled.size(); i++) {
                        String compilePackage = toBeCompiled.get(i).get(0);
                        ProfileItem currentItem = items.get(compilePackage);

                        @SuppressLint("StringFormatMatches")
                        // Seems like there's a bug with lint according to
                                // https://stackoverflow.com/questions/23960019/
                                // lint-gives-wrong-format-type-when-using-long-values-in-strings
                                // -xml

                                String format = String.format(
                                profileFragment.getString(R.string.profile_compile_progress),
                                i + 1,
                                toBeCompiled.size(),
                                compilePackage);
                        publishProgress(format);

                        String theme = currentItem.getParentTheme();

                        boolean encrypted = false;
                        String encrypt_check =
                                Packages.getOverlayMetadata(
                                        profileFragment.context, theme, metadataEncryption);

                        if ((encrypt_check != null) && encrypt_check.equals
                                (metadataEncryptionValue) &&
                                !theme.equals(prevTheme)) {
                            prevTheme = theme;
                            Log.d(TAG, "This overlay for " +
                                    Packages.getPackageName(profileFragment.context, theme) +
                                    " is encrypted, passing handshake to the theme package...");
                            encrypted = true;

                            Theming.getThemeKeys(profileFragment.context, theme);

                            keyRetrieval = new KeyRetrieval();
                            IntentFilter if1 = new IntentFilter(KEY_RETRIEVAL);
                            localBroadcastManager = LocalBroadcastManager.getInstance(
                                    profileFragment.context);
                            localBroadcastManager.registerReceiver(keyRetrieval, if1);

                            handler.postDelayed(runnable, 100L);
                            int counter = 0;
                            while ((securityIntent == null) && (counter < 5)) {
                                try {
                                    Thread.sleep(500L);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                counter++;
                            }
                            if (counter > 5) {
                                Log.e(TAG, "Could not receive handshake in time...");
                                return null;
                            }

                            if (securityIntent != null) {
                                try {
                                    byte[] encryption_key =
                                            securityIntent.getByteArrayExtra(ENCRYPTION_KEY_EXTRA);
                                    byte[] iv_encrypt_key =
                                            securityIntent.getByteArrayExtra(
                                                    IV_ENCRYPTION_KEY_EXTRA);

                                    cipher = Cipher.getInstance(CIPHER_ALGORITHM);
                                    cipher.init(
                                            Cipher.DECRYPT_MODE,
                                            new SecretKeySpec(encryption_key, SECRET_KEY_SPEC),
                                            new IvParameterSpec(iv_encrypt_key)
                                    );
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            }
                        }

                        Resources themeResources = null;
                        try {
                            themeResources = profileFragment.context.getPackageManager()
                                    .getResourcesForApplication(theme);
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                        assert themeResources != null;
                        AssetManager themeAssetManager = themeResources.getAssets();

                        String target = currentItem.getTargetPackage();
                        String type1a = currentItem.getType1a();
                        String type1b = currentItem.getType1b();
                        String type1c = currentItem.getType1c();
                        String type2 = currentItem.getType2();
                        String type3 = currentItem.getType3();
                        String type4 = currentItem.getType4();

                        String type1aDir = "overlays/" + target + "/type1a_" + type1a +
                                (encrypted ? ".xml" + ENCRYPTED_FILE_EXTENSION : ".xml");
                        String type1bDir = "overlays/" + target + "/type1b_" + type1b +
                                (encrypted ? ".xml" + ENCRYPTED_FILE_EXTENSION : ".xml");
                        String type1cDir = "overlays/" + target + "/type1c_" + type1c +
                                (encrypted ? ".xml" + ENCRYPTED_FILE_EXTENSION : ".xml");

                        String additional_variant = (!type2.isEmpty() ? type2 : null);
                        String base_variant = (!type3.isEmpty() ? type3 : null);

                        // Pre-notions
                        String suffix;
                        boolean useType3CommonDir = false;
                        if (type3.length() > 0) {
                            try {
                                useType3CommonDir = themeAssetManager.list(
                                        "overlays/" + target + "/type3-common").length > 0;
                            } catch (IOException e) {
                                //
                            }
                            if (useType3CommonDir) {
                                suffix = "/type3-common";
                            } else {
                                suffix = "/type3_" + type3;
                            }
                        } else {
                            suffix = "/res";
                        }
                        String workingDirectory =
                                profileFragment.context.getCacheDir().getAbsolutePath() +
                                        SUBSTRATUM_BUILDER_CACHE.substring(0,
                                                SUBSTRATUM_BUILDER_CACHE.length() - 1);
                        File created = new File(workingDirectory);
                        if (created.exists()) {
                            FileOperations.delete(
                                    profileFragment.context, created.getAbsolutePath());
                        }
                        FileOperations.createNewFolder(
                                profileFragment.context, created.getAbsolutePath());

                        // Handle the resource folder
                        String listDir = "overlays/" + target + suffix;
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                listDir,
                                workingDirectory + suffix,
                                listDir,
                                cipher);

                        if (useType3CommonDir) {
                            String type3Dir = "overlays/" + target + "/type3_" + type3;
                            FileOperations.copyFileOrDir(
                                    themeAssetManager,
                                    type3Dir,
                                    workingDirectory + suffix,
                                    type3Dir,
                                    cipher
                            );
                        }

                        // Handle the type1s
                        if (!type1a.isEmpty()) {
                            FileOperations.copyFileOrDir(
                                    themeAssetManager,
                                    type1aDir,
                                    workingDirectory + suffix + "/values/type1a.xml",
                                    type1aDir,
                                    cipher);
                        }
                        if (!type1b.isEmpty()) {
                            FileOperations.copyFileOrDir(
                                    themeAssetManager,
                                    type1bDir,
                                    workingDirectory + suffix + "/values/type1b.xml",
                                    type1bDir,
                                    cipher);
                        }
                        if (!type1c.isEmpty()) {
                            FileOperations.copyFileOrDir(
                                    themeAssetManager,
                                    type1cDir,
                                    workingDirectory + suffix + "/values/type1c.xml",
                                    type1cDir,
                                    cipher);
                        }

                        SubstratumBuilder sb = new SubstratumBuilder();
                        sb.beginAction(
                                profileFragment.context,
                                target,
                                Packages.getPackageName(profileFragment.context, theme),
                                compilePackage,
                                additional_variant,
                                base_variant,
                                Packages.getAppVersion(profileFragment.context,
                                        currentItem.getParentTheme()),
                                Systems.checkOMS(profileFragment.context),
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
                        if (sb.hasErroredOut) {
                            // TODO: Handle failed compilation
                            Log.d(TAG, "Failed to compile profile...");
                        } else {
                            if (sb.specialSnowflake || sb.noInstall.length() > 0) {
                                profileFragment.lateInstall.add(sb.noInstall);
                                toBeRun.add(compilePackage);
                            } else {
                                if (needToWait) {
                                    // Thread wait
                                    Substratum.startWaitingInstall();
                                    do {
                                        try {
                                            Thread.sleep(SPECIAL_SNOWFLAKE_DELAY);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    } while (Substratum.isWaitingInstall());
                                }
                                // Add current package to enable queue
                                toBeRun.add(compilePackage);
                            }
                        }
                    }
                }

                publishProgress(profileFragment.getString(R.string.profile_compile_processing));
                if (profileName != null && toBeRun != null) {
                    HandlerThread thread = new HandlerThread("ProfileThread", Thread.MAX_PRIORITY);
                    thread.start();
                    Handler handler = new Handler(thread.getLooper());
                    Runnable r = this::continueProcess;
                    handler.post(r);
                }
                continueProcessWallpaper();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ProfileFragment profileFragment = ref.get();
            if (profileFragment != null) {
                progressDialog.dismiss();
            }
        }

        void continueProcess() {
            ProfileFragment profileFragment = ref.get();
            if (profileFragment != null) {

                File theme = new File(Environment.getExternalStorageDirectory()
                        .getAbsolutePath() +
                        PROFILE_DIRECTORY + profileName + THEME_DIR);

                // Encrypted devices boot Animation
                File bootanimation = new File(theme, BOOTANIMATION);
                if (bootanimation.exists() &&
                        (Systems.getDeviceEncryptionStatus(profileFragment.context) > 1)) {
                    FileOperations.mountRW();
                    FileOperations.move(
                            profileFragment.context,
                            BOOTANIMATION_LOCATION,
                            BOOTANIMATION_BU_LOCATION);
                    FileOperations.copy(
                            profileFragment.context,
                            bootanimation.getAbsolutePath(),
                            BOOTANIMATION_LOCATION);
                    FileOperations.setPermissions(THEME_644, BOOTANIMATION_LOCATION);
                    FileOperations.mountRO();
                }

                // Late install
                for (String o : profileFragment.lateInstall) {
                    ThemeManager.installOverlay(profileFragment.context, o);
                    if (Substratum.needToWaitInstall()) {
                        // Wait until the overlays to fully install so on compile enable
                        // mode it can be enabled after.
                        Substratum.startWaitingInstall();
                        do {
                            try {
                                Thread.sleep(SPECIAL_SNOWFLAKE_DELAY);
                            } catch (InterruptedException e) {
                                // Still waiting
                            }
                        } while (Substratum.isWaitingInstall());
                    }
                }

                Substratum.getInstance().unregisterFinishReceiver();
                ArrayList<String> toBeDisabled =
                        new ArrayList<>(ThemeManager.listOverlays(
                                profileFragment.context, ThemeManager.STATE_ENABLED));
                boolean shouldRestartUi =
                        ThemeManager.shouldRestartUI(profileFragment.context, toBeDisabled)
                                || ThemeManager.shouldRestartUI(profileFragment.context, toBeRun);
                if (Systems.checkSubstratumService(profileFragment.context)) {
                    SubstratumService.applyProfile(
                            profileName,
                            toBeDisabled,
                            toBeRun,
                            shouldRestartUi);
                } else if (Systems.checkThemeInterfacer(profileFragment.context)) {
                    ThemeInterfacerService.applyProfile(
                            profileName,
                            toBeDisabled,
                            toBeRun,
                            shouldRestartUi);
                } else {
                    // Restore the whole backed up profile back to /data/system/theme/
                    if (theme.exists()) {
                        FileOperations.delete(profileFragment.context, THEME_DIRECTORY, false);
                        FileOperations.copyDir(profileFragment.context,
                                theme.getAbsolutePath(), THEME_DIRECTORY);
                        FileOperations.setPermissionsRecursively(THEME_644, AUDIO_THEME_DIRECTORY);
                        FileOperations.setPermissions(THEME_755, AUDIO_THEME_DIRECTORY);
                        FileOperations.setPermissions(THEME_755, ALARM_THEME_DIRECTORY);
                        FileOperations.setPermissions(THEME_755, NOTIF_THEME_DIRECTORY);
                        FileOperations.setPermissions(THEME_755, RINGTONE_THEME_DIRECTORY);
                        FileOperations.setPermissions(THEME_755, RINGTONE_THEME_DIRECTORY);
                        FileOperations.setPermissionsRecursively(THEME_644, FONTS_THEME_DIRECTORY);
                        FileOperations.setPermissions(THEME_755, FONTS_THEME_DIRECTORY);
                        FileOperations.setSystemFileContext(THEME_DIRECTORY);
                    }

                    ThemeManager.disableAllThemeOverlays(profileFragment.context);
                    ThemeManager.enableOverlay(profileFragment.context, toBeRun);
                }
            }
        }

        void continueProcessWallpaper() {
            ProfileFragment profileFragment = ref.get();
            if (profileFragment != null) {
                String homeWallPath = Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + PROFILE_DIRECTORY + profileName + WALLPAPER_FILE_NAME;
                String lockWallPath = Environment.getExternalStorageDirectory()
                        .getAbsolutePath() +
                        PROFILE_DIRECTORY + profileName + LOCK_WALLPAPER_FILE_NAME;
                File homeWall = new File(homeWallPath);
                File lockWall = new File(lockWallPath);
                if (homeWall.exists() || lockWall.exists()) {
                    try {
                        WallpapersManager.setWallpaper(
                                profileFragment.context, homeWallPath, HOME_WALLPAPER);
                        WallpapersManager.setWallpaper(
                                profileFragment.context, lockWallPath, LOCK_WALLPAPER);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        class KeyRetrieval extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                securityIntent = intent;
            }
        }
    }
}