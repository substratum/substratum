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
    private String package_name;
    private Context context;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (PACKAGE_ADDED.equals(intent.getAction())) {
            this.package_name = intent.getData().toString().substring(8);
            this.context = context;

            if (ThemeManager.isOverlay(context, this.package_name)) {
                return;
            }

            if (this.package_name.equals(SST_ADDON_PACKAGE) || this.package_name.equals(ANDROMEDA_PACKAGE)) {
                final SharedPreferences prefs =
                        context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
                prefs.edit().clear().apply();
                Broadcasts.sendKillMessage(context);
            }

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final Boolean to_update = prefs.getBoolean("overlay_alert", false);
            if (!to_update) return;

            // When the package is being updated, continue.
            final Boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            if (!replacing) new OverlayUpdate(this).execute("");
        }
    }

    private static class OverlayUpdate extends AsyncTask<String, Integer, String> {

        private final WeakReference<OverlayFound> ref;
        private NotificationManager mNotifyManager;
        private NotificationCompat.Builder mBuilder;
        private List<ResolveInfo> installed_themes;
        private List<String> matching_criteria;

        OverlayUpdate(final OverlayFound overlayFound) {
            super();
            this.ref = new WeakReference<>(overlayFound);
        }

        @Override
        protected void onPreExecute() {
            final OverlayFound overlayFound = this.ref.get();
            if (overlayFound != null) {
                this.mNotifyManager = (NotificationManager) overlayFound.context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                this.installed_themes = Packages.getThemes(overlayFound.context);
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            if (!this.matching_criteria.isEmpty()) {
                for (int i = 0; i < this.matching_criteria.size(); i++) {
                    this.bundleNotifications(this.matching_criteria.get(i));
                }
            }
        }

        @SuppressWarnings("deprecation")
        void bundleNotifications(final String theme_package) {
            final OverlayFound overlayFound = this.ref.get();
            if (overlayFound != null) {
                this.mBuilder = new NotificationCompat.Builder(overlayFound.context);
                this.mBuilder.setAutoCancel(true);
                this.mBuilder.setOngoing(false);
                this.mBuilder.setSmallIcon(R.drawable.notification_overlay_found);
                this.mBuilder.setLargeIcon(((BitmapDrawable)
                        Packages.getAppIcon(overlayFound.context, theme_package)).getBitmap());

                final Intent notificationIntent = Theming.themeIntent(
                        overlayFound.context, theme_package, null,
                        References.TEMPLATE_THEME_MODE);
                final PendingIntent contentIntent = PendingIntent.getActivity(
                        overlayFound.context,
                        (int) System.currentTimeMillis(),
                        notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                this.mBuilder.setContentIntent(contentIntent);

                final String format = String.format(
                        overlayFound.context.getString(
                                R.string.notification_overlay_found_specific),
                        Packages.getPackageName(overlayFound.context, overlayFound.package_name),
                        Packages.getPackageName(overlayFound.context, theme_package));
                this.mBuilder.setContentTitle(format);
                this.mBuilder.setContentText(
                        overlayFound.context.getString(
                                R.string.notification_overlay_found_description));
                this.mNotifyManager.notify(
                        ThreadLocalRandom.current().nextInt(0, 100 + 1), this.mBuilder.build());
            }
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(final String... sUrl) {
            final OverlayFound overlayFound = this.ref.get();
            if (overlayFound != null) {
                this.matching_criteria = new ArrayList<>();
                for (int i = 0; i < this.installed_themes.size(); i++) {
                    final String theme_pid = this.installed_themes.get(i).activityInfo.packageName;
                    Log.d(TAG, "Searching theme for themable overlay: " + theme_pid);
                    try {
                        final Resources themeResources = overlayFound.context.getPackageManager()
                                .getResourcesForApplication(theme_pid);
                        final AssetManager themeAssetManager = themeResources.getAssets();
                        final String[] listArray = themeAssetManager.list("overlays");
                        final List<String> list = Arrays.asList(listArray);
                        if (list.contains(overlayFound.package_name))
                            this.matching_criteria.add(theme_pid);
                    } catch (final Exception e) {
                        // Suppress exception
                    }
                }
            }
            return null;
        }
    }
}