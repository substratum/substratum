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

package projekt.substratum.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.util.Calendar;

import projekt.substratum.config.FileOperations;
import projekt.substratum.config.References;

import static projekt.substratum.fragments.ProfileFragment.DAY;
import static projekt.substratum.fragments.ProfileFragment.DAY_PROFILE_HOUR;
import static projekt.substratum.fragments.ProfileFragment.NIGHT;
import static projekt.substratum.fragments.ProfileFragment.NIGHT_PROFILE_HOUR;
import static projekt.substratum.fragments.ProfileFragment.NIGHT_PROFILE_MINUTE;
import static projekt.substratum.fragments.ProfileFragment.SCHEDULED_PROFILE_CURRENT_PROFILE;
import static projekt.substratum.fragments.ProfileFragment.SCHEDULED_PROFILE_ENABLED;
import static projekt.substratum.fragments.ProfileFragment.SCHEDULED_PROFILE_TYPE_EXTRA;

public class BootCompletedDetector extends BroadcastReceiver {

    private static final String APP_CRASHED = "projekt.substratum.APP_CRASHED";
    private static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    private static final String PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";
    private SharedPreferences prefs;

    private boolean registerBroadcastReceivers(Context context) {
        try {
            IntentFilter intentAppCrashed = new IntentFilter(APP_CRASHED);
            IntentFilter intentPackageAdded = new IntentFilter(PACKAGE_ADDED);
            context.registerReceiver(new AppCrashReceiver(), intentAppCrashed);
            context.registerReceiver(new PackageModificationDetector(), intentPackageAdded);
            return true;
        } catch (Exception e) {
            // Suppress warning
        }
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BOOT_COMPLETED.equals(intent.getAction())) {
            boolean registered = registerBroadcastReceivers(context);
            if (registered) {
                Log.d(this.getClass().getSimpleName(),
                        "Successfully registered broadcast receivers " +
                                "for Substratum functionality!");
            } else {
                Log.e(this.getClass().getSimpleName(),
                        "Failed to register broadcast receivers for Substratum functionality...");
            }
            clearSubstratumCompileFolder(context);
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getBoolean(SCHEDULED_PROFILE_ENABLED, false))
                setupScheduledProfile(context);
        }
    }

    private boolean clearSubstratumCompileFolder(Context context) {
        File deleted = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/.substratum");
        FileOperations.delete(context, deleted.getAbsolutePath());
        if (!deleted.exists())
            Log.d(References.SUBSTRATUM_LOG,
                    "Successfully cleared the temporary compilation folder on " +
                            "the external storage.");
        return !deleted.exists();
    }

    private void setupScheduledProfile(Context context) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ScheduledProfileReceiver.class);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, NIGHT);
        PendingIntent nightIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, DAY);
        PendingIntent dayIntent = PendingIntent.getBroadcast(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final String currentProfile = prefs.getString(SCHEDULED_PROFILE_CURRENT_PROFILE, "");
        final int dayHour = prefs.getInt(DAY_PROFILE_HOUR, 0);
        final int dayMinute = prefs.getInt(NIGHT_PROFILE_MINUTE, 0);
        final int nightHour = prefs.getInt(NIGHT_PROFILE_HOUR, 0);
        final int nightMinute = prefs.getInt(NIGHT_PROFILE_MINUTE, 0);

        // Set up current calendar instance
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(System.currentTimeMillis());

        // Set up day night calendar instance
        Calendar calendarNight = Calendar.getInstance();
        calendarNight.setTimeInMillis(System.currentTimeMillis());
        calendarNight.set(Calendar.HOUR_OF_DAY, nightHour);
        calendarNight.set(Calendar.MINUTE, nightMinute);
        calendarNight.set(Calendar.SECOND, 0);

        Calendar calendarDay = Calendar.getInstance();
        calendarDay.setTimeInMillis(System.currentTimeMillis());
        calendarDay.set(Calendar.HOUR_OF_DAY, dayHour);
        calendarDay.set(Calendar.MINUTE, dayMinute);
        calendarDay.set(Calendar.SECOND, 0);

        // night time
        if (currentProfile.equals(NIGHT)) {
            calendarNight.add(Calendar.DAY_OF_YEAR, 1);
        }
        alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendarNight.getTimeInMillis(),
                nightIntent);

        // Bring back the day in case we went to the conditional if before
        calendarNight.set(Calendar.DAY_OF_YEAR, current.get(Calendar.DAY_OF_YEAR));

        // day time
        if (currentProfile.equals(DAY) || current.after(calendarNight)) {
            calendarDay.add(Calendar.DAY_OF_YEAR, 1);
        }
        alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendarDay.getTimeInMillis(),
                dayIntent);
    }
}