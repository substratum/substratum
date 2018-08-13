/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.services.crash;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;
import projekt.substratum.common.platform.ThemeManager;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;
import static projekt.substratum.common.References.CRASH_PACKAGE_NAME;
import static projekt.substratum.common.References.CRASH_REPEATING;
import static projekt.substratum.common.Resources.SYSTEMUI;

public class AppCrashReceiver extends BroadcastReceiver {

    private static final String TAG = "AppCrashReceiver";
    private static final int NOTIFICATION_ID = 2476;

    private static void postNotificationAndDisableOverlays(Context context,
                                                           String packageName,
                                                           List<String> overlays) {
        String appCrashTitle =
                String.format(context.getString(R.string.app_crash_title), packageName);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                References.DEFAULT_NOTIFICATION_CHANNEL_ID);
        builder.setSmallIcon(R.drawable.notification_overlay_corruption);
        builder.setContentTitle(appCrashTitle);
        builder.setContentText(context.getString(R.string.app_crash_content));
        builder.setOngoing(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null)
            notificationManager.notify(NOTIFICATION_ID, builder.build());

        ThemeManager.disableOverlay(context, new ArrayList<>(overlays));
    }

    private static String getApplicationLabel(Context context, String packageName) {
        try {
            ApplicationInfo applicationInfo = context.getPackageManager()
                    .getApplicationInfo(packageName, 0);
            packageName = context.getPackageManager()
                    .getApplicationLabel(applicationInfo).toString();
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return packageName;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = Substratum.getPreferences();
        boolean enabled = sharedPreferences.getBoolean("crash_receiver", true);
        if (!enabled) return;

        String packageName = intent.getStringExtra(CRASH_PACKAGE_NAME);
        boolean repeating = intent.getBooleanExtra(CRASH_REPEATING, false);
        if (repeating) {
            Log.e(TAG, '\'' + packageName + "\' is repeatedly stopping...");
            Log.e(TAG, "Now disabling all overlays for \'" + packageName + "\'...");

            List<String> overlays = ThemeManager.listEnabledOverlaysForTarget(context,
                    packageName);
            if (!overlays.isEmpty()) {
                for (String overlay : overlays) {
                    Substratum.log("AppCrashReciever", String.format("Disabling overlay %s for package " +
                            "%s", overlay, packageName));
                }

                AppCrashReceiver.postNotificationAndDisableOverlays(context,
                        AppCrashReceiver.getApplicationLabel(context, packageName),
                        overlays);

            }
        } else if (SYSTEMUI.equals(packageName)) {
            if (ThemeManager.listEnabledOverlaysForTarget(context, SYSTEMUI).isEmpty()) return;
            switch (sharedPreferences.getInt("sysui_crash_count", 0)) {
                case 0:
                case 1:
                    Substratum.log("AppCrashReceiver",
                            String.format("SystemUI crash count %s",
                                    sharedPreferences.getInt("sysui_crash_count", 1)));
                    sharedPreferences.edit().putInt("sysui_crash_count",
                            sharedPreferences.getInt("sysui_crash_count",
                                    0) + 1).apply();
                    break;
                case 2:
                    Substratum.log("AppCrashReceiver", "Disabling all SystemUI overlays now.");
                    sharedPreferences.edit().remove("sysui_crash_count").apply();
                    AppCrashReceiver.postNotificationAndDisableOverlays(context,
                            AppCrashReceiver.getApplicationLabel(context, packageName),
                            ThemeManager.listEnabledOverlaysForTarget(context, SYSTEMUI));
                    break;
                default:
                    sharedPreferences.edit().remove("sysui_crash_count").apply();
                    break;
            }
        } else {
            Log.e(TAG, packageName + " stopped unexpectedly...");
        }
    }
}