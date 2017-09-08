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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.Resources;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.util.files.MD5;
import projekt.substratum.util.files.Root;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.checkAndromeda;
import static projekt.substratum.common.References.checkOMS;
import static projekt.substratum.common.References.checkThemeInterfacer;
import static projekt.substratum.common.References.grabOverlayParent;
import static projekt.substratum.common.References.isPackageInstalled;

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
    // State values of OverlayInfo
    public static final int STATE_MISSING_TARGET = SDK_INT >= O ? 0 : 1;
    public static final int STATE_DISABLED = SDK_INT >= O ? 2 : 4;
    public static final int STATE_ENABLED = SDK_INT >= O ? 3 : 5;
    private static final String enableOverlay = "cmd overlay enable";
    private static final String listAllOverlays = "cmd overlay list";
    private static final String setPriority = "cmd overlay set-priority";
    private static final String[] blacklistedPackages = new String[]{
            INTERFACER_PACKAGE,
    };
    // Non-Interfacer (NI) values
    private static final Integer NI_restartSystemUIDelay = 2000;
    private static final int EXPORT_RETURN_ALL_OVERLAYS = 0;
    private static final int EXPORT_RETURN_MULTIPLE_TARGETS_ENABLED = 1;
    private static final int EXPORT_RETURN_DEFAULT = 2;
    private static final int STATE_LIST_ALL_OVERLAYS = 13579;

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
            blacklisted.add("com.android.systemui.navbars");
            blacklisted.add("com.android.systemui.statusbars");
        }
        return blacklisted.contains(packageName);
    }

    public static void enableOverlay(Context context, ArrayList<String> overlays) {
        if (overlays.isEmpty()) return;
        overlays.removeAll(listOverlays(context, STATE_ENABLED));
        if (overlays.isEmpty()) return;

        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.enableOverlays(
                    context, overlays, shouldRestartUI(context, overlays));
        } else if (checkAndromeda(context)) {
            if (!AndromedaService.enableOverlays(overlays)) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_andromeda_timed_out),
                                Toast.LENGTH_LONG).show()
                );
                References.notifyAndromedaDisconnected(context);
            }
        } else {
            StringBuilder commands = new StringBuilder(enableOverlay + " " + overlays.get(0));
            for (int i = 1; i < overlays.size(); i++) {
                commands.append(";" + enableOverlay + " ").append(overlays.get(i));
            }
            new ElevatedCommands.ThreadRunner().execute(commands.toString());
            try {
                Thread.sleep(NI_restartSystemUIDelay);
                if (shouldRestartUI(context, overlays)) {
                    if (optInFromUIRestart(context)) {
                        restartSystemUI(context);
                    } else {
                        killSystemUINotificationsOnStockOreo();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void disableOverlay(Context context, ArrayList<String> overlays) {
        if (overlays.isEmpty()) return;
        overlays.removeAll(listOverlays(context, STATE_DISABLED));
        if (overlays.isEmpty()) return;

        if (checkThemeInterfacer(context)) {
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
                References.notifyAndromedaDisconnected(context);
            }
        } else {
            StringBuilder commands = new StringBuilder(disableOverlay + " " + overlays.get(0));
            for (int i = 1; i < overlays.size(); i++) {
                commands.append(";" + disableOverlay + " ").append(overlays.get(i));
            }
            new ElevatedCommands.ThreadRunner().execute(commands.toString());
            try {
                Thread.sleep(NI_restartSystemUIDelay);
                if (shouldRestartUI(context, overlays)) {
                    if (optInFromUIRestart(context)) {
                        restartSystemUI(context);
                    } else {
                        killSystemUINotificationsOnStockOreo();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setPriority(Context context, ArrayList<String> overlays) {
        if (checkThemeInterfacer(context)) {
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
                References.notifyAndromedaDisconnected(context);
            }
        } else {
            StringBuilder commands = new StringBuilder();
            for (int i = 0; i < overlays.size() - 1; i++) {
                String packageName = overlays.get(i);
                String parentName = overlays.get(i + 1);
                commands.append((commands.length() == 0) ? "" : " && ").append(setPriority)
                        .append(" ").append(packageName).append(" ").append(parentName);
            }
            new ElevatedCommands.ThreadRunner().execute(commands.toString());
            if (shouldRestartUI(context, overlays)) {
                if (optInFromUIRestart(context)) {
                    restartSystemUI(context);
                } else {
                    killSystemUINotificationsOnStockOreo();
                }
            }
        }
    }

    public static void disableAllThemeOverlays(Context context) {
        List<String> list = ThemeManager.listOverlays(context, STATE_ENABLED).stream()
                .filter(o -> grabOverlayParent(context, o) != null)
                .collect(Collectors.toList());
        ThemeManager.disableOverlay(context, new ArrayList<>(list));
    }

    public static void restartSystemUI(Context context) {
        Log.d(References.SUBSTRATUM_LOG, "Restarting SystemUI");
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.restartSystemUI(context);
        } else {
            Root.runCommand("pkill -f com.android.systemui");
        }
    }

    private static void killSystemUINotificationsOnStockOreo() {
        if (Root.checkRootAccess() && References.checkOreo()) {
            Root.runCommand("service call notification 1");
        }
    }

    public static void forceStopService(Context context) {
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.forceStopService(context);
        }
    }

    public static List<String> listAllOverlays(Context context) {
        return listOverlays(context, STATE_LIST_ALL_OVERLAYS, EXPORT_RETURN_DEFAULT);
    }

    public static List<String> listOverlays(Context context, int state) {
        return listOverlays(context, state, EXPORT_RETURN_DEFAULT);
    }

    public static List<String> listTargetWithMultipleOverlaysEnabled(Context context) {
        return listOverlays(context, STATE_ENABLED,
                EXPORT_RETURN_MULTIPLE_TARGETS_ENABLED);
    }

    public static List<String> listOverlays(Context context, int state, int state2) {
        List<String> list = new ArrayList<>();
        try {
            // Throw certain exceptions intentionally when unsupported device found
            if (References.isSamsung(context)) throw new Exception();
            if (!References.checkOMS(context)) throw new Exception();

            // Now let's assume everything that gets through will now be only in OMS ROMs
            @SuppressWarnings({"unchecked", "deprecation"})
            Map<String, List<OverlayInfo>> allOverlays = OverlayManagerService.getAllOverlays();
            if (allOverlays != null) {
                Set<String> set = allOverlays.keySet();
                switch (state2) {
                    case EXPORT_RETURN_ALL_OVERLAYS:
                        for (String targetPackageName : set) {
                            for (OverlayInfo oi : allOverlays.get(targetPackageName)) {
                                if (oi.isApproved()) {
                                    list.add(oi.packageName);
                                }
                            }
                        }
                        break;
                    case EXPORT_RETURN_MULTIPLE_TARGETS_ENABLED:
                        for (String targetPackageName : set) {
                            List<OverlayInfo> targetOverlays = allOverlays.get(targetPackageName);
                            int targetOverlaysSize = targetOverlays.size();
                            int count = 0;
                            for (OverlayInfo oi : targetOverlays) {
                                if (oi.isEnabled()) count++;
                            }
                            if (targetOverlaysSize > 1 && count > 1) list.add(targetPackageName);
                        }
                        break;
                    case EXPORT_RETURN_DEFAULT:
                        for (String targetPackageName : set) {
                            for (OverlayInfo oi : allOverlays.get(targetPackageName)) {
                                if (state == STATE_ENABLED && oi.isEnabled()) {
                                    list.add(oi.packageName);
                                } else if (state == STATE_DISABLED && !oi.isEnabled()) {
                                    list.add(oi.packageName);
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
        } catch (Exception e) {
            // At this point, we probably ran into a legacy command or stock OMS
            if (References.checkOMS(context) || References.checkOreo()) {
                String prefix;
                if (state == STATE_ENABLED) {
                    prefix = "[x]";
                } else if (state == STATE_DISABLED) {
                    prefix = "[ ]";
                } else {
                    prefix = "---";
                }

                String[] arrList = null;

                // This is a check for Oreo and Andromeda's integration
                if (References.checkAndromeda(context)) {
                    File overlays = new File(
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/.andromeda/overlays.xml");

                    // Call Andromeda to output the file!
                    SharedPreferences prefs =
                            PreferenceManager.getDefaultSharedPreferences(context);
                    if (!overlays.exists()) {
                        Log.d("ThemeManager", "Fetching new file from Andromeda, please wait!");
                        if (!AndromedaService.listOverlays()) {
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(() ->
                                    Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_andromeda_timed_out),
                                            Toast.LENGTH_LONG).show()
                            );
                            References.notifyAndromedaDisconnected(context);
                        }

                        // Now we wait till the file is made quickly!
                        int counter = 0;
                        while (!overlays.exists() && counter <= 20) {
                            try {
                                Thread.sleep(100);
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
                    try {
                        arrList = Root.runCommand(listAllOverlays)
                                .split(System.getProperty("line.separator"));
                    } catch (NullPointerException ignored) {
                    }
                }
                switch (state2) {
                    case EXPORT_RETURN_MULTIPLE_TARGETS_ENABLED:
                        int counter = 0;
                        String currentApp = "";
                        if (arrList != null) {
                            for (String line : arrList) {
                                if (line.startsWith(prefix)) {
                                    if (grabOverlayParent(context, line.substring(4)) != null &&
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
                            References.notifyAndromedaDisconnected(context);
                        }
                        break;
                    case EXPORT_RETURN_ALL_OVERLAYS:
                    case EXPORT_RETURN_DEFAULT:
                        if (arrList != null) {
                            String enabledPrefix = "[x]";
                            String disabledPrefix = "[ ]";
                            for (String line : arrList) {
                                boolean checker;
                                switch (state) {
                                    case STATE_LIST_ALL_OVERLAYS:
                                        checker = line.startsWith(enabledPrefix) ||
                                                line.startsWith(disabledPrefix);
                                        break;
                                    default:
                                        checker = line.startsWith(prefix);
                                }
                                if (checker) {
                                    String packageName = line.substring(4);
                                    if (grabOverlayParent(context, line.substring(4)) != null &&
                                            isPackageInstalled(context, line.substring(4))) {
                                        try {
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
                        } else if (checkAndromeda(context)) {
                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                Toast.makeText(context,
                                        context.getString(R.string.toast_andromeda_timed_out),
                                        Toast.LENGTH_LONG).show();
                            }
                            References.notifyAndromedaDisconnected(context);
                        }
                        break;
                }
            } else {
                // We now know this is not OMS, so fallback for Samsung and Legacy
                if (state == STATE_LIST_ALL_OVERLAYS || state == STATE_ENABLED) {
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
                } else {
                    list.clear();
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
            if (grabOverlayParent(context, overlays.get(i)).equals(target)) {
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
        List<String> overlays = listOverlays(context, STATE_ENABLED);
        list.addAll(overlays.stream().filter(o -> o.startsWith(target))
                .collect(Collectors.toList()));
        return list;
    }

    public static List<String> listDisabledOverlaysForTarget(Context context, String target) {
        List<String> list = new ArrayList<>();
        List<String> overlays = listOverlays(context, STATE_DISABLED);
        list.addAll(overlays.stream().filter(o -> o.startsWith(target))
                .collect(Collectors.toList()));
        return list;
    }

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

    public static void installOverlay(Context context, String overlay) {
        if (checkThemeInterfacer(context)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(overlay);
            ThemeInterfacerService.installOverlays(context, list);
        } else if (checkAndromeda(context)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(overlay);
            if (!AndromedaService.installOverlays(list)) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_andromeda_timed_out),
                                Toast.LENGTH_LONG).show()
                );
                References.notifyAndromedaDisconnected(context);
            }
        } else {
            new ElevatedCommands.ThreadRunner().execute("pm install -r " + overlay);
        }
    }

    public static void installOverlay(Context context, ArrayList<String> overlays) {
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.installOverlays(context, overlays);
        } else if (checkAndromeda(context)) {
            if (!AndromedaService.installOverlays(overlays)) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_andromeda_timed_out),
                                Toast.LENGTH_LONG).show()
                );
                References.notifyAndromedaDisconnected(context);
            }
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
        temp.removeAll(listOverlays(context, STATE_DISABLED));
        disableOverlay(context, temp);

        // if enabled list is not contains any overlays
        if (checkThemeInterfacer(context) && !References.isSamsung(context)) {
            ThemeInterfacerService.uninstallOverlays(
                    context,
                    overlays,
                    false);
        } else if (checkAndromeda(context) && !References.isSamsung(context)) {
            if (!AndromedaService.uninstallOverlays(overlays)) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() ->
                        Toast.makeText(
                                context,
                                context.getString(R.string.toast_andromeda_timed_out),
                                Toast.LENGTH_LONG).show()
                );
                References.notifyAndromedaDisconnected(context);
            }
        } else if (References.isSamsung(context) &&
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
            new ElevatedCommands.ThreadRunner().execute(command.toString());
        }
    }

    public static boolean shouldRestartUI(Context context, String overlay) {
        if (overlay.startsWith("com.android.systemui")) {
            return checkOMS(context);
        }
        return false;
    }

    public static boolean shouldRestartUI(Context context, ArrayList<String> overlays) {
        for (String o : overlays) {
            if (shouldRestartUI(context, o)) {
                return true;
            }
        }
        return false;
    }

    private static boolean optInFromUIRestart(Context context) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean("opt_in_sysui_restart", true);
    }
}
