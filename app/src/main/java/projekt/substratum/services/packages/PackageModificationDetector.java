/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.services.packages;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.activities.shortcuts.AppShortcutLaunch;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.services.notification.UnsupportedThemeReceiver;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

import static projekt.substratum.common.Internal.THEME_PID;
import static projekt.substratum.common.References.ANDROMEDA_PACKAGE;
import static projekt.substratum.common.References.SST_ADDON_PACKAGE;
import static projekt.substratum.common.References.metadataSamsungSupport;

public class PackageModificationDetector extends BroadcastReceiver {

    private static final String TAG = "SubstratumDetector";

    static PendingIntent getPendingIntent(Context context, String packageName) {
        Intent myIntent = new Intent(context, AppShortcutLaunch.class);
        myIntent.putExtra(THEME_PID, packageName);
        return PendingIntent.getActivity(
                context,
                ThreadLocalRandom.current().nextInt(0, 10000),
                myIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Uri packageName = intent.getData();
        String packageName1;
        if (packageName != null) {
            packageName1 = packageName.toString().substring(8);
        } else {
            return;
        }

        if (packageName1.equals(SST_ADDON_PACKAGE)) {
            Broadcasts.sendKillMessage(context);
            return;
        } else if (packageName1.equals(ANDROMEDA_PACKAGE)) {
            System.exit(0);
        }

        if (Systems.isSamsungDevice(context) || MainActivity.instanceBasedAndromedaFailure) {
            Broadcasts.sendOverlayRefreshMessage(context);
            Broadcasts.sendRefreshManagerMessage(context);
        }

        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    packageName1, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                // First, check if the app installed is actually a substratum overlay
                String checkOverlayParent =
                        appInfo.metaData.getString(References.metadataOverlayParent);
                String checkOverlayTarget =
                        appInfo.metaData.getString(References.metadataOverlayTarget);
                if ((checkOverlayParent != null) && (checkOverlayTarget != null)) {
                    Broadcasts.sendOverlayRefreshMessage(context);
                    Broadcasts.sendRefreshManagerMessage(context);
                    return;
                }

                // Then, check if the app installed is actually a substratum theme
                String checkThemeName =
                        appInfo.metaData.getString(References.metadataName);
                String checkThemeAuthor =
                        appInfo.metaData.getString(References.metadataAuthor);
                if ((checkThemeName == null) && (checkThemeAuthor == null)) return;
            } else {
                return;
            }
        } catch (Exception e) {
            return;
        }

        // When it is a proper theme, then we can continue
        boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

        // Let's add it to the list of installed themes on shared prefs
        SharedPreferences mainPrefs = Substratum.getPreferences();
        Set<String> installedThemes =
                mainPrefs.getStringSet("installed_themes", new HashSet<>());
        Set<String> installedSorted = new TreeSet<>();

        int beginningSize = installedThemes.size();
        if (!installedThemes.contains(packageName1)) {
            installedThemes.add(packageName1);
            installedSorted.addAll(installedThemes);
        }
        if (installedThemes.size() > beginningSize) {
            mainPrefs.edit().putStringSet("installed_themes", installedSorted).apply();
        }

