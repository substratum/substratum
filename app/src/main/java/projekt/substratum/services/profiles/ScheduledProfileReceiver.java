/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
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
import androidx.core.app.NotificationCompat;
import projekt.substratum.R;
import projekt.substratum.Substratum;
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
                Substratum.log(TAG, extra + " profile will be applied.");
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
                Substratum.log(TAG, extra + " profile will be applied after screen off...");
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