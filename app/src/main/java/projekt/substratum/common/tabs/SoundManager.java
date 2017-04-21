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

package projekt.substratum.common.tabs;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeInterfacerService;
import projekt.substratum.common.platform.ThemeManager;

import static projekt.substratum.common.References.checkOMS;
import static projekt.substratum.common.References.checkThemeInterfacer;
import static projekt.substratum.common.References.getProp;

public class SoundManager {

    public static final String[] ALLOWED_SOUNDS = {
            "alarm.mp3",
            "alarm.ogg",
            "notification.mp3",
            "notification.ogg",
            "ringtone.mp3",
            "ringtone.ogg",
            "Effect_Tick.mp3",
            "Effect_Tick.ogg",
            "Lock.mp3",
            "Lock.ogg",
            "Unlock.mp3",
            "Unlock.ogg",
    };
    // Sounds processing methods
    private static final String SYSTEM_MEDIA_PATH = "/system/media/audio";
    private static final String PATH_SPLIT = SYSTEM_MEDIA_PATH + File.separator;
    private static final String SYSTEM_ALARMS_PATH = PATH_SPLIT + "alarms";
    private static final String SYSTEM_RINGTONES_PATH = PATH_SPLIT + "ringtones";
    private static final String SYSTEM_NOTIFICATIONS_PATH = PATH_SPLIT + "notifications";
    private static final String MEDIA_CONTENT_URI = "content://media/internal/audio/media";
    private static final String SYSTEM_CONTENT_URI = "content://settings/global";

