/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.common;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.om.IOverlayManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import projekt.substratum.BuildConfig;
import projekt.substratum.Substratum;
import projekt.substratum.common.platform.SubstratumService;
import projekt.substratum.util.helpers.FileDownloader;
import projekt.substratum.util.helpers.Root;
import projekt.substratum.util.readers.ReadSupportedROMsFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static projekt.substratum.common.References.ANDROMEDA_PACKAGE;
import static projekt.substratum.common.References.BYPASS_SYSTEM_VERSION_CHECK;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.References.NO_THEME_ENGINE;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_N_UNROOTED;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ROOTED;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_UNROOTED;
import static projekt.substratum.common.References.PLAY_STORE_PACKAGE_NAME;
import static projekt.substratum.common.References.RUNTIME_RESOURCE_OVERLAY_N_ROOTED;
import static projekt.substratum.common.References.SAMSUNG_THEME_ENGINE_N;
import static projekt.substratum.common.References.SST_ADDON_PACKAGE;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.References.SUBSTRATUM_THEME;
import static projekt.substratum.common.References.hashPassthrough;
import static projekt.substratum.common.References.isNetworkAvailable;
import static projekt.substratum.common.References.isServiceRunning;

public class Systems {

    public static final boolean IS_PIE = Build.VERSION.SDK_INT == Build.VERSION_CODES.P;
    public static final boolean IS_OREO = Build.VERSION.SDK_INT == Build.VERSION_CODES.O ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1;
    public static final boolean IS_NOUGAT = Build.VERSION.SDK_INT == Build.VERSION_CODES.N ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1;

    private static Boolean checkPackageSupported;
    private static SharedPreferences prefs = Substratum.getPreferences();
    /**
     * Cached boolean to find if system
     * passes all cases of Samsung checks
     */
    private static Boolean isSamsungDevice = null;

    /**
     * This method is used to determine whether there the system is initiated with OMS
     *
     * @param context Self explanatory, sir
     */
    public static Boolean checkOMS(Context context) {
        if (context == null) return true; // Safe to assume that window refreshes only on OMS
        if (!BYPASS_SYSTEM_VERSION_CHECK) {
            if (!prefs.contains("oms_state")) {
                setAndCheckOMS(context);
            }
            return prefs.getBoolean("oms_state", false);
        } else {
            return false;
        }
    }

    /**
     * Checks the backend system (SysServ or Interfacer) whether it is authorized to be used
     *
     * @param context Context!
     * @return True, if the current theming system is authorized
     */
    public static Boolean authorizedToUseBackend(Context context) {
        return !BuildConfig.DEBUG || getForceAuthorizationSetting(context).equals("1");
    }

    /**
     * Obtain the system setting's authorization setting for Interfacer/SysServ
     *
     * @param context Context!
     * @return A string object of the obtained setting
     */
    private static String getForceAuthorizationSetting(Context context) {
        String forceAuthorize = Settings.Secure.getString(context.getContentResolver(),
                "force_authorize_substratum_packages");
        return forceAuthorize == null ? "0" : forceAuthorize;
    }

    /**
     * Check what theme system is being used on the device
     *
     * @param context Self explanatory...
     * @return Returns the theme mode
     */
    public static int checkThemeSystemModule(Context context) {
        return checkThemeSystemModule(context, false);
    }

