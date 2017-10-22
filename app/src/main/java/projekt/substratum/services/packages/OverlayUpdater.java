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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.Theming;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.compilers.SubstratumBuilder;

import static projekt.substratum.common.References.DEFAULT_NOTIFICATION_CHANNEL_ID;
import static projekt.substratum.common.References.KEY_RETRIEVAL;
import static projekt.substratum.common.References.PACKAGE_ADDED;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;
import static projekt.substratum.common.References.metadataEncryption;
import static projekt.substratum.common.References.metadataEncryptionValue;
import static projekt.substratum.common.References.metadataOverlayDevice;
import static projekt.substratum.common.References.metadataOverlayParent;
import static projekt.substratum.common.References.metadataOverlayTarget;
import static projekt.substratum.common.References.metadataOverlayType1a;
import static projekt.substratum.common.References.metadataOverlayType1b;
import static projekt.substratum.common.References.metadataOverlayType1c;
import static projekt.substratum.common.References.metadataOverlayType2;
import static projekt.substratum.common.References.metadataOverlayType3;
import static projekt.substratum.common.References.metadataOverlayType4;

public class OverlayUpdater extends BroadcastReceiver {

    private static final String TAG = "OverlayUpdater";
    private static final String overlaysDir = "overlays";
    private static final String APP_UPGRADE = "AppUpgrade";
    private static final String THEME_UPGRADE = "ThemeUpgrade";
    private static final Integer APP_UPGRADE_NOTIFICATION_ID = 24768941;
    private static final Integer THEME_UPGRADE_NOTIFICATION_ID = 13573743;

