/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.common;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.annotation.Nullable;
import projekt.substratum.InformationActivity;
import projekt.substratum.LauncherActivity;
import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.activities.shortcuts.AppShortcutLaunch;
import projekt.substratum.services.profiles.ScheduledProfileReceiver;
import projekt.substratum.util.helpers.BinaryInstaller;
import projekt.substratum.util.helpers.TranslatorParser;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static android.content.Context.CLIPBOARD_SERVICE;
import static projekt.substratum.common.Internal.BYTE_ACCESS_RATE;

public class References {

    public static final boolean ENABLE_ROOT_CHECK = true; // Force the app to run without root
    public static final boolean ENABLE_EXTRAS_DIALOG = false; // Show a dialog when applying extras
    public static final boolean ENABLE_AAPT_OUTPUT = false; // WARNING, DEVELOPERS - BREAKS COMPILE
    public static final boolean ENABLE_PACKAGE_LOGGING = false; // Show time/date/place of install
    public static final boolean ENABLE_DIRECT_ASSETS_LOGGING = false; // Self explanatory
    public static final boolean BYPASS_SYSTEM_VERSION_CHECK = false; // For developer previews only!
    public static final boolean BYPASS_SUBSTRATUM_BUILDER_DELETION = false; // Do not delete cache?
    public static final String SECURITY_UPDATE_WARN_AFTER = "2018-02-01";
    // These are specific log tags for different classes
    public static final String SUBSTRATUM_BUILDER = "SubstratumBuilder";
    public static final String SUBSTRATUM_LOG = "SubstratumLogger";
    public static final String SUBSTRATUM_VALIDATOR = "SubstratumValidator";
    // These are package names for our backend systems
    public static final String ANDROMEDA_PACKAGE = "projekt.andromeda";
    public static final String INTERFACER_PACKAGE = "projekt.interfacer";
    public static final String SUBSTRATUM_PACKAGE = "projekt.substratum";
    public static final String COMMON_PACKAGE = "com.mon";
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
    public static final String[] metadataOverlayTypes = new String[]{
            References.metadataOverlayType1a,
            References.metadataOverlayType1b,
            References.metadataOverlayType1c,
            References.metadataOverlayType2,
            References.metadataOverlayType3,
            References.metadataOverlayType4
    };
    public static final String metadataOverlayVersion = "Substratum_OverlayVersion";
    public static final String metadataThemeVersion = "Substratum_Version";
    public static final String metadataSamsungSupport = "Substratum_Samsung";
    public static final String resourceChangelog = "ThemeChangelog";
    // These strings control the folders to detect in the assets
    public static final String overlaysFolder = "overlays";
    public static final String bootAnimationsFolder = "bootanimation";
    public static final String shutdownAnimationsFolder = "shutdownanimation";
    public static final String fontsFolder = "fonts";
    public static final String soundsFolder = "audio";
    // These strings control the fragments to be loaded by the app
    public static final String overlaysFragment = "overlays";
    public static final String bootAnimationsFragment = "bootanimations";
    public static final String fontsFragment = "fonts";
    public static final String soundsFragment = "sounds";
    public static final String wallpaperFragment = "wallpapers";
    // These strings control the showcase metadata parsing
    public static final String paidTheme = "paid";
    // These strings control the directories that Substratum uses
    public static final String EXTERNAL_STORAGE_CACHE = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/.substratum/";
    public static final String EXTERNAL_STORAGE_SAMSUNG_OVERLAY_CACHE =
            Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.samsung_overlays.xml";
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
    public static final int NOTIFICATION_ID_COMPILER = 17589715;
    // This int controls the delay for window refreshes to occur
    public static final int REFRESH_WINDOW_DELAY = 500;
    // This int controls the default grid count for the theme adapter
    public static final int MIN_GRID_COUNT = 1;
    public static final int DEFAULT_GRID_COUNT = 2;
    public static final int MAX_GRID_COUNT = 4;
    // This String controls the default theme setting
    public static final String APP_THEME = "app_theme";
    public static final String DEFAULT_THEME = "light";
    public static final String AUTO_THEME = "auto";
    public static final String DARK_THEME = "dark";
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
    public static final boolean VALIDATE_WITH_LOGS = false;
    // Special permission for Samsung devices
    public static final String SAMSUNG_OVERLAY_PERMISSION =
            "com.samsung.android.permission.SAMSUNG_OVERLAY_COMPONENT";
    public static final String MAGISK_MODULE_DIR = "/sbin/.magisk/img/substratum/";
    // This string controls the hero image name
    static final String heroImageResourceName = "heroimage";
    static final String heroImageGridResourceName = "heroimage_grid";
    static final String heroImageMainResourceName = "heroimage_banner";
    // Specific intents Substratum should be listening to
    static final String APP_CRASHED = "projekt.substratum.APP_CRASHED";
    // Control the animation duration
    private static final int FADE_FROM_GRAYSCALE_TO_COLOR_DURATION = 1250;
    // Localized variables shared amongst common resources
    static ScheduledProfileReceiver scheduledProfileReceiver;
    private static int hashValue;