        try {
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(packageName1, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                // Legacy check to see if an OMS theme is guarded from being installed on legacy
                boolean checkLegacy = appInfo.metaData.getBoolean(References.metadataLegacy);
                if (!Systems.checkOMS(context) && !checkLegacy) {
                    Log.e(TAG, "Device is non-OMS, while an " +
                            "OMS theme is installed, aborting operation!");

                    String parse = String.format(context.getString(
                            R.string.failed_to_install_text_notification),
                            appInfo.metaData.getString(
                                    References.metadataName));

                    // Jot the notification id
                    int notificationId = ThreadLocalRandom.current().nextInt(0, 10000);

                    // Create an Intent for the BroadcastReceiver
                    Intent buttonIntent = new Intent(context, UnsupportedThemeReceiver.class);
                    buttonIntent.putExtra("package_to_uninstall", packageName1);
                    buttonIntent.putExtra("notification_to_close", notificationId);

                    // Create the PendingIntent
                    PendingIntent btPendingIntent =
                            PendingIntent.getBroadcast(
                                    context,
                                    notificationId,
                                    buttonIntent,
                                    PendingIntent.FLAG_CANCEL_CURRENT
                            );

                    NotificationManager notificationManager = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
                    NotificationCompat.Builder builder = new
                            NotificationCompat.Builder(context,
                            References.DEFAULT_NOTIFICATION_CHANNEL_ID)
                            .setContentTitle(context.getString(
                                    R.string.failed_to_install_title_notification))
                            .setContentText(parse)
                            .setAutoCancel(true)
                            .setContentIntent(btPendingIntent)
                            .addAction(android.R.color.transparent,
                                    context.getString(R.string.refused_to_install_notification_button), btPendingIntent)
                            .setSmallIcon(R.drawable.notification_warning_icon)
                            .setPriority(Notification.PRIORITY_MAX);
                    if (notificationManager != null) {
                        notificationManager.notify(notificationId, builder.build());
                    }
                    return;
                }

                // Samsung check to see if an intentionally disable substratum theme was installed
                // on a Samsung device
                boolean samsungSupport = appInfo.metaData.getBoolean(metadataSamsungSupport, true);
                if (!samsungSupport &&
                        (Systems.isSamsungDevice(context) || Systems.isNewSamsungDevice())) {
                    Log.e(TAG, "Theme does not support Samsung, yet the theme was installed, " +
                            "aborting operation!");

                    String parse = String.format(context.getString(
                            R.string.refused_to_install_text_notification),
                            appInfo.metaData.getString(
                                    References.metadataName));

                    // Jot the notification id
                    int notificationId = ThreadLocalRandom.current().nextInt(0, 10000);

                    // Create an Intent for the BroadcastReceiver
                    Intent buttonIntent = new Intent(context, UnsupportedThemeReceiver.class);
                    buttonIntent.putExtra("package_to_uninstall", packageName1);
                    buttonIntent.putExtra("notification_to_close", notificationId);

                    // Create the PendingIntent
                    PendingIntent btPendingIntent =
                            PendingIntent.getBroadcast(context, 0, buttonIntent, 0);

                    NotificationManager notificationManager = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
                    NotificationCompat.Builder builder = new
                            NotificationCompat.Builder(context,
                            References.DEFAULT_NOTIFICATION_CHANNEL_ID)
                            .setContentTitle(context.getString(
                                    R.string.refused_to_install_title_notification))
                            .setContentText(parse)
                            .setAutoCancel(true)
                            .setContentIntent(btPendingIntent)
                            .addAction(android.R.color.transparent,
                                    context.getString(R.string.refused_to_install_notification_button), btPendingIntent)
                            .setSmallIcon(R.drawable.notification_warning_icon)
                            .setPriority(Notification.PRIORITY_MAX);
                    if (notificationManager != null) {
                        notificationManager.notify(notificationId, builder.build());
                    }
                    return;
                }
            }
        } catch (Exception ignored) {
        }

        if (replacing) {
            // We need to check if this is a new install or not
            Substratum.log(TAG, '\'' + packageName1 + "' has been updated.");
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new
                    NotificationCompat.Builder(context,
                    References.DEFAULT_NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(
                            Packages.getPackageName(context,
                                    packageName1) + ' ' + context.getString(R.string.notification_theme_updated))
                    .setContentText(context.getString(R.string.notification_theme_updated_content))
                    .setAutoCancel(true)
                    .setContentIntent(getPendingIntent(context, packageName1))
                    .setSmallIcon(R.drawable.notification_updated)
                    .setLargeIcon(Packages.getBitmapFromDrawable(
                            Packages.getAppIcon(context, packageName1)))
                    .setPriority(Notification.PRIORITY_MAX);
            if (notificationManager != null) {
                notificationManager.notify(
                        ThreadLocalRandom.current().nextInt(0, 1000), builder.build());
            }
        } else {
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new
                    NotificationCompat.Builder(context,
                    References.DEFAULT_NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(
                            Packages.getPackageName(context, packageName1) + ' ' +
                                    context.getString(
                                            R.string.notification_theme_installed))
                    .setContentText(
                            context.getString(
                                    R.string.notification_theme_installed_content))
                    .setAutoCancel(true)
                    .setContentIntent(getPendingIntent(context, packageName1))
                    .setAutoCancel(true)
                    .setContentIntent(getPendingIntent(context, packageName1))
                    .setSmallIcon(R.drawable.notification_icon)
                    .setLargeIcon(BitmapFactory.decodeResource(
                            context.getResources(),
                            R.mipmap.main_launcher))
                    .setPriority(Notification.PRIORITY_MAX);
            if (notificationManager != null) {
                notificationManager.notify(
                        ThreadLocalRandom.current().nextInt(0, 1000), builder.build());
            }
        }
        Broadcasts.sendRefreshMessage(context);
        Broadcasts.sendActivityFinisherMessage(context, packageName1);
    }
}