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
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.om.IOverlayManager;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.VectorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import projekt.substratum.R;
import projekt.substratum.activities.launch.AppShortcutLaunch;
import projekt.substratum.activities.launch.ThemeLaunchActivity;
import projekt.substratum.common.analytics.FirebaseAnalytics;
import projekt.substratum.common.analytics.PackageAnalytics;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.platform.ThemeInterfacerService;
import projekt.substratum.common.tabs.SoundManager;
import projekt.substratum.services.crash.AppCrashReceiver;
import projekt.substratum.services.packages.OverlayFound;
import projekt.substratum.services.packages.OverlayUpdater;
import projekt.substratum.services.packages.PackageModificationDetector;
import projekt.substratum.services.profiles.ScheduledProfileReceiver;
import projekt.substratum.services.system.InterfacerAuthorizationReceiver;
import projekt.substratum.util.compilers.CacheCreator;
import projekt.substratum.util.files.Root;
import projekt.substratum.util.injectors.CheckBinaries;
import projekt.substratum.util.readers.ReadSupportedROMsFile;
import projekt.substratum.util.readers.ReadVariantPrioritizedColor;

import static projekt.substratum.common.analytics.PackageAnalytics.PACKAGE_TAG;

public class References {

    public static final Boolean ENABLE_ROOT_CHECK = true; // Force the app to run without root
    public static final Boolean ENABLE_EXTRAS_DIALOG = false; // Show a dialog when applying extras
    public static final Boolean ENABLE_AOPT_OUTPUT = false; // WARNING, DEVELOPERS - BREAKS COMPILE
    public static final Boolean ENABLE_PACKAGE_LOGGING = false; // Show time/date/place of install
    public static final Boolean ENABLE_DIRECT_ASSETS_LOGGING = false; // Self explanatory
    public static final Boolean BYPASS_ALL_VERSION_CHECKS = false; // For developer previews only!
    public static final Boolean BYPASS_SUBSTRATUM_BUILDER_DELETION = false; // Do not delete cache?
    @SuppressWarnings("WeakerAccess")
    public static final Boolean FORCE_SAMSUNG_VARIANT = false; // Debugging on a non-Samsung device
    public static final Integer OVERLAY_UPDATE_RANGE = 815; // Overlays require updating since ver
    // These are specific log tags for different classes
    public static final String SUBSTRATUM_BUILDER = "SubstratumBuilder";
    public static final String SUBSTRATUM_LOG = "SubstratumLogger";
    public static final String SUBSTRATUM_VALIDATOR = "SubstratumValidator";
    // These are package names for our backend systems
    public static final String ANDROMEDA_PACKAGE = "projekt.andromeda";
    public static final String INTERFACER_PACKAGE = "projekt.interfacer";
    public static final String INTERFACER_SERVICE = INTERFACER_PACKAGE + ".services.JobService";
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
    public static final String MANAGER_REFRESH = "projekt.substratum.MANAGER_REFRESH";
    public static final String KEY_RETRIEVAL = "Substratum.KeyRetrieval";
    public static final String TEMPLATE_THEME_MODE = "projekt.substratum.THEME";
    public static final String TEMPLATE_GET_KEYS = "projekt.substratum.GET_KEYS";
    // Keep it simple, stupid!
    public static final int HIDDEN_CACHING_MODE_TAP_COUNT = 7;
    public static final int SHOWCASE_SHUFFLE_COUNT = 5;
    // These strings control the current filter for themes
    public static final String metadataName = "Substratum_Name";
    public static final String metadataAuthor = "Substratum_Author";
    public static final String metadataEmail = "Substratum_Email";
    public static final String metadataLegacy = "Substratum_Legacy";
    public static final String metadataEncryption = "Substratum_Encryption";
    public static final String metadataEncryptionValue = "onCompileVerify";
    public static final String metadataWallpapers = "Substratum_Wallpapers";
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
    // This controls the filter used by the post-6.0.0 template checker
    private static final String SUBSTRATUM_THEME = "projekt.substratum.THEME";
    private static final String SUBSTRATUM_LAUNCHER_CLASS = ".SubstratumLauncher";
    private static final String SUBSTRATUM_LAUNCHER_CLASS_PATH =
            "substratum.theme.template.SubstratumLauncher";
    // Specific intents Substratum should be listening to
    private static final String APP_CRASHED = "projekt.substratum.APP_CRASHED";
    private static final String TEMPLATE_RECEIVE_KEYS = "projekt.substratum.RECEIVE_KEYS";
    // Metadata used in theme templates to denote specific parts of a theme
    private static final String metadataVersion = "Substratum_Plugin";
    private static final String metadataThemeReady = "Substratum_ThemeReady";
    private static final String metadataSamsungSupport = "Substratum_Samsung";
    private static final String resourceChangelog = "ThemeChangelog";
    // This string controls the hero image name
    private static final String heroImageResourceName = "heroimage";
    // This int controls the notification identifier
    public static int firebase_notification_id = 24862486;
    public static int notification_id = 2486;
    public static int notification_id_upgrade = 248600;
    // This int controls the delay for window refreshes to occur
    public static int REFRESH_WINDOW_DELAY = 500;
    // This int controls the default grid count for the theme adapter
    public static int MIN_GRID_COUNT = 1;
    public static int DEFAULT_GRID_COUNT = 2;
    public static int MAX_GRID_COUNT = 4;
    // This int controls the default priority level for legacy overlays
    public static int DEFAULT_PRIORITY = 999;
    // These strings control package names for system apps
    public static String settingsPackageName = "com.android.settings";
    public static String settingsSubstratumDrawableName = "ic_settings_substratum";
    // These values control the dynamic certification of substratum
    private static Boolean uncertified = null;
    private static int hashValue;
    // Localized variables shared amongst common resources
    private static ScheduledProfileReceiver scheduledProfileReceiver;

