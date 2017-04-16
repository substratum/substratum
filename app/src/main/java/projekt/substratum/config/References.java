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
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import projekt.substratum.BuildConfig;
import projekt.substratum.R;
import projekt.substratum.services.AppCrashReceiver;
import projekt.substratum.services.PackageModificationDetector;
import projekt.substratum.services.ScheduledProfileReceiver;
import projekt.substratum.util.AOPTCheck;
import projekt.substratum.util.CacheCreator;
import projekt.substratum.util.ReadVariantPrioritizedColor;

public class References {

    // These are specific log tags for different classes
    public static final Boolean ENABLE_SIGNING = true;
    public static final Boolean ENABLE_ROOT_CHECK = true;
    public static final Boolean ENABLE_AOPT_OUTPUT = false; // WARNING, DEVELOPERS - BREAKS COMPILE
    public static final Boolean ENABLE_DIRECT_ASSETS_LOGGING = false;
    public static final Boolean BYPASS_ALL_VERSION_CHECKS = false; // For developer previews only!
    public static final Boolean BYPASS_SUBSTRATUM_BUILDER_DELETION = false;
    public static final String SUBSTRATUM_BUILDER = "SubstratumBuilder";
    public static final String SUBSTRATUM_LOG = "SubstratumLogger";
    public static final String SUBSTRATUM_ICON_BUILDER = "SubstratumIconBuilder";
    public static final String SUBSTRATUM_VALIDATOR = "SubstratumValidator";
    public static final String INTERFACER_PACKAGE = "projekt.interfacer";
    @Deprecated
    public static final String MASQUERADE_PACKAGE = "masquerade.substratum";
    // Showcase shuffler
    public static final int SHOWCASE_SHUFFLE_COUNT = 5;
    // Delays for Icon Pack Handling
    public static final int MAIN_WINDOW_REFRESH_DELAY = 2000;
    public static final int FIRST_WINDOW_REFRESH_DELAY = 1000;
    public static final int SECOND_WINDOW_REFRESH_DELAY = 3000;
    private static final String SUBSTRATUM_THEME = "projekt.substratum.THEME";
    // This controls the package name for the specified launchers allowed for Studio
    private static final String NOVA_LAUNCHER = "com.novalauncher.THEME";
    // November security update (incompatible firmware) timestamp;
    private static final long NOVEMBER_PATCH_TIMESTAMP = 1478304000000L;
    private static final long JANUARY_PATCH_TIMESTAMP = 1483549200000L;
    private static final String APP_CRASHED = "projekt.substratum.APP_CRASHED";
    private static final String PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";
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
    public static Boolean uncertified = null;
    private static String metadataWallpapers = "Substratum_Wallpapers";
    private static String metadataVersion = "Substratum_Plugin";
    private static String metadataThemeReady = "Substratum_ThemeReady";
    private static String resourceChangelog = "ThemeChangelog";
    private static int hashValue;
    private static ScheduledProfileReceiver scheduledProfileReceiver;
    private Context mContext; // Used for support checker

    public static void registerBroadcastReceivers(Context context) {
        try {
            IntentFilter intentAppCrashed = new IntentFilter(APP_CRASHED);
            IntentFilter intentPackageAdded = new IntentFilter(PACKAGE_ADDED);
            context.getApplicationContext().registerReceiver(
                    new AppCrashReceiver(), intentAppCrashed);
            context.getApplicationContext().registerReceiver(
                    new PackageModificationDetector(), intentPackageAdded);
            Log.d(SUBSTRATUM_LOG,
                    "Successfully registered broadcast receivers for Substratum functionality!");
        } catch (Exception e) {
            Log.e(SUBSTRATUM_LOG,
                    "Failed to register broadcast receivers for Substratum functionality...");
        }
    }

