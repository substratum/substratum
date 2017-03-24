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

package projekt.substratum.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ThreadLocalRandom;

import projekt.substratum.InformationActivity;
import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.config.References;

public class SubstratumThemeUpdater {

    private Context mContext;
    private SharedPreferences prefs;
    private String packageName;
    private boolean showNotification;

    public void initialize(Context context, String packageName, boolean notification) {
        this.mContext = context;
        this.packageName = packageName;
        this.showNotification = notification;

        prefs = context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("is_updating", true).apply();

        new SubstratumThemeUpdate().execute("");
    }

    private String getThemeName(String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (References.checkOMS(mContext)) {
                    if (appInfo.metaData.getString(References.metadataName) != null) {
                        if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                            return appInfo.metaData.getString(References.metadataName);
                        }
                    }
                } else {
                    if (appInfo.metaData.getBoolean(References.metadataLegacy, false)) {
                        if (appInfo.metaData.getString(References.metadataName) != null) {
                            if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                                return appInfo.metaData.getString(References.metadataName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(References.SUBSTRATUM_LOG, "Unable to find package identifier (INDEX OUT OF " +
                    "BOUNDS)");
        }
        return null;
    }

    private class SubstratumThemeUpdate extends AsyncTask<String, Integer, String> {

        Boolean success = false;

        @Override
        protected void onPostExecute(String result) {

            final int id = ThreadLocalRandom.current().nextInt(0, 1000);
            final int notification_priority = 2; // PRIORITY_MAX == 2

            if (showNotification && success) {
                Intent notificationIntent;
                PendingIntent intent;
                try {
                    Intent myIntent = References.sendLaunchIntent(mContext, packageName, false,
                            null, true);
                    if (myIntent != null) {
                        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext)
                                .addParentStack(InformationActivity.class)
                                .addNextIntent(myIntent);
                        intent = stackBuilder.getPendingIntent(0, PendingIntent
                                .FLAG_CANCEL_CURRENT);
                    } else {
                        notificationIntent = new Intent(mContext, MainActivity.class);
                        intent = PendingIntent.getActivity(mContext, 0, notificationIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT);
                    }

                    // This is the time when the notification should be shown on the user's screen
                    NotificationManager mNotifyManager =
                            (NotificationManager) mContext.getSystemService(
                                    Context.NOTIFICATION_SERVICE);
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder
                            (mContext);
                    mBuilder.setContentTitle(getThemeName(packageName) + " " + mContext.getString(
                            R.string.notification_theme_updated))
                            .setContentText(mContext.getString(R.string
                                    .notification_theme_updated_content))
                            .setAutoCancel(true)
                            .setContentIntent(intent)
                            .setSmallIcon(R.drawable.notification_updated)
                            .setLargeIcon(BitmapFactory.decodeResource(
                                    mContext.getResources(), R.mipmap
                                            .restore_launcher))
                            .setPriority(notification_priority);
                    mNotifyManager.notify(id, mBuilder.build());
                } catch (Exception ex) {
                    Toast toast = Toast.makeText(mContext,
                            mContext.getString(R.string
                                    .information_activity_upgrade_toast),
                            Toast.LENGTH_LONG);
                    toast.show();
                }
            } else if (success) {
                Intent notificationIntent;
                PendingIntent intent;
                try {
                    Intent myIntent = References.sendLaunchIntent(mContext, packageName, false,
                            null, true);
                    if (myIntent != null) {
                        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext)
                                .addParentStack(InformationActivity.class)
                                .addNextIntent(myIntent);
                        intent = stackBuilder.getPendingIntent(0, PendingIntent
                                .FLAG_CANCEL_CURRENT);
                    } else {
                        notificationIntent = new Intent(mContext, MainActivity.class);
                        intent = PendingIntent.getActivity(mContext, 0, notificationIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT);
                    }

                    // This is the time when the notification should be shown on the user's screen
                    NotificationManager mNotifyManager =
                            (NotificationManager) mContext.getSystemService(
                                    Context.NOTIFICATION_SERVICE);
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder
                            (mContext);
                    mBuilder.setContentTitle(getThemeName(packageName) + " " + mContext.getString(
                            R.string.notification_theme_installed))
                            .setContentText(mContext.getString(R.string
                                    .notification_theme_installed_content))
                            .setAutoCancel(true)
                            .setContentIntent(intent)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setLargeIcon(BitmapFactory.decodeResource(
                                    mContext.getResources(), R.mipmap
                                            .main_launcher))
                            .setPriority(notification_priority);
                    mNotifyManager.notify(id, mBuilder.build());
                } catch (Exception ex) {
                    Toast toast = Toast.makeText(mContext,
                            mContext.getString(R.string
                                    .information_activity_upgrade_toast),
                            Toast.LENGTH_LONG);
                    toast.show();
                }
            } else {
                Log.d("SubstratumCacher",
                        "Process was interrupted by the user, rolling back changes...");
            }
            prefs.edit().putBoolean("is_updating", false).apply();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            success = !References.isCachingEnabled(mContext) ||
                    new CacheCreator().initializeCache(mContext, packageName);
            return null;
        }
    }
}