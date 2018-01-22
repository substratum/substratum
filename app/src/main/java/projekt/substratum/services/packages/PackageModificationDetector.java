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
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

import projekt.substratum.R;
import projekt.substratum.activities.shortcuts.AppShortcutLaunch;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.services.notification.UnsupportedThemeReceiver;

import static projekt.substratum.common.Internal.THEME_PID;
import static projekt.substratum.common.References.SST_ADDON_PACKAGE;
import static projekt.substratum.common.References.metadataSamsungSupport;

public class PackageModificationDetector extends BroadcastReceiver {

    private static final String TAG = "SubstratumDetector";
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        Uri packageName = intent.getData();
        String package_name;
        if (packageName != null) {
            package_name = packageName.toString().substring(8);
        } else {
            return;
        }

        if (package_name.equals(SST_ADDON_PACKAGE)) {
            Broadcasts.sendKillMessage(context);
            return;
        }

        if (Systems.isSamsungDevice(context)) {
            Broadcasts.sendOverlayRefreshMessage(context);
            Broadcasts.sendRefreshManagerMessage(context);
        }

        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                // First, check if the app installed is actually a substratum overlay
                String check_overlay_parent =
                        appInfo.metaData.getString(References.metadataOverlayParent);
                String check_overlay_target =
                        appInfo.metaData.getString(References.metadataOverlayTarget);
                if ((check_overlay_parent != null) && (check_overlay_target != null)) {
                    Broadcasts.sendOverlayRefreshMessage(context);
                    Broadcasts.sendRefreshManagerMessage(context);
                    return;
                }