    public static void registerProfileScreenOffReceiver(Context context) {
        scheduledProfileReceiver = new ScheduledProfileReceiver();
        context.registerReceiver(scheduledProfileReceiver,
                new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    public static void unregisterProfileScreenOffReceiver(Context context) {
        try {
            context.unregisterReceiver(scheduledProfileReceiver);
        } catch (Exception e) {
            // Don't mind it.
        }
    }

    public static boolean isCachingEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("caching_enabled", false);
    }

    public static boolean checkOreo() {
        switch (Build.VERSION.RELEASE) {
            case "O":
                return true;
        }
        return false;
    }

    public static void createShortcut(Context context, String theme_pid, String theme_name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            Bitmap app_icon = ((BitmapDrawable)
                    References.grabAppIcon(context, theme_pid)).getBitmap();
            try {
                Intent myIntent = new Intent(Intent.ACTION_MAIN);
                myIntent.putExtra("theme_name", theme_name);
                myIntent.putExtra("theme_pid", theme_pid);
                myIntent.setComponent(
                        ComponentName.unflattenFromString(
                                "projekt.substratum/projekt.substratum.LaunchTheme"));
                myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);

                ShortcutInfo shortcut =
                        new ShortcutInfo.Builder(context, "favorite")
                                .setShortLabel(theme_name)
                                .setLongLabel(theme_name)
                                .setIcon(Icon.createWithBitmap(app_icon))
                                .setIntent(myIntent)
                                .build();
                shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
                Log.d(SUBSTRATUM_LOG, "Successfully added dynamic app shortcut!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void clearShortcut(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            shortcutManager.removeAllDynamicShortcuts();
            Log.d(SUBSTRATUM_LOG, "Successfully removed all dynamic app shortcuts!");
        }
    }

    public static String checkXposedVersion() {
        String xposed_version = "";

        File f = new File("/system/framework/XposedBridge.jar");
        if (f.exists() && !f.isDirectory()) {
            File file = new File("/system/", "xposed.prop");
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String unparsed_br = br.readLine();
                xposed_version = unparsed_br.substring(8, 10);
            } catch (FileNotFoundException e) {
                Log.e("XposedChecker", "'xposed.prop' could not be found!");
            } catch (IOException e) {
                Log.e("XposedChecker", "Unable to parse BufferedReader from 'xposed.prop'");
            }
            xposed_version = ", " + R.string.logcat_email_xposed_check + " (" +
                    xposed_version + ")";
        }
        return xposed_version;
    }

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

