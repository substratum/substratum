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

package projekt.substratum.common;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import projekt.substratum.InformationActivity;
import projekt.substratum.LauncherActivity;
import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.activities.shortcuts.AppShortcutLaunch;
import projekt.substratum.common.analytics.FirebaseAnalytics;
import projekt.substratum.services.profiles.ScheduledProfileReceiver;
import projekt.substratum.util.injectors.BinaryInstaller;

import static android.content.Context.CLIPBOARD_SERVICE;
import static projekt.substratum.common.Internal.BYTE_ACCESS_RATE;

public enum References {
    ;

    public static final Boolean ENABLE_ROOT_CHECK = true; // Force the app to run without root
    public static final Boolean ENABLE_EXTRAS_DIALOG = false; // Show a dialog when applying extras
    public static final Boolean ENABLE_AAPT_OUTPUT = false; // WARNING, DEVELOPERS - BREAKS COMPILE
    public static final Boolean ENABLE_PACKAGE_LOGGING = false; // Show time/date/place of install
    public static final Boolean ENABLE_DIRECT_ASSETS_LOGGING = true; // Self explanatory
    public static final Boolean BYPASS_ALL_VERSION_CHECKS = false; // For developer previews only!
    public static final Boolean BYPASS_SUBSTRATUM_BUILDER_DELETION = false; // Do not delete cache?
    @SuppressWarnings("WeakerAccess")
    public static final Integer OVERLAY_UPDATE_RANGE = 815; // Overlays require updating since ver
    // These are specific log tags for different classes
    public static final String SUBSTRATUM_BUILDER = "SubstratumBuilder";
    public static final String SUBSTRATUM_LOG = "SubstratumLogger";
    public static final String SUBSTRATUM_VALIDATOR = "SubstratumValidator";
    // These are package names for our backend systems
    public static final String ANDROMEDA_PACKAGE = "projekt.andromeda";
    public static final String INTERFACER_PACKAGE = "projekt.interfacer";
    public static final String INTERFACER_SERVICE = INTERFACER_PACKAGE + ".services.JobService";
    public static final String SUBSTRATUM_PACKAGE = "projekt.substratum";
    // Samsung package names
    public static final String SST_ADDON_PACKAGE = "projekt.sungstratum";
    public static final String PLAY_STORE_PACKAGE_NAME = "com.android.vending";
    // Specific intent for receiving completion status on backend
    public static final String ANDROMEDA_BINDED = ANDROMEDA_PACKAGE + ".INITIALIZE";
    public static final String INTERFACER_BINDED = INTERFACER_PACKAGE + ".INITIALIZE";
    public static final String STATUS_CHANGED = INTERFACER_PACKAGE + ".STATUS_CHANGED";
    public static final String CRASH_PACKAGE_NAME = "projekt.substratum.EXTRA_PACKAGE_NAME";
    public static final String CRASH_REPEATING = "projekt.substratum.EXTRA_CRASH_REPEATING";
    // System intents to catch
    public static final String BOOT_COMPLETED = Intent.ACTION_BOOT_COMPLETED;
    public static final String PACKAGE_ADDED = Intent.ACTION_PACKAGE_ADDED;
    public static final String PACKAGE_FULLY_REMOVED = Intent.ACTION_PACKAGE_FULLY_REMOVED;
    // App intents to send
    public static final String ACTIVITY_FINISHER = "projekt.substratum.ACTIVITY_FINISHER";
    public static final String MANAGER_REFRESH = "projekt.substratum.MANAGER_REFRESH";
    public static final String KEY_RETRIEVAL = "Substratum.KeyRetrieval";
    public static final String SUBSTRATUM_THEME = "projekt.substratum.THEME";
    public static final String TEMPLATE_THEME_MODE = SUBSTRATUM_THEME;
    public static final String TEMPLATE_GET_KEYS = "projekt.substratum.GET_KEYS";
    public static final String TEMPLATE_RECEIVE_KEYS = "projekt.substratum.RECEIVE_KEYS";
    // Keep it simple, stupid!
    public static final int SHOWCASE_SHUFFLE_COUNT = 5;
    // These strings control the current filter for themes
    public static final String metadataName = "Substratum_Name";
    public static final String metadataAuthor = "Substratum_Author";
    public static final String metadataEmail = "Substratum_Email";
    public static final String metadataLegacy = "Substratum_Legacy";
    public static final String metadataEncryption = "Substratum_Encryption";
    public static final String metadataEncryptionValue = "onCompileVerify";
    public static final String metadataWallpapers = "Substratum_Wallpapers";
    public static final String metadataHeroOverride = "Substratum_HeroOverride";
    public static final String metadataOverlayDevice = "Substratum_Device";
    public static final String metadataOverlayParent = "Substratum_Parent";
    public static final String metadataOverlayTarget = "Substratum_Target";
    public static final String metadataOverlayType1a = "Substratum_Type1a";
    public static final String metadataOverlayType1b = "Substratum_Type1b";
    public static final String metadataOverlayType1c = "Substratum_Type1c";
    public static final String metadataOverlayType2 = "Substratum_Type2";
    public static final String metadataOverlayType3 = "Substratum_Type3";
    public static final String metadataOverlayType4 = "Substratum_Type4";
    public static final String metadataOverlayVersion = "Substratum_Version";
    public static final String metadataSamsungSupport = "Substratum_Samsung";
    public static final String metadataThemeReady = "Substratum_ThemeReady";
    public static final String resourceChangelog = "ThemeChangelog";
    // These are Samsung specific manifest values
    public static final Boolean toggleShowSamsungOverlayInSettings = false;
    public static final String permissionSamsungOverlay =
            "com.samsung.android.permission.SAMSUNG_OVERLAY_COMPONENT";
    // These strings control the nav drawer filter for ThemeFragment
    public static final String homeFragment = "";
    public static final String overlaysFragment = "overlays";
    public static final String bootAnimationsFragment = "bootanimation";
    public static final String shutdownAnimationsFragment = "shutdownanimation";
    public static final String fontsFragment = "fonts";
    public static final String soundsFragment = "audio";
    public static final String wallpaperFragment = "wallpapers";
    // These strings control the showcase metadata parsing
    public static final String paidTheme = "paid";
    public static final String showcaseFonts = "fonts";
    public static final String showcaseWallpapers = "wallpapers";
    public static final String showcaseBootanimations = "bootanimations";
    public static final String showcaseOverlays = "overlays";
    public static final String showcaseSounds = "sounds";
    // These strings control the directories that Substratum uses
    public static final String EXTERNAL_STORAGE_CACHE = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/.substratum/";
    public static final String LOGCHAR_DIR = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/substratum" + File.separator + "LogCharReports";
    public static final String SUBSTRATUM_BUILDER_CACHE = "/SubstratumBuilder/";
    // These strings control the legacy overlay location
    public static final String DATA_RESOURCE_DIR = "/data/resource-cache/";
    public static final String PIXEL_NEXUS_DIR = "/system/overlay/";
    public static final String LEGACY_NEXUS_DIR = "/system/vendor/overlay/";
    public static final String VENDOR_DIR = "/vendor/overlay/";
    // Notification Channel
    public static final String DEFAULT_NOTIFICATION_CHANNEL_ID = "default";
    public static final String ONGOING_NOTIFICATION_CHANNEL_ID = "ongoing";
    public static final String ANDROMEDA_NOTIFICATION_CHANNEL_ID = "andromeda";
    // Different theme systems
    public static final int OVERLAY_MANAGER_SERVICE_O_ANDROMEDA = 1089303;
    public static final int OVERLAY_MANAGER_SERVICE_O_UNROOTED = 13970147;
    public static final int OVERLAY_MANAGER_SERVICE_O_ROOTED = 1310794;
    public static final int OVERLAY_MANAGER_SERVICE_N_UNROOTED = 18723789;
    @SuppressWarnings("WeakerAccess")
    public static final int RUNTIME_RESOURCE_OVERLAY_N_ROOTED = 8282713;
    public static final int SAMSUNG_THEME_ENGINE_N = 2389284;
    public static final int NO_THEME_ENGINE = 0;
    // This int controls the notification identifier
    public static final int firebase_notification_id = 24862486;
    public static final int notification_id = 2486;
    public static final int notification_id_compiler = 17589715;
    // This int controls the delay for window refreshes to occur
    public static final int REFRESH_WINDOW_DELAY = 500;
    // This int controls the default grid count for the theme adapter
    public static final int MIN_GRID_COUNT = 1;
    public static final int DEFAULT_GRID_COUNT = 1;
    public static final int MAX_GRID_COUNT = 4;
    // This int controls the default priority level for legacy overlays
    public static final int DEFAULT_PRIORITY = 1004;
    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = 9999;
    // These strings control package names for system apps
    public static final String settingsPackageName = "com.android.settings";
    public static final String settingsSubstratumDrawableName = "ic_settings_substratum";
    public static final String SUBSTRATUM_LAUNCHER_CLASS = ".SubstratumLauncher";
    // Metadata used in theme templates to denote specific parts of a theme
    public static final String metadataVersion = "Substratum_Plugin";
    // Validate with logs
    public static final Boolean VALIDATE_WITH_LOGS = false;
    // This string controls the hero image name
    static final String heroImageResourceName = "heroimage";
    static final String heroImageGridResourceName = "heroimage_grid";
    static final String heroImageMainResourceName = "heroimage_banner";
    // Specific intents Substratum should be listening to
    static final String APP_CRASHED = "projekt.substratum.APP_CRASHED";
    // Localized variables shared amongst common resources
    static ScheduledProfileReceiver scheduledProfileReceiver;
    // These values control the dynamic certification of substratum
    private static Boolean uncertified;
    private static int hashValue;

