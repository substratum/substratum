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
import java.util.Set;
import java.util.stream.Collectors;

import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Resources;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.util.files.MD5;
import projekt.substratum.util.files.Root;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static projekt.substratum.common.Packages.getOverlayParent;
import static projekt.substratum.common.Packages.getOverlayTarget;
import static projekt.substratum.common.Packages.isPackageInstalled;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ROOTED;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_UNROOTED;
import static projekt.substratum.common.Systems.checkAndromeda;
import static projekt.substratum.common.Systems.checkOMS;
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

    public static boolean blacklisted(final String packageName, final Boolean unsupportedSamsung) {
        final Collection<String> blacklisted = new ArrayList<>(Arrays.asList(blacklistedPackages));
        if (unsupportedSamsung) {
            blacklisted.addAll(new ArrayList<>(Arrays.asList(Resources.ALLOWED_SETTINGS_ELEMENTS)));
            blacklisted.add("android");
            blacklisted.add("com.android.settings");
            blacklisted.add("com.android.settings.icons");
            blacklisted.add("com.android.systemui");
            blacklisted.add("com.android.systemui.headers");
            blacklisted.add("com.android.systemui.tiles");
            blacklisted.add("com.android.systemui.navbars");
            blacklisted.add("com.android.systemui.statusbars");
        }
        return blacklisted.contains(packageName);
    }

    public static void enableOverlay(final Context context, final ArrayList<String> overlays) {
        if (overlays.isEmpty()) return;
        overlays.removeAll(listOverlays(context, STATE_ENABLED));
        if (overlays.isEmpty()) return;

        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPrefs.getBoolean("auto_disable_target_overlays", false)) {
            for (final String overlay : overlays) {
                // Disable other overlays for target if preference enabled
                final String overlayTarget = getOverlayTarget(context, overlay);
                if ((overlayTarget != null) && !overlayTarget.isEmpty()) {
                    final ArrayList<String> enabledOverlaysForTarget = new ArrayList<>(
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

        final boolean hasThemeInterfacer = checkThemeInterfacer(context);
        final boolean hasAndromeda = checkAndromeda(context);

        if (hasThemeInterfacer) {
            ThemeInterfacerService.enableOverlays(
                    context, overlays, shouldRestartUI(context, overlays));
        } else if (hasAndromeda) {
            if (!AndromedaService.enableOverlays(overlays)) {
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_andromeda_timed_out),
                                Toast.LENGTH_LONG).show()
                );
            }
        } else {
            final StringBuilder commands = new StringBuilder(enableOverlay + " " + overlays.get(0));
            for (int i = 1; i < overlays.size(); i++) {
                commands.append(";" + enableOverlay + " ").append(overlays.get(i));
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
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void disableOverlay(final Context context, final ArrayList<String> overlays) {
        if (overlays.isEmpty()) return;
        overlays.removeAll(listOverlays(context, STATE_DISABLED));
        if (overlays.isEmpty()) return;

        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.disableOverlays(
                    context, overlays, shouldRestartUI(context, overlays));
        } else if (checkAndromeda(context)) {
            if (!AndromedaService.disableOverlays(overlays)) {
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_andromeda_timed_out),
                                Toast.LENGTH_LONG).show()
                );
            }
        } else {
            final StringBuilder commands = new StringBuilder(disableOverlay + " " + overlays.get(0));
            for (int i = 1; i < overlays.size(); i++) {
                commands.append(";" + disableOverlay + " ").append(overlays.get(i));
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
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setPriority(final Context context, final ArrayList<String> overlays) {
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.setPriority(
                    context, overlays, shouldRestartUI(context, overlays));
        } else if (checkAndromeda(context)) {
            if (!AndromedaService.setPriority(overlays)) {
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_andromeda_timed_out),
                                Toast.LENGTH_LONG).show()
                );
            }
        } else {
            final StringBuilder commands = new StringBuilder();
            for (int i = 0; i < (overlays.size() - 1); i++) {
                final String packageName = overlays.get(i);
                final String parentName = overlays.get(i + 1);
                commands.append((commands.length() == 0) ? "" : " && ").append(setPriority)
                        .append(" ").append(packageName).append(" ").append(parentName);
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

    public static void disableAllThemeOverlays(final Context context) {
        final List<String> list = ThemeManager.listOverlays(context, STATE_ENABLED).stream()
                .filter(o -> getOverlayParent(context, o) != null)
                .collect(Collectors.toList());
        ThemeManager.disableOverlay(context, new ArrayList<>(list));
    }

    public static void restartSystemUI(final Context context) {
        Log.d(References.SUBSTRATUM_LOG, "Restarting SystemUI");
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.restartSystemUI(context);
        } else {
            Root.runCommand("pkill -f com.android.systemui");
        }
    }

    private static void killSystemUINotificationsOnStockOreo(final Context context) {
        if (checkThemeSystemModule(context) == OVERLAY_MANAGER_SERVICE_O_ROOTED) {
            Root.runCommand("service call notification 1");
        }
    }

    public static void forceStopService(final Context context) {
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.forceStopService(context);
        }
    }

    public static List<String> listAllOverlays(final Context context) {
        return listOverlays(context, STATE_LIST_ALL_OVERLAYS, EXPORT_RETURN_DEFAULT);
    }

    public static List<String> listOverlays(final Context context, final int state) {
        return listOverlays(context, state, EXPORT_RETURN_DEFAULT);
    }

    public static List<String> listTargetWithMultipleOverlaysEnabled(final Context context) {
        return listOverlays(context, STATE_ENABLED,
                EXPORT_RETURN_MULTIPLE_TARGETS_ENABLED);
    }

    private static List<String> listOverlays(final Context context, final int state, final int state2) {
        final List<String> list = new ArrayList<>();
        try {
            // Throw certain exceptions intentionally when unsupported device found
            if (Systems.isSamsung(context)) throw new Exception();
            if (!Systems.checkOMS(context)) throw new Exception();

            // Now let's assume everything that gets through will now be only in OMS ROMs
            final Map<String, List<OverlayInfo>> allOverlays;
            // On Oreo, use interfacer to get installed overlays
            if (checkThemeSystemModule(context) == OVERLAY_MANAGER_SERVICE_O_UNROOTED) {
                allOverlays = ThemeInterfacerService.getAllOverlays(context);
            } else {
                // noinspection deprecation
                allOverlays = OverlayManagerService.getAllOverlays();
            }
            if (allOverlays != null) {
                final Set<String> set = allOverlays.keySet();
                switch (state2) {
                    case EXPORT_RETURN_ALL_OVERLAYS:
                        for (final Map.Entry<String, List<OverlayInfo>> stringListEntry : allOverlays.entrySet()) {
                            for (final OverlayInfo oi : stringListEntry.getValue()) {
                                if (oi.isApproved()) {
                                    list.add(oi.packageName);
                                }
                            }
                        }
                        break;
                    case EXPORT_RETURN_MULTIPLE_TARGETS_ENABLED:
                        for (final Map.Entry<String, List<OverlayInfo>> stringListEntry : allOverlays.entrySet()) {
                            final List<OverlayInfo> targetOverlays = stringListEntry.getValue();
                            final int targetOverlaysSize = targetOverlays.size();
                            int count = 0;
                            for (final OverlayInfo oi : targetOverlays) {
                                if (oi.isEnabled()) count++;
                            }
                            if ((targetOverlaysSize > 1) && (count > 1)) list.add((String) stringListEntry.getKey());
                        }
                        break;
                    case EXPORT_RETURN_DEFAULT:
                        for (final Map.Entry<String, List<OverlayInfo>> stringListEntry : allOverlays.entrySet()) {
                            for (final OverlayInfo oi : stringListEntry.getValue()) {
                                if ((state == STATE_ENABLED) && oi.isEnabled()) {
                                    list.add(oi.packageName);
                                } else if ((state == STATE_DISABLED) && !oi.isEnabled()) {
                                    list.add(oi.packageName);
                                } else if (state == STATE_MISSING_TARGET) {
                                    if (oi.state == STATE_MISSING_TARGET) {
                                        list.add(oi.packageName);
                                    }
                                } else if (state == STATE_LIST_ALL_OVERLAYS) {
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
        } catch (final Exception e) {
            // At this point, we probably ran into a legacy command or stock OMS
            if (Systems.checkOMS(context) || Systems.checkOreo()) {
                final String prefix;
                if (state == STATE_ENABLED) {
                    prefix = "[x]";
                } else if (state == STATE_DISABLED) {
                    prefix = "[ ]";
                } else {
                    prefix = "---";
                }

                String[] arrList = null;

                // This is a check for Oreo and Andromeda's integration
                if (Systems.checkAndromeda(context)) {
                    final File overlays = new File(
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/.andromeda/overlays.xml");

                    // Call Andromeda to output the file!
                    final SharedPreferences prefs =
                            PreferenceManager.getDefaultSharedPreferences(context);
                    if (!overlays.exists()) {
                        Log.d("ThemeManager", "Fetching new file from Andromeda, please wait!");
                        if (!AndromedaService.listOverlays()) {
                            final Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(() ->
                                    Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_andromeda_timed_out),
                                            Toast.LENGTH_LONG).show()
                            );
                        }

                        // Now we wait till the file is made quickly!
                        int counter = 0;
                        while (!overlays.exists() && (counter <= 20)) {
                            try {
                                Thread.sleep(100L);
                                counter++;
                                Log.d("ThemeManager",
                                        "Substratum is still waiting for a response " +
                                                "from Andromeda...");
                            } catch (final InterruptedException e1) {
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
                                final String str = new String(
                                        Files.readAllBytes(Paths.get(overlays.getAbsolutePath())));
                                arrList = str.split(System.getProperty("line.separator"));
                            }
                        } catch (final IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                } else {
                    try {
                        arrList = Root.runCommand(listAllOverlays)
                                .split(System.getProperty("line.separator"));
                    } catch (final NullPointerException ignored) {
                    }
                }
                switch (state2) {
                    case EXPORT_RETURN_MULTIPLE_TARGETS_ENABLED:
                        if (arrList != null) {
                            String currentApp = "";
                            int counter = 0;
                            for (final String line : arrList) {
                                if (line.startsWith(prefix)) {
                                    if ((getOverlayParent(context, line.substring(4)) != null) &&
                                            isPackageInstalled(context, line.substring(4))) {
                                        counter++;
                                    }
                                } else if (!line.startsWith("[")) {
                                    if (counter > 1) list.add(currentApp);
                                    counter = 0;
                                    currentApp = line;
                                }
                            }
                            if (counter > 1) list.add(currentApp);
                        } else if (checkAndromeda(context)) {
                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                Toast.makeText(context,
                                        context.getString(R.string.toast_andromeda_timed_out),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                        break;
                    case EXPORT_RETURN_ALL_OVERLAYS:
                    case EXPORT_RETURN_DEFAULT:
                        if (arrList != null) {
                            final String enabledPrefix = "[x]";
                            final String disabledPrefix = "[ ]";
                            for (final String line : arrList) {
                                final boolean checker;
                                switch (state) {
                                    case STATE_LIST_ALL_OVERLAYS:
                                        checker = line.startsWith(enabledPrefix) ||
                                                line.startsWith(disabledPrefix);
                                        break;
                                    default:
                                        checker = line.startsWith(prefix);
                                }
                                if (checker) {
                                    if ((getOverlayParent(context, line.substring(4)) != null) &&
                                            isPackageInstalled(context, line.substring(4))) {
                                        try {
                                            final String packageName = line.substring(4);
                                            final String sourceDir = context.getPackageManager()
                                                    .getApplicationInfo(packageName, 0).sourceDir;
                                            if (!sourceDir.startsWith("/vendor/overlay/")) {
                                                list.add(packageName);
                                            }
                                        } catch (final Exception e2) {
                                            // Suppress warning
                                        }
                                    }
                                }
                            }
                        } else if (checkAndromeda(context)) {
                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                Toast.makeText(context,
                                        context.getString(R.string.toast_andromeda_timed_out),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                        break;
                }
            } else {
                // We now know this is not OMS, so fallback for Samsung and Legacy
                if ((state == STATE_LIST_ALL_OVERLAYS) || (state == STATE_ENABLED)) {
                    if (Systems.isSamsung(context)) {
                        final PackageManager pm = context.getPackageManager();
                        final List<ApplicationInfo> packages =
                                pm.getInstalledApplications(PackageManager.GET_META_DATA);
                        list.clear();
                        for (final ApplicationInfo packageInfo : packages) {
                            if (Packages.getOverlayMetadata(
                                    context,
                                    packageInfo.packageName,
                                    References.metadataOverlayParent) != null) {
                                list.add(packageInfo.packageName);
                            }
                        }
                    } else {
                        final File legacyCheck = new File(LEGACY_NEXUS_DIR);
                        if (legacyCheck.exists() && legacyCheck.isDirectory()) {
                            list.clear();
                            final String[] lister = legacyCheck.list();
                            for (final String aLister : lister) {
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

    public static boolean isOverlay(final Context context, final String target) {
        final List<String> overlays = listAllOverlays(context);
        for (int i = 0; i < overlays.size(); i++) {
            if (overlays.get(i).equals(target)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> listOverlaysByTheme(final Context context, final String target) {
        final List<String> list = new ArrayList<>();
        final List<String> overlays = listAllOverlays(context);
        for (int i = 0; i < overlays.size(); i++) {
            if (getOverlayParent(context, overlays.get(i)).equals(target)) {
                list.add(overlays.get(i));
            }
        }
        return list;
    }

    public static List<String> listOverlaysForTarget(final Context context, final String target) {
        final List<String> list = new ArrayList<>();
        final List<String> overlays = listAllOverlays(context);
        list.addAll(overlays.stream().filter(o -> o.startsWith(target))
                .collect(Collectors.toList()));
        return list;
    }

    public static List<String> listEnabledOverlaysForTarget(final Context context, final String target) {
        final List<String> list = new ArrayList<>();
        final List<String> overlays = listOverlays(context, STATE_ENABLED);
        list.addAll(overlays.stream().filter(o -> o.startsWith(target))
                .collect(Collectors.toList()));
        return list;
    }

    public static List<String> listDisabledOverlaysForTarget(final Context context, final String target) {
        final List<String> list = new ArrayList<>();
        final List<String> overlays = listOverlays(context, STATE_DISABLED);
        list.addAll(overlays.stream().filter(o -> o.startsWith(target))
                .collect(Collectors.toList()));
        return list;
    }

    public static boolean isOverlayEnabled(final Context context, final String overlayName) {
        final List<String> enabledOverlays = ThemeManager.listOverlays(context, STATE_ENABLED);
        for (final String o : enabledOverlays) {
            if (o.equals(overlayName)) return true;
        }
        return false;
    }

    /*
        Begin interaction with the ThemeInterfacerService or the PackageManager binaries.

        These methods will handle all possible commands to be sent to PackageManager when handling
        an overlay, such as installing and uninstalling APKs directly on the device.
     */

    public static void installOverlay(final Context context, final String overlay) {
        if (checkThemeInterfacer(context)) {
            final ArrayList<String> list = new ArrayList<>();
            list.add(overlay);
            ThemeInterfacerService.installOverlays(context, list);
        } else if (checkAndromeda(context)) {
            final List<String> list = new ArrayList<>();
            list.add(overlay);
            if (!AndromedaService.installOverlays(list)) {
                final Handler handler = new Handler(Looper.getMainLooper());
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

    public static void uninstallOverlay(final Context context,
                                        final ArrayList<String> overlays) {

        if (Systems.checkOMS(context)) {
            final ArrayList<String> temp = new ArrayList<>(overlays);
            temp.removeAll(listOverlays(context, STATE_DISABLED));
            disableOverlay(context, temp);
        }

        // if enabled list is not contains any overlays
        if (checkThemeInterfacer(context) && !Systems.isSamsung(context)) {
            ThemeInterfacerService.uninstallOverlays(
                    context,
                    overlays,
                    false);
        } else if (checkAndromeda(context) && !Systems.isSamsung(context)) {
            if (!AndromedaService.uninstallOverlays(overlays)) {
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_andromeda_timed_out),
                                Toast.LENGTH_LONG).show()
                );
            }
        } else if (Systems.isSamsung(context) &&
                !Root.checkRootAccess() &&
                !Root.requestRootAccess()) {
            for (int i = 0; i < overlays.size(); i++) {
                final Uri packageURI = Uri.parse("package:" + overlays.get(i));
                final Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                context.startActivity(uninstallIntent);
            }
        } else {
            final StringBuilder command = new StringBuilder();
            for (final String packageName : overlays) {
                command.append((command.length() == 0) ? "" : " && ")
                        .append("pm uninstall ")
                        .append(packageName);
            }
            ElevatedCommands.runThreadedCommand(command.toString());
        }
    }

    public static boolean shouldRestartUI(final Context context, final String overlay) {
        if (overlay.startsWith("com.android.systemui")) {
            return checkOMS(context);
        }
        return false;
    }

    public static boolean shouldRestartUI(final Context context, final Iterable<String> overlays) {
        for (final String o : overlays) {
            if (shouldRestartUI(context, o)) {
                return true;
            }
        }
        return false;
    }

    private static boolean optInFromUIRestart(final Context context) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean("opt_in_sysui_restart", true);
    }
}