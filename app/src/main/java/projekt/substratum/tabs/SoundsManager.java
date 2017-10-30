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

package projekt.substratum.tabs;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.RestrictTo;
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
import projekt.substratum.common.Internal;
import projekt.substratum.common.Resources;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.SubstratumService;
import projekt.substratum.common.platform.ThemeInterfacerService;
import projekt.substratum.common.platform.ThemeManager;

import static projekt.substratum.common.Internal.ALARM_THEME_DIRECTORY;
import static projekt.substratum.common.Internal.AUDIO_THEME_DIRECTORY;
import static projekt.substratum.common.Internal.BYTE_ACCESS_RATE;
import static projekt.substratum.common.Internal.NOTIF_THEME_DIRECTORY;
import static projekt.substratum.common.Internal.RINGTONE_THEME_DIRECTORY;
import static projekt.substratum.common.Internal.SOUNDS_CACHE;
import static projekt.substratum.common.Internal.SOUNDS_CREATION_CACHE;
import static projekt.substratum.common.Internal.THEME_644;
import static projekt.substratum.common.Internal.THEME_755;
import static projekt.substratum.common.Internal.THEME_DIRECTORY;
import static projekt.substratum.common.Internal.UI_THEME_DIRECTORY;
import static projekt.substratum.common.Systems.checkOMS;
import static projekt.substratum.common.Systems.checkSubstratumService;
import static projekt.substratum.common.Systems.checkThemeInterfacer;
import static projekt.substratum.common.Systems.getProp;

public enum SoundsManager {
    ;

    private static final String TAG = "SoundsManager";
    private static final String PATH_SPLIT = Internal.SYSTEM_MEDIA_PATH + File.separator;
    private static final String SYSTEM_ALARMS_PATH = PATH_SPLIT + "alarms";
    private static final String SYSTEM_RINGTONES_PATH = PATH_SPLIT + "ringtones";
    private static final String SYSTEM_NOTIFICATIONS_PATH = PATH_SPLIT + "notifications";
    private static final String MEDIA_CONTENT_URI = "content://media/internal/audio/media";
    private static final String SYSTEM_CONTENT_URI = "content://settings/global";