    public static boolean[] setSounds(Context context, String theme_pid, String name) {
        boolean has_failed = false;
        boolean ringtone = false;

        if (checkOMS(context) && checkThemeInterfacer(context)) {
            ThemeInterfacerService.setThemedSounds(context, theme_pid, name);
            ringtone = true; // Always assume that the process is succeeded;
        } else {
            // Move the file from assets folder to a new working area
            Log.d("SoundUtils", "Copying over the selected sounds to working directory...");

            File cacheDirectory = new File(context.getCacheDir(), "/SoundsCache/");
            if (!cacheDirectory.exists()) {
                boolean created = cacheDirectory.mkdirs();
                if (created) Log.d("SoundUtils", "Sounds folder created");
            }
            File cacheDirectory2 = new File(context.getCacheDir(), "/SoundsCache/SoundsInjector/");
            if (!cacheDirectory2.exists() && cacheDirectory2.mkdirs()) {
                Log.d("SoundUtils", "Sounds work folder created");
            } else {
                FileOperations.delete(context, context.getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/");
                boolean created = cacheDirectory2.mkdirs();
                if (created) Log.d("SoundUtils", "Sounds work folder recreated");
            }

            String sounds = name;

            Log.d("SoundUtils", "Analyzing integrity of sounds archive file...");
            try {
                Context otherContext = context.createPackageContext(theme_pid, 0);
                AssetManager am = otherContext.getAssets();
                try (InputStream inputStream = am.open("audio/" + sounds + ".zip");
                     OutputStream outputStream = new FileOutputStream(context.getCacheDir()
                             .getAbsolutePath() + "/SoundsCache/SoundsInjector/" +
                             sounds + ".zip")) {
                    byte[] buffer = new byte[5120];
                    int length = inputStream.read(buffer);
                    while (length > 0) {
                        outputStream.write(buffer, 0, length);
                        length = inputStream.read(buffer);
                    }
                }
            } catch (Exception e) {
                Log.e("SoundUtils",
                        "There is no sounds.zip found within the assets of this theme! " +
                                e.getMessage());
                has_failed = true;
            }

            // Rename the file
            File workingDirectory = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/");
            File from = new File(workingDirectory, sounds + ".zip");
            sounds = sounds.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
            File to = new File(workingDirectory, sounds + ".zip");
            boolean rename = from.renameTo(to);
            if (rename) Log.d("SoundUtils", "Sounds archive successfully moved to new directory");

            // Unzip the sounds archive to get it prepared for the preview
            String source = context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/" + sounds + ".zip";
            String destination = context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/";
            try (ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(source)))) {
                ZipEntry zipEntry;
                int count;
                byte[] buffer = new byte[8192];
                while ((zipEntry = inputStream.getNextEntry()) != null) {
                    File file = new File(destination, zipEntry.getName());
                    File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException(
                                "Failed to ensure directory: " + dir.getAbsolutePath());
                    if (zipEntry.isDirectory())
                        continue;
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        while ((count = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, count);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("SoundUtils",
                        "An issue has occurred while attempting to decompress this archive. " +
                                e.getMessage());
            }

            if (!has_failed) {
                Log.d("SoundUtils",
                        "Moving sounds to theme directory " +
                                "and setting correct contextual parameters...");

                File themeDirectory = new File("/data/system/theme/");
                if (!themeDirectory.exists()) {
                    FileOperations.mountRWData();
                    FileOperations.createNewFolder("/data/system/theme/");
                    FileOperations.setPermissions(755, "/data/system/theme/");
                    FileOperations.mountROData();
                }
                File audioDirectory = new File("/data/system/theme/audio/");
                if (!audioDirectory.exists()) {
                    FileOperations.mountRWData();
                    FileOperations.createNewFolder("/data/system/theme/audio/");
                    FileOperations.setPermissions(755, "/data/system/theme/audio/");
                    FileOperations.mountROData();
                }
                ringtone = perform_action(context);
            }
        }
        return new boolean[]{has_failed, ringtone};
    }

    public static void clearSounds(Context context) {
        // ATTENTION (to developers):
        //
        // Sounds that aren't cleared (for testing purposes), but removed from the folder
        // are cleared on the next reboot. The way the ContentResolver SQL database works is that it
        // checks the file integrity of _data (file path), and if the file is missing, the database
        // entry is removed.
        if (checkOMS(context) && checkThemeInterfacer(context)) {
            ThemeInterfacerService.clearThemedSounds(context);
        } else {
            FileOperations.delete(context, "/data/system/theme/audio/");
            setDefaultAudible(context, RingtoneManager.TYPE_ALARM);
            setDefaultAudible(context, RingtoneManager.TYPE_NOTIFICATION);
            setDefaultAudible(context, RingtoneManager.TYPE_RINGTONE);
            setDefaultUISounds("lock_sound", "Lock.ogg");
            setDefaultUISounds("unlock_sound", "Unlock.ogg");
            setDefaultUISounds("low_battery_sound", "LowBattery.ogg");
            ThemeManager.restartSystemUI(context);
        }
    }

    private static boolean perform_action(Context context) {
        // Let's start with user interface sounds
        File ui = new File(context.getCacheDir().getAbsolutePath() +
                "/SoundsCache/SoundsInjector/ui/");
        File ui_temp = new File("/data/system/theme/audio/ui/");
        if (ui_temp.exists()) {
            FileOperations.delete(context, "/data/system/theme/audio/ui/");
        }
        if (ui.exists()) {
            FileOperations.createNewFolder("/data/system/theme/audio/ui/");

            File effect_tick_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Effect_Tick.mp3");
            File effect_tick_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Effect_Tick.ogg");
            if (effect_tick_mp3.exists() || effect_tick_ogg.exists()) {
                boolean mp3 = effect_tick_mp3.exists();
                boolean ogg = effect_tick_ogg.exists();
                if (mp3) {
                    FileOperations.copyDir(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Effect_Tick.mp3",
                            "/data/system/theme/audio/ui/Effect_Tick.mp3");
                    setUIAudible(
                            context,
                            effect_tick_mp3,
                            new File("/data/system/theme/audio/ui/Effect_Tick.mp3"),
                            RingtoneManager.TYPE_RINGTONE, "Effect_Tick");
                }
                if (ogg) {
                    FileOperations.copyDir(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Effect_Tick.ogg",
                            "/data/system/theme/audio/ui/Effect_Tick.ogg");
                    setUIAudible(
                            context,
                            effect_tick_ogg,
                            new File("/data/system/theme/audio/ui/Effect_Tick.ogg"),
                            RingtoneManager.TYPE_RINGTONE, "Effect_Tick");
                }
            } else {
                setDefaultUISounds("lock_sound", "Lock.ogg");
            }

            File new_lock_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Lock.mp3");
            File new_lock_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Lock.ogg");
            if (new_lock_mp3.exists() || new_lock_ogg.exists()) {
                boolean mp3 = new_lock_mp3.exists();
                boolean ogg = new_lock_ogg.exists();
                if (mp3) {
                    FileOperations.move(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Lock.mp3",
                            "/data/system/theme/audio/ui/Lock.mp3");
                    setUISounds("lock_sound", "/data/system/theme/audio/ui/Lock.mp3");
                }
                if (ogg) {
                    FileOperations.move(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Lock.ogg",
                            "/data/system/theme/audio/ui/Lock.ogg");
                    setUISounds("lock_sound", "/data/system/theme/audio/ui/Lock.ogg");
                }
            } else {
                setDefaultUISounds("lock_sound", "Lock.ogg");
            }

            File new_unlock_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Unlock.mp3");
            File new_unlock_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Unlock.ogg");
            if (new_unlock_mp3.exists() || new_unlock_ogg.exists()) {
                boolean mp3 = new_unlock_mp3.exists();
                boolean ogg = new_unlock_ogg.exists();
                if (mp3) {
                    FileOperations.move(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Unlock.mp3",
                            "/data/system/theme/audio/ui/Unlock.mp3");
                    setUISounds("unlock_sound", "/data/system/theme/audio/ui/Unlock.mp3");
                }
                if (ogg) {
                    FileOperations.move(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Unlock.ogg",
                            "/data/system/theme/audio/ui/Unlock.ogg");
                    setUISounds("unlock_sound", "/data/system/theme/audio/ui/Unlock.ogg");
                }
            } else {
                setDefaultUISounds("unlock_sound", "Unlock.ogg");
            }

            File new_lowbattery_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/LowBattery.mp3");
            File new_lowbattery_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/LowBattery.ogg");
            if (new_lowbattery_mp3.exists() || new_lowbattery_ogg.exists()) {
                boolean mp3 = new_lowbattery_mp3.exists();
                boolean ogg = new_lowbattery_ogg.exists();
                if (mp3) {
                    FileOperations.move(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/LowBattery.mp3",
                            "/data/system/theme/audio/ui/LowBattery.mp3");
                    setUISounds("low_battery_sound", "/data/system/theme/audio/ui/LowBattery.mp3");
                }
                if (ogg) {
                    FileOperations.move(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/LowBattery.ogg",
                            "/data/system/theme/audio/ui/LowBattery.ogg");
                    setUISounds("low_battery_sound", "/data/system/theme/audio/ui/LowBattery.ogg");
                }
            } else {
                setDefaultUISounds("low_battery_sound", "LowBattery.ogg");
            }
            FileOperations.setPermissionsRecursively(644, "/data/system/theme/audio/ui/");
            FileOperations.setPermissions(755, "/data/system/theme/audio/ui/");
            FileOperations.setPermissions(755, "/data/system/theme/audio/");
            FileOperations.setPermissions(755, "/data/system/theme/");
            FileOperations.setContext("/data/system/theme");
        }

        // Now let's set the common user's sound files found in Settings
        File alarms = new File(context.getCacheDir().getAbsolutePath() +
                "/SoundsCache/SoundsInjector/alarms/");
        File alarms_temp = new File("/data/system/theme/audio/alarms/");
        if (alarms_temp.exists())
            FileOperations.delete(context, "/data/system/theme/audio/alarms/");
        if (alarms.exists()) {
            File new_alarm_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/alarms/" + "/alarm.mp3");
            File new_alarm_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/alarms/" + "/alarm.ogg");
            if (new_alarm_mp3.exists() || new_alarm_ogg.exists()) {
                boolean mp3 = new_alarm_mp3.exists();
                boolean ogg = new_alarm_ogg.exists();

                FileOperations.copyDir(
                        context,
                        context.getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/alarms/",
                        "/data/system/theme/audio/");
                FileOperations.setPermissionsRecursively(644, "/data/system/theme/audio/alarms/");
                FileOperations.setPermissions(755, "/data/system/theme/audio/alarms/");

                // Prior to setting, we should clear out the current ones
                clearAudibles(context, "/data/system/theme/audio/alarms/alarm.mp3");
                clearAudibles(context, "/data/system/theme/audio/alarms/alarm.ogg");

                if (mp3)
                    setAudible(
                            context,
                            new File("/data/system/theme/audio/alarms/alarm.mp3"),
                            new File(alarms.getAbsolutePath(), "alarm.mp3"),
                            RingtoneManager.TYPE_ALARM,
                            context.getString(R.string.content_resolver_alarm_metadata));
                if (ogg)
                    setAudible(
                            context,
                            new File("/data/system/theme/audio/alarms/alarm.ogg"),
                            new File(alarms.getAbsolutePath(), "alarm.ogg"),
                            RingtoneManager.TYPE_ALARM,
                            context.getString(R.string.content_resolver_alarm_metadata));
            } else {
                setDefaultAudible(context, RingtoneManager.TYPE_ALARM);
            }
        }


        File notifications = new File(context.getCacheDir().getAbsolutePath() +
                "/SoundsCache/SoundsInjector/notifications/");
        File notifications_temp = new File("/data/system/theme/audio/notifications/");
        if (notifications_temp.exists())
            FileOperations.delete(context, "/data/system/theme/audio/notifications/");
        if (notifications.exists()) {
            File new_notifications_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/notifications/" + "/notification.mp3");
            File new_notifications_ogg = new File(context.getCacheDir()
                    .getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/notifications/" + "/notification.ogg");
            if (new_notifications_mp3.exists() || new_notifications_ogg.exists()) {
                boolean mp3 = new_notifications_mp3.exists();
                boolean ogg = new_notifications_ogg.exists();

                FileOperations.copyDir(
                        context,
                        context.getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/notifications/",
                        "/data/system/theme/audio/");
                FileOperations.setPermissionsRecursively(644,
                        "/data/system/theme/audio/notifications/");
                FileOperations.setPermissions(755, "/data/system/theme/audio/notifications/");

                // Prior to setting, we should clear out the current ones
                clearAudibles(context, "/data/system/theme/audio/notifications/notification.mp3");
                clearAudibles(context, "/data/system/theme/audio/notifications/notification.ogg");

                if (mp3)
                    setAudible(
                            context,
                            new File("/data/system/theme/audio/notifications/notification.mp3"),
                            new File(notifications.getAbsolutePath(), "notification.mp3"),
                            RingtoneManager.TYPE_NOTIFICATION,
                            context.getString(R.string.content_resolver_notification_metadata));
                if (ogg)
                    setAudible(
                            context,
                            new File("/data/system/theme/audio/notifications/notification.ogg"),
                            new File(notifications.getAbsolutePath(), "notification.ogg"),
                            RingtoneManager.TYPE_NOTIFICATION,
                            context.getString(R.string.content_resolver_notification_metadata));
            } else {
                setDefaultAudible(context, RingtoneManager.TYPE_NOTIFICATION);
            }
        }

        File ringtones = new File(context.getCacheDir().getAbsolutePath() +
                "/SoundsCache/SoundsInjector/ringtones/");
        File ringtones_temp = new File("/data/system/theme/audio/ringtones/");
        Boolean ringtone;
        if (ringtones_temp.exists())
            FileOperations.delete(context, "/data/system/theme/audio/ringtones/");
        if (ringtones.exists()) {
            ringtone = true;
            File new_ringtones_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ringtones/ringtone.mp3");
            File new_ringtones_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ringtones/ringtone.ogg");
            if (new_ringtones_mp3.exists() || new_ringtones_ogg.exists()) {
                boolean mp3 = new_ringtones_mp3.exists();
                boolean ogg = new_ringtones_ogg.exists();

                FileOperations.copyDir(
                        context,
                        context.getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/ringtones/",
                        "/data/system/theme/audio/");
                FileOperations.setPermissionsRecursively(644,
                        "/data/system/theme/audio/ringtones/");
                FileOperations.setPermissions(755, "/data/system/theme/audio/ringtones/");

                // Prior to setting, we should clear out the current ones
                clearAudibles(context, "/data/system/theme/audio/ringtones/ringtone.mp3");
                clearAudibles(context, "/data/system/theme/audio/ringtones/ringtone.ogg");

                if (mp3)
                    setAudible(
                            context,
                            new File("/data/system/theme/audio/ringtones/ringtone.mp3"),
                            new File(ringtones.getAbsolutePath(), "ringtone.mp3"),
                            RingtoneManager.TYPE_RINGTONE,
                            context.getString(R.string.content_resolver_ringtone_metadata));
                if (ogg)
                    setAudible(
                            context,
                            new File("/data/system/theme/audio/ringtones/ringtone.ogg"),
                            new File(ringtones.getAbsolutePath(), "ringtone.ogg"),
                            RingtoneManager.TYPE_RINGTONE,
                            context.getString(R.string.content_resolver_ringtone_metadata));
            } else {
                setDefaultAudible(context, RingtoneManager.TYPE_RINGTONE);
            }
        } else {
            ringtone = false;
        }

        return ringtone;
    }

    private static String getDefaultAudiblePath(int type) {
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

    private static boolean setUISounds(String sound_name, String location) {
        if (References.allowedUISound(sound_name)) {
            FileOperations.adjustContentProvider(SYSTEM_CONTENT_URI, sound_name, location);
            return true;
        }
        return false;
    }

    private static void setDefaultUISounds(String sound_name, String sound_file) {
        FileOperations.adjustContentProvider(SYSTEM_CONTENT_URI, sound_name,
                "/system/media/audio/ui/" + sound_file);
    }

    private static boolean setAudible(Context context, File ringtone, File ringtoneCache, int type,
                                      String name) {
        final String path = ringtone.getAbsolutePath();
        final String mimeType = name.endsWith(".ogg") ? "application/ogg" : "application/mp3";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, path);
        values.put(MediaStore.MediaColumns.TITLE, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.SIZE, ringtoneCache.length());
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

    private static boolean setUIAudible(Context context, File localized_ringtone,
                                        File ringtone_file, int type, String name) {
        final String path = ringtone_file.getAbsolutePath();

        final String path_clone = "/system/media/audio/ui/" + name + ".ogg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, path);
        values.put(MediaStore.MediaColumns.TITLE, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/ogg");
        values.put(MediaStore.MediaColumns.SIZE, localized_ringtone.length());
        values.put(MediaStore.Audio.Media.IS_RINGTONE, false);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
        values.put(MediaStore.Audio.Media.IS_ALARM, false);
        values.put(MediaStore.Audio.Media.IS_MUSIC, true);

        Uri uri = MediaStore.Audio.Media.getContentUriForPath(path);
        Uri newUri = null;
        Cursor c = context.getContentResolver().query(uri,
                new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DATA + "='" + path_clone + "'",
                null, null);
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            long id = c.getLong(0);
            Log.e("ContentResolver", id + "");
            c.close();
            newUri = Uri.withAppendedPath(Uri.parse(MEDIA_CONTENT_URI), "" + id);
            try {
                context.getContentResolver().update(uri, values,
                        MediaStore.MediaColumns._ID + "=" + id, null);
            } catch (Exception e) {
                Log.d("SoundUtils", "The content provider does not need to be updated. " +
                        e.getMessage());
            }
        }
        if (newUri == null)
            newUri = context.getContentResolver().insert(uri, values);
        try {
            RingtoneManager.setActualDefaultRingtoneUri(context, type, newUri);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean setDefaultAudible(Context context, int type) {
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
                uri = Uri.withAppendedPath(Uri.parse(MEDIA_CONTENT_URI), "" + id);
            }
            if (uri != null)
                RingtoneManager.setActualDefaultRingtoneUri(context, type, uri);
        } else {
            return false;
        }
        return true;
    }

    private static void clearAudibles(Context context, String audiblePath) {
        final File audibleDir = new File(audiblePath);
        if (audibleDir.exists() && audibleDir.isDirectory()) {
            String[] files = audibleDir.list();
            final ContentResolver resolver = context.getContentResolver();
            for (String s : files) {
                final String filePath = audiblePath + File.separator + s;
                Uri uri = MediaStore.Audio.Media.getContentUriForPath(filePath);
                resolver.delete(uri, MediaStore.MediaColumns.DATA + "=\"" + filePath + "\"", null);
                boolean deleted = (new File(filePath)).delete();
                if (deleted) Log.e("SoundUtils", "Database cleared");
            }
        }
    }
}