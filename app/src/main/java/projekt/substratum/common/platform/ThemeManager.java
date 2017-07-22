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
import android.content.om.OverlayInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import projekt.substratum.common.References;
import projekt.substratum.common.Resources;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.util.files.Root;

import static android.content.om.OverlayInfo.STATE_APPROVED_DISABLED;
import static android.content.om.OverlayInfo.STATE_APPROVED_ENABLED;
import static android.content.om.OverlayInfo.STATE_NOT_APPROVED_DANGEROUS_OVERLAY;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.checkOMS;
import static projekt.substratum.common.References.checkThemeInterfacer;

public class ThemeManager {

    /**
     * Begin interaction with the OverlayManagerService binaries.
     * <p>
     * These methods will concurrently list all the possible functions that is open to Substratum
     * for usage with the OMS7 and OMS7-R systems.
     * <p>
     * NOTE: Deprecation at the OMS3 level. We no longer support OMS3 commands.
     */
    public static final String disableOverlay = "cmd overlay disable";
    public static final String enableOverlay = "cmd overlay enable";
    private static final String listAllOverlays = "cmd overlay list";
    private static final String disableAllOverlays = "cmd overlay disable-all";
    private static final String setPriority = "cmd overlay set-priority";
    private static final String[] blacklistedPackages = new String[]{
            INTERFACER_PACKAGE,
    };
    // Non-Interfacer (NI) values
    private static final Integer NI_restartSystemUIDelay = 2000;

    public static boolean blacklisted(String packageName, Boolean unsupportedSamsung) {
        List<String> blacklisted = new ArrayList<>(Arrays.asList(blacklistedPackages));
        if (unsupportedSamsung) {
            blacklisted.addAll(new ArrayList<>(Arrays.asList(Resources.ALLOWED_SETTINGS_ELEMENTS)));
            blacklisted.add("android");
            blacklisted.add("com.android.settings");
            blacklisted.add("com.android.settings.icons");
            blacklisted.add("com.android.systemui");
            blacklisted.add("com.android.systemui.headers");
            blacklisted.add("com.android.systemui.tiles");
        }
        return blacklisted.contains(packageName);
    }

