package projekt.substratum.tabs;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.adapters.SoundsAdapter;
import projekt.substratum.model.SoundsInfo;
import projekt.substratum.util.RecyclerItemClickListener;
import projekt.substratum.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class SoundPackager extends Fragment {

    public static final String SYSTEM_MEDIA_PATH = "/system/media/audio";
    public static final String SYSTEM_ALARMS_PATH = SYSTEM_MEDIA_PATH + File.separator
            + "alarms";
    public static final String SYSTEM_RINGTONES_PATH = SYSTEM_MEDIA_PATH + File.separator
            + "ringtones";
    public static final String SYSTEM_NOTIFICATIONS_PATH = SYSTEM_MEDIA_PATH + File.separator
            + "notifications";
    private static final String MEDIA_CONTENT_URI = "content://media/internal/audio/media";
    private static final String SYSTEM_CONTENT_URI = "content://settings/global";
    private String theme_pid;
    private ViewGroup root;
    private MaterialProgressBar progressBar;
    private ImageButton imageButton;
    private Spinner soundsSelector;
    private ColorStateList unchecked, checked;
    private ProgressDialog progress;
    private boolean has_failed;
    private ArrayList<SoundsInfo> wordList;
    private RecyclerView recyclerView;

    public static boolean isAllowedUI(String targetValue) {
        String[] allowed_themable = new String[]{"lock_sound", "unlock_sound", "car_undock_sound",
                "trusted_sound", "desk_undock_sound", "car_dock_sound", "low_battery_sound",
                "wireless_charging_started_sound", "desk_dock_sound"};
        return Arrays.asList(allowed_themable).contains(targetValue);
    }

    private static boolean setAudible(Context context, File ringtone, int type, String name) {
        final String path = ringtone.getAbsolutePath();
        final String mimeType = name.endsWith(".ogg") ? "audio/ogg" : "audio/mp3";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, path);
        values.put(MediaStore.MediaColumns.TITLE, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.SIZE, ringtone.length());
        values.put(MediaStore.Audio.Media.IS_RINGTONE, type == RingtoneManager.TYPE_RINGTONE);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION,
                type == RingtoneManager.TYPE_NOTIFICATION);
        values.put(MediaStore.Audio.Media.IS_ALARM, type == RingtoneManager.TYPE_ALARM);
        values.put(MediaStore.Audio.Media.IS_MUSIC, false);

        Uri uri = MediaStore.Audio.Media.getContentUriForPath(path);
        Uri newUri = null;
        Cursor c = context.getContentResolver().query(uri,
                new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DATA + "='" + path + "'",
                null, null);
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            long id = c.getLong(0);
            c.close();
            newUri = Uri.withAppendedPath(Uri.parse(MEDIA_CONTENT_URI), "" + id);
            context.getContentResolver().update(uri, values,
                    MediaStore.MediaColumns._ID + "=" + id, null);
        }
        if (newUri == null)
            newUri = context.getContentResolver().insert(uri, values);
        try {
            RingtoneManager.setActualDefaultRingtoneUri(context, type, newUri);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean setDefaultAudible(Context context, int type) {
        final String audiblePath = getDefaultAudiblePath(type);
        if (audiblePath != null) {
            Uri uri = MediaStore.Audio.Media.getContentUriForPath(audiblePath);
            Cursor c = context.getContentResolver().query(uri,
                    new String[]{MediaStore.MediaColumns._ID},
                    MediaStore.MediaColumns.DATA + "='" + audiblePath + "'",
                    null, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                long id = c.getLong(0);
                c.close();
                uri = Uri.withAppendedPath(
                        Uri.parse(MEDIA_CONTENT_URI), "" + id);
            }
            if (uri != null)
                RingtoneManager.setActualDefaultRingtoneUri(context, type, uri);
        } else {
            return false;
        }
        return true;
    }

    public static String getDefaultAudiblePath(int type) {
        final String name;
        final String path;
        switch (type) {
            case RingtoneManager.TYPE_ALARM:
                name = getProp("ro.config.alarm_alert");
                path = name != null ? SYSTEM_ALARMS_PATH + File.separator + name : null;
                break;
            case RingtoneManager.TYPE_NOTIFICATION:
                name = getProp("ro.config.notification_sound");
                path = name != null ? SYSTEM_NOTIFICATIONS_PATH + File.separator + name : null;
                break;
            case RingtoneManager.TYPE_RINGTONE:
                name = getProp("ro.config.ringtone");
                path = name != null ? SYSTEM_RINGTONES_PATH + File.separator + name : null;
                break;
            default:
                path = null;
                break;
        }
        return path;
    }

    public static void clearAudibles(Context context, String audiblePath) {
        final File audibleDir = new File(audiblePath);
        if (audibleDir.exists()) {
            String[] files = audibleDir.list();
            final ContentResolver resolver = context.getContentResolver();
            for (String s : files) {
                final String filePath = audiblePath + File.separator + s;
                Uri uri = MediaStore.Audio.Media.getContentUriForPath(filePath);
                resolver.delete(uri, MediaStore.MediaColumns.DATA + "=\""
                        + filePath + "\"", null);
                boolean deleted = (new File(filePath)).delete();
                if (deleted) Log.e("SoundsHandler", "Database cleared");
            }
        }
    }

    public static String getProp(String propName) {
        Process p;
        String result = "";
        try {
            p = new ProcessBuilder("/system/bin/getprop",
                    propName).redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                result = line;
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private boolean checkWriteSettingsPermissions() {
        String permission = "android.permission.WRITE_SETTINGS";
        int res = getContext().checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        theme_pid = InformationActivity.getThemePID();

        root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_5, container, false);

        progressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);
        progressBar.setVisibility(View.GONE);

        imageButton = (ImageButton) root.findViewById(R.id.checkBox);
        imageButton.setClickable(false);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SoundsHandler().execute(soundsSelector.getSelectedItem()
                        .toString());
            }
        });

        final RelativeLayout sounds_preview = (RelativeLayout) root.findViewById(R.id
                .sounds_placeholder);
        final RelativeLayout relativeLayout = (RelativeLayout) root.findViewById(R.id
                .relativeLayout);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayList<SoundsInfo> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new SoundsAdapter(empty_array);
        recyclerView.setAdapter(empty_adapter);

        unchecked = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        getContext().getColor(R.color.sounds_unchecked),
                        getContext().getColor(R.color.sounds_unchecked)
                }
        );
        checked = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        getContext().getColor(R.color.sounds_checked),
                        getContext().getColor(R.color.sounds_checked)
                }
        );

        try {
            Context otherContext = getContext().createPackageContext(theme_pid, 0);
            AssetManager am = otherContext.getAssets();
            String[] archivedSounds = am.list("audio");
            ArrayList<String> unarchivedSounds = new ArrayList<>();
            unarchivedSounds.add(getString(R.string.sounds_default_spinner));
            for (int i = 0; i < archivedSounds.length; i++) {
                unarchivedSounds.add(archivedSounds[i].substring(0,
                        archivedSounds[i].length() - 4));
            }
            ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, unarchivedSounds);
            soundsSelector = (Spinner) root.findViewById(R.id.soundsSelection);
            soundsSelector.setAdapter(adapter1);
            soundsSelector.setOnItemSelectedListener(new AdapterView
                    .OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {
                    if (pos == 0) {
                        imageButton.setClickable(false);
                        imageButton.setImageTintList(unchecked);
                        relativeLayout.setVisibility(View.GONE);
                        sounds_preview.setVisibility(View.VISIBLE);
                    } else {
                        sounds_preview.setVisibility(View.GONE);
                        relativeLayout.setVisibility(View.VISIBLE);
                        String[] commands = {arg0.getSelectedItem().toString()};
                        new SoundsPreview().execute(commands);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("SoundsHandler", "There is no sounds.zip found within the assets " +
                    "of this theme!");
        }

        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getContext(), new RecyclerItemClickListener
                        .OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        wordList.get(position);
                        try {
                            MediaPlayer mp = new MediaPlayer();
                            mp.setDataSource(wordList.get(position).getAbsolutePath());
                            mp.prepare();
                            if (mp.isPlaying()) {
                                mp.pause();
                            } else {
                                mp.start();
                            }
                        } catch (IOException ioe) {
                            Log.e("SoundsHandler", "Playback has failed for " + wordList.get
                                    (position).getTitle());
                        }
                    }
                })
        );
        return root;
    }

    private boolean setUISounds(String sound_name, String location) {
        if (isAllowedUI(sound_name)) {
            Root.runCommand("content insert --uri " + SYSTEM_CONTENT_URI + " " +
                    "--bind name:s:" + sound_name + " --bind value:s:" + location);
            return true;
        }
        return false;
    }

    private void setDefaultUISounds(String sound_name, String sound_file) {
        Root.runCommand("content insert --uri " + SYSTEM_CONTENT_URI + " " +
                "--bind name:s:" + sound_name + " --bind value:s:" +
                "/system/media/audio/ui/" + sound_file);
    }

    private class SoundsHandler extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getContext(), android.R.style
                    .Theme_DeviceDefault_Dialog_Alert);
            progress.setMessage(getString(R.string.sounds_dialog_apply_text));
            progress.setIndeterminate(false);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();
            if (!has_failed) {
                Toast toast = Toast.makeText(getContext(),
                        getString(R.string.sounds_dialog_apply_success), Toast.LENGTH_LONG);
                toast.show();
            } else {
                Toast toast = Toast.makeText(getContext(),
                        getString(R.string.sounds_dialog_apply_failed), Toast.LENGTH_LONG);
                toast.show();
            }
            Root.runCommand("mount -o remount,ro /");
            Root.runCommand("mount -o remount,ro /data");
            Root.runCommand("mount -o remount,ro /system");

            if (!checkWriteSettingsPermissions()) {
                new AlertDialog.Builder(getContext())
                        .setTitle(getString(R.string.sounds_dialog_permissions_title))
                        .setMessage(getString(R.string.sounds_dialog_permissions_text))
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                        .setIcon(getContext().getDrawable(R.drawable.sounds_dialog_alert))
                        .show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {

            has_failed = false;

            // Move the file from assets folder to a new working area

            Log.d("SoundsHandler", "Copying over the selected sounds to working directory...");

            File cacheDirectory = new File(getContext().getCacheDir(), "/SoundsCache/");
            if (!cacheDirectory.exists()) {
                boolean created = cacheDirectory.mkdirs();
                if (created) Log.d("SoundsHandler", "Sounds folder created");
            }
            File cacheDirectory2 = new File(getContext().getCacheDir(), "/SoundsCache/" +
                    "SoundsInjector/");
            if (!cacheDirectory2.exists()) {
                boolean created = cacheDirectory2.mkdirs();
                if (created) Log.d("SoundsHandler", "Sounds work folder created");
            } else {
                Root.runCommand(
                        "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/");
                boolean created = cacheDirectory2.mkdirs();
                if (created) Log.d("SoundsHandler", "Sounds work folder recreated");
            }

            String sounds = sUrl[0];

            if (!has_failed) {
                Log.d("SoundsHandler", "Analyzing integrity of sounds archive file...");
                try {
                    Context otherContext = getContext().createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    InputStream inputStream = am.open("audio/" + sounds + ".zip");
                    OutputStream outputStream = new FileOutputStream(getContext().getCacheDir()
                            .getAbsolutePath() + "/SoundsCache/SoundsInjector/" +
                            sounds + ".zip");

                    CopyStream(inputStream, outputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("SoundsHandler", "There is no sounds.zip found within the assets " +
                            "of this theme!");
                    has_failed = true;
                }

                // Rename the file

                File workingDirectory = new File(getContext().getCacheDir()
                        .getAbsolutePath() + "/SoundsCache/SoundsInjector/");
                File from = new File(workingDirectory, sounds + ".zip");
                sounds = sounds.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+",
                        "");
                File to = new File(workingDirectory, sounds + ".zip");
                boolean rename = from.renameTo(to);
                if (rename)
                    Log.d("SoundsHandler", "Sounds archive successfully moved to new " +
                            "directory");

                // Unzip the sounds archive to get it prepared for the preview
                unzip(getContext().getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/" + sounds + ".zip",
                        getContext().getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/");
            }

            if (!has_failed) {
                Log.d("SoundsHandler", "Moving sounds to theme directory " +
                        "and setting correct contextual parameters...");

                File themeDirectory = new File("/data/system/theme/");
                if (!themeDirectory.exists()) {
                    Root.runCommand("mount -o remount,rw /data");
                    Root.runCommand("mkdir /data/system/theme/");
                }
                File audioDirectory = new File("/data/system/theme/audio/");
                if (!audioDirectory.exists()) {
                    Root.runCommand("mount -o remount,rw /data");
                    Root.runCommand("mkdir /data/system/theme/audio/");
                }

                // Move all the assets to the finalized folders

                File alarms = new File(getContext().getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/alarms/");
                File alarms_temp = new File("/data/system/theme/audio/alarms/");
                if (alarms_temp.exists()) Root.runCommand("rm -r /data/system/theme/audio/alarms/");
                if (alarms.exists()) {
                    File new_alarm_mp3 = new File(getContext().getCacheDir().getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/alarms/" + "/alarm.mp3");
                    File new_alarm_ogg = new File(getContext().getCacheDir().getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/alarms/" + "/alarm.ogg");
                    if (new_alarm_mp3.exists() || new_alarm_ogg.exists()) {
                        boolean mp3 = new_alarm_mp3.exists();
                        boolean ogg = new_alarm_ogg.exists();

                        Root.runCommand(
                                "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                        "/SoundsCache/SoundsInjector/alarms/ " +
                                        "/data/system/theme/audio/");
                        Root.runCommand("chmod -R 644 /data/system/theme/audio/alarms/");
                        Root.runCommand("chmod 755 /data/system/theme/audio/alarms/");

                        clearAudibles(getContext(), "/data/system/theme/audio/alarms/alarm.mp3");
                        clearAudibles(getContext(), "/data/system/theme/audio/alarms/alarm.ogg");

                        if (mp3)
                            setAudible(getContext(), new File
                                            ("/data/system/theme/audio/alarms/alarm.mp3"),
                                    RingtoneManager.TYPE_ALARM, "alarm.mp3");
                        if (ogg)
                            setAudible(getContext(), new File
                                            ("/data/system/theme/audio/alarms/alarm.ogg"),
                                    RingtoneManager.TYPE_ALARM, "alarm.ogg");
                    } else {
                        setDefaultAudible(getContext(), RingtoneManager.TYPE_ALARM);
                    }
                }

                File notifications = new File(getContext().getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/notifications/");
                File notifications_temp = new File("/data/system/theme/audio/notifications/");
                if (notifications_temp.exists())
                    Root.runCommand("rm -r /data/system/theme/audio/notifications/");
                if (notifications.exists()) {
                    File new_notifications_mp3 = new File(getContext().getCacheDir()
                            .getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/notifications/" + "/notification.mp3");
                    File new_notifications_ogg = new File(getContext().getCacheDir()
                            .getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/notifications/" + "/notification.ogg");
                    if (new_notifications_mp3.exists() || new_notifications_ogg.exists()) {
                        boolean mp3 = new_notifications_mp3.exists();
                        boolean ogg = new_notifications_ogg.exists();

                        Root.runCommand(
                                "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                        "/SoundsCache/SoundsInjector/notifications/ " +
                                        "/data/system/theme/audio/");
                        Root.runCommand("chmod -R 644 /data/system/theme/audio/notifications/");
                        Root.runCommand("chmod 755 /data/system/theme/audio/notifications/");

                        clearAudibles(getContext(),
                                "/data/system/theme/audio/notifications/notification.mp3");
                        clearAudibles(getContext(),
                                "/data/system/theme/audio/notifications/notification.ogg");

                        if (mp3)
                            setAudible(getContext(), new File
                                            ("/data/system/theme/audio/notifications/notification" +
                                                    ".mp3"),
                                    RingtoneManager.TYPE_ALARM, "notification.mp3");
                        if (ogg)
                            setAudible(getContext(), new File
                                            ("/data/system/theme/audio/notifications/notification" +
                                                    ".ogg"),
                                    RingtoneManager.TYPE_ALARM, "notification.ogg");
                    } else {
                        setDefaultAudible(getContext(), RingtoneManager.TYPE_NOTIFICATION);
                    }
                }

                File ringtones = new File(getContext().getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/ringtones/");
                File ringtones_temp = new File("/data/system/theme/audio/ringtones/");
                if (ringtones_temp.exists())
                    Root.runCommand("rm -r /data/system/theme/audio/ringtones/");
                if (ringtones.exists()) {
                    File new_ringtones_mp3 = new File(getContext().getCacheDir().getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ringtones/" + "/ringtone.mp3");
                    File new_ringtones_ogg = new File(getContext().getCacheDir().getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ringtones/" + "/ringtone.ogg");
                    if (new_ringtones_mp3.exists() || new_ringtones_ogg.exists()) {
                        boolean mp3 = new_ringtones_mp3.exists();
                        boolean ogg = new_ringtones_ogg.exists();

                        Root.runCommand(
                                "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                        "/SoundsCache/SoundsInjector/ringtones/ " +
                                        "/data/system/theme/audio/");
                        Root.runCommand("chmod -R 644 /data/system/theme/audio/ringtones/");
                        Root.runCommand("chmod 755 /data/system/theme/audio/ringtones/");

                        clearAudibles(getContext(), "/data/system/theme/audio/ringtones/ringtone" +
                                ".mp3");
                        clearAudibles(getContext(), "/data/system/theme/audio/ringtones/ringtone" +
                                ".ogg");

                        if (mp3)
                            setAudible(getContext(), new File
                                            ("/data/system/theme/audio/ringtones/ringtone.mp3"),
                                    RingtoneManager.TYPE_RINGTONE, "ringtone.mp3");
                        if (ogg)
                            setAudible(getContext(), new File
                                            ("/data/system/theme/audio/ringtones/ringtone.ogg"),
                                    RingtoneManager.TYPE_RINGTONE, "ringtone.ogg");
                    } else {
                        setDefaultAudible(getContext(), RingtoneManager.TYPE_RINGTONE);
                    }
                }

                File ui = new File(getContext().getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/ui/");
                File ui_temp = new File("/data/system/theme/audio/ui/");
                if (ui_temp.exists()) {
                    Root.runCommand("rm -r /data/system/theme/audio/ui/");
                }
                if (ui.exists()) {
                    Root.runCommand("mkdir /data/system/theme/audio/ui/");

                    File new_lock_mp3 = new File(getContext().getCacheDir().getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ui/Lock.mp3");
                    File new_lock_ogg = new File(getContext().getCacheDir().getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ui/Lock.ogg");
                    if (new_lock_mp3.exists() || new_lock_ogg.exists()) {
                        boolean mp3 = new_lock_mp3.exists();
                        boolean ogg = new_lock_ogg.exists();
                        if (mp3) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/Lock.mp3 " +
                                            "/data/system/theme/audio/ui/Lock.mp3");
                            setUISounds("lock_sound", "/data/system/theme/audio/ui/Lock.mp3");
                        }
                        if (ogg) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/Lock.ogg " +
                                            "/data/system/theme/audio/ui/Lock.ogg");
                            setUISounds("lock_sound", "/data/system/theme/audio/ui/Lock.ogg");
                        }
                    } else {
                        setDefaultUISounds("lock_sound", "Lock.ogg");
                    }

                    File new_unlock_mp3 = new File(getContext().getCacheDir().getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ui/Unlock.mp3");
                    File new_unlock_ogg = new File(getContext().getCacheDir().getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ui/Unlock.ogg");
                    if (new_unlock_mp3.exists() || new_unlock_ogg.exists()) {
                        boolean mp3 = new_unlock_mp3.exists();
                        boolean ogg = new_unlock_ogg.exists();
                        if (mp3) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/Unlock.mp3 " +
                                            "/data/system/theme/audio/ui/Unlock.mp3");
                            setUISounds("unlock_sound", "/data/system/theme/audio/ui/Unlock.mp3");
                        }
                        if (ogg) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/Unlock.ogg " +
                                            "/data/system/theme/audio/ui/Unlock.ogg");
                            setUISounds("unlock_sound", "/data/system/theme/audio/ui/Unlock.ogg");
                        }
                    } else {
                        setDefaultUISounds("unlock_sound", "Unlock.ogg");
                    }

                    File new_car_undock_mp3 = new File(getContext().getCacheDir().getAbsolutePath
                            () +
                            "/SoundsCache/SoundsInjector/ui/Undock.mp3");
                    File new_car_undock_ogg = new File(getContext().getCacheDir().getAbsolutePath
                            () +
                            "/SoundsCache/SoundsInjector/ui/Undock.ogg");
                    if (new_car_undock_mp3.exists() || new_car_undock_ogg.exists()) {
                        boolean mp3 = new_car_undock_mp3.exists();
                        boolean ogg = new_car_undock_ogg.exists();
                        if (mp3) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/Undock.mp3 " +
                                            "/data/system/theme/audio/ui/Undock.mp3");
                            setUISounds("car_undock_sound", "/data/system/theme/audio/ui/Undock" +
                                    ".mp3");
                        }
                        if (ogg) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/Undock.ogg " +
                                            "/data/system/theme/audio/ui/Undock.ogg");
                            setUISounds("car_undock_sound", "/data/system/theme/audio/ui/Undock" +
                                    ".ogg");
                        }
                    } else {
                        setDefaultUISounds("car_undock_sound", "Undock.ogg");
                    }

                    File new_car_dock_mp3 = new File(getContext().getCacheDir().getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ui/Dock.mp3");
                    File new_car_dock_ogg = new File(getContext().getCacheDir().getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ui/Dock.ogg");
                    if (new_car_dock_mp3.exists() || new_car_dock_ogg.exists()) {
                        boolean mp3 = new_car_dock_mp3.exists();
                        boolean ogg = new_car_dock_ogg.exists();
                        if (mp3) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/Dock.mp3 " +
                                            "/data/system/theme/audio/ui/Dock.mp3");
                            setUISounds("car_dock_sound", "/data/system/theme/audio/ui/Dock.mp3");
                        }
                        if (ogg) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/Dock.ogg " +
                                            "/data/system/theme/audio/ui/Dock.ogg");
                            setUISounds("car_dock_sound", "/data/system/theme/audio/ui/Dock.ogg");
                        }
                    } else {
                        setDefaultUISounds("car_dock_sound", "Dock.ogg");
                    }

                    File new_desk_undock_mp3 = new File(getContext().getCacheDir()
                            .getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ui/Undock.mp3");
                    File new_desk_undock_ogg = new File(getContext().getCacheDir()
                            .getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ui/Undock.ogg");
                    if (new_desk_undock_mp3.exists() || new_desk_undock_ogg.exists()) {
                        boolean mp3 = new_desk_undock_mp3.exists();
                        boolean ogg = new_desk_undock_ogg.exists();
                        if (mp3) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/Undock.mp3 " +
                                            "/data/system/theme/audio/ui/Undock.mp3");
                            setUISounds("desk_undock_sound", "/data/system/theme/audio/ui/Undock" +
                                    ".mp3");
                        }
                        if (ogg) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/Undock.ogg " +
                                            "/data/system/theme/audio/ui/Undock.ogg");
                            setUISounds("desk_undock_sound", "/data/system/theme/audio/ui/Undock" +
                                    ".ogg");
                        }
                    } else {
                        setDefaultUISounds("desk_undock_sound", "Undock.ogg");
                    }

                    File new_desk_dock_mp3 = new File(getContext().getCacheDir().getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ui/Dock.mp3");
                    File new_desk_dock_ogg = new File(getContext().getCacheDir().getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ui/Dock.ogg");
                    if (new_desk_dock_mp3.exists() || new_desk_dock_ogg.exists()) {
                        boolean mp3 = new_desk_dock_mp3.exists();
                        boolean ogg = new_desk_dock_ogg.exists();
                        if (mp3) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/Dock.mp3 " +
                                            "/data/system/theme/audio/ui/Dock.mp3");
                            setUISounds("desk_dock_sound", "/data/system/theme/audio/ui/Dock.mp3");
                        }
                        if (ogg) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/Dock.ogg " +
                                            "/data/system/theme/audio/ui/Dock.ogg");
                            setUISounds("desk_dock_sound", "/data/system/theme/audio/ui/Dock.ogg");
                        }
                    } else {
                        setDefaultUISounds("desk_dock_sound", "Dock.ogg");
                    }

                    File new_trusted_mp3 = new File(getContext().getCacheDir().getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ui/Trusted.mp3");
                    File new_trusted_ogg = new File(getContext().getCacheDir().getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ui/Trusted.ogg");
                    if (new_trusted_mp3.exists() || new_trusted_ogg.exists()) {
                        boolean mp3 = new_trusted_mp3.exists();
                        boolean ogg = new_trusted_ogg.exists();
                        if (mp3) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/Trusted.mp3 " +
                                            "/data/system/theme/audio/ui/Trusted.mp3");
                            setUISounds("trusted_sound", "/data/system/theme/audio/ui/Trusted.mp3");
                        }
                        if (ogg) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/Trusted.ogg " +
                                            "/data/system/theme/audio/ui/Trusted.ogg");
                            setUISounds("trusted_sound", "/data/system/theme/audio/ui/Trusted.ogg");
                        }
                    } else {
                        setDefaultUISounds("trusted_sound", "Trusted.ogg");
                    }

                    File new_lowbattery_mp3 = new File(getContext().getCacheDir().getAbsolutePath
                            () +
                            "/SoundsCache/SoundsInjector/ui/LowBattery.mp3");
                    File new_lowbattery_ogg = new File(getContext().getCacheDir().getAbsolutePath
                            () +
                            "/SoundsCache/SoundsInjector/ui/LowBattery.ogg");
                    if (new_lowbattery_mp3.exists() || new_lowbattery_ogg.exists()) {
                        boolean mp3 = new_lowbattery_mp3.exists();
                        boolean ogg = new_lowbattery_ogg.exists();
                        if (mp3) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/LowBattery.mp3 " +
                                            "/data/system/theme/audio/ui/LowBattery.mp3");
                            setUISounds("low_battery_sound",
                                    "/data/system/theme/audio/ui/LowBattery.mp3");
                        }
                        if (ogg) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/LowBattery.ogg " +
                                            "/data/system/theme/audio/ui/LowBattery.ogg");
                            setUISounds("low_battery_sound",
                                    "/data/system/theme/audio/ui/LowBattery.ogg");
                        }
                    } else {
                        setDefaultUISounds("low_battery_sound", "LowBattery.ogg");
                    }

                    File new_wireless_charging_started_mp3 = new File(getContext().getCacheDir()
                            .getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ui/WirelessChargingStarted.mp3");
                    File new_wireless_charging_started_ogg = new File(getContext().getCacheDir()
                            .getAbsolutePath() +
                            "/SoundsCache/SoundsInjector/ui/WirelessChargingStarted.ogg");
                    if (new_wireless_charging_started_mp3.exists() ||
                            new_wireless_charging_started_ogg.exists()) {
                        boolean mp3 = new_wireless_charging_started_mp3.exists();
                        boolean ogg = new_wireless_charging_started_ogg.exists();
                        if (mp3) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/WirelessChargingStarted.mp3 " +
                                            "/data/system/theme/audio/ui/WirelessChargingStarted" +
                                            ".mp3");
                            setUISounds("wireless_charging_started_sound",
                                    "/data/system/theme/audio/ui/WirelessChargingStarted.mp3");
                        }
                        if (ogg) {
                            Root.runCommand(
                                    "mv -f " + getContext().getCacheDir().getAbsolutePath() +
                                            "/SoundsCache/SoundsInjector/ui/WirelessChargingStarted.ogg " +
                                            "/data/system/theme/audio/ui/WirelessChargingStarted" +
                                            ".ogg");
                            setUISounds("wireless_charging_started_sound",
                                    "/data/system/theme/audio/ui/WirelessChargingStarted.ogg");
                        }
                    } else {
                        setDefaultUISounds("wireless_charging_started_sound",
                                "WirelessChargingStarted.ogg");
                    }

                    Root.runCommand("chmod -R 644 /data/system/theme/audio/ui/");
                    Root.runCommand("chmod 755 /data/system/theme/audio/ui/");
                    Root.runCommand("chmod 755 /data/system/theme/audio/");
                    Root.runCommand("chcon -R u:object_r:system_file:s0 " +
                            "/data/system/theme");
                }
            }

            if (!has_failed) {
                Log.d("SoundsHandler", "Sound pack installed!");
                /*Root.runCommand(
                        "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/");*/
            } else {
                Log.e("SoundsHandler", "Sound installation aborted!");
                Root.runCommand(
                        "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/");
            }
            return null;
        }

        private void CopyStream(InputStream Input, OutputStream Output) throws IOException {
            byte[] buffer = new byte[5120];
            int length = Input.read(buffer);
            while (length > 0) {
                Output.write(buffer, 0, length);
                length = Input.read(buffer);
            }
        }


        private void unzip(String source, String destination) {
            try {
                ZipInputStream inputStream = new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(source)));
                ZipEntry zipEntry;
                int count;
                byte[] buffer = new byte[8192];
                while ((zipEntry = inputStream.getNextEntry()) != null) {
                    File file = new File(destination, zipEntry.getName());
                    File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException("Failed to ensure directory: " +
                                dir.getAbsolutePath());
                    if (zipEntry.isDirectory())
                        continue;
                    FileOutputStream outputStream = new FileOutputStream(file);
                    while ((count = inputStream.read(buffer)) != -1)
                        outputStream.write(buffer, 0, count);
                    outputStream.close();
                }
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("SoundsHandler",
                        "An issue has occurred while attempting to decompress this archive.");
            }
        }
    }

    private class SoundsPreview extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            imageButton.setClickable(false);
            imageButton.setImageTintList(unchecked);
            progressBar.setVisibility(View.VISIBLE);
            recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                List<SoundsInfo> adapter1 = new ArrayList<>(wordList);
                recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
                SoundsAdapter mAdapter = new SoundsAdapter(adapter1);
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
                recyclerView.setLayoutManager(mLayoutManager);
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                recyclerView.setAdapter(mAdapter);

                imageButton.setImageTintList(checked);
                imageButton.setClickable(true);
                progressBar.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e("SoundsHandler", "Window was destroyed before AsyncTask could " +
                        "perform " +
                        "postExecute()");
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            try {
                File cacheDirectory = new File(getContext().getCacheDir(), "/SoundsCache/");
                if (!cacheDirectory.exists()) {
                    boolean created = cacheDirectory.mkdirs();
                    if (created) Log.d("SoundsHandler", "Sounds folder created");
                }
                File cacheDirectory2 = new File(getContext().getCacheDir(), "/SoundCache/" +
                        "sounds_preview/");
                if (!cacheDirectory2.exists()) {
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("SoundsHandler", "Sounds work folder created");
                } else {
                    Root.runCommand(
                            "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                    "/SoundCache/sounds_preview/");
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("SoundsHandler", "Sounds folder recreated");
                }

                // Copy the sounds.zip from assets/sounds of the theme's assets

                String source = sUrl[0] + ".zip";

                final AssetManager am;

                try {
                    Context otherContext = getContext().createPackageContext(theme_pid, 0);
                    am = otherContext.getAssets();
                    InputStream inputStream = am.open("audio/" + source);
                    OutputStream outputStream = new FileOutputStream(getContext().getCacheDir()
                            .getAbsolutePath() + "/SoundsCache/" + source);
                    CopyStream(inputStream, outputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("SoundsHandler", "There is no sounds.zip found within the" +
                            " assets of this theme!");
                }

                // Unzip the sounds archive to get it prepared for the preview
                unzip(getContext().getCacheDir().getAbsolutePath() +
                                "/SoundsCache/" + source,
                        getContext().getCacheDir().getAbsolutePath() +
                                "/SoundsCache/sounds_preview/");

                wordList = new ArrayList<>();
                File testDirectory = new File(getContext().getCacheDir().getAbsolutePath() +
                        "/SoundsCache/sounds_preview/");
                listFilesForFolder(testDirectory);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("SoundsHandler", "Unexpectedly lost connection to the application " +
                        "host");
            }
            return null;
        }

        public void listFilesForFolder(final File folder) {
            for (final File fileEntry : folder.listFiles()) {
                if (fileEntry.isDirectory()) {
                    listFilesForFolder(fileEntry);
                } else {
                    if (!fileEntry.getName().substring(0, 1).equals(".")) {
                        wordList.add(new SoundsInfo(fileEntry.getName(), fileEntry
                                .getAbsolutePath()));
                    }
                }
            }
        }

        private void unzip(String source, String destination) {
            try {
                ZipInputStream inputStream = new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(source)));
                ZipEntry zipEntry;
                int count;
                byte[] buffer = new byte[8192];
                while ((zipEntry = inputStream.getNextEntry()) != null) {
                    File file = new File(destination, zipEntry.getName());
                    File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException("Failed to ensure directory: " +
                                dir.getAbsolutePath());
                    if (zipEntry.isDirectory())
                        continue;
                    FileOutputStream outputStream = new FileOutputStream(file);
                    while ((count = inputStream.read(buffer)) != -1)
                        outputStream.write(buffer, 0, count);
                    outputStream.close();
                }
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("SoundsHandler",
                        "An issue has occurred while attempting to decompress this archive.");
            }
        }

        private void CopyStream(InputStream Input, OutputStream Output) throws IOException {
            byte[] buffer = new byte[5120];
            int length = Input.read(buffer);
            while (length > 0) {
                Output.write(buffer, 0, length);
                length = Input.read(buffer);
            }
        }
    }
}