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

package projekt.substratum.tabs;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import org.zeroturnaround.zip.commons.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.adapters.tabs.overlays.OverlaysAdapter;
import projekt.substratum.adapters.tabs.overlays.OverlaysItem;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.services.notification.NotificationButtonReceiver;
import projekt.substratum.util.compilers.CacheCreator;
import projekt.substratum.util.compilers.SubstratumBuilder;
import projekt.substratum.util.files.Root;

import static android.content.Context.NOTIFICATION_SERVICE;
import static projekt.substratum.InformationActivity.currentShownLunchBar;
import static projekt.substratum.common.References.DEFAULT_NOTIFICATION_CHANNEL_ID;
import static projekt.substratum.common.References.REFRESH_WINDOW_DELAY;
import static projekt.substratum.common.References.checkOMS;
import static projekt.substratum.common.References.checkThemeInterfacer;

public class OverlayFunctions {
    private static final String TAG = "Substratum OverlayFunctions";
    private ProgressDialog pDialog = null;

    public OverlayFunctions() {

    }

    static class getThemeCache extends AsyncTask<String, Integer, String> {
        WeakReference<Overlays> ref;

        getThemeCache(Overlays overlays) {
            ref = new WeakReference<>(overlays);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Overlays overlays = ref.get();
            if (overlays != null) {
                Context context = overlays.getActivity();
                overlays.final_runner = new ArrayList<>();
                overlays.late_install = new ArrayList<>();

                if (!overlays.enable_mode && !overlays.disable_mode && !overlays.enable_disable_mode) {
                    int notification_priority = Notification.PRIORITY_MAX;

                    // Create an Intent for the BroadcastReceiver
                    Intent buttonIntent = new Intent(context, NotificationButtonReceiver.class);

                    // Create the PendingIntent
                    PendingIntent btPendingIntent = PendingIntent.getBroadcast(
                            context, 0, buttonIntent, 0);
                    PendingIntent resultPendingIntent = PendingIntent.getActivity(
                            context, 0, new Intent(), 0);

                    // This is the time when the notification should be shown on the user's screen
                    overlays.mNotifyManager =
                            (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                    overlays.mBuilder = new NotificationCompat.Builder(context,
                            References.DEFAULT_NOTIFICATION_CHANNEL_ID);
                    overlays.mBuilder.setContentTitle(
                            context.getString(R.string.notification_initial_title))
                            .setProgress(100, 0, true)
                            .addAction(android.R.color.transparent, context.getString(R.string
                                    .notification_hide), btPendingIntent)
                            .setSmallIcon(android.R.drawable.ic_popup_sync)
                            .setPriority(notification_priority)
                            .setContentIntent(resultPendingIntent)
                            .setOngoing(true);
                    overlays.mNotifyManager.notify(overlays.id, overlays.mBuilder.build());
                    overlays.mProgressDialog = null;
                    overlays.mProgressDialog = new ProgressDialog(context,
                            R.style.SubstratumBuilder_ActivityTheme);
                    overlays.mProgressDialog.setIndeterminate(false);
                    overlays.mProgressDialog.setCancelable(false);
                    overlays.mProgressDialog.show();
                    overlays.mProgressDialog.setContentView(R.layout.compile_dialog_loader);
                    if (overlays.mProgressDialog.getWindow() != null) {
                        overlays.mProgressDialog.getWindow().addFlags(
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }

                    final float radius = 5;
                    final View decorView = overlays.getActivity().getWindow().getDecorView();
                    final ViewGroup rootView = decorView.findViewById(android.R.id.content);

                    final Drawable windowBackground = decorView.getBackground();

                    BlurView blurView = overlays.mProgressDialog.findViewById(R.id.blurView);

                    if (rootView != null) {
                        blurView.setupWith(rootView)
                                .windowBackground(windowBackground)
                                .blurAlgorithm(new RenderScriptBlur(context))
                                .blurRadius(radius);
                    }

                    overlays.dialogProgress = overlays.mProgressDialog.findViewById(
                            R.id.loading_bar);
                    overlays.dialogProgress.setProgressTintList(
                            ColorStateList.valueOf(context.getColor(
                                    R.color.compile_dialog_wave_color)));
                    overlays.dialogProgress.setIndeterminate(false);

                    overlays.loader_string = overlays.mProgressDialog.findViewById(R.id.title);
                    overlays.loader_string.setText(context.getResources().getString(
                            R.string.sb_phase_1_loader));
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Overlays overlays = ref.get();
            if (overlays != null) {
                Phase3_mainFunction phase3_mainFunction = new Phase3_mainFunction(overlays);
                if (result != null) {
                    phase3_mainFunction.execute(result);
                } else {
                    phase3_mainFunction.execute("");
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Context context = overlays.getActivity();
                if (!overlays.enable_mode && !overlays.disable_mode && !overlays.enable_disable_mode) {
                    // Initialize Substratum cache with theme only if permitted
                    if (References.isCachingEnabled(context) && !overlays.has_initialized_cache) {
                        Log.d(Overlays.TAG,
                                "Decompiling and initializing work area with the " +
                                        "selected theme's assets...");
                        overlays.sb = new SubstratumBuilder();

                        File versioning = new File(context.getCacheDir().getAbsoluteFile() +
                                References.SUBSTRATUM_BUILDER_CACHE + overlays.theme_pid +
                                "/substratum.xml");
                        if (versioning.exists()) {
                            overlays.has_initialized_cache = true;
                        } else {
                            new CacheCreator().initializeCache(context, overlays.theme_pid,
                                    overlays.cipher);
                            overlays.has_initialized_cache = true;
                        }
                    } else {
                        try {
                            Resources themeResources = context.getPackageManager()
                                    .getResourcesForApplication(overlays.theme_pid);
                            overlays.themeAssetManager = themeResources.getAssets();
                        } catch (PackageManager.NameNotFoundException e) {
                            // Suppress exception
                        }
                        Log.d(Overlays.TAG, "Work area is ready to be compiled!");
                    }
                    if (sUrl[0].length() != 0) {
                        return sUrl[0];
                    } else {
                        return null;
                    }
                }
            }
            return null;
        }

        public void clear() {
            if (ref != null) ref.clear();
        }
    }

    static class Phase3_mainFunction extends AsyncTask<String, Integer, String> {
        private WeakReference<Overlays> ref;

        Phase3_mainFunction(Overlays overlays) {
            ref = new WeakReference<>(overlays);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(Overlays.TAG,
                    "Substratum is proceeding with your actions and is now actively running...");
            Overlays overlays = ref.get();
            if (overlays != null) {
                Context context = overlays.getActivity();

                overlays.missingType3 = false;
                overlays.has_failed = false;
                overlays.fail_count = 0;
                overlays.error_logs = new StringBuilder();

                if (!overlays.enable_mode && !overlays.disable_mode && !overlays.enable_disable_mode) {
                    // Change title in preparation for loop to change subtext
                    if (overlays.checkActiveNotifications()) {
                        overlays.mBuilder
                                .setContentTitle(
                                        context.getString(R.string.notification_processing_n))
                                .setProgress(100, 0, false);
                        overlays.mNotifyManager.notify(overlays.id, overlays.mBuilder.build());
                    }
                    overlays.loader_string.setText(context.getResources().getString(
                            R.string.sb_phase_2_loader));
                } else {
                    overlays.progressBar.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            Overlays overlays = ref.get();
            if (overlays != null) {
                TextView textView = overlays.mProgressDialog.findViewById(R.id.current_object);
                textView.setText(overlays.current_dialog_overlay);
                double progress = (overlays.current_amount / overlays.total_amount) * 100;
                overlays.dialogProgress.setProgress((int) progress, true);
            }
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected void onPostExecute(String result) {
            // TODO: onPostExecute runs on UI thread, so move the hard job to doInBackground
            super.onPostExecute(result);
            Overlays overlays = ref.get();
            if (overlays != null) {
                Context context = overlays.getActivity();
                overlays.final_command = new ArrayList<>();

                // Check if not compile_enable_mode
                if (!overlays.compile_enable_mode) {
                    overlays.final_command.addAll(overlays.final_runner);
                } else {
                    // It's compile and enable mode, we have to first sort out all the
                    // "pm install"'s from the final_commands
                    overlays.final_command.addAll(overlays.final_runner);
                }

                if (!overlays.enable_mode && !overlays.disable_mode && !overlays.enable_disable_mode) {
                    new Phase4_finishUpdateFunction(overlays).execute();
                    if (overlays.has_failed) {
                        overlays.failedFunction(context);
                    } else {
                        // Restart SystemUI if an enabled SystemUI overlay is updated
                        if (checkOMS(context)) {
                            for (int i = 0; i < overlays.checkedOverlays.size(); i++) {
                                String targetOverlay = overlays.checkedOverlays.get(i)
                                        .getPackageName();
                                if (targetOverlay.equals("com.android.systemui")) {
                                    String packageName =
                                            overlays.checkedOverlays.get(i)
                                                    .getFullOverlayParameters();
                                    if (ThemeManager.isOverlayEnabled(context, packageName)) {
                                        if (ThemeManager.shouldRestartUI(context, packageName))
                                            ThemeManager.restartSystemUI(context);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    try {
                        context.unregisterReceiver(overlays.finishReceiver);
                    } catch (IllegalArgumentException e) {
                        // Suppress warning
                    }
                } else if (overlays.enable_mode) {
                    new Phase4_finishEnableFunction(overlays).execute();
                } else if (overlays.disable_mode) {
                    new Phase4_finishDisableFunction(overlays).execute();
                } else if (overlays.enable_disable_mode){
                    new Phase4_finishEnableDisableFunction(overlays).execute();
                }
                if (References.isSamsung(context) &&
                        overlays.late_install != null &&
                        overlays.late_install.size() > 0) {
                    if (Root.checkRootAccess() && Root.requestRootAccess()) {
                        overlays.progressBar.setVisibility(View.VISIBLE);
                        overlays.overlaysWaiting = overlays.late_install.size();
                        for (int i = 0; i < overlays.late_install.size(); i++) {
                            ThemeManager.installOverlay(
                                    overlays.getActivity(),
                                    overlays.late_install.get(i));
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = FileProvider.getUriForFile(
                                context,
                                context.getApplicationContext().getPackageName() + ".provider",
                                new File(overlays.late_install.get(0)));
                        intent.setDataAndType(
                                uri,
                                "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        overlays.startActivityForResult(intent, 2486);
                    }
                } else if (!References.checkOMS(context) &&
                        overlays.final_runner.size() == overlays.fail_count) {
                    final AlertDialog.Builder alertDialogBuilder =
                            new AlertDialog.Builder(context);
                    alertDialogBuilder
                            .setTitle(context.getString(R.string.legacy_dialog_soft_reboot_title));
                    alertDialogBuilder
                            .setMessage(context.getString(R.string.legacy_dialog_soft_reboot_text));
                    alertDialogBuilder
                            .setPositiveButton(android.R.string.ok,
                                    (dialog, id12) -> ElevatedCommands.reboot());
                    alertDialogBuilder
                            .setNegativeButton(R.string.remove_dialog_later,
                                    (dialog, id1) -> {
                                        overlays.progressBar.setVisibility(View.GONE);
                                        dialog.dismiss();
                                    });
                    alertDialogBuilder.setCancelable(false);
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
                overlays.is_overlay_active = false;
                overlays.mAdapter.notifyDataSetChanged();
                if (overlays.toggle_all.isChecked()) overlays.toggle_all.setChecked(false);
            }
        }


        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(String... sUrl) {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Context context = overlays.getActivity();
                String parsedVariant = sUrl[0].replaceAll("\\s+", "");
                String unparsedVariant = sUrl[0];
                overlays.failed_packages = new StringBuilder();
                if (overlays.mixAndMatchMode && !References.checkOMS(context)) {
                    String current_directory;
                    if (References.inNexusFilter()) {
                        current_directory = References.PIXEL_NEXUS_DIR;
                    } else {
                        current_directory = References.LEGACY_NEXUS_DIR;
                    }
                    File file = new File(current_directory);
                    if (file.exists()) {
                        FileOperations.mountRW();
                        FileOperations.delete(context, current_directory);
                    }
                }

                // Enable listener
                if ((References.checkThemeInterfacer(context) &&
                        !References.isBinderInterfacer(context)) ||
                        References.checkAndromeda(context)) {
                    overlays.finishReceiver = new Overlays.FinishReceiver(overlays);
                    IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
                    filter.addDataScheme("package");
                    context.registerReceiver(overlays.finishReceiver, filter);
                }

                overlays.total_amount = overlays.checkedOverlays.size();
                for (int i = 0; i < overlays.checkedOverlays.size(); i++) {
                    overlays.type1a = "";
                    overlays.type1b = "";
                    overlays.type1c = "";
                    overlays.type2 = "";
                    overlays.type3 = "";
                    overlays.type4 = "";

                    overlays.current_amount = i + 1;
                    String theme_name_parsed =
                            overlays.theme_name.replaceAll("\\s+", "")
                                    .replaceAll("[^a-zA-Z0-9]+", "");

                    String current_overlay = overlays.checkedOverlays.get(i).getPackageName();
                    overlays.current_dialog_overlay =
                            "'" + References.grabPackageName(context, current_overlay) + "'";

                    if (!overlays.enable_mode && !overlays.disable_mode
                            && !overlays.enable_disable_mode) {
                        publishProgress((int) overlays.current_amount);
                        if (overlays.compile_enable_mode) {
                            if (overlays.final_runner == null) {
                                overlays.final_runner = new ArrayList<>();
                            }
                            String package_name = overlays.checkedOverlays.get(i)
                                    .getFullOverlayParameters();
                            if (References.isPackageInstalled(context, package_name) ||
                                    overlays.compile_enable_mode) {
                                overlays.final_runner.add(package_name);
                            }
                        }
                        try {
                            String packageTitle = "";
                            if (References.allowedSystemUIOverlay(current_overlay)) {
                                switch (current_overlay) {
                                    case "com.android.systemui.headers":
                                        packageTitle = context.getString(R.string.systemui_headers);
                                        break;
                                    case "com.android.systemui.navbars":
                                        packageTitle = context.getString(R.string
                                                .systemui_navigation);
                                        break;
                                    case "com.android.systemui.statusbars":
                                        packageTitle = context.getString(R.string
                                                .systemui_statusbar);
                                        break;
                                    case "com.android.systemui.tiles":
                                        packageTitle = context.getString(R.string
                                                .systemui_qs_tiles);
                                        break;
                                }
                            } else if (References.allowedSettingsOverlay(current_overlay)) {
                                switch (current_overlay) {
                                    case "com.android.settings.icons":
                                        packageTitle = context.getString(R.string.settings_icons);
                                        break;
                                }
                            } else {
                                ApplicationInfo applicationInfo =
                                        null;
                                try {
                                    applicationInfo = context.getPackageManager()
                                            .getApplicationInfo(current_overlay, 0);
                                } catch (PackageManager.NameNotFoundException e) {
                                    e.printStackTrace();
                                }
                                packageTitle = context.getPackageManager()
                                        .getApplicationLabel(applicationInfo).toString();
                            }

                            // Initialize working notification
                            if (overlays.checkActiveNotifications()) {
                                overlays.mBuilder.setProgress(100, (int) (((double) (i + 1) /
                                        overlays.checkedOverlays.size()) * 100), false);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    overlays.mBuilder.setContentText("\"" + packageTitle + "\"");
                                } else {
                                    overlays.mBuilder.setContentText(
                                            context.getString(R.string.notification_processing) +
                                                    "\"" + packageTitle + "\"");
                                }
                                overlays.mNotifyManager.notify(overlays.id,
                                        overlays.mBuilder.build());
                            }

                            String workingDirectory = context.getCacheDir().getAbsolutePath() +
                                    References.SUBSTRATUM_BUILDER_CACHE + overlays.theme_pid +
                                    "/assets/overlays/" + current_overlay;

                            if (!References.checkOMS(context)) {
                                File check_legacy = new File(context.getCacheDir()
                                        .getAbsolutePath() + References.SUBSTRATUM_BUILDER_CACHE +
                                        overlays.theme_pid + "/assets/overlays_legacy/" +
                                        current_overlay);
                                if (check_legacy.exists()) {
                                    workingDirectory = check_legacy.getAbsolutePath();
                                }
                            }
                            String suffix = ((sUrl[0].length() != 0) ?
                                    "/type3_" + parsedVariant : "/res");
                            String unparsedSuffix =
                                    ((sUrl[0].length() != 0) ? "/type3_" + unparsedVariant :
                                            "/res");
                            overlays.type3 = parsedVariant;
                            if (References.isCachingEnabled(context)) {
                                File srcDir = new File(workingDirectory +
                                        ((sUrl[0].length() != 0) ? "/type3_" + sUrl[0] : "/res"));
                                File destDir = new File(workingDirectory + "/workdir");
                                if (destDir.exists()) {
                                    FileOperations.delete(context, destDir.getAbsolutePath());
                                }
                                try {
                                    FileUtils.copyDirectory(srcDir, destDir);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                workingDirectory = context.getCacheDir().getAbsolutePath() +
                                        References.SUBSTRATUM_BUILDER_CACHE.substring(0,
                                                References.SUBSTRATUM_BUILDER_CACHE.length() - 1);
                                File created = new File(workingDirectory);
                                if (created.exists()) {
                                    FileOperations.delete(context, created.getAbsolutePath());
                                }
                                FileOperations.createNewFolder(context, created
                                        .getAbsolutePath());
                            }
                            String listDir = Overlays.overlaysDir + "/" + current_overlay +
                                    unparsedSuffix;

                            FileOperations.copyFileOrDir(
                                    overlays.themeAssetManager,
                                    listDir,
                                    workingDirectory + suffix,
                                    listDir,
                                    overlays.cipher
                            );

                            if (overlays.checkedOverlays.get(i).is_variant_chosen ||
                                    sUrl[0].length() != 0) {
                                // Type 1a
                                if (overlays.checkedOverlays.get(i).is_variant_chosen1) {
                                    overlays.type1a =
                                            overlays.checkedOverlays.get(i)
                                                    .getSelectedVariantName();
                                    if (References.isCachingEnabled(context)) {
                                        String sourceLocation = workingDirectory + "/type1a_" +
                                                overlays.checkedOverlays.get(i)
                                                        .getSelectedVariantName()
                                                + ".xml";

                                        String targetLocation = workingDirectory +
                                                "/workdir/values/type1a.xml";

                                        Log.d(Overlays.TAG,
                                                "You have selected variant file \"" +
                                                        overlays.checkedOverlays.get(i)
                                                                .getSelectedVariantName() + "\"");
                                        Log.d(Overlays.TAG, "Moving variant file to: " +
                                                targetLocation);
                                        FileOperations.copy(
                                                context,
                                                sourceLocation,
                                                targetLocation);
                                    } else {
                                        Log.d(Overlays.TAG, "You have selected variant file \"" +
                                                overlays.checkedOverlays.get(i)
                                                        .getSelectedVariantName() + "\"");
                                        Log.d(Overlays.TAG, "Moving variant file to: " +
                                                workingDirectory + suffix + "/values/type1a.xml");

                                        String to_copy =
                                                Overlays.overlaysDir + "/" + current_overlay +
                                                        "/type1a_" +
                                                        overlays.checkedOverlays.get(i)
                                                                .getSelectedVariantName() +
                                                        (overlays.encrypted ? ".xml.enc" : ".xml");

                                        FileOperations.copyFileOrDir(
                                                overlays.themeAssetManager,
                                                to_copy,
                                                workingDirectory + suffix + (
                                                        overlays.encrypted ?
                                                                "/values/type1a.xml.enc" :
                                                                "/values/type1a.xml"),
                                                to_copy,
                                                overlays.cipher);
                                    }
                                }

                                // Type 1b
                                if (overlays.checkedOverlays.get(i).is_variant_chosen2) {
                                    overlays.type1b =
                                            overlays.checkedOverlays.get(i)
                                                    .getSelectedVariantName2();
                                    if (References.isCachingEnabled(context)) {
                                        String sourceLocation2 = workingDirectory + "/type1b_" +
                                                overlays.checkedOverlays.get(i)
                                                        .getSelectedVariantName2() + ".xml";

                                        String targetLocation2 = workingDirectory +
                                                "/workdir/values/type1b.xml";

                                        Log.d(Overlays.TAG, "You have selected variant file \"" +
                                                overlays.checkedOverlays.get(i)
                                                        .getSelectedVariantName2() + "\"");
                                        Log.d(Overlays.TAG, "Moving variant file to: " +
                                                targetLocation2);
                                        FileOperations.copy(context, sourceLocation2,
                                                targetLocation2);
                                    } else {
                                        Log.d(Overlays.TAG, "You have selected variant file \"" +
                                                overlays.checkedOverlays.get(i)
                                                        .getSelectedVariantName2() + "\"");
                                        Log.d(Overlays.TAG, "Moving variant file to: " +
                                                workingDirectory + suffix + "/values/type1b.xml");

                                        String to_copy =
                                                Overlays.overlaysDir + "/" + current_overlay +
                                                        "/type1b_" +
                                                        overlays.checkedOverlays.get(i)
                                                                .getSelectedVariantName2() +
                                                        (overlays.encrypted ? ".xml.enc" : ".xml");

                                        FileOperations.copyFileOrDir(
                                                overlays.themeAssetManager,
                                                to_copy,
                                                workingDirectory + suffix + (
                                                        overlays.encrypted ?
                                                                "/values/type1b.xml.enc" :
                                                                "/values/type1b.xml"),
                                                to_copy,
                                                overlays.cipher);
                                    }
                                }
                                // Type 1c
                                if (overlays.checkedOverlays.get(i).is_variant_chosen3) {
                                    overlays.type1c =
                                            overlays.checkedOverlays.get(i)
                                                    .getSelectedVariantName3();
                                    if (References.isCachingEnabled(context)) {
                                        String sourceLocation3 = workingDirectory + "/type1c_" +
                                                overlays.checkedOverlays.get(i)
                                                        .getSelectedVariantName3() + ".xml";

                                        String targetLocation3 = workingDirectory +
                                                "/workdir/values/type1c.xml";

                                        Log.d(Overlays.TAG, "You have selected variant file \"" +
                                                overlays.checkedOverlays.get(i)
                                                        .getSelectedVariantName3() + "\"");
                                        Log.d(Overlays.TAG, "Moving variant file to: " +
                                                targetLocation3);

                                        FileOperations.copy(
                                                context,
                                                sourceLocation3,
                                                targetLocation3);
                                    } else {
                                        Log.d(Overlays.TAG, "You have selected variant file \"" +
                                                overlays.checkedOverlays.get(i)
                                                        .getSelectedVariantName3() + "\"");
                                        Log.d(Overlays.TAG, "Moving variant file to: " +
                                                workingDirectory + suffix + "/values/type1c.xml");

                                        String to_copy =
                                                Overlays.overlaysDir + "/" + current_overlay +
                                                        "/type1c_" +
                                                        overlays.checkedOverlays.get(i)
                                                                .getSelectedVariantName3() +
                                                        (overlays.encrypted ? ".xml.enc" : ".xml");

                                        FileOperations.copyFileOrDir(
                                                overlays.themeAssetManager,
                                                to_copy,
                                                workingDirectory + suffix + (
                                                        overlays.encrypted ?
                                                                "/values/type1c.xml.enc" :
                                                                "/values/type1c.xml"),
                                                to_copy,
                                                overlays.cipher);
                                    }
                                }

                                String packageName =
                                        (overlays.checkedOverlays.get(i).is_variant_chosen1 ?
                                                overlays.checkedOverlays.get(i)
                                                        .getSelectedVariantName() : "") +
                                                (overlays.checkedOverlays
                                                        .get(i).is_variant_chosen2 ?
                                                        overlays.checkedOverlays.get(i)
                                                                .getSelectedVariantName2() : "") +
                                                (overlays.checkedOverlays
                                                        .get(i).is_variant_chosen3 ?
                                                        overlays.checkedOverlays.get(i)
                                                                .getSelectedVariantName3() : "") +
                                                (overlays.checkedOverlays
                                                        .get(i).is_variant_chosen5 ?
                                                        overlays.checkedOverlays.get(i)
                                                                .getSelectedVariantName5() : "")
                                                        .replaceAll("\\s+", "").replaceAll
                                                        ("[^a-zA-Z0-9]+", "");

                                if (overlays.checkedOverlays.get(i).is_variant_chosen5) {
                                    // Copy over the type4 assets
                                    overlays.type4 = overlays.checkedOverlays.get(i)
                                            .getSelectedVariantName5();
                                    String type4folder = "/type4_" + overlays.type4;
                                    String type4folderOutput = "/assets";
                                    String to_copy2 = Overlays.overlaysDir + "/" + current_overlay +
                                            type4folder;
                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            to_copy2,
                                            workingDirectory + type4folderOutput,
                                            to_copy2,
                                            overlays.cipher);
                                }

                                if (overlays.checkedOverlays.get(i).is_variant_chosen4) {
                                    packageName = (packageName + overlays.checkedOverlays.get(i)
                                            .getSelectedVariantName4()).replaceAll("\\s+", "")
                                            .replaceAll("[^a-zA-Z0-9]+", "");

                                    // Copy over the type2 assets
                                    overlays.type2 = overlays.checkedOverlays.get(i)
                                            .getSelectedVariantName4();
                                    String type2folder = "/type2_" + overlays.type2;
                                    String to_copy = Overlays.overlaysDir + "/" + current_overlay +
                                            type2folder;
                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            to_copy,
                                            workingDirectory + type2folder,
                                            to_copy,
                                            overlays.cipher);

                                    // Let's get started
                                    Log.d(Overlays.TAG, "Currently processing package" +
                                            " \"" + overlays.checkedOverlays.get(i)
                                            .getFullOverlayParameters() + "\"...");
                                    if (sUrl[0].length() != 0) {
                                        overlays.sb = new SubstratumBuilder();
                                        overlays.sb.beginAction(
                                                context,
                                                overlays.theme_pid,
                                                current_overlay,
                                                overlays.theme_name,
                                                packageName,
                                                overlays.checkedOverlays.get(i)
                                                        .getSelectedVariantName4(),
                                                sUrl[0],
                                                overlays.versionName,
                                                References.checkOMS(context),
                                                overlays.theme_pid,
                                                suffix,
                                                overlays.type1a,
                                                overlays.type1b,
                                                overlays.type1c,
                                                overlays.type2,
                                                overlays.type3,
                                                overlays.type4,
                                                null);
                                        overlays.logTypes();
                                    } else {
                                        overlays.sb = new SubstratumBuilder();
                                        overlays.sb.beginAction(
                                                context,
                                                overlays.theme_pid,
                                                current_overlay,
                                                overlays.theme_name,
                                                packageName,
                                                overlays.checkedOverlays.get(i)
                                                        .getSelectedVariantName4(),
                                                null,
                                                overlays.versionName,
                                                References.checkOMS(context),
                                                overlays.theme_pid,
                                                suffix,
                                                overlays.type1a,
                                                overlays.type1b,
                                                overlays.type1c,
                                                overlays.type2,
                                                overlays.type3,
                                                overlays.type4,
                                                null);
                                        overlays.logTypes();
                                    }
                                } else {
                                    Log.d(Overlays.TAG, "Currently processing package" +
                                            " \"" + overlays.checkedOverlays.get(i)
                                            .getFullOverlayParameters() + "\"...");

                                    if (sUrl[0].length() != 0) {
                                        overlays.sb = new SubstratumBuilder();
                                        overlays.sb.beginAction(
                                                context,
                                                overlays.theme_pid,
                                                current_overlay,
                                                overlays.theme_name,
                                                packageName,
                                                null,
                                                sUrl[0],
                                                overlays.versionName,
                                                References.checkOMS(context),
                                                overlays.theme_pid,
                                                suffix,
                                                overlays.type1a,
                                                overlays.type1b,
                                                overlays.type1c,
                                                overlays.type2,
                                                overlays.type3,
                                                overlays.type4,
                                                null);
                                        overlays.logTypes();
                                    } else {
                                        overlays.sb = new SubstratumBuilder();
                                        overlays.sb.beginAction(
                                                context,
                                                overlays.theme_pid,
                                                current_overlay,
                                                overlays.theme_name,
                                                packageName,
                                                null,
                                                null,
                                                overlays.versionName,
                                                References.checkOMS(context),
                                                overlays.theme_pid,
                                                suffix,
                                                overlays.type1a,
                                                overlays.type1b,
                                                overlays.type1c,
                                                overlays.type2,
                                                overlays.type3,
                                                overlays.type4,
                                                null);
                                        overlays.logTypes();
                                    }
                                }
                                if (overlays.sb.has_errored_out) {
                                    if (!overlays.sb.getErrorLogs().contains("type3") ||
                                            !overlays.sb.getErrorLogs().contains("does not " +
                                                    "exist")) {
                                        overlays.fail_count += 1;
                                        if (overlays.error_logs.length() == 0) {
                                            overlays.error_logs.append(overlays.sb.getErrorLogs());
                                        } else {
                                            overlays.error_logs.append("\n")
                                                    .append(overlays.sb.getErrorLogs());
                                        }
                                        overlays.failed_packages.append(current_overlay);
                                        overlays.failed_packages.append(" (");
                                        overlays.failed_packages.append(
                                                References.grabAppVersion(context,
                                                        current_overlay));
                                        overlays.failed_packages.append(")\n");
                                        overlays.has_failed = true;
                                    } else {
                                        overlays.missingType3 = true;
                                    }
                                } else {
                                    if (overlays.sb.special_snowflake ||
                                            overlays.sb.no_install.length() > 0) {
                                        overlays.late_install.add(overlays.sb.no_install);
                                    } else if ((References.checkThemeInterfacer(context) &&
                                            !References.isBinderInterfacer(context)) ||
                                            References.checkAndromeda(context)) {
                                        // Thread wait
                                        overlays.isWaiting = true;
                                        do {
                                            try {
                                                Thread.sleep(Overlays.THREAD_WAIT_DURATION);
                                            } catch (InterruptedException e) {
                                                Thread.currentThread().interrupt();
                                            }
                                        } while (overlays.isWaiting);
                                    }
                                }
                            } else {
                                Log.d(Overlays.TAG, "Currently processing package" +
                                        " \"" + current_overlay + "." + theme_name_parsed +
                                        "\"...");
                                overlays.sb = new SubstratumBuilder();
                                overlays.sb.beginAction(
                                        context,
                                        overlays.theme_pid,
                                        current_overlay,
                                        overlays.theme_name,
                                        null,
                                        null,
                                        null,
                                        overlays.versionName,
                                        References.checkOMS(context),
                                        overlays.theme_pid,
                                        suffix,
                                        overlays.type1a,
                                        overlays.type1b,
                                        overlays.type1c,
                                        overlays.type2,
                                        overlays.type3,
                                        overlays.type4,
                                        null);
                                overlays.logTypes();

                                if (overlays.sb.has_errored_out) {
                                    overlays.fail_count += 1;
                                    if (overlays.error_logs.length() == 0) {
                                        overlays.error_logs.append(overlays.sb.getErrorLogs());
                                    } else {
                                        overlays.error_logs.append("\n")
                                                .append(overlays.sb.getErrorLogs());
                                    }
                                    overlays.failed_packages.append(current_overlay);
                                    overlays.failed_packages.append(" (");
                                    overlays.failed_packages.append(
                                            References.grabAppVersion(context, current_overlay));
                                    overlays.failed_packages.append(")\n");
                                    overlays.has_failed = true;
                                } else {
                                    if (overlays.sb.special_snowflake ||
                                            overlays.sb.no_install.length() > 0) {
                                        overlays.late_install.add(overlays.sb.no_install);
                                    } else if ((References.checkThemeInterfacer(context) &&
                                            !References.isBinderInterfacer(context)) ||
                                            References.checkAndromeda(context)) {
                                        // Thread wait
                                        overlays.isWaiting = true;
                                        do {
                                            try {
                                                Thread.sleep(Overlays.THREAD_WAIT_DURATION);
                                            } catch (InterruptedException e) {
                                                Thread.currentThread().interrupt();
                                            }
                                        } while (overlays.isWaiting);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(Overlays.TAG, "Main function has unexpectedly stopped!");
                        }
                    } else {
                        if (overlays.final_runner == null)
                            overlays.final_runner = new ArrayList<>();
                        if (overlays.enable_mode || overlays.compile_enable_mode ||
                                overlays.disable_mode || overlays.enable_disable_mode) {
                            String package_name =
                                    overlays.checkedOverlays.get(i).getFullOverlayParameters();
                            if (References.isPackageInstalled(context, package_name)) {
                                overlays.final_runner.add(package_name);
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    static class Phase4_finishEnableFunction extends AsyncTask<Void, Void, Void> {
        WeakReference<Overlays> ref;
        WeakReference<Context> refContext;

        Phase4_finishEnableFunction(Overlays overlays) {
            ref = new WeakReference<>(overlays);
            refContext = new WeakReference<>(overlays.getContext());
        }

        @Override
        protected void onPreExecute() {
            Overlays overlays = ref.get();
            if (overlays != null) {
                overlays.progressBar.setVisibility(View.VISIBLE);
                if (overlays.toggle_all.isChecked()) overlays.toggle_all.setChecked(false);
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Context context = overlays.getActivity();

                if (overlays.final_runner.size() > 0) {
                    overlays.enable_mode = false;

                    if (overlays.mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        for (int i = 0; i < overlays.all_installed_overlays.size(); i++) {
                            if (!References.grabOverlayParent(context,
                                    overlays.all_installed_overlays.get(i))
                                    .equals(overlays.theme_pid)) {
                                disableBeforeEnabling.add(overlays.all_installed_overlays.get(i));
                            }
                        }
                        ThemeManager.disableOverlay(context, disableBeforeEnabling);
                        ThemeManager.enableOverlay(context, overlays.final_command);
                    } else {
                        ThemeManager.enableOverlay(context, overlays.final_command);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Overlays overlays = ref.get();
            Context context = refContext.get();
            if (overlays != null && context != null) {
                if (overlays.final_runner.size() > 0) {
                    overlays.progressBar.setVisibility(View.GONE);
                    if (overlays.needsRecreate(context)) {
                        Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                overlays.overlaysLists = ((OverlaysAdapter) overlays.mAdapter)
                                        .getOverlayList();
                                for (int i = 0; i < overlays.overlaysLists.size(); i++) {
                                    OverlaysItem currentOverlay = overlays.overlaysLists.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(
                                            overlays.updateEnabledOverlays());
                                    overlays.mAdapter.notifyDataSetChanged();
                                }
                            } catch (Exception e) {
                                // Consume window refresh
                            }
                        }, References.REFRESH_WINDOW_DELAY);
                    }
                } else {
                    overlays.compile_enable_mode = false;
                    overlays.enable_mode = false;
                    currentShownLunchBar = Lunchbar.make(
                            overlays.getActivityView(),
                            R.string.toast_disabled3,
                            Lunchbar.LENGTH_LONG);
                    currentShownLunchBar.show();
                }
            }
        }
    }


    static class Phase4_finishDisableFunction extends AsyncTask<Void, Void, Void> {
        WeakReference<Overlays> ref;
        WeakReference<Context> refContext;

        Phase4_finishDisableFunction(Overlays overlays) {
            ref = new WeakReference<>(overlays);
            refContext = new WeakReference<>(overlays.getContext());
        }

        @Override
        protected void onPreExecute() {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Activity activity = overlays.getActivity();
                if (overlays.final_runner.size() > 0) {
                    overlays.progressBar.setVisibility(View.VISIBLE);
                    if (overlays.toggle_all.isChecked())
                        activity.runOnUiThread(() -> overlays.toggle_all.setChecked(false));
                }
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Context context = overlays.getActivity();

                if (overlays.final_runner.size() > 0) {
                    overlays.disable_mode = false;
                    ThemeManager.disableOverlay(context, overlays.final_command);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Overlays overlays = ref.get();
            Context context = refContext.get();
            if (overlays != null && context != null) {
                Activity activity = overlays.getActivity();
                overlays.progressBar.setVisibility(View.GONE);
                if (overlays.final_runner.size() > 0) {
                    if (overlays.needsRecreate(context)) {
                        Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                overlays.overlaysLists = ((OverlaysAdapter) overlays.mAdapter)
                                        .getOverlayList();
                                for (int i = 0; i < overlays.overlaysLists.size(); i++) {
                                    OverlaysItem currentOverlay = overlays.overlaysLists.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(
                                            overlays.updateEnabledOverlays());
                                    activity.runOnUiThread(() ->
                                            overlays.mAdapter.notifyDataSetChanged());
                                }
                            } catch (Exception e) {
                                // Consume window refresh
                            }
                        }, References.REFRESH_WINDOW_DELAY);
                    }
                } else {
                    overlays.disable_mode = false;
                    currentShownLunchBar = Lunchbar.make(
                            overlays.getActivityView(),
                            R.string.toast_disabled4,
                            Lunchbar.LENGTH_LONG);
                    currentShownLunchBar.show();
                }
            }
        }
    }

    static class Phase4_finishEnableDisableFunction extends AsyncTask<Void, Void, Void> {
        WeakReference<Overlays> ref;
        WeakReference<Context> refContext;

        Phase4_finishEnableDisableFunction(Overlays overlays) {
            ref = new WeakReference<>(overlays);
            refContext = new WeakReference<>(overlays.getContext());
        }

        @Override
        protected void onPreExecute() {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Activity activity = overlays.getActivity();
                if (overlays.final_runner.size() > 0) {
                    overlays.progressBar.setVisibility(View.VISIBLE);
                    if (overlays.toggle_all.isChecked())
                        activity.runOnUiThread(() -> overlays.toggle_all.setChecked(false));
                }
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Context context = overlays.getActivity();

                if (overlays.final_runner.size() > 0) {
                    overlays.enable_disable_mode = false;

                    ArrayList<String> enableOverlays = new ArrayList<>();
                    ArrayList<String> disableOverlays = new ArrayList<>();
                    for (int i = 0; i<overlays.final_command.size(); i++) {
                        if (!overlays.checkedOverlays.get(i).isOverlayEnabled())
                            enableOverlays.add(overlays.final_command.get(i));
                        else
                            disableOverlays.add(overlays.final_command.get(i));
                    }
                    ThemeManager.disableOverlay(context, disableOverlays);
                    if (overlays.mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        for (int i = 0; i < overlays.all_installed_overlays.size(); i++) {
                            if (!References.grabOverlayParent(context,
                                    overlays.all_installed_overlays.get(i))
                                    .equals(overlays.theme_pid)) {
                                disableBeforeEnabling.add(overlays.all_installed_overlays.get(i));
                            }
                        }
                        ThemeManager.disableOverlay(context, disableBeforeEnabling);
                    }
                    ThemeManager.enableOverlay(context, enableOverlays);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Overlays overlays = ref.get();
            Context context = refContext.get();
            if (overlays != null && context != null) {
                Activity activity = overlays.getActivity();
                overlays.progressBar.setVisibility(View.GONE);
                if (overlays.final_runner.size() > 0) {
                    if (overlays.needsRecreate(context)) {
                        Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                overlays.overlaysLists = ((OverlaysAdapter) overlays.mAdapter)
                                        .getOverlayList();
                                for (int i = 0; i < overlays.overlaysLists.size(); i++) {
                                    OverlaysItem currentOverlay = overlays.overlaysLists.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(
                                            overlays.updateEnabledOverlays());
                                    activity.runOnUiThread(() ->
                                            overlays.mAdapter.notifyDataSetChanged());
                                }
                            } catch (Exception e) {
                                // Consume window refresh
                            }
                        }, References.REFRESH_WINDOW_DELAY);
                    }
                } else {
                    overlays.enable_disable_mode = false;
                    //CURRENTLY: Showing both enable and disable errors toasts.
                    currentShownLunchBar = Lunchbar.make(
                            overlays.getActivityView(),
                            R.string.toast_disabled4,
                            Lunchbar.LENGTH_LONG);
                    currentShownLunchBar.show();
                    currentShownLunchBar = Lunchbar.make(
                            overlays.getActivityView(),
                            R.string.toast_disabled3,
                            Lunchbar.LENGTH_LONG);
                    currentShownLunchBar.show();
                }
            }
        }
    }

    static class Phase4_finishUpdateFunction extends AsyncTask<Void, Void, Void> {
        WeakReference<Overlays> ref;
        WeakReference<Context> refContext;

        Phase4_finishUpdateFunction(Overlays overlays) {
            ref = new WeakReference<>(overlays);
            refContext = new WeakReference<>(overlays.getContext());
        }

        @Override
        protected void onPreExecute() {
            Overlays overlays = ref.get();
            Context context = refContext.get();
            if (context != null && overlays != null) {
                if (overlays.mProgressDialog != null) overlays.mProgressDialog.dismiss();

                // Add dummy intent to be able to close the notification on click
                Intent notificationIntent = new Intent(context, InformationActivity.class);
                notificationIntent.putExtra("theme_name", overlays.theme_name);
                notificationIntent.putExtra("theme_pid", overlays.theme_pid);
                notificationIntent.setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent intent =
                        PendingIntent.getActivity(context, 0, notificationIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT);

                if (!overlays.has_failed) {
                    // Closing off the persistent notification
                    if (overlays.checkActiveNotifications()) {
                        overlays.mNotifyManager.cancel(overlays.id);
                        overlays.mBuilder = new NotificationCompat.Builder(
                                context, DEFAULT_NOTIFICATION_CHANNEL_ID);
                        overlays.mBuilder.setAutoCancel(true);
                        overlays.mBuilder.setProgress(0, 0, false);
                        overlays.mBuilder.setOngoing(false);
                        overlays.mBuilder.setContentIntent(intent);
                        overlays.mBuilder.setSmallIcon(R.drawable.notification_success_icon);
                        overlays.mBuilder.setContentTitle(
                                context.getString(R.string.notification_done_title));
                        overlays.mBuilder.setContentText(
                                context.getString(R.string.notification_no_errors_found));
                        if (overlays.prefs.getBoolean("vibrate_on_compiled", false)) {
                            overlays.mBuilder.setVibrate(new long[]{100, 200, 100, 500});
                        }
                        overlays.mNotifyManager.notify(overlays.id, overlays.mBuilder.build());
                    }

                    if (overlays.missingType3) {
                        currentShownLunchBar = Lunchbar.make(
                                overlays.getActivityView(),
                                R.string.toast_compiled_missing,
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    } else {
                        currentShownLunchBar = Lunchbar.make(
                                overlays.getActivityView(),
                                R.string.toast_compiled_updated,
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    }
                }
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Activity activity = overlays.getActivity();
                Context context = overlays.getContext();

                if (!overlays.has_failed || overlays.final_runner.size() > overlays.fail_count) {
                    new StringBuilder();
                    StringBuilder final_commands;
                    if (overlays.compile_enable_mode && overlays.mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        for (String p : overlays.all_installed_overlays) {
                            if (!overlays.theme_pid.equals(References.
                                    grabOverlayParent(context, p))) {
                                disableBeforeEnabling.add(p);
                            } else {
                                for (OverlaysItem oi : overlays.checkedOverlays) {
                                    String targetOverlay = oi.getPackageName();
                                    if (targetOverlay.equals(
                                            References.grabOverlayTarget(context, p))) {
                                        disableBeforeEnabling.add(p);
                                    }
                                }
                            }
                        }
                        if (checkThemeInterfacer(context)) {
                            ThemeManager.disableOverlay(context, disableBeforeEnabling);
                        } else {
                            final_commands = new StringBuilder(ThemeManager.disableOverlay);
                            for (int i = 0; i < disableBeforeEnabling.size(); i++) {
                                final_commands.append(" ").append(disableBeforeEnabling.get(i))
                                        .append(" ");
                            }
                            Log.d(TAG, final_commands.toString());
                        }
                    }

                    if (overlays.compile_enable_mode) {
                        ThemeManager.enableOverlay(context, overlays.final_command);
                    }

                    if (overlays.final_runner.size() == 0) {
                        if (overlays.base_spinner.getSelectedItemPosition() == 0) {
                            activity.runOnUiThread(() -> overlays.mAdapter.notifyDataSetChanged());
                        } else {
                            activity.runOnUiThread(() -> overlays.mAdapter.notifyDataSetChanged());
                        }
                    } else {
                        activity.runOnUiThread(() ->
                                overlays.progressBar.setVisibility(View.VISIBLE));
                        if (overlays.toggle_all.isChecked())
                            activity.runOnUiThread(() -> overlays.toggle_all.setChecked(false));
                        activity.runOnUiThread(() -> overlays.mAdapter.notifyDataSetChanged());
                    }

                    activity.runOnUiThread(() -> overlays.progressBar.setVisibility(View.GONE));
                    if (overlays.needsRecreate(context)) {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                overlays.overlaysLists =
                                        ((OverlaysAdapter) overlays.mAdapter).getOverlayList();
                                for (int i = 0; i < overlays.overlaysLists.size(); i++) {
                                    OverlaysItem currentOverlay = overlays.overlaysLists.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(
                                            overlays.updateEnabledOverlays());
                                    activity.runOnUiThread(() ->
                                            overlays.mAdapter.notifyDataSetChanged());
                                }
                            } catch (Exception e) {
                                // Consume window refresh
                            }
                        }, REFRESH_WINDOW_DELAY);
                    }

                    if (!overlays.late_install.isEmpty() && !References.isSamsung(context)) {
                        // Install remaining overlays
                        ThemeManager.installOverlay(context, overlays.late_install);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }
}