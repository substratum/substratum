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

package projekt.substratum.services.crash;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.platform.ThemeManager;

import static android.content.Context.NOTIFICATION_SERVICE;
import static projekt.substratum.common.References.CRASH_CLASS_NAME;
import static projekt.substratum.common.References.CRASH_PACKAGE_NAME;
import static projekt.substratum.common.References.CRASH_REPEATING;

public class AppCrashReceiver extends BroadcastReceiver {

    public final static String TAG = "AppCrashReceiver";
    public final static int NOTIFICATION_ID = 2476;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!References.checkOMS(context)) {
            return;
        }

        boolean enabled = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("crash_receiver", true);
        if (!enabled) return;

        String packageName =
                intent.getStringExtra(CRASH_PACKAGE_NAME);
        String exceptionClass =
                intent.getStringExtra(CRASH_CLASS_NAME);
        boolean repeating =
                intent.getBooleanExtra(CRASH_REPEATING, false);

        if (repeating) {
            Log.e(TAG, "\'" + packageName + "\' is repeatedly stopping...");
            Log.e(TAG, "Now disabling all overlays for \'" + packageName + "\'...");

            List<String> overlays = ThemeManager.listEnabledOverlaysForTarget(packageName);

            if (overlays.size() > 0) {
                try {
                    ApplicationInfo applicationInfo = context.getPackageManager()
                            .getApplicationInfo(packageName, 0);
                    packageName = context.getPackageManager()
                            .getApplicationLabel(applicationInfo).toString();
                } catch (PackageManager.NameNotFoundException e) {
                    // Suppress warning
                }
                String app_crash_title =
                        String.format(context.getString(R.string.app_crash_title), packageName);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                builder.setSmallIcon(R.drawable.notification_overlay_corruption);
                builder.setContentTitle(app_crash_title);
                builder.setContentText(context.getString(R.string.app_crash_content));
                builder.setOngoing(false);
                builder.setPriority(NotificationCompat.PRIORITY_MAX);
                builder.setCategory(NotificationCompat.CATEGORY_SERVICE);
                NotificationManager mNotifyMgr =
                        (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.notify(NOTIFICATION_ID, builder.build());

                ThemeManager.disableOverlay(context, new ArrayList<>(overlays));
            }
        } else {
            Log.e(TAG, packageName + " stopped unexpectedly...");
        }
    }
}