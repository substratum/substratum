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

    @SuppressWarnings({"ConstantConditions"})
    @Override
    public void onReceive(Context context, Intent intent) {
        if (PACKAGE_ADDED.equals(intent.getAction())) {
            String package_name = intent.getData().toString().substring(8);

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
            Boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (replacing && !Packages.getThemesArray(context).contains(package_name)) {
                // When the package is replacing, and also not a theme, update the overlays too!
                Boolean to_update = prefs.getBoolean("overlay_updater", false);
                if (!to_update) return;
                new OverlayUpdate(
                        context,
                        package_name,
                        APP_UPGRADE,
                        APP_UPGRADE_NOTIFICATION_ID
                ).execute(APP_UPGRADE);
            } else if (replacing && Packages.getThemesArray(context).contains(package_name)) {
                // When the package is replacing, and also a theme, update the overlays too!
                Boolean to_update = prefs.getBoolean("theme_updater", false);
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
        @SuppressLint("StaticFieldLeak")
        private final Context context;
        private final String package_name;
        private final StringBuilder error_logs = new StringBuilder();
        private final int id;
        private final Handler handler = new Handler();
        private NotificationManager mNotifyManager;
        private NotificationCompat.Builder mBuilder;
        private List<String> installed_overlays;
        private List<String> errored_packages;
        private LocalBroadcastManager localBroadcastManager;
        private KeyRetrieval keyRetrieval;
        private Intent securityIntent;
        private final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Waiting for encryption key handshake approval...");
                if (securityIntent != null) {
                    Log.d(TAG, "Encryption key handshake approved!");
                    handler.removeCallbacks(runnable);
                } else {
                    Log.d(TAG, "Encryption key still null...");
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
        private String upgrade_mode;

        OverlayUpdate(Context context, String package_name, String mode, final
        int id) {
            super();
            this.context = context;
            this.package_name = package_name;
            this.upgrade_mode = mode;
            this.id = id;
        }

        @Override
        protected void onPreExecute() {
            switch (upgrade_mode) {
                case APP_UPGRADE:
                    installed_overlays = ThemeManager.listOverlaysForTarget(context,
                            package_name);
                    break;
                case THEME_UPGRADE:
                    installed_overlays = ThemeManager.listOverlaysByTheme(context, this
                            .package_name);
                    break;
            }
            if ((upgrade_mode != null) && upgrade_mode != null && !upgrade_mode
                    .isEmpty() && !installed_overlays.isEmpty()) {
                errored_packages = new ArrayList<>();
                mNotifyManager = (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                mBuilder = new NotificationCompat.Builder(context,
                        DEFAULT_NOTIFICATION_CHANNEL_ID);
                String format = String.format(
                        context.getString(R.string.notification_initial_title_upgrade_intent),
                        Packages.getPackageName(context, package_name));
                mBuilder.setContentTitle(format)
                        .setProgress(100, 0, true)
                        .setLargeIcon(Packages.getBitmapFromVector(
                                Packages.getAppIcon(
                                        context,
                                        package_name)))
                        .setSmallIcon(android.R.drawable.ic_popup_sync)
                        .setPriority(notification_priority)
                        .setOngoing(true);
                mNotifyManager.notify(id, mBuilder.build());

                localBroadcastManager = LocalBroadcastManager.getInstance(context);

            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (!installed_overlays.isEmpty()) {
                mNotifyManager.cancel(id);
                mBuilder.setAutoCancel(true);
                mBuilder.setProgress(0, 0, false);
                mBuilder.setOngoing(false);
                mBuilder.setLargeIcon(Packages.getBitmapFromVector(
                        Packages.getAppIcon(
                                context,
                                package_name)));

                if (!errored_packages.isEmpty()) {
                    mBuilder.setSmallIcon(R.drawable.notification_warning_icon);
                    String format = String.format(
                            context.getString(R.string
                                    .notification_done_upgrade_title_warning),
                            Packages.getPackageName(context, package_name));
                    mBuilder.setContentTitle(format);

                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < errored_packages.size(); i++) {
                        stringBuilder.append(errored_packages.get(i));
                        if ((errored_packages.size() - 1) > i) {
                            stringBuilder.append('\n');
                        }
                    }
                    String format2 = String.format(
                            context.getString(R.string.notification_done_upgrade_title_failed),
                            stringBuilder.toString());
                    mBuilder.setContentText(format2);
                    Intent intent = new Intent("Updater.LOGS");
                    intent.putExtra("error_logs", error_logs.toString());
                    PendingIntent pintent = PendingIntent.getActivity(context, 0,
                            intent, PendingIntent.FLAG_ONE_SHOT);
                    mBuilder.setContentIntent(pintent);
                } else {
                    mBuilder.setSmallIcon(R.drawable.notification_success_icon);
                    String format = String.format(
                            context.getString(R.string.notification_done_upgrade_title),
                            Packages.getPackageName(context, package_name));
                    mBuilder.setContentTitle(format);
                    switch (upgrade_mode) {
                        case APP_UPGRADE:
                            mBuilder.setContentText(
                                    context.getString(R.string.notification_done_update_text));
                            break;
                        case THEME_UPGRADE:
                            mBuilder.setContentText(
                                    context.getString(R.string
                                            .notification_done_upgrade_text));
                            break;
                    }
                }
                errored_packages = new ArrayList<>();
                upgrade_mode = "";
                mNotifyManager.notify(id, mBuilder.build());
            }
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(String... sUrl) {
            if (!installed_overlays.isEmpty()) {
                Log.d(TAG, '\'' + package_name +
                        "' was just updated with overlays present, updating...");
                for (int i = 0; i < installed_overlays.size(); i++) {
                    Log.d(TAG, "Current overlay found in stash: " + installed_overlays.get(i));

                    mBuilder.setProgress(100, (int) (((double) (i + 1) /
                            (double) installed_overlays.size()) * 100.0), false);

                    switch (upgrade_mode) {
                        case APP_UPGRADE:
                            mBuilder.setContentText(
                                    Packages.getPackageName(context, package_name) + " " +
                                            "(" +
                                            Packages.getPackageName(
                                                    context,
                                                    Packages.getOverlayParent(
                                                            context,
                                                            installed_overlays.get(i))
                                            ) + ')');
                            break;
                        case THEME_UPGRADE:
                            mBuilder.setContentText(
                                    Packages.getPackageName(context,
                                            Packages.getOverlayTarget(
                                                    context,
                                                    installed_overlays.get(i))));
                            mBuilder.setLargeIcon(
                                    Packages.getBitmapFromVector(
                                            Packages.getAppIcon(
                                                    context,
                                                    Packages.getOverlayTarget(
                                                            context,
                                                            installed_overlays.get(i)))));
                            break;
                    }
                    mNotifyManager.notify(id, mBuilder.build());

                    String theme = Packages.getOverlayMetadata(context,
                            installed_overlays.get(i), metadataOverlayParent);

                    Boolean encrypted = false;
                    String encrypt_check =
                            Packages.getOverlayMetadata(context, theme, metadataEncryption);

                    if ((encrypt_check != null) && encrypt_check.equals(metadataEncryptionValue)) {
                        Log.d(TAG, "This overlay for " +
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
                                byte[] encryption_key =
                                        securityIntent.getByteArrayExtra(ENCRYPTION_KEY_EXTRA);
                                byte[] iv_encrypt_key =
                                        securityIntent.getByteArrayExtra(IV_ENCRYPTION_KEY_EXTRA);

                                cipher = Cipher.getInstance(CIPHER_ALGORITHM);
                                cipher.init(
                                        Cipher.DECRYPT_MODE,
                                        new SecretKeySpec(encryption_key, SECRET_KEY_SPEC),
                                        new IvParameterSpec(iv_encrypt_key)
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
                            context, installed_overlays.get(i), metadataOverlayTarget);

                    String type1a = Packages.getOverlayMetadata(
                            context, installed_overlays.get(i), metadataOverlayType1a);
                    String type1b = Packages.getOverlayMetadata(
                            context, installed_overlays.get(i), metadataOverlayType1b);
                    String type1c = Packages.getOverlayMetadata(
                            context, installed_overlays.get(i), metadataOverlayType1c);
                    String type2 = Packages.getOverlayMetadata(
                            context, installed_overlays.get(i), metadataOverlayType2);
                    String type3 = Packages.getOverlayMetadata(
                            context, installed_overlays.get(i), metadataOverlayType3);
                    String type4 = Packages.getOverlayMetadata(
                            context, installed_overlays.get(i), metadataOverlayType4);

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

                    String additional_variant = (((type2 != null) && !type2.isEmpty()) ?
                            type2Dir.split("/")[2].substring(6) : null);
                    String base_variant = (((type3Dir != null) && !type3Dir.isEmpty()) ?
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
                            (upgrade_mode.equals(APP_UPGRADE) ?
                                    package_name :
                                    Packages.getOverlayTarget(context, this
                                            .installed_overlays.get(i))
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
                            (upgrade_mode.equals(APP_UPGRADE) ?
                                    package_name :
                                    Packages.getOverlayTarget(
                                            context,
                                            installed_overlays.get(i))),
                            Packages.getPackageName(context, theme),
                            packageName,
                            additional_variant,
                            base_variant,
                            Packages.getAppVersion(context,
                                    Packages.getOverlayParent(context, this
                                            .installed_overlays.get(i))),
                            Systems.checkOMS(context),
                            theme,
                            suffix,
                            type1a,
                            type1b,
                            type1c,
                            type2,
                            type3,
                            type4,
                            installed_overlays.get(i),
                            true
                    );
                    if (sb.has_errored_out) {
                        errored_packages.add(installed_overlays.get(i));
                        if (sb.getErrorLogs() != null) {
                            if (error_logs.length() == 0)
                                error_logs.append(sb.getErrorLogs());
                            else
                                error_logs.append('\n').append(sb.getErrorLogs());
                        }
                    }

                    if (encrypted) {
                        try {
                            localBroadcastManager.unregisterReceiver(keyRetrieval);
                        } catch (IllegalArgumentException e) {
                            // Unregistered already
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
        public static void invokeLogCharDialog(Context context, String error_logs) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder
                    (context)
                    .setTitle(R.string.logcat_dialog_title)
                    .setMessage('\n' + error_logs)
                    .setNeutralButton(R.string
                            .customactivityoncrash_error_activity_error_details_close, null)
                    .setNegativeButton(R.string
                                    .customactivityoncrash_error_activity_error_details_copy,
                            (dialog1, which) -> References.copyToClipboard(context,
                                    "substratum_log",
                                    error_logs));
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