    // This method is used to determine whether there the system is initiated with OMS
    public static Boolean checkOMS(Context context) {
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

    public static void setAndCheckOMS(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove("oms_state").apply();

        try {
            Process p = Runtime.getRuntime().exec("cmd overlay");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            boolean usesInterfacer = checkThemeInterfacer(context);
            boolean usesOMS7old = reader.readLine() != null && reader.readLine().equals(
                    "The overlay manager has already been initialized.");
            if (usesInterfacer || usesOMS7old) {
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

    public static boolean checkSubstratumFeature(Context context) {
        // Using lowercase because that's how we defined it in our permissions xml
        return context.getPackageManager().hasSystemFeature(SUBSTRATUM_THEME.toLowerCase());
    }

    // This method is used to determine whether there the system was dirty flashed / upgraded
    public static Boolean checkROMVersion(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains("build_date")) {
            setROMVersion(context, false);
        }
        return prefs.getInt("build_date", 0) == Integer.parseInt(References
                .getProp("ro.build.date.utc"));
    }

    public static void setROMVersion(Context context, boolean force) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains("build_date") || force) {
            prefs.edit().putInt("build_date",
                    Integer.parseInt(References.getProp("ro.build.date.utc")))
                    .apply();
        }
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

    // This method is used to check whether a build.prop value is found
    public static StringBuilder getBuildProp() {
        Process p;
        StringBuilder result = new StringBuilder();
        try {
            p = new ProcessBuilder("/system/bin/getprop")
                    .redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line + "\n");
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    // Load SharedPreference defaults
    public static void loadDefaultConfig(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("first_run", true);
        editor.putBoolean("show_app_icon", true);
        editor.putBoolean("substratum_oms", References.checkOMS(context));
        editor.putBoolean("show_template_version", false);
        editor.putBoolean("vibrate_on_compiled", false);
        editor.putBoolean("nougat_style_cards", false);
        editor.putBoolean("aopt_debug", false);
        editor.putBoolean("theme_debug", false);
        editor.putBoolean("force_independence", false);
        editor.putBoolean("force_english", false);
        editor.putBoolean("floatui_show_android_system_overlays", false);
        editor.putBoolean("alphabetize_showcase", false);
        editor.putBoolean("complexion", true);
        editor.putString("compiler", "aapt");
        editor.putBoolean("crash_receiver", true);

        // Initial parse of what is installed on the device
        Set<String> installed_themes = new TreeSet<>();
        List<ResolveInfo> all_themes = References.getThemes(context);
        for (int i = 0; i < all_themes.size(); i++) {
            installed_themes.add(all_themes.get(i).activityInfo.packageName);
        }
        editor.putStringSet("installed_themes", installed_themes);

        editor.apply();
        editor = context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE).edit();
        editor.putBoolean("is_updating", false);
        editor.apply();
        new AOPTCheck().injectAOPT(context, true);
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
    public static boolean isOffensive(Context context, String toBeProcessed) {
        if (toBeProcessed == null) {
            return true;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (References.isNetworkAvailable(context)) {
            FirebaseAnalytics.withdrawNames(context);
        }

        if (prefs.contains("blacklisted_names")) {
            Set<String> pref = prefs.getStringSet("blacklisted_names", new HashSet<>());
            for (String check : pref) {
                if (toBeProcessed.toLowerCase().contains(check.toLowerCase())) {
                    Log.d("BlacklistedDatabase", "Loaded a theme containing blacklisted words.");
                    return true;
                }
            }
        }
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
        return isPackageInstalled(context, package_name, true);
    }

    // This method determines whether a specified package is installed (enabled OR disabled)
    public static boolean isPackageInstalled(Context context, String package_name, boolean
            enabled) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(package_name, 0);
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            if (enabled) {
                return ai.enabled;
            }
            // if package doesn't exist, an Exception will be thrown, so return true in every case
            return true;
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

    // This method obtains the overlay parent icon for specified package, returns self package icon
    // if not found
    public static Drawable grabOverlayParentIcon(Context context, String package_name) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString("Substratum_Parent") != null) {
                    return grabAppIcon(context, appInfo.metaData.getString("Substratum_Parent"));
                }
            }
        } catch (Exception e) {
            //
        }
        return grabAppIcon(context, package_name);
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
    public static String grabAppVersion(Context mContext, String package_name) {
        try {
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(package_name, 0);
            return pInfo.versionName;
        } catch (Exception e) {
            // Suppress warning
        }
        return null;
    }

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

    // Grab Color Resource
    public static int grabColorResource(Context mContext, String package_name, String colorName) {
        Resources res;
        try {
            res = mContext.getPackageManager().getResourcesForApplication(package_name);
            int resourceId = res.getIdentifier(package_name + ":color/" + colorName, null, null);
            if (0 != resourceId) {
                return res.getColor(resourceId, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
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

    public static int hashPassthrough(Context context) {
        if (hashValue != 0) {
            return hashValue;
        }
        try {
            Signature[] sigs = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_SIGNATURES).signatures;
            for (Signature sig : sigs) {
                if (sig != null) {
                    hashValue = sig.hashCode();
                    return hashValue;
                }
            }
        } catch (PackageManager.NameNotFoundException nnfe) {
            nnfe.printStackTrace();
        }
        return 0;
    }

    public static Boolean spreadYourWingsAndFly(Context context) {
        if (uncertified != null) {
            return uncertified;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (References.isNetworkAvailable(context)) {
            FirebaseAnalytics.withdrawBlacklistedPackages(context);
        }

        if (prefs.contains("blacklisted_packages")) {
            Set<String> pref = prefs.getStringSet("blacklisted_packages", new HashSet<>());
            for (String check : pref) {
                if (References.isPackageInstalled(context, check, false)) {
                    Log.d("PatcherDatabase",
                            "The database has triggered a primary level blacklist package.");
                    uncertified = true;
                    return true;
                } else if (getMetaData(context, check) || getProviders(context, check) ||
                        getIntents(context, check)) {
                    Log.d("PatcherDatabase",
                            "The database has triggered a secondary level blacklist package.");
                    uncertified = true;
                    return true;
                }
            }
        }
        String[] checker = checkPackageSupport();
        for (String check : checker) {
            if (References.isPackageInstalled(context, check, false)) {
                uncertified = true;
                return true;
            } else if (getMetaData(context, check) || getProviders(context, check) ||
                    getIntents(context, check)) {
                uncertified = true;
                return true;
            }
        }
        uncertified = false;
        return false;
    }

    // Check for the denied packages if existing on the device
    private static String[] checkPackageSupport() {
        return new String[]{
                "com.android.vending.billing.InAppBillingService.",
                "com.android.vending.billing.InAppBillingService.LOCK",
                "com.android.vending.billing.InAppBillingService.LACK",
                "uret.jasi2169.",
                "uret.jasi2169.patcher",
                "com.dimonvideo.luckypatcher",
                "com.chelpus.lackypatch",
                "com.forpda.lp",
                "com.android.vending.billing.InAppBillingService.LUCK"
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Intent initializer = sendLaunchIntent(mContext, package_name,
                !References.checkOMS(mContext), theme_mode, notification);
        if (initializer != null) {
            initializer.putExtra("hash_passthrough", hashPassthrough(mContext));
            initializer.putExtra("certified", !References.spreadYourWingsAndFly(mContext));
        }
        String integrityCheck = new AOPTCheck().checkAOPTIntegrity(mContext);
        if (integrityCheck != null &&
                (integrityCheck.equals(mContext.getString(R.string.aapt_version)) ||
                        integrityCheck.equals(mContext.getString(R.string.aopt_version)))) {
            // Check if AOPT is used with a release build, if yes then change it to aapt
            if (integrityCheck.equals(mContext.getString(R.string.aapt_version)) ||
                    !BuildConfig.DEBUG) {
                if (initializer != null) {
                    mContext.startActivity(initializer);
                }
            } else {
                prefs.edit().putString("compiler", "aapt").apply();
                new AOPTCheck().injectAOPT(mContext, true);
                if (initializer != null) {
                    mContext.startActivity(initializer);
                }
            }
        } else {
            // At this point, AOPT is not found and must be injected in!
            Log.e(SUBSTRATUM_LOG, "Android Assets Packaging Tool was not found, " +
                    "trying to reinject...");

            new AOPTCheck().injectAOPT(mContext, true);

            prefs.edit().putString("compiler", "aapt").apply();
            if (initializer != null) {
                mContext.startActivity(initializer);
            }
        }
        return false;
    }

    // Begin check if device is running on the latest theme interface
    public static boolean checkThemeInterfacer(Context context) {
        boolean forceIndependence = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("force_independence", false);
        if (!forceIndependence) {
            return getThemeInterfacerPackage(context) != null;
        }
        return false;
    }

    public static boolean isBinderInterfacer(Context context) {
        PackageInfo packageInfo = getThemeInterfacerPackage(context);
        return packageInfo != null && packageInfo.versionCode >= 60;
    }

    // Obtain a live sample of the metadata in an app
    private static boolean getMetaData(Context context, String trigger) {
        List<ApplicationInfo> list =
                context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).packageName.startsWith(trigger)) {
                return true;
            }
        }
        return false;
    }

    // Obtain a live sample of the content providers in an app
    private static boolean getProviders(Context context, String trigger) {
        List<PackageInfo> list =
                context.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).packageName.startsWith(trigger)) {
                return true;
            }
        }
        return false;
    }

