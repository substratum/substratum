/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.common.platform;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.om.OM;
import android.content.om.OverlayInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.RestrictTo;
import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Resources;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.commands.SamsungOverlayCacher;
import projekt.substratum.util.helpers.Root;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static projekt.substratum.common.Packages.getOverlayParent;
import static projekt.substratum.common.Packages.getOverlayTarget;
import static projekt.substratum.common.Packages.isPackageInstalled;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_SAMSUNG_OVERLAY_CACHE;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.Resources.FRAMEWORK;
import static projekt.substratum.common.Resources.PIXEL_OVERLAY_PACKAGES;
import static projekt.substratum.common.Resources.SETTINGS;
import static projekt.substratum.common.Resources.SETTINGS_ICONS;
import static projekt.substratum.common.Resources.SYSTEMUI;
import static projekt.substratum.common.Resources.SYSTEMUI_HEADERS;
import static projekt.substratum.common.Resources.SYSTEMUI_NAVBARS;
import static projekt.substratum.common.Resources.SYSTEMUI_QSTILES;
import static projekt.substratum.common.Resources.SYSTEMUI_STATUSBARS;
import static projekt.substratum.common.Systems.checkAndromeda;
import static projekt.substratum.common.Systems.checkOMS;
import static projekt.substratum.common.Systems.checkSubstratumService;
import static projekt.substratum.common.Systems.checkThemeInterfacer;
import static projekt.substratum.common.Systems.isNewSamsungDevice;
import static projekt.substratum.common.Systems.isNewSamsungDeviceAndromeda;

public class ThemeManager {

    // State values of OverlayInfo
    public static final int STATE_MISSING_TARGET = (SDK_INT >= O) ? 0 : 1;
    public static final int STATE_DISABLED = (SDK_INT >= O) ? 2 : 4;
    public static final int STATE_ENABLED = (SDK_INT >= O) ? 3 : 5;
    /**
     * Begin interaction with the OverlayManagerService binaries.
     * <p>
     * These methods will concurrently list all the possible functions that is open to Substratum
     * for usage with the OMS7 and OMS7-R systems.
     * <p>
     * NOTE: Deprecation at the OMS3 level. We no longer support OMS3 commands.
     */
    private static final String disableOverlay = "cmd overlay disable";
    private static final String enableOverlay = "cmd overlay enable";
    private static final String listAllOverlays = "cmd overlay list";
    private static final String setPriority = "cmd overlay set-priority";
    private static final String[] blacklistedPackages = {
            INTERFACER_PACKAGE,
    };
    // Non-Interfacer (NI) values
    private static final Integer NI_restartSystemUIDelay = 2000;
    private static final int EXPORT_RETURN_ALL_OVERLAYS = 0;
    private static final int EXPORT_RETURN_MULTIPLE_TARGETS_ENABLED = 1;
    private static final int EXPORT_RETURN_DEFAULT = 2;
    private static final int STATE_LIST_ALL_OVERLAYS = 13579;

    /**
     * Blacklisted applications to hide on specific devices, for example Samsung devices
     *
     * @param packageName        Package name to compare
     * @param unsupportedSamsung Whether or not the flag to show dangerous overlays is turned on
     * @return True, if the specified packageName is blacklisted
     */
    public static boolean blacklisted(String packageName, Boolean unsupportedSamsung) {
        Collection<String> blacklisted = new ArrayList<>(Arrays.asList(blacklistedPackages));
        if (unsupportedSamsung) {
            blacklisted.addAll(new ArrayList<>(Arrays.asList(Resources.ALLOWED_SETTINGS_ELEMENTS)));
            blacklisted.add(FRAMEWORK);
            blacklisted.add(SETTINGS);
            blacklisted.add(SETTINGS_ICONS);
            blacklisted.add(SYSTEMUI);
            blacklisted.add(SYSTEMUI_HEADERS);
            blacklisted.add(SYSTEMUI_QSTILES);
            blacklisted.add(SYSTEMUI_NAVBARS);
            blacklisted.add(SYSTEMUI_STATUSBARS);
        }
        return blacklisted.contains(packageName);
    }