    public static String checkFirmwareSupport(Context context, String urls, String inputFileName) {
        String supported_rom = "";
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(urls).openConnection();
            connection.connect();

            if (References.isNetworkAvailable(context)) {
                try (InputStream input = connection.getInputStream();
                     OutputStream output = new FileOutputStream(
                             context.getCacheDir().getAbsolutePath() + "/" + inputFileName)) {

                    // expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return "Server returned HTTP " + connection.getResponseCode()
                                + " " + connection.getResponseMessage();
                    }

                    byte data[] = new byte[4096];
                    int count;
                    while ((count = input.read(data)) != -1) {
                        // allow canceling with back button
                        output.write(data, 0, count);
                    }
                }
            } else {
                File check = new File(context.getCacheDir().getAbsolutePath() +
                        "/" + inputFileName);
                if (!check.exists()) {
                    return "";
                }
            }

            HashMap<String, String> listOfRoms =
                    ReadSupportedROMsFile.main(context.getCacheDir() + "/" + inputFileName);
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
                    if (line != null && line.length() > 0) {
                        if (value == null || value.length() == 0) {
                            String current = key;
                            if (current.contains(".")) {
                                current = current.split("\\.")[1];
                            }
                            Log.d(References.SUBSTRATUM_LOG, "Supported ROM: " + current);
                            supported_rom = current;
                            supported = true;
                        } else {
                            Log.d(References.SUBSTRATUM_LOG, "Supported ROM: " + value);
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

                            if (line.toLowerCase(Locale.getDefault())
                                    .contains(key.toLowerCase(
                                            Locale.getDefault()))) {
                                if (value == null || value.length() == 0) {
                                    String current = key;
                                    if (current.contains(".")) {
                                        current = current.split("\\.")[1];
                                    }
                                    Log.d(References.SUBSTRATUM_LOG,
                                            "Supported ROM (1): " + current);
                                    supported_rom = current;
                                    supported = true;
                                } else {
                                    Log.d(References.SUBSTRATUM_LOG,
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

    public static void registerBroadcastReceivers(Context context) {
        try {
            IntentFilter intentPackageAdded = new IntentFilter(PACKAGE_ADDED);
            intentPackageAdded.addDataScheme("package");
            IntentFilter intentPackageFullyRemoved = new IntentFilter(PACKAGE_FULLY_REMOVED);
            intentPackageFullyRemoved.addDataScheme("package");

            if (checkOMS(context)) {
                IntentFilter intentAppCrashed = new IntentFilter(APP_CRASHED);
                context.getApplicationContext().registerReceiver(
                        new AppCrashReceiver(), intentAppCrashed);
                context.getApplicationContext().registerReceiver(
                        new OverlayFound(), intentPackageAdded);
                context.getApplicationContext().registerReceiver(
                        new OverlayUpdater(), intentPackageAdded);
            }

            if (checkThemeInterfacer(context)) {
                IntentFilter interfacerAuthorize = new IntentFilter(
                        INTERFACER_PACKAGE + ".CALLER_AUTHORIZED");
                context.getApplicationContext().registerReceiver(
                        new InterfacerAuthorizationReceiver(), interfacerAuthorize);
            }

            context.getApplicationContext().registerReceiver(
                    new PackageModificationDetector(), intentPackageAdded);
            context.getApplicationContext().registerReceiver(
                    new PackageModificationDetector(), intentPackageFullyRemoved);

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
            // Suppress warning
        }
    }

    public static boolean isCachingEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("caching_enabled", false);
    }

    public static void createLauncherIcon(Context context, String theme_pid, String theme_name) {
        Intent myIntent = new Intent(Intent.ACTION_MAIN);
        myIntent.putExtra("theme_name", theme_name);
        myIntent.putExtra("theme_pid", theme_pid);
        myIntent.setComponent(
                ComponentName.unflattenFromString(
                        context.getPackageName() +
                                "/" + AppShortcutLaunch.class.getName()));
        myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Bitmap app_icon = getBitmapFromDrawable(grabAppIcon(context, theme_pid));

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
    }

    public static void createShortcut(Context context, String theme_pid, String theme_name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            Bitmap app_icon = ((BitmapDrawable)
                    grabAppIcon(context, theme_pid)).getBitmap();
            try {
                Intent myIntent = new Intent(Intent.ACTION_MAIN);
                myIntent.putExtra("theme_name", theme_name);
                myIntent.putExtra("theme_pid", theme_pid);
                myIntent.setComponent(
                        ComponentName.unflattenFromString(
                                context.getPackageName() +
                                        "/" + AppShortcutLaunch.class.getName()));
                myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                ShortcutInfo shortcut =
                        new ShortcutInfo.Builder(context, "favorite")
                                .setShortLabel(theme_name)
                                .setLongLabel(theme_name)
                                .setIcon(Icon.createWithBitmap(app_icon))
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

    public static void clearShortcut(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            if (shortcutManager != null) {
                shortcutManager.removeAllDynamicShortcuts();
            }
            Log.d(SUBSTRATUM_LOG, "Successfully removed all dynamic app shortcuts!");
        }
    }

    public static String checkXposedVersion() {
        String xposed_version = "";
        File f = new File("/system/framework/XposedBridge.jar");
        if (f.isFile()) {
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
        if (dpm != null) status = dpm.getStorageEncryptionStatus();
        return status;
    }

    // This method is used to place the Substratum Rescue archives if they are not present
    public static void injectRescueArchives(Context context) {
        File storageDirectory = new File(Environment.getExternalStorageDirectory(), "/substratum/");
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
        if (rescueFile.exists()) {
            rescueFile.delete();
        }

        if (rescueFileLegacy.exists()) {
            rescueFileLegacy.delete();
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

    private static void copyRescueFile(Context context, String sourceFileName, String
            destFileName) {
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
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isOMSRunning(Context context, Class<?> serviceClass) {
        final ActivityManager activityManager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        assert activityManager != null;
        final List<ActivityManager.RunningServiceInfo> services =
                activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClass.getName())) {
                return true;
            }
        }
        return false;
    }

    // This method is used to determine whether there the system is initiated with OMS
    public static Boolean checkOMS(@NonNull Context context) {
        if (FORCE_SAMSUNG_VARIANT) return false;
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

    public static Boolean checkOreo() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.O;
    }

    public static void setAndCheckOMS(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove("oms_state").apply();
        try {
            boolean foundOms = false;
            if (!isSamsungDevice(context)) {
                if (checkThemeInterfacer(context)) {
                    foundOms = true;
                } else {
                    Boolean isOMSRunning = isOMSRunning(context.getApplicationContext(),
                            IOverlayManager.class);
                    if (isOMSRunning || References.checkOreo()) {
                        Log.d(SUBSTRATUM_LOG, "Found Overlay Manager Service...");
                        foundOms = true;
                    } else {
                        String out = Root.runCommand("cmd overlay").split("\n")[0];
                        if (out.equals("The overlay manager has already been initialized.") ||
                                out.equals("Overlay manager (overlay) commands:")) {
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
        } catch (Exception e) {
            prefs.edit().putBoolean("oms_state", false).apply();
            prefs.edit().putInt("oms_version", 0).apply();
            Log.d(SUBSTRATUM_LOG, "Initializing Substratum with the second " +
                    "iteration of the Resource Runtime Overlay system...");
        }
    }

    public static boolean checkSubstratumFeature(Context context) {
        // Using lowercase because that's how we defined it in our permissions xml
        return context.getPackageManager().hasSystemFeature(SUBSTRATUM_THEME.toLowerCase(Locale
                .getDefault()));
    }

    // This method is used to determine whether there the system was dirty flashed / upgraded
    public static Boolean checkROMVersion(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains("build_date")) {
            setROMVersion(context, false);
        }
        String prop = getProp("ro.build.date.utc");
        return prefs.getInt("build_date", 0) ==
                ((prop != null && prop.length() > 0) ? Integer.parseInt(prop) : 0);
    }

    public static void setROMVersion(Context context, boolean force) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains("build_date") || force) {
            String prop = getProp("ro.build.date.utc");
            prefs.edit().putInt("build_date",
                    (prop != null && prop.length() > 0) ? Integer.parseInt(prop) : 0)
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

    // Load SharedPreference defaults
    public static void loadDefaultConfig(Context context) {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean("show_app_icon", true);
        editor.putBoolean("substratum_oms", checkOMS(context));
        editor.putBoolean("show_template_version", false);
        editor.putBoolean("vibrate_on_compiled", false);
        editor.putBoolean("nougat_style_cards", false);
        editor.putBoolean("aopt_debug", false);
        editor.putBoolean("theme_debug", false);
        editor.putBoolean("force_english", false);
        editor.putBoolean("floatui_show_android_system_overlays", false);
        editor.putBoolean("alphabetize_showcase", false);
        editor.putBoolean("complexion", true);
        editor.putString("compiler", "aapt");
        editor.putBoolean("crash_receiver", true);
        editor.putBoolean("enable_swapping_overlays", false);
        editor.putBoolean("overlay_alert", false);
        editor.putBoolean("overlay_updater", false);
        editor.putBoolean("theme_updater", false);
        editor.putBoolean("show_dangerous_samsung_overlays", false);
        editor.putBoolean("autosave_logchar", true);
        editor.putBoolean("grid_style_cards", true);
        editor.putInt("grid_style_cards_count", DEFAULT_GRID_COUNT);
        editor.putInt("legacy_overlay_priority", DEFAULT_PRIORITY);
        editor.remove("seen_restore_warning");
        editor.remove("previous_logchar_cleanup");
        editor.remove("seen_legacy_warning");

        refreshInstalledThemesPref(context);
        editor.apply();
        editor = context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE).edit();
        editor.putBoolean("is_updating", false);
        editor.apply();
        CheckBinaries.install(context, true);
    }

    public static void refreshInstalledThemesPref(Context context) {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();

        // Initial parse of what is installed on the device
        Set<String> installed_themes = new TreeSet<>();
        List<ResolveInfo> all_themes = getThemes(context);
        for (int i = 0; i < all_themes.size(); i++) {
            installed_themes.add(all_themes.get(i).activityInfo.packageName);
        }
        editor.putStringSet("installed_themes", installed_themes);
        editor.apply();
    }

    public static String grabInstallerId(Context context, String package_name) {
        return context.getPackageManager().getInstallerPackageName(package_name);
    }

    // This method configures the new devices and their configuration of their vendor folders
    public static Boolean inNexusFilter() {
        return Arrays.asList(Resources.NEXUS_FILTER).contains(Build.DEVICE);
    }

    // This method checks whether there is any network available for Wallpapers
    public static boolean isNetworkAvailable(Context mContext) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // This method checks whether fonts is supported by the system
    public static boolean isFontsSupported() {
        if (FORCE_SAMSUNG_VARIANT) return false;
        try {
            Class<?> cls = Class.forName("android.graphics.Typeface");
            cls.getDeclaredMethod("getSystemFontDirLocation");
            cls.getDeclaredMethod("getThemeFontConfigLocation");
            cls.getDeclaredMethod("getThemeFontDirLocation");
            Log.d(References.SUBSTRATUM_LOG, "This system fully supports font hotswapping.");
            return true;
        } catch (Exception ex) {
            // Suppress Fonts
        }
        return false;
    }

    // This string array contains all the SystemUI acceptable overlay packs
    public static Boolean allowedSounds(String current) {
        return Arrays.asList(SoundManager.ALLOWED_SOUNDS).contains(current);
    }

    // This string array contains all the SystemUI acceptable overlay packs
    public static Boolean allowedSystemUIOverlay(String current) {
        return Arrays.asList(Resources.ALLOWED_SYSTEMUI_ELEMENTS).contains(current);
    }

    // This string array contains all the Settings acceptable overlay packs
    public static Boolean allowedSettingsOverlay(String current) {
        return Arrays.asList(Resources.ALLOWED_SETTINGS_ELEMENTS).contains(current);
    }

    // This string array contains all the SystemUI acceptable sound files
    public static Boolean allowedUISound(String targetValue) {
        return Arrays.asList(Resources.ALLOWED_UI_THEMABLE_SOUNDS).contains(targetValue);
    }

    // This string array contains all the legacy allowed folders
    public static Boolean allowedForLegacy(String targetValue) {
        return Arrays.asList(Resources.ALLOWED_LEGACY_ASSETS).contains(targetValue);
    }

    // This string array contains all blacklisted app for theme
    public static Boolean allowedAppOverlay(String targetValue) {
        return !Arrays.asList(Resources.BLACKLIST_THEME_TARGET_APPS).contains(targetValue);
    }

    // This method determines whether a specified package is installed
    public static boolean isPackageInstalled(Context context, String package_name) {
        return isPackageInstalled(context, package_name, true);
    }

    // This method determines whether a specified package is installed (enabled OR disabled)
    public static boolean isPackageInstalled(
            Context context,
            String package_name,
            boolean enabled) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(package_name, 0);
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            if (enabled) return ai.enabled;
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
        assert manager != null;
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
            android.content.res.Resources resources = context.getResources();
            int drawablePointer = resources.getIdentifier(
                    resource_name, // Drawable name explicitly defined
                    resource_type, // Declared icon is a drawable, indeed.
                    package_Name); // Icon pack package name
            return drawablePointer != 0;
        } catch (Exception e) {
            return false;
        }
    }

    // This method converts a vector drawable into a bitmap object
    public static Bitmap getBitmapFromVector(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap getBitmapFromDrawable(Drawable drawable) {
        Bitmap bitmap = null;
        if (drawable instanceof VectorDrawable) {
            bitmap = References.getBitmapFromVector(drawable);
        } else if (drawable instanceof BitmapDrawable
                | drawable instanceof ShapeDrawable) {
            //noinspection ConstantConditions
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof AdaptiveIconDrawable) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // First we must get the top and bottom layers of the drawable
                Drawable backgroundDrawable = ((AdaptiveIconDrawable) drawable).getBackground();
                Drawable foregroundDrawable = ((AdaptiveIconDrawable) drawable).getForeground();

                // Then we have to set up the drawable array to format these as an instantiation
                Drawable[] drawableArray = new Drawable[2];
                drawableArray[0] = backgroundDrawable;
                drawableArray[1] = foregroundDrawable;

                // We then have to create a layered drawable based on the drawable array
                LayerDrawable layerDrawable = new LayerDrawable(drawableArray);

                // Now set up the width and height of the output
                int width = layerDrawable.getIntrinsicWidth();
                int height = layerDrawable.getIntrinsicHeight();

                // Formulate the bitmap properly
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                // Draw the canvas
                Canvas canvas = new Canvas(bitmap);

                // Finalize
                layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                layerDrawable.draw(canvas);
            }
        }
        return bitmap;
    }

    // This method obtains the application icon for a specified package
    public static Drawable grabAppIcon(Context context, String package_name) {
        try {
            Drawable icon;
            if (allowedSystemUIOverlay(package_name)) {
                icon = context.getPackageManager().getApplicationIcon("com.android.systemui");
            } else if (allowedSettingsOverlay(package_name)) {
                icon = context.getPackageManager().getApplicationIcon("com.android.settings");
            } else {
                icon = context.getPackageManager().getApplicationIcon(package_name);
            }
            return icon;
        } catch (Exception e) {
            // Suppress warning
        }
        if (package_name != null &&
                package_name.equals(INTERFACER_PACKAGE) &&
                !checkOMS(context)) {
            return context.getDrawable(R.mipmap.main_launcher);
        } else {
            return context.getDrawable(R.drawable.default_overlay_icon);
        }
    }

    // This method obtains the overlay's compiler version
    public static int grabOverlaySubstratumVersion(Context context, String package_name) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                return appInfo.metaData.getInt(metadataOverlayVersion);
            }
        } catch (Exception e) {
            // Suppress warning
        }
        return 0;
    }

    // This method obtains the overlay parent icon for specified package, returns self package icon
    // if not found
    public static Drawable grabOverlayParentIcon(Context context, String package_name) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null &&
                    appInfo.metaData.getString(metadataOverlayParent) != null) {
                return grabAppIcon(context, appInfo.metaData.getString(metadataOverlayParent));
            }
        } catch (Exception e) {
            // Suppress warning
        }
        return grabAppIcon(context, package_name);
    }

    public static List<ResolveInfo> getThemes(Context context) {
        // Scavenge through the packages on the device with specific substratum metadata in
        // their manifest
        PackageManager packageManager = context.getPackageManager();
        return packageManager.queryIntentActivities(new Intent(SUBSTRATUM_THEME),
                PackageManager.GET_META_DATA);
    }

    public static ArrayList<String> getThemesArray(Context context) {
        ArrayList<String> returnArray = new ArrayList<>();
        List<ResolveInfo> themesResolveInfo = getThemes(context);
        for (int i = 0; i < themesResolveInfo.size(); i++) {
            returnArray.add(themesResolveInfo.get(i).activityInfo.packageName);
        }
        return returnArray;
    }

    // Run shell command and return a StringBuilder of the output
    @SuppressWarnings("SameParameterValue")
    public static StringBuilder runShellCommand(String input) {
        try {
            Process shell = Runtime.getRuntime().exec(input);
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(shell.getInputStream()));

            StringBuilder returnString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                returnString.append(line).append("\n");
            }
            return returnString;
        } catch (Exception e) {
            // Suppress warning
        }
        return null;
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

    public static int grabAppVersionCode(Context mContext, String packageName) {
        try {
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(packageName, 0);
            return pInfo.versionCode;
        } catch (Exception e) {
            // Suppress warning
        }
        return 0;
    }

    public static String grabThemeVersion(Context mContext, String package_name) {
        try {
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(package_name, 0);
            return pInfo.versionName + " (" + pInfo.versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            // Suppress warning
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
            // Suppress warning
        }
        return null;
    }

    // Grab specified metadats
    public static String getOverlayMetadata(
            Context mContext,
            String package_name,
            String metadata) {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null && appInfo.metaData.getString(metadata) != null) {
                return appInfo.metaData.getString(metadata);
            }
        } catch (Exception e) {
            // Suppress warning
        }
        return null;
    }

    // Grab specified metadats
    public static Boolean getOverlayMetadata(
            Context mContext,
            String package_name,
            String metadata,
            Boolean defaultValue) {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                return appInfo.metaData.getBoolean(metadata);
            }
        } catch (Exception e) {
            // Suppress warning
        }
        return defaultValue;
    }

    // Grab specified metadats
    public static int getOverlaySubstratumVersion(
            Context mContext,
            String package_name,
            String metadata) {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                return appInfo.metaData.getInt(metadata);
            }
        } catch (Exception e) {
            // Suppress warning
        }
        return 0;
    }

    // Grab any resource from any package
    private static int getResource(Context mContext,
                                   String package_name,
                                   String resourceName,
                                   String type) {
        try {
            android.content.res.Resources res =
                    mContext.getPackageManager().getResourcesForApplication(package_name);
            return res.getIdentifier(
                    package_name + ":" + type + "/" + resourceName,
                    type,
                    package_name);
        } catch (Exception e) {
            // Suppress warning
        }
        return 0;
    }

    // Grab Color Resource
    public static int grabColorResource(Context mContext, String package_name, String colorName) {
        return getResource(mContext, package_name, colorName, "color");
    }

    // Grab Theme Changelog
    public static String[] grabThemeChangelog(Context mContext, String package_name) {
        try {
            android.content.res.Resources res =
                    mContext.getPackageManager().getResourcesForApplication(package_name);
            int array_position = getResource(mContext, package_name, resourceChangelog, "array");
            return res.getStringArray(array_position);
        } catch (Exception e) {
            // Suppress warning
        }
        return null;
    }

    // Grab Theme Hero Image
    public static Drawable grabPackageHeroImage(Context mContext, String package_name) {
        android.content.res.Resources res;
        Drawable hero = mContext.getDrawable(android.R.color.transparent); // Initialize to be clear
        try {
            res = mContext.getPackageManager().getResourcesForApplication(package_name);
            int resourceId = res.getIdentifier(
                    package_name + ":drawable/" + heroImageResourceName, null, null);
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
        PackageManager pm = mContext.getPackageManager();
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

    // Grab Theme Ready Metadata
    public static String grabThemeReadyVisibility(Context mContext, String package_name) {
        return getOverlayMetadata(mContext, package_name, metadataThemeReady);
    }

    // Grab Theme Plugin Metadata
    public static String grabPackageTemplateVersion(Context mContext, String package_name) {
        String template_version = getOverlayMetadata(mContext, package_name, metadataVersion);
        if (template_version != null) {
            return mContext.getString(R.string.plugin_template) + ": " + template_version;
        }
        return null;
    }

    // Grab Overlay Parent
    public static String grabOverlayParent(Context mContext, String package_name) {
        return getOverlayMetadata(mContext, package_name, metadataOverlayParent);
    }

    // Grab Overlay Target
    public static String grabOverlayTarget(Context mContext, String package_name) {
        return getOverlayMetadata(mContext, package_name, metadataOverlayTarget);
    }

    static int hashPassthrough(Context context) {
        if (hashValue != 0) {
            return hashValue;
        }
        try {
            @SuppressLint("PackageManagerGetSignatures")
            Signature[] sigs = context.getPackageManager().getPackageInfo(
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
                if (isPackageInstalled(context, check, false)) {
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
        if (checkPackageSupport(context)) {
            uncertified = true;
            return true;
        }
        uncertified = false;
        return false;
    }

    // Check for the denied packages if existing on the device
    private static boolean checkPackageSupport(Context context) {
        boolean blacklistedPackageFound = false;
        String[] blacklistedPackages = new String[]{
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
        for (String packageName : blacklistedPackages) {
            if (isPackageInstalled(context, packageName, false)) {
                blacklistedPackageFound = true;
                break;
            }
        }
        return blacklistedPackageFound;
    }

    // Check if notification is visible for the user
    public static boolean isNotificationVisible(Context mContext, int notification_id) {
        NotificationManager mNotificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        assert mNotificationManager != null;
        StatusBarNotification[] notifications = mNotificationManager.getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == notification_id) return true;
        }
        return false;
    }

    // Clear all of the current notifications of Substratum when a cache clear is called
    public static void clearAllNotifications(Context mContext) {
        NotificationManager mNotificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        assert mNotificationManager != null;
        StatusBarNotification[] notifications = mNotificationManager.getActiveNotifications();
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

    // Check if user application or not
    public static boolean isUserApp(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
            return (ai.flags & mask) == 0;
        } catch (PackageManager.NameNotFoundException e) {
            // Suppress warning
        }
        return false;
    }

    static void sendLocalizedKeyMessage(Context context,
                                        byte[] encryption_key,
                                        byte[] iv_encrypt_key) {
        Log.d("KeyRetrieval",
                "The system has completed the handshake for keys retrieval " +
                        "and is now passing it to the activity...");
        Intent intent = new Intent(KEY_RETRIEVAL);
        intent.putExtra("encryption_key", encryption_key);
        intent.putExtra("iv_encrypt_key", iv_encrypt_key);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendKillMessage(Context context) {
        Log.d("SubstratumKiller",
                "A crucial action has been conducted by the user and Substratum is now shutting " +
                        "down!");
        Intent intent = new Intent("MainActivity.KILL");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendAndromedaRefreshMessage(Context context) {
        Log.d("AndromedaReceiver",
                "Andromeda has been killed, notifying the MainActivity now!");
        Intent intent = new Intent("AndromedaReceiver.KILL");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendRefreshMessage(Context context) {
        Log.d("ThemeFragmentRefresher",
                "A theme has been modified, sending update signal to refresh the list!");
        Intent intent = new Intent("ThemeFragment.REFRESH");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendOverlayRefreshMessage(Context context) {
        Log.d("OverlayRefresher",
                "A theme has been modified, sending update signal to refresh the list!");
        Intent intent = new Intent("Overlay.REFRESH");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendRefreshManagerMessage(Context context) {
        Intent intent = new Intent(MANAGER_REFRESH);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    // Locate the proper launch intent for the themes
    @SuppressWarnings("SameParameterValue")
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
        }
        originalIntent.putExtra("hash_passthrough", hashPassthrough(mContext));
        originalIntent.putExtra("certified", !spreadYourWingsAndFly(mContext));
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo info = pm.getPackageInfo(currentTheme, PackageManager.GET_ACTIVITIES);
            ActivityInfo[] list = info.activities;
            for (ActivityInfo aList : list) {
                // We need to look for what the themer assigned the class to be! This is a dynamic
                // function that only launches the correct SubstratumLauncher class. Having it
                // hardcoded is bad.
                if (aList.name.equals(currentTheme + SUBSTRATUM_LAUNCHER_CLASS)) {
                    originalIntent.setComponent(
                            new ComponentName(
                                    currentTheme, currentTheme + SUBSTRATUM_LAUNCHER_CLASS));
                    return originalIntent;
                } else if (aList.name.equals(SUBSTRATUM_LAUNCHER_CLASS_PATH)) {
                    originalIntent.setComponent(
                            new ComponentName(
                                    currentTheme, SUBSTRATUM_LAUNCHER_CLASS_PATH));
                    return originalIntent;
                }
            }
        } catch (Exception e) {
            // Suppress warning
        }
        return null;
    }

    // Launch intent for a theme
    public static void launchTheme(Context mContext,
                                   String package_name,
                                   String theme_mode) {
        Intent theme_intent = themeIntent(
                mContext,
                package_name,
                theme_mode,
                TEMPLATE_THEME_MODE);
        mContext.startActivity(theme_intent);
    }

    // Key return of a theme
    public static void grabThemeKeys(Context mContext, String package_name) {
        Intent theme_intent = themeIntent(
                mContext,
                package_name,
                null,
                TEMPLATE_GET_KEYS);
        try {
            mContext.startActivity(theme_intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startKeyRetrievalReceiver(Context context) {
        try {
            IntentFilter intentGetKeys = new IntentFilter(TEMPLATE_RECEIVE_KEYS);
            context.getApplicationContext().registerReceiver(
                    new KeyRetriever(), intentGetKeys);

            Log.d(SUBSTRATUM_LOG,
                    "Successfully registered key retrieval receiver!");
        } catch (Exception e) {
            Log.e(SUBSTRATUM_LOG,
                    "Failed to register key retrieval receiver...");
        }
    }

    public static Intent themeIntent(Context mContext,
                                     String package_name,
                                     String theme_mode,
                                     String actionIntent) {
        boolean should_debug = projekt.substratum.BuildConfig.DEBUG;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (should_debug) Log.d("ThemeLauncher", "Creating new intent...");
        Intent intentActivity;
        if (actionIntent.equals(TEMPLATE_GET_KEYS)) {
            intentActivity = new Intent();
        } else {
            intentActivity = new Intent(mContext, ThemeLaunchActivity.class);
        }
        intentActivity.putExtra("package_name", package_name);
        if (should_debug) Log.d("ThemeLauncher", "Assigning action to intent...");
        intentActivity.setAction(actionIntent);
        if (should_debug) Log.d("ThemeLauncher", "Assigning package name to intent...");
        intentActivity.setPackage(package_name);
        intentActivity.putExtra("calling_package_name", mContext.getPackageName());
        if (should_debug) Log.d("ThemeLauncher", "Checking for theme system type...");
        intentActivity.putExtra("oms_check", !checkOMS(mContext));
        intentActivity.putExtra("theme_mode", theme_mode);
        intentActivity.putExtra("notification", false);
        if (should_debug) Log.d("ThemeLauncher", "Obtaining APK signature hash...");
        intentActivity.putExtra("hash_passthrough", hashPassthrough(mContext));
        if (should_debug) Log.d("ThemeLauncher", "Checking for certification...");
        intentActivity.putExtra("certified", prefs.getBoolean("complexion", true));
        if (should_debug) Log.d("ThemeLauncher", "Starting Activity...");
        return intentActivity;
    }

    // Begin check if device is running on the latest theme interface
    public static boolean checkThemeInterfacer(Context context) {
        if (context == null) {
            Log.e(SUBSTRATUM_LOG,
                    "activity has been destroyed, cannot check if interfacer is used");
            return false;
        }
        return getThemeInterfacerPackage(context) != null;
    }

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
                "andromeda_exp_fp_" + grabAppVersionCode(context, ANDROMEDA_PACKAGE), "0");
        String installer = prefs.getString("andromeda_installer", "o");

        boolean andromedaPresent = isAndromedaDevice(context);
        andromedaPresent &= installer.equals(PLAY_STORE_PACKAGE_NAME);
        andromedaPresent &= fingerprint.toUpperCase(Locale.getDefault())
                .equals(expFingerprint.toUpperCase(Locale.getDefault()));
        return andromedaPresent;
    }

    public static boolean isAndromedaDevice(Context context) {
        if (context == null) {
            Log.e(SUBSTRATUM_LOG,
                    "activity has been destroyed, cannot check if andromeda is used");
            return false;
        }

        return checkOreo() && getAndromedaPackage(context) != null;
    }

    // Begin check if device is running on the latest theme interface
    public static boolean isBinderInterfacer(Context context) {
        PackageInfo packageInfo = getThemeInterfacerPackage(context);
        return packageInfo != null && packageInfo.versionCode >= 60;
    }

    // Check if the system is of the Samsung variant
    public static boolean isSamsung(Context context) {
        if (FORCE_SAMSUNG_VARIANT) return true;
        boolean isTouchWiz = isSamsungDevice(context);
        if (!isTouchWiz) return false;

        SharedPreferences prefs =
                context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);

        boolean debuggingValue = prefs.getBoolean("sungstratum_debug", true);
        boolean installer = prefs.getBoolean("sungstratum_installer", false);
        String fingerprint = prefs.getString("sungstratum_fp", "0");
        String expFingerprint = prefs.getString(
                "sungstratum_exp_fp_" + grabAppVersionCode(context, SST_ADDON_PACKAGE), "o");
        String liveInstaller = PackageAnalytics.getPackageInstaller(context, SST_ADDON_PACKAGE);
        boolean liveInstallerValidity = liveInstaller != null &&
                liveInstaller.equals(PLAY_STORE_PACKAGE_NAME);

        boolean sungstratumPresent = !debuggingValue;
        sungstratumPresent &= installer;
        sungstratumPresent &= fingerprint.toUpperCase(
                Locale.getDefault()).equals(
                expFingerprint.toUpperCase(Locale.getDefault()));
        sungstratumPresent &= liveInstallerValidity;
        return sungstratumPresent;
    }

    // Check if the system is of the Samsung variant
    public static boolean isSamsungDevice(Context context) {
        if (context != null) {
            if (FORCE_SAMSUNG_VARIANT) return true;
            List<String> listOfFeatures =
                    Arrays.asList(context.getPackageManager().getSystemSharedLibraryNames());
            return listOfFeatures.contains("touchwiz");
        } else {
            return false;
        }
    }

    // Check if theme is Samsung supported
    public static boolean isSamsungTheme(Context context, String package_name) {
        return getOverlayMetadata(context, package_name, metadataSamsungSupport, false);
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

    // Comparing lists
    public static boolean stringContainsItemFromList(String inputStr, String[] items) {
        return Arrays.stream(items).parallel().anyMatch(inputStr::contains);
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
    private static boolean getIntents(Context context, String trigger) {
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

    private static PackageInfo getAndromedaPackage(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(ANDROMEDA_PACKAGE, 0);
        } catch (Exception e) {
            // Andromeda was not installed
        }
        return null;
    }

    public static PackageInfo getThemeInterfacerPackage(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(INTERFACER_PACKAGE, 0);
        } catch (Exception e) {
            // Theme Interfacer was not installed
        }
        return null;
    }

    public static boolean needsRecreate(Context context, ArrayList<String> list) {
        for (String o : list) {
            if (o.equals("android") || o.equals("projekt.substratum")) {
                return false;
            }
        }
        return checkOMS(context);
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
                                                                  HashMap packages,
                                                                  String home_type,
                                                                  String search_filter) {
        try {
            List<ResolveInfo> listOfThemes = getThemes(context);
            for (ResolveInfo ri : listOfThemes) {
                String packageName = ri.activityInfo.packageName;
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        packageName, PackageManager.GET_META_DATA);

                Boolean can_continue = true;
                if (appInfo.metaData.getString(metadataName) != null &&
                        appInfo.metaData.getString(metadataAuthor) != null) {
                    if (search_filter != null && search_filter.length() > 0) {
                        String name = appInfo.metaData.getString(metadataName) +
                                " " + appInfo.metaData.getString(metadataAuthor);
                        if (!name.toLowerCase(Locale.getDefault()).contains(
                                search_filter.toLowerCase(Locale.getDefault()))) {
                            can_continue = false;
                        }
                    }
                }
                if (can_continue) {
                    Context otherContext = context.createPackageContext(packageName, 0);
                    if (home_type.equals(wallpaperFragment)) {
                        if (appInfo.metaData.getString(metadataWallpapers) != null) {
                            String[] data = {appInfo.metaData.getString(metadataAuthor),
                                    packageName};
                            packages.put(appInfo.metaData.getString(metadataName), data);
                        }
                    } else {
                        if (home_type.length() == 0) {
                            String[] data = {appInfo.metaData.getString(metadataAuthor),
                                    packageName};
                            packages.put(appInfo.metaData.getString(metadataName), data);
                            Log.d(PACKAGE_TAG,
                                    "Loaded Substratum Theme: [" + packageName + "]");
                            if (References.ENABLE_PACKAGE_LOGGING)
                                PackageAnalytics.logPackageInfo(context, packageName);
                        } else {
                            try {
                                try (ZipFile zf = new ZipFile(
                                        otherContext.getApplicationInfo().sourceDir)) {
                                    for (Enumeration<? extends ZipEntry> e = zf.entries();
                                         e.hasMoreElements(); ) {
                                        ZipEntry ze = e.nextElement();
                                        String name = ze.getName();
                                        if (name.startsWith("assets/" + home_type + "/")) {
                                            String[] data = {
                                                    appInfo.metaData.getString(metadataAuthor),
                                                    packageName};
                                            packages.put(
                                                    appInfo.metaData.getString(metadataName),
                                                    data);
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(SUBSTRATUM_LOG,
                                        "Unable to find package identifier");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Suppress warning
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

        // We need to clone the InputStream so that we can ensure that the name and color are
        // mutually exclusive
        byte[] byteArray;
        try {
            byteArray = IOUtils.toByteArray(overlay);
        } catch (IOException e) {
            Log.e(SUBSTRATUM_LOG, "Unable to clone InputStream");
            return null;
        }

        try (InputStream clone1 = new ByteArrayInputStream(byteArray);
             InputStream clone2 = new ByteArrayInputStream(byteArray)) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hex;
    }

    public static boolean isPackageDebuggable(Context context, String packageName) {
        X500Principal DEBUG_1 = new X500Principal("C=US,O=Android,CN=Android Debug");
        X500Principal DEBUG_2 = new X500Principal("CN=Android Debug,O=Android,C=US");
        boolean debuggable = false;

        try {
            @SuppressLint("PackageManagerGetSignatures")
            PackageInfo pinfo = context.getPackageManager()
                    .getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            Signature signatures[] = pinfo.signatures;
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            for (Signature signature : signatures) {
                ByteArrayInputStream stream = new ByteArrayInputStream(signature.toByteArray());
                X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);
                debuggable = cert.getSubjectX500Principal().equals(DEBUG_1) ||
                        cert.getSubjectX500Principal().equals(DEBUG_2);
                if (debuggable) break;
            }
        } catch (PackageManager.NameNotFoundException | CertificateException e) {
            //cacheable variable will remain false
        }
        return debuggable;
    }

    // Save a text file from LogChar
    public static void writeLogCharFile(String packageName, String data) {
        try {
            Calendar c = Calendar.getInstance();
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String formattedDate = df.format(c.getTime());

            File logcharFolder = new File(Environment.getExternalStorageDirectory() +
                    File.separator + "substratum" + File.separator + "LogCharReports");
            if (!logcharFolder.exists() && logcharFolder.mkdirs()) {
                Log.d("LogChar Utility", "Created LogChar directory!");
            }

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                    new File(logcharFolder.getAbsolutePath() + File.separator +
                            packageName + "_" + formattedDate + ".txt")));
            bufferedWriter.write(data);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Markdown extends AsyncTask<Void, Void, Void> {
        @SuppressLint("StaticFieldLeak")
        private Context context;
        private SharedPreferences prefs;

        public Markdown(Context context, SharedPreferences prefs) {
            this.context = context;
            this.prefs = prefs;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected Void doInBackground(Void... sUrl) {
            prefs.edit().putBoolean("complexion",
                    !References.spreadYourWingsAndFly(context) &&
                            References.hashPassthrough(context) != 0).apply();
            return null;
        }
    }

    // This class serves to update the theme's cache on demand
    public static class SubstratumThemeUpdate extends AsyncTask<Void, Integer, String> {
        private final String TAG = "SubstratumThemeUpdate";
        private ProgressDialog progress;
        private String theme_name, theme_package, theme_mode;
        private Boolean launch = false;
        private Boolean cacheable = false;
        private Context mContext;
        private LocalBroadcastManager localBroadcastManager;
        private KeyRetrieval keyRetrieval;
        private Intent securityIntent;
        private Cipher cipher;
        private Handler handler = new Handler();
        private Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Waiting for encryption key handshake approval...");
                if (securityIntent != null) {
                    Log.d(TAG, "Encryption key handshake approved!");
                    handler.removeCallbacks(runnable);
                } else {
                    Log.d(TAG, "Encryption key still null...");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.postDelayed(this, 100);
                }
            }
        };

        public SubstratumThemeUpdate(Context mContext, String theme_package, String theme_name,
                                     String theme_mode) {
            this.mContext = mContext;
            this.theme_package = theme_package;
            this.theme_name = theme_name;
            this.theme_mode = theme_mode;
            this.cacheable = isPackageDebuggable(mContext, theme_package);
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
            if (cacheable) progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            if (progress != null) {
                progress.dismiss();
            }
            if (launch) {
                Toast.makeText(mContext, mContext.getString(R.string
                                .background_updated_toast),
                        Toast.LENGTH_SHORT).show();
                // At this point, we can safely assume that the theme has successfully extracted
                launchTheme(mContext, theme_package, theme_mode);
            } else if (!cacheable) {
                Toast.makeText(mContext, mContext.getString(R.string.
                                background_updated_toast_rejected),
                        Toast.LENGTH_SHORT).show();
                // Just in case.
                new CacheCreator().wipeCache(mContext, theme_package);
            } else {
                Toast.makeText(mContext, mContext.getString(R.string
                                .background_updated_toast_cancel),
                        Toast.LENGTH_SHORT).show();
                // We don't want this cache anymore, delete it from the system completely
                new CacheCreator().wipeCache(mContext, theme_package);
            }
        }

        @Override
        protected String doInBackground(Void... Params) {
            if (!cacheable) return null;

            String encrypt_check =
                    References.getOverlayMetadata(mContext, theme_package, metadataEncryption);

            if (encrypt_check != null && encrypt_check.equals(metadataEncryptionValue)) {
                Log.d(TAG, "This overlay for " +
                        References.grabPackageName(mContext, theme_package) +
                        " is encrypted, passing handshake to the theme package...");

                References.grabThemeKeys(mContext, theme_package);

                keyRetrieval = new KeyRetrieval();
                IntentFilter if1 = new IntentFilter(KEY_RETRIEVAL);
                localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
                localBroadcastManager.registerReceiver(keyRetrieval, if1);

                int counter = 0;
                handler.postDelayed(runnable, 100);
                while (securityIntent == null && counter < 5) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    counter++;
                }
                if (counter > 5) {
                    Log.e(TAG, "Could not receive handshake in time...");
                    return null;
                }

                if (securityIntent != null) {
                    try {
                        byte[] encryption_key =
                                securityIntent.getByteArrayExtra("encryption_key");
                        byte[] iv_encrypt_key =
                                securityIntent.getByteArrayExtra("iv_encrypt_key");

                        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        cipher.init(
                                Cipher.DECRYPT_MODE,
                                new SecretKeySpec(encryption_key, "AES"),
                                new IvParameterSpec(iv_encrypt_key)
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }

            launch = new CacheCreator().initializeCache(mContext, theme_package, cipher);
            return null;
        }

        class KeyRetrieval extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                securityIntent = intent;
            }
        }
    }

    public static class KeyRetriever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            sendLocalizedKeyMessage(
                    context,
                    intent.getByteArrayExtra("encryption_key"),
                    intent.getByteArrayExtra("iv_encrypt_key"));
        }
    }
}
