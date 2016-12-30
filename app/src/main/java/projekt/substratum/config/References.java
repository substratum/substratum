package projekt.substratum.config;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import projekt.substrate.LetsGetStarted;
import projekt.substrate.ShowMeYourFierceEyes;
import projekt.substratum.BuildConfig;
import projekt.substratum.R;
import projekt.substratum.util.CacheCreator;
import projekt.substratum.util.Root;

public class References {

    // These are specific log tags for different classes
    public static final Boolean ENABLE_SIGNING = true;
    public static final Boolean ENABLE_AOPT_OUTPUT = false; // WARNING, DEVELOPERS - BREAKS COMPILE
    public static final String SUBSTRATUM_BUILDER = "SubstratumBuilder";
    public static final String SUBSTRATUM_LOG = "SubstratumLogger";
    public static final String SUBSTRATUM_ICON_BUILDER = "SubstratumIconBuilder";
    // Delays for Masquerade Icon Pack Handling
    public static final int MAIN_WINDOW_REFRESH_DELAY = 2000;
    public static final int FIRST_WINDOW_REFRESH_DELAY = 1000;
    public static final int SECOND_WINDOW_REFRESH_DELAY = 3000;
    private static final String SUBSTRATUM_THEME = "projekt.substratum.THEME";
    // This controls the package name for the specified launchers allowed for Studio
    private static final String NOVA_LAUNCHER = "com.novalauncher.THEME";
    // November security update (incompatible firmware) timestamp;
    private static final long NOVEMBER_PATCH_TIMESTAMP = 1478304000000L;
    // Lucky Patcher's Package Name
    public static String lp_package_identifier = "com.android.vending.billing" +
            ".InAppBillingService.LOCK";
    // This int controls the notification identifier
    public static int firebase_notification_id = 24862486;
    public static int notification_id = 2486;
    public static int notification_id_upgrade = 0;
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

