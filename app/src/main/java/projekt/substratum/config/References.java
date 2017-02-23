package projekt.substratum.config;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import projekt.substratum.R;
import projekt.substratum.util.AOPTCheck;
import projekt.substratum.util.CacheCreator;

public class References {

    // These are specific log tags for different classes
    public static final Boolean ENABLE_SIGNING = true;
    public static final Boolean ENABLE_ROOT_CHECK = true;
    public static final Boolean ENABLE_AOPT_OUTPUT = false; // WARNING, DEVELOPERS - BREAKS COMPILE
    public static final String SUBSTRATUM_BUILDER = "SubstratumBuilder";
    public static final String SUBSTRATUM_LOG = "SubstratumLogger";
    public static final String SUBSTRATUM_ICON_BUILDER = "SubstratumIconBuilder";
    public static final String MASQUERADE_PACKAGE = "masquerade.substratum";
    // Delays for Masquerade Icon Pack Handling
    public static final int MAIN_WINDOW_REFRESH_DELAY = 2000;
    public static final int FIRST_WINDOW_REFRESH_DELAY = 1000;
    public static final int SECOND_WINDOW_REFRESH_DELAY = 3000;
    private static final String SUBSTRATUM_THEME = "projekt.substratum.THEME";
    // This controls the package name for the specified launchers allowed for Studio
    private static final String NOVA_LAUNCHER = "com.novalauncher.THEME";
    // November security update (incompatible firmware) timestamp;
    private static final long NOVEMBER_PATCH_TIMESTAMP = 1478304000000L;
    private static final long JANUARY_PATCH_TIMESTAMP = 1483549200000L;
    // This int controls the notification identifier
    public static int firebase_notification_id = 24862486;
    public static int notification_id = 2486;
    public static int notification_id_upgrade = 248600;
    // Universal switch for Application-wide Debugging
    public static Boolean DEBUG = true;
    // This int controls the delay for window refreshes to occur
    public static int REFRESH_WINDOW_DELAY = 500;
    public static int SYSTEMUI_PAUSE = 2;
    // These strings control the current filter for themes
    public static String metadataName = "Substratum_Name";
    public static String metadataAuthor = "Substratum_Author";
    public static String metadataLegacy = "Substratum_Legacy";
    // These strings control the nav drawer filter for ThemeFragment
    public static String homeFragment = "";
    public static String overlaysFragment = "overlays";
    public static String bootAnimationsFragment = "bootanimation";
    public static String fontsFragment = "fonts";
    public static String soundsFragment = "audio";
    public static String wallpaperFragment = "wallpapers";
    // These strings control the showcase metadata parsing
    public static String paidTheme = "paid";
    public static String showcaseFonts = "fonts";
    public static String showcaseWallpapers = "wallpapers";
    public static String showcaseBootanimations = "bootanimations";
    public static String showcaseOverlays = "overlays";
    public static String showcaseSounds = "sounds";
    // This int controls the default priority level for legacy overlays
    public static int DEFAULT_PRIORITY = 50;
    // These strings control package names for system apps
    public static String settingsPackageName = "com.android.settings";
    public static String settingsSubstratumDrawableName = "ic_settings_substratum";
    private static String metadataWallpapers = "Substratum_Wallpapers";
    private static String metadataVersion = "Substratum_Plugin";
    private static String metadataThemeReady = "Substratum_ThemeReady";
    private static String resourceChangelog = "ThemeChangelog";