                // Then, check if the app installed is actually a substratum theme
                String check_theme_name =
                        appInfo.metaData.getString(References.metadataName);
                String check_theme_author =
                        appInfo.metaData.getString(References.metadataAuthor);
                if ((check_theme_name == null) && (check_theme_author == null)) return;
            } else {
                return;
            }
        } catch (Exception e) {
            return;
        }

        // When it is a proper theme, then we can continue
        Boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

        // Let's add it to the list of installed themes on shared prefs
        SharedPreferences mainPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> installed_themes =
                mainPrefs.getStringSet("installed_themes", new HashSet<>());
        Set<String> installed_sorted = new TreeSet<>();

        int beginning_size = installed_themes.size();
        if (!installed_themes.contains(package_name)) {
            installed_themes.add(package_name);
            installed_sorted.addAll(installed_themes);
        }
        if (installed_themes.size() > beginning_size) {
            mainPrefs.edit().putStringSet("installed_themes", installed_sorted).apply();
        }

        try {
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                // Legacy check to see if an OMS theme is guarded from being installed on legacy
                Boolean check_legacy = appInfo.metaData.getBoolean(References.metadataLegacy);
                if (!Systems.checkOMS(context) && !check_legacy) {
                    Log.e(TAG, "Device is non-OMS, while an " +
                            "OMS theme is installed, aborting operation!");

                    String parse = String.format(context.getString(
                            R.string.failed_to_install_text_notification),
                            appInfo.metaData.getString(
                                    References.metadataName));

                    // Create an Intent for the BroadcastReceiver
                    Intent buttonIntent = new Intent(context, UnsupportedThemeReceiver.class);
                    buttonIntent.putExtra("package_to_uninstall", package_name);

                    // Create the PendingIntent
                    PendingIntent btPendingIntent =
                            PendingIntent.getBroadcast(context, 0, buttonIntent, 0);

                    NotificationManager mNotifyManager = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
                    NotificationCompat.Builder mBuilder = new
                            NotificationCompat.Builder(context,
                            References.DEFAULT_NOTIFICATION_CHANNEL_ID);
                    mBuilder.setContentTitle(context.getString(
                            R.string.failed_to_install_title_notification));
                    mBuilder.setContentText(parse);
                    mBuilder.setAutoCancel(true);
                    mBuilder.setContentIntent(btPendingIntent);
                    mBuilder.addAction(android.R.color.transparent,
                            context.getString(R.string.refused_to_install_notification_button),
                            btPendingIntent);
                    mBuilder.setSmallIcon(R.drawable.notification_warning_icon);
                    mBuilder.setPriority(Notification.PRIORITY_MAX);
                    if (mNotifyManager != null) {
                        mNotifyManager.notify(References.notification_id, mBuilder.build());
                    }

                    Packages.uninstallPackage(context, package_name);
                    return;
                }

                // Samsung check to see if an intentionally disable substratum theme was installed
                // on a Samsung device
                Boolean samsung_support = appInfo.metaData.getBoolean(metadataSamsungSupport, true);
                if (!samsung_support &&
                        (Systems.isSamsungDevice(context) || Systems.isNewSamsungDevice(context))) {
                    Log.e(TAG, "Theme does not support Samsung, yet the theme was installed, " +
                            "aborting operation!");

                    String parse = String.format(context.getString(
                            R.string.refused_to_install_text_notification),
                            appInfo.metaData.getString(
                                    References.metadataName));

                    // Create an Intent for the BroadcastReceiver
                    Intent buttonIntent = new Intent(context, UnsupportedThemeReceiver.class);
                    buttonIntent.putExtra("package_to_uninstall", package_name);

                    // Create the PendingIntent
                    PendingIntent btPendingIntent =
                            PendingIntent.getBroadcast(context, 0, buttonIntent, 0);

                    NotificationManager mNotifyManager = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
                    NotificationCompat.Builder mBuilder = new
                            NotificationCompat.Builder(context,
                            References.DEFAULT_NOTIFICATION_CHANNEL_ID);
                    mBuilder.setContentTitle(context.getString(
                            R.string.refused_to_install_title_notification));
                    mBuilder.setContentText(parse);
                    mBuilder.setAutoCancel(true);
                    mBuilder.setContentIntent(btPendingIntent);
                    mBuilder.addAction(android.R.color.transparent,
                            context.getString(R.string.refused_to_install_notification_button),
                            btPendingIntent);
                    mBuilder.setSmallIcon(R.drawable.notification_warning_icon);
                    mBuilder.setPriority(Notification.PRIORITY_MAX);
                    if (mNotifyManager != null) {
                        mNotifyManager.notify(References.notification_id, mBuilder.build());
                    }

                    Packages.uninstallPackage(context, package_name);
                    return;
                }
            }
        } catch (Exception e) {
            // Suppress warning
        }

        if (replacing) {
            // We need to check if this is a new install or not
            Log.d(TAG, '\'' + package_name + "' has been updated.");
            NotificationManager mNotifyManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new
                    NotificationCompat.Builder(context,
                    References.DEFAULT_NOTIFICATION_CHANNEL_ID);
            mBuilder.setContentTitle(
                    Packages.getPackageName(context, package_name) + ' ' +
                            context.getString(R.string.notification_theme_updated));
            mBuilder.setContentText(context.getString(R.string.notification_theme_updated_content));
            mBuilder.setAutoCancel(true);
            mBuilder.setContentIntent(getPendingIntent(package_name));
            mBuilder.setSmallIcon(R.drawable.notification_updated);
            mBuilder.setLargeIcon(Packages.getBitmapFromDrawable(
                    Packages.getAppIcon(context, package_name)));
            mBuilder.setPriority(Notification.PRIORITY_MAX);
            if (mNotifyManager != null) {
                mNotifyManager.notify(
                        ThreadLocalRandom.current().nextInt(0, 1000), mBuilder.build());
            }
        } else {
            NotificationManager mNotifyManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new
                    NotificationCompat.Builder(context,
                    References.DEFAULT_NOTIFICATION_CHANNEL_ID);
            mBuilder.setContentTitle(
                    Packages.getPackageName(context, package_name) + ' ' +
                            context.getString(R.string.notification_theme_installed));
            mBuilder.setContentText(
                    context.getString(R.string.notification_theme_installed_content));
            mBuilder.setAutoCancel(true);
            mBuilder.setContentIntent(getPendingIntent(package_name));
            mBuilder.setSmallIcon(R.drawable.notification_icon);
            mBuilder.setLargeIcon(BitmapFactory.decodeResource(
                    context.getResources(), R.mipmap.main_launcher));
            mBuilder.setPriority(Notification.PRIORITY_MAX);
            if (mNotifyManager != null) {
                mNotifyManager.notify(
                        ThreadLocalRandom.current().nextInt(0, 1000), mBuilder.build());
            }
        }
        Broadcasts.sendRefreshMessage(context);
        Broadcasts.sendActivityFinisherMessage(context, package_name);
    }

    private PendingIntent getPendingIntent(String package_name) {
        Intent myIntent = new Intent(context, AppShortcutLaunch.class);
        myIntent.putExtra(THEME_PID, package_name);
        return PendingIntent.getActivity(context, 0, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }
}