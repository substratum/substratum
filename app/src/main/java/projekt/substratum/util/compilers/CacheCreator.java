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

package projekt.substratum.util.compilers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.crypto.Cipher;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.services.notification.NotificationUpgradeReceiver;

import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;

public class CacheCreator {

    private Context mContext;
    private int id = References.notification_id_upgrade;
    private NotificationManager mNotifyManager;

    public boolean initializeCache(Context context, String package_identifier, Cipher cipher) {
        // Extract the files from the assets folder of the target theme
        mContext = context;
        try {
            return extractAsset(package_identifier, cipher);
        } catch (IOException ioe) {
            // Exception
        }
        return false;
    }

    private String getThemeName(String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString(References.metadataName) != null) {
                    if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                        return appInfo.metaData.getString(References.metadataName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(References.SUBSTRATUM_LOG,
                    "Unable to find package identifier (INDEX OUT OF BOUNDS)");
        }
        return null;
    }

    private String getThemeVersion(String package_name) {
        try {
            PackageInfo pinfo = mContext.getPackageManager().getPackageInfo(package_name, 0);
            return pinfo.versionName;
        } catch (Exception e) {
            // Exception
        }
        return null;
    }

    public boolean wipeCache(Context context, String package_identifier) {
        mContext = context;
        File myDir2 = new File(mContext.getCacheDir().getAbsoluteFile() +
                SUBSTRATUM_BUILDER_CACHE + package_identifier);
        if (myDir2.exists()) {
            FileOperations.delete(mContext, myDir2.getAbsolutePath());
            return myDir2.mkdir();
        }
        return false;
    }

    private boolean extractAsset(String package_identifier, Cipher cipher) throws IOException {
        // First, extract the APK as a zip so we don't have to access the APK multiple times
        try {
            File myDir = new File(mContext.getCacheDir().getAbsoluteFile() +
                    SUBSTRATUM_BUILDER_CACHE + package_identifier + "/assets");
            if (myDir.exists()) {
                FileOperations.delete(mContext, myDir.getAbsolutePath());
            }
            FileOperations.createNewFolder(mContext, myDir.getAbsolutePath());

            // Now count the amount of files needed to be extracted
            Resources themeResources = mContext.getPackageManager()
                    .getResourcesForApplication(package_identifier);
            assert themeResources != null;
            AssetManager themeAssetManager = themeResources.getAssets();
            String[] assets = themeAssetManager.list("");
            int files = assets.length;

            // Buffer proper English and parse it (no 's after a name that ends with s)
            String theme = getThemeName(package_identifier);
            if (theme != null) {
                if (theme.substring(theme.length() - 1).equals("s")) {
                    theme = theme + "\'";
                } else {
                    theme = theme + "\'s";
                }
            }
            String theme_name = String.format(
                    mContext.getString(R.string.notification_initial_title_upgrade), theme);

            // Initialize Notification
            int notification_priority = Notification.PRIORITY_MAX;

            // Create an Intent for the BroadcastReceiver
            Intent buttonIntent = new Intent(mContext, NotificationUpgradeReceiver.class);

            // Create the PendingIntent
            PendingIntent btPendingIntent = PendingIntent.getBroadcast(
                    mContext, 0, buttonIntent, 0);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(
                    mContext, 0, new Intent(), 0);

            mNotifyManager = (NotificationManager) mContext.getSystemService(
                    Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext,
                    References.MAIN_NOTIFICATION_CHANNEL_ID);

            mBuilder.setContentTitle(theme_name)
                    .setProgress(files, 0, true)
                    .addAction(android.R.color.transparent, mContext.getString(R.string
                            .notification_hide_upgrade), btPendingIntent)
                    .setSmallIcon(android.R.drawable.ic_popup_sync)
                    .setPriority(notification_priority)
                    .setContentIntent(resultPendingIntent)
                    .setOngoing(true);
            mNotifyManager.notify(id, mBuilder.build());

            int count = 0;
            for (String dir : assets) {
                FileOperations.copyFileOrDir(
                        themeAssetManager,
                        dir,
                        myDir.getAbsolutePath() + "/" + dir,
                        dir,
                        cipher);
                count++;
                mBuilder.setProgress(files, count, false);
                mNotifyManager.notify(id, mBuilder.build());
            }
            if (checkNotificationVisibility()) {
                createVersioningPlaceholderFile(package_identifier, package_identifier);
                mNotifyManager.cancel(id);
                Log.d("SubstratumCacher",
                        "The theme's assets have been successfully expanded to the work area!");
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(References.SUBSTRATUM_LOG,
                    "There is no valid package found installed on this device.");
        }
        return false;
    }

    private boolean checkNotificationVisibility() {
        StatusBarNotification[] notifications = mNotifyManager.getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == id) {
                return true;
            }
        }
        return false;
    }

    // Create a version.xml to take note of whether the cache needs to be rebuilt

    private void createVersioningPlaceholderFile(String package_identifier, String package_name) {
        File root = new File(mContext.getCacheDir().getAbsoluteFile() +
                SUBSTRATUM_BUILDER_CACHE + package_name + "/substratum.xml");
        try {
            Boolean created = root.createNewFile();
            if (!created) Log.e(References.SUBSTRATUM_LOG,
                    "Unable to create versioning placeholder file...");
            FileWriter fw = new FileWriter(root);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            String manifest = getThemeVersion(package_identifier);
            pw.write((manifest == null) ? "" : manifest);
            pw.close();
            bw.close();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}