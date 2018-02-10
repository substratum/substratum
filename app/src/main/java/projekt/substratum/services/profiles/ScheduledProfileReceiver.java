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

package projekt.substratum.services.profiles;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import projekt.substratum.R;
import projekt.substratum.common.Broadcasts;

import static projekt.substratum.common.systems.ProfileManager.SCHEDULED_PROFILE_TYPE_EXTRA;


public class ScheduledProfileReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 1023;
    private static final String TAG = "ScheduledProfile";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs =
                context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
        String extra = intent.getStringExtra(SCHEDULED_PROFILE_TYPE_EXTRA);
        if (extra == null) {
            extra = prefs.getString(SCHEDULED_PROFILE_TYPE_EXTRA, null);
        }

        PowerManager powerManager = (PowerManager) context.getSystemService(Context
                .POWER_SERVICE);
        if (powerManager != null) {
            if (!powerManager.isInteractive()) {
                Log.d(TAG, extra + " profile will be applied.");
                prefs.edit().remove(SCHEDULED_PROFILE_TYPE_EXTRA).apply();
                Broadcasts.unregisterProfileScreenOffReceiver(context.getApplicationContext());

                PersistableBundle bundle = new PersistableBundle();
                bundle.putString(SCHEDULED_PROFILE_TYPE_EXTRA, extra);

                ComponentName serviceComponent = new ComponentName(context,
                        ScheduledProfileService.class);
                JobInfo jobInfo = new JobInfo.Builder(NOTIFICATION_ID, serviceComponent)
                        .setMinimumLatency(5000L)
                        .setExtras(bundle)
                        .build();

                JobScheduler jobScheduler = (JobScheduler) context.getSystemService(
                        Context.JOB_SCHEDULER_SERVICE);
                if (jobScheduler != null) {
                    jobScheduler.schedule(jobInfo);
                }
            } else {
                Log.d(TAG, extra + " profile will be applied after screen off...");
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context
                                .NOTIFICATION_SERVICE);
                @SuppressWarnings("deprecation") NotificationCompat.Builder builder = new
                        NotificationCompat.Builder(context);
                builder.setContentTitle(
                        String.format(context.getString(R.string.profile_notification_title),
                                extra))
                        .setSmallIcon(R.drawable.ic_substratum)
                        .setPriority(Notification.PRIORITY_DEFAULT)
                        .setContentText(context.getString(R.string.profile_pending_notification))
                        .setOngoing(true);
                if (notificationManager != null) {
                    notificationManager.notify(NOTIFICATION_ID, builder.build());
                }

                prefs.edit().putString(SCHEDULED_PROFILE_TYPE_EXTRA, extra).apply();
                Broadcasts.registerProfileScreenOffReceiver(context.getApplicationContext());
            }
        }
    }
}