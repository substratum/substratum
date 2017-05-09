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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.compilers.SubstratumBuilder;

import static projekt.substratum.common.References.PACKAGE_ADDED;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;
import static projekt.substratum.common.References.metadataOverlayParent;
import static projekt.substratum.common.References.metadataOverlayTarget;
import static projekt.substratum.common.References.metadataOverlayType1a;
import static projekt.substratum.common.References.metadataOverlayType1b;
import static projekt.substratum.common.References.metadataOverlayType1c;
import static projekt.substratum.common.References.metadataOverlayType2;
import static projekt.substratum.common.References.metadataOverlayType3;

public class OverlayUpdater extends BroadcastReceiver {

    private static final String TAG = "OverlayUpdater";
    private static final String overlaysDir = "overlays";
    private static final String APP_UPGRADE = "AppUpgrade";
    private static final String THEME_UPGRADE = "ThemeUpgrade";
    private static final Integer APP_UPGRADE_NOTIFICATION_ID = 24768941;
    private static final Integer THEME_UPGRADE_NOTIFICATION_ID = 13573743;
    private String UPGRADE_MODE = "";
    private String package_name;
    private Context context;
    private Integer currentNotificationID;

    @SuppressWarnings({"ConstantConditions"})
    @Override
    public void onReceive(Context context, Intent intent) {
        if (PACKAGE_ADDED.equals(intent.getAction())) {
            this.package_name = intent.getData().toString().substring(8);
            this.context = context;

            if (ThemeManager.isOverlay(package_name) ||
                    !References.checkOMS(context) ||
                    References.isCachingEnabled(context)) {
                return;
            }

            // When the package is being updated, continue.
            Boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (replacing && !References.getThemesArray(context).contains(package_name)) {
                // When the package is replacing, and also not a theme, update the overlays too!
                Boolean to_update = prefs.getBoolean("overlay_updater", false);
                if (!to_update) return;
                UPGRADE_MODE = APP_UPGRADE;
                currentNotificationID = APP_UPGRADE_NOTIFICATION_ID;
                new OverlayUpdate().execute(APP_UPGRADE);
            } else if (replacing && References.getThemesArray(context).contains(package_name)) {
                // When the package is replacing, and also a theme, update the overlays too!
                Boolean to_update = prefs.getBoolean("theme_updater", false);
                if (!to_update) return;
                UPGRADE_MODE = THEME_UPGRADE;
                currentNotificationID = THEME_UPGRADE_NOTIFICATION_ID;
                new OverlayUpdate().execute(THEME_UPGRADE);
            }
        }
    }

    private class OverlayUpdate extends AsyncTask<String, Integer, String> {

        int notification_priority = Notification.PRIORITY_MAX;
        private NotificationManager mNotifyManager;
        private NotificationCompat.Builder mBuilder;
        private List<String> installed_overlays;
        private int id = currentNotificationID;
        private ArrayList<String> errored_packages;