    /**
     * Set sound pack
     *
     * @param context   Context
     * @param theme_pid Theme package name
     * @param name      Name of sound pack
     * @return Return an array of booleans
     */
    public static boolean[] setSounds(
            Context context,
            String theme_pid,
            String name) {
        boolean has_failed = false;
        boolean ringtone = false;

        if (checkOMS(context) && checkSubstratumService(context)) {
            SubstratumService.setSounds(theme_pid, name);
        } else if (checkOMS(context) && checkThemeInterfacer(context)) {
            ThemeInterfacerService.setThemedSounds(context, theme_pid, name);
            ringtone = true; // Always assume that the process is succeeded;
        } else {
            // Move the file from assets folder to a new working area
            Log.d(TAG, "Copying over the selected sounds to working directory...");

            File cacheDirectory = new File(context.getCacheDir(), SOUNDS_CACHE);
            if (!cacheDirectory.exists()) {
                boolean created = cacheDirectory.mkdirs();
                if (created) Log.d(TAG, "Sounds folder created");
            }
            File cacheDirectory2 = new File(context.getCacheDir(),
                    SOUNDS_CREATION_CACHE);
            if (!cacheDirectory2.exists() && cacheDirectory2.mkdirs()) {
                Log.d(TAG, "Sounds work folder created");
            } else {
                FileOperations.delete(context, context.getCacheDir().getAbsolutePath() +
                        SOUNDS_CREATION_CACHE);
                boolean created = cacheDirectory2.mkdirs();
                if (created) Log.d(TAG, "Sounds work folder recreated");
            }

            Log.d(TAG, "Analyzing integrity of sounds archive file...");
            String sounds = name;
            try {
                Context otherContext = context.createPackageContext(theme_pid, 0);
                AssetManager am = otherContext.getAssets();

                try (InputStream inputStream = am.open("audio/" + sounds + ".zip");
                     OutputStream outputStream = new FileOutputStream(context.getCacheDir()
                             .getAbsolutePath() + SOUNDS_CREATION_CACHE +
                             sounds + ".zip")) {
                    byte[] buffer = new byte[BYTE_ACCESS_RATE];
                    int length = inputStream.read(buffer);
                    while (length > 0) {
                        outputStream.write(buffer, 0, length);
                        length = inputStream.read(buffer);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG,
                        "There is no sounds.zip found within the assets of this theme! " +
                                e.getMessage());
                has_failed = true;
            }

            // Rename the file
            File workingDirectory = new File(context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE);
            File from = new File(workingDirectory, sounds + ".zip");
            sounds = sounds.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
            File to = new File(workingDirectory, sounds + ".zip");
            boolean rename = from.renameTo(to);
            if (rename) Log.d(TAG, "Sounds archive successfully moved to new directory");

            // Unzip the sounds archive to get it prepared for the preview
            String source = context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + sounds + ".zip";
            String destination = context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE;
            try (ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(source)))) {
                ZipEntry zipEntry;
                byte[] buffer = new byte[BYTE_ACCESS_RATE];
                while ((zipEntry = inputStream.getNextEntry()) != null) {
                    File file = new File(destination, zipEntry.getName());
                    File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException(
                                "Failed to ensure directory: " + dir.getAbsolutePath());
                    if (zipEntry.isDirectory())
                        continue;
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        int count;
                        while ((count = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, count);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG,
                        "An issue has occurred while attempting to decompress this archive. " +
                                e.getMessage());
            }

            if (!has_failed) {
                Log.d(TAG,
                        "Moving sounds to theme directory " +
                                "and setting correct contextual parameters...");

                File themeDirectory = new File(THEME_DIRECTORY);
                if (!themeDirectory.exists()) {
                    FileOperations.mountRWData();
                    FileOperations.createNewFolder(THEME_DIRECTORY);
                    FileOperations.setPermissions(THEME_755, THEME_DIRECTORY);
                    FileOperations.mountROData();
                }
                File audioDirectory = new File(AUDIO_THEME_DIRECTORY);
                if (!audioDirectory.exists()) {
                    FileOperations.mountRWData();
                    FileOperations.createNewFolder(AUDIO_THEME_DIRECTORY);
                    FileOperations.setPermissions(THEME_755, AUDIO_THEME_DIRECTORY);
                    FileOperations.mountROData();
                }
                ringtone = perform_action(context);
            }
        }
        return new boolean[]{has_failed, ringtone};
    }

    /**
     * Clear applied sound pack
     *
     * @param context Context
     */
    public static void clearSounds(Context context) {
        // ATTENTION (to developers):
        //
        // Sounds that aren't cleared (for testing purposes), but removed from the folder
        // are cleared on the next reboot. The way the ContentResolver SQL database works is that it
        // checks the file integrity of _data (file path), and if the file is missing, the database
        // entry is removed.
        if (checkOMS(context) && checkSubstratumService(context)) {
            SubstratumService.clearSounds();
        } else if (checkOMS(context) && checkThemeInterfacer(context)) {
            ThemeInterfacerService.clearThemedSounds(context);
        } else {
            FileOperations.delete(context, AUDIO_THEME_DIRECTORY);
            setDefaultAudible(context, RingtoneManager.TYPE_ALARM);
            setDefaultAudible(context, RingtoneManager.TYPE_NOTIFICATION);
            setDefaultAudible(context, RingtoneManager.TYPE_RINGTONE);
            setDefaultUISounds("lock_sound", "Lock.ogg");
            setDefaultUISounds("unlock_sound", "Unlock.ogg");
            setDefaultUISounds("low_battery_sound", "LowBattery.ogg");
            ThemeManager.restartSystemUI(context);
        }
    }

