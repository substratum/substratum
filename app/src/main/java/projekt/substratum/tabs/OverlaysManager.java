/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
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
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
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
import projekt.substratum.common.commands.SamsungOverlayCacher;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.compilers.SubstratumBuilder;
import projekt.substratum.util.helpers.Root;
import projekt.substratum.util.views.Lunchbar;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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
import static projekt.substratum.common.Internal.SPECIAL_SNOWFLAKE_DELAY_SS;
import static projekt.substratum.common.Internal.THEME_NAME;
import static projekt.substratum.common.Internal.THEME_PID;
import static projekt.substratum.common.References.DEFAULT_NOTIFICATION_CHANNEL_ID;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.REFRESH_WINDOW_DELAY;
import static projekt.substratum.common.References.SECURITY_UPDATE_WARN_AFTER;
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
import static projekt.substratum.common.Systems.checkSubstratumService;
import static projekt.substratum.common.Systems.checkThemeInterfacer;
import static projekt.substratum.common.Systems.isNewSamsungDevice;
import static projekt.substratum.common.Systems.isNewSamsungDeviceAndromeda;
import static projekt.substratum.common.Systems.isSystemSecurityPatchNewer;

class OverlaysManager {

    private static final String TAG = "OverlaysManager";

    /**
     * Consolidated function to compile overlays
     *
     * @param overlays Overlays fragment
     */
    static void selectStateMode(Overlays overlays, String state) {
        overlays.overlayItemList = overlays.overlaysAdapter.getOverlayList();
        overlays.currentInstance.reset();

        for (int i = 0; i < overlays.overlayItemList.size(); i++) {
            OverlaysItem currentOverlay = overlays.overlayItemList.get(i);
            if (currentOverlay.isSelected()) {
                overlays.currentInstance.checkedOverlays.add(currentOverlay);
            }
        }

        if (!overlays.currentInstance.checkedOverlays.isEmpty()) {
            compileFunction compile = new compileFunction(overlays, state);
            if ((overlays.baseSpinner.getSelectedItemPosition() != 0) &&
                    (overlays.baseSpinner.getVisibility() == View.VISIBLE)) {
                compile.execute(overlays.baseSpinner.getSelectedItem().toString());
            } else {
                compile.execute("");
            }
            for (OverlaysItem overlay : overlays.currentInstance.checkedOverlays) {
                Substratum.log("OverlayTargetPackageKiller", "Killing package : " + overlay
                        .getPackageName());
                overlays.activityManager.killBackgroundProcesses(overlay.getPackageName());
            }
        } else {
            if (overlays.toggleAll.isChecked()) overlays.toggleAll.setChecked(false);
            currentShownLunchBar = Lunchbar.make(
                    overlays.getActivityView(),
                    overlays.context.getString(R.string.toast_disabled5),
                    Snackbar.LENGTH_LONG);
            currentShownLunchBar.show();
        }
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
        overlays.overlayItemList = overlays.overlaysAdapter.getOverlayList();
        overlays.overlaysAdapter.refreshOverlayStateList(overlays.context);
        for (int i = 0; i < overlays.overlayItemList.size(); i++) {
            OverlaysItem currentOverlay = overlays.overlayItemList.get(i);
            if (overlays.context != null) {
                if (Systems.checkOMS(overlays.context) &&
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
                    overlays.overlaysAdapter.notifyDataSetChanged();
                }
            }
        }

        if (overlays.context != null && Systems.checkOMS(overlays.context)) {
            if (!overlays.currentInstance.checkedOverlays.isEmpty()) {
                compileFunction compile = new compileFunction(overlays, mode);
                if ((overlays.baseSpinner.getSelectedItemPosition() != 0) &&
                        (overlays.baseSpinner.getVisibility() == View.VISIBLE)) {
                    compile.execute(overlays.baseSpinner.getSelectedItem().toString());
                } else {
                    compile.execute("");
                }
                for (OverlaysItem overlay : overlays.currentInstance.checkedOverlays) {
                    Substratum.log("OverlayTargetPackageKiller", "Killing package: " +
                            overlay.getPackageName());
                    overlays.activityManager.killBackgroundProcesses(overlay.getPackageName());
                }
            } else {
                if (overlays.toggleAll.isChecked()) overlays.toggleAll.setChecked(false);
                currentShownLunchBar = Lunchbar.make(
                        overlays.getActivityView(),
                        overlays.context.getString(R.string.toast_disabled5),
                        Snackbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }
    }

    /**
     * Consolidated function to disable overlays on legacy based devices
     *
     * @param overlays Overlays fragment
     */
    static void legacyDisable(Overlays overlays) {
        String currentDirectory;
        if (inNexusFilter()) {
            currentDirectory = PIXEL_NEXUS_DIR;
        } else {
            currentDirectory = LEGACY_NEXUS_DIR;
        }

        if (!overlays.currentInstance.checkedOverlays.isEmpty()) {
            if (Systems.isSamsungDevice(overlays.context)) {
                if (Root.checkRootAccess() && Root.requestRootAccess()) {
                    ArrayList<String> checked_overlays = new ArrayList<>();
                    for (int i = 0; i < overlays.currentInstance.checkedOverlays.size(); i++) {
                        checked_overlays.add(
                                overlays.currentInstance.checkedOverlays.get(i)
                                        .getFullOverlayParameters());
                    }
                    ThemeManager.uninstallOverlay(overlays.context, checked_overlays);
                } else {
                    for (int i = 0; i < overlays.currentInstance.checkedOverlays.size(); i++) {
                        Uri packageURI = Uri.parse("package:" +
                                overlays.currentInstance.checkedOverlays.get(i)
                                        .getFullOverlayParameters());
                        Intent uninstallIntent =
                                new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
                        overlays.startActivity(uninstallIntent);
                    }
                }
            } else {
                overlays.overlaysAdapter.refreshOverlayStateList(overlays.context);
                for (int i = 0; i < overlays.currentInstance.checkedOverlays.size(); i++) {
                    FileOperations.mountRW();
                    FileOperations.delete(overlays.context, currentDirectory +
                            overlays.currentInstance.checkedOverlays.get(i)
                                    .getFullOverlayParameters() + ".apk");
                    overlays.overlaysAdapter.notifyDataSetChanged();
                }
                // Untick all options in the adapter after compiling
                overlays.toggleAll.setChecked(false);
                overlays.overlayItemList = overlays.overlaysAdapter.getOverlayList();
                for (int i = 0; i < overlays.overlayItemList.size(); i++) {
                    OverlaysItem currentOverlay = overlays.overlayItemList.get(i);
                    if (currentOverlay.isSelected()) {
                        currentOverlay.setSelected(false);
                    }
                }
                Toast.makeText(overlays.context,
                        overlays.getString(R.string.toast_disabled6),
                        Toast.LENGTH_SHORT).show();
                assert overlays.context != null;
                AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(overlays.context);
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
            if (overlays.toggleAll.isChecked()) overlays.toggleAll.setChecked(false);
            currentShownLunchBar = Lunchbar.make(
                    overlays.getActivityView(),
                    overlays.context.getString(R.string.toast_disabled5),
                    Snackbar.LENGTH_LONG);
            currentShownLunchBar.show();
        }
    }

    private static void endingEnableDisable(Overlays overlays, Context context) {
        if ((overlays != null) && (context != null)) {
            if (!overlays.currentInstance.finalRunner.isEmpty()) {
                List<String> to_analyze = new ArrayList<>();
                List<OverlaysItem> allOverlays = overlays.currentInstance.checkedOverlays;
                for (OverlaysItem overlay : allOverlays) {
                    to_analyze.add(overlay.getPackageName());
                }
                if (Packages.needsRecreate(context, to_analyze)) {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        // OMS may not have written all the changes so quickly just yet
                        // so we may need to have a small delay
                        try {
                            overlays.overlayItemList = overlays.overlaysAdapter.getOverlayList();
                            overlays.getCurrentOverlays();
                            overlays.overlaysAdapter.refreshOverlayStateList(context);
                            for (int i = 0; i < overlays.overlayItemList.size(); i++) {
                                OverlaysItem currentOverlay = overlays.overlayItemList.get(i);
                                currentOverlay.setSelected(false);
                                currentOverlay.updateEnabledOverlays(
                                        overlays.currentInstanceOverlays);
                                overlays.overlaysAdapter.notifyDataSetChanged();
                            }
                        } catch (Exception ignored) {
                            // Consume window refresh
                        }
                    }, (long) References.REFRESH_WINDOW_DELAY);
                }
            } else {
                currentShownLunchBar = Lunchbar.make(
                        overlays.getActivityView(),
                        overlays.context.getString(R.string.toast_disabled3),
                        Snackbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
            overlays.progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Main beef of the compilation process
     */
    static class compileFunction extends AsyncTask<String, Integer, String> {

        private final WeakReference<Overlays> ref;
        private final String state;
        private String currentPackageName = "";
        private String currentDialogOverlay;

        compileFunction(Overlays overlays, String state) {
            super();
            ref = new WeakReference<>(overlays);
            this.state = state;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Overlays overlays = ref.get();
            Substratum.log(SUBSTRATUM_BUILDER,
                    "Substratum is proceeding with your actions and is now actively running...");
            if (overlays != null) {
                Context context = overlays.getActivity();
                if (state.equals(COMPILE_ENABLE) || state.equals(COMPILE_UPDATE)) {
                    // This is the time when the notification should be shown on the user's screen
                    assert context != null;
                    overlays.builder = new NotificationCompat.Builder(context,
                            References.DEFAULT_NOTIFICATION_CHANNEL_ID);
                    int notificationPriority = Notification.PRIORITY_MAX;
                    overlays.builder.setContentTitle(
                            context.getString(R.string.notification_initial_title))
                            .setProgress(100, 0, true)
                            .setSmallIcon(android.R.drawable.ic_popup_sync)
                            .setPriority(notificationPriority)
                            .setOngoing(true);
                    overlays.notifyManager.notify(
                            References.NOTIFICATION_ID_COMPILER, overlays.builder.build());
                    overlays.compileDialog.setCancelable(false);
                    View sheetView = View.inflate(context, R.layout.tab_overlays_compile_sheet_dialog, null);
                    overlays.compileDialog.setContentView(sheetView);
                    BottomSheetBehavior sheetBehavior =
                            BottomSheetBehavior.from((View) sheetView.getParent());
                    overlays.compileDialog.setOnShowListener(dialogInterface ->
                            sheetBehavior.setPeekHeight(sheetView.getHeight()));
                    overlays.compileDialog.show();
                    InformationActivity.compilingProcess = true;

                    // Do not sleep the device when the sheet is raised
                    if (overlays.compileDialog.getWindow() != null) {
                        overlays.compileDialog.getWindow().addFlags(
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }

                    // Set the variables for the sheet dialog's titles
                    overlays.dialogProgress =
                            overlays.compileDialog.findViewById(R.id.loading_bar);
                    if (overlays.dialogProgress != null) {
                        overlays.dialogProgress.setIndeterminate(false);
                    }
                    TextView loaderString = overlays.compileDialog.findViewById(R.id.title);
                    if (loaderString != null)
                        loaderString.setText(context.getResources().getString(
                                R.string.sb_phase_1_loader));

                    try {
                        Resources themeResources = context.getPackageManager()
                                .getResourcesForApplication(overlays.themePid);
                        overlays.themeAssetManager = themeResources.getAssets();
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }

                    // Change title in preparation for loop to change subtext
                    if (overlays.checkActiveNotifications()) {
                        overlays.builder
                                .setContentTitle(
                                        context.getString(R.string.notification_processing_n))
                                .setProgress(100, 0, false);
                        overlays.notifyManager.notify(
                                References.NOTIFICATION_ID_COMPILER, overlays.builder.build());
                    }
                    if (loaderString != null)
                        loaderString.setText(
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
                TextView textView = overlays.compileDialog.findViewById(R.id.current_object);
                if (textView != null) textView.setText(currentDialogOverlay);
                ImageView loaderImage = overlays.compileDialog.findViewById(R.id.icon);
                if (loaderImage != null) {
                    loaderImage.setImageDrawable(
                            Packages.getAppIcon(overlays.context, this
                                    .currentPackageName));
                }
                double progress = (overlays.currentInstance.currentAmount /
                        overlays.currentInstance.totalAmount) * 100.0;
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
                        overlays.currentInstance.finalCommand
                                .addAll(overlays.currentInstance.finalRunner);
                        break;
                    default:
                        overlays.currentInstance.finalCommand
                                .addAll(overlays.currentInstance.finalRunner);
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
                        if (isNewSamsungDevice() || isNewSamsungDeviceAndromeda(context)) {
                            SamsungOverlayCacher samsungOverlayCacher =
                                    new SamsungOverlayCacher(context);
                            for (int i = 0; i <
                                    overlays.currentInstance.checkedOverlays.size(); i++) {
                                String packageName =
                                        overlays.currentInstance.checkedOverlays.get(i)
                                                .getFullOverlayParameters();
                                samsungOverlayCacher.updateSamsungCache(packageName);
                            }
                        }

                        new finishUpdateFunction(overlays, state).execute();
                        if (overlays.currentInstance.hasFailed) {
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
                        if (overlays.currentInstance.lateInstall.isEmpty()) {
                            Substratum.getInstance().unregisterFinishReceiver();
                        }
                        break;
                }
                if (Systems.isSamsungDevice(context) &&
                        (overlays.currentInstance.lateInstall != null) &&
                        !overlays.currentInstance.lateInstall.isEmpty()) {
                    if (Root.checkRootAccess() && Root.requestRootAccess()) {
                        overlays.progressBar.setVisibility(View.VISIBLE);
                        overlays.currentInstance.overlaysWaiting =
                                overlays.currentInstance.lateInstall.size();
                        if (Systems.checkOMS(context)) {
                            List<String> allEnabledOverlays = ThemeManager
                                    .listOverlays(context, ThemeManager.STATE_ENABLED);
                            ArrayList<String> overlaysToDisable = new ArrayList<>();
                            for (String pkg : overlays.currentInstance.lateInstall) {
                                if (allEnabledOverlays.contains(pkg)) {
                                    overlaysToDisable.add(pkg);
                                }
                            }
                            ThemeManager.disableOverlay(context, overlaysToDisable);
                        }
                        for (int i = 0; i < overlays.currentInstance.lateInstall.size(); i++) {
                            ThemeManager.installOverlay(
                                    overlays.getActivity(),
                                    overlays.currentInstance.lateInstall.get(i));
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = FileProvider.getUriForFile(
                                context,
                                context.getApplicationContext().getPackageName() + ".provider",
                                new File(overlays.currentInstance.lateInstall.get(0)));
                        intent.setDataAndType(uri, PACKAGE_INSTALL_URI);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        overlays.startActivityForResult(intent, 2486);
                    }
                } else if (!Systems.checkOMS(context) &&
                        (overlays.currentInstance.finalRunner.size() ==
                                overlays.currentInstance.failCount)) {
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
                overlays.overlaysAdapter.refreshOverlayStateList(overlays.context);
                overlays.overlaysAdapter.notifyDataSetChanged();
                if (overlays.toggleAll.isChecked()) overlays.toggleAll.setChecked(false);
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
                    String currentDirectory;
                    if (inNexusFilter()) {
                        currentDirectory = References.PIXEL_NEXUS_DIR;
                    } else {
                        currentDirectory = References.LEGACY_NEXUS_DIR;
                    }
                    File file = new File(currentDirectory);
                    if (file.exists()) {
                        FileOperations.mountRW();
                        FileOperations.delete(context, currentDirectory);
                    }
                }

                // Enable finish install listener
                boolean needToWait =
                        !Systems.isNewSamsungDevice() &&
                                Substratum.needToWaitInstall() &&
                                Systems.checkOMS(context) &&
                                !Systems.IS_PIE;

                if (needToWait) {
                    Substratum.getInstance().registerFinishReceiver();
                }

                overlays.currentInstance.totalAmount =
                        (double) overlays.currentInstance.checkedOverlays.size();
                for (int i = 0; i < overlays.currentInstance.checkedOverlays.size(); i++) {
                    String type1a = "";
                    String type1b = "";
                    String type1c = "";
                    String type2 = "";
                    String type3;
                    String type4 = "";

                    OverlaysItem checked = overlays.currentInstance.checkedOverlays.get(i);

                    overlays.currentInstance.currentAmount = (double) (i + 1);
                    String themeNameParsed =
                            overlays.themeName.replaceAll("\\s+", "")
                                    .replaceAll("[^a-zA-Z0-9]+", "");

                    String currentOverlay = checked.getPackageName();
                    currentDialogOverlay =
                            '\'' + Packages.getPackageName(context, currentOverlay) + '\'';
                    currentPackageName = currentOverlay;

                    if (state.equals(COMPILE_UPDATE) || state.equals(COMPILE_ENABLE)) {
                        publishProgress((int) overlays.currentInstance.currentAmount);
                        if (state.equals(COMPILE_ENABLE)) {
                            if (overlays.currentInstance.finalRunner == null) {
                                overlays.currentInstance.finalRunner = new ArrayList<>();
                            }
                            String package_name = checked.getFullOverlayParameters();
                            if (Packages.isPackageInstalled(context, package_name) ||
                                    state.equals(COMPILE_ENABLE)) {
                                overlays.currentInstance.finalRunner.add(package_name);
                            }
                        }
                        try {
                            String packageTitle = "";
                            if (projekt.substratum.common.Resources.allowedSystemUIOverlay
                                    (currentOverlay) ||
                                    projekt.substratum.common.Resources.allowedFrameworkOverlay
                                            (currentOverlay) ||
                                    projekt.substratum.common.Resources.allowedSettingsOverlay
                                            (currentOverlay)) {
                                switch (currentOverlay) {
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
                                    case SAMSUNG_FRAMEWORK:
                                        packageTitle = context.getString(
                                                R.string.samsung_framework);
                                        break;
                                    case LG_FRAMEWORK:
                                        packageTitle = context.getString(R.string.lg_framework);
                                        break;
                                    case SETTINGS_ICONS:
                                        packageTitle = context.getString(R.string.settings_icons);
                                        break;
                                }
                            } else {
                                ApplicationInfo applicationInfo = null;
                                try {
                                    applicationInfo = context.getPackageManager()
                                            .getApplicationInfo(currentOverlay, 0);
                                } catch (PackageManager.NameNotFoundException e) {
                                    e.printStackTrace();
                                }
                                packageTitle = context.getPackageManager()
                                        .getApplicationLabel(applicationInfo).toString();
                            }

                            // Initialize working notification
                            if (overlays.checkActiveNotifications()) {
                                overlays.builder.setProgress(100, (int) (((double) (i + 1) /
                                        (double) overlays.currentInstance.checkedOverlays.size())
                                        * 100.0), false);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    overlays.builder.setContentText('"' + packageTitle + '"');
                                } else {
                                    overlays.builder.setContentText(
                                            context.getString(R.string.notification_processing) +
                                                    '"' + packageTitle + '"');
                                }
                                overlays.notifyManager.notify(References.NOTIFICATION_ID_COMPILER,
                                        overlays.builder.build());
                            }

                            String unparsedSuffix;
                            boolean useType3CommonDir = false;
                            if (!sUrl[0].isEmpty()) {
                                useType3CommonDir = overlays.themeAssetManager
                                        .list(OVERLAYS_DIR + '/' + currentOverlay +
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
                            FileOperations.createNewFolder(context, created.getAbsolutePath());
                            String versionFile = OVERLAYS_DIR + '/' + currentOverlay + "/version";
                            String priorityFile = OVERLAYS_DIR + '/' + currentOverlay + "/priority";
                            String listDir = OVERLAYS_DIR + '/' + currentOverlay + unparsedSuffix;

                            FileOperations.copyFileOrDir(
                                    overlays.themeAssetManager,
                                    listDir,
                                    workingDirectory + parsedSuffix,
                                    listDir,
                                    overlays.themeCipher
                            );

                            FileOperations.copyFileOrDir(
                                    overlays.themeAssetManager,
                                    versionFile,
                                    workingDirectory + "/version",
                                    versionFile,
                                    overlays.themeCipher
                            );

                            if (!checkOMS(context)) {
                                FileOperations.copyFileOrDir(
                                        overlays.themeAssetManager,
                                        priorityFile,
                                        workingDirectory + "/priority",
                                        priorityFile,
                                        overlays.themeCipher
                                );
                            }

                            if (useType3CommonDir) {
                                String type3Dir = OVERLAYS_DIR + '/' +
                                        currentOverlay +
                                        "/type3_" + unparsedVariant;
                                FileOperations.copyFileOrDir(
                                        overlays.themeAssetManager,
                                        type3Dir,
                                        workingDirectory + parsedSuffix,
                                        type3Dir,
                                        overlays.themeCipher
                                );
                            }

                            if (checked.isVariantChosen || !sUrl[0].isEmpty()) {
                                // Type 1a
                                if (checked.isVariantChosen1) {
                                    type1a = checked.getSelectedVariantName();
                                    Substratum.log(SUBSTRATUM_BUILDER, "You have selected variant file \"" +
                                            checked.getSelectedVariantName() + '"');
                                    Substratum.log(SUBSTRATUM_BUILDER, "Moving variant file to: " +
                                            workingDirectory + parsedSuffix + "/values/type1a.xml");

                                    String toCopy =
                                            OVERLAYS_DIR + '/' + currentOverlay +
                                                    "/type1a_" +
                                                    checked.getSelectedVariantName() +
                                                    (overlays.encrypted ? ".xml" +
                                                            ENCRYPTED_FILE_EXTENSION : ".xml");

                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            toCopy,
                                            workingDirectory + parsedSuffix + (
                                                    overlays.encrypted ?
                                                            "/values/type1a.xml" +
                                                                    ENCRYPTED_FILE_EXTENSION :
                                                            "/values/type1a.xml"),
                                            toCopy,
                                            overlays.themeCipher);
                                }

                                // Type 1b
                                if (checked.isVariantChosen2) {
                                    type1b = checked.getSelectedVariantName2();
                                    Substratum.log(SUBSTRATUM_BUILDER, "You have selected variant file \"" +
                                            checked.getSelectedVariantName2() + '"');
                                    Substratum.log(SUBSTRATUM_BUILDER, "Moving variant file to: " +
                                            workingDirectory + parsedSuffix + "/values/type1b.xml");

                                    String toCopy =
                                            OVERLAYS_DIR + '/' + currentOverlay +
                                                    "/type1b_" +
                                                    checked.getSelectedVariantName2() +
                                                    (overlays.encrypted ? ".xml" +
                                                            ENCRYPTED_FILE_EXTENSION : ".xml");

                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            toCopy,
                                            workingDirectory + parsedSuffix + (
                                                    overlays.encrypted ?
                                                            "/values/type1b.xml" +
                                                                    ENCRYPTED_FILE_EXTENSION :
                                                            "/values/type1b.xml"),
                                            toCopy,
                                            overlays.themeCipher);
                                }
                                // Type 1c
                                if (checked.isVariantChosen3) {
                                    type1c = checked.getSelectedVariantName3();
                                    Substratum.log(SUBSTRATUM_BUILDER, "You have selected variant file \"" +
                                            checked.getSelectedVariantName3() + '"');
                                    Substratum.log(SUBSTRATUM_BUILDER, "Moving variant file to: " +
                                            workingDirectory + parsedSuffix + "/values/type1c.xml");

                                    String toCopy =
                                            OVERLAYS_DIR + '/' + currentOverlay +
                                                    "/type1c_" +
                                                    checked.getSelectedVariantName3() +
                                                    (overlays.encrypted ? ".xml" +
                                                            ENCRYPTED_FILE_EXTENSION : ".xml");

                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            toCopy,
                                            workingDirectory + parsedSuffix + (
                                                    overlays.encrypted ?
                                                            "/values/type1c.xml" +
                                                                    ENCRYPTED_FILE_EXTENSION :
                                                            "/values/type1c.xml"),
                                            toCopy,
                                            overlays.themeCipher);
                                }

                                String variantSection =
                                        (checked.isVariantChosen1 ?
                                                checked.getSelectedVariantName() : "") +
                                                (overlays.currentInstance.checkedOverlays
                                                        .get(i).isVariantChosen2 ?
                                                        overlays.currentInstance.checkedOverlays
                                                                .get(i)
                                                                .getSelectedVariantName2() : "") +
                                                (overlays.currentInstance.checkedOverlays
                                                        .get(i).isVariantChosen3 ?
                                                        overlays.currentInstance.checkedOverlays
                                                                .get(i)
                                                                .getSelectedVariantName3() : "") +
                                                (overlays.currentInstance.checkedOverlays
                                                        .get(i).isVariantChosen4 ?
                                                        overlays.currentInstance.checkedOverlays
                                                                .get(i)
                                                                .getSelectedVariantName4() : "") +
                                                (overlays.currentInstance.checkedOverlays
                                                        .get(i).isVariantChosen5 ?
                                                        overlays.currentInstance.checkedOverlays
                                                                .get(i)
                                                                .getSelectedVariantName5() : "")
                                                        .replaceAll("\\s+", "").replaceAll
                                                        ("[^a-zA-Z0-9]+", "");

                                if (checked.isVariantChosen5) {
                                    // Copy over the type4 assets
                                    type4 = checked.getSelectedVariantName5();
                                    String type4folder = "/type4_" + type4;
                                    String type4folderOutput = "/assets";
                                    String toCopy2 = OVERLAYS_DIR + '/' +
                                            currentOverlay +
                                            type4folder;
                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            toCopy2,
                                            workingDirectory + type4folderOutput,
                                            toCopy2,
                                            overlays.themeCipher);
                                }
                                if (checked.isVariantChosen4) {
                                    // Copy over the type2 assets
                                    type2 = checked.getSelectedVariantName4();
                                    String type2folder = "/type2_" + type2;
                                    String toCopy = OVERLAYS_DIR + '/' +
                                            currentOverlay +
                                            type2folder;
                                    FileOperations.copyFileOrDir(
                                            overlays.themeAssetManager,
                                            toCopy,
                                            workingDirectory + type2folder,
                                            toCopy,
                                            overlays.themeCipher);

                                    // Let's get started
                                    Substratum.log(SUBSTRATUM_BUILDER, "Currently processing package" +
                                            " \"" + checked.getFullOverlayParameters() + "\"...");

                                    overlays.compileInstance = new SubstratumBuilder();
                                    overlays.compileInstance.beginAction(
                                            context,
                                            currentOverlay,
                                            overlays.themeName,
                                            variantSection,
                                            checked.getSelectedVariantName4(),
                                            !sUrl[0].isEmpty() ? sUrl[0] : null,
                                            overlays.themeVersion,
                                            Systems.checkOMS(context),
                                            overlays.themePid,
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
                                    Substratum.log(SUBSTRATUM_BUILDER, "Currently processing package" +
                                            " \"" + checked.getFullOverlayParameters() + "\"...");
                                    overlays.compileInstance = new SubstratumBuilder();
                                    overlays.compileInstance.beginAction(
                                            context,
                                            currentOverlay,
                                            overlays.themeName,
                                            variantSection,
                                            null,
                                            !sUrl[0].isEmpty() ? sUrl[0] : null,
                                            overlays.themeVersion,
                                            Systems.checkOMS(context),
                                            overlays.themePid,
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
                                if (overlays.compileInstance.hasErroredOut) {
                                    if (!overlays.compileInstance.getErrorLogs()
                                            .contains("type3") ||
                                            !overlays.compileInstance.getErrorLogs().contains(
                                                    "does not exist")) {
                                        overlays.currentInstance.failCount += 1;
                                        if (overlays.currentInstance.errorLogs.length() == 0) {
                                            overlays.currentInstance.errorLogs.append(
                                                    overlays.compileInstance.getErrorLogs());
                                        } else {
                                            overlays.currentInstance.errorLogs.append('\n')
                                                    .append(overlays.compileInstance
                                                            .getErrorLogs());
                                        }
                                        overlays.currentInstance.failedPackages
                                                .append(currentOverlay);
                                        overlays.currentInstance.failedPackages.append(" (");
                                        overlays.currentInstance.failedPackages.append(
                                                Packages.getAppVersion(context,
                                                        currentOverlay));
                                        overlays.currentInstance.failedPackages.append(")\n");
                                        overlays.currentInstance.hasFailed = true;
                                    } else {
                                        overlays.currentInstance.missingType3 = true;
                                    }
                                } else {
                                    if (overlays.compileInstance.specialSnowflake ||
                                            !overlays.compileInstance.noInstall.isEmpty()) {
                                        overlays.currentInstance.lateInstall.add(
                                                overlays.compileInstance.noInstall);
                                    } else if (needToWait) {
                                        // Thread wait
                                        Substratum.startWaitingInstall();
                                        do {
                                            try {
                                                Thread.sleep((long) SPECIAL_SNOWFLAKE_DELAY);
                                            } catch (InterruptedException ignored) {
                                                Thread.currentThread().interrupt();
                                            }
                                        } while (Substratum.isWaitingInstall());
                                    }
                                }
                            } else {
                                Substratum.log(SUBSTRATUM_BUILDER, "Currently processing package" +
                                        " \"" + currentOverlay + '.' + themeNameParsed +
                                        "\"...");
                                overlays.compileInstance = new SubstratumBuilder();
                                overlays.compileInstance.beginAction(
                                        context,
                                        currentOverlay,
                                        overlays.themeName,
                                        null,
                                        null,
                                        null,
                                        overlays.themeVersion,
                                        Systems.checkOMS(context),
                                        overlays.themePid,
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

                                if (overlays.compileInstance.hasErroredOut) {
                                    overlays.currentInstance.failCount += 1;
                                    if (overlays.currentInstance.errorLogs.length() == 0) {
                                        overlays.currentInstance.errorLogs.append(
                                                overlays.compileInstance.getErrorLogs());
                                    } else {
                                        overlays.currentInstance.errorLogs.append('\n')
                                                .append(overlays.compileInstance.getErrorLogs());
                                    }
                                    overlays.currentInstance.failedPackages
                                            .append(currentOverlay);
                                    overlays.currentInstance.failedPackages.append(" (");
                                    overlays.currentInstance.failedPackages.append(
                                            Packages.getAppVersion(context, currentOverlay));
                                    overlays.currentInstance.failedPackages.append(")\n");
                                    overlays.currentInstance.hasFailed = true;
                                } else {
                                    if (overlays.compileInstance.specialSnowflake ||
                                            !overlays.compileInstance.noInstall.isEmpty()) {
                                        overlays.currentInstance.lateInstall
                                                .add(overlays.compileInstance.noInstall);
                                    } else if (needToWait) {
                                        // Thread wait
                                        Substratum.startWaitingInstall();
                                        do {
                                            try {
                                                Thread.sleep((long) SPECIAL_SNOWFLAKE_DELAY);
                                            } catch (InterruptedException ignored) {
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
                        String packageName = checked.getFullOverlayParameters();
                        if (Packages.isPackageInstalled(context, packageName)) {
                            overlays.currentInstance.finalRunner.add(packageName);
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
        final WeakReference<Overlays> ref;
        final WeakReference<Context> refContext;

        finishEnableFunction(Overlays overlays) {
            super();
            ref = new WeakReference<>(overlays);
            refContext = new WeakReference<>(overlays.context);
        }

        @Override
        protected void onPreExecute() {
            Overlays overlays = ref.get();
            if (overlays != null) {
                overlays.progressBar.setVisibility(View.VISIBLE);
                if (overlays.toggleAll.isChecked()) overlays.toggleAll.setChecked(false);
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Context context = overlays.getActivity();
                if (!overlays.currentInstance.finalRunner.isEmpty()) {
                    if (overlays.mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        List<String> allInstalledOverlays = ThemeManager.listAllOverlays
                                (context);
                        for (String installedOverlay : allInstalledOverlays) {
                            if (!Packages.getOverlayParent(context, installedOverlay)
                                    .equals(overlays.themePid)) {
                                disableBeforeEnabling.add(installedOverlay);
                            }
                        }
                        ThemeManager.disableOverlay(context, disableBeforeEnabling);
                        ThemeManager.enableOverlay(context, overlays.currentInstance.finalCommand);
                    } else {
                        ThemeManager.enableOverlay(context, overlays.currentInstance.finalCommand);
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
        final WeakReference<Overlays> ref;
        final WeakReference<Context> refContext;

        finishDisableFunction(Overlays overlays) {
            super();
            ref = new WeakReference<>(overlays);
            refContext = new WeakReference<>(overlays.context);
        }

        @Override
        protected void onPreExecute() {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Activity activity = overlays.getActivity();
                if (!overlays.currentInstance.finalRunner.isEmpty()) {
                    overlays.progressBar.setVisibility(View.VISIBLE);
                    if (overlays.toggleAll.isChecked() && activity != null)
                        activity.runOnUiThread(() -> overlays.toggleAll.setChecked(false));
                }
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Context context = overlays.getActivity();
                if (!overlays.currentInstance.finalRunner.isEmpty()) {
                    ThemeManager.disableOverlay(context, overlays.currentInstance.finalCommand);
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
        final WeakReference<Overlays> ref;
        final WeakReference<Context> refContext;

        finishEnableDisableFunction(Overlays overlays) {
            super();
            ref = new WeakReference<>(overlays);
            refContext = new WeakReference<>(overlays.context);
        }

        @Override
        protected void onPreExecute() {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Activity activity = overlays.getActivity();
                if (!overlays.currentInstance.finalRunner.isEmpty()) {
                    overlays.progressBar.setVisibility(View.VISIBLE);
                    if (overlays.toggleAll.isChecked()) {
                        if (activity != null) {
                            activity.runOnUiThread(() -> overlays.toggleAll.setChecked(false));
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
                if (!overlays.currentInstance.finalRunner.isEmpty()) {
                    ArrayList<String> enableOverlays = new ArrayList<>();
                    ArrayList<String> disableOverlays = new ArrayList<>();
                    for (int i = 0; i < overlays.currentInstance.finalCommand.size(); i++) {
                        if (!overlays.currentInstance.checkedOverlays.get(i).isOverlayEnabled()) {
                            enableOverlays.add(overlays.currentInstance.finalCommand.get(i));
                        } else {
                            disableOverlays.add(overlays.currentInstance.finalCommand.get(i));
                        }
                    }
                    ThemeManager.disableOverlay(context, disableOverlays);
                    if (overlays.mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        List<String> allInstalledOverlays = ThemeManager.listAllOverlays
                                (context);
                        for (String installedOverlay : allInstalledOverlays) {
                            if (!Packages.getOverlayParent(context,
                                    installedOverlay).equals(overlays.themePid)) {
                                disableBeforeEnabling.add(installedOverlay);
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
        final WeakReference<Overlays> ref;
        final WeakReference<Context> refContext;
        private final String state;

        finishUpdateFunction(Overlays overlays, String state) {
            super();
            ref = new WeakReference<>(overlays);
            refContext = new WeakReference<>(overlays.context);
            this.state = state;
        }

        @Override
        protected void onPreExecute() {
            Overlays overlays = ref.get();
            Context context = refContext.get();
            if ((context != null) && (overlays != null)) {
                if (overlays.compileDialog != null) overlays.compileDialog.dismiss();

                // Add dummy intent to be able to close the notification on click
                Intent notificationIntent = new Intent(context, InformationActivity.class);
                notificationIntent.putExtra(THEME_NAME, overlays.themeName);
                notificationIntent.putExtra(THEME_PID, overlays.themePid);
                notificationIntent.setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent intent =
                        PendingIntent.getActivity(context, 0, notificationIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT);

                overlays.notifyManager.cancel(References.NOTIFICATION_ID_COMPILER);
                if (!overlays.currentInstance.hasFailed) {
                    overlays.builder = new NotificationCompat.Builder(
                            context, DEFAULT_NOTIFICATION_CHANNEL_ID);
                    overlays.builder.setAutoCancel(true);
                    overlays.builder.setProgress(0, 0, false);
                    overlays.builder.setOngoing(false);
                    overlays.builder.setContentIntent(intent);
                    overlays.builder.setSmallIcon(R.drawable.notification_success_icon);
                    overlays.builder.setContentTitle(
                            context.getString(R.string.notification_done_title));
                    overlays.builder.setContentText(
                            context.getString(R.string.notification_no_errors_found));
                    if (overlays.prefs.getBoolean("vibrate_on_compiled", false)) {
                        overlays.builder.setVibrate(new long[]{100L, 200L, 100L, 500L});
                    }
                    overlays.notifyManager.notify(
                            References.NOTIFICATION_ID_COMPILER, overlays.builder.build());

                    if (overlays.currentInstance.missingType3) {
                        currentShownLunchBar = Lunchbar.make(
                                overlays.getActivityView(),
                                overlays.context.getString(R.string.toast_compiled_missing),
                                Snackbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    } else {
                        currentShownLunchBar = Lunchbar.make(
                                overlays.getActivityView(),
                                overlays.context.getString(R.string.toast_compiled_updated),
                                Snackbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    }
                }

                if (InformationActivity.compilingProcess &&
                        InformationActivity.shouldRestartActivity) {
                    // Gracefully finish, so that we won't close the activity when theme is upgraded
                    Broadcasts.sendActivityFinisherMessage(
                            overlays.context, overlays.themePid);
                }
                InformationActivity.shouldRestartActivity = false;
                InformationActivity.compilingProcess = false;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Overlays overlays = ref.get();
            Context context = refContext.get();
            if ((context != null) && (overlays != null)) {
                if (Systems.IS_OREO &&
                        isSystemSecurityPatchNewer(SECURITY_UPDATE_WARN_AFTER) &&
                        !checkSubstratumService(context)) {
                    if (!overlays.prefs.getBoolean("new_stock_dismissal", false)) {
                        AlertDialog.Builder alertDialogBuilder =
                                new AlertDialog.Builder(overlays.context);
                        alertDialogBuilder.setTitle(
                                overlays.getString(R.string.new_stock_commits_title));
                        // Start of message
                        String message = overlays.getString(R.string.new_stock_commits_text);
                        // If user is Samsung
                        if (isNewSamsungDevice()) message += "\n\n" +
                                overlays.getString(R.string.new_stock_commits_text_samsung);
                        // Explain the pink state
                        if (!isNewSamsungDevice()) message += "\n\n" +
                                overlays.getString(R.string.new_stock_commits_text_pink);
                        alertDialogBuilder.setMessage(message);
                        alertDialogBuilder.setPositiveButton(
                                R.string.dialog_ok,
                                (dialog, id12) -> {
                                    overlays.progressBar.setVisibility(View.GONE);
                                    dialog.dismiss();
                                });
                        alertDialogBuilder.setNeutralButton(
                                R.string.dialog_do_not_show_again, (dialog, id1) -> {
                                    overlays.prefs.edit().putBoolean(
                                            "new_stock_dismissal", true).apply();
                                    overlays.progressBar.setVisibility(View.GONE);
                                    dialog.dismiss();
                                });
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                }
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Overlays overlays = ref.get();
            if (overlays != null) {
                Activity activity = overlays.getActivity();
                Context context = refContext.get();

                if (!overlays.currentInstance.hasFailed ||
                        (overlays.currentInstance.finalRunner.size() >
                                overlays.currentInstance.failCount)) {
                    if (state.equals(COMPILE_ENABLE) && overlays.mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        List<String> allInstalledOverlays = ThemeManager.listAllOverlays
                                (context);
                        for (String installedOverlay : allInstalledOverlays) {
                            if (!overlays.themePid.equals(Packages.
                                    getOverlayParent(context, installedOverlay))) {
                                disableBeforeEnabling.add(installedOverlay);
                            } else {
                                for (OverlaysItem oi : overlays.currentInstance.checkedOverlays) {
                                    String targetOverlay = oi.getPackageName();
                                    if (targetOverlay.equals(
                                            Packages.getOverlayTarget(context, installedOverlay))) {
                                        disableBeforeEnabling.add(installedOverlay);
                                    }
                                }
                            }
                        }
                        ThemeManager.disableOverlay(context, disableBeforeEnabling);
                    }

                    if (state.equals(COMPILE_ENABLE)) {
                        ThemeManager.enableOverlay(context, overlays.currentInstance.finalCommand);
                    }

                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            if (!overlays.currentInstance.finalRunner.isEmpty()) {
                                overlays.progressBar.setVisibility(View.VISIBLE);
                                if (overlays.toggleAll.isChecked())
                                    activity.runOnUiThread(() ->
                                            overlays.toggleAll.setChecked(false));
                            }
                            overlays.overlaysAdapter.refreshOverlayStateList(overlays.context);
                            overlays.overlaysAdapter.notifyDataSetChanged();
                            overlays.progressBar.setVisibility(View.GONE);
                        });
                    }

                    List<String> toAnalyze = new ArrayList<>();
                    List<OverlaysItem> allOverlays = overlays.currentInstance.checkedOverlays;
                    for (OverlaysItem overlay : allOverlays) {
                        toAnalyze.add(overlay.getPackageName());
                    }
                    if (Packages.needsRecreate(context, toAnalyze)) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                overlays.overlayItemList = overlays.overlaysAdapter.getOverlayList();
                                overlays.getCurrentOverlays();
                                overlays.overlaysAdapter.refreshOverlayStateList(overlays.context);
                                for (int i = 0; i < overlays.overlayItemList.size(); i++) {
                                    OverlaysItem currentOverlay = overlays.overlayItemList.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(
                                            overlays.currentInstanceOverlays);
                                    if (activity != null) {
                                        activity.runOnUiThread(() ->
                                                overlays.overlaysAdapter.notifyDataSetChanged());
                                    }
                                }
                            } catch (Exception ignored) {
                                // Consume window refresh
                            }
                        }, (long) REFRESH_WINDOW_DELAY);
                    }
                }

                if (!overlays.currentInstance.lateInstall.isEmpty() &&
                        !Systems.isSamsungDevice(context)) {
                    // Install remaining overlays
                    HandlerThread thread = new HandlerThread("LateInstallThread",
                            Thread.MAX_PRIORITY);
                    thread.start();
                    new Handler(thread.getLooper()).post(() -> {
                        ArrayList<String> packages = new ArrayList<>();
                        for (String o : overlays.currentInstance.lateInstall) {
                            ThemeManager.installOverlay(context, o);
                            String packageName =
                                    o.substring(o.lastIndexOf('/') + 1, o.lastIndexOf('-'));
                            packages.add(packageName);
                            if (!Systems.isNewSamsungDevice() &&
                                    checkThemeInterfacer(context) ||
                                    Systems.isAndromedaDevice(context)) {
                                // Wait until the overlays to fully install so on compile enable
                                // mode it can be enabled after.
                                Substratum.startWaitingInstall();
                                do {
                                    try {
                                        Thread.sleep((long) SPECIAL_SNOWFLAKE_DELAY);
                                    } catch (InterruptedException ignored) {
                                        // Still waiting
                                    }
                                } while (Substratum.isWaitingInstall());
                            }
                        }
                        if (state.equals(COMPILE_ENABLE)) {
                            try {
                                Thread.sleep((long) (SPECIAL_SNOWFLAKE_DELAY_SS));
                                ThemeManager.enableOverlay(context, packages);
                            } catch (InterruptedException ignored) {
                                // Still waiting
                            }
                        }
                        Substratum.getInstance().unregisterFinishReceiver();
                        thread.quitSafely();
                    });
                }
            }
            return null;
        }
    }
}
