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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import projekt.substratum.R;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Theming;
import projekt.substratum.common.platform.ThemeManager;

import static projekt.substratum.common.References.ANDROMEDA_PACKAGE;
import static projekt.substratum.common.References.PACKAGE_ADDED;
import static projekt.substratum.common.References.SST_ADDON_PACKAGE;

public class OverlayFound extends BroadcastReceiver {

    private static final String TAG = "OverlayFound";
    private String packageName;
    private Context context;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (PACKAGE_ADDED.equals(intent.getAction())) {
            packageName = intent.getData().toString().substring(8);
            this.context = context;

            if (ThemeManager.isOverlay(context, packageName)) {
                return;
            }

            if (packageName.equals(SST_ADDON_PACKAGE) ||
                    packageName.equals(ANDROMEDA_PACKAGE)) {
                SharedPreferences prefs =
                        context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
                prefs.edit().clear().apply();
                Broadcasts.sendKillMessage(context);
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean toUpdate = prefs.getBoolean("overlay_alert", false);
            if (!toUpdate) return;

            // When the package is being updated, continue.
            boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            if (!replacing) new OverlayUpdate(this).execute("");
        }
    }

    private static class OverlayUpdate extends AsyncTask<String, Integer, String> {

        private WeakReference<OverlayFound> ref;
        private NotificationManager mNotifyManager;
        private NotificationCompat.Builder mBuilder;
        private List<ResolveInfo> installedThemes;
        private List<String> matchingCriteria;

        OverlayUpdate(OverlayFound overlayFound) {
            super();
            ref = new WeakReference<>(overlayFound);
        }

        @Override
        protected void onPreExecute() {
            OverlayFound overlayFound = ref.get();
            if (overlayFound != null) {
                mNotifyManager = (NotificationManager) overlayFound.context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                installedThemes = Packages.getThemes(overlayFound.context);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (!matchingCriteria.isEmpty()) {
                for (int i = 0; i < matchingCriteria.size(); i++) {
                    bundleNotifications(matchingCriteria.get(i));
                }
            }
        }

        @SuppressWarnings("deprecation")
        void bundleNotifications(String theme_package) {
            OverlayFound overlayFound = ref.get();
            if (overlayFound != null) {
                mBuilder = new NotificationCompat.Builder(overlayFound.context);
                mBuilder.setAutoCancel(true);
                mBuilder.setOngoing(false);
                mBuilder.setSmallIcon(R.drawable.notification_overlay_found);
                mBuilder.setLargeIcon(((BitmapDrawable)
                        Packages.getAppIcon(overlayFound.context, theme_package)).getBitmap());

                Intent notificationIntent = Theming.themeIntent(
                        overlayFound.context,
                        theme_package,
                        null,
                        References.TEMPLATE_THEME_MODE
                );
                PendingIntent contentIntent = PendingIntent.getActivity(
                        overlayFound.context,
                        (int) System.currentTimeMillis(),
                        notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(contentIntent);

                mBuilder.setContentTitle(String.format(
                        overlayFound.context.getString(
                                R.string.notification_overlay_found_specific),
                        Packages.getPackageName(overlayFound.context, overlayFound.packageName),
                        Packages.getPackageName(overlayFound.context, theme_package)));
                mBuilder.setContentText(
                        overlayFound.context.getString(
                                R.string.notification_overlay_found_description));
                mNotifyManager.notify(
                        ThreadLocalRandom.current().nextInt(0, 100 + 1), mBuilder.build());
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            OverlayFound overlayFound = ref.get();
            if (overlayFound != null) {
                matchingCriteria = new ArrayList<>();
                for (int i = 0; i < installedThemes.size(); i++) {
                    String theme_pid = installedThemes.get(i).activityInfo.packageName;
                    Log.d(TAG, "Searching theme for themable overlay: " + theme_pid);
                    try {
                        Resources themeResources = overlayFound.context.getPackageManager()
                                .getResourcesForApplication(theme_pid);
                        AssetManager themeAssetManager = themeResources.getAssets();
                        String[] listArray = themeAssetManager.list("overlays");
                        List<String> list = Arrays.asList(listArray);
                        if (list.contains(overlayFound.packageName))
                            matchingCriteria.add(theme_pid);
                    } catch (Exception e) {
                        // Suppress exception
                    }
                }
            }
            return null;
        }
    }
}