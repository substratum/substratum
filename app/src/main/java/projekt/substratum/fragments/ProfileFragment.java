package projekt.substratum.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
import java.util.Calendar;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.config.MasqueradeService;
import projekt.substratum.config.References;
import projekt.substratum.services.ScheduledProfileReceiver;
import projekt.substratum.util.ReadOverlaysFile;

public class ProfileFragment extends Fragment {

    public static final String SCHEDULED_PROFILE_ENABLED = "scheduled_profile_enabled";
    public static final String SCHEDULED_PROFILE_TYPE_EXTRA = "type";
    public static final String SCHEDULED_PROFILE_CURRENT_PROFILE = "current_profile";
    public static final String NIGHT = "night";
    public static final String NIGHT_PROFILE = "night_profile";
    public static final String NIGHT_PROFILE_HOUR = "night_profile_hour";
    public static final String NIGHT_PROFILE_MINUTE = "night_profile_minute";
    public static final String DAY = "day";
    public static final String DAY_PROFILE = "day_profile";
    public static final String DAY_PROFILE_HOUR = "day_profile_hour";
    public static final String DAY_PROFILE_MINUTE = "day_profile_minute";
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
    private SharedPreferences prefs;
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
        backup_name = (EditText) root.findViewById(R.id.edittext);

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

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder
                        .setTitle(R.string.profile_dialog_title)
                        .setMultiChoiceItems(items, null, (dialog1, which, isChecked) -> {
                            if (isChecked) {
                                selectedBackup.add(items[which]);
                            } else if (selectedBackup.contains(items[which])) {
                                selectedBackup.remove(items[which]);
                            }
                        })
                        .setPositiveButton(R.string.profile_dialog_ok, null)
                        .setNegativeButton(R.string.profile_dialog_cancel, null);

