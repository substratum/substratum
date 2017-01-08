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
import projekt.substratum.config.References;

import static projekt.substratum.fragments.ProfileFragment.DAY_PROFILE_HOUR;
import static projekt.substratum.fragments.ProfileFragment.NIGHT_PROFILE_HOUR;
import static projekt.substratum.fragments.ProfileFragment.NIGHT_PROFILE_MINUTE;
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
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, context.getString(R.string.night));
        PendingIntent nightIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, R.string.day);
        PendingIntent dayIntent = PendingIntent.getBroadcast(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final int dayHour = prefs.getInt(DAY_PROFILE_HOUR, 0);
        final int dayMinute = prefs.getInt(NIGHT_PROFILE_MINUTE, 0);
        final int nightHour = prefs.getInt(NIGHT_PROFILE_HOUR, 0);
        final int nightMinute = prefs.getInt(NIGHT_PROFILE_MINUTE, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        // night time
        calendar.set(Calendar.HOUR_OF_DAY, nightHour);
        calendar.set(Calendar.MINUTE, nightMinute);
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                nightIntent);

        // day time
        calendar.set(Calendar.HOUR_OF_DAY, dayHour);
        calendar.set(Calendar.MINUTE, dayMinute);
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                dayIntent);
    }
}
