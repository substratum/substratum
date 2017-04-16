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

package projekt.substratum.config;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Calendar;

import projekt.substratum.services.ScheduledProfileReceiver;

public class ProfileManager {
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

    public static void updateScheduledProfile(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
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

    public static void enableScheduledProfile(Context context, String dayProfile,
                                              int dayHour, int dayMinute, String nightProfile,
                                              int nightHour, int nightMinute) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ScheduledProfileReceiver.class);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, NIGHT);
        PendingIntent nightIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, DAY);
        PendingIntent dayIntent = PendingIntent.getBroadcast(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

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
        editor.putBoolean(SCHEDULED_PROFILE_ENABLED, true)
                .putString(NIGHT_PROFILE, nightProfile)
                .putString(DAY_PROFILE, dayProfile)
                .putInt(NIGHT_PROFILE_HOUR, nightHour)
                .putInt(NIGHT_PROFILE_MINUTE, nightMinute)
                .putInt(DAY_PROFILE_HOUR, dayHour)
                .putInt(DAY_PROFILE_MINUTE, dayMinute)
                .apply();
    }

    public static void disableScheduledProfile(Context context) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context)
                .edit();
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ScheduledProfileReceiver.class);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, NIGHT);
        PendingIntent nightIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, DAY);
        PendingIntent dayIntent = PendingIntent.getBroadcast(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

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
        }
    }
}
