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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.Substratum;
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

import static projekt.substratum.InformationActivity.currentShownLunchBar;
import static projekt.substratum.common.Internal.COMPILE_ENABLE;
import static projekt.substratum.common.Internal.COMPILE_UPDATE;
import static projekt.substratum.common.Internal.DISABLE_MODE;
import static projekt.substratum.common.Internal.ENABLE_DISABLE;
import static projekt.substratum.common.Internal.ENABLE_MODE;
import static projekt.substratum.common.Internal.ENCRYPTED_FILE_EXTENSION;
import static projekt.substratum.common.Internal.OVERLAYS_DIR;
import static projekt.substratum.common.Internal.PACKAGE_INSTALL_URI;
import static projekt.substratum.common.Internal.SPECIAL_SNOWFLAKE_DELAY;
import static projekt.substratum.common.Internal.THEME_NAME;
import static projekt.substratum.common.Internal.THEME_PID;
import static projekt.substratum.common.References.DEFAULT_NOTIFICATION_CHANNEL_ID;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.REFRESH_WINDOW_DELAY;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER;
import static projekt.substratum.common.Resources.LG_FRAMEWORK;
import static projekt.substratum.common.Resources.SAMSUNG_FRAMEWORK;
import static projekt.substratum.common.Resources.SETTINGS_ICONS;
import static projekt.substratum.common.Resources.SYSTEMUI;
import static projekt.substratum.common.Resources.SYSTEMUI_HEADERS;
import static projekt.substratum.common.Resources.SYSTEMUI_NAVBARS;
import static projekt.substratum.common.Resources.SYSTEMUI_QSTILES;
import static projekt.substratum.common.Resources.SYSTEMUI_STATUSBARS;
import static projekt.substratum.common.Resources.inNexusFilter;
import static projekt.substratum.common.Systems.checkOMS;
import static projekt.substratum.common.Systems.checkThemeInterfacer;

enum OverlaysManager {
    ;

    private static final String TAG = "OverlaysManager";

    /**
     * Consolidated function to compile overlays
     *
     * @param overlays Overlays fragment
     */
    static void selectStateMode(Overlays overlays, String state) {
        overlays.overlayItemList = overlays.mAdapter.getOverlayList();
        overlays.currentInstance.reset();

        for (int i = 0; i < overlays.overlayItemList.size(); i++) {
            OverlaysItem currentOverlay = overlays.overlayItemList.get(i);
            if (currentOverlay.isSelected()) {
                overlays.currentInstance.checkedOverlays.add(currentOverlay);
            }
        }

        if (!overlays.currentInstance.checkedOverlays.isEmpty()) {
            compileFunction compile = new compileFunction(overlays, state);
            if ((overlays.base_spinner.getSelectedItemPosition() != 0) &&
                    (overlays.base_spinner.getVisibility() == View.VISIBLE)) {
                compile.execute(overlays.base_spinner.getSelectedItem().toString());
            } else {
                compile.execute("");
            }
            for (OverlaysItem overlay : overlays.currentInstance.checkedOverlays) {
                Log.d("OverlayTargetPackageKiller", "Killing package : " + overlay
                        .getPackageName());
                overlays.activityManager.killBackgroundProcesses(overlay.getPackageName());
            }
        } else {
            if (overlays.toggle_all.isChecked()) overlays.toggle_all.setChecked(false);
            currentShownLunchBar = Lunchbar.make(
                    overlays.getActivityView(),
                    R.string.toast_disabled5,
                    Lunchbar.LENGTH_LONG);
            currentShownLunchBar.show();
        }
        overlays.currentInstance.stop();
    }