    // This method is used to check the version of the masquerade theme system
    public static int checkMasquerade(Context context) {
        try {
            PackageInfo pInfo =
                    context.getPackageManager().getPackageInfo("masquerade.substratum", 0);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // Suppress warning
        }
        return 0;
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

        File om = new File("/system/bin/om");
        if (om.exists()) {
            prefs.edit().putBoolean("oms_state", true).apply();
            prefs.edit().putInt("oms_version", 3).apply();
            Log.d(References.SUBSTRATUM_LOG, "Initializing Substratum with the third iteration of" +
                    " " +
                    "the Overlay Manager Service...");
        } else {
            // At this point, we must perform an OMS7 check
            try {
                Process p = Runtime.getRuntime().exec("cmd overlay");
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                if (reader.readLine().equals(
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

    // These methods are now used to interact with the console to output the proper commands whether
    // it is being run on an OMS3 or an OMS7 device.
    public static String enableOverlay() {
        File om = new File("/system/bin/om");
        if (om.exists()) {
            return "om enable";
        } else {
            return "cmd overlay enable";
        }
    }

    public static String disableOverlay() {
        File om = new File("/system/bin/om");
        if (om.exists()) {
            return "om disable";
        } else {
            return "cmd overlay disable";
        }
    }

    public static String disableAllOverlays() {
        File om = new File("/system/bin/om");
        if (om.exists()) {
            return "om disable-all";
        } else {
            return "cmd overlay disable-all";
        }
    }

    public static String listAllOverlays() {
        File om = new File("/system/bin/om");
        if (om.exists()) {
            return "om list";
        } else {
            return "cmd overlay list";
        }
    }

    public static String refreshWindows() {
        File om = new File("/system/bin/om");
        if (om.exists()) {
            return "om refresh";
        } else {
            return "cmd overlay refresh";
        }
    }

    public static String setPriority() {
        File om = new File("/system/bin/om");
        if (om.exists()) {
            return "om set-priority";
        } else {
            return "cmd overlay set-priority";
        }
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
        if (Root.requestRootAccess()) {
            Root.runCommand("rm -rf " + mContext.getCacheDir().getAbsolutePath());
            Toast toast = Toast.makeText(mContext, mContext.getString(R.string
                            .char_success),
                    Toast.LENGTH_SHORT);
            toast.show();
        } else {
            Toast toast = Toast.makeText(mContext, mContext.getString(R.string
                            .char_error),
                    Toast.LENGTH_SHORT);
            toast.show();
        }
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
        prefs.edit().putBoolean("display_old_themes", true).apply();
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
    public static Boolean allowedUISound(String targetValue) {
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
        String[] checker = ShowMeYourFierceEyes.withSomeMascaraOn();
        for (String check : checker) {
            if (References.isPackageInstalled(context, check)) {
                return true;
            }
        }
        return false;
    }

    // Launch intent for a theme
    public static boolean launchTheme(Context mContext, String package_name, String theme_mode,
                                      Boolean notification) {
        Intent initializer = LetsGetStarted.initialize(mContext, package_name,
                !References.checkOMS(mContext), theme_mode, notification);
        if (initializer != null) {
            mContext.startActivity(initializer);
        }
        return false;
    }

    // Save data to Firebase
    public static void backupDebuggableStatistics(Context mContext, String tag, String data,
                                                  String reason) {
        try {
            FirebaseDatabase mDatabaseInstance = FirebaseDatabase.getInstance();
            DatabaseReference mDatabase = mDatabaseInstance.getReference(tag);
            String currentTimeAndDate = java.text.DateFormat.getDateTimeInstance().format(
                    Calendar.getInstance().getTime());
            Account[] accounts = AccountManager.get(mContext).getAccountsByType("com.google");
            String main_acc = "null";
            for (Account account : accounts) {
                if (account.name != null) {
                    main_acc = account.name.replace(".", "(dot)");
                }
            }
            String entryId = main_acc;
            String userId = FirebaseInstanceId.getInstance().getToken();
            DeviceCollection user = new DeviceCollection(
                    currentTimeAndDate,
                    userId,
                    Long.parseLong(data),
                    reason,
                    BuildConfig.VERSION_CODE,
                    BuildConfig.VERSION_NAME);
            mDatabase.child(entryId).child(data).setValue(user);
        } catch (RuntimeException re) {
            // Suppress Warning
        }
    }

    // These methods allow for a more secure method to mount RW and mount RO
    private static String checkMountCMD() {
        Process process = null;
        try {
            Runtime rt = Runtime.getRuntime();
            process = rt.exec(new String[]{"readlink", "/system/bin/mount"});
            try (BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(process.getInputStream()))) {
                return stdInput.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return null;
    }

    // Begin consolidation of Root commands in Substratum
    public static void adjustContentProvider(final String uri,
                                             final String topic, final String fileName) {
        Root.runCommand("content insert --uri " + uri + " " +
                "--bind name:s:" + topic + " --bind value:s:" + fileName);
    }

    public static void copy(final String source, final String destination) {
        Root.runCommand("cp -f " + source + " " + destination);
    }

    public static void copyDir(final String source, final String destination) {
        Root.runCommand("cp -rf " + source + " " + destination);
    }

    public static void createNewFolder(final String foldername) {
        Root.runCommand("mkdir " + foldername);
    }

    public static void delete(final String directory) {
        Root.runCommand("rm -rf " + directory);
    }

    public static void move(final String source, final String destination) {
        Root.runCommand("mv -f " + source + " " + destination);
    }

    public static void mountRW() {
        String mountCMD = checkMountCMD();
        if (mountCMD != null) {
            if (mountCMD.equals("toybox")) {
                Root.runCommand("mount -o rw,remount /system");
            } else if (mountCMD.equals("toolbox")) {
                Root.runCommand("mount -o remount,rw /system");
            }
        }
    }

    public static void mountRWData() {
        String mountCMD = checkMountCMD();
        if (mountCMD != null) {
            if (mountCMD.equals("toybox")) {
                Root.runCommand("mount -o rw,remount /data");
            } else if (mountCMD.equals("toolbox")) {
                Root.runCommand("mount -o remount,rw /data");
            }
        }
    }

    public static void mountRWVendor() {
        String mountCMD = checkMountCMD();
        if (mountCMD != null) {
            if (mountCMD.equals("toybox")) {
                Root.runCommand("mount -o rw,remount /vendor");
            } else if (mountCMD.equals("toolbox")) {
                Root.runCommand("mount -o remount,rw /vendor");
            }
        }
    }

    public static void mountRO() {
        String mountCMD = checkMountCMD();
        if (mountCMD != null) {
            if (mountCMD.equals("toybox")) {
                Root.runCommand("mount -o ro,remount /system");
            } else if (mountCMD.equals("toolbox")) {
                Root.runCommand("mount -o remount,ro /system");
            }
        }
    }

    public static void mountROData() {
        String mountCMD = checkMountCMD();
        if (mountCMD != null) {
            if (mountCMD.equals("toybox")) {
                Root.runCommand("mount -o ro,remount /data");
            } else if (mountCMD.equals("toolbox")) {
                Root.runCommand("mount -o remount,ro /data");
            }
        }
    }

    public static void mountROVendor() {
        String mountCMD = checkMountCMD();
        if (mountCMD != null) {
            if (mountCMD.equals("toybox")) {
                Root.runCommand("mount -o ro,remount /vendor");
            } else if (mountCMD.equals("toolbox")) {
                Root.runCommand("mount -o remount,ro /vendor");
            }
        }
    }

    public static void grantPermission(final String packager, final String permission) {
        Root.runCommand("pm grant " + packager + " " + permission);
    }

    public static void installOverlay(final String overlay) {
        Root.runCommand("pm install -r " + overlay);
    }

    public static void runCommands(final String commands) {
        Root.runCommand(commands);
    }

    public static void reboot() {
        Root.runCommand("reboot");
    }

    public static void refreshWindow() {
        // This is a deprecated call for OMS3 only. Do not call this from OMS7
        Root.runCommand(References.refreshWindows());
    }

    public static void restartMasquerade() {
        Root.runCommand("pkill -f masquerade.substratum");
    }

    public static void restartSystemUI() {
        Root.runCommand("pkill -f com.android.systemui");
    }

    public static void setContext(final String foldername) {
        Root.runCommand("chcon -R u:object_r:system_file:s0 " + foldername);
    }

    public static void setPermissions(final int permission, final String foldername) {
        Root.runCommand("chmod " + permission + " " + foldername);
    }

    public static void setPermissionsRecursively(final int permission, final String foldername) {
        Root.runCommand("chmod -R " + permission + " " + foldername);
    }

    public static void setProp(final String propName, final String propValue) {
        Root.runCommand("setprop " + propName + " " + propValue);
    }

    public static void softReboot() {
        Root.runCommand("pkill -f zygote");
    }

    public static void symlink(final String source, final String destination) {
        Root.runCommand("ln -s " + source + " " + destination);
    }

    public static void uninstallOverlay(final String overlay) {
        Root.runCommand("pm uninstall " + overlay);
    }

    public static boolean isIncompatibleFirmware() {

        String currentPatch = References.getProp("ro.build.version.security_patch");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        try {
            Date date = format.parse(currentPatch);
            long currentPatchTimestamp = date.getTime();

            return currentPatchTimestamp > NOVEMBER_PATCH_TIMESTAMP;
        } catch (ParseException pe) {
            pe.printStackTrace();
        }

        // Something bad happened. Aborting
        return false;
    }

    // This method checks whether these are legitimate packages for Substratum
    @SuppressWarnings("unchecked")
    public static HashMap<String, String[]> getSubstratumPackages(Context context,
                                                                  String package_name,
                                                                  HashMap packages,
                                                                  String home_type,
                                                                  Boolean old_algorithm) {
        if (old_algorithm) {
            // This algorithm was used during 490 and below and runs at a speed where the number of
            // overlay packages installed would affect the theme reload time. We are keeping this to
            // retain the old filter to show pre-6.0.0 themes.
            try {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        package_name, PackageManager.GET_META_DATA);
                Context otherContext = context.createPackageContext(package_name, 0);
                AssetManager am = otherContext.getAssets();
                if (appInfo.metaData != null) {
                    boolean can_continue = true;
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
        } else {
            // This algorithm was used during 499 and above runs at a speed where the number of
            // overlay packages installed DOES NOT affect the theme reload time.
            try {
                List<ResolveInfo> listOfThemes = References.getThemes(context);
                for (ResolveInfo ri : listOfThemes) {
                    String packageName = ri.activityInfo.packageName;
                    ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                            packageName, PackageManager.GET_META_DATA);
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
            } catch (Exception e) {
                // Exception
            }
        }
        return packages;
    }

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

    @IgnoreExtraProperties
    @SuppressWarnings("WeakerAccess")
    /*
      Firebase statistics report
     */
    private static class DeviceCollection {

        public String CurrentTime;
        public String FireBaseID;
        public long ID;
        public String Reason;
        public int VersionCode;
        public String VersionName;

        public DeviceCollection(String CurrentTime, String FireBaseID, long ID, String Reason,
                                int VersionCode, String VersionName) {
            this.CurrentTime = CurrentTime;
            this.FireBaseID = FireBaseID;
            this.ID = ID;
            this.Reason = Reason;
            this.VersionCode = VersionCode;
            this.VersionName = VersionName;
        }
    }

    public static class ThreadRunner extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... sUrl) {
            try {
                Root.runCommand(sUrl[0]);
            } catch (Exception e) {
                // Consume window refresh
            }
            return null;
        }
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