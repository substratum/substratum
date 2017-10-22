package projekt.substratum.common;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.om.IOverlayManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import projekt.substratum.common.analytics.PackageAnalytics;
import projekt.substratum.util.files.FileDownloader;
import projekt.substratum.util.files.Root;
import projekt.substratum.util.readers.ReadSupportedROMsFile;

import static projekt.substratum.common.References.ANDROMEDA_PACKAGE;
import static projekt.substratum.common.References.BYPASS_ALL_VERSION_CHECKS;
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
import static projekt.substratum.common.References.isNetworkAvailable;

public enum Systems {
    ;

    private static boolean isOMSRunning(final Context context, final Class<?> serviceClass) {
        final ActivityManager activityManager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        assert activityManager != null;
        final List<ActivityManager.RunningServiceInfo> services =
                activityManager.getRunningServices(Integer.MAX_VALUE);

        for (final ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClass.getName())) {
                return true;
            }
        }
        return false;
    }

    // This method is used to determine whether there the system is initiated with OMS
    public static Boolean checkOMS(@NonNull final Context context) {
        //noinspection ConstantConditions
        if (context == null) return true; // Safe to assume that window refreshes only on OMS
        if (!BYPASS_ALL_VERSION_CHECKS) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (!prefs.contains("oms_state")) {
                setAndCheckOMS(context);
            }
            return prefs.getBoolean("oms_state", false);
        } else {
            return false;
        }
    }

    public static int checkThemeSystemModule(final Context context, final boolean firstLaunch) {
        if (context != null) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (!firstLaunch &&
                    prefs.getInt("CURRENT_THEME_MODE", NO_THEME_ENGINE) != NO_THEME_ENGINE) {
                return prefs.getInt("CURRENT_THEME_MODE", NO_THEME_ENGINE);
            }

            final Boolean rooted = Root.checkRootAccess();
            if (checkOreo()) {
                if (rooted) {
                    // Rooted mode
                    prefs.edit().putInt(
                            "CURRENT_THEME_MODE",
                            OVERLAY_MANAGER_SERVICE_O_ROOTED
                    ).apply();
                    return OVERLAY_MANAGER_SERVICE_O_ROOTED;
                } else if (isAndromedaDevice(context) && !isBinderInterfacer(context)) {
                    // Andromeda mode
                    prefs.edit().putInt(
                            "CURRENT_THEME_MODE",
                            OVERLAY_MANAGER_SERVICE_O_ANDROMEDA
                    ).apply();
                    return OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
                } else if (isBinderInterfacer(context)) {
                    // Interfacer mode
                    prefs.edit().putInt(
                            "CURRENT_THEME_MODE",
                            OVERLAY_MANAGER_SERVICE_O_UNROOTED
                    ).apply();
                    return OVERLAY_MANAGER_SERVICE_O_UNROOTED;
                }
            } else if (checkNougat()) {
                if (isBinderInterfacer(context)) {
                    // Interfacer mode
                    prefs.edit().putInt(
                            "CURRENT_THEME_MODE",
                            OVERLAY_MANAGER_SERVICE_N_UNROOTED
                    ).apply();
                    return OVERLAY_MANAGER_SERVICE_N_UNROOTED;
                } else if (isSamsungDevice(context)) {
                    // Sungstratum mode
                    prefs.edit().putInt(
                            "CURRENT_THEME_MODE",
                            SAMSUNG_THEME_ENGINE_N
                    ).apply();
                    return SAMSUNG_THEME_ENGINE_N;
                } else if (rooted) {
                    // Rooted mode
                    prefs.edit().putInt(
                            "CURRENT_THEME_MODE",
                            RUNTIME_RESOURCE_OVERLAY_N_ROOTED
                    ).apply();
                    return RUNTIME_RESOURCE_OVERLAY_N_ROOTED;
                }
            }
        }
        return NO_THEME_ENGINE;
    }

    public static int checkThemeSystemModule(final Context context) {
        return checkThemeSystemModule(context, false);
    }

    public static Boolean checkOreo() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.O;
    }

    private static Boolean checkNougat() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.N ||
                Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1;
    }

    public static void setAndCheckOMS(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove("oms_state").apply();
        try {
            boolean foundOms = false;
            if (!isSamsungDevice(context)) {
                if (checkThemeInterfacer(context)) {
                    foundOms = true;
                } else {
                    final Boolean isOMSRunning = isOMSRunning(context.getApplicationContext(),
                            IOverlayManager.class);
                    if (isOMSRunning || checkOreo()) {
                        Log.d(SUBSTRATUM_LOG, "Found Overlay Manager Service...");
                        foundOms = true;
                    } else {
                        final String out = Root.runCommand("cmd overlay").split("\n")[0];
                        if ("The overlay manager has already been initialized.".equals(out) ||
                                "Overlay manager (overlay) commands:".equals(out)) {
                            Log.d(SUBSTRATUM_LOG, "Found Overlay Manager Service...");
                            foundOms = true;
                        }
                    }
                }
            }

            if (foundOms && !isSamsungDevice(context)) {
                prefs.edit().putBoolean("oms_state", true).apply();
                prefs.edit().putInt("oms_version", 7).apply();
                Log.d(SUBSTRATUM_LOG, "Initializing Substratum with the seventh " +
                        "iteration of the Overlay Manager Service...");
            } else {
                prefs.edit().putBoolean("oms_state", false).apply();
                prefs.edit().putInt("oms_version", 0).apply();
                Log.d(SUBSTRATUM_LOG, "Initializing Substratum with the second " +
                        "iteration of the Resource Runtime Overlay system...");
            }
        } catch (final Exception e) {
            prefs.edit().putBoolean("oms_state", false).apply();
            prefs.edit().putInt("oms_version", 0).apply();
            Log.d(SUBSTRATUM_LOG, "Initializing Substratum with the second " +
                    "iteration of the Resource Runtime Overlay system...");
        }
    }

    // Begin check if device is running on the latest theme interface
    public static boolean checkThemeInterfacer(final Context context) {
        if (context == null) {
            Log.e(SUBSTRATUM_LOG,
                    "activity has been destroyed, cannot check if interfacer is used");
            return false;
        }
        return getThemeInterfacerPackage(context) != null;
    }

    public static boolean checkAndromeda(final Context context) {
        if (context == null) {
            Log.e(SUBSTRATUM_LOG,
                    "activity has been destroyed, cannot check if andromeda is used");
            return false;
        }

        final SharedPreferences prefs =
                context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
        final String fingerprint = prefs.getString("andromeda_fp", "o");
        final String expFingerprint = prefs.getString(
                "andromeda_exp_fp_" + Packages.getAppVersionCode(context, ANDROMEDA_PACKAGE), "0");
        final String installer = prefs.getString("andromeda_installer", "o");

        boolean andromedaPresent = isAndromedaDevice(context);
        andromedaPresent &= installer.equals(PLAY_STORE_PACKAGE_NAME);
        andromedaPresent &= fingerprint.toUpperCase(Locale.US)
                .equals(expFingerprint.toUpperCase(Locale.US));
        return andromedaPresent;
    }

    public static boolean isAndromedaDevice(final Context context) {
        if (context == null) {
            Log.e(SUBSTRATUM_LOG,
                    "activity has been destroyed, cannot check if andromeda is used");
            return false;
        }

        return getAndromedaPackage(context) != null;
    }

    // Begin check if device is running on the latest theme interface
    public static boolean isBinderInterfacer(final Context context) {
        final boolean isEnabled = Packages.isAvailablePackage(context, References.INTERFACER_PACKAGE);
        final PackageInfo packageInfo = getThemeInterfacerPackage(context);
        return packageInfo != null && packageInfo.versionCode >= 60 && isEnabled;
    }

    // Check if the system is of the Samsung variant
    public static boolean isSamsung(final Context context) {
        final boolean isTouchWiz = isSamsungDevice(context);
        if (!isTouchWiz) return false;

        final SharedPreferences prefs =
                context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);

        final boolean debuggingValue = prefs.getBoolean("sungstratum_debug", true);
        final boolean installer = prefs.getBoolean("sungstratum_installer", false);
        final String fingerprint = prefs.getString("sungstratum_fp", "0");
        final String expFingerprint = prefs.getString(
                "sungstratum_exp_fp_" + Packages.getAppVersionCode(context, SST_ADDON_PACKAGE),
                "o");
        final String liveInstaller = PackageAnalytics.getPackageInstaller(context, SST_ADDON_PACKAGE);
        final boolean liveInstallerValidity = liveInstaller != null &&
                liveInstaller.equals(PLAY_STORE_PACKAGE_NAME);

        boolean sungstratumPresent = !debuggingValue;
        sungstratumPresent &= installer;
        sungstratumPresent &= fingerprint.toUpperCase(
                Locale.US).equals(
                expFingerprint.toUpperCase(Locale.US));
        sungstratumPresent &= liveInstallerValidity;
        return sungstratumPresent;
    }

    // Check if the system is of the Samsung variant
    public static boolean isSamsungDevice(final Context context) {
        if (context != null) {
            final List<String> listOfFeatures =
                    Arrays.asList(context.getPackageManager().getSystemSharedLibraryNames());
            return listOfFeatures.contains("touchwiz");
        } else {
            return false;
        }
    }

    private static PackageInfo getAndromedaPackage(final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(ANDROMEDA_PACKAGE, 0);
        } catch (final Exception e) {
            // Andromeda was not installed
        }
        return null;
    }

    public static PackageInfo getThemeInterfacerPackage(final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(INTERFACER_PACKAGE, 0);
        } catch (final Exception e) {
            // Theme Interfacer was not installed
        }
        return null;
    }

    public static int getDeviceEncryptionStatus(final Context context) {
        // 0: ENCRYPTION_STATUS_UNSUPPORTED
        // 1: ENCRYPTION_STATUS_INACTIVE
        // 2: ENCRYPTION_STATUS_ACTIVATING
        // 3: ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY
        // 4: ENCRYPTION_STATUS_ACTIVE
        // 5: ENCRYPTION_STATUS_ACTIVE_PER_USER
        int status = DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED;
        final DevicePolicyManager dpm = (DevicePolicyManager)
                context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) status = dpm.getStorageEncryptionStatus();
        return status;
    }

    public static boolean checkSubstratumFeature(final Context context) {
        // Using lowercase because that's how we defined it in our permissions xml
        return context.getPackageManager()
                .hasSystemFeature(SUBSTRATUM_THEME.toLowerCase(Locale.US));
    }

    // This method is used to determine whether there the system was dirty flashed / upgraded
    public static Boolean checkROMVersion(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains("build_date")) {
            setROMVersion(context, false);
        }
        final String prop = getProp("ro.build.date.utc");
        return prefs.getInt("build_date", 0) ==
                ((prop != null && !prop.isEmpty()) ? Integer.parseInt(prop) : 0);
    }

    public static void setROMVersion(final Context context, final boolean force) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains("build_date") || force) {
            final String prop = getProp("ro.build.date.utc");
            prefs.edit().putInt("build_date",
                    (prop != null && !prop.isEmpty()) ? Integer.parseInt(prop) : 0)
                    .apply();
        }
    }

    // This method is used to obtain the device ID of the current device (set up)
    @SuppressLint("HardwareIds")
    public static String getDeviceID(final Context context) {
        return Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    // This method is used to check whether a build.prop value is found
    public static String getProp(final String propName) {
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
            } catch (final IOException e) {
                e.printStackTrace();
            }
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
        return result;
    }

    // Check for the denied packages if existing on the device
    static boolean checkPackageSupport(final Context context) {
        boolean blacklistedPackageFound = false;
        final String[] blacklistedPackages = {
                "com.android.vending.billing.InAppBillingService.",
                "com.android.vending.billing.InAppBillingService.LOCK",
                "com.android.vending.billing.InAppBillingService.LACK",
                "uret.jasi2169.",
                "uret.jasi2169.patcher",
                "com.dimonvideo.luckypatcher",
                "com.chelpus.lackypatch",
                "com.forpda.lp",
                "com.android.vending.billing.InAppBillingService.LUCK",
                "zone.jasi2169.uretpatcher",
                "zone.jasi2169."
        };
        for (final String packageName : blacklistedPackages) {
            if (Packages.isPackageInstalled(context, packageName, false)) {
                blacklistedPackageFound = true;
                break;
            }
        }
        return blacklistedPackageFound;
    }

    public static String checkFirmwareSupport(final Context context, final String url, final String fileName) {
        String supported_rom = "";
        try {
            if (isNetworkAvailable(context)) {
                FileDownloader.init(context, url, "", fileName);
            } else {
                final File check = new File(context.getCacheDir().getAbsolutePath() + "/" + fileName);
                if (!check.exists()) return "";
            }

            final Map<String, String> listOfRoms =
                    ReadSupportedROMsFile.main(context.getCacheDir() + "/" + fileName);
            Boolean supported = false;

            // First check if it is a valid prop
            for (final Object o : listOfRoms.entrySet()) {
                final Map.Entry pair = (Map.Entry) o;
                final String key = (String) pair.getKey();
                final String value = (String) pair.getValue();
                final Process process = Runtime.getRuntime().exec("getprop " + key);
                process.waitFor();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    final String line = reader.readLine();
                    if (line != null && !line.isEmpty()) {
                        if (value == null || value.isEmpty()) {
                            String current = key;
                            if (current.contains(".")) {
                                current = current.split("\\.")[1];
                            }
                            Log.d(SUBSTRATUM_LOG, "Supported ROM: " + current);
                            supported_rom = current;
                            supported = true;
                        } else {
                            Log.d(SUBSTRATUM_LOG, "Supported ROM: " + value);
                            supported_rom = value;
                            supported = true;
                        }
                        break;
                    }
                }
            }

            // Then check ro.product.flavor
            if (!supported) {
                final Iterator it = listOfRoms.entrySet().iterator();
                final Process process = Runtime.getRuntime().exec("getprop ro.build.flavor");
                process.waitFor();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        while (it.hasNext()) {
                            final Map.Entry pair = (Map.Entry) it.next();

                            final String key = (String) pair.getKey();
                            final String value = (String) pair.getValue();

                            if (line.toLowerCase(Locale.US)
                                    .contains(key.toLowerCase(
                                            Locale.US))) {
                                if (value == null || value.isEmpty()) {
                                    String current = key;
                                    if (current.contains(".")) {
                                        current = current.split("\\.")[1];
                                    }
                                    Log.d(SUBSTRATUM_LOG,
                                            "Supported ROM (1): " + current);
                                    supported_rom = current;
                                    supported = true;
                                } else {
                                    Log.d(SUBSTRATUM_LOG,
                                            "Supported ROM (1): " + value);
                                    supported_rom = value;
                                    supported = true;
                                }
                                break;
                            }
                        }
                        if (supported) break;
                    }
                }
            }
        } catch (final Exception e) {
            // Suppress warning
        }
        return supported_rom;
    }

    // Check usage permissions
    public static boolean checkUsagePermissions(final Context mContext) {
        try {
            final PackageManager packageManager = mContext.getPackageManager();
            final ApplicationInfo applicationInfo =
                    packageManager.getApplicationInfo(mContext.getPackageName(), 0);
            final AppOpsManager appOpsManager = (AppOpsManager)
                    mContext.getSystemService(Context.APP_OPS_SERVICE);
            assert appOpsManager != null;
            final int mode = appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid,
                    applicationInfo.packageName);
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (final PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
