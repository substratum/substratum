/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.services.packages;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.Theming;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.compilers.SubstratumBuilder;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static projekt.substratum.common.Internal.CIPHER_ALGORITHM;
import static projekt.substratum.common.Internal.ENCRYPTED_FILE_EXTENSION;
import static projekt.substratum.common.Internal.ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.IV_ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.OVERLAYS_DIR;
import static projekt.substratum.common.Internal.SECRET_KEY_SPEC;
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
    private static final String APP_UPGRADE = "AppUpgrade";
    private static final String THEME_UPGRADE = "ThemeUpgrade";
    private static final Integer APP_UPGRADE_NOTIFICATION_ID = 24768941;
    private static final Integer THEME_UPGRADE_NOTIFICATION_ID = 13573743;
    private static final SharedPreferences prefs = Substratum.getPreferences();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Systems.isNewSamsungDeviceAndromeda(context))
            return;
        if (intent.getData() != null &&
                PACKAGE_ADDED.equals(intent.getAction())) {

            String packageName = intent.getData().toString().substring(8);

            if (ThemeManager.isOverlay(context, intent.getData().toString().substring(8)) ||
                    (Packages.getOverlayMetadata(
                            context,
                            intent.getData().toString().substring(8),
                            metadataOverlayDevice) != null)) {
                return;
            }

            // Let's start the intent filter
            UpdaterLogs updaterLogs = new UpdaterLogs();
            LocalBroadcastManager.getInstance(context).registerReceiver(updaterLogs,
                    new IntentFilter("Updater.LOGS"));

            // When the package is being updated, continue.
            boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            if (replacing && !Packages.getThemesArray(context).contains(packageName)) {
                // When the package is replacing, and also not a theme, update the overlays too!
                boolean toUpdate = prefs.getBoolean("overlay_updater", false);
                if (!toUpdate) return;
                new OverlayUpdate(
                        context,
                        packageName,
                        APP_UPGRADE,
                        APP_UPGRADE_NOTIFICATION_ID
                ).execute(APP_UPGRADE);
            } else if (replacing && Packages.getThemesArray(context).contains(packageName)) {
                // When the package is replacing, and also a theme, update the overlays too!
                boolean toUpdate = prefs.getBoolean("theme_updater", false);
                if (!toUpdate) return;
                new OverlayUpdate(
                        context,
                        packageName,
                        THEME_UPGRADE,
                        THEME_UPGRADE_NOTIFICATION_ID
                ).execute(APP_UPGRADE);
            }
        }
    }

    private static class OverlayUpdate extends AsyncTask<String, Integer, String> {

        final int notificationPriority = Notification.PRIORITY_MAX;
        @SuppressLint("StaticFieldLeak")
        private final Context context;
        private final String packageName;
        private final StringBuilder errorLogs = new StringBuilder();
        private final int id;
        private final Handler handler = new Handler();
        private NotificationManager notificationManager;
        private NotificationCompat.Builder builder;
        private List<String> installedOverlays;
        private List<String> erroredPackages;
        private LocalBroadcastManager localBroadcastManager;
        private KeyRetrieval keyRetrieval;
        private Intent securityIntent;
        private final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Substratum.log(TAG, "Waiting for encryption key handshake approval...");
                if (securityIntent != null) {
                    Substratum.log(TAG, "Encryption key handshake approved!");
                    handler.removeCallbacks(runnable);
                } else {
                    Substratum.log(TAG, "Encryption key still null...");
                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.postDelayed(this, 100L);
                }
            }
        };
        private Cipher cipher;
        private String upgradeMode;

        OverlayUpdate(Context context, String packageName, String mode, final
        int id) {
            super();
            this.context = context;
            this.packageName = packageName;
            this.upgradeMode = mode;
            this.id = id;
        }

        @Override
        protected void onPreExecute() {
            switch (upgradeMode) {
                case APP_UPGRADE:
                    installedOverlays = ThemeManager.listOverlaysForTarget(context,
                            packageName);
                    break;
                case THEME_UPGRADE:
                    installedOverlays = ThemeManager.listOverlaysByTheme(context, this
                            .packageName);
                    break;
            }
            if ((upgradeMode != null) && upgradeMode != null && !upgradeMode
                    .isEmpty() && !installedOverlays.isEmpty()) {
                erroredPackages = new ArrayList<>();
                notificationManager = (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                builder = new NotificationCompat.Builder(context,
                        DEFAULT_NOTIFICATION_CHANNEL_ID);
                String format = String.format(
                        context.getString(R.string.notification_initial_title_upgrade_intent),
                        Packages.getPackageName(context, packageName));
                builder.setContentTitle(format)
                        .setProgress(100, 0, true)
                        .setLargeIcon(Packages.getBitmapFromVector(
                                Packages.getAppIcon(
                                        context,
                                        packageName)))
                        .setSmallIcon(android.R.drawable.ic_popup_sync)
                        .setPriority(notificationPriority)
                        .setOngoing(true);
                notificationManager.notify(id, builder.build());

                localBroadcastManager = LocalBroadcastManager.getInstance(context);

            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (!installedOverlays.isEmpty()) {
                notificationManager.cancel(id);
                builder.setAutoCancel(true);
                builder.setProgress(0, 0, false);
                builder.setOngoing(false);
                builder.setLargeIcon(Packages.getBitmapFromVector(
                        Packages.getAppIcon(
                                context,
                                packageName)));

                if (!erroredPackages.isEmpty()) {
                    builder.setSmallIcon(R.drawable.notification_warning_icon);
                    builder.setContentTitle(String.format(
                            context.getString(R.string
                                    .notification_done_upgrade_title_warning),
                            Packages.getPackageName(context, packageName)));

                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < erroredPackages.size(); i++) {
                        stringBuilder.append(erroredPackages.get(i));
                        if ((erroredPackages.size() - 1) > i) {
                            stringBuilder.append('\n');
                        }
                    }
                    builder.setContentText(String.format(
                            context.getString(R.string.notification_done_upgrade_title_failed),
                            stringBuilder.toString()));
                    Intent intent = new Intent("Updater.LOGS");
                    intent.putExtra("error_logs", errorLogs.toString());
                    PendingIntent pintent = PendingIntent.getActivity(context, 0,
                            intent, PendingIntent.FLAG_ONE_SHOT);
                    builder.setContentIntent(pintent);
                } else {
                    builder.setSmallIcon(R.drawable.notification_success_icon);
                    String format = String.format(
                            context.getString(R.string.notification_done_upgrade_title),
                            Packages.getPackageName(context, packageName));
                    builder.setContentTitle(format);
                    switch (upgradeMode) {
                        case APP_UPGRADE:
                            builder.setContentText(
                                    context.getString(R.string.notification_done_update_text));
                            break;
                        case THEME_UPGRADE:
                            builder.setContentText(
                                    context.getString(R.string
                                            .notification_done_upgrade_text));
                            break;
                    }
                }
                erroredPackages = new ArrayList<>();
                upgradeMode = "";
                notificationManager.notify(id, builder.build());
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            if (!installedOverlays.isEmpty()) {
                Substratum.log(TAG, '\'' + packageName +
                        "' was just updated with overlays present, updating...");
                for (int i = 0; i < installedOverlays.size(); i++) {
                    Substratum.log(TAG, "Current overlay found in stash: " + installedOverlays.get(i));

                    builder.setProgress(100, (int) (((double) (i + 1) /
                            (double) installedOverlays.size()) * 100.0), false);

                    switch (upgradeMode) {
                        case APP_UPGRADE:
                            builder.setContentText(
                                    Packages.getPackageName(context, packageName) + " " +
                                            "(" +
                                            Packages.getPackageName(
                                                    context,
                                                    Packages.getOverlayParent(
                                                            context,
                                                            installedOverlays.get(i))
                                            ) + ')');
                            break;
                        case THEME_UPGRADE:
                            builder.setContentText(
                                    Packages.getPackageName(context,
                                            Packages.getOverlayTarget(
                                                    context,
                                                    installedOverlays.get(i))));
                            builder.setLargeIcon(
                                    Packages.getBitmapFromVector(
                                            Packages.getAppIcon(
                                                    context,
                                                    Packages.getOverlayTarget(
                                                            context,
                                                            installedOverlays.get(i)))));
                            break;
                    }
                    notificationManager.notify(id, builder.build());

                    String theme = Packages.getOverlayMetadata(context,
                            installedOverlays.get(i), metadataOverlayParent);

                    boolean encrypted = false;
                    String encryptCheck =
                            Packages.getOverlayMetadata(context, theme, metadataEncryption);

                    if ((encryptCheck != null) && encryptCheck.equals(metadataEncryptionValue)) {
                        Substratum.log(TAG, "This overlay for " +
                                Packages.getPackageName(context, theme) +
                                " is encrypted, passing handshake to the theme package...");
                        encrypted = true;

                        Theming.getThemeKeys(context, theme);

                        keyRetrieval = new KeyRetrieval();
                        localBroadcastManager.registerReceiver(keyRetrieval,
                                new IntentFilter(KEY_RETRIEVAL));


                        handler.postDelayed(runnable, 100L);
                        int counter = 0;
                        while ((securityIntent == null) && (counter < 5)) {
                            try {
                                Thread.sleep(500L);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            counter++;
                        }
                        if (counter > 5) {
                            Log.e(TAG, "Could not receive handshake in time...");
                            return null;
                        }

                        if (securityIntent != null) {
                            try {
                                byte[] encryptionKey =
                                        securityIntent.getByteArrayExtra(ENCRYPTION_KEY_EXTRA);
                                byte[] ivEncryptKey =
                                        securityIntent.getByteArrayExtra(IV_ENCRYPTION_KEY_EXTRA);

                                cipher = Cipher.getInstance(CIPHER_ALGORITHM);
                                cipher.init(
                                        Cipher.DECRYPT_MODE,
                                        new SecretKeySpec(encryptionKey, SECRET_KEY_SPEC),
                                        new IvParameterSpec(ivEncryptKey)
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                    }

                    Resources themeResources = null;
                    try {
                        themeResources = context.getPackageManager()
                                .getResourcesForApplication(theme);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    assert themeResources != null;
                    AssetManager themeAssetManager = themeResources.getAssets();

                    String target = Packages.getOverlayMetadata(
                            context, installedOverlays.get(i), metadataOverlayTarget);
                    String type1a = Packages.getOverlayMetadata(
                            context, installedOverlays.get(i), metadataOverlayType1a);
                    String type1b = Packages.getOverlayMetadata(
                            context, installedOverlays.get(i), metadataOverlayType1b);
                    String type1c = Packages.getOverlayMetadata(
                            context, installedOverlays.get(i), metadataOverlayType1c);
                    String type2 = Packages.getOverlayMetadata(
                            context, installedOverlays.get(i), metadataOverlayType2);
                    String type3 = Packages.getOverlayMetadata(
                            context, installedOverlays.get(i), metadataOverlayType3);
                    String type4 = Packages.getOverlayMetadata(
                            context, installedOverlays.get(i), metadataOverlayType4);

                    if ((type1a != null) && type1a.contains("overlays/")) return null;
                    if ((type1b != null) && type1b.contains("overlays/")) return null;
                    if ((type1c != null) && type1c.contains("overlays/")) return null;
                    if ((type2 != null) && type2.contains("overlays/")) return null;
                    if ((type3 != null) && type3.contains("overlays/")) return null;

                    String type1aDir = "overlays/" + target + "/type1a_" + type1a +
                            (encrypted ? ".xml" + ENCRYPTED_FILE_EXTENSION : ".xml");
                    String type1bDir = "overlays/" + target + "/type1b_" + type1b +
                            (encrypted ? ".xml" + ENCRYPTED_FILE_EXTENSION : ".xml");
                    String type1cDir = "overlays/" + target + "/type1c_" + type1c +
                            (encrypted ? ".xml" + ENCRYPTED_FILE_EXTENSION : ".xml");
                    String type2Dir = "overlays/" + target + "/type2_" + type2;
                    String type3Dir = "overlays/" + target + "/type3_" + type3;

                    String additionalVariant = (((type2 != null) && !type2.isEmpty()) ?
                            type2Dir.split("/")[2].substring(6) : null);
                    String baseVariant = (((type3Dir != null) && !type3Dir.isEmpty()) ?
                            type3Dir.split("/")[2].substring(6) : null);

                    // Prenotions
                    String suffix = (((type3 != null) && !type3.isEmpty()) ?
                            ('/' + type3Dir) : "/res");
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
                    String listDir = OVERLAYS_DIR + '/' +
                            (upgradeMode.equals(APP_UPGRADE) ?
                                    packageName :
                                    Packages.getOverlayTarget(context, this
                                            .installedOverlays.get(i))
                            ) + suffix;
                    FileOperations.copyFileOrDir(
                            themeAssetManager,
                            listDir,
                            workingDirectory + suffix,
                            listDir,
                            (encrypted ? cipher : null));
                    if ((type2 != null) && !type2.isEmpty()) {
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                listDir,
                                workingDirectory + "/type2_" + type2,
                                listDir,
                                (encrypted ? cipher : null));
                    }

                    // Handle the types
                    if ((type1a != null) && !type1a.isEmpty()) {
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                type1aDir,
                                workingDirectory + suffix + "/values/type1a.xml",
                                type1aDir,
                                (encrypted ? cipher : null));
                    }
                    if ((type1b != null) && !type1b.isEmpty()) {
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                type1bDir,
                                workingDirectory + suffix + "/values/type1b.xml",
                                type1bDir,
                                (encrypted ? cipher : null));
                    }
                    if ((type1c != null) && !type1c.isEmpty()) {
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                type1cDir,
                                workingDirectory + suffix + "/values/type1c.xml",
                                type1cDir,
                                (encrypted ? cipher : null));
                    }

                    if ((type4 != null) && !type4.isEmpty()) {
                        String type4Dir = "overlays/" + target + "/type4_" + type4;
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                type4Dir,
                                workingDirectory + "/assets",
                                type1cDir,
                                (encrypted ? cipher : null));
                    }

                    File workDir = new File(context.getCacheDir().getAbsolutePath() +
                            SUBSTRATUM_BUILDER_CACHE);
                    if (!workDir.exists() && !workDir.mkdirs())
                        Log.e(TAG, "Could not make cache directory...");

                    String packageName =
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

                    SubstratumBuilder sb = new SubstratumBuilder();
                    sb.beginAction(
                            context,
                            (upgradeMode.equals(APP_UPGRADE) ?
                                    this.packageName :
                                    Packages.getOverlayTarget(
                                            context,
                                            installedOverlays.get(i))),
                            Packages.getPackageName(context, theme),
                            packageName,
                            additionalVariant,
                            baseVariant,
                            Packages.getAppVersion(context,
                                    Packages.getOverlayParent(context, this
                                            .installedOverlays.get(i))),
                            Systems.checkOMS(context),
                            theme,
                            suffix,
                            type1a,
                            type1b,
                            type1c,
                            type2,
                            type3,
                            type4,
                            installedOverlays.get(i),
                            true
                    );
                    if (sb.hasErroredOut) {
                        erroredPackages.add(installedOverlays.get(i));
                        if (sb.getErrorLogs() != null) {
                            if (errorLogs.length() == 0)
                                errorLogs.append(sb.getErrorLogs());
                            else
                                errorLogs.append('\n').append(sb.getErrorLogs());
                        }
                    }

                    if (encrypted) {
                        try {
                            localBroadcastManager.unregisterReceiver(keyRetrieval);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
            return null;
        }

        class KeyRetrieval extends BroadcastReceiver {

            @Override
            public void onReceive(Context context, Intent intent) {
                securityIntent = intent;
            }
        }
    }

    static class UpdaterLogs extends BroadcastReceiver {
        static void invokeLogCharDialog(Context context, String errorLogs) {
            AlertDialog.Builder builder = new AlertDialog.Builder
                    (context)
                    .setTitle(R.string.logcat_dialog_title)
                    .setMessage('\n' + errorLogs)
                    .setNeutralButton(R.string
                            .customactivityoncrash_error_activity_error_details_close, null)
                    .setNegativeButton(R.string
                                    .customactivityoncrash_error_activity_error_details_copy,
                            (dialog1, which) -> References.copyToClipboard(context,
                                    "substratum_log",
                                    errorLogs));
            builder.show();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if ("Updater.LOGS".equals(intent.getAction())) {
                    if (intent.getStringExtra("error_logs") != null)
                        UpdaterLogs.invokeLogCharDialog(context, intent.getStringExtra
                                ("error_logs"));
                }
            }
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        }
    }
}