    // Obtain a live sample of the intents in an app
    public static boolean getIntents(Context context, String trigger) {
        ArrayList<String> startupApps = new ArrayList<>();
        ArrayList<Intent> intentArray = new ArrayList<>();
        intentArray.add(new Intent(Intent.ACTION_BOOT_COMPLETED));
        intentArray.add(new Intent(Intent.ACTION_PACKAGE_ADDED));
        intentArray.add(new Intent(Intent.ACTION_PACKAGE_CHANGED));
        intentArray.add(new Intent(Intent.ACTION_PACKAGE_REPLACED));
        intentArray.add(new Intent(Intent.ACTION_PACKAGE_REMOVED));
        intentArray.add(new Intent(Intent.ACTION_MEDIA_SCANNER_FINISHED));
        intentArray.add(new Intent(Intent.ACTION_MEDIA_SCANNER_STARTED));
        intentArray.add(new Intent(Intent.ACTION_MEDIA_MOUNTED));
        intentArray.add(new Intent(Intent.ACTION_MEDIA_REMOVED));
        for (Intent intent : intentArray) {
            List<ResolveInfo> activities =
                    context.getPackageManager().queryBroadcastReceivers(intent, 0);
            for (ResolveInfo resolveInfo : activities) {
                ActivityInfo activityInfo = resolveInfo.activityInfo;
                if (activityInfo != null && activityInfo.name.startsWith(trigger)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static PackageInfo getThemeInterfacerPackage(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(INTERFACER_PACKAGE, 0);
        } catch (Exception e) {
            // Theme Interfacer was not installed
        }
        return null;
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

    public static boolean needsRecreate(Context context, ArrayList<String> list) {
        for (String o : list) {
            if (o.equals("android") || o.equals("projekt.substratum")) {
                return false;
            }
        }
        return References.checkOMS(context);
    }

    public static void uninstallPackage(Context context, String packageName) {
        if (checkThemeInterfacer(context)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(packageName);
            ThemeInterfacerService.uninstallOverlays(context, list, false);
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

    // This method parses a specific overlay resource file (.xml) and returns the specified value
    public static String getOverlayResource(InputStream overlay) {
        String hex = null;

        // Try to clone the InputStream (WARNING: Might be an ugly hek)
        InputStream clone1, clone2;
        try {
            byte[] byteArray = IOUtils.toByteArray(overlay);
            clone1 = new ByteArrayInputStream(byteArray);
            clone2 = new ByteArrayInputStream(byteArray);
        } catch (IOException e) {
            Log.e(SUBSTRATUM_LOG, "Unable to clone InputStream");
            return null;
        }

        // Find the name of the top most color in the file first.
        String resource_name = new ReadVariantPrioritizedColor(clone1).run();

        if (resource_name != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(clone2))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("\"" + resource_name + "\"")) {
                        String[] split = line.substring(line.lastIndexOf("\">") + 2).split("<");
                        hex = split[0];
                        if (hex.startsWith("?")) hex = "#00000000";
                    }
                }
            } catch (IOException ioe) {
                Log.e(SUBSTRATUM_LOG, "Unable to find " + resource_name + " in this overlay!");
            }
        }

        try {
            clone1.close();
            clone2.close();
        } catch (IOException e) {
            Log.e(SUBSTRATUM_LOG, "Failed to close InputStream");
        }
        return hex;
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