    public static int getDeviceEncryptionStatus(Context context) {
        // 0: ENCRYPTION_STATUS_UNSUPPORTED
        // 1: ENCRYPTION_STATUS_INACTIVE
        // 2: ENCRYPTION_STATUS_ACTIVATING
        // 3: ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY
        // 4: ENCRYPTION_STATUS_ACTIVE
        // 5: ENCRYPTION_STATUS_ACTIVE_PER_USER
        int status = DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED;
        final DevicePolicyManager dpm = (DevicePolicyManager)
                context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) {
            status = dpm.getStorageEncryptionStatus();
        }
        return status;
    }

    // This method is used to place the Substratum Rescue archives if they are not present
    public static void injectRescueArchives(Context context) {
        File storageDirectory = new File(Environment
                .getExternalStorageDirectory(),
                "/substratum/");
        if (!storageDirectory.exists()) {
            Boolean made = storageDirectory.mkdirs();
            if (!made) Log.e(References.SUBSTRATUM_LOG,
                    "Unable to create storage directory");
        }
        File rescueFile = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() +
                        File.separator + "substratum" +
                        File.separator + "SubstratumRescue.zip");
        File rescueFileLegacy = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() +
                        File.separator + "substratum" +
                        File.separator + "SubstratumRescue_Legacy.zip");
        if (!rescueFile.exists()) {
            copyRescueFile(context, "rescue.dat",
                    Environment.getExternalStorageDirectory().getAbsolutePath() +
                            java.io.File.separator + "substratum" +
                            java.io.File.separator + "SubstratumRescue.zip");
        }
        if (!rescueFileLegacy.exists()) {
            copyRescueFile(context, "rescue_legacy.dat",
                    Environment.getExternalStorageDirectory().getAbsolutePath() +
                            java.io.File.separator + "substratum" +
                            java.io.File.separator +
                            "SubstratumRescue_Legacy.zip");
        }
    }

    private static boolean copyRescueFile(Context context, String sourceFileName, String
            destFileName) {
        AssetManager assetManager = context.getAssets();

        File destFile = new File(destFileName);
        File destParentDir = destFile.getParentFile();
        if (!destParentDir.exists()) {
            Boolean made = destParentDir.mkdir();
            if (!made) Log.e(References.SUBSTRATUM_LOG,
                    "Unable to create directories for rescue archive dumps.");
        }

        InputStream in;
        OutputStream out;
        try {
            in = assetManager.open(sourceFileName);
            out = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // This method is used to check the version of the masquerade theme system
    public static int checkMasquerade(Context context) {
        try {
            PackageInfo pInfo =
                    context.getPackageManager().getPackageInfo(MASQUERADE_PACKAGE, 0);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // Suppress warning
        }
        return 0;
    }

    // This method is used to force the application to use English
    public static boolean forceEnglishLocale(Context context) {
        // Please only use getApplicationContext on context, or else it would not change correctly
        try {
            Resources res = context.getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            android.content.res.Configuration conf = res.getConfiguration();
            conf.locale = new Locale(Locale.ENGLISH.getLanguage());
            res.updateConfiguration(conf, dm);
            return true;
        } catch (Exception e) {
            // Suppress warning
        }
        return false;
    }

    // This method restores the system language
    public static boolean forceSystemLocale(Context context) {
        // Please only use getApplicationContext on context, or else it would not change correctly
        try {
            Resources res = context.getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            android.content.res.Configuration conf = res.getConfiguration();
            conf.locale = new Locale(Locale.getDefault().getLanguage());
            res.updateConfiguration(conf, dm);
            return true;
        } catch (Exception e) {
            // Suppress warning
        }
        return false;
    }

    // This method is used to determine whether there the system is initiated with OMS
    public static Boolean checkOMS(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains("oms_state")) {
            setAndCheckOMS(context);
        }
        return prefs.getBoolean("oms_state", false);
    }

    public static void setAndCheckOMS(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove("oms_state").apply();

        try {
            Process p = Runtime.getRuntime().exec("cmd overlay");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            if (checkMasqueradeJobService(context) ||
                    reader.readLine().equals(
                            "The overlay manager has already been initialized.")) {
                prefs.edit().putBoolean("oms_state", true).apply();
                prefs.edit().putInt("oms_version", 7).apply();
                Log.d(References.SUBSTRATUM_LOG, "Initializing Substratum with the seventh " +
                        "iteration of the Overlay Manager Service...");
            } else {
                prefs.edit().putBoolean("oms_state", false).apply();
                prefs.edit().putInt("oms_version", 0).apply();
                Log.d(References.SUBSTRATUM_LOG, "Initializing Substratum with the second " +
                        "iteration of the Resource Runtime Overlay system...");
            }
        } catch (Exception e) {
            e.printStackTrace();
            prefs.edit().putBoolean("oms_state", false).apply();
            prefs.edit().putInt("oms_version", 0).apply();
            Log.d(References.SUBSTRATUM_LOG, "Initializing Substratum with the second " +
                    "iteration of the Resource Runtime Overlay system...");
        }
    }

    public static int checkOMSVersion(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains("oms_version")) {
            setAndCheckOMS(context);
        }
        return prefs.getInt("oms_version", 0);
    }

    // This method is used to obtain the device ID of the current device (set up)
    @SuppressLint("HardwareIds")
    public static String getDeviceID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    // This method is used to check whether a build.prop value is found
    public static String getProp(String propName) {
        Process p;
        String result = "";
        try {
            p = new ProcessBuilder("/system/bin/getprop", propName)
                    .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                result = line;
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    // This method clears the app's cache
    public static void clearAppCache(Context mContext) {
        FileOperations.delete(mContext, mContext.getCacheDir()
                .getAbsolutePath() + "/SubstratumBuilder/");
        Toast toast = Toast.makeText(mContext, mContext.getString(R.string
                        .char_success),
                Toast.LENGTH_SHORT);
        toast.show();
    }

    // Load SharedPreference defaults
    public static void loadDefaultConfig(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("first_run", true).apply();
        prefs.edit().putBoolean("show_app_icon", true).apply();
        if (References.getProp("ro.substratum.recreate").equals("true")) {
            prefs.edit().putBoolean("systemui_recreate", true).apply();
        } else {
            prefs.edit().putBoolean("systemui_recreate", false).apply();
        }
        prefs.edit().putBoolean("substratum_oms", References.checkOMS(context)).apply();
        prefs.edit().putBoolean("show_template_version", false).apply();
        prefs.edit().putBoolean("vibrate_on_compiled", false).apply();
        prefs.edit().putBoolean("nougat_style_cards", false).apply();
        prefs.edit().putString("compiler", "aapt").apply();
        new AOPTCheck().injectAOPT(context, true);
        prefs.edit().putBoolean("aopt_debug", false).apply();
        prefs.edit().putBoolean("force_english", false).apply();
        prefs.edit().putBoolean("floatui_show_android_system_overlays", false).apply();
        prefs = context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("is_updating", false).apply();
    }

    // This method configures the new devices and their configuration of their vendor folders
    public static Boolean inNexusFilter() {
        String[] nexus_filter = {"angler", "bullhead", "flounder", "dragon", "marlin", "sailfish"};
        return Arrays.asList(nexus_filter).contains(Build.DEVICE);
    }

    // This method checks whether there is any network available for Wallpapers
    public static boolean isNetworkAvailable(Context mContext) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // This method checks for offensive words
    public static boolean isOffensive(String toBeProcessed) {
        String[] offensive = {
                "shit",
                "fuck",
                "#uck_nicholas_chummy",
                "nicholas_chummy",
                "cracked"
        };
        for (String anOffensive : offensive) {
            if (toBeProcessed.toLowerCase().contains(anOffensive)) {
                return true;
            }
        }
        return false;
    }

    // This string array contains all the SystemUI acceptable overlay packs
    public static Boolean allowedSounds(String current) {
        String[] allowed_sounds = {
                "alarm.mp3",
                "alarm.ogg",
                "notification.mp3",
                "notification.ogg",
                "ringtone.mp3",
                "ringtone.ogg",
                "Effect_Tick.mp3",
                "Effect_Tick.ogg",
                "Lock.mp3",
                "Lock.ogg",
                "Unlock.mp3",
                "Unlock.ogg",
        };
        return Arrays.asList(allowed_sounds).contains(current);
    }

    // This string array contains all the SystemUI acceptable overlay packs
    public static Boolean allowedSystemUIOverlay(String current) {
        String[] allowed_overlays = {
                "com.android.systemui.headers",
                "com.android.systemui.navbars",
                "com.android.systemui.statusbars",
                "com.android.systemui.tiles"
        };
        return Arrays.asList(allowed_overlays).contains(current);
    }

    // This string array contains all the Settings acceptable overlay packs
    public static Boolean allowedSettingsOverlay(String current) {
        String[] allowed_overlays = {
                "com.android.settings.icons",
        };
        return Arrays.asList(allowed_overlays).contains(current);
    }

    // This string array contains all the SystemUI acceptable sound files
    static Boolean allowedUISound(String targetValue) {
        String[] allowed_themable = {
                "lock_sound",
                "unlock_sound",
                "low_battery_sound"};
        return Arrays.asList(allowed_themable).contains(targetValue);
    }

    // This string array contains all the legacy allowed folders
    public static Boolean allowedForLegacy(String targetValue) {
        String[] allowed_themable = {
                "overlays",
                "overlays_legacy",
                "sounds"};
        return Arrays.asList(allowed_themable).contains(targetValue);
    }

    // This string array contains all the legacy allowed folders
    public static Boolean checkIconPackNotAllowed(String targetValue) {
        String[] not_allowed_themable = {
                "com.keramidas.TitaniumBackup"};
        return Arrays.asList(not_allowed_themable).contains(targetValue);
    }

    // This method determines whether a specified package is installed
    public static boolean isPackageInstalled(Context context, String package_name) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(package_name, 0);
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return ai.enabled;
        } catch (Exception e) {
            return false;
        }
    }

    // This method determines the installed directory of the overlay for legacy mode
    public static String getInstalledDirectory(Context context, String package_name) {
        PackageManager pm = context.getPackageManager();
        for (ApplicationInfo app : pm.getInstalledApplications(0)) {
            if (app.packageName.equals(package_name)) {
                // The way this works is that Android will traverse within the SYMLINK and not the
                // actual directory. e.g.:
                // rm -r /vendor/overlay/com.android.systemui.navbars.Mono.apk (ON NEXUS FILTER)
                return app.sourceDir;
            }
        }
        return null;
    }

    // Check if a service is running from our app
    public static boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    // This method validates the resources by their name in a specific package
    public static Boolean validateResource(Context mContext, String package_Name,
                                           String resource_name, String resource_type) {
        try {
            Context context = mContext.createPackageContext(package_Name, 0);
            Resources resources = context.getResources();
            int drawablePointer = resources.getIdentifier(
                    resource_name, // Drawable name explicitly defined
                    resource_type, // Declared icon is a drawable, indeed.
                    package_Name); // Icon pack package name
            return drawablePointer != 0;
        } catch (Exception e) {
            return false;
        }
    }

    // This method obtains the application icon for a specified package
    public static Drawable grabAppIcon(Context context, String package_name) {
        try {
            Drawable icon;
            if (References.allowedSystemUIOverlay(package_name)) {
                icon = context.getPackageManager().getApplicationIcon("com.android.systemui");
            } else {
                if (References.allowedSettingsOverlay(package_name)) {
                    icon = context.getPackageManager().getApplicationIcon("com.android.settings");
                } else {
                    icon = context.getPackageManager().getApplicationIcon(package_name);
                }
            }
            return icon;
        } catch (Exception e) {
            Log.e(References.SUBSTRATUM_LOG, "Could not grab the application icon for \"" +
                    package_name
                    + "\"");
        }
        return context.getDrawable(R.drawable.default_overlay_icon);
    }

    public static List<ResolveInfo> getIconPacks(Context context) {
        // Scavenge through the packages on the device with specific launcher metadata in
        // their manifest
        PackageManager packageManager = context.getPackageManager();
        return packageManager.queryIntentActivities(new Intent(NOVA_LAUNCHER),
                PackageManager.GET_META_DATA);
    }

    public static List<ResolveInfo> getThemes(Context context) {
        // Scavenge through the packages on the device with specific substratum metadata in
        // their manifest
        PackageManager packageManager = context.getPackageManager();
        return packageManager.queryIntentActivities(new Intent(SUBSTRATUM_THEME),
                PackageManager.GET_META_DATA);
    }

    @SuppressWarnings("unchecked")
    public static HashMap getIconState(Context mContext, @NonNull String packageName) {
        /*
          Returns a HashMap in a specific order, of which the key would be the activityName
          that is most likely a perfect match in what icon we want to be overlaying. A check should
          be made to ensure this specific activity is the one being overlaid.

          The object will be an ArrayList of icon directories where the icon occurs inside the
          to-be-themed target. For example "res/mipmap-xxxhdpi/ic_launcher.png".
         */
        try {
            ApplicationInfo ai =
                    mContext.getPackageManager().getApplicationInfo(packageName, 0);
            Process process = Runtime.getRuntime().exec("aopt d badging " + ai.sourceDir);

            DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            HashMap hashMap = new HashMap<>();
            ArrayList<String> iconArray = new ArrayList();
            Boolean has_passed_icons = false;

            ArrayList<String> lines = new ArrayList<>();
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("application-icon")) {
                    String appIcon = lines.get(i).split(":")[1];
                    appIcon = appIcon.replace("'", "");
                    appIcon = appIcon.replace("-v4", "");
                    if (!iconArray.contains(appIcon)) {
                        // Do not contain duplicates in AOPT report, such as 65534-65535
                        iconArray.add(appIcon);
                        has_passed_icons = true;
                    }
                } else if (lines.get(i).startsWith("launchable-activity") && !has_passed_icons) {
                    String appIcon = lines.get(i);
                    appIcon = appIcon.substring(appIcon.lastIndexOf("=") + 1);
                    appIcon = appIcon.replace("'", ""); // Strip the quotes
                    appIcon = appIcon.replace("-v4", ""); // Make it to a non-API dependency
                    if (!iconArray.contains(appIcon)) {
                        iconArray.add(appIcon);
                        has_passed_icons = true;
                    }
                }
            }
            if (has_passed_icons) {
                hashMap.put(packageName, iconArray);
                // Once we reach this point, we have concluded the map assignation
                return hashMap;
            }
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static String getPackageIconName(Context mContext, @NonNull String packageName) {
        /*
          Returns the name of the icon in the package

          The object will be an ArrayList of icon directories where the icon occurs inside the
          to-be-themed target. For example "res/mipmap-xxxhdpi/ic_launcher.png".
         */
        try {
            ApplicationInfo ai =
                    mContext.getPackageManager().getApplicationInfo(packageName, 0);
            Process process = Runtime.getRuntime().exec("aopt d badging " + ai.sourceDir);

            DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            String s;
            while ((s = reader.readLine()) != null) {
                if (s.contains("application-icon")) {
                    String appIcon = s.split(":")[1];
                    appIcon = appIcon.substring(1, appIcon.length() - 1).replace("-v4", "");
                    return appIcon.split("/")[2].substring(0, appIcon.split("/")[2].length() - 4);
                }
            }
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            process.waitFor();
        } catch (Exception e) {
            // At this point we could simply show that there is no app icon in the package
            // e.g. DocumentsUI
        }
        return "ic_launcher";
    }

    // PackageName Crawling Methods
    public static String grabThemeVersion(Context mContext, String package_name) {
        try {
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(package_name, 0);
            return pInfo.versionName + " (" + pInfo.versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            //
        }
        return null;
    }

    public static String grabThemeAPIs(Context mContext, String package_name) {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                try {
                    if (appInfo.minSdkVersion == appInfo.targetSdkVersion) {
                        int target = appInfo.targetSdkVersion;
                        if (target == 23) {
                            return mContext.getString(R.string.api_23);
                        } else if (target == 24) {
                            return mContext.getString(R.string.api_24);
                        } else if (target == 25) {
                            return mContext.getString(R.string.api_25);
                        } else if (target == 26) {
                            return mContext.getString(R.string.api_26);
                        }
                    } else {
                        String minSdk = "";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            int min = appInfo.minSdkVersion;
                            if (min == 21) {
                                minSdk = mContext.getString(R.string.api_21);
                            } else if (min == 22) {
                                minSdk = mContext.getString(R.string.api_22);
                            } else if (min == 23) {
                                minSdk = mContext.getString(R.string.api_23);
                            } else if (min == 24) {
                                minSdk = mContext.getString(R.string.api_24);
                            } else if (min == 25) {
                                minSdk = mContext.getString(R.string.api_25);
                            } else if (min == 26) {
                                minSdk = mContext.getString(R.string.api_26);
                            }
                        } else {
                            // At this point, it is under API24 (API warning) thus we'll do an
                            // educated guess here.
                            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                                minSdk = mContext.getString(R.string.api_21);
                            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1) {
                                minSdk = mContext.getString(R.string.api_22);
                            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
                                minSdk = mContext.getString(R.string.api_23);
                            }
                        }
                        String targetSdk = "";
                        int target = appInfo.targetSdkVersion;
                        if (target == 23) {
                            targetSdk = mContext.getString(R.string.api_23);
                        } else if (target == 24) {
                            targetSdk = mContext.getString(R.string.api_24);
                        } else if (target == 25) {
                            targetSdk = mContext.getString(R.string.api_25);
                        } else if (target == 26) {
                            targetSdk = mContext.getString(R.string.api_26);
                        }
                        return minSdk + " - " + targetSdk;
                    }
                } catch (NoSuchFieldError noSuchFieldError) {
                    // The device is API 23 if it throws a NoSuchFieldError
                    if (appInfo.targetSdkVersion == 23) {
                        return mContext.getString(R.string.api_23);
                    } else {
                        String targetAPI = "";
                        int target = appInfo.targetSdkVersion;
                        if (target == 24) {
                            targetAPI = mContext.getString(R.string.api_24);
                        } else if (target == 25) {
                            targetAPI = mContext.getString(R.string.api_25);
                        } else if (target == 26) {
                            targetAPI = mContext.getString(R.string.api_26);
                        }
                        return mContext.getString(R.string.api_23) + " - " + targetAPI;
                    }
                }
            }
        } catch (Exception e) {
            //
        }
        return null;
    }

    // Grab Theme Ready Metadata
    public static String grabThemeReadyVisibility(Context mContext, String package_name) {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString(References.metadataThemeReady) != null) {
                    return appInfo.metaData.getString(References.metadataThemeReady);
                }
            }
        } catch (Exception e) {
            //
        }
        return null;
    }

    // Grab Theme Changelog
    public static String[] grabThemeChangelog(Context mContext, String package_name) {
        try {
            Resources res = mContext.getPackageManager()
                    .getResourcesForApplication(package_name);
            int array_position = res.getIdentifier(package_name + ":array/" +
                    resourceChangelog, "array", package_name);
            return res.getStringArray(array_position);
        } catch (Exception e) {
            // Suppress warnings
        }
        return null;
    }

    // Grab Theme Author
    public static String grabThemeAuthor(Context mContext, String package_name) {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                    return appInfo.metaData.getString(References.metadataAuthor);
                }
            }
        } catch (Exception e) {
            //
        }
        return null;
    }

    // Grab Theme Plugin Metadata
    public static String grabPackageTemplateVersion(Context mContext, String package_name) {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.get(References.metadataVersion) != null) {
                    return mContext.getString(R.string.plugin_template) + ": " + appInfo.metaData
                            .get(References.metadataVersion);
                }
            }
        } catch (Exception e) {
            //
        }
        return null;
    }

    // Grab Theme Hero Image
    public static Drawable grabPackageHeroImage(Context mContext, String package_name) {
        Resources res;
        Drawable hero = mContext.getDrawable(android.R.color.black); // Initialize to be black
        try {
            res = mContext.getPackageManager().getResourcesForApplication(package_name);
            int resourceId = res.getIdentifier(package_name + ":drawable/heroimage", null, null);
            if (0 != resourceId) {
                hero = mContext.getPackageManager().getDrawable(package_name, resourceId, null);
            }
            return hero;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hero;
    }

    // Grab Overlay Target Package Name (Human Readable)
    public static String grabPackageName(Context mContext, String package_name) {
        final PackageManager pm = mContext.getPackageManager();
        ApplicationInfo ai;
        try {
            switch (package_name) {
                case "com.android.systemui.navbars":
                    return mContext.getString(R.string.systemui_navigation);
                case "com.android.systemui.headers":
                    return mContext.getString(R.string.systemui_headers);
                case "com.android.systemui.tiles":
                    return mContext.getString(R.string.systemui_qs_tiles);
                case "com.android.systemui.statusbars":
                    return mContext.getString(R.string.systemui_statusbar);
                case "com.android.settings.icons":
                    return mContext.getString(R.string.settings_icons);
            }
            ai = pm.getApplicationInfo(package_name, 0);
        } catch (Exception e) {
            ai = null;
        }
        return (String) (ai != null ? pm.getApplicationLabel(ai) : null);
    }

    // Grab Overlay Parent
    public static String grabOverlayParent(Context mContext, String package_name) {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString("Substratum_Parent") != null) {
                    return appInfo.metaData.getString("Substratum_Parent");
                }
            }
        } catch (Exception e) {
            //
        }
        return null;
    }

    // Grab Overlay Target
    public static String grabOverlayTarget(Context mContext, String package_name) {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString("Substratum_Target") != null) {
                    return appInfo.metaData.getString("Substratum_Target");
                }
            }
        } catch (Exception e) {
            //
        }
        return null;
    }

    // Grab IconPack Parent
    public static Boolean grabIconPack(Context mContext, String package_name,
                                       String expectedPackName) {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                String current = appInfo.metaData.getString("Substratum_IconPack");
                if (current != null) return current.equals(expectedPackName);
            }
        } catch (Exception e) {
            //
        }
        return false;
    }

    public static Boolean spreadYourWingsAndFly(Context context) {
        String[] checker = checkPackageSupport();
        for (String check : checker) {
            if (References.isPackageInstalled(context, check)) {
                return true;
            }
        }
        return false;
    }

    // Check for the denied packages if existing on the device
    private static String[] checkPackageSupport() {
        return new String[]{
                "com.android.vending.billing.InAppBillingService.LOCK",
                "com.android.vending.billing.InAppBillingService.LACK",
                "uret.jasi2169.patcher"
        };
    }

    // Check if notification is visible for the user
    public static boolean isNotificationVisible(Context mContext, int notification_id) {
        NotificationManager mNotificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] notifications =
                mNotificationManager.getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == notification_id) {
                return true;
            }
        }
        return false;
    }

    // Clear all of the current notifications of Substratum when a cache clear is called
    public static void clearAllNotifications(Context mContext) {
        NotificationManager mNotificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] notifications =
                mNotificationManager.getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            if (Objects.equals(notification.getPackageName(), mContext.getPackageName())) {
                mNotificationManager.cancel(notification.getId());
            }
        }
    }

    // Check usage permissions
    public static boolean checkUsagePermissions(Context mContext) {
        try {
            PackageManager packageManager = mContext.getPackageManager();
            ApplicationInfo applicationInfo =
                    packageManager.getApplicationInfo(mContext.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager)
                    mContext.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid,
                    applicationInfo.packageName);
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // Locate the proper launch intent for the themes
    public static Intent sendLaunchIntent(Context mContext, String currentTheme,
                                          boolean theme_legacy, String theme_mode,
                                          Boolean notification) {
        Intent originalIntent = new Intent(Intent.ACTION_MAIN);
        if (theme_legacy)
            originalIntent.putExtra("theme_legacy", true);
        if (theme_mode != null) {
            originalIntent.putExtra("theme_mode", theme_mode);
        }
        if (notification) {
            originalIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            originalIntent.putExtra("refresh_mode", true);
        }
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo info = pm.getPackageInfo(currentTheme, PackageManager.GET_ACTIVITIES);
            ActivityInfo[] list = info.activities;
            for (ActivityInfo aList : list) {
                // We need to look for what the themer assigned the class to be! This is a dynamic
                // function that only launches the correct SubstratumLauncher class. Having it
                // hardcoded is bad.
                if (aList.name.equals(currentTheme + ".SubstratumLauncher")) {
                    originalIntent.setComponent(
                            new ComponentName(currentTheme, currentTheme + ".SubstratumLauncher"));
                    return originalIntent;
                } else if (aList.name.equals("substratum.theme.template.SubstratumLauncher")) {
                    originalIntent.setComponent(
                            new ComponentName(
                                    currentTheme, "substratum.theme.template.SubstratumLauncher"));
                    return originalIntent;
                }
            }
        } catch (Exception e) {
            // Suppress warning
        }
        return null;
    }

    // Launch intent for a theme
    public static boolean launchTheme(Context mContext, String package_name, String theme_mode,
                                      Boolean notification) {
        Intent initializer = sendLaunchIntent(mContext, package_name,
                !References.checkOMS(mContext), theme_mode, notification);
        String integrityCheck = new AOPTCheck().checkAOPTIntegrity(mContext);
        if (integrityCheck != null &&
                (integrityCheck.equals(mContext.getString(R.string.aapt_version)) ||
                        integrityCheck.equals(mContext.getString(R.string.aopt_version)))) {
            if (initializer != null) {
                mContext.startActivity(initializer);
            }
        } else {
            // At this point, AOPT is not found and must be injected in!
            Log.e(SUBSTRATUM_LOG, "Android Assets Packaging Tool was not found, " +
                    "trying to reinject...");

            new AOPTCheck().injectAOPT(mContext, true);

            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(mContext);
            prefs.edit().putString("compiler", "aapt").apply();
            if (initializer != null) {
                mContext.startActivity(initializer);
            }
        }
        return false;
    }

    // Begin check if device is running on the latest Masquerade
    public static Boolean checkMasqueradeJobService(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(MASQUERADE_PACKAGE, PackageManager.GET_SERVICES);
            ServiceInfo[] list = info.services;
            for (ServiceInfo aList : list) {
                if (aList.name.equals("masquerade.substratum.services.JobService")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Suppress warning
        }
        return false;
    }

    public static boolean isIncompatibleFirmware() {
        String currentPatch = References.getProp("ro.build.version.security_patch");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        try {
            Date date = format.parse(currentPatch);
            long currentPatchTimestamp = date.getTime();
            return currentPatchTimestamp > NOVEMBER_PATCH_TIMESTAMP &&
                    currentPatchTimestamp < JANUARY_PATCH_TIMESTAMP;
        } catch (ParseException pe) {
            pe.printStackTrace();
        }

        // Something bad happened. Aborting
        return false;
    }

    public static void uninstallPackage(Context context, String packageName) {
        if (checkMasqueradeJobService(context)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(packageName);
            MasqueradeService.uninstallOverlays(context, list, false);
        } else {
            new ElevatedCommands.ThreadRunner().execute("pm uninstall " + packageName);
        }
    }

    // This method checks whether these are legitimate packages for Substratum
    @SuppressWarnings("unchecked")
    public static HashMap<String, String[]> getSubstratumPackages(Context context,
                                                                  String package_name,
                                                                  HashMap packages,
                                                                  String home_type,
                                                                  Boolean old_algorithm,
                                                                  String search_filter) {
        // This algorithm was used during 490 and below and runs at a speed where the number of
        // overlay packages installed would affect the theme reload time. We are keeping this to
        // retain the old filter to show pre-6.0.0 themes.
        if (old_algorithm) try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            Context otherContext = context.createPackageContext(package_name, 0);
            AssetManager am = otherContext.getAssets();
            if (appInfo.metaData != null) {
                boolean can_continue = true;
                if (appInfo.metaData.getString(References.metadataName) != null &&
                        appInfo.metaData.getString(References.metadataAuthor) != null) {
                    if (search_filter != null && search_filter.length() > 0) {
                        String name = appInfo.metaData.getString(References.metadataName) + " " +
                                appInfo.metaData.getString(References.metadataAuthor);
                        can_continue = name.toLowerCase()
                                .contains(search_filter.toLowerCase());
                    }
                }
                if (!References.checkOMS(context)) {
                    if (!appInfo.metaData.getBoolean(References.metadataLegacy, true)) {
                        can_continue = false;
                    }
                }
                if (can_continue) {
                    if (appInfo.metaData.getString(References.metadataName) != null) {
                        if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                            if (home_type.equals("wallpapers")) {
                                if (appInfo.metaData.getString(References.metadataWallpapers)
                                        != null) {
                                    String[] data = {appInfo.metaData.getString
                                            (References.metadataAuthor), package_name};
                                    packages.put(appInfo.metaData.getString(
                                            References.metadataName), data);
                                }
                            } else if (home_type.length() == 0) {
                                String[] data = {appInfo.metaData.getString
                                        (References.metadataAuthor), package_name};
                                packages.put(appInfo.metaData.getString
                                        (References.metadataName), data);
                                Log.d("Substratum Ready Theme", package_name);
                            } else {
                                try {
                                    String[] stringArray = am.list("");
                                    if (Arrays.asList(stringArray).contains(home_type)) {
                                        String[] data = {appInfo.metaData.getString
                                                (References.metadataAuthor), package_name};
                                        packages.put(appInfo.metaData.getString
                                                (References.metadataName), data);
                                    }
                                } catch (Exception e) {
                                    Log.e(References.SUBSTRATUM_LOG,
                                            "Unable to find package identifier");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Exception
        }
        else {
            // This algorithm was used during 499 and above runs at a speed where the number of
            // overlay packages installed DOES NOT affect the theme reload time.
            try {
                List<ResolveInfo> listOfThemes = References.getThemes(context);
                for (ResolveInfo ri : listOfThemes) {
                    String packageName = ri.activityInfo.packageName;
                    ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                            packageName, PackageManager.GET_META_DATA);

                    Boolean can_continue = true;
                    if (appInfo.metaData.getString(References.metadataName) != null &&
                            appInfo.metaData.getString(References.metadataAuthor) != null) {
                        if (search_filter != null && search_filter.length() > 0) {
                            String name = appInfo.metaData.getString(References.metadataName) +
                                    " " + appInfo.metaData.getString(References.metadataAuthor);
                            if (!name.toLowerCase().contains(
                                    search_filter.toLowerCase())) {
                                can_continue = false;
                            }
                        }
                    }

                    if (can_continue) {
                        Context otherContext = context.createPackageContext(packageName, 0);
                        AssetManager am = otherContext.getAssets();
                        if (home_type.equals(References.wallpaperFragment)) {
                            if (appInfo.metaData.getString(References.metadataWallpapers) != null) {
                                String[] data = {appInfo.metaData.getString
                                        (References.metadataAuthor),
                                        packageName};
                                packages.put(appInfo.metaData.getString(
                                        References.metadataName), data);
                            }
                        } else {
                            if (home_type.length() == 0) {
                                String[] data = {appInfo.metaData.getString
                                        (References.metadataAuthor),
                                        packageName};
                                packages.put(appInfo.metaData.getString
                                        (References.metadataName), data);
                                Log.d("Substratum Ready Theme", packageName);
                            } else {
                                try {
                                    String[] stringArray = am.list("");
                                    if (Arrays.asList(stringArray).contains(home_type)) {
                                        String[] data = {appInfo.metaData.getString
                                                (References.metadataAuthor),
                                                packageName};
                                        packages.put(appInfo.metaData.getString
                                                (References.metadataName), data);
                                    }
                                } catch (Exception e) {
                                    Log.e(References.SUBSTRATUM_LOG,
                                            "Unable to find package identifier");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Exception
            }
        }
        return packages;
    }

    @SuppressWarnings("deprecation")
    public static String parseTime(Context context, int hour, int minute) {
        Locale locale;
        String parse;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            locale = context.getResources().getConfiguration().locale;
        }

        if (android.text.format.DateFormat.is24HourFormat(context)) {
            parse = String.format(locale, "%02d:%02d", hour, minute);
        } else {
            String AM_PM = hour <= 12 ? "AM" : "PM";
            hour = hour <= 12 ? hour : hour - 12;
            parse = String.format(locale, "%d:%02d " + AM_PM, hour, minute);
        }
        return parse;
    }

    // This class serves to update the theme's cache on demand
    public static class SubstratumThemeUpdate extends AsyncTask<Void, Integer, String> {

        private ProgressDialog progress;
        private String theme_name, theme_package, theme_mode;
        private Boolean launch;
        private Context mContext;

        public SubstratumThemeUpdate(Context mContext, String theme_package, String theme_name,
                                     String theme_mode) {
            this.mContext = mContext;
            this.theme_package = theme_package;
            this.theme_name = theme_name;
            this.theme_mode = theme_mode;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(mContext, R.style.AppTheme_DialogAlert);

            String parse = String.format(mContext.getString(R.string.on_demand_updating_text),
                    theme_name);

            progress.setTitle(mContext.getString(R.string.on_demand_updating_title));
            progress.setMessage(parse);
            progress.setIndeterminate(false);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();
            Toast toast = Toast.makeText(mContext, mContext.getString(R.string
                            .background_updated_toast),
                    Toast.LENGTH_SHORT);
            Toast toast2 = Toast.makeText(mContext, mContext.getString(R.string
                            .background_updated_toast_cancel),
                    Toast.LENGTH_SHORT);
            if (launch) {
                toast.show();
                // At this point, we can safely assume that the theme has successfully extracted
                launchTheme(mContext, theme_package, theme_mode, false);
            } else {
                toast2.show();
                // We don't want this cache anymore, delete it from the system completely
                new CacheCreator().wipeCache(mContext, theme_package);
            }
        }

        @Override
        protected String doInBackground(Void... Params) {
            launch = new CacheCreator().initializeCache(mContext, theme_package);
            return null;
        }
    }
}