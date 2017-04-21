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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeInterfacerService;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.systems.ProfileManager;
import projekt.substratum.common.tabs.WallpaperManager;
import projekt.substratum.util.readers.ReadOverlaysFile;

import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.VENDOR_DIR;
import static projekt.substratum.common.systems.ProfileManager.DAY_PROFILE;
import static projekt.substratum.common.systems.ProfileManager.DAY_PROFILE_HOUR;
import static projekt.substratum.common.systems.ProfileManager.DAY_PROFILE_MINUTE;
import static projekt.substratum.common.systems.ProfileManager.NIGHT_PROFILE;
import static projekt.substratum.common.systems.ProfileManager.NIGHT_PROFILE_HOUR;
import static projekt.substratum.common.systems.ProfileManager.NIGHT_PROFILE_MINUTE;
import static projekt.substratum.common.systems.ProfileManager.SCHEDULED_PROFILE_ENABLED;

public class ProfileFragment extends Fragment {

    public static int nightHour, nightMinute, dayHour, dayMinute;

    private List<String> list;
    private ProgressBar headerProgress;
    private Spinner profile_selector, dayProfile, nightProfile;
    private EditText backup_name;
    private String backup_getText;
    private String to_be_run_commands;
    private ArrayAdapter<String> adapter;
    private List<List<String>> cannot_run_overlays;
    private String dialog_message;
    private boolean dayNightEnabled;
    private ArrayList<CharSequence> selectedBackup;

    public static void setNightProfileStart(int hour, int minute) {
        nightHour = hour;
        nightMinute = minute;
    }

    public static void setDayProfileStart(int hour, int minute) {
        dayHour = hour;
        dayMinute = minute;
    }