    /**
     * Consolidated function to enable, disable or swap overlay states
     *
     * @param overlays Overlays fragment
     * @param mode     ENABLE, DISABLE or SWAP modes
     */
    static void selectEnabledDisabled(Overlays overlays, String mode) {
        if (mode == null) {
            Log.e(TAG, "selectEnabledDisabled must use a valid mode, or else it will not work!");
            return;
        }
        overlays.currentInstance.reset();
        overlays.overlayItemList = overlays.mAdapter.getOverlayList();

        for (int i = 0; i < overlays.overlayItemList.size(); i++) {
            OverlaysItem currentOverlay = overlays.overlayItemList.get(i);
            if (overlays.mContext != null) {
                if (Systems.checkOMS(overlays.mContext) &&
                        currentOverlay.isSelected() &&
                        !mode.equals(ENABLE_DISABLE)) {
                    // This is an OMS device, so we can check enabled status
                    if (mode.equals(ENABLE_MODE) && !currentOverlay.isOverlayEnabled()) {
                        overlays.currentInstance.checkedOverlays.add(currentOverlay);
                    } else if (!mode.equals(ENABLE_MODE) && currentOverlay.isOverlayEnabled()) {
                        overlays.currentInstance.checkedOverlays.add(currentOverlay);
                    }
                } else if (currentOverlay.isSelected()) {
                    overlays.currentInstance.checkedOverlays.add(currentOverlay);
                } else {
                    currentOverlay.setSelected(false);
                    overlays.mAdapter.notifyDataSetChanged();
                }
            }
        }

        if (overlays.mContext != null && Systems.checkOMS(overlays.mContext)) {
            if (!overlays.currentInstance.checkedOverlays.isEmpty()) {
                compileFunction compile = new compileFunction(overlays, mode);
                if ((overlays.base_spinner.getSelectedItemPosition() != 0) &&
                        (overlays.base_spinner.getVisibility() == View.VISIBLE)) {
                    compile.execute(overlays.base_spinner.getSelectedItem().toString());
                } else {
                    compile.execute("");
                }
                for (OverlaysItem overlay : overlays.currentInstance.checkedOverlays) {
                    Log.d("OverlayTargetPackageKiller", "Killing package: " +
                            overlay.getPackageName());
                    overlays.activityManager.killBackgroundProcesses(overlay.getPackageName());
                }
            } else {
                if (overlays.toggle_all.isChecked()) overlays.toggle_all.setChecked(false);
                currentShownLunchBar = Lunchbar.make(
                        overlays.getActivityView(),
                        R.string.toast_disabled5,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
            overlays.currentInstance.stop();
        }
    }

    /**
     * Consolidated function to disable overlays on legacy based devices
     *
     * @param overlays Overlays fragment
     */
    static void legacyDisable(Overlays overlays) {
        String current_directory;
        if (inNexusFilter()) {
            current_directory = PIXEL_NEXUS_DIR;
        } else {
            current_directory = LEGACY_NEXUS_DIR;
        }

        if (!overlays.currentInstance.checkedOverlays.isEmpty()) {
            if (Systems.isSamsung(overlays.mContext)) {
                if (Root.checkRootAccess() && Root.requestRootAccess()) {
                    ArrayList<String> checked_overlays = new ArrayList<>();
                    for (int i = 0; i < overlays.currentInstance.checkedOverlays.size(); i++) {
                        checked_overlays.add(
                                overlays.currentInstance.checkedOverlays.get(i)
                                        .getFullOverlayParameters());
                    }
                    ThemeManager.uninstallOverlay(overlays.mContext, checked_overlays);
                } else {
                    for (int i = 0; i < overlays.currentInstance.checkedOverlays.size(); i++) {
                        Uri packageURI = Uri.parse("package:" +
                                overlays.currentInstance.checkedOverlays.get(i)
                                        .getFullOverlayParameters());
                        Intent uninstallIntent =
                                new Intent(Intent.ACTION_DELETE, packageURI);
                        overlays.startActivity(uninstallIntent);
                    }
                }
            } else {
                for (int i = 0; i < overlays.currentInstance.checkedOverlays.size(); i++) {
                    FileOperations.mountRW();
                    FileOperations.delete(overlays.mContext, current_directory +
                            overlays.currentInstance.checkedOverlays.get(i)
                                    .getFullOverlayParameters() + ".apk");
                    overlays.mAdapter.notifyDataSetChanged();
                }
                // Untick all options in the adapter after compiling
                overlays.toggle_all.setChecked(false);
                overlays.overlayItemList = overlays.mAdapter.getOverlayList();
                for (int i = 0; i < overlays.overlayItemList.size(); i++) {
                    OverlaysItem currentOverlay = overlays.overlayItemList.get(i);
                    if (currentOverlay.isSelected()) {
                        currentOverlay.setSelected(false);
                    }
                }
                Toast.makeText(overlays.mContext,
                        overlays.getString(R.string.toast_disabled6),
                        Toast.LENGTH_SHORT).show();
                assert overlays.mContext != null;
                AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(overlays.mContext);
                alertDialogBuilder.setTitle(
                        overlays.getString(R.string.legacy_dialog_soft_reboot_title));
                alertDialogBuilder.setMessage(
                        overlays.getString(R.string.legacy_dialog_soft_reboot_text));
                alertDialogBuilder.setPositiveButton(
                        android.R.string.ok,
                        (dialog, id12) -> ElevatedCommands.reboot());
                alertDialogBuilder.setNegativeButton(
                        R.string.remove_dialog_later, (dialog, id1) -> {
                            overlays.progressBar.setVisibility(View.GONE);
                            dialog.dismiss();
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        } else {
            if (overlays.toggle_all.isChecked()) overlays.toggle_all.setChecked(false);
            currentShownLunchBar = Lunchbar.make(
                    overlays.getActivityView(),
                    R.string.toast_disabled5,
                    Lunchbar.LENGTH_LONG);
            currentShownLunchBar.show();
        }
        overlays.currentInstance.stop();
    }

    static void endingEnableDisable(Overlays overlays, Context context) {
        if ((overlays != null) && (context != null)) {
            if (!overlays.currentInstance.final_runner.isEmpty()) {
                List<String> to_analyze = new ArrayList<>();
                List<OverlaysItem> totality = overlays.currentInstance.checkedOverlays;
                for (int i = 0; i < totality.size(); i++) {
                    to_analyze.add(totality.get(i).getPackageName());
                }
                if (Packages.needsRecreate(context, to_analyze)) {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        // OMS may not have written all the changes so quickly just yet
                        // so we may need to have a small delay
                        try {
                            overlays.overlayItemList = overlays.mAdapter
                                    .getOverlayList();
                            for (int i = 0; i < overlays.overlayItemList.size(); i++) {
                                OverlaysItem currentOverlay = overlays.overlayItemList
                                        .get(i);
                                currentOverlay.setSelected(false);
                                currentOverlay.updateEnabledOverlays(
                                        overlays.getCurrentOverlays());
                                overlays.mAdapter.notifyDataSetChanged();
                            }
                        } catch (Exception e) {
                            // Consume window refresh
                        }
                    }, (long) References.REFRESH_WINDOW_DELAY);
                }
            } else {
                currentShownLunchBar = Lunchbar.make(
                        overlays.getActivityView(),
                        R.string.toast_disabled3,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
            overlays.progressBar.setVisibility(View.GONE);
            overlays.currentInstance.stop();
        }
    }

    /**
     * Main beef of the compilation process
     */
    static class compileFunction extends AsyncTask<String, Integer, String> {

        private WeakReference<Overlays> ref;
        private String currentPackageName = "";
        private String current_dialog_overlay;
        private String state;

        compileFunction(Overlays overlays, String state) {
            super();
            ref = new WeakReference<>(overlays);
            this.state = state;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Overlays overlays = ref.get();
            Log.d(SUBSTRATUM_BUILDER,
                    "Substratum is proceeding with your actions and is now actively running...");
            if (overlays != null) {
                Context context = overlays.getActivity();
                if (state.equals(COMPILE_ENABLE) || state.equals(COMPILE_UPDATE)) {
                    // This is the time when the notification should be shown on the user's screen
                    assert context != null;
                    overlays.mBuilder = new NotificationCompat.Builder(context,
                            References.DEFAULT_NOTIFICATION_CHANNEL_ID);
                    int notification_priority = Notification.PRIORITY_MAX;
                    overlays.mBuilder.setContentTitle(
                            context.getString(R.string.notification_initial_title))
                            .setProgress(100, 0, true)
                            .setSmallIcon(android.R.drawable.ic_popup_sync)
                            .setPriority(notification_priority)
                            .setOngoing(true);
                    overlays.mNotifyManager.notify(
                            References.notification_id_compiler, overlays.mBuilder.build());
                    overlays.mCompileDialog.setCancelable(false);
                    View sheetView = View.inflate(context,
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
                    TextView loader_string = overlays.mCompileDialog.findViewById(R.id.title);
                    if (loader_string != null)
                        loader_string.setText(context.getResources().getString(
                                R.string.sb_phase_1_loader));

                    try {
                        Resources themeResources = context.getPackageManager()
                                .getResourcesForApplication(overlays.theme_pid);
                        overlays.themeAssetManager = themeResources.getAssets();
                    } catch (PackageManager.NameNotFoundException e) {
                        // Suppress exception
                    }

                    // Change title in preparation for loop to change subtext
                    if (overlays.checkActiveNotifications()) {
                        overlays.mBuilder
                                .setContentTitle(
                                        context.getString(R.string.notification_processing_n))
                                .setProgress(100, 0, false);
                        overlays.mNotifyManager.notify(
                                References.notification_id_compiler, overlays.mBuilder.build());
                    }
                    if (loader_string != null)
                        loader_string.setText(
                                context.getResources().getString(R.string.sb_phase_2_loader));
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
                TextView textView = overlays.mCompileDialog.findViewById(R.id.current_object);
                if (textView != null) textView.setText(current_dialog_overlay);
                ImageView loader_image = overlays.mCompileDialog.findViewById(R.id.icon);
                if (loader_image != null) {
                    loader_image.setImageBitmap(
                            Packages.getBitmapFromDrawable(
                                    Packages.getAppIcon(overlays.mContext, this
                                            .currentPackageName)));
                }
                double progress = (overlays.currentInstance.current_amount /
                        overlays.currentInstance.total_amount) * 100.0;
                overlays.dialogProgress.setProgress((int) progress, true);
            }
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Overlays overlays = ref.get();
            if (overlays != null) {
                Context context = overlays.getActivity();
                switch (state) {
                    case COMPILE_ENABLE:
                        // It's compile and enable mode, we have to first sort out all the
                        // "pm install"'s from the final_commands
                        overlays.currentInstance.final_command
                                .addAll(overlays.currentInstance.final_runner);
                        break;
                    default:
                        overlays.currentInstance.final_command
                                .addAll(overlays.currentInstance.final_runner);
                        break;
                }
                switch (state) {
                    case ENABLE_MODE:
                        new finishEnableFunction(overlays).execute();
                        break;
                    case DISABLE_MODE:
                        new finishDisableFunction(overlays).execute();
                        break;
                    case ENABLE_DISABLE:
                        new finishEnableDisableFunction(overlays).execute();
                        break;
                    case COMPILE_ENABLE:
                    case COMPILE_UPDATE:
                        new finishUpdateFunction(overlays, state).execute();
                        if (overlays.currentInstance.has_failed) {
                            overlays.failedFunction(context);
                        } else {
                            // Restart SystemUI if an enabled SystemUI overlay is updated
                            if (checkOMS(context)) {
                                for (int i = 0; i <
                                        overlays.currentInstance.checkedOverlays.size(); i++) {
                                    String targetOverlay =
                                            overlays.currentInstance.checkedOverlays.get(i)
                                                    .getPackageName();
                                    if (targetOverlay.equals(SYSTEMUI)) {
                                        String packageName =
                                                overlays.currentInstance.checkedOverlays.get(i)
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
                        if (overlays.currentInstance.late_install.isEmpty()) {
                            Substratum.getInstance().unregisterFinishReceiver();
                        }
                        break;
                }
                if (Systems.isSamsung(context) &&
                        (overlays.currentInstance.late_install != null) &&
                        !overlays.currentInstance.late_install.isEmpty()) {
                    if (Root.checkRootAccess() && Root.requestRootAccess()) {
                        overlays.progressBar.setVisibility(View.VISIBLE);
                        overlays.currentInstance.overlaysWaiting =
                                overlays.currentInstance.late_install.size();
                        for (int i = 0; i < overlays.currentInstance.late_install.size(); i++) {
                            ThemeManager.installOverlay(
                                    overlays.getActivity(),
                                    overlays.currentInstance.late_install.get(i));
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = FileProvider.getUriForFile(
                                context,
                                context.getApplicationContext().getPackageName() + ".provider",
                                new File(overlays.currentInstance.late_install.get(0)));
                        intent.setDataAndType(uri, PACKAGE_INSTALL_URI);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        overlays.startActivityForResult(intent, 2486);
                    }
                } else if (!Systems.checkOMS(context) &&
                        (overlays.currentInstance.final_runner.size() ==
                                overlays.currentInstance.fail_count)) {
                    AlertDialog.Builder alertDialogBuilder =
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
                if (overlays.mixAndMatchMode && !Systems.checkOMS(context)) {
                    String current_directory;
                    if (inNexusFilter()) {
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

                // Enable finish install listener
                boolean needToWait = Substratum.needToWaitInstall() && Systems.checkOMS(context);
                if (needToWait) {
                    Substratum.getInstance().registerFinishReceiver();
                }

                overlays.currentInstance.total_amount =
                        (double) overlays.currentInstance.checkedOverlays.size();
                for (int i = 0; i < overlays.currentInstance.checkedOverlays.size(); i++) {
                    String type1a = "";
                    String type1b = "";
                    String type1c = "";
                    String type2 = "";
                    String type3;
                    String type4 = "";

                    OverlaysItem checked = overlays.currentInstance.checkedOverlays.get(i);

                    overlays.currentInstance.current_amount = (double) (i + 1);
                    String theme_name_parsed =
                            overlays.theme_name.replaceAll("\\s+", "")
                                    .replaceAll("[^a-zA-Z0-9]+", "");

                    String current_overlay = checked.getPackageName();
                    current_dialog_overlay =
                            '\'' + Packages.getPackageName(context, current_overlay) + '\'';
                    currentPackageName = current_overlay;

                    if (state.equals(COMPILE_UPDATE) || state.equals(COMPILE_ENABLE)) {
                        publishProgress((int) overlays.currentInstance.current_amount);
                        if (state.equals(COMPILE_ENABLE)) {
                            if (overlays.currentInstance.final_runner == null) {
                                overlays.currentInstance.final_runner = new ArrayList<>();
                            }
                            String package_name = checked.getFullOverlayParameters();
                            if (Packages.isPackageInstalled(context, package_name) ||
                                    state.equals(COMPILE_ENABLE)) {
                                overlays.currentInstance.final_runner.add(package_name);
                            }
                        }
                        try {
                            String packageTitle = "";
                            if (projekt.substratum.common.Resources.allowedSystemUIOverlay
                                    (current_overlay)) {
                                switch (current_overlay) {
                                    case SYSTEMUI_HEADERS:
                                        packageTitle = context.getString(R.string.systemui_headers);
                                        break;
                                    case SYSTEMUI_NAVBARS:
                                        packageTitle = context.getString(R.string
                                                .systemui_navigation);
                                        break;
                                    case SYSTEMUI_STATUSBARS:
                                        packageTitle = context.getString(R.string
                                                .systemui_statusbar);
                                        break;
                                    case SYSTEMUI_QSTILES:
                                        packageTitle = context.getString(R.string
                                                .systemui_qs_tiles);
                                        break;
                                }
                            } else if (projekt.substratum.common.Resources.allowedSettingsOverlay
                                    (current_overlay)) {
                                switch (current_overlay) {
                                    case SETTINGS_ICONS:
                                        packageTitle = context.getString(R.string.settings_icons);
                                        break;
                                }
                            } else if (projekt.substratum.common.Resources.allowedFrameworkOverlay
                                    (current_overlay)) {
                                switch (current_overlay) {
                                    case SAMSUNG_FRAMEWORK:
                                        packageTitle = context.getString(
                                                R.string.samsung_framework);
                                        break;
                                    case LG_FRAMEWORK:
                                        packageTitle = context.getString(R.string.lg_framework);
                                        break;
                                }
                            } else {
                                ApplicationInfo applicationInfo = null;
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
                                        (double) overlays.currentInstance.checkedOverlays.size())
                                        * 100.0), false);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    overlays.mBuilder.setContentText('"' + packageTitle + '"');
                                } else {
                                    overlays.mBuilder.setContentText(
                                            context.getString(R.string.notification_processing) +
                                                    '"' + packageTitle + '"');
                                }
                                overlays.mNotifyManager.notify(References.notification_id_compiler,
                                        overlays.mBuilder.build());
                            }

                            String unparsedSuffix;
                            boolean useType3CommonDir = false;
                            if (!sUrl[0].isEmpty()) {
                                useType3CommonDir = overlays.themeAssetManager
                                        .list(OVERLAYS_DIR + '/' + current_overlay +
                                                "/type3-common").length > 0;
                                if (useType3CommonDir) {
                                    unparsedSuffix = "/type3-common";
                                } else {
                                    unparsedSuffix = "/type3_" + unparsedVariant;
                                }
                            } else {
                                unparsedSuffix = "/res";
                            }

                            String parsedSuffix = ((!sUrl[0].isEmpty()) ?
                                    ("/type3_" + parsedVariant) : "/res");
                            type3 = parsedVariant;

                            String workingDirectory = context.getCacheDir().getAbsolutePath() +
                                    References.SUBSTRATUM_BUILDER_CACHE.substring(0,
                                            References.SUBSTRATUM_BUILDER_CACHE.length() - 1);

                            File created = new File(workingDirectory);
                            if (created.exists()) {
                                FileOperations.delete(context, created.getAbsolutePath());
                            }
                            FileOperations.createNewFolder(context, created
                                    .getAbsolutePath());
                            String listDir = OVERLAYS_DIR + '/' + current_overlay +
                                    unparsedSuffix;

                            FileOperations.copyFileOrDir(
                                    overlays.themeAssetManager,
                                    listDir,
                                    workingDirectory + parsedSuffix,
                                    listDir,
                                    overlays.theme_cipher
                            );

                            if (useType3CommonDir) {
                                String type3Dir = OVERLAYS_DIR + '/' +
                                        current_overlay +
                                        "/type3_" + unparsedVariant;
                                FileOperations.copyFileOrDir(
                                        overlays.themeAssetManager,
                                        type3Dir,
                                        workingDirectory + parsedSuffix,
                                        type3Dir,
                                        overlays.theme_cipher
                                );
                            }

                            if (checked.is_variant_chosen || !sUrl[0].isEmpty()) {
                                // Type 1a
                                if (checked.is_variant_chosen1) {
                                    type1a = checked.getSelectedVariantName();
                                    Log.d(SUBSTRATUM_BUILDER, "You have selected variant file \"" +
                                            checked.getSelectedVariantName() + '"');
                                    Log.d(SUBSTRATUM_BUILDER, "Moving variant file to: " +
                                            workingDirectory + parsedSuffix + "/values/type1a.xml");

                                    String to_copy =
                                            OVERLAYS_DIR + '/' + current_overlay +
                                                    "/type1a_" +
                                                    checked.getSelectedVariantName() +
                                                    (overlays.encrypted ? ".xml" +
                                                            ENCRYPTED_FILE_EXTENSION : ".xml");

                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            to_copy,
                                            workingDirectory + parsedSuffix + (
                                                    overlays.encrypted ?
                                                            "/values/type1a.xml" +
                                                                    ENCRYPTED_FILE_EXTENSION :
                                                            "/values/type1a.xml"),
                                            to_copy,
                                            overlays.theme_cipher);
                                }

                                // Type 1b
                                if (checked.is_variant_chosen2) {
                                    type1b = checked.getSelectedVariantName2();
                                    Log.d(SUBSTRATUM_BUILDER, "You have selected variant file \"" +
                                            checked.getSelectedVariantName2() + '"');
                                    Log.d(SUBSTRATUM_BUILDER, "Moving variant file to: " +
                                            workingDirectory + parsedSuffix + "/values/type1b.xml");

                                    String to_copy =
                                            OVERLAYS_DIR + '/' + current_overlay +
                                                    "/type1b_" +
                                                    checked.getSelectedVariantName2() +
                                                    (overlays.encrypted ? ".xml" +
                                                            ENCRYPTED_FILE_EXTENSION : ".xml");

                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            to_copy,
                                            workingDirectory + parsedSuffix + (
                                                    overlays.encrypted ?
                                                            "/values/type1b.xml" +
                                                                    ENCRYPTED_FILE_EXTENSION :
                                                            "/values/type1b.xml"),
                                            to_copy,
                                            overlays.theme_cipher);
                                }
                                // Type 1c
                                if (checked.is_variant_chosen3) {
                                    type1c = checked.getSelectedVariantName3();
                                    Log.d(SUBSTRATUM_BUILDER, "You have selected variant file \"" +
                                            checked.getSelectedVariantName3() + '"');
                                    Log.d(SUBSTRATUM_BUILDER, "Moving variant file to: " +
                                            workingDirectory + parsedSuffix + "/values/type1c.xml");

                                    String to_copy =
                                            OVERLAYS_DIR + '/' + current_overlay +
                                                    "/type1c_" +
                                                    checked.getSelectedVariantName3() +
                                                    (overlays.encrypted ? ".xml" +
                                                            ENCRYPTED_FILE_EXTENSION : ".xml");

                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            to_copy,
                                            workingDirectory + parsedSuffix + (
                                                    overlays.encrypted ?
                                                            "/values/type1c.xml" +
                                                                    ENCRYPTED_FILE_EXTENSION :
                                                            "/values/type1c.xml"),
                                            to_copy,
                                            overlays.theme_cipher);
                                }

                                String packageName =
                                        (checked.is_variant_chosen1 ?
                                                checked.getSelectedVariantName() : "") +
                                                (overlays.currentInstance.checkedOverlays
                                                        .get(i).is_variant_chosen2 ?
                                                        overlays.currentInstance.checkedOverlays
                                                                .get(i)
                                                                .getSelectedVariantName2() : "") +
                                                (overlays.currentInstance.checkedOverlays
                                                        .get(i).is_variant_chosen3 ?
                                                        overlays.currentInstance.checkedOverlays
                                                                .get(i)
                                                                .getSelectedVariantName3() : "") +
                                                (overlays.currentInstance.checkedOverlays
                                                        .get(i).is_variant_chosen5 ?
                                                        overlays.currentInstance.checkedOverlays
                                                                .get(i)
                                                                .getSelectedVariantName5() : "")
                                                        .replaceAll("\\s+", "").replaceAll
                                                        ("[^a-zA-Z0-9]+", "");

                                if (checked.is_variant_chosen5) {
                                    // Copy over the type4 assets
                                    type4 = checked.getSelectedVariantName5();
                                    String type4folder = "/type4_" + type4;
                                    String type4folderOutput = "/assets";
                                    String to_copy2 = OVERLAYS_DIR + '/' +
                                            current_overlay +
                                            type4folder;
                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            to_copy2,
                                            workingDirectory + type4folderOutput,
                                            to_copy2,
                                            overlays.theme_cipher);
                                }
                                if (checked.is_variant_chosen4) {
                                    packageName = (packageName + overlays.currentInstance
                                            .checkedOverlays.get(i)
                                            .getSelectedVariantName4()).replaceAll("\\s+", "")
                                            .replaceAll("[^a-zA-Z0-9]+", "");

                                    // Copy over the type2 assets
                                    type2 = checked.getSelectedVariantName4();
                                    String type2folder = "/type2_" + type2;
                                    String to_copy = OVERLAYS_DIR + '/' +
                                            current_overlay +
                                            type2folder;
                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            to_copy,
                                            workingDirectory + type2folder,
                                            to_copy,
                                            overlays.theme_cipher);

                                    // Let's get started
                                    Log.d(SUBSTRATUM_BUILDER, "Currently processing package" +
                                            " \"" + checked.getFullOverlayParameters() + "\"...");

                                    overlays.compileInstance = new SubstratumBuilder();
                                    overlays.compileInstance.beginAction(
                                            context,
                                            current_overlay,
                                            overlays.theme_name,
                                            packageName,
                                            checked.getSelectedVariantName4(),
                                            !sUrl[0].isEmpty() ? sUrl[0] : null,
                                            overlays.theme_version,
                                            Systems.checkOMS(context),
                                            overlays.theme_pid,
                                            parsedSuffix,
                                            type1a,
                                            type1b,
                                            type1c,
                                            type2,
                                            type3,
                                            type4,
                                            null,
                                            false
                                    );
                                } else {
                                    Log.d(SUBSTRATUM_BUILDER, "Currently processing package" +
                                            " \"" + checked.getFullOverlayParameters() + "\"...");
                                    overlays.compileInstance = new SubstratumBuilder();
                                    overlays.compileInstance.beginAction(
                                            context,
                                            current_overlay,
                                            overlays.theme_name,
                                            packageName,
                                            null,
                                            !sUrl[0].isEmpty() ? sUrl[0] : null,
                                            overlays.theme_version,
                                            Systems.checkOMS(context),
                                            overlays.theme_pid,
                                            parsedSuffix,
                                            type1a,
                                            type1b,
                                            type1c,
                                            type2,
                                            type3,
                                            type4,
                                            null,
                                            false
                                    );
                                }
                                if (overlays.compileInstance.has_errored_out) {
                                    if (!overlays.compileInstance.getErrorLogs()
                                            .contains("type3") ||
                                            !overlays.compileInstance.getErrorLogs().contains(
                                                    "does not exist")) {
                                        overlays.currentInstance.fail_count += 1;
                                        if (overlays.currentInstance.error_logs.length() == 0) {
                                            overlays.currentInstance.error_logs.append(
                                                    overlays.compileInstance.getErrorLogs());
                                        } else {
                                            overlays.currentInstance.error_logs.append('\n')
                                                    .append(overlays.compileInstance
                                                            .getErrorLogs());
                                        }
                                        overlays.currentInstance.failed_packages
                                                .append(current_overlay);
                                        overlays.currentInstance.failed_packages.append(" (");
                                        overlays.currentInstance.failed_packages.append(
                                                Packages.getAppVersion(context,
                                                        current_overlay));
                                        overlays.currentInstance.failed_packages.append(")\n");
                                        overlays.currentInstance.has_failed = true;
                                    } else {
                                        overlays.currentInstance.missingType3 = true;
                                    }
                                } else {
                                    if (overlays.compileInstance.special_snowflake ||
                                            !overlays.compileInstance.no_install.isEmpty()) {
                                        overlays.currentInstance.late_install.add(
                                                overlays.compileInstance.no_install);
                                    } else if (needToWait) {
                                        // Thread wait
                                        Substratum.startWaitingInstall();
                                        do {
                                            try {
                                                Thread.sleep((long) SPECIAL_SNOWFLAKE_DELAY);
                                            } catch (InterruptedException e) {
                                                Thread.currentThread().interrupt();
                                            }
                                        } while (Substratum.isWaitingInstall());
                                    }
                                }
                            } else {
                                Log.d(SUBSTRATUM_BUILDER, "Currently processing package" +
                                        " \"" + current_overlay + '.' + theme_name_parsed +
                                        "\"...");
                                overlays.compileInstance = new SubstratumBuilder();
                                overlays.compileInstance.beginAction(
                                        context,
                                        current_overlay,
                                        overlays.theme_name,
                                        null,
                                        null,
                                        null,
                                        overlays.theme_version,
                                        Systems.checkOMS(context),
                                        overlays.theme_pid,
                                        parsedSuffix,
                                        type1a,
                                        type1b,
                                        type1c,
                                        type2,
                                        type3,
                                        type4,
                                        null,
                                        false
                                );

                                if (overlays.compileInstance.has_errored_out) {
                                    overlays.currentInstance.fail_count += 1;
                                    if (overlays.currentInstance.error_logs.length() == 0) {
                                        overlays.currentInstance.error_logs.append(
                                                overlays.compileInstance.getErrorLogs());
                                    } else {
                                        overlays.currentInstance.error_logs.append('\n')
                                                .append(overlays.compileInstance.getErrorLogs());
                                    }
                                    overlays.currentInstance.failed_packages
                                            .append(current_overlay);
                                    overlays.currentInstance.failed_packages.append(" (");
                                    overlays.currentInstance.failed_packages.append(
                                            Packages.getAppVersion(context, current_overlay));
                                    overlays.currentInstance.failed_packages.append(")\n");
                                    overlays.currentInstance.has_failed = true;
                                } else {
                                    if (overlays.compileInstance.special_snowflake ||
                                            !overlays.compileInstance.no_install.isEmpty()) {
                                        overlays.currentInstance.late_install
                                                .add(overlays.compileInstance.no_install);
                                    } else if (needToWait) {
                                        // Thread wait
                                        Substratum.startWaitingInstall();
                                        do {
                                            try {
                                                Thread.sleep((long) SPECIAL_SNOWFLAKE_DELAY);
                                            } catch (InterruptedException e) {
                                                Thread.currentThread().interrupt();
                                            }
                                        } while (Substratum.isWaitingInstall());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(SUBSTRATUM_BUILDER, "Main function has unexpectedly stopped!");
                        }
                    } else if (!state.equals(COMPILE_UPDATE)) {
                        String package_name = checked.getFullOverlayParameters();
                        if (Packages.isPackageInstalled(context, package_name)) {
                            overlays.currentInstance.final_runner.add(package_name);
                        }
                    }
                }
            }
            return null;
        }
    }

    /**
     * Concluding function to end the enabling process gracefully
     */
    static class finishEnableFunction extends AsyncTask<Void, Void, Void> {
        WeakReference<Overlays> ref;
        WeakReference<Context> refContext;

        finishEnableFunction(Overlays overlays) {
            super();
            ref = new WeakReference<>(overlays);
            refContext = new WeakReference<>(overlays.mContext);
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
                if (!overlays.currentInstance.final_runner.isEmpty()) {
                    if (overlays.mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        List<String> all_installed_overlays = ThemeManager.listAllOverlays
                                (context);
                        for (int i = 0; i < all_installed_overlays.size(); i++) {
                            if (!Packages.getOverlayParent(context, all_installed_overlays.get(i))
                                    .equals(overlays.theme_pid)) {
                                disableBeforeEnabling.add(all_installed_overlays.get(i));
                            }
                        }
                        ThemeManager.disableOverlay(context, disableBeforeEnabling);
                        ThemeManager.enableOverlay(context, overlays.currentInstance.final_command);
                    } else {
                        ThemeManager.enableOverlay(context, overlays.currentInstance.final_command);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Overlays overlays = ref.get();
            Context context = refContext.get();
            endingEnableDisable(overlays, context);
        }
    }

    /**
     * Concluding function to end the disabling process gracefully
     */
    static class finishDisableFunction extends AsyncTask<Void, Void, Void> {
        WeakReference<Overlays> ref;
        WeakReference<Context> refContext;

        finishDisableFunction(Overlays overlays) {
            super();
            ref = new WeakReference<>(overlays);
            refContext = new WeakReference<>(overlays.mContext);
        }

        @Override
        protected void onPreExecute() {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Activity activity = overlays.getActivity();
                if (!overlays.currentInstance.final_runner.isEmpty()) {
                    overlays.progressBar.setVisibility(View.VISIBLE);
                    if (overlays.toggle_all.isChecked())
                        if (activity != null)
                            activity.runOnUiThread(() -> overlays.toggle_all.setChecked(false));
                }
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Context context = overlays.getActivity();
                if (!overlays.currentInstance.final_runner.isEmpty()) {
                    ThemeManager.disableOverlay(context, overlays.currentInstance.final_command);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Overlays overlays = ref.get();
            Context context = refContext.get();
            endingEnableDisable(overlays, context);
        }
    }

    /**
     * Concluding function to end the swapping process gracefully
     */
    static class finishEnableDisableFunction extends AsyncTask<Void, Void, Void> {
        WeakReference<Overlays> ref;
        WeakReference<Context> refContext;

        finishEnableDisableFunction(Overlays overlays) {
            super();
            ref = new WeakReference<>(overlays);
            refContext = new WeakReference<>(overlays.mContext);
        }

        @Override
        protected void onPreExecute() {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Activity activity = overlays.getActivity();
                if (!overlays.currentInstance.final_runner.isEmpty()) {
                    overlays.progressBar.setVisibility(View.VISIBLE);
                    if (overlays.toggle_all.isChecked()) {
                        if (activity != null) {
                            activity.runOnUiThread(() -> overlays.toggle_all.setChecked(false));
                        }
                    }
                }
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Context context = overlays.getActivity();
                if (!overlays.currentInstance.final_runner.isEmpty()) {
                    ArrayList<String> enableOverlays = new ArrayList<>();
                    ArrayList<String> disableOverlays = new ArrayList<>();
                    for (int i = 0; i < overlays.currentInstance.final_command.size(); i++) {
                        if (!overlays.currentInstance.checkedOverlays.get(i).isOverlayEnabled()) {
                            enableOverlays.add(overlays.currentInstance.final_command.get(i));
                        } else {
                            disableOverlays.add(overlays.currentInstance.final_command.get(i));
                        }
                    }
                    ThemeManager.disableOverlay(context, disableOverlays);
                    if (overlays.mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        List<String> all_installed_overlays = ThemeManager.listAllOverlays
                                (context);
                        for (int i = 0; i < all_installed_overlays.size(); i++) {
                            if (!Packages.getOverlayParent(context,
                                    all_installed_overlays.get(i)).equals(overlays.theme_pid)) {
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
        protected void onPostExecute(Void result) {
            Overlays overlays = ref.get();
            Context context = refContext.get();
            endingEnableDisable(overlays, context);
        }
    }

    /**
     * Concluding function to end the update process gracefully
     */
    static class finishUpdateFunction extends AsyncTask<Void, Void, Void> {
        WeakReference<Overlays> ref;
        WeakReference<Context> refContext;
        private String state;

        finishUpdateFunction(Overlays overlays, String state) {
            super();
            ref = new WeakReference<>(overlays);
            refContext = new WeakReference<>(overlays.mContext);
            this.state = state;
        }

        @Override
        protected void onPreExecute() {
            Overlays overlays = ref.get();
            Context context = refContext.get();
            if ((context != null) && (overlays != null)) {
                if (overlays.mCompileDialog != null) overlays.mCompileDialog.dismiss();

                // Add dummy intent to be able to close the notification on click
                Intent notificationIntent = new Intent(context, InformationActivity.class);
                notificationIntent.putExtra(THEME_NAME, overlays.theme_name);
                notificationIntent.putExtra(THEME_PID, overlays.theme_pid);
                notificationIntent.setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent intent =
                        PendingIntent.getActivity(context, 0, notificationIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT);

                overlays.mNotifyManager.cancel(References.notification_id_compiler);
                if (!overlays.currentInstance.has_failed) {
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

                    if (overlays.currentInstance.missingType3) {
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
                    // Gracefully finish, so that we won't close the activity when theme is upgraded
                    InformationActivity.shouldRestartActivity = false;
                    InformationActivity.compilingProcess = false;
                    Broadcasts.sendActivityFinisherMessage(
                            overlays.mContext, overlays.theme_pid);
                }

                overlays.currentInstance.stop();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Activity activity = overlays.getActivity();
                Context context = refContext.get();

                if (!overlays.currentInstance.has_failed ||
                        (overlays.currentInstance.final_runner.size() >
                                overlays.currentInstance.fail_count)) {
                    new StringBuilder();
                    if (state.equals(COMPILE_ENABLE) && overlays.mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        List<String> all_installed_overlays = ThemeManager.listAllOverlays
                                (context);
                        for (String p : all_installed_overlays) {
                            if (!overlays.theme_pid.equals(Packages.
                                    getOverlayParent(context, p))) {
                                disableBeforeEnabling.add(p);
                            } else {
                                for (OverlaysItem oi : overlays.currentInstance.checkedOverlays) {
                                    String targetOverlay = oi.getPackageName();
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
                            StringBuilder final_commands = new StringBuilder(
                                    ThemeManager.disableOverlay);
                            for (int i = 0; i < disableBeforeEnabling.size(); i++) {
                                final_commands
                                        .append(' ')
                                        .append(disableBeforeEnabling.get(i))
                                        .append(' ');
                            }
                            Log.d(TAG, final_commands.toString());
                        }
                    }

                    if (state.equals(COMPILE_ENABLE)) {
                        ThemeManager.enableOverlay(context, overlays.currentInstance.final_command);
                    }

                    if (activity != null) {
                        if (overlays.currentInstance.final_runner.isEmpty()) {
                            if (overlays.base_spinner.getSelectedItemPosition() == 0) {
                                activity.runOnUiThread(() ->
                                        overlays.mAdapter.notifyDataSetChanged());
                            } else {
                                activity.runOnUiThread(() ->
                                        overlays.mAdapter.notifyDataSetChanged());
                            }
                        } else {
                            activity.runOnUiThread(() ->
                                    overlays.progressBar.setVisibility(View.VISIBLE));
                            if (overlays.toggle_all.isChecked())
                                activity.runOnUiThread(() -> overlays.toggle_all.setChecked(false));
                            activity.runOnUiThread(() -> overlays.mAdapter.notifyDataSetChanged());
                        }
                        activity.runOnUiThread(() -> overlays.progressBar.setVisibility(View.GONE));
                    }

                    List<String> to_analyze = new ArrayList<>();
                    List<OverlaysItem> totality = overlays.currentInstance.checkedOverlays;
                    for (int i = 0; i < totality.size(); i++) {
                        to_analyze.add(totality.get(i).getPackageName());
                    }
                    if (Packages.needsRecreate(context, to_analyze)) {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                overlays.overlayItemList =
                                        overlays.mAdapter.getOverlayList();
                                for (int i = 0; i < overlays.overlayItemList.size(); i++) {
                                    OverlaysItem currentOverlay = overlays.overlayItemList
                                            .get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(
                                            overlays.getCurrentOverlays());
                                    if (activity != null) {
                                        activity.runOnUiThread(() ->
                                                overlays.mAdapter.notifyDataSetChanged());
                                    }
                                }
                            } catch (Exception e) {
                                // Consume window refresh
                            }
                        }, (long) REFRESH_WINDOW_DELAY);
                    }
                }

                if (!overlays.currentInstance.late_install.isEmpty() &&
                        !Systems.isSamsung(context)) {
                    // Install remaining overlays
                    HandlerThread thread = new HandlerThread("LateInstallThread",
                            Thread.MAX_PRIORITY);
                    thread.start();
                    Handler handler = new Handler(thread.getLooper());
                    Runnable r = () -> {
                        ArrayList<String> packages = new ArrayList<>();
                        for (String o : overlays.currentInstance.late_install) {
                            ThemeManager.installOverlay(context, o);
                            String packageName =
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
                                        Thread.sleep((long) SPECIAL_SNOWFLAKE_DELAY);
                                    } catch (InterruptedException e) {
                                        // Still waiting
                                    }
                                } while (Substratum.isWaitingInstall());
                            }
                        }
                        if (state.equals(COMPILE_ENABLE)) {
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
    }
}