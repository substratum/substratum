package projekt.substratum.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import java.util.Calendar;

import projekt.substratum.R;

public class BootCompletedDetector extends BroadcastReceiver {

    private SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent pushIntent = new Intent(context, ThemeService.class);
            context.startService(pushIntent);

            prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getBoolean("scheduled_profile_enabled", false))
                setupScheduledProfile(context);
        }
    }

    private void setupScheduledProfile(Context context) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ScheduledProfileReceiver.class);
        intent.putExtra("type", context.getString(R.string.night));
        PendingIntent nightIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra("type", R.string.day);
        PendingIntent dayIntent = PendingIntent.getBroadcast(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final int dayHour = prefs.getInt("day_profile_hour", 0);
        final int dayMinute = prefs.getInt("night_profile_minute", 0);
        final int nightHour = prefs.getInt("night_profile_hour", 0);
        final int nightMinute = prefs.getInt("night_profile_minute", 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        // night time
        calendar.set(Calendar.HOUR_OF_DAY, nightHour);
        calendar.set(Calendar.MINUTE, nightMinute);
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, nightIntent);

        // day time
        calendar.set(Calendar.HOUR_OF_DAY, dayHour);
        calendar.set(Calendar.MINUTE, dayMinute);
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, dayIntent);
    }
}