    /**
     * Brain matter of {@link #checkThemeSystemModule(Context)}
     *
     * @param context     Self explantory, sur..,,
     * @param firstLaunch Whether it is the first start of the app
     * @return Returns the theme mode
     */
    public static int checkThemeSystemModule(Context context,
                                             boolean firstLaunch) {
        if (context != null) {
            if (!firstLaunch &&
                    prefs.contains("current_theme_mode") &&
                    prefs.getInt("current_theme_mode", NO_THEME_ENGINE) != NO_THEME_ENGINE) {
                return prefs.getInt("current_theme_mode", NO_THEME_ENGINE);
            }

            if (IS_PIE) {
                if (isNewSamsungDevice() && isAndromedaDevice(context)) {
                    // Andromeda mode
                    prefs.edit().putInt(
                            "current_theme_mode",
                            OVERLAY_MANAGER_SERVICE_O_ANDROMEDA
                    ).apply();
                    return OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
                } else if (checkSubstratumService(context)) {
                    // SS mode
                    prefs.edit().putInt(
                            "current_theme_mode",
                            OVERLAY_MANAGER_SERVICE_O_UNROOTED
                    ).apply();
                    return OVERLAY_MANAGER_SERVICE_O_UNROOTED;
                }
            } else if (IS_OREO) {
                if (isAndromedaDevice(context)) {
                    // Andromeda mode
                    prefs.edit().putInt(
                            "current_theme_mode",
                            OVERLAY_MANAGER_SERVICE_O_ANDROMEDA
                    ).apply();
                    return OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
                } else if (checkSubstratumService(context)) {
                    // SS mode
                    prefs.edit().putInt(
                            "current_theme_mode",
                            OVERLAY_MANAGER_SERVICE_O_UNROOTED
                    ).apply();
                    return OVERLAY_MANAGER_SERVICE_O_UNROOTED;
                } else if (Root.checkRootAccess()) {
                    // Rooted mode
                    prefs.edit().putInt(
                            "current_theme_mode",
                            OVERLAY_MANAGER_SERVICE_O_ROOTED
                    ).apply();
                    return OVERLAY_MANAGER_SERVICE_O_ROOTED;
                }
            } else if (IS_NOUGAT) {
                if (isBinderInterfacer(context)) {
                    // Interfacer mode
                    prefs.edit().putInt(
                            "current_theme_mode",
                            OVERLAY_MANAGER_SERVICE_N_UNROOTED
                    ).apply();
                    return OVERLAY_MANAGER_SERVICE_N_UNROOTED;
                } else if (isSamsungDevice(context)) {
                    // Sungstratum mode
                    prefs.edit().putInt(
                            "current_theme_mode",
                            SAMSUNG_THEME_ENGINE_N
                    ).apply();
                    return SAMSUNG_THEME_ENGINE_N;
                } else if (Root.checkRootAccess()) {
                    // Rooted mode
                    prefs.edit().putInt(
                            "current_theme_mode",
                            RUNTIME_RESOURCE_OVERLAY_N_ROOTED
                    ).apply();
                    return RUNTIME_RESOURCE_OVERLAY_N_ROOTED;
                }
            }
        }
        return NO_THEME_ENGINE;
    }

    /**
     * Set a retained property to refer to rather than constantly calling the OMS state
     *
     * @param context If you haven't gotten this by now, better start reading Android docs
     */
    public static void setAndCheckOMS(Context context) {
        prefs.edit().remove("oms_state").apply();
        try {
            boolean foundOms = false;
            if (!isSamsungDevice(context)) {
                String sonyCheck = getProp("ro.sony.fota.encrypteddata");
                if (checkThemeInterfacer(context) || checkSubstratumService(context)) {
                    foundOms = true;
                } else if (sonyCheck == null || sonyCheck.length() == 0) {
                    boolean isOMSRunning = isServiceRunning(IOverlayManager.class,
                            context.getApplicationContext());
                    if (isOMSRunning || IS_OREO || IS_PIE) {
                        Substratum.log(SUBSTRATUM_LOG,
                                "This device fully supports the Overlay Manager Service...");
                        foundOms = true;
                    } else {
                        String out = Root.runCommand("cmd overlay").split("\n")[0];
                        if ("The overlay manager has already been initialized.".equals(out) ||
                                "Overlay manager (overlay) commands:".equals(out)) {
                            Substratum.log(SUBSTRATUM_LOG,
                                    "This device fully supports the Overlay Manager Service...");
                            foundOms = true;
                        }
                    }
                } else {
                    Substratum.log(SUBSTRATUM_LOG, "Sony Mobile Overlay Manager Service found, " +
                            "falling back to RRO2 for vendor bypass...");
                }
            }

            if (foundOms && !isSamsungDevice(context)) {
                prefs.edit().putBoolean("oms_state", true).apply();
                Substratum.log(SUBSTRATUM_LOG, "Initializing Substratum with Dynamic Overlay / " +
                        "Overlay Manager Service support!");
            } else {
                prefs.edit().putBoolean("oms_state", false).apply();
                Substratum.log(SUBSTRATUM_LOG,
                        "Initializing Substratum with Runtime Resource Overlay support!");
            }
        } catch (Exception e) {
            prefs.edit().putBoolean("oms_state", false).apply();
            Substratum.log(SUBSTRATUM_LOG, "Initializing Substratum with Runtime Resource Overlay support!");
        }
    }

