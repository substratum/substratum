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
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.platform.ThemeManager;

import static android.content.Context.NOTIFICATION_SERVICE;
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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences
                (context);
        boolean enabled = sharedPreferences.getBoolean("crash_receiver", true);
        if (!enabled) return;


        String packageName =
                intent.getStringExtra(CRASH_PACKAGE_NAME);
        boolean repeating =
                intent.getBooleanExtra(CRASH_REPEATING, false);

        if (repeating) {
            Log.e(TAG, "\'" + packageName + "\' is repeatedly stopping...");
            Log.e(TAG, "Now disabling all overlays for \'" + packageName + "\'...");

            List<String> overlays = ThemeManager.listEnabledOverlaysForTarget(context, packageName);
            if (overlays.size() > 0) {
                for (String overlay : overlays) {
                    Log.d("AppCrashReciever", String.format("Disabling overlay %s for package " +
                            "%s", overlay, packageName));
                }

                postNotificationAndDisableOverlays(context,
                        getApplicationLabel(context, packageName),
                        overlays);

            }
        } else if (Objects.equals(packageName, "com.android.systemui")) {
            switch (sharedPreferences.getInt("sysui_crash_count", 0)) {
                case 0:
                case 1:
                    Log.d("AppCrashReceiver",
                            String.format("SystemUI crash count %s",
                                    sharedPreferences.getInt("sysui_crash_count", 1)));
                    sharedPreferences.edit().putInt("sysui_crash_count",
                            sharedPreferences.getInt("sysui_crash_count",
                                    0) + 1).apply();
                    break;
                case 2:
                    Log.d("AppCrashReceiver", "Disabling all SystemUI overlays now.");
                    sharedPreferences.edit().remove("sysui_crash_count").apply();
                    postNotificationAndDisableOverlays(context,
                            getApplicationLabel(context, packageName),
                            ThemeManager.listEnabledOverlaysForTarget(context, "com.android" +
                                    ".systemui"));
                    break;
                default:
                    sharedPreferences.edit().remove("sysui_crash_count").apply();
                    break;
            }
        } else {
            Log.e(TAG, packageName + " stopped unexpectedly...");
        }
    }

    private void postNotificationAndDisableOverlays(Context context, String packageName,
                                                    List<String> overlays) {
        String app_crash_title =
                String.format(context.getString(R.string.app_crash_title), packageName);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                References.DEFAULT_NOTIFICATION_CHANNEL_ID);
        builder.setSmallIcon(R.drawable.notification_overlay_corruption);
        builder.setContentTitle(app_crash_title);
        builder.setContentText(context.getString(R.string.app_crash_content));
        builder.setOngoing(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (mNotifyMgr != null) {
            mNotifyMgr.notify(NOTIFICATION_ID, builder.build());
        }

        ThemeManager.disableOverlay(context, new ArrayList<>(overlays));
    }

    private String getApplicationLabel(Context context, String packageName) {
        try {
            ApplicationInfo applicationInfo = context.getPackageManager()
                    .getApplicationInfo(packageName, 0);
            packageName = context.getPackageManager()
                    .getApplicationLabel(applicationInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            // Suppress warning
        }
        return packageName;
    }
}