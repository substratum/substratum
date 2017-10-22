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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

import projekt.substratum.R;
import projekt.substratum.activities.launch.AppShortcutLaunch;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.util.helpers.NotificationCreator;

import static projekt.substratum.common.References.PACKAGE_ADDED;
import static projekt.substratum.common.References.PACKAGE_FULLY_REMOVED;

public class PackageModificationDetector extends BroadcastReceiver {

    private final static String TAG = "SubstratumDetector";
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.mContext = context;

        Uri packageName = intent.getData();
        String package_name;
        if (packageName != null) {
            package_name = packageName.toString().substring(8);
        } else {
            return;
        }

        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case PACKAGE_ADDED:
                    Broadcasts.sendOverlayRefreshMessage(this.mContext);
                    if (Systems.isSamsungDevice(context)) {
                        Broadcasts.sendRefreshManagerMessage(this.mContext);
                    }
                    break;
                case PACKAGE_FULLY_REMOVED:
                    Broadcasts.sendRefreshManagerMessage(this.mContext);
                    if (Systems.isSamsungDevice(context)) {
                        Broadcasts.sendOverlayRefreshMessage(this.mContext);
                    }
                    return;
            }
        }

        try {
            ApplicationInfo appInfo = this.mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                // First, check if the app installed is actually a substratum overlay
                String check_overlay_parent =
                        appInfo.metaData.getString(References.metadataOverlayParent);
                String check_overlay_target =
                        appInfo.metaData.getString(References.metadataOverlayTarget);
                if (check_overlay_parent != null && check_overlay_target != null) {
                    Broadcasts.sendOverlayRefreshMessage(this.mContext);
                    Broadcasts.sendRefreshManagerMessage(this.mContext);
                    return;
                }

                // Then, check if the app installed is actually a substratum theme
                String check_theme_name =
                        appInfo.metaData.getString(References.metadataName);
                String check_theme_author =
                        appInfo.metaData.getString(References.metadataAuthor);
                if (check_theme_name == null && check_theme_author == null) return;
            } else {
                return;
            }
        } catch (Exception e) {
            return;
        }

        // When it is a proper theme, then we can continue
        Boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

        // Let's add it to the list of installed themes on shared prefs
        SharedPreferences mainPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
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

        // Legacy check to see if an OMS theme is guarded from being installed on legacy
        if (!Systems.checkOMS(this.mContext)) {
            try {
                ApplicationInfo appInfo = this.mContext.getPackageManager().getApplicationInfo(
                        package_name, PackageManager.GET_META_DATA);
                if (appInfo.metaData != null) {
                    Boolean check_legacy =
                            appInfo.metaData.getBoolean(References.metadataLegacy);
                    if (!check_legacy) {
                        Log.e(TAG, "Device is non-OMS, while an " +
                                "OMS theme is installed, aborting operation!");

                        String parse = String.format(this.mContext.getString(
                                R.string.failed_to_install_text_notification),
                                appInfo.metaData.getString(
                                        References.metadataName));

                        Intent showIntent = new Intent();
                        PendingIntent contentIntent = PendingIntent.getActivity(
                                this.mContext, 0, showIntent, 0);

                        new NotificationCreator(
                                this.mContext,
                                this.mContext.getString(
                                        R.string.failed_to_install_title_notification),
                                parse,
                                true,
                                contentIntent,
                                R.drawable.notification_warning_icon,
                                null,
                                Notification.PRIORITY_MAX,
                                References.notification_id).createNotification();

                        Packages.uninstallPackage(this.mContext, package_name);
                        return;
                    }
                }
            } catch (Exception e) {
                // Suppress warning
            }
        }

        if (replacing) {
            // We need to check if this is a new install or not
            Log.d(TAG, "'" + package_name + "' has been updated.");
            Bitmap bitmap = Packages.getBitmapFromDrawable(
                    Packages.getAppIcon(this.mContext, package_name));

            new NotificationCreator(
                    this.mContext,
                    Packages.getPackageName(this.mContext, package_name) + " " + this.mContext.getString(
                            R.string.notification_theme_updated),
                    this.mContext.getString(R.string.notification_theme_updated_content),
                    true,
                    getPendingIntent(package_name),
                    R.drawable.notification_updated,
                    bitmap,
                    Notification.PRIORITY_MAX,
                    ThreadLocalRandom.current().nextInt(0, 1000)).createNotification();
        } else {
            new NotificationCreator(
                    this.mContext,
                    Packages.getPackageName(this.mContext, package_name) + " " + this.mContext.getString(
                            R.string.notification_theme_installed),
                    this.mContext.getString(R.string.notification_theme_installed_content),
                    true,
                    getPendingIntent(package_name),
                    R.drawable.notification_icon,
                    BitmapFactory.decodeResource(
                            this.mContext.getResources(), R.mipmap.main_launcher),
                    Notification.PRIORITY_MAX,
                    ThreadLocalRandom.current().nextInt(0, 1000)).createNotification();
        }
        Broadcasts.sendRefreshMessage(this.mContext);
        Broadcasts.sendActivityFinisherMessage(this.mContext, package_name);
    }

    public PendingIntent getPendingIntent(String package_name) {
        Intent myIntent = new Intent(this.mContext, AppShortcutLaunch.class);
        myIntent.putExtra("theme_pid", package_name);
        return PendingIntent.getActivity(this.mContext, 0, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }
}