    /**
     * Check if it is using the Theme Interfacer backend
     *
     * @param context Just, no
     * @return True, if using Theme Interfacer
     */
    public static boolean checkThemeInterfacer(Context context) {
        if (context == null) {
            Log.e(SUBSTRATUM_LOG,
                    "activity has been destroyed, cannot check if interfacer is used");
            return false;
        }
        return getThemeInterfacerPackage(context) != null;
    }

    /**
     * Set a retained property to refer to rather than constantly calling the SS state
     */
    public static void setAndCheckSubstratumService() {
        StringBuilder check = References.runShellCommand("cmd -l");
        boolean present = check != null && check.toString().contains("substratum");
        prefs.edit().putBoolean("substratum_service_present", false).apply();
        if (present) {
            prefs.edit().putBoolean("substratum_service_present", true).apply();
        }
    }

    /**
     * Determine whether the device supports the new Substratum service
     *
     * @return True, if Substratum service
     */
    public static boolean checkSubstratumService(Context context) {
        if (context == null) return true; // Safe to assume that window refreshes only on OMS
        if (!prefs.contains("substratum_service_present")) {
            setAndCheckSubstratumService();
        }
        return prefs.getBoolean("substratum_service_present", false);
    }

    public static boolean checkSubstratumServiceApi(Context context) {
        return !checkSubstratumService(context) || SubstratumService.checkApi();
    }

    /**
     * Check if it is using the Andromeda backend
     *
     * @param context You know it, right?
     * @return True, if using Andromeda
     */
    public static boolean isAndromedaDevice(Context context) {
        if (context == null) {
            Log.e(SUBSTRATUM_LOG,
                    "activity has been destroyed, cannot check if andromeda is used");
            return false;
        }

        boolean isEnabled = Packages.isAvailablePackage(context, References.ANDROMEDA_PACKAGE);
        PackageInfo packageInfo = getAndromedaPackage(context);
        return (packageInfo != null) && isEnabled;
    }

    /**
     * Check if it is using the latest Theme Interfacer backend
     *
     * @param context Context!
     * @return True, if using Theme Interfacer
     */
    public static boolean isBinderInterfacer(Context context) {
        boolean isEnabled = Packages.isAvailablePackage(context, References
                .INTERFACER_PACKAGE);
        PackageInfo packageInfo = getThemeInterfacerPackage(context);
        return (packageInfo != null) && (packageInfo.versionCode >= 60) && isEnabled;
    }

    /**
     * Checks if the device is running a Samsung system software
     *
     * @param context Context?
     * @return True, if Samsung
     */
    public static boolean isSamsung(Context context) {
        if (!isSamsungDevice(context)) return false;
        if (isNewSamsungDeviceAndromeda(context)) return true;

        String liveInstaller = Packages.getInstallerId(context, SST_ADDON_PACKAGE);

        return Packages.isPackageInstalled(context, SST_ADDON_PACKAGE) &&
                ((liveInstaller != null) &&
                        liveInstaller.equals(PLAY_STORE_PACKAGE_NAME));
    }

    public static boolean isSamsungDevice(Context context) {
        if (isNewSamsungDeviceAndromeda(context)) return true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return false;
        if (isSamsungDevice != null) return isSamsungDevice;
        if (context != null) {
            List<String> listOfFeatures =
                    Arrays.asList(context.getPackageManager().getSystemSharedLibraryNames());
            isSamsungDevice = listOfFeatures.contains("touchwiz");
            return isSamsungDevice;
        } else {
            return false;
        }
    }