    public static void enableOverlay(Context context, ArrayList<String> overlays) {
        if (overlays.isEmpty()) return;
        overlays.removeAll(listOverlays(context, STATE_APPROVED_ENABLED));
        if (overlays.isEmpty()) return;

        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.enableOverlays(context, overlays, shouldRestartUI(context,
                    overlays));
        } else {
            StringBuilder commands = new StringBuilder(enableOverlay + " " + overlays.get(0));
            for (int i = 1; i < overlays.size(); i++) {
                commands.append(";" + enableOverlay + " ").append(overlays.get(i));
            }
            new ElevatedCommands.ThreadRunner().execute(commands.toString());
            try {
                Thread.sleep(NI_restartSystemUIDelay);
                if (shouldRestartUI(context, overlays)) restartSystemUI(context);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void disableOverlay(Context context, ArrayList<String> overlays) {
        if (overlays.isEmpty()) return;
        overlays.removeAll(listOverlays(context, STATE_APPROVED_DISABLED));
        if (overlays.isEmpty()) return;

        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.disableOverlays(context, overlays, shouldRestartUI(context,
                    overlays));
        } else {
            StringBuilder commands = new StringBuilder(disableOverlay + " " + overlays.get(0));
            for (int i = 1; i < overlays.size(); i++) {
                commands.append(";" + disableOverlay + " ").append(overlays.get(i));
            }
            new ElevatedCommands.ThreadRunner().execute(commands.toString());
            try {
                Thread.sleep(NI_restartSystemUIDelay);
                if (shouldRestartUI(context, overlays)) restartSystemUI(context);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setPriority(Context context, ArrayList<String> overlays) {
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.setPriority(
                    context, overlays, shouldRestartUI(context, overlays));
        } else {
            StringBuilder commands = new StringBuilder();
            for (int i = 0; i < overlays.size() - 1; i++) {
                String packageName = overlays.get(i);
                String parentName = overlays.get(i + 1);
                commands.append((commands.length() == 0) ? "" : " && ").append(setPriority)
                        .append(" ").append(packageName).append(" ").append(parentName);
            }
            new ElevatedCommands.ThreadRunner().execute(commands.toString());
            if (shouldRestartUI(context, overlays)) restartSystemUI(context);
        }
    }

    public static void disableAllThemeOverlays(Context context) {
        List<String> list = ThemeManager.listOverlays(context, STATE_APPROVED_ENABLED).stream()
                .filter(o -> References.grabOverlayParent(context, o) != null)
                .collect(Collectors.toList());
        ThemeManager.disableOverlay(context, new ArrayList<>(list));
    }

    public static void restartSystemUI(Context context) {
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.restartSystemUI(context);
        } else {
            Root.runCommand("pkill -f com.android.systemui");
        }
    }

    public static void forceStopService(Context context) {
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.forceStopService(context);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> listAllOverlays(Context context) {
        List<String> list = new ArrayList<>();
        try {
            if (References.isSamsung(context)) throw new Exception();
            Map<String, List<OverlayInfo>> allOverlays = OverlayManagerService.getAllOverlays();
            if (allOverlays != null) {
                Set<String> set = allOverlays.keySet();
                for (String targetPackageName : set) {
                    for (OverlayInfo oi : allOverlays.get(targetPackageName)) {
                        if (oi.isApproved()) {
                            list.add(oi.packageName);
                        }
                    }
                }
            }
        } catch (Exception | NoSuchMethodError e) {
            // At this point, we probably ran into a legacy command or stock OMS
            if (References.checkOMS(context) && !References.isSamsung(context)) {
                String enabledPrefix = "[x]";
                String disabledPrefix = "[ ]";
                String[] arrList = Root.runCommand(listAllOverlays)
                        .split(System.getProperty("line.separator"));

                for (String line : arrList) {
                    if (line.startsWith(enabledPrefix) || line.startsWith(disabledPrefix)) {
                        String packageName = line.substring(4);
                        if (References.isPackageInstalled(context, packageName)) {
                            try {
                                String sourceDir = context.getPackageManager()
                                        .getApplicationInfo(packageName, 0).sourceDir;
                                if (!sourceDir.startsWith("/vendor/overlay/")) {
                                    list.add(packageName);
                                }
                            } catch (Exception ee) {
                                // Package not found blabla
                            }
                        }
                    }
                }
            } else {
                if (References.isSamsung(context)) {
                    final PackageManager pm = context.getPackageManager();
                    List<ApplicationInfo> packages =
                            pm.getInstalledApplications(PackageManager.GET_META_DATA);
                    list.clear();
                    for (ApplicationInfo packageInfo : packages) {
                        if (References.getOverlayMetadata(
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
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public static List<String> listOverlays(Context context, int state) {
        List<String> list = new ArrayList<>();
        try {
            if (References.isSamsung(context)) throw new Exception();
            Map<String, List<OverlayInfo>> allOverlays = OverlayManagerService.getAllOverlays();
            if (allOverlays != null) {
                Set<String> set = allOverlays.keySet();
                for (String targetPackageName : set) {
                    for (OverlayInfo oi : allOverlays.get(targetPackageName)) {
                        if (state == STATE_APPROVED_ENABLED && oi.isEnabled()) {
                            list.add(oi.packageName);
                        } else if (state == STATE_APPROVED_DISABLED && !oi.isEnabled()) {
                            list.add(oi.packageName);
                        } else if (state <= STATE_NOT_APPROVED_DANGEROUS_OVERLAY &&
                                !oi.isApproved()) {
                            list.add(oi.packageName);
                        }
                    }
                }
            }
        } catch (Exception | NoSuchMethodError e) {
            // At this point, we probably ran into a legacy command or stock OMS
            if (References.checkOMS(context) && !References.isSamsung(context)) {
                String prefix;
                switch (state) {
                    case STATE_APPROVED_ENABLED:
                        prefix = "[x]";
                        break;
                    case STATE_APPROVED_DISABLED:
                        prefix = "[ ]";
                        break;
                    default:
                        prefix = "---";
                        break;
                }

                String[] arrList = Root.runCommand(listAllOverlays)
                        .split(System.getProperty("line.separator"));
                for (String line : arrList) {
                    if (line.startsWith(prefix)) {
                        String packageName = line.substring(4);
                        if (References.isPackageInstalled(context, packageName)) {
                            try {
                                String sourceDir = context.getPackageManager()
                                        .getApplicationInfo(packageName, 0).sourceDir;
                                if (!sourceDir.startsWith("/vendor/overlay/")) {
                                    list.add(packageName);
                                }
                            } catch (Exception ee) {
                                // Package not found blabla
                            }
                        }
                    }
                }
            } else {
                switch (state) {
                    case STATE_APPROVED_ENABLED:
                        if (References.isSamsung(context)) {
                            final PackageManager pm = context.getPackageManager();
                            List<ApplicationInfo> packages =
                                    pm.getInstalledApplications(PackageManager.GET_META_DATA);
                            list.clear();
                            for (ApplicationInfo packageInfo : packages) {
                                if (References.getOverlayMetadata(
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
                        break;
                    default:
                        list.clear();
                        break;
                }
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public static List<String> listTargetWithMultipleOverlaysEnabled(Context context) {
        List<String> list = new ArrayList<>();
        try {
            Map<String, List<OverlayInfo>> allOverlays = OverlayManagerService.getAllOverlays();
            if (allOverlays != null) {
                Set<String> set = allOverlays.keySet();
                for (String targetPackageName : set) {
                    List<OverlayInfo> targetOverlays = allOverlays.get(targetPackageName);
                    int targetOverlaysSize = targetOverlays.size();
                    int count = 0;

                    for (OverlayInfo oi : targetOverlays) {
                        if (oi.isEnabled()) {
                            count++;
                        }
                    }
                    if (targetOverlaysSize > 1 && count > 1) {
                        list.add(targetPackageName);
                    }
                }
            }
        } catch (Exception | NoSuchMethodError e) {
            // Stock OMS goes here
            String prefix = "[x]";
            String[] arrList = Root.runCommand(listAllOverlays)
                    .split(System.getProperty("line.separator"));
            int counter = 0;
            String currentApp = "";
            for (String line : arrList) {
                if (line.startsWith(prefix)) {
                    if (References.isPackageInstalled(context, line.substring(4))) {
                        counter++;
                    }
                } else if (!line.startsWith("---")) {
                    if (counter > 1) {
                        list.add(currentApp);
                    }
                    counter = 0;
                    currentApp = line;
                }
            }
        }
        return list;
    }

    public static boolean isOverlay(Context context, String target) {
        List<String> overlays = listAllOverlays(context);
        for (int i = 0; i < overlays.size(); i++) {
            if (overlays.get(i).equals(target)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> listOverlaysByTheme(Context context, String target) {
        List<String> list = new ArrayList<>();
        List<String> overlays = listAllOverlays(context);
        for (int i = 0; i < overlays.size(); i++) {
            if (References.grabOverlayParent(context, overlays.get(i)).equals(target)) {
                list.add(overlays.get(i));
            }
        }
        return list;
    }

    public static List<String> listOverlaysForTarget(Context context, String target) {
        List<String> list = new ArrayList<>();
        List<String> overlays = listAllOverlays(context);
        list.addAll(overlays.stream().filter(o -> o.startsWith(target))
                .collect(Collectors.toList()));
        return list;
    }

    public static List<String> listEnabledOverlaysForTarget(Context context, String target) {
        List<String> list = new ArrayList<>();
        List<String> overlays = listOverlays(context, STATE_APPROVED_ENABLED);
        list.addAll(overlays.stream().filter(o -> o.startsWith(target))
                .collect(Collectors.toList()));
        return list;
    }

    public static boolean isOverlayEnabled(Context context, String overlayName) {
        List<String> enabledOverlays = ThemeManager.listOverlays(context, STATE_APPROVED_ENABLED);
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

    public static void installOverlay(Context context, String overlay) {
        if (checkThemeInterfacer(context)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(overlay);
            ThemeInterfacerService.installOverlays(context, list);
        } else {
            new ElevatedCommands.ThreadRunner().execute("pm install -r " + overlay);
        }
    }

    public static void installOverlay(Context context, ArrayList<String> overlays) {
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.installOverlays(context, overlays);
        } else {
            StringBuilder packages = new StringBuilder();
            for (String o : overlays) {
                packages.append(o).append(" ");
            }
            new ElevatedCommands.ThreadRunner().execute("pm install -r " + packages);
        }
    }

    public static void uninstallOverlay(Context context,
                                        ArrayList<String> overlays) {
        ArrayList<String> temp = new ArrayList<>(overlays);
        temp.removeAll(listOverlays(context, STATE_APPROVED_DISABLED));
        boolean shouldRestartUi = shouldRestartUI(context, temp);
        disableOverlay(context, temp);

        // if enabled list is not contains any overlays
        if (checkThemeInterfacer(context) && !References.isSamsung(context)) {
            ThemeInterfacerService.uninstallOverlays(
                    context,
                    overlays,
                    shouldRestartUi);
        } else if (References.isSamsung(context)) {
            for (int i = 0; i < overlays.size(); i++) {
                Uri packageURI = Uri.parse("package:" + overlays.get(i));
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                context.startActivity(uninstallIntent);
            }
        } else {
            StringBuilder command = new StringBuilder();
            for (String packageName : overlays) {
                command.append((command.length() == 0) ? "" : " && ").append("pm uninstall ")
                        .append(packageName);
            }
            new ElevatedCommands.ThreadRunner().execute(command.toString());
            if (checkOMS(context) && shouldRestartUi)
                restartSystemUI(context);
        }
    }

    public static boolean shouldRestartUI(Context context, ArrayList<String> overlays) {
        if (checkOMS(context)) {
            for (String o : overlays) {
                if (o.startsWith("com.android.systemui"))
                    return true;
            }
        }
        return false;
    }
}