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

import static projekt.substratum.fragments.ProfileFragment.SCHEDULED_PROFILE_TYPE_EXTRA;

public class ScheduledProfileReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String TAG = "ScheduledProfile";
        SharedPreferences prefs = context.getSharedPreferences("substratum_state",
                Context.MODE_PRIVATE);
        String extra = intent.getStringExtra(SCHEDULED_PROFILE_TYPE_EXTRA);
        if (extra == null) {
            extra = prefs.getString(SCHEDULED_PROFILE_TYPE_EXTRA, null);
        }

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (!powerManager.isInteractive()) {
            Log.d(TAG, extra + " profile will be applied.");
            prefs.edit().remove(SCHEDULED_PROFILE_TYPE_EXTRA).apply();
            BinderService.unregisterProfileScreenOffReceiver(context);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(SCHEDULED_PROFILE_TYPE_EXTRA, extra);

            ComponentName serviceComponent = new ComponentName(context,
                    ScheduledProfileService.class);
            JobInfo jobInfo = new JobInfo.Builder(1023, serviceComponent)
                    .setMinimumLatency(5000)
                    .setExtras(bundle)
                    .build();

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(jobInfo);
        } else {
            Log.d(TAG, extra + " profile will be applied after screen off...");
            NotificationManager mNotifyManager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
            mBuilder.setContentTitle(
                    String.format(context.getString(R.string.profile_notification_title), extra))
                    .setSmallIcon(R.drawable.ic_substratum)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setContentText(context.getString(R.string.profile_pending_notification))
                    .setOngoing(true);
            mNotifyManager.notify(1023, mBuilder.build());

            prefs.edit().putString(SCHEDULED_PROFILE_TYPE_EXTRA, extra).apply();
            BinderService.registerProfileScreenOffReceiver(context);
        }
    }
}