    /**
     * Checks if it passes all cases of new Samsung Oreo checks
     *
     * @return True, if it passes all Samsung tests
     */
    public static boolean isNewSamsungDevice() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                new File("/system/etc/permissions/com.samsung.device.xml").exists();
    }

    /**
     * Checks if the system supports sungstromeda mode
     *
     * @param context Context
     * @return True, if the device can utilize both sungstratum + andromeda mode on Oreo onwards
     */
    public static boolean isNewSamsungDeviceAndromeda(Context context) {
        boolean sungstromeda = prefs.getBoolean("sungstromeda_mode", true);
        return sungstromeda && isNewSamsungDevice() && isAndromedaDevice(context);
    }

    /**
     * Checks if the device is a Xiaomi device
     *
     * @return True, if it passes all Xiaomi tests
     */
    public static boolean isXiaomiDevice() {
        return new File("/system/etc/permissions/platform-miui.xml").exists();
    }

    /**
     * Obtains the Andromeda Package
     *
     * @param context Context...
     * @return True, if it is enabled and installed
     */
    private static PackageInfo getAndromedaPackage(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(ANDROMEDA_PACKAGE, 0);
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Obtains the Theme Interfacer Package
     *
     * @param context CoNtExT
     * @return True, if it is enabled and installed
     */
    public static PackageInfo getThemeInterfacerPackage(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(INTERFACER_PACKAGE, 0);
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Obtains the device encryption status
     * <p>
     * 0: ENCRYPTION_STATUS_UNSUPPORTED
     * 1: ENCRYPTION_STATUS_INACTIVE
     * 2: ENCRYPTION_STATUS_ACTIVATING
     * 3: ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY
     * 4: ENCRYPTION_STATUS_ACTIVE
     * 5: ENCRYPTION_STATUS_ACTIVE_PER_USER
     *
     * @param context The context bud, THE CONTEXT!
     * @return Returns the system's encryption status
     */
    public static int getDeviceEncryptionStatus(Context context) {
        int status = DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED;
        DevicePolicyManager dpm = (DevicePolicyManager)
                context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) status = dpm.getStorageEncryptionStatus();
        return status;
    }

    /**
     * Checks whether the projekt.substratum.THEME permission is found on a custom ROM
     *
     * @param context The context.
     * @return True, if found
     */
    public static boolean checkSubstratumFeature(Context context) {
        // Using lowercase because that's how we defined it in our permissions xml
        return context.getPackageManager()
                .hasSystemFeature(SUBSTRATUM_THEME.toLowerCase(Locale.US));
    }

    /**
     * Compare a target date with the system security patch
     *
     * @param comparePatch Target date
     * @return True, if system security patch is greater than target date
     */
    public static boolean isSystemSecurityPatchNewer(String comparePatch) {
        try {
            if (comparePatch.length() != 10) throw new Exception("Incorrect string input!");
            String systemSecurityPatch = Build.VERSION.SECURITY_PATCH;
            Date systemPatchDate =
                    new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(systemSecurityPatch);
            Date comparisonPatchDate =
                    new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(comparePatch);
            return systemPatchDate.after(comparisonPatchDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Determine whether the device was dirty/clean flashed
     *
     * @param context Context!
     * @return True, if clean
     */
    public static Boolean checkROMVersion(Context context) {
        if (!prefs.contains("rom_build_date")) {
            setROMVersion(false);
        }
        if ((isSamsungDevice(context) || isNewSamsungDevice()) &&
                !prefs.contains("samsung_migration_key")) {
            prefs.edit().putInt("samsung_migration_key", Build.VERSION.SDK_INT).apply();
        }
        String prop = getProp("ro.build.date.utc");
        return prefs.getInt("rom_build_date", 0) ==
                (((prop != null) && !prop.isEmpty()) ? Integer.parseInt(prop) : 0);
    }

    /**
     * Retain the ROM version in the Shared Preferences of the app
     *
     * @param force Do not dynamically set it, instead, force!
     */
    public static void setROMVersion(boolean force) {
        if (!prefs.contains("rom_build_date") || force) {
            String prop = getProp("ro.build.date.utc");
            prefs.edit().putInt("rom_build_date",
                    ((prop != null) && !prop.isEmpty()) ? Integer.parseInt(prop) : 0)
                    .apply();
        }
    }

    /**
     * Obtain the device ID of the device
     *
     * @param context Context...
     * @return Returns a string of the device's ID
     */
    @SuppressLint("HardwareIds")
    public static String getDeviceID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    /**
     * Build.prop scavenger
     *
     * @param propName Desired prop to find
     * @return String of the prop value's output
     */
    public static String getProp(String propName) {
        Process p = null;
        String result = "";
        try {
            p = new ProcessBuilder("/system/bin/getprop", propName)
                    .redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream())
            )) {
                String line;
                while ((line = br.readLine()) != null) {
                    result = line;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
        return result;
    }

    /**
     * Denied packages on the device
     * <p>
     * This will never be always up to date, so we end up using our main checker!
     *
     * @param context Self explanatory...what?
     * @return True if blacklisted packages found
     */
    public static boolean checkPackageSupport(Context context, Boolean override) {
        if (checkPackageSupported == null || override) {
            String[] blacklistedPackages = {
                    "com.android.vending.billing.InAppBillingService.",
                    "uret.jasi2169.",
                    "com.dimonvideo.",
                    "com.chelpus.",
                    "com.forpda.",
                    "zone.jasi2169."
            };
            checkPackageSupported = checkPackageRegex(context, blacklistedPackages) || hashPassthrough(context) == 0;
        }
        return checkPackageSupported;
    }

    /**
     * Helper function of checkPackageSupport {@link #checkPackageSupport(Context, Boolean)}
     *
     * @param context     Self explanatory...what?
     * @param stringArray List of packages to check
     * @return True if blacklisted packages found
     */
    private static Boolean checkPackageRegex(Context context,
                                             String[] stringArray) {
        if (stringArray.length == 0) return true;
        final PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> listOfInstalled = new ArrayList<>();
        for (ApplicationInfo packageInfo : packages) {
            listOfInstalled.add(packageInfo.packageName);
        }
        String invocation = listOfInstalled.toString();
        for (String packageName : stringArray) {
            if (invocation.contains(packageName))
                return true;
        }
        return false;
    }

    /**
     * Firmware analyzer
     *
     * @param context  Context!!!???
     * @param url      Source of where to check the main repository of supported devices
     * @param fileName Filename of the file to read
     * @return Return string of the supported ROM
     */
    public static String checkFirmwareSupport(
            Context context,
            String url,
            String fileName) {
        String supportedRom = "";
        try {
            if (isNetworkAvailable(context)) {
                FileDownloader.init(context, url, "", fileName);
            } else {
                File check = new File(
                        context.getCacheDir().getAbsolutePath() + '/' + fileName);
                if (!check.exists()) return "";
            }

            Map<String, String> listOfRoms =
                    ReadSupportedROMsFile.read(context.getCacheDir() + "/" + fileName);
            boolean supported = false;

            // First check if it is a valid prop
            for (Object o : listOfRoms.entrySet()) {
                Map.Entry pair = (Map.Entry) o;
                String key = (String) pair.getKey();
                String value = (String) pair.getValue();
                Process process = Runtime.getRuntime().exec("getprop " + key);
                process.waitFor();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if ((line != null) && !line.isEmpty()) {
                        if ((value == null) || value.isEmpty()) {
                            String current = key;
                            if (current.contains(".")) {
                                current = current.split("\\.")[1];
                            }
                            Substratum.log(SUBSTRATUM_LOG, "Supported ROM: " + current);
                            supportedRom = current;
                            supported = true;
                        } else {
                            Substratum.log(SUBSTRATUM_LOG, "Supported ROM: " + value);
                            supportedRom = value;
                            supported = true;
                        }
                        break;
                    }
                }
            }

            // Then check ro.product.flavor
            if (!supported) {
                Iterator it = listOfRoms.entrySet().iterator();
                Process process = Runtime.getRuntime().exec("getprop ro.build.flavor");
                process.waitFor();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry) it.next();

                            String key = (String) pair.getKey();
                            String value = (String) pair.getValue();

                            if (line.toLowerCase(Locale.US)
                                    .contains(key.toLowerCase(
                                            Locale.US))) {
                                if ((value == null) || value.isEmpty()) {
                                    String current = key;
                                    if (current.contains(".")) {
                                        current = current.split("\\.")[1];
                                    }
                                    Substratum.log(SUBSTRATUM_LOG,
                                            "Supported ROM (1): " + current);
                                    supportedRom = current;
                                    supported = true;
                                } else {
                                    Substratum.log(SUBSTRATUM_LOG,
                                            "Supported ROM (1): " + value);
                                    supportedRom = value;
                                    supported = true;
                                }
                                break;
                            }
                        }
                        if (supported) break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return supportedRom;
    }

    /**
     * Check whether usage permissions are granted to the app
     *
     * @param context My god, this context thing is really annoying!
     * @return True, if granted.
     */
    public static boolean checkUsagePermissions(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo =
                    packageManager.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager)
                    context.getSystemService(Context.APP_OPS_SERVICE);
            assert appOpsManager != null;
            int mode = appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid,
                    applicationInfo.packageName);
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks whether there is root access on the device
     *
     * @return True, if root is granted
     */
    public static boolean checkRootAccess() {
        if (!prefs.contains("root_access")) {
            setAndCheckRootAccess();
        }
        return prefs.getBoolean("root_access", false);
    }

    /**
     * Set a retained property to refer to rather than constantly calling the requestRootAccess method
     */
    private static void setAndCheckRootAccess() {
        boolean access = Root.requestRootAccess();
        prefs.edit().putBoolean("root_access", false).apply();
        if (access) {
            prefs.edit().putBoolean("root_access", true).apply();
        }
    }
}