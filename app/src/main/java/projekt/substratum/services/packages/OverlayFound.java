/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.services.packages;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import androidx.core.app.NotificationCompat;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.platform.ThemeManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static projekt.substratum.common.Packages.getBitmapFromDrawable;
import static projekt.substratum.common.References.ANDROMEDA_PACKAGE;
import static projekt.substratum.common.References.PACKAGE_ADDED;
import static projekt.substratum.common.References.SST_ADDON_PACKAGE;
import static projekt.substratum.services.packages.PackageModificationDetector.getPendingIntent;

public class OverlayFound extends BroadcastReceiver {

    private static final String TAG = "OverlayFound";
    private String packageName;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getData() != null &&
                PACKAGE_ADDED.equals(intent.getAction())) {
            packageName = intent.getData().toString().substring(8);
            this.context = context;

            if (ThemeManager.isOverlay(context, packageName)) {
                return;
            }

            if (packageName.equals(SST_ADDON_PACKAGE) ||
                    packageName.equals(ANDROMEDA_PACKAGE)) {
                SharedPreferences prefs = context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
                prefs.edit().clear().apply();
                Broadcasts.sendKillMessage(context);
            }

            boolean toUpdate = Substratum.getPreferences().getBoolean("overlay_alert", false);
            if (!toUpdate) return;

            // When the package is being updated, continue.
            boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            if (!replacing) new OverlayUpdate(this).execute("");
        }
    }

    private static class OverlayUpdate extends AsyncTask<String, Integer, String> {

        private final WeakReference<OverlayFound> ref;
        private NotificationManager notificationManager;
        private NotificationCompat.Builder builder;
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
                notificationManager = (NotificationManager) overlayFound.context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                installedThemes = Packages.getThemes(overlayFound.context);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (!matchingCriteria.isEmpty()) {
                for (String criteria : matchingCriteria) {
                    bundleNotifications(criteria);
                }
            }
        }

        void bundleNotifications(String theme_package) {
            OverlayFound overlayFound = ref.get();
            if (overlayFound != null) {
                builder = new NotificationCompat.Builder(overlayFound.context,
                        References.DEFAULT_NOTIFICATION_CHANNEL_ID);
                builder.setAutoCancel(true);
                builder.setOngoing(false);
                builder.setSmallIcon(R.drawable.notification_overlay_found);
                builder.setLargeIcon((
                        getBitmapFromDrawable(
                                Packages.getAppIcon(overlayFound.context, theme_package))));
                builder.setContentIntent(getPendingIntent(overlayFound.context, theme_package));
                builder.setContentTitle(String.format(
                        overlayFound.context.getString(
                                R.string.notification_overlay_found_specific),
                        Packages.getPackageName(overlayFound.context, overlayFound.packageName),
                        Packages.getPackageName(overlayFound.context, theme_package)));
                builder.setContentText(
                        overlayFound.context.getString(
                                R.string.notification_overlay_found_description));
                notificationManager.notify(
                        ThreadLocalRandom.current().nextInt(0, 10000), builder.build());
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            OverlayFound overlayFound = ref.get();
            if (overlayFound != null) {
                matchingCriteria = new ArrayList<>();
                for (ResolveInfo installedTheme : installedThemes) {
                    String themePid = installedTheme.activityInfo.packageName;
                    Substratum.log(TAG, "Searching theme for themable overlay: " + themePid);
                    try {
                        Resources themeResources = overlayFound.context.getPackageManager()
                                .getResourcesForApplication(themePid);
                        AssetManager themeAssetManager = themeResources.getAssets();
                        String[] listArray = themeAssetManager.list("overlays");
                        List<String> list = Arrays.asList(listArray);
                        if (list.contains(overlayFound.packageName)) {
                            Substratum.log(TAG, "Found in theme: " + themePid);
                            matchingCriteria.add(themePid);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            return null;
        }
    }
}