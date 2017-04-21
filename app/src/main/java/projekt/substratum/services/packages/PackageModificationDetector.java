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
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

import projekt.substratum.InformationActivity;
import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.util.compilers.CacheCreator;
import projekt.substratum.util.helpers.NotificationCreator;

public class PackageModificationDetector extends BroadcastReceiver {

    private final static String PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";
    private final static String TAG = "SubstratumDetector";
    private SharedPreferences prefs;
    private Context mContext;
    private String package_name;
    private Boolean replacing;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (PACKAGE_ADDED.equals(intent.getAction())) {
            this.mContext = context;
            Uri packageName = intent.getData();
            package_name = packageName.toString().substring(8);

            // First, check if the app installed is actually a substratum theme
            try {
                ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                        package_name, PackageManager.GET_META_DATA);
                if (appInfo.metaData != null) {
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
            replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            prefs = context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);

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

            // Legacy check to see if an OMS theme is guarded from being installed on legacy
            if (!References.checkOMS(context)) {
                try {
                    ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                            package_name, PackageManager.GET_META_DATA);
                    if (appInfo.metaData != null) {
                        Boolean check_legacy =
                                appInfo.metaData.getBoolean(References.metadataLegacy);
                        if (!check_legacy) {
                            Log.e(TAG, "Device is non-OMS, while an " +
                                    "OMS theme is installed, aborting operation!");

                            String parse = String.format(mContext.getString(
                                    R.string.failed_to_install_text_notification),
                                    appInfo.metaData.getString(
                                            References.metadataName));

                            Intent showIntent = new Intent();
                            PendingIntent contentIntent = PendingIntent.getActivity(
                                    mContext, 0, showIntent, 0);

                            new NotificationCreator(
                                    context,
                                    mContext.getString(
                                            R.string.failed_to_install_title_notification),
                                    parse,
                                    true,
                                    contentIntent,
                                    R.drawable.notification_warning_icon,
                                    null,
                                    Notification.PRIORITY_MAX,
                                    References.notification_id).createNotification();

                            References.uninstallPackage(mContext, package_name);
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
                if (!References.isCachingEnabled(context)) {
                    Log.d(TAG, "'" + package_name +
                            "' has been updated with caching mode disabled.");

                    new NotificationCreator(
                            context,
                            getThemeName(package_name) + " " + mContext.getString(
                                    R.string.notification_theme_updated),
                            mContext.getString(R.string.notification_theme_updated_content),
                            true,
                            grabPendingIntent(package_name),
                            R.drawable.notification_updated,
                            BitmapFactory.decodeResource(
                                    mContext.getResources(), R.mipmap.restore_launcher),
                            Notification.PRIORITY_MAX,
                            ThreadLocalRandom.current().nextInt(0, 1000)).createNotification();
                } else {
                    Log.d(TAG, "'" + package_name +
                            "' has been updated with caching mode enabled.");
                    prefs.edit().putBoolean("is_updating", true).apply();
                    new ThemeCacher().execute("");
                }
            } else if (!References.isCachingEnabled(context)) {
                // This is a new install, but caching mode is disabled, so pass right through!
                Log.d(TAG, "'" + package_name + "' has been installed with caching mode disabled.");

                new NotificationCreator(
                        context,
                        getThemeName(package_name) + " " + mContext.getString(
                                R.string.notification_theme_installed),
                        mContext.getString(R.string.notification_theme_installed_content),
                        true,
                        grabPendingIntent(package_name),
                        R.drawable.notification_icon,
                        BitmapFactory.decodeResource(
                                mContext.getResources(), R.mipmap.main_launcher),
                        Notification.PRIORITY_MAX,
                        ThreadLocalRandom.current().nextInt(0, 1000)).createNotification();
            } else {
                Log.d(TAG, "'" + package_name + "' has been installed with caching mode enabled.");
                new ThemeCacher().execute("");
            }
            References.sendRefreshMessage(context);
        }
    }

    private String getThemeName(String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (References.checkOMS(mContext) &&
                        appInfo.metaData.getString(References.metadataName) != null &&
                        appInfo.metaData.getString(References.metadataAuthor) != null) {
                    return appInfo.metaData.getString(References.metadataName);
                } else if (appInfo.metaData.getBoolean(References.metadataLegacy, false) &&
                        appInfo.metaData.getString(References.metadataName) != null &&
                        appInfo.metaData.getString(References.metadataAuthor) != null) {
                    return appInfo.metaData.getString(References.metadataName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to find package identifier (INDEX OUT OF BOUNDS)");
        }
        return null;
    }

    public PendingIntent grabPendingIntent(String package_name) {
        Intent notificationIntent;
        PendingIntent pIntent = null;
        try {
            Intent myIntent =
                    References.sendLaunchIntent(
                            mContext, package_name, false, null, true);
            if (myIntent != null) {
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
                stackBuilder.addParentStack(InformationActivity.class);
                stackBuilder.addNextIntent(myIntent);
                pIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);
            } else {
                notificationIntent = new Intent(mContext, MainActivity.class);
                pIntent = PendingIntent.getActivity(
                        mContext,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);
            }
        } catch (Exception e) {
            // Suppress warning
        }
        return pIntent;
    }

    private class ThemeCacher extends AsyncTask<String, Integer, String> {

        Boolean success = false;

        @Override
        protected void onPostExecute(String result) {
            if (success) {
                if (replacing) {
                    new NotificationCreator(
                            mContext,
                            getThemeName(package_name) + " " + mContext.getString(
                                    R.string.notification_theme_updated),
                            mContext.getString(R.string.notification_theme_updated_content),
                            true,
                            grabPendingIntent(package_name),
                            R.drawable.notification_updated,
                            BitmapFactory.decodeResource(
                                    mContext.getResources(), R.mipmap.restore_launcher),
                            Notification.PRIORITY_MAX,
                            ThreadLocalRandom.current().nextInt(0, 1000)).createNotification();
                } else {
                    new NotificationCreator(
                            mContext,
                            getThemeName(package_name) + " " + mContext.getString(
                                    R.string.notification_theme_installed),
                            mContext.getString(R.string.notification_theme_installed_content),
                            true,
                            grabPendingIntent(package_name),
                            R.drawable.notification_icon,
                            BitmapFactory.decodeResource(
                                    mContext.getResources(), R.mipmap.main_launcher),
                            Notification.PRIORITY_MAX,
                            ThreadLocalRandom.current().nextInt(0, 1000)).createNotification();
                }
            } else {
                Log.d(TAG, "Process was interrupted by the user, rolling back changes...");
            }
            prefs.edit().putBoolean("is_updating", false).apply();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            success = !References.isCachingEnabled(mContext) ||
                    new CacheCreator().initializeCache(mContext, package_name);
            return null;
        }
    }
}