    public void RefreshSpinner() {
        list.clear();
        list.add(getResources().getString(R.string.spinner_default_item));

        // Now lets add all the located profiles
        File f = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/");
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
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.profile_fragment, container, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        headerProgress = (ProgressBar) root.findViewById(R.id.header_loading_bar);
        headerProgress.setVisibility(View.GONE);

        // Create a user viewable directory for profiles
        File directory = new File(
                Environment.getExternalStorageDirectory(), "/substratum/");
        if (!directory.exists() && !directory.mkdirs()) {
            Log.e(References.SUBSTRATUM_LOG, "Could not create Substratum directory...");
        }
        File directory2 = new File(
                Environment.getExternalStorageDirectory(), "/substratum/profiles");
        if (!directory2.exists() && directory2.mkdirs()) {
            Log.e(References.SUBSTRATUM_LOG, "Could not create profile directory...");
        }

        // Handle Backups
        backup_name = (EditText) root.findViewById(R.id.edittext);

        // Restrict whitespace for profile name
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (Character.isWhitespace(source.charAt(i))) {
                    Toast.makeText(getContext(),
                            R.string.profile_edittext_whitespace_warning_toast,
                            Toast.LENGTH_LONG)
                            .show();
                    return "";
                }
            }
            return null;
        };
        backup_name.setFilters(new InputFilter[]{filter});
        backup_name.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager imm = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(backup_name.getWindowToken(),
                        InputMethodManager.RESULT_UNCHANGED_SHOWN);
            }
        });

        final Button backupButton = (Button) root.findViewById(R.id.backupButton);
        backupButton.setOnClickListener(v -> {
            if (backup_name.getText().length() > 0) {
                selectedBackup = new ArrayList<>();
                boolean fonts_allowed = false;
                try {
                    Class<?> cls = Class.forName("android.graphics.Typeface");
                    cls.getDeclaredMethod("getSystemFontDirLocation");
                    cls.getDeclaredMethod("getThemeFontConfigLocation");
                    cls.getDeclaredMethod("getThemeFontDirLocation");
                    fonts_allowed = true;
                } catch (Exception ex) {
                    // Suppress Fonts
                }
                CharSequence[] items;
                if (References.checkOMS(getContext()) || fonts_allowed) {
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

                AlertDialog dialog = new AlertDialog.Builder(getContext())
                        .setTitle(R.string.profile_dialog_title)
                        .setMultiChoiceItems(items, null, (dialog1, which, isChecked) -> {
                            if (isChecked) {
                                if (items[which].equals(getString(R.string.profile_boot_animation))
                                        && References.getDeviceEncryptionStatus(getContext()) > 1
                                        && References.checkThemeInterfacer(getContext())) {
                                    AlertDialog dialog2 = new AlertDialog.Builder(getContext())
                                            .setTitle(R.string.root_required_title)
                                            .setMessage(R.string
                                                    .root_required_boot_animation_profile)
                                            .setPositiveButton(android.R.string.ok, null)
                                            .create();
                                    dialog2.show();
                                }
                                selectedBackup.add(items[which]);
                            } else if (selectedBackup.contains(items[which])) {
                                selectedBackup.remove(items[which]);
                            }
                        })
                        .setPositiveButton(R.string.profile_dialog_ok, null)
                        .setNegativeButton(R.string.profile_dialog_cancel, null)
                        .create();

                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                    if (!selectedBackup.isEmpty()) {
                        backup_getText = backup_name.getText().toString();
                        BackupFunction backupFunction = new BackupFunction();
                        backupFunction.execute();
                        Log.d(References.SUBSTRATUM_LOG, selectedBackup.toString());
                        dialog.dismiss();
                    } else {
                        Toast.makeText(getContext(), R.string.profile_no_selection_warning,
                                Toast.LENGTH_LONG).show();
                    }
                });

                InputMethodManager imm = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(backupButton.getWindowToken(),
                        InputMethodManager.RESULT_UNCHANGED_SHOWN);
            } else {
                if (getView() != null) {
                    Lunchbar.make(getView(),
                            getString(R.string.profile_edittext_empty_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            }
        });

        // Handle Restores

        list = new ArrayList<>();
        list.add(getResources().getString(R.string.spinner_default_item));

        profile_selector = (Spinner) root.findViewById(R.id.restore_spinner);

        // Now lets add all the located profiles
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/substratum/profiles/");
        File[] files = f.listFiles();
        if (files != null) {
            for (File inFile : files) {
                if (inFile.isDirectory()) {
                    list.add(inFile.getName());
                }
            }
        }

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, list);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profile_selector.setAdapter(adapter);

        ImageButton imageButton = (ImageButton) root.findViewById(R.id.remove_profile);
        imageButton.setOnClickListener(v -> {
            if (profile_selector.getSelectedItemPosition() > 0) {
                String formatted = String.format(getString(R.string.delete_dialog_text),
                        profile_selector.getSelectedItem());
                new AlertDialog.Builder(getContext())
                        .setTitle(getString(R.string.delete_dialog_title))
                        .setMessage(formatted)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.delete_dialog_okay),
                                (dialog, which) -> {
                                    File f1 = new File(Environment
                                            .getExternalStorageDirectory().getAbsolutePath()
                                            + "/substratum/profiles/" + profile_selector
                                            .getSelectedItem() + "" +
                                            ".substratum");
                                    boolean deleted = f1.delete();
                                    if (!deleted)
                                        Log.e(References.SUBSTRATUM_LOG,
                                                "Could not delete profile directory.");
                                    FileOperations.delete(getContext(),
                                            Environment.getExternalStorageDirectory()
                                                    .getAbsolutePath() +
                                                    "/substratum/profiles/" +
                                                    profile_selector.getSelectedItem());
                                    RefreshSpinner();
                                })
                        .setNegativeButton(getString(R.string.delete_dialog_cancel), (dialog,
                                                                                      which) ->
                                dialog.cancel())
                        .create()
                        .show();
            } else {
                if (getView() != null) {
                    Lunchbar.make(getView(),
                            getString(R.string.profile_delete_button_none_selected_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            }
        });

        final Button restoreButton = (Button) root.findViewById(R.id.restoreButton);
        restoreButton.setOnClickListener(v -> {
            if (profile_selector.getSelectedItemPosition() > 0) {
                RestoreFunction restoreFunction = new RestoreFunction();
                restoreFunction.execute(profile_selector.getSelectedItem().toString());
            } else {
                if (getView() != null) {
                    Lunchbar.make(getView(),
                            getString(R.string.restore_button_none_selected_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            }
        });

        final CardView scheduledProfileCard = (CardView) root.findViewById(R.id.cardListView3);
        if (References.checkOMS(getContext()) && References.checkThemeInterfacer(getContext
                ())) {
            final ExpandableLayout scheduledProfileLayout = (ExpandableLayout) root.findViewById(
                    R.id.scheduled_profile_card_content_container);
            final Switch dayNightSwitch = (Switch) root.findViewById(R.id.profile_switch);
            dayNightSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) {
                    scheduledProfileLayout.expand();
                } else {
                    scheduledProfileLayout.collapse();
                }
                dayNightEnabled = b;
            });

            final FragmentManager fm = getActivity().getSupportFragmentManager();
            final Button startTime = (Button) root.findViewById(R.id.night_start_time);
            startTime.setOnClickListener(view -> {
                DialogFragment timePickerFragment = new TimePickerFragment();
                if (startTime.getText().equals(getResources().getString(R.string.start_time))) {
                    TimePickerFragment.setFlag(TimePickerFragment.FLAG_START_TIME);
                } else {
                    TimePickerFragment.setFlag(
                            TimePickerFragment.FLAG_START_TIME | TimePickerFragment.FLAG_GET_VALUE);
                }
                timePickerFragment.show(fm, "TimePicker");
            });

            final Button endTime = (Button) root.findViewById(R.id.night_end_time);
            endTime.setOnClickListener(view -> {
                DialogFragment timePickerFragment = new TimePickerFragment();
                if (endTime.getText().equals(getResources().getString(R.string.end_time))) {
                    TimePickerFragment.setFlag(TimePickerFragment.FLAG_END_TIME);
                } else {
                    TimePickerFragment.setFlag(
                            TimePickerFragment.FLAG_END_TIME | TimePickerFragment.FLAG_GET_VALUE);
                }
                timePickerFragment.show(fm, "TimePicker");
            });

            dayProfile = (Spinner) root.findViewById(R.id.day_spinner);
            dayProfile.setAdapter(adapter);
            nightProfile = (Spinner) root.findViewById(R.id.night_spinner);
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

            final Button applyScheduledProfileButton = (Button) root.findViewById(
                    R.id.apply_schedule_button);
            applyScheduledProfileButton.setOnClickListener(view -> {
                if (dayNightEnabled) {
                    if (dayProfile.getSelectedItemPosition() > 0 &&
                            nightProfile.getSelectedItemPosition() > 0) {
                        if (!startTime.getText().equals(getResources()
                                .getString(R.string.start_time)) && !endTime.getText()
                                .equals(getResources().getString(R.string.end_time))) {
                            if (dayHour != nightHour || dayMinute != nightMinute) {
                                ProfileManager.enableScheduledProfile(getActivity(),
                                        dayProfile.getSelectedItem().toString(), dayHour, dayMinute,
                                        nightProfile.getSelectedItem().toString(), nightHour,
                                        nightMinute);
                                if (getView() != null) {
                                    Lunchbar.make(getView(),
                                            R.string.scheduled_profile_apply_success,
                                            Lunchbar.LENGTH_LONG)
                                            .show();
                                }
                            } else {
                                if (getView() != null) {
                                    Lunchbar.make(getView(), R.string.time_equal_warning,
                                            Lunchbar.LENGTH_LONG)
                                            .show();
                                }
                            }
                        } else {
                            if (getView() != null) {
                                Lunchbar.make(getView(), R.string.time_empty_warning,
                                        Lunchbar.LENGTH_LONG)
                                        .show();
                            }
                        }
                    } else {
                        if (getView() != null) {
                            Lunchbar.make(getView(), R.string.profile_empty_warning,
                                    Lunchbar.LENGTH_LONG)
                                    .show();
                        }
                    }
                } else {
                    ProfileManager.disableScheduledProfile(getActivity());
                    if (getView() != null) {
                        Lunchbar.make(getView(),
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

    // TODO: move this to ProfileManager
    private class BackupFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            headerProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            headerProgress.setVisibility(View.GONE);
            if (References.checkOMS(getContext())) {
                String directory_parse = String.format(getString(R.string.toast_backup_success),
                        backup_getText + ".substratum");
                if (getView() != null) {
                    Lunchbar.make(getView(),
                            directory_parse,
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            } else {
                String directory_parse = String.format(getString(R.string.toast_backup_success),
                        backup_getText + "/");
                if (getView() != null) {
                    Lunchbar.make(getView(),
                            directory_parse,
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            }
            RefreshSpinner();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            String uid = Environment.getExternalStorageDirectory().getAbsolutePath().split("/")[3];

            File nomediaFile = new File(Environment.getExternalStorageDirectory() +
                    "/substratum/.nomedia");
            try {
                if (!nomediaFile.createNewFile()) {
                    Log.d(References.SUBSTRATUM_LOG, "Could not create .nomedia file or" +
                            " file already exist!");
                }
            } catch (IOException e) {
                Log.d(References.SUBSTRATUM_LOG, "Could not create .nomedia file!");
                e.printStackTrace();
            }

            if (References.checkOMS(getContext())) {
                File profileDir = new File(Environment
                        .getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/" + backup_getText + "/");
                if (profileDir.exists()) {
                    FileOperations.delete(getContext(),
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/substratum/profiles/" + backup_getText);
                    if (!profileDir.mkdir())
                        Log.e(References.SUBSTRATUM_LOG, "Could not create profile directory.");
                } else {
                    if (!profileDir.mkdir())
                        Log.e(References.SUBSTRATUM_LOG, "Could not create profile directory.");
                }

                File profileFile = new File(Environment.getExternalStorageDirectory()
                        .getAbsolutePath() +
                        "/substratum/profiles/" + backup_getText + "/" + "overlays.xml");
                if (profileFile.exists()) {
                    FileOperations.delete(getContext(), profileFile.getAbsolutePath());
                }

                if (selectedBackup.contains(getString(R.string.profile_overlay))) {
                    FileOperations.copy(getContext(), "/data/system/overlays.xml",
                            profileFile.getAbsolutePath());
                }

                // Backup the entire /data/system/theme/ folder
                FileOperations.copyDir(getContext(), "/data/system/theme",
                        Environment.getExternalStorageDirectory().getAbsolutePath() +
                                "/substratum/profiles/" + backup_getText + "/theme");

                // Delete the items user don't want to backup
                if (!selectedBackup.contains(getString(R.string.profile_boot_animation))) {
                    File bootanimation = new File(profileDir, "theme/bootanimation.zip");
                    if (bootanimation.exists()) {
                        FileOperations.delete(getContext(), bootanimation.getAbsolutePath());
                    }
                }
                if (!selectedBackup.contains(getString(R.string.profile_font))) {
                    File fonts = new File(profileDir, "theme/fonts");
                    if (fonts.exists()) {
                        FileOperations.delete(getContext(), fonts.getAbsolutePath());
                    }
                }
                if (!selectedBackup.contains(getString(R.string.profile_sound))) {
                    File sounds = new File(profileDir, "theme/audio");
                    if (sounds.exists()) {
                        FileOperations.delete(getContext(), sounds.getAbsolutePath());
                    }
                }

                // Backup wallpapers if wanted
                if (selectedBackup.contains(getString(R.string.profile_wallpaper))) {
                    FileOperations.copy(getContext(), "/data/system/users/" + uid + "/wallpaper",
                            Environment.getExternalStorageDirectory().getAbsolutePath()
                                    + "/substratum/profiles/" + backup_getText + "/wallpaper.png");
                    FileOperations.copy(getContext(), "/data/system/users/" + uid +
                                    "/wallpaper_lock",
                            Environment.getExternalStorageDirectory().getAbsolutePath()
                                    + "/substratum/profiles/" + backup_getText +
                                    "/wallpaper_lock.png");
                }

                // Backup system bootanimation if encrypted
                if (References.getDeviceEncryptionStatus(getContext()) > 1 &&
                        selectedBackup.contains(getString(R.string.profile_boot_animation))) {
                    FileOperations.copy(getContext(), "/system/media/bootanimation.zip",
                            Environment.getExternalStorageDirectory().getAbsolutePath()
                                    + "/substratum/profiles/" + backup_getText +
                                    "/bootanimation.zip");
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
                if (References.inNexusFilter()) {
                    current_directory = "/system/overlay/";
                } else {
                    current_directory = "/system/vendor/overlay/";
                }
                File file = new File(current_directory);
                if (file.exists()) {
                    FileOperations.mountRW();
                    if (selectedBackup.contains(getString(R.string.profile_overlay))) {
                        FileOperations.copyDir(getContext(), current_directory,
                                Environment.getExternalStorageDirectory().getAbsolutePath() +
                                        "/substratum/profiles/");
                        File oldFolder = new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/substratum/profiles/overlay");
                        File newFolder = new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/substratum/profiles/" + backup_getText);
                        boolean success = oldFolder.renameTo(newFolder);
                        if (!success)
                            Log.e(References.SUBSTRATUM_LOG, "Could not move profile directory...");
                    }

                    if (selectedBackup.contains(getString(R.string.profile_sound))) {
                        // Now begin backing up sounds
                        FileOperations.copyDir(getContext(), "/data/system/theme/audio/",
                                Environment.getExternalStorageDirectory()
                                        .getAbsolutePath() + "/substratum/profiles/"
                                        + backup_getText);
                    }
                    FileOperations.mountRO();

                    // Don't forget the wallpaper if wanted
                    if (selectedBackup.contains(getString(R.string.profile_wallpaper))) {
                        File homeWall = new File("/data/system/users/" + uid + "/wallpaper");
                        File lockWall = new File("/data/system/users/" + uid + "/wallpaper_lock");
                        if (homeWall.exists()) {
                            FileOperations.copy(getContext(), homeWall.getAbsolutePath(),
                                    Environment.getExternalStorageDirectory().getAbsolutePath()
                                            + "/substratum/profiles/" + backup_getText +
                                            "/wallpaper.png");
                        }
                        if (lockWall.exists()) {
                            FileOperations.copy(getContext(), lockWall.getAbsolutePath(),
                                    Environment.getExternalStorageDirectory().getAbsolutePath()
                                            + "/substratum/profiles/" + backup_getText +
                                            "/wallpaper_lock.png");
                        }
                    }

                    // And bootanimation if wanted
                    if (selectedBackup.contains(getString(R.string.profile_boot_animation))) {
                        FileOperations.copy(getContext(), "/system/media/bootanimation.zip",
                                Environment.getExternalStorageDirectory().getAbsolutePath()
                                        + "/substratum/profiles/" + backup_getText);
                    }
                } else {
                    if (getView() != null) {
                        Lunchbar.make(getView(),
                                getString(R.string.backup_no_overlays),
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                }
            }
            return null;
        }
    }

    // TODO: this one too
    private class RestoreFunction extends AsyncTask<String, Integer, String> {
        List<String> system = new ArrayList<>(); // All installed overlays
        ArrayList<String> to_be_run = new ArrayList<>(); // Overlays going to be enabled
        String profile_name;

        @Override
        protected void onPreExecute() {
            headerProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String result) {
            if (References.checkOMS(getContext())) {
                if (cannot_run_overlays.size() > 0) {
                    new AlertDialog.Builder(getContext())
                            .setTitle(getString(R.string.restore_dialog_title))
                            .setMessage(dialog_message)
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.restore_dialog_okay), (dialog,
                                                                                         which) -> {
                                dialog.dismiss();
                                continueProcess();
                            })
                            .setNegativeButton(getString(R.string.restore_dialog_cancel),
                                    (dialog, which) -> headerProgress.setVisibility(View.GONE))
                            .create().show();
                } else {
                    continueProcess();
                    headerProgress.setVisibility(View.GONE);
                }
            } else {
                String current_directory;
                if (References.inNexusFilter()) {
                    current_directory = PIXEL_NEXUS_DIR;
                } else {
                    current_directory = LEGACY_NEXUS_DIR;
                }
                File file = new File(current_directory);
                if (file.exists()) {
                    // Delete destination overlays
                    FileOperations.mountRW();
                    FileOperations.delete(getContext(), current_directory);
                    FileOperations.delete(getContext(), "/data/system/theme/");
                    FileOperations.createNewFolder(getContext(), current_directory);
                    FileOperations.createNewFolder(getContext(), "/data/system/theme/");
                    FileOperations.setPermissions(755, "/data/system/theme/");

                    File profile_apk_files = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/substratum/profiles/" +
                            profile_selector.getSelectedItem() + "/");
                    String[] located_files = profile_apk_files.list();
                    for (String found : located_files) {
                        if (!found.equals("audio")) {
                            FileOperations.copyDir(getContext(), Environment
                                    .getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/substratum/profiles/" + profile_selector.getSelectedItem() +
                                    "/" + found, current_directory);
                        } else {
                            FileOperations.copyDir(getContext(), Environment
                                    .getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/substratum/profiles/" + profile_selector.getSelectedItem() +
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
                    String vendor_location = LEGACY_NEXUS_DIR;
                    String vendor_partition = VENDOR_DIR;
                    String vendor_symlink = PIXEL_NEXUS_DIR;
                    String current_vendor =
                            ((References.inNexusFilter()) ? vendor_partition :
                                    vendor_location);
                    FileOperations.mountRW();
                    File vendor = new File(current_vendor);
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

                    FileOperations.delete(getContext(), "/data/system/theme/");
                    FileOperations.createNewFolder(getContext(), "/data/system/theme/");

                    File profile_apk_files = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/substratum/profiles/" +
                            profile_selector.getSelectedItem() + "/");
                    String[] located_files = profile_apk_files.list();
                    for (String found : located_files) {
                        if (!found.equals("audio")) {
                            FileOperations.copyDir(getContext(), Environment
                                    .getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/substratum/profiles/" + profile_selector.getSelectedItem() +
                                    "/" + found, current_directory);
                        } else {
                            FileOperations.setPermissions(755, "/data/system/theme/");
                            FileOperations.copyDir(getContext(), Environment
                                    .getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/substratum/profiles/" + profile_selector.getSelectedItem() +
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
                }
                continueProcessWallpaper();
                AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(getContext());
                alertDialogBuilder
                        .setTitle(getString(R.string.legacy_dialog_soft_reboot_title));
                alertDialogBuilder
                        .setMessage(getString(R.string.legacy_dialog_soft_reboot_text));
                alertDialogBuilder
                        .setPositiveButton(
                                android.R.string.ok, (dialog, id) -> ElevatedCommands.reboot());
                alertDialogBuilder.setCancelable(false);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                headerProgress.setVisibility(View.GONE);
            }
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {

            if (References.checkOMS(getContext())) {  // RRO doesn't need any of this
                profile_name = sUrl[0];
                cannot_run_overlays = new ArrayList<>();
                dialog_message = "";
                to_be_run_commands = "";

                File overlays = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/substratum/profiles/" + profile_name + "/overlays.xml");

                if (overlays.exists()) {
                    String[] commandsSystem4 = {"/data/system/overlays.xml", "4"};
                    String[] commandsSystem5 = {"/data/system/overlays.xml", "5"};
                    String[] commands = {overlays.getAbsolutePath(), "5"};

                    List<List<String>> profile = ReadOverlaysFile.withTargetPackage(
                            getContext(), commands);
                    system = ReadOverlaysFile.main(getContext(), commandsSystem4);
                    system.addAll(ReadOverlaysFile.main(getContext(), commandsSystem5));

                    // Now process the overlays to be enabled
                    for (int i = 0, size = profile.size(); i < size; i++) {
                        String packageName = profile.get(i).get(0);
                        String targetPackage = profile.get(i).get(1);
                        if (References.isPackageInstalled(getContext(), targetPackage)) {
                            if (!packageName.endsWith(".icon")) {
                                if (system.contains(packageName)) {
                                    to_be_run.add(packageName);
                                } else {
                                    cannot_run_overlays.add(profile.get(i));
                                }
                            }
                        }
                    }

                    // Parse non-exist profile overlay packages
                    for (int i = 0; i < cannot_run_overlays.size(); i++) {
                        String packageName = cannot_run_overlays.get(i).get(0);
                        String targetPackage = cannot_run_overlays.get(i).get(1);
                        String packageDetail = packageName.replace(targetPackage + ".", "");
                        String detailSplit = Arrays.toString(packageDetail.split("\\."))
                                .replace("[", "")
                                .replace("]", "")
                                .replace(",", " ");

                        if (dialog_message.length() == 0) {
                            dialog_message = dialog_message + "\u2022 " + targetPackage + " (" +
                                    detailSplit + ")";
                        } else {
                            dialog_message = dialog_message + "\n" + "\u2022 " + targetPackage
                                    + " (" + detailSplit + ")";
                        }
                    }
                }
            } else {
                String profile_name = sUrl[0];
                to_be_run_commands = to_be_run_commands +
                        " && mount -o rw,remount /system";
                to_be_run_commands = to_be_run_commands +
                        " && mv -f /system/media/bootanimation.zip" +
                        " /system/media/bootanimation-backup.zip";
                to_be_run_commands = to_be_run_commands + " && cp -f " +
                        Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/" + profile_name + "/ /system/media/";
                to_be_run_commands = to_be_run_commands +
                        " && chmod 644 /system/media/bootanimation.zip" +
                        " && mount -o ro,remount /system";
            }
            return null;
        }

        void continueProcess() {
            File theme = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/substratum/profiles/" + profile_name + "/theme");

            // Encrypted devices boot Animation
            File bootanimation = new File(theme, "bootanimation.zip");
            if (bootanimation.exists() &&
                    References.getDeviceEncryptionStatus(getContext()) > 1) {
                FileOperations.mountRW();
                FileOperations.move(getContext(), "/system/media/bootanimation.zip",
                        "/system/madia/bootanimation-backup.zip");
                FileOperations.copy(getContext(), bootanimation.getAbsolutePath(),
                        "/system/media/bootanimation.zip");
                FileOperations.setPermissions(644, "/system/media/bootanimation.zip");
                FileOperations.mountRO();
            }

            if (References.checkThemeInterfacer(getContext())) {
                ArrayList<String> toBeDisabled = new ArrayList<>(system);
                boolean shouldRestartUi = ThemeManager.shouldRestartUI(getContext(), toBeDisabled)
                        || ThemeManager.shouldRestartUI(getContext(), to_be_run);
                ThemeInterfacerService.applyProfile(getContext(), profile_name, toBeDisabled,
                        to_be_run,
                        shouldRestartUi);
            } else {
                // Restore the whole backed up profile back to /data/system/theme/
                if (theme.exists()) {
                    FileOperations.delete(getContext(), "/data/system/theme", false);
                    FileOperations.copyDir(getContext(), theme.getAbsolutePath(),
                            "/data/system/theme");
                    FileOperations.setPermissionsRecursively(644, "/data/system/theme/audio");
                    FileOperations.setPermissions(755, "/data/system/theme/audio");
                    FileOperations.setPermissions(755, "/data/system/theme/audio/alarms");
                    FileOperations.setPermissions(755, "/data/system/theme/audio/notifications");
                    FileOperations.setPermissions(755, "/data/system/theme/audio/ringtones");
                    FileOperations.setPermissions(755, "/data/system/theme/audio/ringtones");
                    FileOperations.setPermissionsRecursively(644, "/data/system/theme/fonts/");
                    FileOperations.setPermissions(755, "/data/system/theme/fonts/");
                    FileOperations.setContext("/data/system/theme");

                    ThemeManager.disableAll(getContext());
                    ThemeManager.enableOverlay(getContext(), to_be_run);
                    ThemeManager.restartSystemUI(getContext());
                }
            }

            // Restore wallpapers
            continueProcessWallpaper();
        }

        void continueProcessWallpaper() {
            String homeWallPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/substratum/profiles/" + profile_name + "/wallpaper.png";
            String lockWallPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/substratum/profiles/" + profile_name + "/wallpaper_lock.png";
            File homeWall = new File(homeWallPath);
            File lockWall = new File(lockWallPath);
            if (homeWall.exists() || lockWall.exists()) {
                try {
                    WallpaperManager.setWallpaper(getContext(), homeWallPath, "home");
                    WallpaperManager.setWallpaper(getContext(), lockWallPath, "lock");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}