        @Override
        protected void onPreExecute() {
            switch (UPGRADE_MODE) {
                case APP_UPGRADE:
                    installed_overlays = ThemeManager.listOverlaysForTarget(package_name);
                    break;
                case THEME_UPGRADE:
                    installed_overlays = ThemeManager.listOverlaysByTheme(context, package_name);
                    break;
            }
            if (UPGRADE_MODE != null && !UPGRADE_MODE.equals("") && installed_overlays.size() > 0) {
                errored_packages = new ArrayList<>();
                mNotifyManager = (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                mBuilder = new NotificationCompat.Builder(context);
                String format = String.format(
                        context.getString(R.string.notification_initial_title_upgrade_intent),
                        References.grabPackageName(context, package_name));
                mBuilder.setContentTitle(format)
                        .setProgress(100, 0, true)
                        .setLargeIcon(References.getBitmapFromVector(
                                context,
                                References.grabAppIcon(
                                        context,
                                        package_name)))
                        .setSmallIcon(android.R.drawable.ic_popup_sync)
                        .setPriority(notification_priority)
                        .setOngoing(true);
                mNotifyManager.notify(id, mBuilder.build());
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (installed_overlays.size() > 0) {
                mNotifyManager.cancel(id);
                mBuilder.setAutoCancel(true);
                mBuilder.setProgress(0, 0, false);
                mBuilder.setOngoing(false);
                mBuilder.setLargeIcon(References.getBitmapFromVector(
                        context,
                        References.grabAppIcon(
                                context,
                                package_name)));

                if (errored_packages.size() > 0) {
                    mBuilder.setSmallIcon(R.drawable.notification_warning_icon);
                    String format = String.format(
                            context.getString(R.string.notification_done_upgrade_title_warning),
                            References.grabPackageName(context, package_name));
                    mBuilder.setContentTitle(format);

                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < errored_packages.size(); i++) {
                        stringBuilder.append(errored_packages.get(i));
                        if ((errored_packages.size() - 1) > i) {
                            stringBuilder.append("\n");
                        }
                    }
                    String format2 = String.format(
                            context.getString(R.string.notification_done_upgrade_title_failed),
                            stringBuilder.toString());
                    mBuilder.setContentText(format2);
                } else {
                    mBuilder.setSmallIcon(R.drawable.notification_success_icon);
                    String format = String.format(
                            context.getString(R.string.notification_done_upgrade_title),
                            References.grabPackageName(context, package_name));
                    mBuilder.setContentTitle(format);
                    switch (UPGRADE_MODE) {
                        case APP_UPGRADE:
                            mBuilder.setContentText(
                                    context.getString(R.string.notification_done_update_text));
                            break;
                        case THEME_UPGRADE:
                            mBuilder.setContentText(
                                    context.getString(R.string.notification_done_upgrade_text));
                            break;
                    }
                }
                errored_packages = new ArrayList<>();
                UPGRADE_MODE = "";
                currentNotificationID = 0;
                mNotifyManager.notify(id, mBuilder.build());
            }
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(String... sUrl) {
            if (installed_overlays.size() > 0) {
                Log.d(TAG, "'" + package_name +
                        "' was just updated with overlays present, updating...");
                for (int i = 0; i < installed_overlays.size(); i++) {
                    Log.d(TAG, "Current overlay found in stash: " + installed_overlays.get(i));

                    mBuilder.setProgress(100, (int) (((double) (i + 1) /
                            installed_overlays.size()) * 100), false);

                    switch (UPGRADE_MODE) {
                        case APP_UPGRADE:
                            mBuilder.setContentText(
                                    References.grabPackageName(context, package_name) + " (" +
                                            References.grabPackageName(
                                                    context,
                                                    References.grabOverlayParent(
                                                            context,
                                                            installed_overlays.get(i))
                                            ) + ")");
                            break;
                        case THEME_UPGRADE:
                            mBuilder.setContentText(
                                    References.grabPackageName(context,
                                            References.grabOverlayTarget(
                                                    context,
                                                    installed_overlays.get(i))));
                            mBuilder.setLargeIcon(
                                    References.getBitmapFromVector(
                                            context,
                                            References.grabAppIcon(
                                                    context,
                                                    References.grabOverlayTarget(
                                                            context,
                                                            installed_overlays.get(i)))));
                            break;
                    }
                    mNotifyManager.notify(id, mBuilder.build());

                    String theme = References.getOverlayMetadata(context,
                            installed_overlays.get(i), metadataOverlayParent);

                    AssetManager themeAssetManager;
                    Resources themeResources = null;
                    try {
                        themeResources = context.getPackageManager()
                                .getResourcesForApplication(theme);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    assert themeResources != null;
                    themeAssetManager = themeResources.getAssets();

                    String target = References.getOverlayMetadata(
                            context, installed_overlays.get(i), metadataOverlayTarget);
                    String type1a = References.getOverlayMetadata(
                            context, installed_overlays.get(i), metadataOverlayType1a);
                    String type1b = References.getOverlayMetadata(
                            context, installed_overlays.get(i), metadataOverlayType1b);
                    String type1c = References.getOverlayMetadata(
                            context, installed_overlays.get(i), metadataOverlayType1c);
                    String type2 = References.getOverlayMetadata(
                            context, installed_overlays.get(i), metadataOverlayType2);
                    String type3 = References.getOverlayMetadata(
                            context, installed_overlays.get(i), metadataOverlayType3);

                    if (type1a != null && type1a.contains("overlays/")) return null;
                    if (type1b != null && type1b.contains("overlays/")) return null;
                    if (type1c != null && type1c.contains("overlays/")) return null;
                    if (type2 != null && type2.contains("overlays/")) return null;
                    if (type3 != null && type3.contains("overlays/")) return null;

                    String type1aDir = "overlays/" + target + "/type1a_" + type1a + ".xml";
                    String type1bDir = "overlays/" + target + "/type1b_" + type1b + ".xml";
                    String type1cDir = "overlays/" + target + "/type1c_" + type1c + ".xml";
                    String type2Dir = "overlays/" + target + "/type2_" + type2;
                    String type3Dir = "overlays/" + target + "/type3_" + type3;

                    String additional_variant = ((type2 != null && type2.length() > 0) ?
                            type2Dir.split("/")[2].substring(6) : null);
                    String base_variant = ((type3Dir != null && type3Dir.length() > 0) ?
                            type3Dir.split("/")[2].substring(6) : null);

                    // Prenotions
                    String suffix = ((type3 != null && type3.length() != 0) ?
                            "/" + type3Dir : "/res");
                    String workingDirectory = context.getCacheDir().getAbsolutePath() +
                            SUBSTRATUM_BUILDER_CACHE.substring(0,
                                    SUBSTRATUM_BUILDER_CACHE.length() - 1);
                    File created = new File(workingDirectory);
                    if (created.exists()) {
                        FileOperations.delete(context, created.getAbsolutePath());
                        FileOperations.createNewFolder(context, created.getAbsolutePath());
                    } else {
                        FileOperations.createNewFolder(context, created.getAbsolutePath());
                    }

                    // Handle the resource folder
                    String listDir = overlaysDir + "/" +
                            (UPGRADE_MODE.equals(APP_UPGRADE) ?
                                    package_name :
                                    References.grabOverlayTarget(context, installed_overlays.get(i))
                            ) + suffix;
                    FileOperations.copyFileOrDir(
                            themeAssetManager,
                            listDir,
                            workingDirectory + suffix,
                            listDir);
                    if (type2 != null && type2.length() > 0) {
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                listDir,
                                workingDirectory + "/type2_" + type2,
                                listDir);
                    }

                    // Handle the type1s
                    if (type1a != null && type1a.length() > 0) {
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                type1aDir,
                                workingDirectory + suffix + "/values/type1a.xml",
                                type1aDir);
                    }
                    if (type1b != null && type1b.length() > 0) {
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                type1bDir,
                                workingDirectory + suffix + "/values/type1b.xml",
                                type1bDir);
                    }
                    if (type1c != null && type1c.length() > 0) {
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                type1cDir,
                                workingDirectory + suffix + "/values/type1c.xml",
                                type1cDir);
                    }

                    File workDir = new File(context.getCacheDir().getAbsolutePath() +
                            SUBSTRATUM_BUILDER_CACHE);
                    if (!workDir.exists() && !workDir.mkdirs())
                        Log.e(TAG, "Could not make cache directory...");

                    SubstratumBuilder sb = new SubstratumBuilder();
                    sb.beginAction(
                            context,
                            theme,
                            (UPGRADE_MODE.equals(APP_UPGRADE) ?
                                    package_name :
                                    References.grabOverlayTarget(
                                            context,
                                            installed_overlays.get(i))),
                            References.grabPackageName(context, theme),
                            (UPGRADE_MODE.equals(APP_UPGRADE) ?
                                    package_name :
                                    References.grabOverlayTarget(
                                            context,
                                            installed_overlays.get(i))),
                            additional_variant,
                            base_variant,
                            References.grabAppVersion(context, installed_overlays.get(i)),
                            References.checkOMS(context),
                            theme,
                            suffix,
                            type1a,
                            type1b,
                            type1c,
                            type2,
                            type3,
                            installed_overlays.get(i)
                    );
                    if (sb.has_errored_out) errored_packages.add(installed_overlays.get(i));
                }
            }
            return null;
        }
    }
}