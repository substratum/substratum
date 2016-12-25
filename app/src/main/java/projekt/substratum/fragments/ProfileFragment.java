package projekt.substratum.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;

import com.alimuzaffar.lib.widgets.AnimatedEditText;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.config.References;
import projekt.substratum.services.ScheduledProfileReceiver;
import projekt.substratum.util.ReadOverlaysFile;
import projekt.substratum.util.ViewAnimationUtils;

public class ProfileFragment extends Fragment {

    private static final String SCHEDULED_PROFILE_ENABLED = "scheduled_profile_enabled";
    private static final String NIGHT_PROFILE = "night_profile";
    private static final String NIGHT_PROFILE_HOUR = "night_profile_hour";
    private static final String NIGHT_PROFILE_MINUTE = "night_profile_minute";
    private static final String DAY_PROFILE = "day_profile";
    private static final String DAY_PROFILE_HOUR = "day_profile_hour";
    private static final String DAY_PROFILE_MINUTE = "day_profile_minute";
    private static int nightHour, nightMinute, dayHour, dayMinute;

    private List<String> list;
    private ProgressBar headerProgress;
    private Spinner profile_selector, dayProfile, nightProfile;
    private AnimatedEditText aet_backup;
    private String aet_getText;
    private String to_be_run_commands;
    private ArrayAdapter<String> adapter;
    private List<String> cannot_run_overlays;
    private String dialog_message;
    private boolean helper_exists = true;
    private SharedPreferences prefs;
    private boolean dayNightEnabled;

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
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/substratum/profiles/");
        File[] files = f.listFiles();
        if (files != null) {
            for (File inFile : files) {
                if (References.checkOMS(getContext())) {
                    if (!inFile.isDirectory()) {
                        if (inFile.getName().split("\\.")[inFile.getName().split("\\.").length - 1]
                                .equals("substratum")) {
                            list.add(inFile.getName().substring(0, inFile.getName().length() - 11));
                        }
                    }
                } else {
                    if (inFile.isDirectory()) {
                        // Because overlays.xml does not exist in RRO, there shouldn't need these
                        // files. Remove the .substratum filter.
                        list.add(inFile.getName());
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.profile_fragment, container, false);

        prefs = PreferenceManager.getDefaultSharedPreferences(
                getContext());

        headerProgress = (ProgressBar) root.findViewById(R.id.header_loading_bar);
        headerProgress.setVisibility(View.GONE);

        // Create a user viewable directory for profiles
        File directory = new File(Environment.getExternalStorageDirectory(),
                "/substratum/");
        if (!directory.exists()) {
            Boolean made = directory.mkdirs();
            if (!made) Log.e(References.SUBSTRATUM_LOG, "Could not create Substratum directory...");
        }
        File directory2 = new File(Environment.getExternalStorageDirectory(),
                "/substratum/profiles");
        if (!directory2.exists()) {
            Boolean made = directory2.mkdirs();
            if (!made) Log.e(References.SUBSTRATUM_LOG, "Could not create profile directory...");
        }

        // Handle Backups
        aet_backup = (AnimatedEditText) root.findViewById(R.id.edittext);

        final Button backupButton = (Button) root.findViewById(R.id.backupButton);
        backupButton.setOnClickListener(v -> {
            if (aet_backup.getText().length() > 0) {
                aet_getText = aet_backup.getText().toString();
                BackupFunction backupFunction = new BackupFunction();
                backupFunction.execute();
            } else {
                Snackbar.make(getView(),
                        getString(R.string.profile_edittext_empty_toast),
                        Snackbar.LENGTH_LONG)
                        .show();
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
                if (References.checkOMS(getContext())) {
                    if (!inFile.isDirectory()) {
                        if (inFile.getName().split("\\.")[inFile.getName().split("\\.").length - 1]
                                .equals("substratum")) {
                            list.add(inFile.getName().substring(0, inFile.getName().length() - 11));
                        }
                    }
                } else {
                    if (inFile.isDirectory()) {
                        // Because overlays.xml does not exist in RRO, there shouldn't need these
                        // files. Remove the .substratum filter.
                        list.add(inFile.getName());
                    }
                }
            }
        }

        adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, list);
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
                                        Log.e(References.SUBSTRATUM_LOG, "Could not " +
                                                "delete " +
                                                "profile directory.");
                                    References.delete(
                                            Environment.getExternalStorageDirectory()
                                                    .getAbsolutePath() +
                                                    "/substratum/profiles/" +
                                                    profile_selector
                                                            .getSelectedItem());
                                    RefreshSpinner();
                                })
                        .setNegativeButton(getString(R.string.delete_dialog_cancel), (dialog, which) -> dialog.cancel())
                        .create().show();
            } else {
                Snackbar.make(getView(),
                        getString(R.string.profile_delete_button_none_selected_toast),
                        Snackbar.LENGTH_LONG)
                        .show();
            }
        });

        final Button restoreButton = (Button) root.findViewById(R.id.restoreButton);
        restoreButton.setOnClickListener(v -> {
            if (profile_selector.getSelectedItemPosition() > 0) {
                RestoreFunction restoreFunction = new RestoreFunction();
                restoreFunction.execute(profile_selector.getSelectedItem().toString());
            } else {
                Snackbar.make(getView(),
                        getString(R.string.restore_button_none_selected_toast),
                        Snackbar.LENGTH_LONG)
                        .show();
            }
        });

        final CardView scheduledProfileCard = (CardView) root.findViewById(R.id.cardListView3);
        if (References.checkOMS(getActivity())) {
            final LinearLayout scheduledProfileLayout = (LinearLayout) root.findViewById(
                    R.id.scheduled_profile_card_content);
            final Switch dayNightSwitch = (Switch) root.findViewById(R.id.profile_switch);
            dayNightSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) {
                    ViewAnimationUtils.expand(scheduledProfileLayout);
                } else {
                    ViewAnimationUtils.collapse(scheduledProfileLayout);
                }
                dayNightEnabled = b;
            });

            final FragmentManager fm = getActivity().getSupportFragmentManager();
            final Button startTime = (Button) root.findViewById(R.id.night_start_time);
            startTime.setOnClickListener(view -> {
                DialogFragment timePickerFragment = new TimePickerFragment();
                TimePickerFragment.setFlag(TimePickerFragment.FLAG_START_TIME);
                timePickerFragment.show(fm, "TimePicker");
            });

            final Button endTime = (Button) root.findViewById(R.id.night_end_time);
            endTime.setOnClickListener(view -> {
                DialogFragment timePickerFragment = new TimePickerFragment();
                TimePickerFragment.setFlag(TimePickerFragment.FLAG_END_TIME);
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
                            setupScheduledProfile();
                        } else {
                            Snackbar.make(getView(), R.string.time_empty_warning,
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                    } else {
                        Snackbar.make(getView(), R.string.profile_empty_warning,
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                } else {
                    setupScheduledProfile();
                }
            });
        } else {
            scheduledProfileCard.setVisibility(View.GONE);
        }
        return root;
    }

    private int getDeviceEncryptionStatus() {
        // 0: ENCRYPTION_STATUS_UNSUPPORTED
        // 1: ENCRYPTION_STATUS_INACTIVE
        // 2: ENCRYPTION_STATUS_ACTIVATING
        // 3: ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY
        // 4: ENCRYPTION_STATUS_ACTIVE
        // 5: ENCRYPTION_STATUS_ACTIVE_PER_USER
        int status = DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED;
        final DevicePolicyManager dpm = (DevicePolicyManager)
                getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) {
            status = dpm.getStorageEncryptionStatus();
        }
        return status;
    }

    private void setupScheduledProfile() {
        SharedPreferences.Editor editor = prefs.edit();
        AlarmManager alarmMgr = (AlarmManager) getActivity()
                .getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getActivity(), ScheduledProfileReceiver.class);
        intent.putExtra("type", getString(R.string.night));
        PendingIntent nightIntent = PendingIntent.getBroadcast(getActivity(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra("type", getString(R.string.day));
        PendingIntent dayIntent = PendingIntent.getBroadcast(getActivity(), 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        if (dayNightEnabled) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());

            editor.putBoolean(SCHEDULED_PROFILE_ENABLED, dayNightEnabled);

            // night time
            calendar.set(Calendar.HOUR_OF_DAY, nightHour);
            calendar.set(Calendar.MINUTE, nightMinute);
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, nightIntent);
            editor.putString(NIGHT_PROFILE, nightProfile.getSelectedItem().toString());
            editor.putInt(NIGHT_PROFILE_HOUR, nightHour);
            editor.putInt(NIGHT_PROFILE_MINUTE, nightMinute);

            // day time
            calendar.set(Calendar.HOUR_OF_DAY, dayHour);
            calendar.set(Calendar.MINUTE, dayMinute);
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, dayIntent);
            editor.putString(DAY_PROFILE, dayProfile.getSelectedItem().toString());
            editor.putInt(DAY_PROFILE_HOUR, dayHour);
            editor.putInt(DAY_PROFILE_MINUTE, dayMinute);
            editor.apply();

            Snackbar.make(getView(), "Scheduled profile successfully applied!",
                    Snackbar.LENGTH_LONG)
                    .show();
        } else {
            if (alarmMgr != null) {
                alarmMgr.cancel(nightIntent);
                alarmMgr.cancel(dayIntent);

                editor
                        .remove(SCHEDULED_PROFILE_ENABLED)
                        .remove(DAY_PROFILE)
                        .remove(DAY_PROFILE_HOUR)
                        .remove(DAY_PROFILE_MINUTE)
                        .remove(NIGHT_PROFILE)
                        .remove(NIGHT_PROFILE_HOUR)
                        .remove(NIGHT_PROFILE_MINUTE)
                        .apply();
                Snackbar.make(getView(), "Scheduled profile successfully disabled!",
                        Snackbar.LENGTH_LONG)
                        .show();
            }
        }
    }

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
                        aet_getText + ".substratum");
                Snackbar.make(getView(),
                        directory_parse,
                        Snackbar.LENGTH_LONG)
                        .show();
            } else {
                String directory_parse = String.format(getString(R.string.toast_backup_success),
                        aet_getText + "/");
                Snackbar.make(getView(),
                        directory_parse,
                        Snackbar.LENGTH_LONG)
                        .show();
            }
            RefreshSpinner();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            String uid = Environment.getExternalStorageDirectory().getAbsolutePath()
                    .split("/")[3];
            if (References.checkOMS(getContext())) {
                References.copy("/data/system/overlays.xml",
                        Environment.getExternalStorageDirectory().getAbsolutePath() +
                                "/substratum/profiles/" + aet_getText + ".substratum");

                File makeProfileDir = new File(Environment
                        .getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/" + aet_getText + "/");
                if (makeProfileDir.exists()) {
                    References.delete(
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/substratum/profiles/" + aet_getText);
                    boolean created = makeProfileDir.mkdir();
                    if (!created)
                        Log.e(References.SUBSTRATUM_LOG, "Could not create profile directory.");
                } else {
                    boolean created = makeProfileDir.mkdir();
                    if (!created)
                        Log.e(References.SUBSTRATUM_LOG, "Could not create profile directory.");
                }

                // Backup the entire /data/system/theme/ folder
                References.copyDir("/data/system/theme/",
                        Environment.getExternalStorageDirectory().getAbsolutePath() +
                                "/substratum/profiles/" + aet_getText);

                // Backup wallpapers
                References.copy("/data/system/users/" + uid + "/wallpaper",
                        Environment.getExternalStorageDirectory().getAbsolutePath()
                                + "/substratum/profiles/" + aet_getText + "/wallpaper.png");
                References.copy("/data/system/users/" + uid + "/wallpaper_lock",
                        Environment.getExternalStorageDirectory().getAbsolutePath()
                                + "/substratum/profiles/" + aet_getText + "/wallpaper_lock.png");

                // Backup system bootanimation if encrypted
                if (getDeviceEncryptionStatus() >= 2) {
                    References.copy("/system/media/bootanimation.zip",
                            Environment.getExternalStorageDirectory().getAbsolutePath()
                                    + "/substratum/profiles/" + aet_getText + "/bootanimation.zip");
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
                    References.mountRW();
                    References.copyDir(current_directory,
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/substratum/profiles/");
                    File oldFolder = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/substratum/profiles/overlay");
                    File newFolder = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/substratum/profiles/" + aet_getText);
                    boolean success = oldFolder.renameTo(newFolder);
                    if (!success)
                        Log.e(References.SUBSTRATUM_LOG, "Could not move profile directory...");

                    // Now begin backing up sounds
                    References.copyDir("/data/system/theme/audio/",
                            Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/substratum/profiles/" + aet_getText);
                    References.mountRO();

                    // Don't forget the wallpaper
                    File homeWall = new File("/data/system/users/" + uid + "/wallpaper");
                    File lockWall = new File("/data/system/users/" + uid + "/wallpaper_lock");
                    if (homeWall.exists()) {
                        References.copy(homeWall.getAbsolutePath(),
                                Environment.getExternalStorageDirectory().getAbsolutePath()
                                        + "/substratum/profiles/" + aet_getText + "/wallpaper.png");
                    }
                    if (lockWall.exists()) {
                        References.copy(lockWall.getAbsolutePath(),
                                Environment.getExternalStorageDirectory().getAbsolutePath()
                                        + "/substratum/profiles/" + aet_getText +
                                        "/wallpaper_lock.png");
                    }

                    // And bootanimation
                    References.copy("/system/media/bootanimation.zip",
                            Environment.getExternalStorageDirectory().getAbsolutePath()
                                    + "/substratum/profiles/" + aet_getText);
                } else {
                    Snackbar.make(getView(),
                            getString(R.string.backup_no_overlays),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
            return null;
        }
    }

    private class RestoreFunction extends AsyncTask<String, Integer, String> {

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
                            .setPositiveButton(getString(R.string.restore_dialog_okay), (dialog, which) -> {
                                if (References.isPackageInstalled(getContext(),
                                        "masquerade.substratum")) {
                                    Intent runCommand = new Intent();
                                    runCommand.addFlags(Intent
                                            .FLAG_INCLUDE_STOPPED_PACKAGES);
                                    runCommand.setAction("masquerade.substratum" +
                                            ".COMMANDS");
                                    runCommand.putExtra("om-commands",
                                            to_be_run_commands);

                                    getContext().sendBroadcast(runCommand);
                                    if (!helper_exists) {
                                        Snackbar.make(getView(),
                                                getString(R.string.
                                                        profile_edittext_empty_toast_sysui),
                                                Snackbar.LENGTH_LONG)
                                                .show();
                                    }
                                } else {
                                    new References.ThreadRunner().execute
                                            (to_be_run_commands);
                                    if (!helper_exists) {
                                        Snackbar.make(getView(),
                                                getString(R.string.
                                                        profile_edittext_empty_toast_sysui),
                                                Snackbar.LENGTH_LONG)
                                                .show();
                                    }
                                }
                            })
                            .setNegativeButton(getString(R.string.restore_dialog_cancel),
                                    (dialog, which) -> headerProgress.setVisibility(View.GONE))
                            .create().show();
                } else {
                    if (References.isPackageInstalled(getContext(), "masquerade.substratum")) {
                        Intent runCommand = new Intent();
                        runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        runCommand.setAction("masquerade.substratum.COMMANDS");
                        runCommand.putExtra("om-commands", to_be_run_commands);
                        getContext().sendBroadcast(runCommand);
                        if (!helper_exists) {
                            Snackbar.make(getView(),
                                    getString(R.string.
                                            profile_edittext_empty_toast_sysui),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                    } else {
                        new References.ThreadRunner().execute(to_be_run_commands);
                        if (!helper_exists) {
                            Snackbar.make(getView(),
                                    getString(R.string.
                                            profile_edittext_empty_toast_sysui),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
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
                    // Delete destination overlays
                    References.mountRW();
                    References.delete(current_directory);
                    References.delete("/data/system/theme/");
                    References.createNewFolder(current_directory);
                    References.createNewFolder("/data/system/theme/");
                    References.setPermissions(755, "/data/system/theme/");

                    File profile_apk_files = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/substratum/profiles/" +
                            profile_selector.getSelectedItem() + "/");
                    String[] located_files = profile_apk_files.list();
                    for (String found : located_files) {
                        if (!found.equals("audio")) {
                            References.copyDir(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/substratum/profiles/" + profile_selector.getSelectedItem() +
                                    "/" + found, current_directory);
                        } else {
                            References.copyDir(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/substratum/profiles/" + profile_selector.getSelectedItem() +
                                    "/" + found + "/", "/data/system/theme/audio/");
                            References.setPermissionsRecursively(644, "/data/system/theme/audio/");
                            References.setPermissions(755, "/data/system/theme/audio/");
                        }
                    }
                    References.setPermissionsRecursively(644, current_directory);
                    References.setPermissions(755, current_directory);
                    References.setContext(current_directory);
                    References.mountRO();
                } else {
                    String vendor_location = "/system/vendor/overlay/";
                    String vendor_partition = "/vendor/overlay/";
                    String vendor_symlink = "/system/overlay/";
                    String current_vendor =
                            ((References.inNexusFilter()) ? vendor_partition :
                                    vendor_location);
                    References.mountRW();
                    File vendor = new File(current_vendor);
                    if (!vendor.exists()) {
                        if (current_vendor.equals(vendor_location)) {
                            References.createNewFolder(current_vendor);
                        } else {
                            References.mountRWVendor();
                            References.createNewFolder(vendor_symlink);
                            References.symlink(vendor_symlink, "/vendor");
                            References.setPermissions(755, vendor_partition);
                            References.mountROVendor();
                        }
                    }

                    References.delete("/data/system/theme/");
                    References.createNewFolder("/data/system/theme/");

                    File profile_apk_files = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/substratum/profiles/" +
                            profile_selector.getSelectedItem() + "/");
                    String[] located_files = profile_apk_files.list();
                    for (String found : located_files) {
                        if (!found.equals("audio")) {
                            References.copyDir(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/substratum/profiles/" + profile_selector.getSelectedItem() +
                                    "/" + found, current_directory);
                        } else {
                            References.setPermissions(755, "/data/system/theme/");
                            References.copyDir(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/substratum/profiles/" + profile_selector.getSelectedItem() +
                                    "/" + found + "/", "/data/system/theme/audio/");
                            References.setPermissionsRecursively(644, "/data/system/theme/audio/");
                            References.setPermissions(755, "/data/system/theme/audio/");
                        }
                    }
                    References.setPermissionsRecursively(644, current_directory);
                    References.setPermissions(755, current_directory);
                    References.setContext(current_directory);
                    References.mountRO();
                }
                AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(getContext());
                alertDialogBuilder
                        .setTitle(getString(
                                R.string.legacy_dialog_soft_reboot_title));
                alertDialogBuilder
                        .setMessage(getString(
                                R.string.legacy_dialog_soft_reboot_text));
                alertDialogBuilder
                        .setPositiveButton(
                                android.R.string.ok, (dialog, id) -> References.reboot());
                alertDialogBuilder.setCancelable(false);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                headerProgress.setVisibility(View.GONE);
            }

            // Restore wallpapers
            String homeWallPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/substratum/profiles/" + profile_selector.getSelectedItem() + "/" +
                    "/wallpaper.png";
            String lockWallPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/substratum/profiles/" + profile_selector.getSelectedItem() + "/" +
                    "/wallpaper_lock.png";
            File homeWall = new File(homeWallPath);
            File lockWall = new File(lockWallPath);
            if (homeWall.exists() || lockWall.exists()) {
                WallpaperManager wm = WallpaperManager.getInstance(getContext());
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wm.setStream(new FileInputStream(homeWallPath), null, true,
                                WallpaperManager.FLAG_SYSTEM);
                        wm.setStream(new FileInputStream(lockWallPath), null, true,
                                WallpaperManager.FLAG_LOCK);
                    } else {
                        wm.setStream(new FileInputStream(homeWallPath));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {

            if (References.checkOMS(getContext())) {  // RRO doesn't need any of this
                String profile_name = sUrl[0];
                helper_exists = true;

                File current_overlays = new File(Environment
                        .getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/current_overlays.xml");
                if (current_overlays.exists()) {
                    References.delete(Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                            "/.substratum/current_overlays.xml");
                }
                References.copy("/data/system/overlays.xml",
                        Environment.getExternalStorageDirectory().getAbsolutePath() +
                                "/.substratum/current_overlays.xml");

                String[] commandsSystem4 = {Environment.getExternalStorageDirectory()
                        .getAbsolutePath() +
                        "/.substratum/current_overlays.xml", "4"};

                String[] commandsSystem5 = {Environment
                        .getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/current_overlays.xml", "5"};

                String[] commands = {Environment.getExternalStorageDirectory()
                        .getAbsolutePath() +
                        "/substratum/profiles/" + profile_name + ".substratum", "5"};

                List<String> profile = ReadOverlaysFile.main(commands);
                List<String> system = ReadOverlaysFile.main(commandsSystem4);
                List<String> system_active = ReadOverlaysFile.main(commandsSystem5);
                system.addAll(ReadOverlaysFile.main(commandsSystem5));
                List<String> to_be_run = new ArrayList<>();

                // Disable everything enabled first
                String to_be_disabled = References.disableAllOverlays();

                // Now process the overlays to be enabled

                cannot_run_overlays = new ArrayList<>();
                for (int i = 0; i < profile.size(); i++) {
                    if (system.contains(profile.get(i))) {
                        to_be_run.add(profile.get(i));
                    } else {
                        cannot_run_overlays.add(profile.get(i));
                    }
                }
                dialog_message = "";
                for (int i = 0; i < cannot_run_overlays.size(); i++) {
                    String not_split = cannot_run_overlays.get(i);
                    String[] split = not_split.split("\\.");
                    String theme_name = split[split.length - 1];
                    String package_id = not_split.substring(0, not_split.length() - theme_name
                            .length() - 1);
                    String package_name = "";
                    try {
                        ApplicationInfo applicationInfo = getContext().getPackageManager()
                                .getApplicationInfo
                                        (package_id, 0);
                        package_name = getContext().getPackageManager().getApplicationLabel
                                (applicationInfo).toString();
                    } catch (Exception e) {
                        Log.e(References.SUBSTRATUM_LOG, "Could not find explicit package " +
                                "identifier" +
                                " in package manager list.");
                    }

                    if (i == 0) {
                        dialog_message = dialog_message + "\u2022 " + theme_name + " [" +
                                package_name + "]";
                    } else {
                        if (i > 0 && dialog_message.length() == 0) {
                            dialog_message = dialog_message + "\u2022 " + theme_name + " [" +
                                    package_name + "]";
                        } else {
                            dialog_message = dialog_message + "\n" + "\u2022 " + theme_name + " [" +
                                    package_name + "]";
                        }
                    }
                }

                to_be_run_commands = "";
                for (int i = 0; i < to_be_run.size(); i++) {
                    if (!to_be_run.get(i).equals("substratum.helper")) {
                        if (i == 0) {
                            to_be_run_commands = References.enableOverlay() + " " +
                                    to_be_run.get(i);
                        } else {
                            if (i > 0 && to_be_run_commands.length() == 0) {
                                to_be_run_commands = References.enableOverlay() + " " +
                                        to_be_run.get(i);
                            } else {
                                to_be_run_commands = to_be_run_commands + " " + to_be_run
                                        .get(i);
                            }
                        }
                    }
                }
                if (to_be_run_commands.length() > 0) {
                    to_be_run_commands = to_be_run_commands + " && cp /data/system/overlays.xml " +
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/.substratum/current_overlays.xml";
                }

                if (to_be_disabled.length() > 0) {
                    to_be_run_commands = to_be_disabled + " && " + to_be_run_commands;
                }

                File theme = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/" + profile_name + "/");
                if (theme.length() > 0) {
                    // Restore the whole backed up profile back to /data/system/theme/
                    to_be_run_commands = to_be_run_commands + " && rm -r " + "/data/system/theme";

                    // Set up work directory again

                    to_be_run_commands = to_be_run_commands + " && cp -rf " +
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/substratum/profiles/" + profile_name + "/ /data/system/theme/";
                    to_be_run_commands = to_be_run_commands + " && chmod 755 /data/system/theme/";

                    // Boot Animation
                    File bootanimation = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/substratum/profiles/" + profile_name +
                            "/bootanimation.zip");
                    if (bootanimation.exists()) {
                        if (getDeviceEncryptionStatus() <= 1) {
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod 644 /data/system/theme/bootanimation.zip";
                        } else {
                            File bootanimationBackup = new File(
                                    "/system/media/bootanimation-backup.zip");
                            to_be_run_commands = to_be_run_commands +
                                    " && mount -o rw,remount /system";
                            if (!bootanimationBackup.exists()) {
                                to_be_run_commands = to_be_run_commands +
                                        " && mv -f /system/media/bootanimation.zip" +
                                        " /system/media/bootanimation-backup.zip";
                            }
                            to_be_run_commands = to_be_run_commands + " && cp -f " +
                                    Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/substratum/profiles/" + profile_name + "/bootanimation.zip" +
                                    " /system/media/bootanimation.zip";
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod 644 /system/media/bootanimation.zip" +
                                    " && mount -o ro,remount /system";
                        }
                    }

                    // Fonts
                    File fonts = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/substratum/profiles/" + profile_name + "/fonts/");
                    if (fonts.exists()) {
                        to_be_run_commands = to_be_run_commands + " && chmod -R 747 /data/system" +
                                "/theme/fonts/";
                        to_be_run_commands = to_be_run_commands + " && chmod 775 /data/system" +
                                "/theme" +
                                "/fonts/";
                    }

                    // Sounds
                    File sounds = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/substratum/profiles/" + profile_name + "/audio/");
                    if (sounds.exists()) {
                        File alarms = new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() +
                                "/substratum/profiles/" + profile_name + "/audio/alarms/");
                        if (alarms.exists()) {
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod -R 644 /data/system/theme/audio/alarms/";
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod 755 /data/system/theme/audio/alarms/";
                        }

                        File notifications = new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() +
                                "/substratum/profiles/" + profile_name + "/audio/notifications/");
                        if (notifications.exists()) {
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod -R 644 /data/system/theme/audio/notifications/";
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod 755 /data/system/theme/audio/notifications/";
                        }

                        File ringtones = new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() +

                                "/substratum/profiles/" + profile_name + "/audio/ringtones/");
                        if (ringtones.exists()) {
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod -R 644 /data/system/theme/audio/ringtones/";
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod 755 /data/system/theme/audio/ringtones/";
                        }

                        File ui = new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() +
                                "/substratum/profiles/" + profile_name + "/audio/ui/");
                        if (ui.exists()) {
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod -R 644 /data/system/theme/audio/ui/";
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod 755 /data/system/theme/audio/ui/";
                        }

                        to_be_run_commands = to_be_run_commands +
                                " && chmod 755 /data/system/theme/audio/";
                    }

                    // Final touch ups
                    to_be_run_commands = to_be_run_commands + " && chcon -R " +
                            "u:object_r:system_file:s0" +
                            " " +
                            "/data/system/theme";
                    to_be_run_commands = to_be_run_commands + " && setprop sys.refresh_theme 1";

                    if (fonts.exists()) {
                        if (system.contains("substratum.helper")) {
                            to_be_run_commands = to_be_run_commands + " && " +
                                    References.enableOverlay() + " substratum.helper";
                        } else {
                            if (system_active.contains("substratum.helper")) {
                                to_be_run_commands = to_be_run_commands + " && " +
                                        References.disableOverlay() + " substratum.helper";
                            } else {
                                helper_exists = false;
                            }
                        }
                    }
                }
                if (!prefs.getBoolean("systemui_recreate", false)) {
                    to_be_run_commands = to_be_run_commands + " && pkill -f com.android.systemui";
                }
                Log.e("SubstratumRestore", to_be_run_commands);
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
    }
}
