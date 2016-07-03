package projekt.substratum.util;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.util.Log;
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
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import projekt.substratum.R;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class SoundsHandler {

    public static final String SYSTEM_MEDIA_PATH = "/system/media/audio";
    public static final String SYSTEM_ALARMS_PATH = SYSTEM_MEDIA_PATH + File.separator
            + "alarms";
    public static final String SYSTEM_RINGTONES_PATH = SYSTEM_MEDIA_PATH + File.separator
            + "ringtones";
    public static final String SYSTEM_NOTIFICATIONS_PATH = SYSTEM_MEDIA_PATH + File.separator
            + "notifications";
    private static final String MEDIA_CONTENT_URI = "content://media/internal/audio/media";
    private static final String SYSTEM_CONTENT_URI = "content://settings/global";

    private Context mContext;
    private ProgressDialog progress;
    private String theme_pid;
    private boolean has_failed;
    private boolean clear_mode = false;

    public void SoundsHandler(String arguments, Context context, String theme_pid) {
        this.mContext = context;
        this.theme_pid = theme_pid;
        new SoundsHandlerAsync().execute(arguments);
    }

    public void SoundsClearer(Context context) {
        this.mContext = context;
        this.clear_mode = true;
        perform_action();
    }

    private void perform_action() {
        // Move all the assets to the finalized folders

        if (!clear_mode) {
            File alarms = new File(mContext.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/alarms/");
            File alarms_temp = new File("/data/system/theme/audio/alarms/");
            if (alarms_temp.exists()) Root.runCommand("rm -r /data/system/theme/audio/alarms/");
            if (alarms.exists()) {
                File new_alarm_mp3 = new File(mContext.getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/alarms/" + "/alarm.mp3");
                File new_alarm_ogg = new File(mContext.getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/alarms/" + "/alarm.ogg");
                if (new_alarm_mp3.exists() || new_alarm_ogg.exists()) {
                    boolean mp3 = new_alarm_mp3.exists();
                    boolean ogg = new_alarm_ogg.exists();

                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/alarms/ " +
                                    "/data/system/theme/audio/");
                    Root.runCommand("chmod -R 644 /data/system/theme/audio/alarms/");
                    Root.runCommand("chmod 755 /data/system/theme/audio/alarms/");

                    clearAudibles(mContext, "/data/system/theme/audio/alarms/alarm.mp3");
                    clearAudibles(mContext, "/data/system/theme/audio/alarms/alarm.ogg");

                    if (mp3)
                        setAudible(mContext, new File
                                        ("/data/system/theme/audio/alarms/alarm.mp3"),
                                RingtoneManager.TYPE_ALARM, "alarm.mp3");
                    if (ogg)
                        setAudible(mContext, new File
                                        ("/data/system/theme/audio/alarms/alarm.ogg"),
                                RingtoneManager.TYPE_ALARM, "alarm.ogg");
                } else {
                    setDefaultAudible(mContext, RingtoneManager.TYPE_ALARM);
                }
            }
        }


        File notifications = new File(mContext.getCacheDir().getAbsolutePath() +
                "/SoundsCache/SoundsInjector/notifications/");
        File notifications_temp = new File("/data/system/theme/audio/notifications/");
        if (notifications_temp.exists())
            Root.runCommand("rm -r /data/system/theme/audio/notifications/");
        if (notifications.exists()) {
            File new_notifications_mp3 = new File(mContext.getCacheDir()
                    .getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/notifications/" + "/notification.mp3");
            File new_notifications_ogg = new File(mContext.getCacheDir()
                    .getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/notifications/" + "/notification.ogg");
            if (new_notifications_mp3.exists() || new_notifications_ogg.exists()) {
                boolean mp3 = new_notifications_mp3.exists();
                boolean ogg = new_notifications_ogg.exists();

                Root.runCommand(
                        "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/notifications/ " +
                                "/data/system/theme/audio/");
                Root.runCommand("chmod -R 644 /data/system/theme/audio/notifications/");
                Root.runCommand("chmod 755 /data/system/theme/audio/notifications/");

                clearAudibles(mContext,
                        "/data/system/theme/audio/notifications/notification.mp3");
                clearAudibles(mContext,
                        "/data/system/theme/audio/notifications/notification.ogg");

                if (mp3)
                    setAudible(mContext, new File
                                    ("/data/system/theme/audio/notifications/notification" +
                                            ".mp3"),
                            RingtoneManager.TYPE_ALARM, "notification.mp3");
                if (ogg)
                    setAudible(mContext, new File
                                    ("/data/system/theme/audio/notifications/notification" +
                                            ".ogg"),
                            RingtoneManager.TYPE_ALARM, "notification.ogg");
            } else {
                setDefaultAudible(mContext, RingtoneManager.TYPE_NOTIFICATION);
            }
        }

        File ringtones = new File(mContext.getCacheDir().getAbsolutePath() +
                "/SoundsCache/SoundsInjector/ringtones/");
        File ringtones_temp = new File("/data/system/theme/audio/ringtones/");
        if (ringtones_temp.exists())
            Root.runCommand("rm -r /data/system/theme/audio/ringtones/");
        if (ringtones.exists()) {
            File new_ringtones_mp3 = new File(mContext.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ringtones/" + "/ringtone.mp3");
            File new_ringtones_ogg = new File(mContext.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ringtones/" + "/ringtone.ogg");
            if (new_ringtones_mp3.exists() || new_ringtones_ogg.exists()) {
                boolean mp3 = new_ringtones_mp3.exists();
                boolean ogg = new_ringtones_ogg.exists();

                Root.runCommand(
                        "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/ringtones/ " +
                                "/data/system/theme/audio/");
                Root.runCommand("chmod -R 644 /data/system/theme/audio/ringtones/");
                Root.runCommand("chmod 755 /data/system/theme/audio/ringtones/");

                clearAudibles(mContext, "/data/system/theme/audio/ringtones/ringtone" +
                        ".mp3");
                clearAudibles(mContext, "/data/system/theme/audio/ringtones/ringtone" +
                        ".ogg");

                if (mp3)
                    setAudible(mContext, new File
                                    ("/data/system/theme/audio/ringtones/ringtone.mp3"),
                            RingtoneManager.TYPE_RINGTONE, "ringtone.mp3");
                if (ogg)
                    setAudible(mContext, new File
                                    ("/data/system/theme/audio/ringtones/ringtone.ogg"),
                            RingtoneManager.TYPE_RINGTONE, "ringtone.ogg");
            } else {
                setDefaultAudible(mContext, RingtoneManager.TYPE_RINGTONE);
            }
        }

        File ui = new File(mContext.getCacheDir().getAbsolutePath() +
                "/SoundsCache/SoundsInjector/ui/");
        File ui_temp = new File("/data/system/theme/audio/ui/");
        if (ui_temp.exists()) {
            Root.runCommand("rm -r /data/system/theme/audio/ui/");
        }
        if (ui.exists()) {
            Root.runCommand("mkdir /data/system/theme/audio/ui/");

            File new_lock_mp3 = new File(mContext.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Lock.mp3");
            File new_lock_ogg = new File(mContext.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Lock.ogg");
            if (new_lock_mp3.exists() || new_lock_ogg.exists()) {
                boolean mp3 = new_lock_mp3.exists();
                boolean ogg = new_lock_ogg.exists();
                if (mp3) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Lock.mp3 " +
                                    "/data/system/theme/audio/ui/Lock.mp3");
                    setUISounds("lock_sound", "/data/system/theme/audio/ui/Lock.mp3");
                }
                if (ogg) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Lock.ogg " +
                                    "/data/system/theme/audio/ui/Lock.ogg");
                    setUISounds("lock_sound", "/data/system/theme/audio/ui/Lock.ogg");
                }
            } else {
                setDefaultUISounds("lock_sound", "Lock.ogg");
            }

            File new_unlock_mp3 = new File(mContext.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Unlock.mp3");
            File new_unlock_ogg = new File(mContext.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Unlock.ogg");
            if (new_unlock_mp3.exists() || new_unlock_ogg.exists()) {
                boolean mp3 = new_unlock_mp3.exists();
                boolean ogg = new_unlock_ogg.exists();
                if (mp3) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Unlock.mp3 " +
                                    "/data/system/theme/audio/ui/Unlock.mp3");
                    setUISounds("unlock_sound", "/data/system/theme/audio/ui/Unlock.mp3");
                }
                if (ogg) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Unlock.ogg " +
                                    "/data/system/theme/audio/ui/Unlock.ogg");
                    setUISounds("unlock_sound", "/data/system/theme/audio/ui/Unlock.ogg");
                }
            } else {
                setDefaultUISounds("unlock_sound", "Unlock.ogg");
            }

            File new_car_undock_mp3 = new File(mContext.getCacheDir().getAbsolutePath
                    () +
                    "/SoundsCache/SoundsInjector/ui/Undock.mp3");
            File new_car_undock_ogg = new File(mContext.getCacheDir().getAbsolutePath
                    () +
                    "/SoundsCache/SoundsInjector/ui/Undock.ogg");
            if (new_car_undock_mp3.exists() || new_car_undock_ogg.exists()) {
                boolean mp3 = new_car_undock_mp3.exists();
                boolean ogg = new_car_undock_ogg.exists();
                if (mp3) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Undock.mp3 " +
                                    "/data/system/theme/audio/ui/Undock.mp3");
                    setUISounds("car_undock_sound", "/data/system/theme/audio/ui/Undock" +
                            ".mp3");
                }
                if (ogg) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Undock.ogg " +
                                    "/data/system/theme/audio/ui/Undock.ogg");
                    setUISounds("car_undock_sound", "/data/system/theme/audio/ui/Undock" +
                            ".ogg");
                }
            } else {
                setDefaultUISounds("car_undock_sound", "Undock.ogg");
            }

            File new_car_dock_mp3 = new File(mContext.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Dock.mp3");
            File new_car_dock_ogg = new File(mContext.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Dock.ogg");
            if (new_car_dock_mp3.exists() || new_car_dock_ogg.exists()) {
                boolean mp3 = new_car_dock_mp3.exists();
                boolean ogg = new_car_dock_ogg.exists();
                if (mp3) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Dock.mp3 " +
                                    "/data/system/theme/audio/ui/Dock.mp3");
                    setUISounds("car_dock_sound", "/data/system/theme/audio/ui/Dock.mp3");
                }
                if (ogg) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Dock.ogg " +
                                    "/data/system/theme/audio/ui/Dock.ogg");
                    setUISounds("car_dock_sound", "/data/system/theme/audio/ui/Dock.ogg");
                }
            } else {
                setDefaultUISounds("car_dock_sound", "Dock.ogg");
            }

            File new_desk_undock_mp3 = new File(mContext.getCacheDir()
                    .getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Undock.mp3");
            File new_desk_undock_ogg = new File(mContext.getCacheDir()
                    .getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Undock.ogg");
            if (new_desk_undock_mp3.exists() || new_desk_undock_ogg.exists()) {
                boolean mp3 = new_desk_undock_mp3.exists();
                boolean ogg = new_desk_undock_ogg.exists();
                if (mp3) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Undock.mp3 " +
                                    "/data/system/theme/audio/ui/Undock.mp3");
                    setUISounds("desk_undock_sound", "/data/system/theme/audio/ui/Undock" +
                            ".mp3");
                }
                if (ogg) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Undock.ogg " +
                                    "/data/system/theme/audio/ui/Undock.ogg");
                    setUISounds("desk_undock_sound", "/data/system/theme/audio/ui/Undock" +
                            ".ogg");
                }
            } else {
                setDefaultUISounds("desk_undock_sound", "Undock.ogg");
            }

            File new_desk_dock_mp3 = new File(mContext.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Dock.mp3");
            File new_desk_dock_ogg = new File(mContext.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Dock.ogg");
            if (new_desk_dock_mp3.exists() || new_desk_dock_ogg.exists()) {
                boolean mp3 = new_desk_dock_mp3.exists();
                boolean ogg = new_desk_dock_ogg.exists();
                if (mp3) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Dock.mp3 " +
                                    "/data/system/theme/audio/ui/Dock.mp3");
                    setUISounds("desk_dock_sound", "/data/system/theme/audio/ui/Dock.mp3");
                }
                if (ogg) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Dock.ogg " +
                                    "/data/system/theme/audio/ui/Dock.ogg");
                    setUISounds("desk_dock_sound", "/data/system/theme/audio/ui/Dock.ogg");
                }
            } else {
                setDefaultUISounds("desk_dock_sound", "Dock.ogg");
            }

            File new_trusted_mp3 = new File(mContext.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Trusted.mp3");
            File new_trusted_ogg = new File(mContext.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Trusted.ogg");
            if (new_trusted_mp3.exists() || new_trusted_ogg.exists()) {
                boolean mp3 = new_trusted_mp3.exists();
                boolean ogg = new_trusted_ogg.exists();
                if (mp3) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Trusted.mp3 " +
                                    "/data/system/theme/audio/ui/Trusted.mp3");
                    setUISounds("trusted_sound", "/data/system/theme/audio/ui/Trusted.mp3");
                }
                if (ogg) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Trusted.ogg " +
                                    "/data/system/theme/audio/ui/Trusted.ogg");
                    setUISounds("trusted_sound", "/data/system/theme/audio/ui/Trusted.ogg");
                }
            } else {
                setDefaultUISounds("trusted_sound", "Trusted.ogg");
            }

            File new_lowbattery_mp3 = new File(mContext.getCacheDir().getAbsolutePath
                    () +
                    "/SoundsCache/SoundsInjector/ui/LowBattery.mp3");
            File new_lowbattery_ogg = new File(mContext.getCacheDir().getAbsolutePath
                    () +
                    "/SoundsCache/SoundsInjector/ui/LowBattery.ogg");
            if (new_lowbattery_mp3.exists() || new_lowbattery_ogg.exists()) {
                boolean mp3 = new_lowbattery_mp3.exists();
                boolean ogg = new_lowbattery_ogg.exists();
                if (mp3) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/LowBattery.mp3 " +
                                    "/data/system/theme/audio/ui/LowBattery.mp3");
                    setUISounds("low_battery_sound",
                            "/data/system/theme/audio/ui/LowBattery.mp3");
                }
                if (ogg) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/LowBattery.ogg " +
                                    "/data/system/theme/audio/ui/LowBattery.ogg");
                    setUISounds("low_battery_sound",
                            "/data/system/theme/audio/ui/LowBattery.ogg");
                }
            } else {
                setDefaultUISounds("low_battery_sound", "LowBattery.ogg");
            }

            File new_wireless_charging_started_mp3 = new File(mContext.getCacheDir()
                    .getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/WirelessChargingStarted.mp3");
            File new_wireless_charging_started_ogg = new File(mContext.getCacheDir()
                    .getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/WirelessChargingStarted.ogg");
            if (new_wireless_charging_started_mp3.exists() ||
                    new_wireless_charging_started_ogg.exists()) {
                boolean mp3 = new_wireless_charging_started_mp3.exists();
                boolean ogg = new_wireless_charging_started_ogg.exists();
                if (mp3) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/WirelessChargingStarted.mp3 " +

                                    "/data/system/theme/audio/ui/WirelessChargingStarted" +
                                    ".mp3");
                    setUISounds("wireless_charging_started_sound",
                            "/data/system/theme/audio/ui/WirelessChargingStarted.mp3");
                }
                if (ogg) {
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui" +
                                    "/WirelessChargingStarted.ogg " +
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

    private boolean isAllowedUI(String targetValue) {
        String[] allowed_themable = new String[]{"lock_sound", "unlock_sound",
                "car_undock_sound",
                "trusted_sound", "desk_undock_sound", "car_dock_sound", "low_battery_sound",
                "wireless_charging_started_sound", "desk_dock_sound"};
        return Arrays.asList(allowed_themable).contains(targetValue);
    }

    private String getDefaultAudiblePath(int type) {
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

    private String getProp(String propName) {
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

    private boolean setAudible(Context context, File ringtone, int type, String name) {
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

    private boolean setDefaultAudible(Context context, int type) {
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

    private void clearAudibles(Context context, String audiblePath) {
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

    public class SoundsHandlerAsync extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(mContext, android.R.style
                    .Theme_DeviceDefault_Dialog_Alert);
            progress.setMessage(mContext.getString(R.string.sounds_dialog_apply_text));
            progress.setIndeterminate(false);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();
            if (!has_failed) {
                Toast toast = Toast.makeText(mContext,
                        mContext.getString(R.string.sounds_dialog_apply_success), Toast
                                .LENGTH_LONG);
                toast.show();
            } else {
                Toast toast = Toast.makeText(mContext,
                        mContext.getString(R.string.sounds_dialog_apply_failed), Toast.LENGTH_LONG);
                toast.show();
            }
            Root.runCommand("mount -o remount,ro /");
            Root.runCommand("mount -o remount,ro /data");
            Root.runCommand("mount -o remount,ro /system");

            if (!checkWriteSettingsPermissions()) {
                new AlertDialog.Builder(mContext)
                        .setTitle(mContext.getString(R.string.sounds_dialog_permissions_title))
                        .setMessage(mContext.getString(R.string.sounds_dialog_permissions_text))
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                        .setIcon(mContext.getDrawable(R.drawable.sounds_dialog_alert))
                        .show();
            }

            Root.runCommand("pkill com.android.systemui");
        }

        @Override
        protected String doInBackground(String... sUrl) {

            has_failed = false;

            // Move the file from assets folder to a new working area

            Log.d("SoundsHandler", "Copying over the selected sounds to working directory...");

            File cacheDirectory = new File(mContext.getCacheDir(), "/SoundsCache/");
            if (!cacheDirectory.exists()) {
                boolean created = cacheDirectory.mkdirs();
                if (created) Log.d("SoundsHandler", "Sounds folder created");
            }
            File cacheDirectory2 = new File(mContext.getCacheDir(), "/SoundsCache/" +
                    "SoundsInjector/");
            if (!cacheDirectory2.exists()) {
                boolean created = cacheDirectory2.mkdirs();
                if (created) Log.d("SoundsHandler", "Sounds work folder created");
            } else {
                Root.runCommand(
                        "rm -r " + mContext.getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/");
                boolean created = cacheDirectory2.mkdirs();
                if (created) Log.d("SoundsHandler", "Sounds work folder recreated");
            }

            String sounds = sUrl[0];

            if (!has_failed) {
                Log.d("SoundsHandler", "Analyzing integrity of sounds archive file...");
                try {
                    Context otherContext = mContext.createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    InputStream inputStream = am.open("audio/" + sounds + ".zip");
                    OutputStream outputStream = new FileOutputStream(mContext.getCacheDir()
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

                File workingDirectory = new File(mContext.getCacheDir()
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
                unzip(mContext.getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/" + sounds + ".zip",
                        mContext.getCacheDir().getAbsolutePath() +
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

                perform_action();

            }

            if (!has_failed) {
                Log.d("SoundsHandler", "Sound pack installed!");
                Root.runCommand(
                        "rm -r " + mContext.getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/");
            } else {
                Log.e("SoundsHandler", "Sound installation aborted!");
                Root.runCommand(
                        "rm -r " + mContext.getCacheDir().getAbsolutePath() +
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

        private boolean checkWriteSettingsPermissions() {
            String permission = "android.permission.WRITE_SETTINGS";
            int res = mContext.checkCallingOrSelfPermission(permission);
            return (res == PackageManager.PERMISSION_GRANTED);
        }
    }
}