    public static String getPieDir() {
        return MAGISK_MODULE_DIR + "system/app/";
    }

    public static String getPieMountPoint() {
        return MAGISK_MODULE_DIR;
    }

    /**
     * Unified method to set theme extra lists
     *
     * @param context                Context
     * @param themePid               Package name
     * @param listDir                Folder to check
     * @param defaultSpinnerText     0th index of the spinner
     * @param spinnerSetDefaultsText 1st index of the spinner
     * @param encryptionLevel        Encrypted or not
     * @param activity               Activity
     * @param spinner                Spinner object
     * @return Mutated spinner object
     */
    public static Spinner setThemeExtraLists(Context context,
                                             String themePid,
                                             String listDir,
                                             String defaultSpinnerText,
                                             String spinnerSetDefaultsText,
                                             boolean encryptionLevel,
                                             Activity activity,
                                             Spinner spinner) {
        AssetManager themeAssetManager = getThemeAssetManager(context, themePid);
        if (themeAssetManager != null) {
            try {
                final String[] fileArray = themeAssetManager.list(listDir);
                final List<String> archivedSounds = new ArrayList<>();
                Collections.addAll(archivedSounds, fileArray != null ? fileArray : new String[0]);

                // Creates the list of dropdown items
                final ArrayList<String> unarchivedExtra = new ArrayList<>();
                unarchivedExtra.add(defaultSpinnerText);
                unarchivedExtra.add(spinnerSetDefaultsText);
                for (String archivedSound : archivedSounds) {
                    unarchivedExtra.add(archivedSound.substring(0,
                            archivedSound.length() - (encryptionLevel ? 8 : 4)));
                }

                assert activity != null;
                final SpinnerAdapter adapter1 = new ArrayAdapter<>(activity,
                        android.R.layout.simple_spinner_dropdown_item, unarchivedExtra);
                spinner.setAdapter(adapter1);
                return spinner;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Get the AssetManager of the theme package
     *
     * @param context     Context
     * @param packageName Package name to pull Asset Manager from
     * @return AssetManager of the specified package name
     */
    public static AssetManager getThemeAssetManager(Context context, String packageName) {
        try {
            Resources themeResources =
                    context.getPackageManager().getResourcesForApplication(packageName);
            return themeResources.getAssets();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Prettify the recycler view UI with fading desaturating colors!
     *
     * @param imageView The view that will be animating
     */
    public static void setRecyclerViewAnimations(ImageView imageView) {
        ColorMatrix matrix = new ColorMatrix();
        ValueAnimator animation = ValueAnimator.ofFloat(0f, 1f);
        animation.setDuration(FADE_FROM_GRAYSCALE_TO_COLOR_DURATION);
        animation.addUpdateListener(animation1 -> {
            matrix.setSaturation(animation1.getAnimatedFraction());
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            imageView.setColorFilter(filter);
        });
        animation.start();
    }

    /**
     * Create a launcher icon/launchable intent
     *
     * @param context   Self explanatory, bro.
     * @param themePid  Theme's package name (launcher intent process)
     * @param themeName Theme's name (launcher icon name)
     */
    public static void createLauncherIcon(Context context,
                                          String themePid,
                                          String themeName) {
        createLauncherIcon(context, themePid, themeName, false);
    }

    /**
     * Create a launcher icon/launchable intent
     *
     * @param context               Self explanatory, bro.
     * @param themePid              Theme's package name (launcher intent process)
     * @param themeName             Theme's name (launcher icon name)
     * @param launchManagerFragment boolean flag that creates an intent to launch
     *                              {@link projekt.substratum.fragments.ManagerFragment}
     * @return Returns an intent with the corresponding action
     */
    public static Intent createLauncherIcon(
            Context context,
            String themePid,
            String themeName,
            boolean launchManagerFragment) {
        Intent myIntent = new Intent(Intent.ACTION_MAIN);
        if (!launchManagerFragment) {
            myIntent.putExtra(Internal.THEME_PID, themePid);
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

        Bitmap appIcon = Packages.getBitmapFromDrawable(Packages.getAppIcon(context,
                themePid));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, myIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, themeName);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, appIcon);
            addIntent.putExtra("duplicate", false);
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            context.sendBroadcast(addIntent);
        } else {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            ShortcutInfo shortcut =
                    new ShortcutInfo.Builder(context, themeName)
                            .setShortLabel(themeName)
                            .setLongLabel(themeName)
                            .setIcon(Icon.createWithBitmap(appIcon))
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
     * @param context   Context
     * @param themePid  Package name
     * @param themeName Theme name
     */
    public static void createShortcut(Context context,
                                      String themePid,
                                      String themeName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            Icon appIcon;
            Drawable appIconDrawable = Packages.getAppIcon(context, themePid);
            //If we are on Oreo and the Theme uses an adaptiveIcon, we have to treat it properly
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    appIconDrawable instanceof AdaptiveIconDrawable) {
                appIcon = Icon.createWithAdaptiveBitmap(Packages.getBitmapFromDrawable
                        (appIconDrawable));
            } else {
                appIcon = Icon.createWithBitmap(Packages.getBitmapFromDrawable(appIconDrawable));
            }
            try {
                Intent myIntent = new Intent(Intent.ACTION_MAIN);
                myIntent.putExtra(Internal.THEME_NAME, themeName);
                myIntent.putExtra(Internal.THEME_PID, themePid);
                myIntent.setComponent(
                        ComponentName.unflattenFromString(
                                context.getPackageName() +
                                        '/' + AppShortcutLaunch.class.getName()));
                myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                ShortcutInfo shortcut =
                        new ShortcutInfo.Builder(context, "favorite")
                                .setShortLabel(themeName)
                                .setLongLabel(themeName)
                                .setIcon(appIcon)
                                .setIntent(myIntent)
                                .build();
                if (shortcutManager != null) {
                    shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
                }
                Substratum.log(SUBSTRATUM_LOG, "Successfully added dynamic app shortcut!");
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
            Substratum.log(SUBSTRATUM_LOG, "Successfully removed all dynamic app shortcuts!");
        }
    }

    /**
     * Check the Xposed version on the device
     *
     * @return Returns the Xposed version
     */
    public static String checkXposedVersion() {
        String xposedVersion = "";
        File f = new File("/system/framework/XposedBridge.jar");
        if (f.isFile()) {
            try {
                File file = new File("/system/", "xposed.prop");
                BufferedReader br = new BufferedReader(new FileReader(file));
                xposedVersion = br.readLine().substring(8, 10);
            } catch (FileNotFoundException e) {
                Log.e("XposedChecker", "'xposed.prop' could not be found!");
            } catch (IOException e) {
                Log.e("XposedChecker", "Unable to parse BufferedReader from 'xposed.prop'");
            }
            xposedVersion = ", " + R.string.logcat_email_xposed_check + " (" +
                    xposedVersion + ')';
        }
        return xposedVersion;
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
        copyRescueFile(context, "rescue_legacy.dat", rescueFileLegacy.getAbsolutePath());
        copyRescueFile(context, "rescue.dat", rescueFile.getAbsolutePath());
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
        SharedPreferences.Editor editor = Substratum.getPreferences().edit();
        SharedPreferences.Editor editor2 =
                context.getSharedPreferences("base_variant", Context.MODE_PRIVATE).edit();
        editor.putBoolean("first_run", false);
        editor.putBoolean("show_app_icon", true);
        editor.putBoolean("oms_state", Systems.checkOMS(context));
        editor.putBoolean("vibrate_on_compiled", false);
        editor.putBoolean("theme_debug", false);
        editor.putBoolean("force_english_locale", false);
        editor.putBoolean("floatui_show_android_system_overlays", false);
        editor.putBoolean("alphabetize_overlays", false);
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
        editor.putBoolean("auto_disable_target_overlays", false);
        editor.putBoolean("lite_mode", false);
        editor.putBoolean("sungstromeda_mode", true);
        editor.putString(APP_THEME, DEFAULT_THEME);
        editor.putInt("grid_style_cards_count", DEFAULT_GRID_COUNT);
        editor.putInt("legacy_overlay_priority", DEFAULT_PRIORITY);
        editor.remove("previous_logchar_cleanup");
        editor.remove("seen_legacy_warning");
        editor.remove("rooted_oms_dismissal");
        editor.remove("new_stock_dismissal");
        editor.remove("xiaomi_enable_development");
        editor.remove("legacy_dismissal");
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
     * @param context Context
     * @return True, if connected to the internet
     */
    public static boolean isNetworkAvailable(Context context) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        try {
            String checkSiteAvailability = InetAddress.getByName("google.com").toString();
            return activeNetworkInfo != null &&
                    activeNetworkInfo.isConnected() &&
                    !checkSiteAvailability.equals("");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Check if a service is running on the device
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
        } catch (Exception ignored) {
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
    public static CharSequence parseTime(Context context,
                                         int hour,
                                         int minute) {

        Locale locale =
                context.getResources().getConfiguration().getLocales().get(0);
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
                Substratum.log("LogChar Utility", "Created LogChar directory!");
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
     * Resizes a given image dynamically based on the user's screen
     *
     * @param image Drawable to be resized dynamically
     * @return Resized drawable
     */
    public static Drawable dynamicallyResize(Drawable image) {
        Bitmap b = ((BitmapDrawable) image).getBitmap();

        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        int gridCount = Substratum.getPreferences().getInt("grid_style_cards_count", 1);
        float targetWidthSize = (float) screenWidth / gridCount;

        int width = image.getIntrinsicWidth();
        float dstMultiplier = targetWidthSize / width;
        int dstWidth = (int) (image.getIntrinsicWidth() * dstMultiplier);
        int dstHeight = (int) (image.getIntrinsicHeight() * dstMultiplier);

        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, dstWidth, dstHeight, true);
        return new BitmapDrawable(Substratum.getInstance().getResources(), bitmapResized);
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
     * @param activity the activity instance to be checked.
     * @return a View object which is an instance of CoordinatorLayout
     */
    @Nullable
    public static View getCoordinatorLayoutView(Activity activity) {
        if (activity instanceof InformationActivity) {
            return getView(activity).findViewById(R.id.coordinator_layout);
        }
        return null;
    }

    public static AlertDialog.Builder invokeTranslatorDialog(Context context,
                                                             List<TranslatorParser.Translator>
                                                                     translators) {
        TableLayout table = new TableLayout(context);
        table.setPadding(30, 10, 30, 10);

        final int size_of_row_text = 10;

        // Dialog title
        TableRow columnHeaders = new TableRow(context);
        TableRow titleRow = new TableRow(context);
        TextView title = new TextView(context);
        title.setText(context.getString(R.string.team_title_two));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, 20, 0, 20);
        titleRow.setGravity(Gravity.CENTER);
        titleRow.addView(title);

        // Equivalent column sizes params
        TableRow.LayoutParams equivalentParams = new TableRow.LayoutParams();
        equivalentParams.width = 0;
        equivalentParams.weight = (float) 0.33;

        // Titles
        TextView nameColumn = new TextView(context);
        nameColumn.setText(context.getString(R.string.translator_name));
        nameColumn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nameColumn.setGravity(Gravity.CENTER_HORIZONTAL);
        nameColumn.setLayoutParams(equivalentParams);

        TextView languageColumn = new TextView(context);
        languageColumn.setText(context.getString(R.string.translator_languages));
        languageColumn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        languageColumn.setGravity(Gravity.CENTER_HORIZONTAL);
        languageColumn.setLayoutParams(equivalentParams);

        TextView translatedWordsColumn = new TextView(context);
        translatedWordsColumn.setText(context.getString(R.string.translator_words));
        translatedWordsColumn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        translatedWordsColumn.setGravity(Gravity.CENTER_HORIZONTAL);
        translatedWordsColumn.setLayoutParams(equivalentParams);

        columnHeaders.addView(nameColumn);
        columnHeaders.addView(languageColumn);
        columnHeaders.addView(translatedWordsColumn);
        columnHeaders.setPadding(0, 10, 0, 30);

        table.addView(titleRow);
        table.addView(columnHeaders);

        for (TranslatorParser.Translator translator : translators) {
            TableRow rowLows = new TableRow(context);

            TextView contributor = new TextView(context);
            contributor.setText(translator.contributorName);
            contributor.setTextSize(size_of_row_text);
            contributor.setTypeface(Typeface.DEFAULT_BOLD);
            contributor.setGravity(Gravity.CENTER_HORIZONTAL);
            contributor.setLayoutParams(equivalentParams);

            String langs = translator.languages.toString();
            langs = langs.substring(1, langs.length() - 1);
            TextView languages = new TextView(context);
            languages.setText(langs);
            languages.setTextSize(size_of_row_text);
            languages.setTypeface(Typeface.DEFAULT_BOLD);
            languages.setGravity(Gravity.CENTER_HORIZONTAL);
            languages.setLayoutParams(equivalentParams);

            TextView translated = new TextView(context);
            translated.setText(String.valueOf(translator.translated_words));
            translated.setTextSize(size_of_row_text);
            translated.setTypeface(Typeface.DEFAULT_BOLD);
            translated.setGravity(Gravity.CENTER_HORIZONTAL);
            translated.setLayoutParams(equivalentParams);

            rowLows.addView(contributor);
            rowLows.addView(languages);
            rowLows.addView(translated);
            table.addView(rowLows);
        }
        ScrollView sv = new ScrollView(context);
        sv.addView(table);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(sv);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.cancel());
        return builder;
    }

    /**
     * Marking down what's important!
     */
    public static class Markdown extends AsyncTask<Void, Void, Void> {
        @SuppressLint("StaticFieldLeak")
        private final Context context;

        public Markdown(Context context) {
            super();
            this.context = context;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected Void doInBackground(Void... sUrl) {
            hashPassthrough(this.context);
            return null;
        }
    }
}