    @SuppressWarnings({"ConstantConditions"})
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (PACKAGE_ADDED.equals(intent.getAction())) {
            final String package_name = intent.getData().toString().substring(8);

            if (ThemeManager.isOverlay(context, intent.getData().toString().substring(8)) ||
                    (Packages.getOverlayMetadata(
                            context,
                            intent.getData().toString().substring(8),
                            metadataOverlayDevice) != null)) {
                return;
            }

            // Let's start the intent filter
            final UpdaterLogs updaterLogs = new UpdaterLogs();
            LocalBroadcastManager.getInstance(context).registerReceiver(updaterLogs,
                    new IntentFilter("Updater.LOGS"));

            // When the package is being updated, continue.
            final Boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (replacing && !Packages.getThemesArray(context).contains(package_name)) {
                // When the package is replacing, and also not a theme, update the overlays too!
                final Boolean to_update = prefs.getBoolean("overlay_updater", false);
                if (!to_update) return;
                new OverlayUpdate(
                        context,
                        package_name,
                        APP_UPGRADE,
                        APP_UPGRADE_NOTIFICATION_ID
                ).execute(APP_UPGRADE);
            } else if (replacing && Packages.getThemesArray(context).contains(package_name)) {
                // When the package is replacing, and also a theme, update the overlays too!
                final Boolean to_update = prefs.getBoolean("theme_updater", false);
                if (!to_update) return;
                new OverlayUpdate(
                        context,
                        package_name,
                        THEME_UPGRADE,
                        THEME_UPGRADE_NOTIFICATION_ID
                ).execute(APP_UPGRADE);
            }
        }
    }

    private static class OverlayUpdate extends AsyncTask<String, Integer, String> {

        final int notification_priority = Notification.PRIORITY_MAX;
        private NotificationManager mNotifyManager;
        private NotificationCompat.Builder mBuilder;
        private List<String> installed_overlays;
        private List<String> errored_packages;
        @SuppressLint("StaticFieldLeak")
        private final Context context;
        private LocalBroadcastManager localBroadcastManager;
        private KeyRetrieval keyRetrieval;
        private Intent securityIntent;
        private Cipher cipher;
        private String upgrade_mode = "";
        private final String package_name;
        private final StringBuilder error_logs = new StringBuilder();
        private final int id;
        private final Handler handler = new Handler();
        private final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Waiting for encryption key handshake approval...");
                if (OverlayUpdate.this.securityIntent != null) {
                    Log.d(TAG, "Encryption key handshake approved!");
                    OverlayUpdate.this.handler.removeCallbacks(OverlayUpdate.this.runnable);
                } else {
                    Log.d(TAG, "Encryption key still null...");
                    try {
                        Thread.sleep(500);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    OverlayUpdate.this.handler.postDelayed(this, 100);
                }
            }
        };

        OverlayUpdate(final Context context, final String package_name, final String mode, final int id) {
            super();
            this.context = context;
            this.package_name = package_name;
            this.upgrade_mode = mode;
            this.id = id;
        }

        @Override
        protected void onPreExecute() {
            switch (this.upgrade_mode) {
                case APP_UPGRADE:
                    this.installed_overlays = ThemeManager.listOverlaysForTarget(this.context, this.package_name);
                    break;
                case THEME_UPGRADE:
                    this.installed_overlays = ThemeManager.listOverlaysByTheme(this.context, this.package_name);
                    break;
            }
            if ((this.upgrade_mode != null) && !"".equals(this.upgrade_mode) && !this.installed_overlays.isEmpty()) {
                this.errored_packages = new ArrayList<>();
                this.mNotifyManager = (NotificationManager) this.context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                this.mBuilder = new NotificationCompat.Builder(this.context, DEFAULT_NOTIFICATION_CHANNEL_ID);
                final String format = String.format(
                        this.context.getString(R.string.notification_initial_title_upgrade_intent),
                        Packages.getPackageName(this.context, this.package_name));
                this.mBuilder.setContentTitle(format)
                        .setProgress(100, 0, true)
                        .setLargeIcon(Packages.getBitmapFromVector(
                                Packages.getAppIcon(
                                        this.context,
                                        this.package_name)))
                        .setSmallIcon(android.R.drawable.ic_popup_sync)
                        .setPriority(this.notification_priority)
                        .setOngoing(true);
                this.mNotifyManager.notify(this.id, this.mBuilder.build());

                this.localBroadcastManager = LocalBroadcastManager.getInstance(this.context);

            }
        }

        @Override
        protected void onPostExecute(final String result) {
            if (!this.installed_overlays.isEmpty()) {
                this.mNotifyManager.cancel(this.id);
                this.mBuilder.setAutoCancel(true);
                this.mBuilder.setProgress(0, 0, false);
                this.mBuilder.setOngoing(false);
                this.mBuilder.setLargeIcon(Packages.getBitmapFromVector(
                        Packages.getAppIcon(
                                this.context,
                                this.package_name)));

                if (!this.errored_packages.isEmpty()) {
                    this.mBuilder.setSmallIcon(R.drawable.notification_warning_icon);
                    final String format = String.format(
                            this.context.getString(R.string.notification_done_upgrade_title_warning),
                            Packages.getPackageName(this.context, this.package_name));
                    this.mBuilder.setContentTitle(format);

                    final StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < this.errored_packages.size(); i++) {
                        stringBuilder.append(this.errored_packages.get(i));
                        if ((this.errored_packages.size() - 1) > i) {
                            stringBuilder.append("\n");
                        }
                    }
                    final String format2 = String.format(
                            this.context.getString(R.string.notification_done_upgrade_title_failed),
                            stringBuilder.toString());
                    this.mBuilder.setContentText(format2);
                    final Intent intent = new Intent("Updater.LOGS");
                    intent.putExtra("error_logs", this.error_logs.toString());
                    final PendingIntent pintent = PendingIntent.getActivity(this.context, 0,
                            intent, PendingIntent.FLAG_ONE_SHOT);
                    this.mBuilder.setContentIntent(pintent);
                } else {
                    this.mBuilder.setSmallIcon(R.drawable.notification_success_icon);
                    final String format = String.format(
                            this.context.getString(R.string.notification_done_upgrade_title),
                            Packages.getPackageName(this.context, this.package_name));
                    this.mBuilder.setContentTitle(format);
                    switch (this.upgrade_mode) {
                        case APP_UPGRADE:
                            this.mBuilder.setContentText(
                                    this.context.getString(R.string.notification_done_update_text));
                            break;
                        case THEME_UPGRADE:
                            this.mBuilder.setContentText(
                                    this.context.getString(R.string.notification_done_upgrade_text));
                            break;
                    }
                }
                this.errored_packages = new ArrayList<>();
                this.upgrade_mode = "";
                this.mNotifyManager.notify(this.id, this.mBuilder.build());
            }
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(final String... sUrl) {
            if (!this.installed_overlays.isEmpty()) {
                Log.d(TAG, "'" + this.package_name +
                        "' was just updated with overlays present, updating...");
                for (int i = 0; i < this.installed_overlays.size(); i++) {
                    Log.d(TAG, "Current overlay found in stash: " + this.installed_overlays.get(i));

                    this.mBuilder.setProgress(100, (int) (((double) (i + 1) /
                            this.installed_overlays.size()) * 100), false);

                    switch (this.upgrade_mode) {
                        case APP_UPGRADE:
                            this.mBuilder.setContentText(
                                    Packages.getPackageName(this.context, this.package_name) + " (" +
                                            Packages.getPackageName(
                                                    this.context,
                                                    Packages.getOverlayParent(
                                                            this.context,
                                                            this.installed_overlays.get(i))
                                            ) + ")");
                            break;
                        case THEME_UPGRADE:
                            this.mBuilder.setContentText(
                                    Packages.getPackageName(this.context,
                                            Packages.getOverlayTarget(
                                                    this.context,
                                                    this.installed_overlays.get(i))));
                            this.mBuilder.setLargeIcon(
                                    Packages.getBitmapFromVector(
                                            Packages.getAppIcon(
                                                    this.context,
                                                    Packages.getOverlayTarget(
                                                            this.context,
                                                            this.installed_overlays.get(i)))));
                            break;
                    }
                    this.mNotifyManager.notify(this.id, this.mBuilder.build());

                    final String theme = Packages.getOverlayMetadata(this.context,
                            this.installed_overlays.get(i), metadataOverlayParent);

                    Boolean encrypted = false;
                    final String encrypt_check =
                            Packages.getOverlayMetadata(this.context, theme, metadataEncryption);

                    if ((encrypt_check != null) && encrypt_check.equals(metadataEncryptionValue)) {
                        Log.d(TAG, "This overlay for " +
                                Packages.getPackageName(this.context, theme) +
                                " is encrypted, passing handshake to the theme package...");
                        encrypted = true;

                        Theming.getThemeKeys(this.context, theme);

                        this.keyRetrieval = new KeyRetrieval();
                        this.localBroadcastManager.registerReceiver(this.keyRetrieval,
                                new IntentFilter(KEY_RETRIEVAL));


                        this.handler.postDelayed(this.runnable, 100);
                        int counter = 0;
                        while ((this.securityIntent == null) && (counter < 5)) {
                            try {
                                Thread.sleep(500);
                            } catch (final InterruptedException e) {
                                e.printStackTrace();
                            }
                            counter++;
                        }
                        if (counter > 5) {
                            Log.e(TAG, "Could not receive handshake in time...");
                            return null;
                        }

                        if (this.securityIntent != null) {
                            try {
                                final byte[] encryption_key =
                                        this.securityIntent.getByteArrayExtra("encryption_key");
                                final byte[] iv_encrypt_key =
                                        this.securityIntent.getByteArrayExtra("iv_encrypt_key");

                                this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                this.cipher.init(
                                        Cipher.DECRYPT_MODE,
                                        new SecretKeySpec(encryption_key, "AES"),
                                        new IvParameterSpec(iv_encrypt_key)
                                );
                            } catch (final Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                    }

                    Resources themeResources = null;
                    try {
                        themeResources = this.context.getPackageManager()
                                .getResourcesForApplication(theme);
                    } catch (final PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    assert themeResources != null;
                    final AssetManager themeAssetManager = themeResources.getAssets();

                    final String target = Packages.getOverlayMetadata(
                            this.context, this.installed_overlays.get(i), metadataOverlayTarget);

                    final String type1a = Packages.getOverlayMetadata(
                            this.context, this.installed_overlays.get(i), metadataOverlayType1a);
                    final String type1b = Packages.getOverlayMetadata(
                            this.context, this.installed_overlays.get(i), metadataOverlayType1b);
                    final String type1c = Packages.getOverlayMetadata(
                            this.context, this.installed_overlays.get(i), metadataOverlayType1c);
                    final String type2 = Packages.getOverlayMetadata(
                            this.context, this.installed_overlays.get(i), metadataOverlayType2);
                    final String type3 = Packages.getOverlayMetadata(
                            this.context, this.installed_overlays.get(i), metadataOverlayType3);
                    final String type4 = Packages.getOverlayMetadata(
                            this.context, this.installed_overlays.get(i), metadataOverlayType4);

                    if ((type1a != null) && type1a.contains("overlays/")) return null;
                    if ((type1b != null) && type1b.contains("overlays/")) return null;
                    if ((type1c != null) && type1c.contains("overlays/")) return null;
                    if ((type2 != null) && type2.contains("overlays/")) return null;
                    if ((type3 != null) && type3.contains("overlays/")) return null;

                    final String type1aDir = "overlays/" + target + "/type1a_" + type1a +
                            (encrypted ? ".xml.enc" : ".xml");
                    final String type1bDir = "overlays/" + target + "/type1b_" + type1b +
                            (encrypted ? ".xml.enc" : ".xml");
                    final String type1cDir = "overlays/" + target + "/type1c_" + type1c +
                            (encrypted ? ".xml.enc" : ".xml");
                    final String type2Dir = "overlays/" + target + "/type2_" + type2;
                    final String type3Dir = "overlays/" + target + "/type3_" + type3;

                    final String additional_variant = (((type2 != null) && !type2.isEmpty()) ?
                            type2Dir.split("/")[2].substring(6) : null);
                    final String base_variant = (((type3Dir != null) && !type3Dir.isEmpty()) ?
                            type3Dir.split("/")[2].substring(6) : null);

                    // Prenotions
                    final String suffix = (((type3 != null) && !type3.isEmpty()) ?
                            ("/" + type3Dir) : "/res");
                    final String workingDirectory = this.context.getCacheDir().getAbsolutePath() +
                            SUBSTRATUM_BUILDER_CACHE.substring(0,
                                    SUBSTRATUM_BUILDER_CACHE.length() - 1);
                    final File created = new File(workingDirectory);
                    if (created.exists()) {
                        FileOperations.delete(this.context, created.getAbsolutePath());
                        FileOperations.createNewFolder(this.context, created.getAbsolutePath());
                    } else {
                        FileOperations.createNewFolder(this.context, created.getAbsolutePath());
                    }

                    // Handle the resource folder
                    final String listDir = overlaysDir + "/" +
                            (this.upgrade_mode.equals(APP_UPGRADE) ?
                                    this.package_name :
                                    Packages.getOverlayTarget(this.context, this.installed_overlays.get(i))
                            ) + suffix;
                    FileOperations.copyFileOrDir(
                            themeAssetManager,
                            listDir,
                            workingDirectory + suffix,
                            listDir,
                            (encrypted ? this.cipher : null));
                    if ((type2 != null) && !type2.isEmpty()) {
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                listDir,
                                workingDirectory + "/type2_" + type2,
                                listDir,
                                (encrypted ? this.cipher : null));
                    }

                    // Handle the types
                    if ((type1a != null) && !type1a.isEmpty()) {
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                type1aDir,
                                workingDirectory + suffix + "/values/type1a.xml",
                                type1aDir,
                                (encrypted ? this.cipher : null));
                    }
                    if ((type1b != null) && !type1b.isEmpty()) {
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                type1bDir,
                                workingDirectory + suffix + "/values/type1b.xml",
                                type1bDir,
                                (encrypted ? this.cipher : null));
                    }
                    if ((type1c != null) && !type1c.isEmpty()) {
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                type1cDir,
                                workingDirectory + suffix + "/values/type1c.xml",
                                type1cDir,
                                (encrypted ? this.cipher : null));
                    }

                    if ((type4 != null) && !type4.isEmpty()) {
                        final String type4Dir = "overlays/" + target + "/type4_" + type4;
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                type4Dir,
                                workingDirectory + "/assets",
                                type1cDir,
                                (encrypted ? this.cipher : null));
                    }

                    final File workDir = new File(this.context.getCacheDir().getAbsolutePath() +
                            SUBSTRATUM_BUILDER_CACHE);
                    if (!workDir.exists() && !workDir.mkdirs())
                        Log.e(TAG, "Could not make cache directory...");

                    final String packageName =
                            (type1a != null && !type1a.isEmpty() ?
                                    type1a.replaceAll("\\s+", "") : "") +
                                    (type1b != null && !type1b.isEmpty() ?
                                            type1b.replaceAll("\\s+", "") : "") +
                                    (type1c != null && !type1c.isEmpty() ?
                                            type1c.replaceAll("\\s+", "") : "") +
                                    (type2 != null && !type2.isEmpty() ?
                                            type2.replaceAll("\\s+", "") : "") +
                                    (type3 != null && !type3.isEmpty() ?
                                            type3.replaceAll("\\s+", "") : "") +
                                    (type4 != null && !type4.isEmpty() ?
                                            type4.replaceAll("\\s+", "") : "");

                    final SubstratumBuilder sb = new SubstratumBuilder();
                    sb.beginAction(
                            this.context,
                            (this.upgrade_mode.equals(APP_UPGRADE) ?
                                    this.package_name :
                                    Packages.getOverlayTarget(
                                            this.context,
                                            this.installed_overlays.get(i))),
                            Packages.getPackageName(this.context, theme),
                            packageName,
                            additional_variant,
                            base_variant,
                            Packages.getAppVersion(this.context,
                                    Packages.getOverlayParent(this.context, this.installed_overlays.get(i))),
                            Systems.checkOMS(this.context),
                            theme,
                            suffix,
                            type1a,
                            type1b,
                            type1c,
                            type2,
                            type3,
                            type4,
                            this.installed_overlays.get(i),
                            true
                    );
                    if (sb.has_errored_out) {
                        this.errored_packages.add(this.installed_overlays.get(i));
                        if (sb.getErrorLogs() != null) {
                            if (this.error_logs.length() == 0)
                                this.error_logs.append(sb.getErrorLogs());
                            else
                                this.error_logs.append("\n").append(sb.getErrorLogs());
                        }
                    }

                    if (encrypted) {
                        try {
                            this.localBroadcastManager.unregisterReceiver(this.keyRetrieval);
                        } catch (final IllegalArgumentException e) {
                            // Unregistered already
                        }
                    }
                }
            }
            return null;
        }

        class KeyRetrieval extends BroadcastReceiver {

            @Override
            public void onReceive(final Context context, final Intent intent) {
                OverlayUpdate.this.securityIntent = intent;
            }
        }
    }

    static class UpdaterLogs extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction() != null) {
                if ("Updater.LOGS".equals(intent.getAction())) {
                    if (intent.getStringExtra("error_logs") != null)
                        this.invokeLogCharDialog(context, intent.getStringExtra("error_logs"));
                }
            }
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        }

        public void invokeLogCharDialog(final Context context, final String error_logs) {
            final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context)
                    .setTitle(R.string.logcat_dialog_title)
                    .setMessage("\n" + error_logs)
                    .setNeutralButton(R.string
                            .customactivityoncrash_error_activity_error_details_close, null)
                    .setNegativeButton(R.string
                                    .customactivityoncrash_error_activity_error_details_copy,
                            (dialog1, which) -> References.copyToClipboard(context,
                                    "substratum_log",
                                    error_logs));
            builder.show();
        }
    }
}