                AlertDialog dialog = builder.create();
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
                    Snackbar.make(getView(),
                            getString(R.string.profile_edittext_empty_toast),
                            Snackbar.LENGTH_LONG)
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
                                    References.delete(getContext(),
                                            Environment.getExternalStorageDirectory()
                                                    .getAbsolutePath() +
                                                    "/substratum/profiles/" +
                                                    profile_selector
                                                            .getSelectedItem());
                                    RefreshSpinner();
                                })
                        .setNegativeButton(getString(R.string.delete_dialog_cancel), (dialog,
                                                                                      which) ->
                                dialog.cancel())
                        .create().show();
            } else {
                if (getView() != null) {
                    Snackbar.make(getView(),
                            getString(R.string.profile_delete_button_none_selected_toast),
                            Snackbar.LENGTH_LONG)
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
                    Snackbar.make(getView(),
                            getString(R.string.restore_button_none_selected_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        });

        final CardView scheduledProfileCard = (CardView) root.findViewById(R.id.cardListView3);
        if (References.checkOMS(getContext()) && References.checkMasquerade(getContext()) >= 22) {
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
                    TimePickerFragment.setFlag(TimePickerFragment.FLAG_START_TIME
                            | TimePickerFragment.FLAG_GET_VALUE);
                }
                timePickerFragment.show(fm, "TimePicker");
            });

            final Button endTime = (Button) root.findViewById(R.id.night_end_time);
            endTime.setOnClickListener(view -> {
                DialogFragment timePickerFragment = new TimePickerFragment();
                if (endTime.getText().equals(getResources().getString(R.string.end_time))) {
                    TimePickerFragment.setFlag(TimePickerFragment.FLAG_END_TIME);
                } else {
                    TimePickerFragment.setFlag(TimePickerFragment.FLAG_END_TIME
                            | TimePickerFragment.FLAG_GET_VALUE);
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
                            if (dayHour != nightHour) {
                                setupScheduledProfile();
                            } else if (dayMinute != nightMinute) {
                                setupScheduledProfile();
                            } else {
                                if (getView() != null) {
                                    Snackbar.make(getView(), R.string.time_equal_warning,
                                            Snackbar.LENGTH_LONG)
                                            .show();
                                }
                            }
                        } else {
                            if (getView() != null) {
                                Snackbar.make(getView(), R.string.time_empty_warning,
                                        Snackbar.LENGTH_LONG)
                                        .show();
                            }
                        }
                    } else {
                        if (getView() != null) {
                            Snackbar.make(getView(), R.string.profile_empty_warning,
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
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

    private void setupScheduledProfile() {
        SharedPreferences.Editor editor = prefs.edit();
        AlarmManager alarmMgr = (AlarmManager) getActivity()
                .getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getActivity(), ScheduledProfileReceiver.class);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, NIGHT);
        PendingIntent nightIntent = PendingIntent.getBroadcast(getActivity(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, DAY);
        PendingIntent dayIntent = PendingIntent.getBroadcast(getActivity(), 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        if (dayNightEnabled) {
            // Set up current calendar instance
            Calendar current = Calendar.getInstance();
            current.setTimeInMillis(System.currentTimeMillis());

            // Set up day night calendar instance
            Calendar calendarNight = Calendar.getInstance();
            calendarNight.setTimeInMillis(System.currentTimeMillis());
            calendarNight.set(Calendar.HOUR_OF_DAY, nightHour);
            calendarNight.set(Calendar.MINUTE, nightMinute);

            Calendar calendarDay = Calendar.getInstance();
            calendarDay.setTimeInMillis(System.currentTimeMillis());
            calendarDay.set(Calendar.HOUR_OF_DAY, dayHour);
            calendarDay.set(Calendar.MINUTE, dayMinute);

            // Apply night profile
            if (calendarDay.after(current) && calendarNight.after(current)) {
                // We will go here when we apply in night profile time on different day,
                // make sure we apply the night profile directly
                calendarNight.add(Calendar.DAY_OF_YEAR, -1);
            }
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendarNight
                            .getTimeInMillis(),
                    nightIntent);

            // Bring back the day in case we went to the conditional if before
            calendarNight.set(Calendar.DAY_OF_YEAR, current.get(Calendar.DAY_OF_YEAR));

            // Apply day profile
            if (calendarNight.before(current)) {
                // We will go here when we apply inside night profile time, this prevent day profile
                // to be triggered
                calendarDay.add(Calendar.DAY_OF_YEAR, 1);
            }
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendarDay
                            .getTimeInMillis(),
                    dayIntent);

            // Apply prefs
            editor.putBoolean(SCHEDULED_PROFILE_ENABLED, dayNightEnabled)
                    .putString(NIGHT_PROFILE, nightProfile.getSelectedItem().toString())
                    .putString(DAY_PROFILE, dayProfile.getSelectedItem().toString())
                    .putInt(NIGHT_PROFILE_HOUR, nightHour)
                    .putInt(NIGHT_PROFILE_MINUTE, nightMinute)
                    .putInt(DAY_PROFILE_HOUR, dayHour)
                    .putInt(DAY_PROFILE_MINUTE, dayMinute)
                    .apply();

            if (getView() != null) {
                Snackbar.make(getView(), R.string.scheduled_profile_apply_success,
                        Snackbar.LENGTH_LONG)
                        .show();
            }
        } else {
            if (alarmMgr != null) {
                alarmMgr.cancel(nightIntent);
                alarmMgr.cancel(dayIntent);

                editor.remove(SCHEDULED_PROFILE_ENABLED)
                        .remove(DAY_PROFILE)
                        .remove(DAY_PROFILE_HOUR)
                        .remove(DAY_PROFILE_MINUTE)
                        .remove(NIGHT_PROFILE)
                        .remove(NIGHT_PROFILE_HOUR)
                        .remove(NIGHT_PROFILE_MINUTE)
                        .apply();
                if (getView() != null) {
                    Snackbar.make(getView(), R.string.scheduled_profile_disable_success,
                            Snackbar.LENGTH_LONG)
                            .show();
                }
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
                        backup_getText + ".substratum");
                if (getView() != null) {
                    Snackbar.make(getView(),
                            directory_parse,
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            } else {
                String directory_parse = String.format(getString(R.string.toast_backup_success),
                        backup_getText + "/");
                if (getView() != null) {
                    Snackbar.make(getView(),
                            directory_parse,
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
            RefreshSpinner();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            String uid = Environment.getExternalStorageDirectory().getAbsolutePath()
                    .split("/")[3];

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
                    References.delete(getContext(),
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
                    References.delete(getContext(), profileFile.getAbsolutePath());
                }

                if (selectedBackup.contains(getString(R.string.profile_overlay))) {
                    References.copy(getContext(), "/data/system/overlays.xml",
                            profileFile.getAbsolutePath());
                }

                // Backup the entire /data/system/theme/ folder
                References.copyDir(getContext(), "/data/system/theme",
                        Environment.getExternalStorageDirectory().getAbsolutePath() +
                                "/substratum/profiles/" + backup_getText + "/theme");

                // Delete the items user don't want to backup
                if (!selectedBackup.contains(getString(R.string.profile_boot_animation))) {
                    File bootanimation = new File(profileDir, "theme/bootanimation.zip");
                    if (bootanimation.exists()) {
                        References.delete(getContext(), bootanimation.getAbsolutePath());
                    }
                }
                if (!selectedBackup.contains(getString(R.string.profile_font))) {
                    File fonts = new File(profileDir, "theme/fonts");
                    if (fonts.exists()) {
                        References.delete(getContext(), fonts.getAbsolutePath());
                    }
                }
                if (!selectedBackup.contains(getString(R.string.profile_sound))) {
                    File sounds = new File(profileDir, "theme/audio");
                    if (sounds.exists()) {
                        References.delete(getContext(), sounds.getAbsolutePath());
                    }
                }

                // Backup wallpapers if wanted
                if (selectedBackup.contains(getString(R.string.profile_wallpaper))) {
                    References.copy(getContext(), "/data/system/users/" + uid + "/wallpaper",
                            Environment.getExternalStorageDirectory().getAbsolutePath()
                                    + "/substratum/profiles/" + backup_getText + "/wallpaper.png");
                    References.copy(getContext(), "/data/system/users/" + uid + "/wallpaper_lock",
                            Environment.getExternalStorageDirectory().getAbsolutePath()
                                    + "/substratum/profiles/" + backup_getText +
                                    "/wallpaper_lock.png");
                }

                // Backup system bootanimation if encrypted
                if (References.getDeviceEncryptionStatus(getContext()) > 1 &&
                        selectedBackup.contains(getString(R.string.profile_boot_animation))) {
                    References.copy(getContext(), "/system/media/bootanimation.zip",
                            Environment.getExternalStorageDirectory().getAbsolutePath()
                                    + "/substratum/profiles/" + backup_getText +
                                    "/bootanimation.zip");
                }

                // Clear theme profile folder if empty
                File profileThemeFolder = new File(profileDir, "theme");
                if (profileThemeFolder.list().length == 0) {
                    Log.d(References.SUBSTRATUM_LOG, "Profile theme directory is empty! delete " +
                            (profileThemeFolder.delete() ? "success" : "failed"));
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
                    if (selectedBackup.contains(getString(R.string.profile_overlay))) {
                        References.copyDir(getContext(), current_directory,
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
                        References.copyDir(getContext(), "/data/system/theme/audio/",
                                Environment.getExternalStorageDirectory()
                                        .getAbsolutePath() + "/substratum/profiles/"
                                        + backup_getText);
                    }
                    References.mountRO();

                    // Don't forget the wallpaper if wanted
                    if (selectedBackup.contains(getString(R.string.profile_wallpaper))) {
                        File homeWall = new File("/data/system/users/" + uid + "/wallpaper");
                        File lockWall = new File("/data/system/users/" + uid + "/wallpaper_lock");
                        if (homeWall.exists()) {
                            References.copy(getContext(), homeWall.getAbsolutePath(),
                                    Environment.getExternalStorageDirectory().getAbsolutePath()
                                            + "/substratum/profiles/" + backup_getText +
                                            "/wallpaper.png");
                        }
                        if (lockWall.exists()) {
                            References.copy(getContext(), lockWall.getAbsolutePath(),
                                    Environment.getExternalStorageDirectory().getAbsolutePath()
                                            + "/substratum/profiles/" + backup_getText +
                                            "/wallpaper_lock.png");
                        }
                    }

                    // And bootanimation if wanted
                    if (selectedBackup.contains(getString(R.string.profile_boot_animation))) {
                        References.copy(getContext(), "/system/media/bootanimation.zip",
                                Environment.getExternalStorageDirectory().getAbsolutePath()
                                        + "/substratum/profiles/" + backup_getText);
                    }
                } else {
                    if (getView() != null) {
                        Snackbar.make(getView(),
                                getString(R.string.backup_no_overlays),
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                }
            }
            return null;
        }
    }

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
                    current_directory = "/system/overlay/";
                } else {
                    current_directory = "/system/vendor/overlay/";
                }
                File file = new File(current_directory);
                if (file.exists()) {
                    // Delete destination overlays
                    References.mountRW();
                    References.delete(getContext(), current_directory);
                    References.delete(getContext(), "/data/system/theme/");
                    References.createNewFolder(current_directory);
                    References.createNewFolder("/data/system/theme/");
                    References.setPermissions(755, "/data/system/theme/");

                    File profile_apk_files = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/substratum/profiles/" +
                            profile_selector.getSelectedItem() + "/");
                    String[] located_files = profile_apk_files.list();
                    for (String found : located_files) {
                        if (!found.equals("audio")) {
                            References.copyDir(getContext(), Environment
                                    .getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/substratum/profiles/" + profile_selector.getSelectedItem() +
                                    "/" + found, current_directory);
                        } else {
                            References.copyDir(getContext(), Environment
                                    .getExternalStorageDirectory()
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

                    References.delete(getContext(), "/data/system/theme/");
                    References.createNewFolder("/data/system/theme/");

                    File profile_apk_files = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/substratum/profiles/" +
                            profile_selector.getSelectedItem() + "/");
                    String[] located_files = profile_apk_files.list();
                    for (String found : located_files) {
                        if (!found.equals("audio")) {
                            References.copyDir(getContext(), Environment
                                    .getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/substratum/profiles/" + profile_selector.getSelectedItem() +
                                    "/" + found, current_directory);
                        } else {
                            References.setPermissions(755, "/data/system/theme/");
                            References.copyDir(getContext(), Environment
                                    .getExternalStorageDirectory()
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
                continueProcessWallpaper();
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
                References.mountRW();
                References.move(getContext(), "/system/media/bootanimation.zip",
                        "/system/madia/bootanimation-backup.zip");
                References.copy(getContext(), bootanimation.getAbsolutePath(),
                        "/system/media/bootanimation.zip");
                References.setPermissions(644, "/system/media/bootanimation.zip");
                References.mountRO();
            }

            if (References.checkMasquerade(getContext()) >= 22) {
                MasqueradeService.applyProfile(getContext(), profile_name, new ArrayList<>(system),
                        to_be_run);
            } else {
                // Restore the whole backed up profile back to /data/system/theme/
                if (theme.exists()) {
                    References.delete(getContext(), "/data/system/theme", false);
                    References.copyDir(getContext(), theme.getAbsolutePath(), "/data/system/theme");
                    References.setPermissionsRecursively(644, "/data/system/theme/audio");
                    References.setPermissions(755, "/data/system/theme/audio");
                    References.setPermissions(755, "/data/system/theme/audio/alarms");
                    References.setPermissions(755, "/data/system/theme/audio/notifications");
                    References.setPermissions(755, "/data/system/theme/audio/ringtones");
                    References.setPermissions(755, "/data/system/theme/audio/ringtones");
                    References.setPermissionsRecursively(644, "/data/system/theme/fonts/");
                    References.setPermissions(755, "/data/system/theme/fonts/");
                    References.setContext("/data/system/theme");

                    References.disableAll(getContext());
                    References.enableOverlay(getContext(), to_be_run);
                    References.restartSystemUI(getContext());
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
                    References.setWallpaper(getContext(), homeWallPath, "home");
                    References.setWallpaper(getContext(), lockWallPath, "lock");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