    /**
     * Enable a list of overlays
     *
     * @param context  Context
     * @param overlays List of overlays
     */
    public static void enableOverlay(Context context,
                                     ArrayList<String> overlays) {
        if (overlays.isEmpty()) return;
        overlays.removeAll(listOverlays(context, STATE_ENABLED));
        if (overlays.isEmpty()) return;
        overlays.removeAll(Arrays.asList(PIXEL_OVERLAY_PACKAGES));

        SharedPreferences sharedPrefs = Substratum.getPreferences();

        if (sharedPrefs.getBoolean("auto_disable_target_overlays", false)) {
            for (String overlay : overlays) {
                // Disable other overlays for target if preference enabled
                String overlayTarget = getOverlayTarget(context, overlay);
                if ((overlayTarget != null) && !overlayTarget.isEmpty()) {
                    ArrayList<String> enabledOverlaysForTarget = new ArrayList<>(
                            listEnabledOverlaysForTarget(context,
                                    overlayTarget));
                    if (Systems.checkOMS(context)) {
                        disableOverlay(context, enabledOverlaysForTarget);
                    } else {
                        uninstallOverlay(context, enabledOverlaysForTarget);
                    }
                }
            }
        }

        boolean hasSS = checkSubstratumService(context);
        boolean hasThemeInterfacer = checkThemeInterfacer(context);
        boolean hasAndromeda = checkAndromeda(context);

        if (hasSS) {
            SubstratumService.switchOverlay(overlays, true, shouldRestartUI(context, overlays));
        } else if (hasThemeInterfacer) {
            ThemeInterfacerService.enableOverlays(
                    overlays, shouldRestartUI(context, overlays));
        } else if (hasAndromeda && !isNewSamsungDeviceAndromeda(context)) {
            if (!AndromedaService.enableOverlays(overlays)) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_andromeda_timed_out),
                                Toast.LENGTH_LONG).show()
                );
            }
        } else {
            StringBuilder commands = new StringBuilder(enableOverlay + ' ' + overlays.get(0));
            for (int i = 1; i < overlays.size(); i++) {
                commands.append(';' + enableOverlay + ' ').append(overlays.get(i));
            }
            ElevatedCommands.runThreadedCommand(commands.toString());
            try {
                Thread.sleep(NI_restartSystemUIDelay);
                if (shouldRestartUI(context, overlays)) {
                    restartSystemUI(context);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Disable a list of overlays
     *
     * @param context  Context
     * @param overlays List of overlays
     */
    public static void disableOverlay(Context context,
                                      ArrayList<String> overlays) {
        if (overlays.isEmpty()) return;
        overlays.removeAll(listOverlays(context, STATE_DISABLED));
        if (overlays.isEmpty()) return;
        overlays.removeAll(Arrays.asList(PIXEL_OVERLAY_PACKAGES));

        if (checkSubstratumService(context)) {
            SubstratumService.switchOverlay(overlays, false, shouldRestartUI(context, overlays));
        } else if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.disableOverlays(
                    overlays, shouldRestartUI(context, overlays));
        } else if (checkAndromeda(context) && !isNewSamsungDeviceAndromeda(context)) {
            if (!AndromedaService.disableOverlays(overlays)) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_andromeda_timed_out),
                                Toast.LENGTH_LONG).show()
                );
            }
        } else {
            StringBuilder commands = new StringBuilder(disableOverlay + ' ' + overlays.get
                    (0));
            for (int i = 1; i < overlays.size(); i++) {
                commands.append(';' + disableOverlay + ' ').append(overlays.get(i));
            }
            ElevatedCommands.runThreadedCommand(commands.toString());
            try {
                Thread.sleep(NI_restartSystemUIDelay);
                if (shouldRestartUI(context, overlays)) {
                    restartSystemUI(context);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Set priority on a list of overlays
     *
     * @param context  Context
     * @param overlays List of overlays
     */
    public static void setPriority(Context context, ArrayList<String> overlays) {
        if (checkSubstratumService(context)) {
            SubstratumService.setPriority(overlays, shouldRestartUI(context, overlays));
        } else if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.setPriority(
                    overlays, shouldRestartUI(context, overlays));
        } else if (checkAndromeda(context) && !isNewSamsungDeviceAndromeda(context)) {
            if (!AndromedaService.setPriority(overlays)) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_andromeda_timed_out),
                                Toast.LENGTH_LONG).show()
                );
            }
        } else {
            StringBuilder commands = new StringBuilder();
            for (int i = 0; i < (overlays.size() - 1); i++) {
                String packageName = overlays.get(i);
                String parentName = overlays.get(i + 1);
                commands.append((commands.length() == 0) ? "" : " && ").append(setPriority)
                        .append(' ').append(packageName).append(' ').append(parentName);
            }
            ElevatedCommands.runThreadedCommand(commands.toString());
            if (shouldRestartUI(context, overlays)) {
                restartSystemUI(context);
            }
        }
    }

    /**
     * Disable all theme overlays
     *
     * @param context Context
     */
    public static void disableAllThemeOverlays(Context context) {
        List<String> list = ThemeManager.listOverlays(context, STATE_ENABLED).stream()
                .filter(o -> getOverlayParent(context, o) != null)
                .collect(Collectors.toList());
        list.removeAll(Arrays.asList(PIXEL_OVERLAY_PACKAGES));
        ThemeManager.disableOverlay(context, new ArrayList<>(list));
    }

    /**
     * Restart SystemUI
     *
     * @param context Context
     */
    public static void restartSystemUI(Context context) {
        Substratum.log(References.SUBSTRATUM_LOG, "Restarting SystemUI");
        if (checkSubstratumService(context)) {
            SubstratumService.restartSystemUi();
        } else if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.restartSystemUI();
        } else {
            Root.runCommand("pkill -f com.android.systemui");
        }
    }

    /**
     * List all overlays on the device
     *
     * @param context Context
     * @return Returns a list of overlays
     */
    public static List<String> listAllOverlays(Context context) {
        return listOverlays(context, STATE_LIST_ALL_OVERLAYS, EXPORT_RETURN_DEFAULT);
    }

    /**
     * List all overlays on the device with states
     *
     * @param context Context
     * @param state   Specified state of which the overlays are in
     * @return Returns a list of overlays
     */
    public static List<String> listOverlays(Context context, int state) {
        return listOverlays(context, state, EXPORT_RETURN_DEFAULT);
    }

    /**
     * List targets with multiple overlays
     *
     * @param context Context
     * @return Returns a list of target packages with multiple overlays enabled
     */
    public static List<String> listTargetWithMultipleOverlaysEnabled(Context context) {
        return listOverlays(context, STATE_ENABLED,
                EXPORT_RETURN_MULTIPLE_TARGETS_ENABLED);
    }

    /**
     * List overlays, beef of {@link #listOverlays(Context, int),
     * {@link #listAllOverlays(Context)} and
     * {@link #listTargetWithMultipleOverlaysEnabled(Context)}}
     *
     * @param context        Context
     * @param overlayState   Overlay state
     * @param secondaryState Secondary state to have a more refined filter
     * @return Returns a list of overlays
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @SuppressWarnings("unchecked")
    private static List<String> listOverlays(Context context,
                                             int overlayState,
                                             int secondaryState) {
        Set<String> list = new HashSet<>();
        try {
            // Throw certain exceptions intentionally when unsupported device found
            boolean substratumService = checkSubstratumService(context);
            boolean themeInterfacer = checkThemeInterfacer(context);
            if (!substratumService && !themeInterfacer) {
                throw new Exception();
            }
            // Now let's assume everything that gets through will now be only in OMS ROMs
            Map<String, List<OverlayInfo>> allOverlays = null;
            if (substratumService) {
                // For direct calls with the Substratum service
                allOverlays = SubstratumService.getAllOverlays();
            } else if (themeInterfacer) {
                // The ol' way
                try {
                    allOverlays = OM.get().getAllOverlays(Process.myUid() / 100000);
                } catch (Exception e) {
                    // Ummmmmmmmmm
                }
            }
            if (allOverlays != null) {
                switch (secondaryState) {
                    case EXPORT_RETURN_ALL_OVERLAYS:
                        for (Map.Entry<String, List<OverlayInfo>> stringListEntry :
                                allOverlays.entrySet()) {
                            for (OverlayInfo oi : stringListEntry.getValue()) {
                                if (oi.isEnabled()) {
                                    list.add(oi.packageName);
                                }
                            }
                        }
                        break;
                    case EXPORT_RETURN_MULTIPLE_TARGETS_ENABLED:
                        for (Map.Entry<String, List<OverlayInfo>>
                                stringListEntry : allOverlays.entrySet()) {
                            List<OverlayInfo> targetOverlays = stringListEntry.getValue();
                            int targetOverlaysSize = targetOverlays.size();
                            int count = 0;
                            for (OverlayInfo oi : targetOverlays) {
                                if (oi.isEnabled()) count++;
                            }
                            if ((targetOverlaysSize > 1) && (count > 1))
                                list.add(stringListEntry.getKey());
                        }
                        break;
                    case EXPORT_RETURN_DEFAULT:
                        for (Map.Entry<String, List<OverlayInfo>>
                                stringListEntry : allOverlays.entrySet()) {
                            for (OverlayInfo oi : stringListEntry.getValue()) {
                                if ((overlayState == STATE_ENABLED) && oi.isEnabled()) {
                                    list.add(oi.packageName);
                                } else if ((overlayState == STATE_DISABLED) && !oi.isEnabled()) {
                                    list.add(oi.packageName);
                                } else if (overlayState == STATE_MISSING_TARGET) {
                                    if (oi.state == STATE_MISSING_TARGET) {
                                        list.add(oi.packageName);
                                    }
                                } else if (overlayState == STATE_LIST_ALL_OVERLAYS) {
                                    list.add(oi.packageName);
                                }
                            }
                        }
                        break;
                }
            } else {
                Log.e("ThemeManager",
                        "Could not queue all overlays from the Overlay Manager Service...");
            }
        } catch (Exception e) {
            // At this point, we probably ran into a legacy command or stock OMS
            if (!isNewSamsungDeviceAndromeda(context) &&
                    (Systems.checkOMS(context) || Systems.IS_OREO) &&
                    !MainActivity.instanceBasedAndromedaFailure) {
                String prefix;
                if (overlayState == STATE_ENABLED) {
                    prefix = "[x]";
                } else if (overlayState == STATE_DISABLED) {
                    prefix = "[ ]";
                } else {
                    prefix = "---";
                }

                String[] arrList = null;

                // This is a check for Oreo and Andromeda's integration
                if (Systems.checkAndromeda(context) && !MainActivity.instanceBasedAndromedaFailure) {
                    File overlays = new File(
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/.andromeda/overlays.xml");

                    // Call Andromeda to output the file!
                    Substratum.log("ThemeManager", "Fetching new file from Andromeda, please wait!");
                    AndromedaService.listOverlays();
                    int counter = 0;
                    while (!overlays.exists() && (counter <= 200)) {
                        try {
                            Thread.sleep(10L);
                            counter++;
                            Substratum.log("ThemeManager",
                                    "Substratum is still waiting for a response " +
                                            "from Andromeda...");
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }

                    // Andromeda's file is done!
                    if (overlays.exists()) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                String str = new String(
                                        Files.readAllBytes(Paths.get(overlays.getAbsolutePath())));
                                arrList = str.split(System.getProperty("line.separator"));
                            }
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                } else {
                    // It's not Andromeda, so it needs to run as root
                    try {
                        arrList = Root.runCommand(listAllOverlays).split(
                                System.getProperty("line.separator"));
                    } catch (NullPointerException npe) {
                        npe.printStackTrace();
                    }
                }
                switch (secondaryState) {
                    case EXPORT_RETURN_MULTIPLE_TARGETS_ENABLED:
                        if (arrList != null) {
                            String currentApp = "";
                            int counter = 0;
                            for (String line : arrList) {
                                if (line.startsWith(prefix)) {
                                    if ((getOverlayParent(context, line.substring(4)) != null) &&
                                            isPackageInstalled(context, line.substring(4))) {
                                        counter++;
                                    }
                                } else if (!line.startsWith("[") && !line.startsWith("-")) {
                                    if (counter > 1) list.add(currentApp);
                                    counter = 0;
                                    currentApp = line;
                                }
                            }
                            if (counter > 1) list.add(currentApp);
                        }
                        break;
                    case EXPORT_RETURN_ALL_OVERLAYS:
                    case EXPORT_RETURN_DEFAULT:
                        if (arrList != null) {
                            String enabledPrefix = "[x]";
                            String disabledPrefix = "[ ]";
                            for (String line : arrList) {
                                boolean checker;
                                switch (overlayState) {
                                    case STATE_LIST_ALL_OVERLAYS:
                                        checker = line.startsWith(enabledPrefix) ||
                                                line.startsWith(disabledPrefix);
                                        break;
                                    default:
                                        checker = line.startsWith(prefix);
                                }
                                if (checker &&
                                        getOverlayParent(context, line.substring(4)) != null &&
                                        isPackageInstalled(context, line.substring(4))) {
                                    try {
                                        String packageName = line.substring(4);
                                        String sourceDir = context.getPackageManager()
                                                .getApplicationInfo(packageName, 0).sourceDir;
                                        if (!sourceDir.startsWith("/vendor/overlay/")) {
                                            list.add(packageName);
                                        }
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }
                        break;
                }
            } else {
                // We now know this is not OMS, so fallback for Samsung and Legacy or
                // offline Andromeda
                if ((overlayState == STATE_LIST_ALL_OVERLAYS) || (overlayState == STATE_ENABLED)) {
                    if (isNewSamsungDeviceAndromeda(context) ||
                            Systems.isSamsungDevice(context) ||
                            MainActivity.instanceBasedAndromedaFailure) {
                        PackageManager pm = context.getPackageManager();
                        List<ApplicationInfo> packages = pm.getInstalledApplications(0);
                        list.clear();

                        if ((isNewSamsungDevice() || isNewSamsungDeviceAndromeda(context)) &&
                                new File(EXTERNAL_STORAGE_SAMSUNG_OVERLAY_CACHE).exists()) {
                            SamsungOverlayCacher samsungOverlayCacher =
                                    new SamsungOverlayCacher(context);
                            list.addAll(samsungOverlayCacher.getOverlays(false));
                        } else {
                            for (ApplicationInfo packageInfo : packages) {
                                if (Packages.getOverlayMetadata(
                                        context,
                                        packageInfo.packageName,
                                        References.metadataOverlayParent) != null) {
                                    list.add(packageInfo.packageName);
                                }
                            }
                        }
                    } else {
                        File legacyCheck = new File(LEGACY_NEXUS_DIR);
                        if (legacyCheck.exists() && legacyCheck.isDirectory()) {
                            list.clear();
                            String[] lister = legacyCheck.list();
                            for (String aLister : lister) {
                                if (aLister.endsWith(".apk")) {
                                    list.add(aLister.substring(0, aLister.length() - 4));
                                }
                            }
                        }
                    }
                } else {
                    list.clear();
                }
            }
        }
        list.removeAll(Arrays.asList(PIXEL_OVERLAY_PACKAGES));
        return new ArrayList<>(list);
    }

    /**
     * Check whether a specified package is an overlay
     *
     * @param context     Context
     * @param packageName Package to be determined
     * @return True, if packageName is an overlay
     */
    public static boolean isOverlay(Context context,
                                    String packageName) {
        List<String> overlays = listAllOverlays(context);
        for (String overlay : overlays) {
            if (overlay.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * List overlays by theme
     *
     * @param context     Context
     * @param packageName Package to be determined
     * @return Returns a list of overlays activated for packageName
     */
    public static List<String> listOverlaysByTheme(Context context,
                                                   String packageName) {
        List<String> list = new ArrayList<>();
        List<String> overlays = listAllOverlays(context);
        for (String overlay : overlays) {
            if (getOverlayParent(context, overlay).equals(packageName)) {
                list.add(overlay);
            }
        }
        return list;
    }

    /**
     * List overlays for target
     *
     * @param context Context
     * @param target  Package to be determined
     * @return Return a list of overlays for target
     */
    public static List<String> listOverlaysForTarget(Context context,
                                                     String target) {
        List<String> overlays = listAllOverlays(context);
        return overlays.stream().filter(o -> o.startsWith(target)).collect(Collectors.toList());
    }

    /**
     * List enabled overlays for target
     *
     * @param context Context
     * @param target  Package to be determined
     * @return Return a list of overlays for target
     */
    public static List<String> listEnabledOverlaysForTarget(Context context,
                                                            String target) {
        List<String> overlays = listOverlays(context, STATE_ENABLED);
        return overlays.stream().filter(o -> o.startsWith(target)).collect(Collectors.toList());
    }

    /**
     * List disabled overlays for target
     *
     * @param context Context
     * @param target  Package to be determined
     * @return Return a list of overlays for target
     */
    public static List<String> listDisabledOverlaysForTarget(Context context,
                                                             String target) {
        List<String> overlays = listOverlays(context, STATE_DISABLED);
        return overlays.stream().filter(o -> o.startsWith(target)).collect(Collectors.toList());
    }

    /**
     * Check if overlay is enabled
     *
     * @param context     Context
     * @param overlayName Overlay package to be determined
     * @return True, if overlay is enabled
     */
    public static boolean isOverlayEnabled(Context context, String overlayName) {
        List<String> enabledOverlays = ThemeManager.listOverlays(context, STATE_ENABLED);
        for (String o : enabledOverlays) {
            if (o.equals(overlayName)) return true;
        }
        return false;
    }

    /*
      Begin interaction with the ThemeInterfacerService or the PackageManager binaries.

      These methods will handle all possible commands to be sent to PackageManager when handling
      an overlay, such as installing and uninstalling APKs directly on the device.
     */

    /**
     * Install an overlay
     *
     * @param context Context
     * @param overlay Overlay to be installed
     */
    public static void installOverlay(Context context,
                                      String overlay) {
        if (checkSubstratumService(context)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(overlay);
            SubstratumService.installOverlay(list);
        } else if (checkThemeInterfacer(context)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(overlay);
            ThemeInterfacerService.installOverlays(list);
        } else if (checkAndromeda(context) && !isNewSamsungDeviceAndromeda(context)) {
            List<String> list = new ArrayList<>();
            list.add(overlay);
            if (!AndromedaService.installOverlays(list)) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_andromeda_timed_out),
                                Toast.LENGTH_LONG).show()
                );
            }
        } else {
            ElevatedCommands.runThreadedCommand("pm install -r " + overlay);
        }
    }

    /**
     * Uninstall a list of overlays
     *
     * @param context  Context
     * @param overlays List of overlays
     */
    public static void uninstallOverlay(Context context,
                                        ArrayList<String> overlays) {

        boolean shouldRestartUi = false;
        if (Systems.checkOMS(context) || checkSubstratumService(context)) {
            ArrayList<String> temp = new ArrayList<>(overlays);
            temp.removeAll(listOverlays(context, STATE_DISABLED));
            disableOverlay(context, temp);
            shouldRestartUi = shouldRestartUI(context, temp);
        }

        if (Systems.IS_PIE && !checkSubstratumService(context)) {
            FileOperations.mountRW();
            for (String overlay : overlays) {
                FileOperations.bruteforceDelete(References.getPieDir() + '_' + overlay + ".apk");
            }
            FileOperations.mountRO();
            return;
        }

        // if enabled list is not contains any overlays
        if (checkSubstratumService(context)) {
            SubstratumService.uninstallOverlay(overlays, shouldRestartUi);
        } else if (checkThemeInterfacer(context) && !Systems.isSamsungDevice(context)) {
            ThemeInterfacerService.uninstallOverlays(
                    overlays
            );
        } else if ((checkAndromeda(context) &&
                !MainActivity.instanceBasedAndromedaFailure) &&
                !Systems.isSamsungDevice(context)) {
            if (!AndromedaService.uninstallOverlays(overlays)) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_andromeda_timed_out),
                                Toast.LENGTH_LONG).show()
                );
            }
        } else if (MainActivity.instanceBasedAndromedaFailure ||
                (Systems.isNewSamsungDeviceAndromeda(context)) ||
                (Systems.isSamsungDevice(context) &&
                        !Root.checkRootAccess() &&
                        !Root.requestRootAccess())) {
            for (String overlay : overlays) {
                Uri packageURI = Uri.parse("package:" + overlay);
                Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
                context.startActivity(uninstallIntent);
            }
        } else {
            StringBuilder command = new StringBuilder();
            for (String packageName : overlays) {
                command.append((command.length() == 0) ? "" : " && ")
                        .append("pm uninstall ")
                        .append(packageName);
            }
            ElevatedCommands.runThreadedCommand(command.toString());
        }
    }

    /**
     * Determine whether to restart SystemUI or not
     *
     * @param context Context
     * @param overlay Overlay to be determined
     * @return True, if should restart SystemUI
     */
    public static boolean shouldRestartUI(Context context, String overlay) {
        if (checkSubstratumService(context)) return false;
        if (overlay.startsWith(SYSTEMUI)) {
            return checkOMS(context);
        }
        return false;
    }

    /**
     * Determine whether to restart SystemUI or not
     *
     * @param context  Context
     * @param overlays Overlays to be determined
     * @return True, if should restart SystemUI
     */
    public static boolean shouldRestartUI(Context context,
                                          Iterable<String> overlays) {
        if (checkSubstratumService(context)) return false;
        for (String o : overlays) {
            if (shouldRestartUI(context, o)) {
                return true;
            }
        }
        return false;
    }
}