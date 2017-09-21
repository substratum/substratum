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
import projekt.substratum.common.References;
import projekt.substratum.common.platform.ThemeManager;

import static projekt.substratum.common.References.ANDROMEDA_PACKAGE;
import static projekt.substratum.common.References.PACKAGE_ADDED;
import static projekt.substratum.common.References.SST_ADDON_PACKAGE;

public class OverlayFound extends BroadcastReceiver {

    private final static String TAG = "OverlayFound";
    private String package_name;
    private Context context;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (PACKAGE_ADDED.equals(intent.getAction())) {
            this.package_name = intent.getData().toString().substring(8);
            this.context = context;

            if (ThemeManager.isOverlay(context, package_name)) {
                return;
            }

            if (package_name.equals(SST_ADDON_PACKAGE) || package_name.equals(ANDROMEDA_PACKAGE)) {
                SharedPreferences prefs =
                        context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
                prefs.edit().clear().apply();
                References.sendKillMessage(context);
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            Boolean to_update = prefs.getBoolean("overlay_alert", false);
            if (!to_update) return;

            // When the package is being updated, continue.
            Boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            if (!replacing) new OverlayUpdate(this).execute("");
        }
    }

    private static class OverlayUpdate extends AsyncTask<String, Integer, String> {

        private WeakReference<OverlayFound> ref;
        private NotificationManager mNotifyManager;
        private NotificationCompat.Builder mBuilder;
        private List<ResolveInfo> installed_themes;
        private ArrayList<String> matching_criteria;

        OverlayUpdate(OverlayFound overlayFound) {
            ref = new WeakReference<>(overlayFound);
        }

        @Override
        protected void onPreExecute() {
            OverlayFound overlayFound = ref.get();
            if (overlayFound != null) {
                mNotifyManager = (NotificationManager) overlayFound.context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                installed_themes = References.getThemes(overlayFound.context);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (matching_criteria.size() > 0) {
                for (int i = 0; i < matching_criteria.size(); i++) {
                    bundleNotifications(matching_criteria.get(i));
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
                        References.grabAppIcon(overlayFound.context, theme_package)).getBitmap());

                Intent notificationIntent = References.themeIntent(
                        overlayFound.context, theme_package, null,
                        References.TEMPLATE_THEME_MODE);
                PendingIntent contentIntent = PendingIntent.getActivity(
                        overlayFound.context,
                        (int) System.currentTimeMillis(),
                        notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(contentIntent);

                String format = String.format(
                        overlayFound.context.getString(
                                R.string.notification_overlay_found_specific),
                        References.grabPackageName(overlayFound.context, overlayFound.package_name),
                        References.grabPackageName(overlayFound.context, theme_package));
                mBuilder.setContentTitle(format);
                mBuilder.setContentText(
                        overlayFound.context.getString(
                                R.string.notification_overlay_found_description));
                mNotifyManager.notify(
                        ThreadLocalRandom.current().nextInt(0, 100 + 1), mBuilder.build());
            }
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(String... sUrl) {
            OverlayFound overlayFound = ref.get();
            if (overlayFound != null) {
                matching_criteria = new ArrayList<>();
                for (int i = 0; i < installed_themes.size(); i++) {
                    String theme_pid = installed_themes.get(i).activityInfo.packageName;
                    Log.d(TAG, "Searching theme for themable overlay: " + theme_pid);
                    try {
                        Resources themeResources = overlayFound.context.getPackageManager()
                                .getResourcesForApplication(theme_pid);
                        AssetManager themeAssetManager = themeResources.getAssets();
                        String[] listArray = themeAssetManager.list("overlays");
                        List<String> list = Arrays.asList(listArray);
                        if (list.contains(overlayFound.package_name))
                            matching_criteria.add(theme_pid);
                    } catch (Exception e) {
                        // Suppress exception
                    }
                }
            }
            return null;
        }
    }
}