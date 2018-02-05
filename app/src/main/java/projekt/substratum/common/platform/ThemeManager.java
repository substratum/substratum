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

package projekt.substratum.common.platform;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.om.OverlayInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.RestrictTo;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Resources;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.util.helpers.MD5;
import projekt.substratum.util.helpers.Root;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static projekt.substratum.common.Packages.getOverlayParent;
import static projekt.substratum.common.Packages.getOverlayTarget;
import static projekt.substratum.common.Packages.isPackageInstalled;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ROOTED;
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
import static projekt.substratum.common.Systems.checkThemeSystemModule;

public enum ThemeManager {
    ;

    /**
     * Begin interaction with the OverlayManagerService binaries.
     * <p>
     * These methods will concurrently list all the possible functions that is open to Substratum
     * for usage with the OMS7 and OMS7-R systems.
     * <p>
     * NOTE: Deprecation at the OMS3 level. We no longer support OMS3 commands.
     */
    public static final String disableOverlay = "cmd overlay disable";
    // State values of OverlayInfo
    public static final int STATE_MISSING_TARGET = (SDK_INT >= O) ? 0 : 1;
    public static final int STATE_DISABLED = (SDK_INT >= O) ? 2 : 4;
    public static final int STATE_ENABLED = (SDK_INT >= O) ? 3 : 5;
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

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

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
                    context, overlays, shouldRestartUI(context, overlays));
        } else if (hasAndromeda) {
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
                    if (optInFromUIRestart(context)) {
                        restartSystemUI(context);
                    } else {
                        killSystemUINotificationsOnStockOreo(context);
                    }
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
                    context, overlays, shouldRestartUI(context, overlays));
        } else if (checkAndromeda(context)) {
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
                    if (optInFromUIRestart(context)) {
                        restartSystemUI(context);
                    } else {
                        killSystemUINotificationsOnStockOreo(context);
                    }
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
                    context, overlays, shouldRestartUI(context, overlays));
        } else if (checkAndromeda(context)) {
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
                if (optInFromUIRestart(context)) {
                    restartSystemUI(context);
                } else {
                    killSystemUINotificationsOnStockOreo(context);
                }
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
        Log.d(References.SUBSTRATUM_LOG, "Restarting SystemUI");
        if (checkSubstratumService(context)) {
            SubstratumService.restartSystemUi();
        } else if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.restartSystemUI(context);
        } else {
            Root.runCommand("pkill -f com.android.systemui");
        }
    }

    /**
     * Kill all notifications on stock Oreo with root, unfortunately this doesn't work on persistent
     * application's notifications such as USB Charging, so unplugging would refresh it.
     * <p>
     * This is only used if you don't want to restart SystemUI and have a more graceful ending
     *
     * @param context Context
     */
    private static void killSystemUINotificationsOnStockOreo(Context context) {
        if (checkThemeSystemModule(context) == OVERLAY_MANAGER_SERVICE_O_ROOTED) {
            Root.runCommand("service call notification 1");
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
        List<String> list = new ArrayList<>();
        try {
            // Throw certain exceptions intentionally when unsupported device found
            boolean substratumService = checkSubstratumService(context);
            boolean themeInterfacer = checkThemeInterfacer(context);
            if (!substratumService && !themeInterfacer) {
                throw new Exception();
            }
            // Now let's assume everything that gets through will now be only in OMS ROMs
            Map<String, List<OverlayInfo>> allOverlays = null;
            if (checkSubstratumService(context)) {
                // For direct calls with the Substratum service
                allOverlays = OverlayManagerService.getAllOverlays();
            } else if (checkThemeInterfacer(context)) {
                // For Theme Interfacer calls
                allOverlays = ThemeInterfacerService.getAllOverlays(context);
            }
            if (allOverlays != null) {
                switch (secondaryState) {
                    case EXPORT_RETURN_ALL_OVERLAYS:
                        for (Map.Entry<String, List<OverlayInfo>> stringListEntry :
                                allOverlays.entrySet()) {
                            for (OverlayInfo oi : stringListEntry.getValue()) {
                                if (oi.isApproved()) {
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
            if (Systems.checkOMS(context) || Systems.checkOreo()) {
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
                if (Systems.checkAndromeda(context)) {
                    File overlays = new File(
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/.andromeda/overlays.xml");

                    // Call Andromeda to output the file!
                    SharedPreferences prefs =
                            PreferenceManager.getDefaultSharedPreferences(context);
                    if (!overlays.exists()) {
                        Log.d("ThemeManager", "Fetching new file from Andromeda, please wait!");
                        AndromedaService.listOverlays();
                        int counter = 0;
                        while (!overlays.exists() && (counter <= 20)) {
                            try {
                                Thread.sleep(100L);
                                counter++;
                                Log.d("ThemeManager",
                                        "Substratum is still waiting for a response " +
                                                "from Andromeda...");
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        }
                        if (overlays.exists()) {
                            prefs.edit().putString("andromeda_overlays",
                                    MD5.calculateMD5(overlays)).apply();
                        }
                    } else {
                        Log.d("ThemeManager", "Queuing list using cached file!");
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
                                    } catch (Exception e2) {
                                        // Suppress warning
                                    }
                                }
                            }
                        }
                        break;
                }
            } else {
                // We now know this is not OMS, so fallback for Samsung and Legacy
                if ((overlayState == STATE_LIST_ALL_OVERLAYS) || (overlayState == STATE_ENABLED)) {
                    if (Systems.isSamsungDevice(context)) {
                        PackageManager pm = context.getPackageManager();
                        List<ApplicationInfo> packages =
                                pm.getInstalledApplications(PackageManager.GET_META_DATA);
                        list.clear();
                        for (ApplicationInfo packageInfo : packages) {
                            if (Packages.getOverlayMetadata(
                                    context,
                                    packageInfo.packageName,
                                    References.metadataOverlayParent) != null) {
                                list.add(packageInfo.packageName);
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
        return list;
    }

    /**
     * Check whether a specified package is an overlay
     *
     * @param context      Context
     * @param package_name Package to be determined
     * @return True, if package_name is an overlay
     */
    public static boolean isOverlay(Context context,
                                    String package_name) {
        List<String> overlays = listAllOverlays(context);
        for (int i = 0; i < overlays.size(); i++) {
            if (overlays.get(i).equals(package_name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * List overlays by theme
     *
     * @param context      Context
     * @param package_name Package to be determined
     * @return Returns a list of overlays activated for package_name
     */
    public static List<String> listOverlaysByTheme(Context context,
                                                   String package_name) {
        List<String> list = new ArrayList<>();
        List<String> overlays = listAllOverlays(context);
        for (int i = 0; i < overlays.size(); i++) {
            if (getOverlayParent(context, overlays.get(i)).equals(package_name)) {
                list.add(overlays.get(i));
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
            ThemeInterfacerService.installOverlays(context, list);
        } else if (checkAndromeda(context)) {
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

        // if enabled list is not contains any overlays
        if (checkSubstratumService(context)) {
            SubstratumService.uninstallOverlay(overlays, shouldRestartUi);
        } else if (checkThemeInterfacer(context) && !Systems.isSamsungDevice(context)) {
            ThemeInterfacerService.uninstallOverlays(
                    context,
                    overlays,
                    false);
        } else if (checkAndromeda(context) && !Systems.isSamsungDevice(context)) {
            if (!AndromedaService.uninstallOverlays(overlays)) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_andromeda_timed_out),
                                Toast.LENGTH_LONG).show()
                );
            }
        } else if (Systems.isSamsungDevice(context) &&
                !Root.checkRootAccess() &&
                !Root.requestRootAccess()) {
            for (int i = 0; i < overlays.size(); i++) {
                Uri packageURI = Uri.parse("package:" + overlays.get(i));
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
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
        for (String o : overlays) {
            if (shouldRestartUI(context, o)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Allow the user to control whether they would get SystemUI restarts of not
     *
     * @param context Context
     * @return True, if users want to restart SystemUI
     */
    private static boolean optInFromUIRestart(Context context) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean("opt_in_sysui_restart", true);
    }
}