    /**
     * Create a launcher icon/launchable intent
     *
     * @param context    Self explanatory, bro.
     * @param theme_pid  Theme's package name (launcher intent process)
     * @param theme_name Theme's name (launcher icon name)
     */
    public static void createLauncherIcon(Context context,
                                          String theme_pid,
                                          String theme_name) {
        createLauncherIcon(context, theme_pid, theme_name, false);
    }

    /**
     * Create a launcher icon/launchable intent
     *
     * @param context               Self explanatory, bro.
     * @param theme_pid             Theme's package name (launcher intent process)
     * @param theme_name            Theme's name (launcher icon name)
     * @param launchManagerFragment Boolean flag that creates an intent to launch
     *                              {@link projekt.substratum.fragments.ManagerFragment}
     * @return Returns an intent with the corresponding action
     */
    public static Intent createLauncherIcon(
            Context context,
            String theme_pid,
            String theme_name,
            boolean launchManagerFragment) {
        Intent myIntent = new Intent(Intent.ACTION_MAIN);
        if (!launchManagerFragment) {
            myIntent.putExtra("theme_pid", theme_pid);
            myIntent.setComponent(
                    ComponentName.unflattenFromString(
                            context.getPackageName() +
                                    '/' + AppShortcutLaunch.class.getName()));
        } else {
            myIntent.putExtra("launch_manager_fragment", true);
            myIntent.setComponent(ComponentName.unflattenFromString(
                    context.getPackageName() +
                            '/' + MainActivity.class.getName()));
        }
        myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (launchManagerFragment) return myIntent;

        Bitmap app_icon = Packages.getBitmapFromDrawable(Packages.getAppIcon(context,
                theme_pid));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, myIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, theme_name);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, app_icon);
            addIntent.putExtra("duplicate", false);
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            context.sendBroadcast(addIntent);
        } else {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            ShortcutInfo shortcut =
                    new ShortcutInfo.Builder(context, theme_name)
                            .setShortLabel(theme_name)
                            .setLongLabel(theme_name)
                            .setIcon(Icon.createWithBitmap(app_icon))
                            .setIntent(myIntent)
                            .build();
            if (shortcutManager != null) {
                shortcutManager.requestPinShortcut(shortcut, null);
            }
        }
        return myIntent;
    }

    /**
     * Create a launcher shortcut
     *
     * @param context    Context
     * @param theme_pid  Package name
     * @param theme_name Theme name
     */
    public static void createShortcut(Context context,
                                      String theme_pid,
                                      String theme_name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            Icon app_icon;
            Drawable app_icon_drawable = Packages.getAppIcon(context, theme_pid);
            //If we are on Oreo and the Theme uses an adaptiveIcon, we have to treat it properly
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    app_icon_drawable instanceof AdaptiveIconDrawable) {
                app_icon = Icon.createWithAdaptiveBitmap(Packages.getBitmapFromDrawable
                        (app_icon_drawable));
            } else {
                app_icon = Icon.createWithBitmap(Packages.getBitmapFromDrawable(app_icon_drawable));
            }
            try {
                Intent myIntent = new Intent(Intent.ACTION_MAIN);
                myIntent.putExtra("theme_name", theme_name);
                myIntent.putExtra("theme_pid", theme_pid);
                myIntent.setComponent(
                        ComponentName.unflattenFromString(
                                context.getPackageName() +
                                        '/' + AppShortcutLaunch.class.getName()));
                myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                ShortcutInfo shortcut =
                        new ShortcutInfo.Builder(context, "favorite")
                                .setShortLabel(theme_name)
                                .setLongLabel(theme_name)
                                .setIcon(app_icon)
                                .setIntent(myIntent)
                                .build();
                if (shortcutManager != null) {
                    shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
                }
                Log.d(SUBSTRATUM_LOG, "Successfully added dynamic app shortcut!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove added app shortcuts on the launcher icon
     *
     * @param context Context
     */
    public static void clearShortcut(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            if (shortcutManager != null) {
                shortcutManager.removeAllDynamicShortcuts();
            }
            Log.d(SUBSTRATUM_LOG, "Successfully removed all dynamic app shortcuts!");
        }
    }

    /**
     * Check the Xposed version on the device
     *
     * @return Returns the Xposed version
     */
    public static String checkXposedVersion() {
        String xposed_version = "";
        File f = new File("/system/framework/XposedBridge.jar");
        if (f.isFile()) {
            try {
                File file = new File("/system/", "xposed.prop");
                BufferedReader br = new BufferedReader(new FileReader(file));
                String unparsed_br = br.readLine();
                xposed_version = unparsed_br.substring(8, 10);
            } catch (FileNotFoundException e) {
                Log.e("XposedChecker", "'xposed.prop' could not be found!");
            } catch (IOException e) {
                Log.e("XposedChecker", "Unable to parse BufferedReader from 'xposed.prop'");
            }
            xposed_version = ", " + R.string.logcat_email_xposed_check + " (" +
                    xposed_version + ')';
        }
        return xposed_version;
    }

    /**
     * Inject the Substratum Rescue System archives
     *
     * @param context Context
     */
    public static void injectRescueArchives(Context context) {
        File storageDirectory = new File(Environment.getExternalStorageDirectory(),
                "/substratum/");
        if (!storageDirectory.exists() && !storageDirectory.mkdirs()) {
            Log.e(SUBSTRATUM_LOG, "Unable to create storage directory");
        }
        File rescueFile = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() +
                        File.separator + "substratum" +
                        File.separator + "SubstratumRescue.zip");
        File rescueFileLegacy = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() +
                        File.separator + "substratum" +
                        File.separator + "SubstratumRescue_Legacy.zip");
        if (rescueFile.exists() && rescueFile.delete()) {
            Log.e(SUBSTRATUM_LOG, "Deleted the rescue file!");
        }
        if (rescueFileLegacy.exists() && rescueFileLegacy.delete()) {
            Log.e(SUBSTRATUM_LOG, "Deleted the rescue legacy file!");
        }
        copyRescueFile(context, "rescue_legacy.dat",
                Environment.getExternalStorageDirectory().getAbsolutePath() +
                        File.separator + "substratum" +
                        File.separator + "SubstratumRescue_Legacy.zip");
        copyRescueFile(context, "rescue.dat",
                Environment.getExternalStorageDirectory().getAbsolutePath() +
                        File.separator + "substratum" +
                        File.separator + "SubstratumRescue.zip");
    }

    /**
     * Copy the rescue file over
     *
     * @param context        Context
     * @param sourceFileName Input file name
     * @param destFileName   Destination file name
     */
    private static void copyRescueFile(Context context,
                                       String sourceFileName,
                                       String destFileName) {
        AssetManager assetManager = context.getAssets();

        File destFile = new File(destFileName);
        File destParentDir = destFile.getParentFile();
        if (!destParentDir.exists() && !destParentDir.mkdir()) {
            Log.e(SUBSTRATUM_LOG,
                    "Unable to create directories for rescue archive dumps.");
        }

        try (
                InputStream in = assetManager.open(sourceFileName);
                OutputStream out = new FileOutputStream(destFile)
        ) {
            byte[] buffer = new byte[BYTE_ACCESS_RATE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load the app's default preferences
     *
     * @param context Context
     */
    public static void loadDefaultConfig(Context context) {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        SharedPreferences.Editor editor2 =
                context.getSharedPreferences("base_variant", Context.MODE_PRIVATE).edit();
        editor.putBoolean("show_app_icon", true);
        editor.putBoolean("substratum_oms", Systems.checkOMS(context));
        editor.putBoolean("show_template_version", false);
        editor.putBoolean("vibrate_on_compiled", false);
        editor.putBoolean("nougat_style_cards", false);
        editor.putBoolean("alternate_drawer_design", false);
        editor.putBoolean("aapt_debug", false);
        editor.putBoolean("theme_debug", false);
        editor.putBoolean("force_english", false);
        editor.putBoolean("floatui_show_android_system_overlays", false);
        editor.putBoolean("alphabetize_overlays", false);
        editor.putBoolean("complexion", true);
        editor.putBoolean("crash_receiver", true);
        editor.putBoolean("enable_swapping_overlays", false);
        editor.putBoolean("overlay_alert", false);
        editor.putBoolean("overlay_updater", false);
        editor.putBoolean("theme_updater", false);
        editor.putBoolean("show_dangerous_samsung_overlays", false);
        editor.putBoolean("autosave_logchar", true);
        editor.putBoolean("grid_style_cards", true);
        editor.putBoolean("force_english_locale", false);
        editor.putBoolean("systemui_recreate", true);
        editor.putBoolean("hide_app_checkbox", false);
        editor.putString("compiler", "aapt");
        editor.putBoolean("auto_disable_target_overlays", false);
        editor.putInt("grid_style_cards_count", DEFAULT_GRID_COUNT);
        editor.putInt("legacy_overlay_priority", DEFAULT_PRIORITY);
        editor.remove("seen_restore_warning");
        editor.remove("previous_logchar_cleanup");
        editor.remove("seen_legacy_warning");
        editor2.clear();
        Theming.refreshInstalledThemesPref(context);
        editor.apply();
        editor2.apply();
        BinaryInstaller.install(context, true);
        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context,
                LauncherActivity.class);
        p.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * Check if there is a network connection available to be used
     *
     * @param mContext Context
     * @return True, if connected to the internet
     */
    public static boolean isNetworkAvailable(Context mContext) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        try {
            InetAddress checkSiteAvailability = InetAddress.getByName("google.com");
            return activeNetworkInfo != null &&
                    activeNetworkInfo.isConnected() &&
                    !checkSiteAvailability.equals("");
        } catch (Exception e) {
            // Suppress warning
        }
        return false;
    }

    /**
     * Check if a service is funning on the device
     *
     * @param serviceClass Specified service to be checked
     * @param context      Context
     * @return True, if service running
     */
    public static boolean isServiceRunning(Class<?> serviceClass,
                                           Context context) {
        ActivityManager manager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Run a shell command on the linux terminal (no root)
     *
     * @param input Input text/command
     * @return Returns any outstanding output from the executed command
     */
    @SuppressWarnings("SameParameterValue")
    public static StringBuilder runShellCommand(String input) {
        try {
            Process shell = Runtime.getRuntime().exec(input);
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(shell.getInputStream()));

            StringBuilder returnString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                returnString.append(line).append('\n');
            }
            return returnString;
        } catch (Exception e) {
            // Suppress warning
        }
        return null;
    }

    /**
     * Checks whether signatures match up
     *
     * @param context Context
     * @return Returns the signature as an int
     */
    static int hashPassthrough(Context context) {
        if (hashValue != 0) {
            return hashValue;
        }
        try {
            @SuppressLint("PackageManagerGetSignatures") Signature[] sigs = context
                    .getPackageManager().getPackageInfo(
                            context.getPackageName(),
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

    /**
     * A beautiful sunshine on a gloomy night, you must escape the grasps of death as it brings you
     * to the destruction of life.
     *
     * @param context What's the point of parameters?
     * @return Is it true that there is an afterlife?
     */
    static Boolean spreadYourWingsAndFly(Context context) {
        if (uncertified != null) {
            return uncertified;
        }
        SharedPreferences prefs = context
                .getSharedPreferences(FirebaseAnalytics.PACKAGES_PREFS, Context.MODE_PRIVATE);
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy", Locale.US);
        String date = dateFormat.format(new Date());

        if (prefs.contains(date)) {
            Set<String> pref = prefs.getStringSet(date, new HashSet<>());
            for (String check : pref) {
                if (Packages.isPackageInstalled(context, check, false)) {
                    Log.d("PatcherDatabase",
                            "The database has triggered a primary level blacklist package.");
                    uncertified = true;
                    return true;
                } else if (Packages.getMetadata(context, check) ||
                        Packages.getProviders(context, check) ||
                        Packages.getIntents(context, check)) {
                    Log.d("PatcherDatabase",
                            "The database has triggered a secondary level blacklist package.");
                    uncertified = true;
                    return true;
                }
            }
        }
        if (Systems.checkPackageSupport(context)) {
            uncertified = true;
            return true;
        }
        uncertified = false;
        return false;
    }

    /**
     * Check if list contains item
     *
     * @param inputStr Item
     * @param items    The list
     * @return True, if containing item
     */
    public static boolean stringContainsItemFromList(String inputStr,
                                                     String[] items) {
        return Arrays.stream(items).parallel().anyMatch(inputStr::contains);
    }

    /**
     * Convert a time from computer-readable to human-readable
     *
     * @param context Context
     * @param hour    Hour
     * @param minute  Minute
     * @return Returns the proper time
     */
    @SuppressWarnings("deprecation")
    public static CharSequence parseTime(Context context,
                                         int hour,
                                         int minute) {
        Locale locale;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            locale = context.getResources().getConfiguration().locale;
        }

        String parse;
        if (android.text.format.DateFormat.is24HourFormat(context)) {
            parse = String.format(locale, "%02d:%02d", hour, minute);
        } else {
            String AM_PM = (hour <= 12) ? "AM" : "PM";
            hour = (hour <= 12) ? hour : (hour - 12);
            parse = String.format(locale, "%d:%02d " + AM_PM, hour, minute);
        }
        return parse;
    }

    /**
     * Write LogChar file
     *
     * @param packageName Package name to write my lengthy essay on
     * @param data        The content that makes my novel interesting
     */
    public static void writeLogCharFile(String packageName,
                                        String data) {
        try {
            Calendar c = Calendar.getInstance();
            @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat
                    ("yyyy-MM-dd_HH-mm-ss");
            String formattedDate = df.format(c.getTime());

            File logcharFolder = new File(Environment.getExternalStorageDirectory() +
                    File.separator + "substratum" + File.separator + "LogCharReports");
            if (!logcharFolder.exists() && logcharFolder.mkdirs()) {
                Log.d("LogChar Utility", "Created LogChar directory!");
            }

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                    new File(logcharFolder.getAbsolutePath() + File.separator +
                            packageName + '_' + formattedDate + ".txt")));
            bufferedWriter.write(data);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Copy text to clipboard
     *
     * @param context Context
     * @param id      Identifier for clipboard data (overridable, as it is not user-visible)
     * @param content Text
     */
    public static void copyToClipboard(Context context,
                                       CharSequence id,
                                       CharSequence content) {
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(id, content);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
    }

    /**
     * Adjust animations for the recyclerView
     *
     * @param context            Context
     * @param view               View
     * @param animation_resource Specified animation resource
     */
    public static void setRecyclerViewAnimation(Context context,
                                                View view,
                                                int animation_resource) {
        Animation animation = AnimationUtils.loadAnimation(context, animation_resource);
        view.startAnimation(animation);
    }

    /**
     * Grab the master view of the activity
     * <p>
     * ATTENTION: Developers, we should not use the ButterKnife library to address the specific
     * master view of android.R.id.content, or else we will lose the LunchBar animation to slide
     * the floating action button up.
     *
     * @return The master view of the activity
     */
    public static View getView(Activity activity) {
        return ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
    }

    /**
     * Returns the Coordinator layout in {@link projekt.substratum.InformationActivity}
     *
     * @param Activity the activity instance to be checked.
     * @return a View object which is an instance of CoordinatorLayout
     */
    @Nullable
    public static View getCoordinatorLayoutView(Activity activity) {
        if (activity instanceof InformationActivity) {
            return getView(activity).findViewById(R.id.coordinator_layout);
        }
        return null;
    }

    /**
     * Marking down what's important!
     */
    public static class Markdown extends AsyncTask<Void, Void, Void> {
        @SuppressLint("StaticFieldLeak")
        private Context context;
        private SharedPreferences prefs;

        public Markdown(Context context, SharedPreferences prefs) {
            super();
            this.context = context;
            this.prefs = prefs;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected Void doInBackground(Void... sUrl) {
            this.prefs.edit().putBoolean("complexion",
                    !spreadYourWingsAndFly(this.context) &&
                            (hashPassthrough(this.context) != 0)).apply();
            return null;
        }
    }
}