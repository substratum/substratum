package projekt.substratum.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import java.util.Calendar;

import static projekt.substratum.fragments.ProfileFragment.DAY;
import static projekt.substratum.fragments.ProfileFragment.DAY_PROFILE_HOUR;
import static projekt.substratum.fragments.ProfileFragment.NIGHT;
import static projekt.substratum.fragments.ProfileFragment.NIGHT_PROFILE_HOUR;
import static projekt.substratum.fragments.ProfileFragment.NIGHT_PROFILE_MINUTE;
import static projekt.substratum.fragments.ProfileFragment.SCHEDULED_PROFILE_CURRENT_PROFILE;
import static projekt.substratum.fragments.ProfileFragment.SCHEDULED_PROFILE_ENABLED;
import static projekt.substratum.fragments.ProfileFragment.SCHEDULED_PROFILE_TYPE_EXTRA;

public class BootCompletedDetector extends BroadcastReceiver {

    private SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent pushIntent = new Intent(context, ThemeService.class);
            context.startService(pushIntent);

            prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getBoolean(SCHEDULED_PROFILE_ENABLED, false))
                setupScheduledProfile(context);
        }
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

        Calendar calendarDay = Calendar.getInstance();
        calendarDay.setTimeInMillis(System.currentTimeMillis());
        calendarDay.set(Calendar.HOUR_OF_DAY, dayHour);
        calendarDay.set(Calendar.MINUTE, dayMinute);

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
