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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import projekt.substratum.BuildConfig;
import projekt.substratum.util.helpers.FileDownloader;
import projekt.substratum.util.helpers.Root;
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
import static projekt.substratum.common.References.hashPassthrough;
import static projekt.substratum.common.References.isNetworkAvailable;
import static projekt.substratum.common.References.spreadYourWingsAndFly;

public enum Systems {
    ;

    /**
     * Cached boolean to find if system
     * passes all cases of Samsung checks
     */
    public static Boolean isSamsungDevice = null;
    static Boolean checkPackageSupported;

    /**
     * Check whether Overlay Manager Service is actually running on the device
     *
     * @param context      Self explantory, bro
     * @param serviceClass Specified OMS class name
     * @return True, if OMS is running
     */
    private static boolean isOMSRunning(
            Context context,
            Class<?> serviceClass) {
        ActivityManager activityManager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        assert activityManager != null;
        List<ActivityManager.RunningServiceInfo> services =
                activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClass.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is used to determine whether there the system is initiated with OMS
     *
     * @param context Self explanatory, sir
     */
    public static Boolean checkOMS(@NonNull Context context) {
        //noinspection ConstantConditions
        if (context == null) return true; // Safe to assume that window refreshes only on OMS
        if (!BYPASS_ALL_VERSION_CHECKS) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
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
    public static String getForceAuthorizationSetting(Context context) {
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
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (!firstLaunch &&
                    (prefs.getInt("CURRENT_THEME_MODE", NO_THEME_ENGINE) != NO_THEME_ENGINE)) {
                return prefs.getInt("CURRENT_THEME_MODE", NO_THEME_ENGINE);
            }

            if (checkOreo()) {
                if (isAndromedaDevice(context)) {
                    // Andromeda mode
                    prefs.edit().putInt(
                            "CURRENT_THEME_MODE",
                            OVERLAY_MANAGER_SERVICE_O_ANDROMEDA
                    ).apply();
                    return OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
                } else if (checkSubstratumService(context)) {
                    // SS mode
                    prefs.edit().putInt(
                            "CURRENT_THEME_MODE",
                            OVERLAY_MANAGER_SERVICE_O_UNROOTED
                    ).apply();
                    return OVERLAY_MANAGER_SERVICE_O_UNROOTED;
                } else if (Root.checkRootAccess()) {
                    // Rooted mode
                    prefs.edit().putInt(
                            "CURRENT_THEME_MODE",
                            OVERLAY_MANAGER_SERVICE_O_ROOTED
                    ).apply();
                    return OVERLAY_MANAGER_SERVICE_O_ROOTED;
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
                } else if (Root.requestRootAccess()) {
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

    /**
     * Check if the device is Oreo based
     *
     * @return True, if yes.
     */
    public static Boolean checkOreo() {
        return (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ||
                (Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1);
    }

    /**
     * Check if the device is Nougat based
     *
     * @return True, if yes.
     */
    private static Boolean checkNougat() {
        return (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) ||
                (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1);
    }

    /**
     * Set a retained property to refer to rather than constantly calling the OMS state
     *
     * @param context If you haven't gotten this by now, better start reading Android docs
     */
    public static void setAndCheckOMS(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove("oms_state").apply();
        try {
            boolean foundOms = false;
            if (!isSamsungDevice(context)) {
                String sonyCheck = getProp("ro.sony.fota.encrypteddata");
                if (checkThemeInterfacer(context) || checkSubstratumService(context)) {
                    foundOms = true;
                } else if (sonyCheck == null || sonyCheck.length() == 0) {
                    Boolean isOMSRunning = isOMSRunning(context.getApplicationContext(),
                            IOverlayManager.class);
                    if (isOMSRunning || checkOreo()) {
                        Log.d(SUBSTRATUM_LOG, "Found Overlay Manager Service...");
                        foundOms = true;
                    } else {
                        String out = Root.runCommand("cmd overlay").split("\n")[0];
                        if ("The overlay manager has already been initialized.".equals(out) ||
                                "Overlay manager (overlay) commands:".equals(out)) {
                            Log.d(SUBSTRATUM_LOG, "Found Overlay Manager Service...");
                            foundOms = true;
                        }
                    }
                } else {
                    Log.d(SUBSTRATUM_LOG, "Sony Mobile Overlay Manager Service found, " +
                            "falling back to RRO2 for vendor bypass...");
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
        } catch (Exception e) {
            prefs.edit().putBoolean("oms_state", false).apply();
            prefs.edit().putInt("oms_version", 0).apply();
            Log.d(SUBSTRATUM_LOG, "Initializing Substratum with the second " +
                    "iteration of the Resource Runtime Overlay system...");
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
     *
     * @param context If you haven't gotten this by now, better start reading Android docs
     */
    public static void setAndCheckSubstratumService(Context context) {
        StringBuilder check = References.runShellCommand("cmd -l");
        Boolean present = check != null && check.toString().contains("substratum");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains("substratum_service_present")) {
            setAndCheckSubstratumService(context);
        }
        return prefs.getBoolean("substratum_service_present", false);
    }

    /**
     * Check if it is using the Andromeda backend
     *
     * @param context You know it, right?
     * @return True, if using Andromeda
     */
    public static boolean checkAndromeda(Context context) {
        if (context == null) {
            Log.e(SUBSTRATUM_LOG,
                    "activity has been destroyed, cannot check if andromeda is used");
            return false;
        }

        SharedPreferences prefs =
                context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
        String fingerprint = prefs.getString("andromeda_fp", "o");
        String expFingerprint = prefs.getString(
                "andromeda_exp_fp_" + Packages.getAppVersionCode(context, ANDROMEDA_PACKAGE), "0");
        String installer = prefs.getString("andromeda_installer", "o");

        boolean andromedaPresent = isAndromedaDevice(context);
        andromedaPresent &= installer.equals(PLAY_STORE_PACKAGE_NAME);
        andromedaPresent &= fingerprint.toUpperCase(Locale.US)
                .equals(expFingerprint.toUpperCase(Locale.US));
        return andromedaPresent;
    }

    /**
     * Check if it is using the Andromeda backend
     *
     * @param context You better know it.
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
        boolean isTouchWiz = isSamsungDevice(context);
        if (!isTouchWiz) return false;

        SharedPreferences prefs =
                context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);

        boolean debuggingValue = prefs.getBoolean("sungstratum_debug", true);
        boolean installer = prefs.getBoolean("sungstratum_installer", false);
        String fingerprint = prefs.getString("sungstratum_fp", "0");
        String expFingerprint = prefs.getString(
                "sungstratum_exp_fp_" + Packages.getAppVersionCode(context, SST_ADDON_PACKAGE),
                "o");
        String liveInstaller = Packages.getInstallerId(context, SST_ADDON_PACKAGE);

        boolean sungstratumPresent = !debuggingValue;
        sungstratumPresent &= installer;
        sungstratumPresent &= fingerprint.toUpperCase(
                Locale.US).equals(
                expFingerprint.toUpperCase(Locale.US));
        boolean liveInstallerValidity = (liveInstaller != null) &&
                liveInstaller.equals(PLAY_STORE_PACKAGE_NAME);
        sungstratumPresent &= liveInstallerValidity;
        return sungstratumPresent;
    }

    public static boolean isSamsungDevice(Context context) {
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
     * @param context CONTEXT!
     * @return True, if it passes all Samsung tests
     */
    public static boolean isNewSamsungDevice(Context context) {
        if (context != null) {
            if (isSamsungDevice(context)) return false;
            List<String> listOfFeatures =
                    Arrays.asList(context.getPackageManager().getSystemSharedLibraryNames());
            return listOfFeatures.contains("timakeystore");
        } else {
            return false;
        }
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
        } catch (Exception e) {
            // Andromeda was not installed
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
        } catch (Exception e) {
            // Theme Interfacer was not installed
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
     * Determine whether the device was dirty/clean flashed
     *
     * @param context Context!
     * @return True, if clean
     */
    public static Boolean checkROMVersion(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains("build_date")) {
            setROMVersion(context, false);
        }
        String prop = getProp("ro.build.date.utc");
        return prefs.getInt("build_date", 0) ==
                (((prop != null) && !prop.isEmpty()) ? Integer.parseInt(prop) : 0);
    }

    /**
     * Retain the ROM version in the Shared Preferences of the app
     *
     * @param context CONTEXT!!!
     * @param force   Do not dynamically set it, instead, force!
     */
    public static void setROMVersion(Context context,
                                     boolean force) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains("build_date") || force) {
            String prop = getProp("ro.build.date.utc");
            prefs.edit().putInt("build_date",
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
            //noinspection ConstantConditions
            checkPackageSupported = blacklistedPackages.length != 0 && (
                    checkPackageRegex(context, blacklistedPackages) ||
                            spreadYourWingsAndFly(context, override) ||
                            hashPassthrough(context, false) == 0);
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
    public static Boolean checkPackageRegex(Context context,
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
        String supported_rom = "";
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
            Boolean supported = false;

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
        } catch (Exception e) {
            // Suppress warning
        }
        return supported_rom;
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
}