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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.adapters.tabs.overlays.OverlaysAdapter;
import projekt.substratum.adapters.tabs.overlays.OverlaysItem;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.compilers.SubstratumBuilder;
import projekt.substratum.util.files.Root;
import projekt.substratum.util.views.SheetDialog;

import static android.content.Context.NOTIFICATION_SERVICE;
import static projekt.substratum.InformationActivity.currentShownLunchBar;
import static projekt.substratum.common.References.DEFAULT_NOTIFICATION_CHANNEL_ID;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ROOTED;
import static projekt.substratum.common.References.REFRESH_WINDOW_DELAY;
import static projekt.substratum.common.References.RUNTIME_RESOURCE_OVERLAY_N_ROOTED;
import static projekt.substratum.common.Systems.checkOMS;
import static projekt.substratum.common.Systems.checkThemeInterfacer;

enum OverlayFunctions {
    ;
    private static final String TAG = "Substratum OverlayFunctions";

    static class getThemeCache extends AsyncTask<String, Integer, String> {
        final WeakReference<Overlays> ref;

        getThemeCache(final Overlays overlays) {
            super();
            this.ref = new WeakReference<>(overlays);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            final Overlays overlays = this.ref.get();
            if (overlays != null) {
                final Context context = overlays.getActivity();
                overlays.final_runner = new ArrayList<>();
                overlays.late_install = new ArrayList<>();

                if (!overlays.enable_mode &&
                        !overlays.disable_mode &&
                        !overlays.enable_disable_mode) {

                    // This is the time when the notification should be shown on the user's screen
                    overlays.mNotifyManager =
                            (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                    overlays.mBuilder = new NotificationCompat.Builder(context,
                            References.DEFAULT_NOTIFICATION_CHANNEL_ID);
                    final int notification_priority = Notification.PRIORITY_MAX;
                    overlays.mBuilder.setContentTitle(
                            context.getString(R.string.notification_initial_title))
                            .setProgress(100, 0, true)
                            .setSmallIcon(android.R.drawable.ic_popup_sync)
                            .setPriority(notification_priority)
                            .setOngoing(true);
                    overlays.mNotifyManager.notify(
                            References.notification_id_compiler, overlays.mBuilder.build());

                    overlays.mCompileDialog = new SheetDialog(context);
                    overlays.mCompileDialog.setCancelable(false);
                    final View sheetView = View.inflate(context,
                            R.layout.compile_sheet_dialog, null);
                    overlays.mCompileDialog.setContentView(sheetView);
                    overlays.mCompileDialog.show();
                    InformationActivity.compilingProcess = true;

                    // Do not sleep the device when the sheet is raised
                    if (overlays.mCompileDialog.getWindow() != null) {
                        overlays.mCompileDialog.getWindow().addFlags(
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }

                    // Set the variables for the sheet dialog's titles
                    overlays.dialogProgress =
                            overlays.mCompileDialog.findViewById(R.id.loading_bar);
                    if (overlays.dialogProgress != null) {
                        overlays.dialogProgress.setIndeterminate(false);
                    }
                    overlays.loader_image = overlays.mCompileDialog.findViewById(R.id.icon);
                    overlays.loader_string = overlays.mCompileDialog.findViewById(R.id.title);
                    overlays.loader_string.setText(context.getResources().getString(
                            R.string.sb_phase_1_loader));
                }
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            super.onPostExecute(result);
            final Overlays overlays = this.ref.get();
            if (overlays != null) {
                final Phase3_mainFunction phase3_mainFunction = new Phase3_mainFunction(overlays);
                if (result != null) {
                    phase3_mainFunction.execute(result);
                } else {
                    phase3_mainFunction.execute("");
                }
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final Overlays overlays = this.ref.get();
            if (overlays != null) {
                final Context context = overlays.getActivity();
                if (!overlays.enable_mode &&
                        !overlays.disable_mode &&
                        !overlays.enable_disable_mode) {
                    try {
                        final Resources themeResources = context.getPackageManager()
                                .getResourcesForApplication(overlays.theme_pid);
                        overlays.themeAssetManager = themeResources.getAssets();
                        Log.d(Overlays.TAG, "Work area is ready to be compiled!");
                    } catch (final PackageManager.NameNotFoundException e) {
                        // Suppress exception
                    }
                    if (!sUrl[0].isEmpty()) {
                        return sUrl[0];
                    } else {
                        return null;
                    }
                }
            }
            return null;
        }

        public void clear() {
            if (this.ref != null) this.ref.clear();
        }
    }

    static class Phase3_mainFunction extends AsyncTask<String, Integer, String> {
        private final WeakReference<Overlays> ref;
        private String currentPackageName = "";

        Phase3_mainFunction(final Overlays overlays) {
            super();
            this.ref = new WeakReference<>(overlays);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(Overlays.TAG,
                    "Substratum is proceeding with your actions and is now actively running...");
            final Overlays overlays = this.ref.get();
            if (overlays != null) {
                final Context context = overlays.getActivity();

                overlays.missingType3 = false;
                overlays.has_failed = false;
                overlays.fail_count = 0;

                if (!overlays.enable_mode && !overlays.disable_mode && !overlays
                        .enable_disable_mode) {
                    overlays.error_logs = new StringBuilder();
                    // Change title in preparation for loop to change subtext
                    if (overlays.checkActiveNotifications()) {
                        overlays.mBuilder
                                .setContentTitle(
                                        context.getString(R.string.notification_processing_n))
                                .setProgress(100, 0, false);
                        overlays.mNotifyManager.notify(
                                References.notification_id_compiler, overlays.mBuilder.build());
                    }
                    overlays.loader_string.setText(context.getResources().getString(
                            R.string.sb_phase_2_loader));
                } else {
                    overlays.progressBar.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        protected void onProgressUpdate(final Integer... values) {
            super.onProgressUpdate(values);
            final Overlays overlays = this.ref.get();
            if (overlays != null) {
                final TextView textView = overlays.mCompileDialog.findViewById(R.id.current_object);
                if (textView != null) {
                    textView.setText(overlays.current_dialog_overlay);
                }
                overlays.loader_image.setImageBitmap(
                        Packages.getBitmapFromDrawable(
                                Packages.getAppIcon(overlays.getContext(), this.currentPackageName)));
                final double progress = (overlays.current_amount / overlays.total_amount) * 100.0;
                overlays.dialogProgress.setProgress((int) progress, true);
            }
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected void onPostExecute(final String result) {
            // TODO: onPostExecute runs on UI thread, so move the hard job to doInBackground
            super.onPostExecute(result);
            final Overlays overlays = this.ref.get();
            if (overlays != null) {
                final Context context = overlays.getActivity();
                overlays.final_command = new ArrayList<>();

                // Check if not compile_enable_mode
                if (!overlays.compile_enable_mode) {
                    overlays.final_command.addAll(overlays.final_runner);
                } else {
                    // It's compile and enable mode, we have to first sort out all the
                    // "pm install"'s from the final_commands
                    overlays.final_command.addAll(overlays.final_runner);
                }

                if (!overlays.enable_mode && !overlays.disable_mode && !overlays
                        .enable_disable_mode) {
                    new Phase4_finishUpdateFunction(overlays).execute();
                    if (overlays.has_failed) {
                        overlays.failedFunction(context);
                    } else {
                        // Restart SystemUI if an enabled SystemUI overlay is updated
                        if (checkOMS(context)) {
                            for (int i = 0; i < overlays.checkedOverlays.size(); i++) {
                                final String targetOverlay = overlays.checkedOverlays.get(i)
                                        .getPackageName();
                                if ("com.android.systemui".equals(targetOverlay)) {
                                    final String packageName =
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
                    if (overlays.late_install.isEmpty()) {
                        Substratum.getInstance().unregisterFinishReceiver();
                    }
                } else if (overlays.enable_mode) {
                    new Phase4_finishEnableFunction(overlays).execute();
                } else if (overlays.disable_mode) {
                    new Phase4_finishDisableFunction(overlays).execute();
                } else if (overlays.enable_disable_mode) {
                    new Phase4_finishEnableDisableFunction(overlays).execute();
                }
                if (Systems.isSamsung(context) &&
                        (overlays.late_install != null) &&
                        !overlays.late_install.isEmpty()) {
                    if (Root.checkRootAccess() && Root.requestRootAccess()) {
                        overlays.progressBar.setVisibility(View.VISIBLE);
                        overlays.overlaysWaiting = overlays.late_install.size();
                        for (int i = 0; i < overlays.late_install.size(); i++) {
                            ThemeManager.installOverlay(
                                    overlays.getActivity(),
                                    overlays.late_install.get(i));
                        }
                    } else {
                        final Intent intent = new Intent(Intent.ACTION_VIEW);
                        final Uri uri = FileProvider.getUriForFile(
                                context,
                                context.getApplicationContext().getPackageName() + ".provider",
                                new File(overlays.late_install.get(0)));
                        intent.setDataAndType(
                                uri,
                                "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        overlays.startActivityForResult(intent, 2486);
                    }
                } else if (!Systems.checkOMS(context) &&
                        (overlays.final_runner.size() == overlays.fail_count)) {
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
                    final AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
                overlays.is_overlay_active = false;
                overlays.mAdapter.notifyDataSetChanged();
                if (overlays.toggle_all.isChecked()) overlays.toggle_all.setChecked(false);
            }
        }


        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(final String... sUrl) {
            final Overlays overlays = this.ref.get();
            if (overlays != null) {
                final Context context = overlays.getActivity();
                final String parsedVariant = sUrl[0].replaceAll("\\s+", "");
                final String unparsedVariant = sUrl[0];
                overlays.failed_packages = new StringBuilder();
                if (overlays.mixAndMatchMode && !Systems.checkOMS(context)) {
                    final String current_directory;
                    if (projekt.substratum.common.Resources.inNexusFilter()) {
                        current_directory = References.PIXEL_NEXUS_DIR;
                    } else {
                        current_directory = References.LEGACY_NEXUS_DIR;
                    }
                    final File file = new File(current_directory);
                    if (file.exists()) {
                        FileOperations.mountRW();
                        FileOperations.delete(context, current_directory);
                    }
                }

                // Enable finish install listener
                // system on root, old interfacer and andromeda need this
                final int system = Systems.checkThemeSystemModule(context);
                final boolean needToWait = (system == OVERLAY_MANAGER_SERVICE_O_ANDROMEDA) ||
                        (system == OVERLAY_MANAGER_SERVICE_O_ROOTED) ||
                        (system == RUNTIME_RESOURCE_OVERLAY_N_ROOTED);
                if (needToWait) {
                    Substratum.getInstance().registerFinishReceiver();
                }

                overlays.total_amount = (double) overlays.checkedOverlays.size();
                for (int i = 0; i < overlays.checkedOverlays.size(); i++) {
                    overlays.type1a = "";
                    overlays.type1b = "";
                    overlays.type1c = "";
                    overlays.type2 = "";
                    overlays.type3 = "";
                    overlays.type4 = "";

                    overlays.current_amount = (double) (i + 1);
                    final String theme_name_parsed =
                            overlays.theme_name.replaceAll("\\s+", "")
                                    .replaceAll("[^a-zA-Z0-9]+", "");

                    final String current_overlay = overlays.checkedOverlays.get(i).getPackageName();
                    overlays.current_dialog_overlay =
                            "'" + Packages.getPackageName(context, current_overlay) + "'";
                    this.currentPackageName = current_overlay;

                    if (!overlays.enable_mode && !overlays.disable_mode
                            && !overlays.enable_disable_mode) {
                        this.publishProgress((int) overlays.current_amount);
                        if (overlays.compile_enable_mode) {
                            if (overlays.final_runner == null) {
                                overlays.final_runner = new ArrayList<>();
                            }
                            final String package_name = overlays.checkedOverlays.get(i)
                                    .getFullOverlayParameters();
                            if (Packages.isPackageInstalled(context, package_name) ||
                                    overlays.compile_enable_mode) {
                                overlays.final_runner.add(package_name);
                            }
                        }
                        try {
                            String packageTitle = "";
                            if (projekt.substratum.common.Resources.allowedSystemUIOverlay
                                    (current_overlay)) {
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
                            } else if (projekt.substratum.common.Resources.allowedSettingsOverlay
                                    (current_overlay)) {
                                switch (current_overlay) {
                                    case "com.android.settings.icons":
                                        packageTitle = context.getString(R.string.settings_icons);
                                        break;
                                }
                            } else if (projekt.substratum.common.Resources.allowedFrameworkOverlay
                                    (current_overlay)) {
                                switch (current_overlay) {
                                    case "fwk":
                                        packageTitle = context.getString(
                                                R.string.samsung_framework);
                                        break;
                                    case "commit":
                                        packageTitle = context.getString(R.string.lg_framework);
                                        break;
                                }
                            } else {
                                ApplicationInfo applicationInfo =
                                        null;
                                try {
                                    applicationInfo = context.getPackageManager()
                                            .getApplicationInfo(current_overlay, 0);
                                } catch (final PackageManager.NameNotFoundException e) {
                                    e.printStackTrace();
                                }
                                packageTitle = context.getPackageManager()
                                        .getApplicationLabel(applicationInfo).toString();
                            }

                            // Initialize working notification
                            if (overlays.checkActiveNotifications()) {
                                overlays.mBuilder.setProgress(100, (int) (((double) (i + 1) /
                                        (double) overlays.checkedOverlays.size()) * 100.0), false);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    overlays.mBuilder.setContentText("\"" + packageTitle + "\"");
                                } else {
                                    overlays.mBuilder.setContentText(
                                            context.getString(R.string.notification_processing) +
                                                    "\"" + packageTitle + "\"");
                                }
                                overlays.mNotifyManager.notify(References.notification_id_compiler,
                                        overlays.mBuilder.build());
                            }

                            final String unparsedSuffix;
                            boolean useType3CommonDir = false;
                            if (!sUrl[0].isEmpty()) {
                                useType3CommonDir = overlays.themeAssetManager
                                        .list(Overlays.overlaysDir + "/" + current_overlay +
                                                "/type3-common").length > 0;
                                if (useType3CommonDir) {
                                    unparsedSuffix = "/type3-common";
                                } else {
                                    unparsedSuffix = "/type3_" + unparsedVariant;
                                }
                            } else {
                                unparsedSuffix = "/res";
                            }

                            final String parsedSuffix = ((!sUrl[0].isEmpty()) ?
                                    ("/type3_" + parsedVariant) : "/res");
                            overlays.type3 = parsedVariant;

                            final String workingDirectory = context.getCacheDir().getAbsolutePath() +
                                    References.SUBSTRATUM_BUILDER_CACHE.substring(0,
                                            References.SUBSTRATUM_BUILDER_CACHE.length() - 1);

                            final File created = new File(workingDirectory);
                            if (created.exists()) {
                                FileOperations.delete(context, created.getAbsolutePath());
                            }
                            FileOperations.createNewFolder(context, created
                                    .getAbsolutePath());
                            final String listDir = Overlays.overlaysDir + "/" + current_overlay +
                                    unparsedSuffix;

                            FileOperations.copyFileOrDir(
                                    overlays.themeAssetManager,
                                    listDir,
                                    workingDirectory + parsedSuffix,
                                    listDir,
                                    overlays.cipher
                            );

                            if (useType3CommonDir) {
                                final String type3Dir = Overlays.overlaysDir + "/" + current_overlay +
                                        "/type3_" + unparsedVariant;
                                FileOperations.copyFileOrDir(
                                        overlays.themeAssetManager,
                                        type3Dir,
                                        workingDirectory + parsedSuffix,
                                        type3Dir,
                                        overlays.cipher
                                );
                            }

                            if (overlays.checkedOverlays.get(i).is_variant_chosen ||
                                    !sUrl[0].isEmpty()) {
                                // Type 1a
                                if (overlays.checkedOverlays.get(i).is_variant_chosen1) {
                                    overlays.type1a =
                                            overlays.checkedOverlays.get(i)
                                                    .getSelectedVariantName();
                                    Log.d(Overlays.TAG, "You have selected variant file \"" +
                                            overlays.checkedOverlays.get(i)
                                                    .getSelectedVariantName() + "\"");
                                    Log.d(Overlays.TAG, "Moving variant file to: " +
                                            workingDirectory + parsedSuffix + "/values/type1a.xml");

                                    final String to_copy =
                                            Overlays.overlaysDir + "/" + current_overlay +
                                                    "/type1a_" +
                                                    overlays.checkedOverlays.get(i)
                                                            .getSelectedVariantName() +
                                                    (overlays.encrypted ? ".xml.enc" : ".xml");

                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            to_copy,
                                            workingDirectory + parsedSuffix + (
                                                    overlays.encrypted ?
                                                            "/values/type1a.xml.enc" :
                                                            "/values/type1a.xml"),
                                            to_copy,
                                            overlays.cipher);
                                }

                                // Type 1b
                                if (overlays.checkedOverlays.get(i).is_variant_chosen2) {
                                    overlays.type1b =
                                            overlays.checkedOverlays.get(i)
                                                    .getSelectedVariantName2();
                                    Log.d(Overlays.TAG, "You have selected variant file \"" +
                                            overlays.checkedOverlays.get(i)
                                                    .getSelectedVariantName2() + "\"");
                                    Log.d(Overlays.TAG, "Moving variant file to: " +
                                            workingDirectory + parsedSuffix + "/values/type1b.xml");

                                    final String to_copy =
                                            Overlays.overlaysDir + "/" + current_overlay +
                                                    "/type1b_" +
                                                    overlays.checkedOverlays.get(i)
                                                            .getSelectedVariantName2() +
                                                    (overlays.encrypted ? ".xml.enc" : ".xml");

                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            to_copy,
                                            workingDirectory + parsedSuffix + (
                                                    overlays.encrypted ?
                                                            "/values/type1b.xml.enc" :
                                                            "/values/type1b.xml"),
                                            to_copy,
                                            overlays.cipher);
                                }
                                // Type 1c
                                if (overlays.checkedOverlays.get(i).is_variant_chosen3) {
                                    overlays.type1c =
                                            overlays.checkedOverlays.get(i)
                                                    .getSelectedVariantName3();
                                    Log.d(Overlays.TAG, "You have selected variant file \"" +
                                            overlays.checkedOverlays.get(i)
                                                    .getSelectedVariantName3() + "\"");
                                    Log.d(Overlays.TAG, "Moving variant file to: " +
                                            workingDirectory + parsedSuffix + "/values/type1c.xml");

                                    final String to_copy =
                                            Overlays.overlaysDir + "/" + current_overlay +
                                                    "/type1c_" +
                                                    overlays.checkedOverlays.get(i)
                                                            .getSelectedVariantName3() +
                                                    (overlays.encrypted ? ".xml.enc" : ".xml");

                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            to_copy,
                                            workingDirectory + parsedSuffix + (
                                                    overlays.encrypted ?
                                                            "/values/type1c.xml.enc" :
                                                            "/values/type1c.xml"),
                                            to_copy,
                                            overlays.cipher);
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
                                    final String type4folder = "/type4_" + overlays.type4;
                                    final String type4folderOutput = "/assets";
                                    final String to_copy2 = Overlays.overlaysDir + "/" + current_overlay +
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
                                    final String type2folder = "/type2_" + overlays.type2;
                                    final String to_copy = Overlays.overlaysDir + "/" + current_overlay +
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
                                    if (!sUrl[0].isEmpty()) {
                                        overlays.sb = new SubstratumBuilder();
                                        overlays.sb.beginAction(
                                                context,
                                                current_overlay,
                                                overlays.theme_name,
                                                packageName,
                                                overlays.checkedOverlays.get(i)
                                                        .getSelectedVariantName4(),
                                                sUrl[0],
                                                overlays.versionName,
                                                Systems.checkOMS(context),
                                                overlays.theme_pid,
                                                parsedSuffix,
                                                overlays.type1a,
                                                overlays.type1b,
                                                overlays.type1c,
                                                overlays.type2,
                                                overlays.type3,
                                                overlays.type4,
                                                null,
                                                false);
                                        overlays.logTypes();
                                    } else {
                                        overlays.sb = new SubstratumBuilder();
                                        overlays.sb.beginAction(
                                                context,
                                                current_overlay,
                                                overlays.theme_name,
                                                packageName,
                                                overlays.checkedOverlays.get(i)
                                                        .getSelectedVariantName4(),
                                                null,
                                                overlays.versionName,
                                                Systems.checkOMS(context),
                                                overlays.theme_pid,
                                                parsedSuffix,
                                                overlays.type1a,
                                                overlays.type1b,
                                                overlays.type1c,
                                                overlays.type2,
                                                overlays.type3,
                                                overlays.type4,
                                                null,
                                                false);
                                        overlays.logTypes();
                                    }
                                } else {
                                    Log.d(Overlays.TAG, "Currently processing package" +
                                            " \"" + overlays.checkedOverlays.get(i)
                                            .getFullOverlayParameters() + "\"...");

                                    if (!sUrl[0].isEmpty()) {
                                        overlays.sb = new SubstratumBuilder();
                                        overlays.sb.beginAction(
                                                context,
                                                current_overlay,
                                                overlays.theme_name,
                                                packageName,
                                                null,
                                                sUrl[0],
                                                overlays.versionName,
                                                Systems.checkOMS(context),
                                                overlays.theme_pid,
                                                parsedSuffix,
                                                overlays.type1a,
                                                overlays.type1b,
                                                overlays.type1c,
                                                overlays.type2,
                                                overlays.type3,
                                                overlays.type4,
                                                null,
                                                false);
                                        overlays.logTypes();
                                    } else {
                                        overlays.sb = new SubstratumBuilder();
                                        overlays.sb.beginAction(
                                                context,
                                                current_overlay,
                                                overlays.theme_name,
                                                packageName,
                                                null,
                                                null,
                                                overlays.versionName,
                                                Systems.checkOMS(context),
                                                overlays.theme_pid,
                                                parsedSuffix,
                                                overlays.type1a,
                                                overlays.type1b,
                                                overlays.type1c,
                                                overlays.type2,
                                                overlays.type3,
                                                overlays.type4,
                                                null,
                                                false);
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
                                                Packages.getAppVersion(context,
                                                        current_overlay));
                                        overlays.failed_packages.append(")\n");
                                        overlays.has_failed = true;
                                    } else {
                                        overlays.missingType3 = true;
                                    }
                                } else {
                                    if (overlays.sb.special_snowflake ||
                                            !overlays.sb.no_install.isEmpty()) {
                                        overlays.late_install.add(overlays.sb.no_install);
                                    } else if (needToWait) {
                                        // Thread wait
                                        Substratum.startWaitingInstall();
                                        do {
                                            try {
                                                Thread.sleep((long) Overlays.THREAD_WAIT_DURATION);
                                            } catch (final InterruptedException e) {
                                                Thread.currentThread().interrupt();
                                            }
                                        } while (Substratum.isWaitingInstall());
                                    }
                                }
                            } else {
                                Log.d(Overlays.TAG, "Currently processing package" +
                                        " \"" + current_overlay + "." + theme_name_parsed +
                                        "\"...");
                                overlays.sb = new SubstratumBuilder();
                                overlays.sb.beginAction(
                                        context,
                                        current_overlay,
                                        overlays.theme_name,
                                        null,
                                        null,
                                        null,
                                        overlays.versionName,
                                        Systems.checkOMS(context),
                                        overlays.theme_pid,
                                        parsedSuffix,
                                        overlays.type1a,
                                        overlays.type1b,
                                        overlays.type1c,
                                        overlays.type2,
                                        overlays.type3,
                                        overlays.type4,
                                        null,
                                        false);
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
                                            Packages.getAppVersion(context, current_overlay));
                                    overlays.failed_packages.append(")\n");
                                    overlays.has_failed = true;
                                } else {
                                    if (overlays.sb.special_snowflake ||
                                            !overlays.sb.no_install.isEmpty()) {
                                        overlays.late_install.add(overlays.sb.no_install);
                                    } else if (needToWait) {
                                        // Thread wait
                                        Substratum.startWaitingInstall();
                                        do {
                                            try {
                                                Thread.sleep((long) Overlays.THREAD_WAIT_DURATION);
                                            } catch (final InterruptedException e) {
                                                Thread.currentThread().interrupt();
                                            }
                                        } while (Substratum.isWaitingInstall());
                                    }
                                }
                            }
                        } catch (final Exception e) {
                            e.printStackTrace();
                            Log.e(Overlays.TAG, "Main function has unexpectedly stopped!");
                        }
                    } else {
                        if (overlays.final_runner == null)
                            overlays.final_runner = new ArrayList<>();
                        if (overlays.enable_mode || overlays.compile_enable_mode ||
                                overlays.disable_mode || overlays.enable_disable_mode) {
                            final String package_name =
                                    overlays.checkedOverlays.get(i).getFullOverlayParameters();
                            if (Packages.isPackageInstalled(context, package_name)) {
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
        final WeakReference<Overlays> ref;
        final WeakReference<Context> refContext;

        Phase4_finishEnableFunction(final Overlays overlays) {
            super();
            this.ref = new WeakReference<>(overlays);
            this.refContext = new WeakReference<>(overlays.getContext());
        }

        @Override
        protected void onPreExecute() {
            final Overlays overlays = this.ref.get();
            if (overlays != null) {
                overlays.progressBar.setVisibility(View.VISIBLE);
                if (overlays.toggle_all.isChecked()) overlays.toggle_all.setChecked(false);
            }
        }

        @Override
        protected Void doInBackground(final Void... voids) {
            final Overlays overlays = this.ref.get();
            if (overlays != null) {
                final Context context = overlays.getActivity();

                if (!overlays.final_runner.isEmpty()) {
                    overlays.enable_mode = false;

                    if (overlays.mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        final ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        final List<String> all_installed_overlays = ThemeManager.listAllOverlays(context);
                        for (int i = 0; i < all_installed_overlays.size(); i++) {
                            if (!Packages.getOverlayParent(context, all_installed_overlays.get(i))
                                    .equals(overlays.theme_pid)) {
                                disableBeforeEnabling.add(all_installed_overlays.get(i));
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
        protected void onPostExecute(final Void result) {
            final Overlays overlays = this.ref.get();
            final Context context = this.refContext.get();
            if ((overlays != null) && (context != null)) {
                if (!overlays.final_runner.isEmpty()) {
                    overlays.progressBar.setVisibility(View.GONE);
                    if (overlays.needsRecreate(context)) {
                        final Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                overlays.overlaysLists = ((OverlaysAdapter) overlays.mAdapter)
                                        .getOverlayList();
                                for (int i = 0; i < overlays.overlaysLists.size(); i++) {
                                    final OverlaysItem currentOverlay = overlays.overlaysLists.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(
                                            overlays.updateEnabledOverlays());
                                    overlays.mAdapter.notifyDataSetChanged();
                                }
                            } catch (final Exception e) {
                                // Consume window refresh
                            }
                        }, (long) References.REFRESH_WINDOW_DELAY);
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
        final WeakReference<Overlays> ref;
        final WeakReference<Context> refContext;

        Phase4_finishDisableFunction(final Overlays overlays) {
            super();
            this.ref = new WeakReference<>(overlays);
            this.refContext = new WeakReference<>(overlays.getContext());
        }

        @Override
        protected void onPreExecute() {
            final Overlays overlays = this.ref.get();
            if (overlays != null) {
                final Activity activity = overlays.getActivity();
                if (!overlays.final_runner.isEmpty()) {
                    overlays.progressBar.setVisibility(View.VISIBLE);
                    if (overlays.toggle_all.isChecked())
                        activity.runOnUiThread(() -> overlays.toggle_all.setChecked(false));
                }
            }
        }

        @Override
        protected Void doInBackground(final Void... voids) {
            final Overlays overlays = this.ref.get();
            if (overlays != null) {
                final Context context = overlays.getActivity();

                if (!overlays.final_runner.isEmpty()) {
                    overlays.disable_mode = false;
                    ThemeManager.disableOverlay(context, overlays.final_command);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            final Overlays overlays = this.ref.get();
            final Context context = this.refContext.get();
            if ((overlays != null) && (context != null)) {
                final Activity activity = overlays.getActivity();
                overlays.progressBar.setVisibility(View.GONE);
                if (!overlays.final_runner.isEmpty()) {
                    if (overlays.needsRecreate(context)) {
                        final Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                overlays.overlaysLists = ((OverlaysAdapter) overlays.mAdapter)
                                        .getOverlayList();
                                for (int i = 0; i < overlays.overlaysLists.size(); i++) {
                                    final OverlaysItem currentOverlay = overlays.overlaysLists.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(
                                            overlays.updateEnabledOverlays());
                                    activity.runOnUiThread(() ->
                                            overlays.mAdapter.notifyDataSetChanged());
                                }
                            } catch (final Exception e) {
                                // Consume window refresh
                            }
                        }, (long) References.REFRESH_WINDOW_DELAY);
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
        final WeakReference<Overlays> ref;
        final WeakReference<Context> refContext;

        Phase4_finishEnableDisableFunction(final Overlays overlays) {
            super();
            this.ref = new WeakReference<>(overlays);
            this.refContext = new WeakReference<>(overlays.getContext());
        }

        @Override
        protected void onPreExecute() {
            final Overlays overlays = this.ref.get();
            if (overlays != null) {
                final Activity activity = overlays.getActivity();
                if (!overlays.final_runner.isEmpty()) {
                    overlays.progressBar.setVisibility(View.VISIBLE);
                    if (overlays.toggle_all.isChecked())
                        activity.runOnUiThread(() -> overlays.toggle_all.setChecked(false));
                }
            }
        }

        @Override
        protected Void doInBackground(final Void... voids) {
            final Overlays overlays = this.ref.get();
            if (overlays != null) {
                final Context context = overlays.getActivity();

                if (!overlays.final_runner.isEmpty()) {
                    overlays.enable_disable_mode = false;

                    final ArrayList<String> enableOverlays = new ArrayList<>();
                    final ArrayList<String> disableOverlays = new ArrayList<>();
                    for (int i = 0; i < overlays.final_command.size(); i++) {
                        if (!overlays.checkedOverlays.get(i).isOverlayEnabled())
                            enableOverlays.add(overlays.final_command.get(i));
                        else
                            disableOverlays.add(overlays.final_command.get(i));
                    }
                    ThemeManager.disableOverlay(context, disableOverlays);
                    if (overlays.mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        final ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        final List<String> all_installed_overlays = ThemeManager.listAllOverlays(context);
                        for (int i = 0; i < all_installed_overlays.size(); i++) {
                            if (!Packages.getOverlayParent(context, all_installed_overlays.get(i))
                                    .equals(overlays.theme_pid)) {
                                disableBeforeEnabling.add(all_installed_overlays.get(i));
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
        protected void onPostExecute(final Void result) {
            final Overlays overlays = this.ref.get();
            final Context context = this.refContext.get();
            if ((overlays != null) && (context != null)) {
                final Activity activity = overlays.getActivity();
                overlays.progressBar.setVisibility(View.GONE);
                if (!overlays.final_runner.isEmpty()) {
                    if (overlays.needsRecreate(context)) {
                        final Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                overlays.overlaysLists = ((OverlaysAdapter) overlays.mAdapter)
                                        .getOverlayList();
                                for (int i = 0; i < overlays.overlaysLists.size(); i++) {
                                    final OverlaysItem currentOverlay = overlays.overlaysLists.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(
                                            overlays.updateEnabledOverlays());
                                    activity.runOnUiThread(() ->
                                            overlays.mAdapter.notifyDataSetChanged());
                                }
                            } catch (final Exception e) {
                                // Consume window refresh
                            }
                        }, (long) References.REFRESH_WINDOW_DELAY);
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
        final WeakReference<Overlays> ref;
        final WeakReference<Context> refContext;

        Phase4_finishUpdateFunction(final Overlays overlays) {
            super();
            this.ref = new WeakReference<>(overlays);
            this.refContext = new WeakReference<>(overlays.getContext());
        }

        @Override
        protected void onPreExecute() {
            final Overlays overlays = this.ref.get();
            final Context context = this.refContext.get();
            if ((context != null) && (overlays != null)) {
                if (overlays.mCompileDialog != null) overlays.mCompileDialog.dismiss();

                // Add dummy intent to be able to close the notification on click
                final Intent notificationIntent = new Intent(context, InformationActivity.class);
                notificationIntent.putExtra("theme_name", overlays.theme_name);
                notificationIntent.putExtra("theme_pid", overlays.theme_pid);
                notificationIntent.setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                final PendingIntent intent =
                        PendingIntent.getActivity(context, 0, notificationIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT);

                overlays.mNotifyManager.cancel(References.notification_id_compiler);
                if (!overlays.has_failed) {
                    // Closing off the persistent notification
                    if (overlays.checkActiveNotifications()) {
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
                            overlays.mBuilder.setVibrate(new long[]{100L, 200L, 100L, 500L});
                        }
                        overlays.mNotifyManager.notify(
                                References.notification_id_compiler, overlays.mBuilder.build());
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

                if (InformationActivity.compilingProcess &&
                        InformationActivity.shouldRestartActivity) {
                    // Gracefully finish
                    InformationActivity.shouldRestartActivity = false;
                    InformationActivity.compilingProcess = false;
                    Broadcasts.sendActivityFinisherMessage(
                            overlays.getContext(), overlays.theme_pid);
                }
            }
        }

        @Override
        protected Void doInBackground(final Void... voids) {
            final Overlays overlays = this.ref.get();
            if (overlays != null) {
                final Activity activity = overlays.getActivity();
                final Context context = this.refContext.get();

                if (!overlays.has_failed || (overlays.final_runner.size() > overlays.fail_count)) {
                    new StringBuilder();
                    if (overlays.compile_enable_mode && overlays.mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        final ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        final List<String> all_installed_overlays = ThemeManager.listAllOverlays(context);
                        for (final String p : all_installed_overlays) {
                            if (!overlays.theme_pid.equals(Packages.
                                    getOverlayParent(context, p))) {
                                disableBeforeEnabling.add(p);
                            } else {
                                for (final OverlaysItem oi : overlays.checkedOverlays) {
                                    final String targetOverlay = oi.getPackageName();
                                    if (targetOverlay.equals(
                                            Packages.getOverlayTarget(context, p))) {
                                        disableBeforeEnabling.add(p);
                                    }
                                }
                            }
                        }
                        if (checkThemeInterfacer(context)) {
                            ThemeManager.disableOverlay(context, disableBeforeEnabling);
                        } else {
                            final StringBuilder final_commands = new StringBuilder(ThemeManager.disableOverlay);
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

                    if (overlays.final_runner.isEmpty()) {
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
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                overlays.overlaysLists =
                                        ((OverlaysAdapter) overlays.mAdapter).getOverlayList();
                                for (int i = 0; i < overlays.overlaysLists.size(); i++) {
                                    final OverlaysItem currentOverlay = overlays.overlaysLists.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(
                                            overlays.updateEnabledOverlays());
                                    activity.runOnUiThread(() ->
                                            overlays.mAdapter.notifyDataSetChanged());
                                }
                            } catch (final Exception e) {
                                // Consume window refresh
                            }
                        }, (long) REFRESH_WINDOW_DELAY);
                    }
                }

                if (!overlays.late_install.isEmpty() && !Systems.isSamsung(context)) {
                    // Install remaining overlays
                    final HandlerThread thread = new HandlerThread("LateInstallThread",
                            Thread.MAX_PRIORITY);
                    thread.start();
                    final Handler handler = new Handler(thread.getLooper());
                    final Runnable r = () -> {
                        final ArrayList<String> packages = new ArrayList<>();
                        for (final String o : overlays.late_install) {
                            ThemeManager.installOverlay(context, o);
                            final String packageName =
                                    o.substring(o.lastIndexOf('/') + 1, o.lastIndexOf('-'));
                            packages.add(packageName);
                            if ((Systems.checkThemeInterfacer(context) &&
                                    !Systems.isBinderInterfacer(context)) ||
                                    Systems.checkAndromeda(context)) {
                                // Wait until the overlays to fully install so on compile enable
                                // mode it can be enabled after.
                                Substratum.startWaitingInstall();
                                do {
                                    try {
                                        Thread.sleep((long) Overlays.THREAD_WAIT_DURATION);
                                    } catch (final InterruptedException e) {
                                        // Still waiting
                                    }
                                } while (Substratum.isWaitingInstall());
                            }
                        }
                        if (overlays.compile_enable_mode) {
                            ThemeManager.enableOverlay(context, packages);
                        }
                        Substratum.getInstance().unregisterFinishReceiver();
                        thread.quitSafely();
                    };
                    handler.post(r);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
        }
    }
}