    /**
     * The beef of it all!
     *
     * @param context Context
     * @return True, if all the specified action passes
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private static boolean perform_action(Context context) {
        // Let's start with user interface sounds
        File ui = new File(context.getCacheDir().getAbsolutePath() + SOUNDS_CREATION_CACHE + "ui/");
        File ui_temp = new File(UI_THEME_DIRECTORY);
        if (ui_temp.exists()) FileOperations.delete(context, UI_THEME_DIRECTORY);
        if (ui.exists()) {
            FileOperations.createNewFolder(UI_THEME_DIRECTORY);

            File effect_tick_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + "ui/Effect_Tick.mp3");
            File effect_tick_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + "ui/Effect_Tick.ogg");
            if (effect_tick_mp3.exists() || effect_tick_ogg.exists()) {
                boolean mp3 = effect_tick_mp3.exists();
                if (mp3) {
                    FileOperations.copyDir(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    SOUNDS_CREATION_CACHE + "ui/Effect_Tick.mp3",
                            UI_THEME_DIRECTORY + "Effect_Tick.mp3");
                    setUIAudible(
                            context,
                            effect_tick_mp3,
                            new File(UI_THEME_DIRECTORY + "Effect_Tick.mp3"),
                            RingtoneManager.TYPE_RINGTONE, "Effect_Tick");
                }
                boolean ogg = effect_tick_ogg.exists();
                if (ogg) {
                    FileOperations.copyDir(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    SOUNDS_CREATION_CACHE + "ui/Effect_Tick.ogg",
                            UI_THEME_DIRECTORY + "Effect_Tick.ogg");
                    setUIAudible(
                            context,
                            effect_tick_ogg,
                            new File(UI_THEME_DIRECTORY + "Effect_Tick.ogg"),
                            RingtoneManager.TYPE_RINGTONE, "Effect_Tick");
                }
            } else {
                setDefaultUISounds("lock_sound", "Lock.ogg");
            }

            File new_lock_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + "ui/Lock.mp3");
            File new_lock_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + "ui/Lock.ogg");
            if (new_lock_mp3.exists() || new_lock_ogg.exists()) {
                boolean mp3 = new_lock_mp3.exists();
                if (mp3) {
                    FileOperations.move(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    SOUNDS_CREATION_CACHE + "Lock.mp3",
                            UI_THEME_DIRECTORY + "Lock.mp3");
                    setUISounds("lock_sound", UI_THEME_DIRECTORY + "Lock.mp3");
                }
                boolean ogg = new_lock_ogg.exists();
                if (ogg) {
                    FileOperations.move(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    SOUNDS_CREATION_CACHE + "ui/Lock.ogg",
                            UI_THEME_DIRECTORY + "Lock.ogg");
                    setUISounds("lock_sound", UI_THEME_DIRECTORY + "Lock.ogg");
                }
            } else {
                setDefaultUISounds("lock_sound", "Lock.ogg");
            }

            File new_unlock_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + "ui/Unlock.mp3");
            File new_unlock_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + "ui/Unlock.ogg");
            if (new_unlock_mp3.exists() || new_unlock_ogg.exists()) {
                boolean mp3 = new_unlock_mp3.exists();
                if (mp3) {
                    FileOperations.move(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    SOUNDS_CREATION_CACHE + "ui/Unlock.mp3",
                            UI_THEME_DIRECTORY + "Unlock.mp3");
                    setUISounds("unlock_sound", UI_THEME_DIRECTORY + "Unlock.mp3");
                }
                boolean ogg = new_unlock_ogg.exists();
                if (ogg) {
                    FileOperations.move(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    SOUNDS_CREATION_CACHE + "ui/Unlock.ogg",
                            UI_THEME_DIRECTORY + "Unlock.ogg");
                    setUISounds("unlock_sound", UI_THEME_DIRECTORY + "Unlock.ogg");
                }
            } else {
                setDefaultUISounds("unlock_sound", "Unlock.ogg");
            }

            File new_lowbattery_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + "ui/LowBattery.mp3");
            File new_lowbattery_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + "ui/LowBattery.ogg");
            if (new_lowbattery_mp3.exists() || new_lowbattery_ogg.exists()) {
                boolean mp3 = new_lowbattery_mp3.exists();
                if (mp3) {
                    FileOperations.move(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    SOUNDS_CREATION_CACHE + "ui/LowBattery.mp3",
                            UI_THEME_DIRECTORY + "LowBattery.mp3");
                    setUISounds("low_battery_sound", UI_THEME_DIRECTORY + "LowBattery.mp3");
                }
                boolean ogg = new_lowbattery_ogg.exists();
                if (ogg) {
                    FileOperations.move(
                            context,
                            context.getCacheDir().getAbsolutePath() +
                                    SOUNDS_CREATION_CACHE + "ui/LowBattery.ogg",
                            UI_THEME_DIRECTORY + "LowBattery.ogg");
                    setUISounds("low_battery_sound", UI_THEME_DIRECTORY + "LowBattery.ogg");
                }
            } else {
                setDefaultUISounds("low_battery_sound", "LowBattery.ogg");
            }
            FileOperations.setPermissionsRecursively(THEME_644, UI_THEME_DIRECTORY);
            FileOperations.setPermissions(THEME_755, UI_THEME_DIRECTORY);
            FileOperations.setPermissions(THEME_755, AUDIO_THEME_DIRECTORY);
            FileOperations.setPermissions(THEME_755, THEME_DIRECTORY);
            FileOperations.setContext(THEME_DIRECTORY);
        }

        // Now let's set the common user's sound files found in Settings
        File alarms = new File(context.getCacheDir().getAbsolutePath() +
                SOUNDS_CREATION_CACHE + "alarms/");
        File alarms_temp = new File(ALARM_THEME_DIRECTORY);
        if (alarms_temp.exists())
            FileOperations.delete(context, ALARM_THEME_DIRECTORY);
        if (alarms.exists()) {
            File new_alarm_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + "alarms/" + "/alarm.mp3");
            File new_alarm_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + "alarms/" + "/alarm.ogg");
            if (new_alarm_mp3.exists() || new_alarm_ogg.exists()) {

                FileOperations.copyDir(
                        context,
                        context.getCacheDir().getAbsolutePath() + SOUNDS_CREATION_CACHE + "alarms/",
                        AUDIO_THEME_DIRECTORY);
                FileOperations.setPermissionsRecursively(THEME_644, ALARM_THEME_DIRECTORY);
                FileOperations.setPermissions(THEME_755, ALARM_THEME_DIRECTORY);

                // Prior to setting, we should clear out the current ones
                clearAudibles(context, ALARM_THEME_DIRECTORY + "alarm.mp3");
                clearAudibles(context, ALARM_THEME_DIRECTORY + "alarm.ogg");

                boolean mp3 = new_alarm_mp3.exists();
                if (mp3)
                    setAudible(
                            context,
                            new File(ALARM_THEME_DIRECTORY + "alarm.mp3"),
                            new File(alarms.getAbsolutePath(), "alarm.mp3"),
                            RingtoneManager.TYPE_ALARM,
                            context.getString(R.string.content_resolver_alarm_metadata));
                boolean ogg = new_alarm_ogg.exists();
                if (ogg)
                    setAudible(
                            context,
                            new File(ALARM_THEME_DIRECTORY + "alarm.ogg"),
                            new File(alarms.getAbsolutePath(), "alarm.ogg"),
                            RingtoneManager.TYPE_ALARM,
                            context.getString(R.string.content_resolver_alarm_metadata));
            } else {
                setDefaultAudible(context, RingtoneManager.TYPE_ALARM);
            }
        }

        File notifications = new File(context.getCacheDir().getAbsolutePath() +
                SOUNDS_CREATION_CACHE + "notifications/");
        File notifications_temp = new File(NOTIF_THEME_DIRECTORY);
        if (notifications_temp.exists())
            FileOperations.delete(context, NOTIF_THEME_DIRECTORY);
        if (notifications.exists()) {
            File new_notifications_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + "notifications/notification.mp3");
            File new_notifications_ogg = new File(context.getCacheDir()
                    .getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + "notifications/notification.ogg");
            if (new_notifications_mp3.exists() || new_notifications_ogg.exists()) {

                FileOperations.copyDir(
                        context,
                        context.getCacheDir().getAbsolutePath() +
                                SOUNDS_CREATION_CACHE + "notifications/",
                        AUDIO_THEME_DIRECTORY);
                FileOperations.setPermissionsRecursively(THEME_644,
                        NOTIF_THEME_DIRECTORY);
                FileOperations.setPermissions(THEME_755, NOTIF_THEME_DIRECTORY);

                // Prior to setting, we should clear out the current ones
                clearAudibles(context, NOTIF_THEME_DIRECTORY + "notification.mp3");
                clearAudibles(context, NOTIF_THEME_DIRECTORY + "notification.ogg");

                boolean mp3 = new_notifications_mp3.exists();
                if (mp3)
                    setAudible(
                            context,
                            new File(NOTIF_THEME_DIRECTORY + "notification.mp3"),
                            new File(notifications.getAbsolutePath(), "notification.mp3"),
                            RingtoneManager.TYPE_NOTIFICATION,
                            context.getString(R.string.content_resolver_notification_metadata));
                boolean ogg = new_notifications_ogg.exists();
                if (ogg)
                    setAudible(
                            context,
                            new File(NOTIF_THEME_DIRECTORY + "notification.ogg"),
                            new File(notifications.getAbsolutePath(), "notification.ogg"),
                            RingtoneManager.TYPE_NOTIFICATION,
                            context.getString(R.string.content_resolver_notification_metadata));
            } else {
                setDefaultAudible(context, RingtoneManager.TYPE_NOTIFICATION);
            }
        }

        File ringtones = new File(context.getCacheDir().getAbsolutePath() +
                SOUNDS_CREATION_CACHE + "ringtones/");
        File ringtones_temp = new File(RINGTONE_THEME_DIRECTORY);
        if (ringtones_temp.exists())
            FileOperations.delete(context, RINGTONE_THEME_DIRECTORY);
        Boolean ringtone;
        if (ringtones.exists()) {
            ringtone = true;
            File new_ringtones_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + "ringtones/ringtone.mp3");
            File new_ringtones_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    SOUNDS_CREATION_CACHE + "ringtones/ringtone.ogg");
            if (new_ringtones_mp3.exists() || new_ringtones_ogg.exists()) {

                FileOperations.copyDir(
                        context,
                        context.getCacheDir().getAbsolutePath() +
                                SOUNDS_CREATION_CACHE + "ringtones/",
                        AUDIO_THEME_DIRECTORY);
                FileOperations.setPermissionsRecursively(THEME_644, RINGTONE_THEME_DIRECTORY);
                FileOperations.setPermissions(THEME_755, RINGTONE_THEME_DIRECTORY);

                // Prior to setting, we should clear out the current ones
                clearAudibles(context, RINGTONE_THEME_DIRECTORY + "ringtone.mp3");
                clearAudibles(context, RINGTONE_THEME_DIRECTORY + "ringtone.ogg");

                boolean mp3 = new_ringtones_mp3.exists();
                if (mp3)
                    setAudible(
                            context,
                            new File(RINGTONE_THEME_DIRECTORY + "ringtone.mp3"),
                            new File(ringtones.getAbsolutePath(), "ringtone.mp3"),
                            RingtoneManager.TYPE_RINGTONE,
                            context.getString(R.string.content_resolver_ringtone_metadata));
                boolean ogg = new_ringtones_ogg.exists();
                if (ogg)
                    setAudible(
                            context,
                            new File(RINGTONE_THEME_DIRECTORY + "ringtone.ogg"),
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

    /**
     * Get the default audible paths
     *
     * @param type RingtoneManager's type
     * @return Returns string of path
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private static String getDefaultAudiblePath(int type) {
        String name;
        String path;
        switch (type) {
            case RingtoneManager.TYPE_ALARM:
                name = getProp("ro.config.alarm_alert");
                path = (name != null) ? (SYSTEM_ALARMS_PATH + File.separator + name) : null;
                break;
            case RingtoneManager.TYPE_NOTIFICATION:
                name = getProp("ro.config.notification_sound");
                path = (name != null) ? (SYSTEM_NOTIFICATIONS_PATH + File.separator + name) : null;
                break;
            case RingtoneManager.TYPE_RINGTONE:
                name = getProp("ro.config.ringtone");
                path = (name != null) ? (SYSTEM_RINGTONES_PATH + File.separator + name) : null;
                break;
            default:
                path = null;
                break;
        }
        return path;
    }

    /**
     * Set UI sound
     *
     * @param sound_name Sound name
     * @param location   Location
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private static void setUISounds(String sound_name,
                                    String location) {
        if (Resources.allowedUISound(sound_name)) {
            FileOperations.adjustContentProvider(SYSTEM_CONTENT_URI, sound_name, location);
        }
    }

    /**
     * Set default UI sounds
     *
     * @param sound_name Sound name
     * @param sound_file Original file name
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private static void setDefaultUISounds(String sound_name,
                                           String sound_file) {
        FileOperations.adjustContentProvider(SYSTEM_CONTENT_URI, sound_name,
                "/system/media/audio/ui/" + sound_file);
    }

    /**
     * Set Audible
     *
     * @param context       Context
     * @param ringtone      File
     * @param ringtoneCache Cache
     * @param type          RingtoneManager type
     * @param name          Name
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private static void setAudible(Context context,
                                   File ringtone,
                                   File ringtoneCache,
                                   Integer type,
                                   String name) {
        String path = ringtone.getAbsolutePath();
        String mimeType = name.endsWith(".ogg") ? "application/ogg" : "application/mp3";
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
                MediaStore.MediaColumns.DATA + "='" + path + '\'',
                null, null);
        if ((c != null) && (c.getCount() > 0)) {
            c.moveToFirst();
            long id = c.getLong(0);
            c.close();
            newUri = Uri.withAppendedPath(Uri.parse(MEDIA_CONTENT_URI), String.valueOf(id));
            context.getContentResolver().update(uri, values,
                    MediaStore.MediaColumns._ID + '=' + id, null);
        }
        if (newUri == null)
            newUri = context.getContentResolver().insert(uri, values);
        try {
            RingtoneManager.setActualDefaultRingtoneUri(context, type, newUri);
        } catch (Exception e) {
            // Suppress warning
        }
    }

    /**
     * Set UI audible
     *
     * @param context            Context
     * @param localized_ringtone File of local sound
     * @param ringtone_file      File of sound
     * @param type               RingtoneManager type
     * @param name               Name
     */
    @SuppressWarnings("SameParameterValue")
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private static void setUIAudible(Context context,
                                     File localized_ringtone,
                                     File ringtone_file,
                                     Integer type,
                                     String name) {
        String path = ringtone_file.getAbsolutePath();

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
        String path_clone = UI_THEME_DIRECTORY + name + ".ogg";
        Cursor c = context.getContentResolver().query(uri,
                new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DATA + "='" + path_clone + '\'',
                null, null);
        if ((c != null) && (c.getCount() > 0)) {
            c.moveToFirst();
            long id = c.getLong(0);
            Log.e("ContentResolver", String.valueOf(id));
            c.close();
            newUri = Uri.withAppendedPath(Uri.parse(MEDIA_CONTENT_URI), String.valueOf(id));
            try {
                context.getContentResolver().update(uri, values,
                        MediaStore.MediaColumns._ID + '=' + id, null);
            } catch (Exception e) {
                Log.d(TAG, "The content provider does not need to be updated. " +
                        e.getMessage());
            }
        }
        if (newUri == null)
            newUri = context.getContentResolver().insert(uri, values);
        try {
            RingtoneManager.setActualDefaultRingtoneUri(context, type, newUri);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set default audible
     *
     * @param context Context
     * @param type    RingtoneManager type
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private static void setDefaultAudible(Context context, int type) {
        String audiblePath = getDefaultAudiblePath(type);
        if (audiblePath != null) {
            Uri uri = MediaStore.Audio.Media.getContentUriForPath(audiblePath);
            Cursor c = context.getContentResolver().query(uri,
                    new String[]{MediaStore.MediaColumns._ID},
                    MediaStore.MediaColumns.DATA + "='" + audiblePath + '\'',
                    null, null);
            if ((c != null) && (c.getCount() > 0)) {
                c.moveToFirst();
                long id = c.getLong(0);
                c.close();
                uri = Uri.withAppendedPath(Uri.parse(MEDIA_CONTENT_URI), String.valueOf(id));
            }
            if (uri != null)
                RingtoneManager.setActualDefaultRingtoneUri(context, type, uri);
        }
    }

    /**
     * Clear custom audibles
     *
     * @param context     Context
     * @param audiblePath Audible path
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private static void clearAudibles(Context context, String audiblePath) {
        File audibleDir = new File(audiblePath);
        if (audibleDir.exists() && audibleDir.isDirectory()) {
            String[] files = audibleDir.list();
            ContentResolver resolver = context.getContentResolver();
            for (String s : files) {
                String filePath = audiblePath + File.separator + s;
                Uri uri = MediaStore.Audio.Media.getContentUriForPath(filePath);
                resolver.delete(uri, MediaStore.MediaColumns.DATA + "=\"" + filePath + '"', null);
                boolean deleted = (new File(filePath)).delete();
                if (deleted) Log.e(TAG, "Database cleared");
            }
        }
    }
}