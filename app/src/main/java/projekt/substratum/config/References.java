package projekt.substratum.config;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
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
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.iid.FirebaseInstanceId;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import projekt.substratum.BuildConfig;
import projekt.substratum.R;
import projekt.substratum.util.AOPTCheck;
import projekt.substratum.util.CacheCreator;
import projekt.substratum.util.ReadOverlaysFile;
import projekt.substratum.util.Root;

public class References {

    // These are specific log tags for different classes
    public static final Boolean ENABLE_SIGNING = true;
    public static final Boolean ENABLE_ROOT_CHECK = false;
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
    // Sounds processing methods
    private static final String SYSTEM_MEDIA_PATH = "/system/media/audio";
    private static final String SYSTEM_ALARMS_PATH =
            SYSTEM_MEDIA_PATH + File.separator + "alarms";
    private static final String SYSTEM_RINGTONES_PATH =
            SYSTEM_MEDIA_PATH + File.separator + "ringtones";
    private static final String SYSTEM_NOTIFICATIONS_PATH =
            SYSTEM_MEDIA_PATH + File.separator + "notifications";
    private static final String MEDIA_CONTENT_URI = "content://media/internal/audio/media";
    private static final String SYSTEM_CONTENT_URI = "content://settings/global";
    // Lucky Patcher's Package Name
    public static String lp_package_identifier = "com.android.vending.billing" +
            ".InAppBillingService.LOCK";
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

        File om = new File("/system/bin/om");
        if (om.exists()) {
            prefs.edit().putBoolean("oms_state", true).apply();
            prefs.edit().putInt("oms_version", 3).apply();
            Log.d(References.SUBSTRATUM_LOG, "Initializing Substratum with the third iteration of" +
                    " the Overlay Manager Service...");
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

    // Future masquerade methods
    public static void enableOverlay(Context context, ArrayList<String> overlays) {
        if (checkMasquerade(context) >= 22) {
            MasqueradeService.enableOverlays(context, overlays);
        } else {
            String commands = enableOverlay();
            int size = overlays.size();
            for (int i = 0; i < size; i++) {
                commands += " " + overlays.get(i);
            }
            new References.ThreadRunner().execute(commands);
            if (shouldRestartUi(context, overlays)) References.restartSystemUI(context);
        }
    }

    public static void disableOverlay(Context context, ArrayList<String> overlays) {
        if (checkMasquerade(context) >= 22) {
            MasqueradeService.disableOverlays(context, overlays);
        } else {
            String commands = disableOverlay();
            int size = overlays.size();
            for (int i = 0; i < size; i++) {
                commands += " " + overlays.get(i);
            }
            new References.ThreadRunner().execute(commands);
            if (shouldRestartUi(context, overlays)) restartSystemUI(context);
        }
    }

    public static void setPriority(Context context, ArrayList<String> overlays) {
        if (checkMasquerade(context) >= 22) {
            MasqueradeService.setPriority(context, overlays);
        } else {
            String commands = "";
            int size = overlays.size();
            for (int i = 0; i < size - 1; i++) {
                String parentName = overlays.get(i);
                String packageName = overlays.get(i + 1);
                commands += (commands.isEmpty() ? "" : " && ") + setPriority() + " " + packageName +
                        " " + parentName;
            }
            new References.ThreadRunner().execute(commands);
            if (shouldRestartUi(context, overlays)) restartSystemUI(context);
        }
    }

    public static void disableAll(Context context) {
        if (checkMasquerade(context) >= 22) {
            String[] command = {"/data/system/overlays.xml", "5"};
            List<String> list = ReadOverlaysFile.main(context, command);
            MasqueradeService.disableOverlays(context, new ArrayList<>(list));
        } else {
            new ThreadRunner().execute(disableAllOverlays());
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
        new AOPTCheck().injectAOPT(context, true);
        prefs.edit().putBoolean("aopt_debug", false).apply();
        prefs.edit().putBoolean("display_old_themes", true).apply();
        prefs.edit().putBoolean("force_english", false).apply();
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
        String[] checker = checkPackageSupport();
        for (String check : checker) {
            if (References.isPackageInstalled(context, check)) {
                return true;
            }
        }
        return false;
    }

    // Check for the denied packages if existing on the device
    public static String[] checkPackageSupport() {
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
            if (notification.getPackageName() == mContext.getPackageName()) {
                mNotificationManager.cancel(notification.getId());
            }
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
            for (int i = 0; i < list.length; i++) {
                // We need to look for what the themer assigned the class to be! This is a dynamic
                // function that only launches the correct SubstratumLauncher class. Having it
                // hardcoded is bad.
                if (list[i].name.equals(currentTheme + ".SubstratumLauncher")) {
                    originalIntent.setComponent(
                            new ComponentName(currentTheme, currentTheme + ".SubstratumLauncher"));
                    return originalIntent;
                } else if (list[i].name.equals("substratum.theme.template.SubstratumLauncher")) {
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

            String integrityCheck2 = new AOPTCheck().checkAOPTIntegrity(mContext);
            if (integrityCheck2 != null &&
                    (integrityCheck2.equals(mContext.getString(R.string.aapt_version)) ||
                            integrityCheck2.equals(mContext.getString(R.string.aopt_version)))) {
                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(mContext);
                prefs.edit().putString("compiler", "aapt").apply();
                if (initializer != null) {
                    mContext.startActivity(initializer);
                }
            } else {
                new AlertDialog.Builder(mContext)
                        .setCancelable(false)
                        .setIcon(R.drawable.dialog_warning_icon)
                        .setTitle(R.string.aopt_warning_title)
                        .setMessage(R.string.aopt_warning_text)
                        .setPositiveButton(R.string.dialog_ok, (dialog, i) -> dialog.cancel())
                        .show();
            }
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
                    data,
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

    // Begin check if device is running on the latest Masquerade
    public static Boolean checkMasqueradeJobService(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(MASQUERADE_PACKAGE, PackageManager.GET_SERVICES);
            ServiceInfo[] list = info.services;
            for (int i = 0; i < list.length; i++) {
                if (list[i].name.equals("masquerade.substratum.services.JobService")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Suppress warning
        }
        return false;
    }

    // Begin consolidation of IO commands in Substratum
    public static void copy(Context context, String source, String destination) {
        String dataDir = context.getDataDir().getAbsolutePath();
        String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        boolean needRoot = (!source.startsWith(dataDir) && !source.startsWith(externalDir) &&
                !source.startsWith("/system")) || (!destination.startsWith(dataDir) &&
                !destination.startsWith(externalDir) && !destination.startsWith("/system"));
        if (checkMasquerade(context) >= 22 && needRoot) {
            Log.d("CopyFunction", "using masquerade no-root operation for copying " + source +
                    " to " + destination);
            MasqueradeService.copy(context, source, destination);

            // Wait until copy success
            File file = new File(destination);
            try {
                int retryCount = 0;
                while (!file.exists() && retryCount < 5) {
                    Thread.sleep(1000);
                    retryCount++;
                }
                if (retryCount == 5) Log.d("CopyFunction", "Operation timeout");
                Log.d("CopyFunction", "Operation " + (file.exists() ? "success" : "failed"));
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        } else {
            copy(source, destination);
        }
    }

    public static void copyDir(Context context, String source, String destination) {
        String dataDir = context.getDataDir().getAbsolutePath();
        String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        boolean needRoot = (!source.startsWith(dataDir) && !source.startsWith(externalDir) &&
                !source.startsWith("/system")) || (!destination.startsWith(dataDir) &&
                !destination.startsWith(externalDir) && !destination.startsWith("/system"));
        if (checkMasquerade(context) >= 22 && needRoot) {
            copy(context, source, destination);
        } else {
            copyDir(source, destination);
        }
    }

    public static void delete(Context context, String directory) {
        delete(context, directory, true);
    }

    public static void delete(Context context, String directory, boolean deleteParent) {
        String dataDir = context.getDataDir().getAbsolutePath();
        String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        boolean needRoot = (!directory.startsWith(dataDir) && !directory.startsWith(externalDir) &&
                !directory.startsWith("/system"));
        if (checkMasquerade(context) >= 22 && needRoot) {
            Log.d("DeleteFunction", "using masquerade no-root operation for deleting " + directory);
            MasqueradeService.delete(context, directory, deleteParent);

            // Wait until delete success
            File file = new File(directory);
            try {
                int retryCount = 0;
                boolean notDone = (deleteParent && file.exists()) ||
                        (!deleteParent && file.list().length == 0);
                while (notDone && retryCount < 5) {
                    Thread.sleep(1000);
                    retryCount++;
                }
                if (retryCount == 5) Log.d("DeleteFunction", "Operation timeout");
                Log.d("DeleteFunction", "Operation " + (!file.exists() ? "success" : "failed"));
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        } else {
            delete(directory, deleteParent);
        }
    }

    public static void move(Context context, String source, String destination) {
        String dataDir = context.getDataDir().getAbsolutePath();
        String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        boolean needRoot = (!source.startsWith(dataDir) && !source.startsWith(externalDir) &&
                !source.startsWith("/system")) || (!destination.startsWith(dataDir) &&
                !destination.startsWith(externalDir) && !destination.startsWith("/system"));
        if (checkMasquerade(context) >= 22 && needRoot) {
            Log.d("MoveFunction", "using masquerade no-root operation for moving " + source +
                    " to " + destination);
            MasqueradeService.move(context, source, destination);

            // Wait until move success
            File file = new File(destination);
            try {
                int retryCount = 0;
                while (!file.exists() && retryCount < 5) {
                    Thread.sleep(1000);
                    retryCount++;
                }
                if (retryCount == 5) Log.d("MoveFunction", "Operation timeout");
                Log.d("MoveFunction", "Operation " + (file.exists() ? "success" : "failed"));
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        } else {
            move(source, destination);
        }
    }

    public static void createNewFolder(String foldername) {
        Log.d("CreateFolderFunction", "using no-root operation for creating " + foldername);
        File folder = new File(foldername);
        if (!folder.exists()) {
            Log.d("CreateFolderFunction", "operation " + (folder.mkdirs() ? "success" : "failed"));
            if (!folder.exists()) {
                Log.d("CreateFolderFunction", "using root operation for creating " + foldername);
                Root.runCommand("mkdir " + foldername);
            }
        } else {
            Log.d("CreateFolderFunction", "folder already exist");
        }
    }

    // These inner IO commands only be called by outer commands
    private static void copy(String source, String destination) {
        Log.d("CopyFunction", "using no-root operation for copying " + source + " to " +
                destination);
        File in = new File(source);
        File out = new File(destination);
        try {
            FileUtils.copyFile(in, out);
        } catch (IOException e) {
            Log.d("CopyFunction", "using no-root operation failed, fallback to root mode...");
            Root.runCommand("cp -f " + source + " " + destination);
        }
        Log.d("CopyFunction", "operation " + (out.exists() ? "success" : "failed"));
    }

    private static void copyDir(String source, String destination) {
        Log.d("CopyFunction", "using no-root operation for copying " + source + " to " +
                destination);
        File in = new File(source);
        File out = new File(destination);
        try {
            FileUtils.copyDirectory(in, out);
        } catch (IOException e) {
            Log.d("CopyFunction", "using no-root operation failed, fallback to root mode...");
            Root.runCommand("cp -rf " + source + " " + destination);
        }
        Log.d("CopyFunction", "operation " + (out.exists() ? "success" : "failed"));
    }

    private static void delete(String directory, boolean deleteParent) {
        Log.d("DeleteFunction", "using no-root operation for deleting " + directory);
        File dir = new File(directory);
        try {
            if (dir.isDirectory()) {
                for (File child : dir.listFiles()) {
                    FileUtils.forceDelete(child);
                }
                if (deleteParent) FileUtils.forceDelete(dir);
            } else {
                FileUtils.forceDelete(dir);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("DeleteFunction", "using no-root operation failed, fallback to root mode...");
            if (deleteParent) {
                Root.runCommand("rm -rf " + directory);
            } else {
                String command = "rm -rf ";
                if (dir.isDirectory()) {
                    for (File child : dir.listFiles()) {
                        command += child.getAbsolutePath() + " ";
                    }
                    Root.runCommand(command);
                } else {
                    Root.runCommand(command + directory);
                }
            }
        }
        Log.d("DeleteFunction", "operation " + (!dir.exists() ? "success" : "failed"));
    }

    private static void move(String source, String destination) {
        Log.d("MoveFunction", "using no-root operation for moving " + source + " to " +
                destination);
        File in = new File(source);
        File out = new File(destination);
        try {
            if (in.isFile()) {
                FileUtils.moveFile(in, out);
            } else {
                FileUtils.moveDirectory(in, out);
            }
        } catch (IOException e) {
            Log.d("MoveFunction", "using no-root operation failed, fallback to root mode... ");
            Root.runCommand("mv -f " + source + " " + destination);
        }
        Log.d("MoveFunction", "operation " + (!in.exists() && out.exists() ? "success" : "failed"));
    }

    // Begin consolidation of root commands in Substratum
    public static void adjustContentProvider(final String uri,
                                             final String topic, final String fileName) {
        Root.runCommand("content insert --uri " + uri + " " +
                "--bind name:s:" + topic + " --bind value:s:" + fileName);
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

    public static void installOverlay(Context context, String overlay) {
        if (checkMasquerade(context) >= 22) {
            // TODO: It should not be like this
            ArrayList<String> list = new ArrayList<>();
            list.add(overlay);
            MasqueradeService.installOverlays(context, list);
        } else {
            new ThreadRunner().execute("pm install -r " + overlay);
        }
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
        Root.runCommand("pkill -f " + MASQUERADE_PACKAGE);
    }

    public static void restartSystemUI(Context context) {
        if (checkMasquerade(context) >= 22) {
            MasqueradeService.restartSystemUI(context);
        } else {
            Root.runCommand("pkill -f com.android.systemui");
        }
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

    public static boolean shouldRestartUi(Context context, String overlay) {
        if (checkOMS(context)) {
            if (overlay.startsWith("com.android.systemui")) return true;
        }
        return false;
    }

    public static boolean shouldRestartUi(Context context, ArrayList<String> overlays) {
        if (checkOMS(context)) {
            for (String o : overlays) {
                if (o.startsWith("com.android.systemui")) return true;
            }
        }
        return false;
    }

    public static void uninstallOverlay(Context context, String overlay) {
        if (checkMasquerade(context) >= 22) {
            ArrayList<String> list = new ArrayList<>();
            list.add(overlay);
            MasqueradeService.uninstallOverlays(context, list, shouldRestartUi(context, overlay));
        } else {
            new ThreadRunner().execute("pm uninstall " + overlay);
            if (checkOMS(context) && shouldRestartUi(context, overlay)) restartSystemUI(context);
        }
    }

    public static void uninstallOverlay(Context context, ArrayList<String> overlays) {
        if (checkMasquerade(context) >= 22) {
            MasqueradeService.uninstallOverlays(context, overlays, shouldRestartUi(context,
                    overlays));
        } else {
            String command = "pm uninstall ";
            for (String packageName : overlays) {
                command += packageName + " ";
            }
            new ThreadRunner().execute(command);
            if (checkOMS(context) && shouldRestartUi(context, overlays)) restartSystemUI(context);
        }
    }

    public static void setWallpaper(Context context, String path, String which) throws IOException {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        switch (which) {
            case "home":
                // Set home screen wallpaper
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Get current lock screen wallpaper to be applied later
                    ParcelFileDescriptor lockFile = wallpaperManager
                            .getWallpaperFile(WallpaperManager.FLAG_LOCK);
                    InputStream input = new FileInputStream(lockFile.getFileDescriptor());
                    // Now apply the wallpapers
                    wallpaperManager.setStream(new FileInputStream(path), null, true,
                            WallpaperManager.FLAG_SYSTEM);
                    // Reapply previous lock screen wallpaper
                    wallpaperManager.setStream(input, null, true, WallpaperManager.FLAG_LOCK);
                } else {
                    wallpaperManager.setStream(new FileInputStream(path));
                }
                break;
            case "lock":
                // Set lock screen wallpaper
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setStream(new FileInputStream(path), null, true,
                            WallpaperManager.FLAG_LOCK);
                }
                break;
            case "all":
                // Apply both wallpaper
                wallpaperManager.setStream(new FileInputStream(path));
                break;
            default:
                Log.e("SetWallpaper", "wrong choice bud");
        }
    }

    public static void clearWallpaper(Context context, String which) throws IOException {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        switch (which) {
            case "home":
                // Clear home screen wallpaper
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Get current lock screen wallpaper to be applied later
                    ParcelFileDescriptor lockFile = wallpaperManager
                            .getWallpaperFile(WallpaperManager.FLAG_LOCK);
                    InputStream input = new FileInputStream(lockFile.getFileDescriptor());
                    // Clear home wallpaper
                    wallpaperManager.clear(WallpaperManager.FLAG_SYSTEM);
                    // Reapply lock screen wallpaper
                    wallpaperManager.setStream(input, null, true, WallpaperManager.FLAG_LOCK);
                } else {
                    wallpaperManager.clear();
                }
                break;
            case "lock":
                // clear lock screen wallpaper
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.clear(WallpaperManager.FLAG_LOCK);
                }
                break;
            case "all":
                // Clear both wallpaper
                wallpaperManager.clear();
                break;
            default:
                Log.e("ClearWallpaper", "wrong choice bud");
        }
    }

    public static void setBootAnimation(Context context, String themeDirectory) {
        String location = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/.substratum/bootanimation.zip";
        if (getDeviceEncryptionStatus(context) <= 1 && checkMasquerade(context) >= 22) {
            Log.d("BootAnimationHandler",
                    "No-root option has been enabled with the inclusion of " +
                            "masquerade v22+...");
            MasqueradeService.setBootAnimation(context, location);
        } else {
            Log.d("BootAnimationHandler", "Root option has been enabled");
            References.mountRW();
            References.setPermissions(755, themeDirectory);
            References.mountRW();
            References.move(context, location, themeDirectory + "/bootanimation.zip");
            References.mountRW();
            References.setPermissions(644, themeDirectory + "/bootanimation.zip");
            References.mountRWData();
            References.setContext(themeDirectory);
        }
    }

    public static void clearBootAnimation(Context context) {
        if (getDeviceEncryptionStatus(context) <= 1 && checkMasquerade(context) >= 22) {
            // OMS with no-root masquerade
            MasqueradeService.clearBootAnimation(context);
        } else if (getDeviceEncryptionStatus(context) <= 1 && !References.checkOMS(context)) {
            // Legacy unencrypted
            References.delete(context, "/data/system/theme/bootanimation.zip");
        } else {
            // Encrypted OMS and legacy
            References.mountRW();
            References.move(context, "/system/media/bootanimation-backup.zip",
                    "/system/media/bootanimation.zip");
            References.delete(context, "/system/addon.d/81-subsboot.sh");
        }
    }

    public static void setFonts(Context context, String theme_pid, String name) {
        if (checkOMS(context) && checkMasquerade(context) >= 22) {
            MasqueradeService.setFonts(context, theme_pid, name);
        } else {
            // oms pre rootless masq or legacy
            try {
                // Move the file from assets folder to a new working area
                Log.d("FontHandler", "Copying over the selected fonts to working " +
                        "directory...");

                File cacheDirectory = new File(context.getCacheDir(), "/FontCache/");
                if (!cacheDirectory.exists()) {
                    boolean created = cacheDirectory.mkdirs();
                    if (created) Log.d("FontHandler", "Successfully created cache folder!");
                }
                File cacheDirectory2 = new File(context.getCacheDir(), "/FontCache/" +
                        "FontCreator/");
                if (!cacheDirectory2.exists()) {
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("FontHandler", "Successfully created cache folder work " +
                            "directory!");
                } else {
                    References.delete(context, context.getCacheDir().getAbsolutePath() +
                            "/FontCache/FontCreator/");
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("FontHandler", "Successfully recreated cache folder work " +
                            "directory!");
                }

                // Copy the font.zip from assets/fonts of the theme's assets
                String sourceFile = name + ".zip";

                try {
                    Context otherContext = context.createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    try (InputStream inputStream = am.open("fonts/" + sourceFile);
                         OutputStream outputStream = new FileOutputStream(context.getCacheDir()
                                 .getAbsolutePath() + "/FontCache/" + sourceFile)) {
                        byte[] buffer = new byte[5120];
                        int length = inputStream.read(buffer);
                        while (length > 0) {
                            outputStream.write(buffer, 0, length);
                            length = inputStream.read(buffer);
                        }
                    }
                } catch (Exception e) {
                    Log.e("FontHandler", "There is no fonts.zip found within the assets " +
                            "of this theme!");
                }

                // Unzip the fonts to get it prepared for the preview
                String source = context.getCacheDir().getAbsolutePath() + "/FontCache/" +
                        sourceFile;
                String destination = context.getCacheDir().getAbsolutePath() +
                        "/FontCache/FontCreator/";

                try (ZipInputStream inputStream = new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(source)))) {
                    ZipEntry zipEntry;
                    int count;
                    byte[] buffer = new byte[8192];
                    while ((zipEntry = inputStream.getNextEntry()) != null) {
                        File file = new File(destination, zipEntry.getName());
                        File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                        if (!dir.isDirectory() && !dir.mkdirs())
                            throw new FileNotFoundException("Failed to ensure directory: " +
                                    dir.getAbsolutePath());
                        if (zipEntry.isDirectory())
                            continue;
                        try (FileOutputStream outputStream = new FileOutputStream(file)) {
                            while ((count = inputStream.read(buffer)) != -1)
                                outputStream.write(buffer, 0, count);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("FontHandler",
                            "An issue has occurred while attempting to decompress this archive.");
                }

                // Copy all the system fonts to /data/system/theme/fonts
                File dataSystemThemeDir = new File("/data/system/theme");
                if (!dataSystemThemeDir.exists()) {
                    References.mountRWData();
                    References.createNewFolder("/data/system/theme/");
                }
                File dataSystemThemeFontsDir = new File("/data/system/theme/fonts");
                if (!dataSystemThemeFontsDir.exists()) {
                    References.mountRWData();
                    References.createNewFolder("/data/system/theme/fonts");
                } else {
                    References.delete(context, "/data/system/theme/fonts/");
                    References.mountRWData();
                    References.createNewFolder("/data/system/theme/fonts");
                }

                // Copy font configuration file (fonts.xml) to the working directory
                File fontsConfig = new File(context.getCacheDir().getAbsolutePath() +
                        "/FontCache/FontCreator/fonts.xml");
                if (!fontsConfig.exists()) {
                    AssetManager assetManager = context.getAssets();
                    final String filename = "fonts.xml";
                    try (InputStream in = assetManager.open(filename);
                         OutputStream out = new FileOutputStream(context.getCacheDir()
                                 .getAbsolutePath() + "/FontCache/FontCreator/" + filename)) {
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    } catch (IOException e) {
                        Log.e("FontHandler", "Failed to move font configuration file to working " +
                                "directory!");
                    }
                }

                References.copy(context, "/system/fonts/*", "/data/system/theme/fonts/");

                // Copy all the files from work directory to /data/system/theme/fonts
                References.copy(context, context.getCacheDir().getAbsolutePath() +
                        "/FontCache/FontCreator/*", "/data/system/theme/fonts/");

                // Check for correct permissions and system file context integrity.
                References.mountRWData();
                References.setPermissions(755, "/data/system/theme/");
                References.setPermissionsRecursively(747, "/data/system/theme/fonts/");
                References.setPermissions(775, "/data/system/theme/fonts/");
                References.mountROData();
                References.setContext("/data/system/theme");
                References.setProp("sys.refresh_theme", "1");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void clearFonts(Context context) {
        if (checkOMS(context) && checkMasquerade(context) >= 22) {
            MasqueradeService.clearFonts(context);
        } else {
            // oms with pre rootless masq and legacy
            References.delete(context, "/data/system/theme/fonts/");
            if (!checkOMS(context)) References.restartSystemUI(context);
        }
    }

    public static boolean[] setSounds(Context context, String theme_pid, String name) {
        boolean has_failed = false;
        boolean ringtone = false;

        if (checkOMS(context) && checkMasquerade(context) >= 22) {
            MasqueradeService.setThemedSounds(context, theme_pid, name);
            ringtone = true; // Always assume that the process is succeeded;
        } else {
            // Move the file from assets folder to a new working area
            Log.d("SoundsHandler", "Copying over the selected sounds to working directory...");

            File cacheDirectory = new File(context.getCacheDir(), "/SoundsCache/");
            if (!cacheDirectory.exists()) {
                boolean created = cacheDirectory.mkdirs();
                if (created) Log.d("SoundsHandler", "Sounds folder created");
            }
            File cacheDirectory2 = new File(context.getCacheDir(), "/SoundsCache/" +
                    "SoundsInjector/");
            if (!cacheDirectory2.exists()) {
                boolean created = cacheDirectory2.mkdirs();
                if (created) Log.d("SoundsHandler", "Sounds work folder created");
            } else {
                References.delete(context, context.getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/");
                boolean created = cacheDirectory2.mkdirs();
                if (created) Log.d("SoundsHandler", "Sounds work folder recreated");
            }

            String sounds = name;

            if (!has_failed) {
                Log.d("SoundsHandler", "Analyzing integrity of sounds archive file...");
                try {
                    Context otherContext = context.createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    try (InputStream inputStream = am.open("audio/" + sounds + ".zip");
                         OutputStream outputStream = new FileOutputStream(context.getCacheDir()
                                 .getAbsolutePath() + "/SoundsCache/SoundsInjector/" +
                                 sounds + ".zip")) {

                        // was CopyStream
                        byte[] buffer = new byte[5120];
                        int length = inputStream.read(buffer);
                        while (length > 0) {
                            outputStream.write(buffer, 0, length);
                            length = inputStream.read(buffer);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("SoundsHandler", "There is no sounds.zip found within the assets " +
                            "of this theme!");
                    has_failed = true;
                }

                // Rename the file
                File workingDirectory = new File(context.getCacheDir()
                        .getAbsolutePath() + "/SoundsCache/SoundsInjector/");
                File from = new File(workingDirectory, sounds + ".zip");
                sounds = sounds.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+",
                        "");
                File to = new File(workingDirectory, sounds + ".zip");
                boolean rename = from.renameTo(to);
                if (rename)
                    Log.d("SoundsHandler", "Sounds archive successfully moved to new " +
                            "directory");

                // Unzip the sounds archive to get it prepared for the preview
                String source = context.getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/" + sounds + ".zip";
                String destination = context.getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/";
                try (ZipInputStream inputStream = new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(source)))) {
                    ZipEntry zipEntry;
                    int count;
                    byte[] buffer = new byte[8192];
                    while ((zipEntry = inputStream.getNextEntry()) != null) {
                        File file = new File(destination, zipEntry.getName());
                        File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                        if (!dir.isDirectory() && !dir.mkdirs())
                            throw new FileNotFoundException("Failed to ensure directory: " +
                                    dir.getAbsolutePath());
                        if (zipEntry.isDirectory())
                            continue;
                        try (FileOutputStream outputStream = new FileOutputStream(file)) {
                            while ((count = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, count);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("SoundsHandler",
                            "An issue has occurred while attempting to decompress this archive.");
                }
            }

            if (!has_failed) {
                Log.d("SoundsHandler", "Moving sounds to theme directory " +
                        "and setting correct contextual parameters...");

                File themeDirectory = new File("/data/system/theme/");
                if (!themeDirectory.exists()) {
                    References.mountRWData();
                    References.createNewFolder("/data/system/theme/");
                    References.setPermissions(755, "/data/system/theme/");
                    References.mountROData();
                }
                File audioDirectory = new File("/data/system/theme/audio/");
                if (!audioDirectory.exists()) {
                    References.mountRWData();
                    References.createNewFolder("/data/system/theme/audio/");
                    References.setPermissions(755, "/data/system/theme/audio/");
                    References.mountROData();
                }
                ringtone = perform_action(context);
            }
        }
        return new boolean[]{has_failed, ringtone};
    }

    public static void clearSounds(Context context) {

        // ATTENTION (to developers):
        //
        // Sounds that aren't cleared (for testing purposes), but removed from the folder
        // are cleared on the next reboot. The way the ContentResolver SQL database works is that it
        // checks the file integrity of _data (file path), and if the file is missing, the database
        // entry is removed.

        if (checkOMS(context) && checkMasquerade(context) >= 22) {
            MasqueradeService.clearThemedSounds(context);
        } else {
            References.delete(context, "/data/system/theme/audio/");
            setDefaultAudible(context, RingtoneManager.TYPE_ALARM);
            setDefaultAudible(context, RingtoneManager.TYPE_NOTIFICATION);
            setDefaultAudible(context, RingtoneManager.TYPE_RINGTONE);
            setDefaultUISounds("lock_sound", "Lock.ogg");
            setDefaultUISounds("unlock_sound", "Unlock.ogg");
            setDefaultUISounds("low_battery_sound", "LowBattery.ogg");
            References.restartSystemUI(context);
        }
    }

    private static boolean perform_action(Context context) {
        boolean ringtone = false;

        // Let's start with user interface sounds
        File ui = new File(context.getCacheDir().getAbsolutePath() +
                "/SoundsCache/SoundsInjector/ui/");
        File ui_temp = new File("/data/system/theme/audio/ui/");
        if (ui_temp.exists()) {
            References.delete(context, "/data/system/theme/audio/ui/");
        }
        if (ui.exists()) {
            References.createNewFolder("/data/system/theme/audio/ui/");

            File effect_tick_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Effect_Tick.mp3");
            File effect_tick_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Effect_Tick.ogg");
            if (effect_tick_mp3.exists() || effect_tick_ogg.exists()) {
                boolean mp3 = effect_tick_mp3.exists();
                boolean ogg = effect_tick_ogg.exists();
                if (mp3) {
                    References.copyDir(context, context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Effect_Tick.mp3",
                            "/data/system/theme/audio/ui/Effect_Tick.mp3");
                    setUIAudible(context, effect_tick_mp3, new File
                            ("/data/system/theme/audio/ui/Effect_Tick.mp3"), RingtoneManager
                            .TYPE_RINGTONE, "Effect_Tick");
                }
                if (ogg) {
                    References.copyDir(context, context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Effect_Tick.ogg",
                            "/data/system/theme/audio/ui/Effect_Tick.ogg");
                    setUIAudible(context, effect_tick_ogg, new File
                            ("/data/system/theme/audio/ui/Effect_Tick.ogg"), RingtoneManager
                            .TYPE_RINGTONE, "Effect_Tick");
                }
            } else {
                setDefaultUISounds("lock_sound", "Lock.ogg");
            }

            File new_lock_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Lock.mp3");
            File new_lock_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Lock.ogg");
            if (new_lock_mp3.exists() || new_lock_ogg.exists()) {
                boolean mp3 = new_lock_mp3.exists();
                boolean ogg = new_lock_ogg.exists();
                if (mp3) {
                    References.move(context, context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Lock.mp3",
                            "/data/system/theme/audio/ui/Lock.mp3");
                    setUISounds("lock_sound", "/data/system/theme/audio/ui/Lock.mp3");
                }
                if (ogg) {
                    References.move(context, context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Lock.ogg",
                            "/data/system/theme/audio/ui/Lock.ogg");
                    setUISounds("lock_sound", "/data/system/theme/audio/ui/Lock.ogg");
                }
            } else {
                setDefaultUISounds("lock_sound", "Lock.ogg");
            }

            File new_unlock_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Unlock.mp3");
            File new_unlock_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/Unlock.ogg");
            if (new_unlock_mp3.exists() || new_unlock_ogg.exists()) {
                boolean mp3 = new_unlock_mp3.exists();
                boolean ogg = new_unlock_ogg.exists();
                if (mp3) {
                    References.move(context, context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Unlock.mp3",
                            "/data/system/theme/audio/ui/Unlock.mp3");
                    setUISounds("unlock_sound", "/data/system/theme/audio/ui/Unlock.mp3");
                }
                if (ogg) {
                    References.move(context, context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/Unlock.ogg",
                            "/data/system/theme/audio/ui/Unlock.ogg");
                    setUISounds("unlock_sound", "/data/system/theme/audio/ui/Unlock.ogg");
                }
            } else {
                setDefaultUISounds("unlock_sound", "Unlock.ogg");
            }

            File new_lowbattery_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/LowBattery.mp3");
            File new_lowbattery_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ui/LowBattery.ogg");
            if (new_lowbattery_mp3.exists() || new_lowbattery_ogg.exists()) {
                boolean mp3 = new_lowbattery_mp3.exists();
                boolean ogg = new_lowbattery_ogg.exists();
                if (mp3) {
                    References.move(context, context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/LowBattery.mp3",
                            "/data/system/theme/audio/ui/LowBattery.mp3");
                    setUISounds("low_battery_sound",
                            "/data/system/theme/audio/ui/LowBattery.mp3");
                }
                if (ogg) {
                    References.move(context, context.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/SoundsInjector/ui/LowBattery.ogg",
                            "/data/system/theme/audio/ui/LowBattery.ogg");
                    setUISounds("low_battery_sound",
                            "/data/system/theme/audio/ui/LowBattery.ogg");
                }
            } else {
                setDefaultUISounds("low_battery_sound", "LowBattery.ogg");
            }
            References.setPermissionsRecursively(644, "/data/system/theme/audio/ui/");
            References.setPermissions(755, "/data/system/theme/audio/ui/");
            References.setPermissions(755, "/data/system/theme/audio/");
            References.setPermissions(755, "/data/system/theme/");
            References.setContext("/data/system/theme");
        }

        // Now let's set the common user's sound files found in Settings
        File alarms = new File(context.getCacheDir().getAbsolutePath() +
                "/SoundsCache/SoundsInjector/alarms/");
        File alarms_temp = new File("/data/system/theme/audio/alarms/");
        if (alarms_temp.exists()) References.delete(context, "/data/system/theme/audio/alarms/");
        if (alarms.exists()) {
            File new_alarm_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/alarms/" + "/alarm.mp3");
            File new_alarm_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/alarms/" + "/alarm.ogg");
            if (new_alarm_mp3.exists() || new_alarm_ogg.exists()) {
                boolean mp3 = new_alarm_mp3.exists();
                boolean ogg = new_alarm_ogg.exists();

                References.copyDir(context, context.getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/alarms/",
                        "/data/system/theme/audio/");
                References.setPermissionsRecursively(644, "/data/system/theme/audio/alarms/");
                References.setPermissions(755, "/data/system/theme/audio/alarms/");

                // Prior to setting, we should clear out the current ones
                clearAudibles(context, "/data/system/theme/audio/alarms/alarm.mp3");
                clearAudibles(context, "/data/system/theme/audio/alarms/alarm.ogg");

                if (mp3)
                    setAudible(context, new File("/data/system/theme/audio/alarms/alarm.mp3"),
                            new File(alarms.getAbsolutePath(), "alarm.mp3"),
                            RingtoneManager.TYPE_ALARM,
                            context.getString(R.string.content_resolver_alarm_metadata));
                if (ogg)
                    setAudible(context, new File("/data/system/theme/audio/alarms/alarm.ogg"),
                            new File(alarms.getAbsolutePath(), "alarm.ogg"),
                            RingtoneManager.TYPE_ALARM,
                            context.getString(R.string.content_resolver_alarm_metadata));
            } else {
                setDefaultAudible(context, RingtoneManager.TYPE_ALARM);
            }
        }


        File notifications = new File(context.getCacheDir().getAbsolutePath() +
                "/SoundsCache/SoundsInjector/notifications/");
        File notifications_temp = new File("/data/system/theme/audio/notifications/");
        if (notifications_temp.exists())
            References.delete(context, "/data/system/theme/audio/notifications/");
        if (notifications.exists()) {
            ringtone = true;
            File new_notifications_mp3 = new File(context.getCacheDir()
                    .getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/notifications/" + "/notification.mp3");
            File new_notifications_ogg = new File(context.getCacheDir()
                    .getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/notifications/" + "/notification.ogg");
            if (new_notifications_mp3.exists() || new_notifications_ogg.exists()) {
                boolean mp3 = new_notifications_mp3.exists();
                boolean ogg = new_notifications_ogg.exists();

                References.copyDir(context, context.getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/notifications/",
                        "/data/system/theme/audio/");
                References.setPermissionsRecursively(644,
                        "/data/system/theme/audio/notifications/");
                References.setPermissions(755, "/data/system/theme/audio/notifications/");

                // Prior to setting, we should clear out the current ones
                clearAudibles(context, "/data/system/theme/audio/notifications/notification.mp3");
                clearAudibles(context, "/data/system/theme/audio/notifications/notification.ogg");

                if (mp3)
                    setAudible(context, new File
                                    ("/data/system/theme/audio/notifications/notification.mp3"),
                            new File(notifications.getAbsolutePath(), "notification.mp3"),
                            RingtoneManager.TYPE_NOTIFICATION,
                            context.getString(R.string.content_resolver_notification_metadata));
                if (ogg)
                    setAudible(context, new File
                                    ("/data/system/theme/audio/notifications/notification.ogg"),
                            new File(notifications.getAbsolutePath(), "notification.ogg"),
                            RingtoneManager.TYPE_NOTIFICATION,
                            context.getString(R.string.content_resolver_notification_metadata));
            } else {
                setDefaultAudible(context, RingtoneManager.TYPE_NOTIFICATION);
            }
        } else {
            ringtone = false;
        }

        File ringtones = new File(context.getCacheDir().getAbsolutePath() +
                "/SoundsCache/SoundsInjector/ringtones/");
        File ringtones_temp = new File("/data/system/theme/audio/ringtones/");
        if (ringtones_temp.exists())
            References.delete(context, "/data/system/theme/audio/ringtones/");
        if (ringtones.exists()) {
            ringtone = true;
            File new_ringtones_mp3 = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ringtones/" + "/ringtone.mp3");
            File new_ringtones_ogg = new File(context.getCacheDir().getAbsolutePath() +
                    "/SoundsCache/SoundsInjector/ringtones/" + "/ringtone.ogg");
            if (new_ringtones_mp3.exists() || new_ringtones_ogg.exists()) {
                boolean mp3 = new_ringtones_mp3.exists();
                boolean ogg = new_ringtones_ogg.exists();

                References.copyDir(context, context.getCacheDir().getAbsolutePath() +
                                "/SoundsCache/SoundsInjector/ringtones/",
                        "/data/system/theme/audio/");
                References.setPermissionsRecursively(644, "/data/system/theme/audio/ringtones/");
                References.setPermissions(755, "/data/system/theme/audio/ringtones/");

                // Prior to setting, we should clear out the current ones
                clearAudibles(context, "/data/system/theme/audio/ringtones/ringtone.mp3");
                clearAudibles(context, "/data/system/theme/audio/ringtones/ringtone.ogg");

                if (mp3)
                    setAudible(context, new File
                                    ("/data/system/theme/audio/ringtones/ringtone.mp3"),
                            new File(ringtones.getAbsolutePath(), "ringtone.mp3"),
                            RingtoneManager.TYPE_RINGTONE,
                            context.getString(R.string.content_resolver_ringtone_metadata));
                if (ogg)
                    setAudible(context, new File
                                    ("/data/system/theme/audio/ringtones/ringtone.ogg"),
                            new File(ringtones.getAbsolutePath(), "ringtone.ogg"),
                            RingtoneManager.TYPE_RINGTONE,
                            context.getString(R.string.content_resolver_ringtone_metadata));
            } else {
                setDefaultAudible(context, RingtoneManager.TYPE_RINGTONE);
            }
        } else {
            ringtone = false;
        }

        return ringtone;
    }

    private static String getDefaultAudiblePath(int type) {
        final String name;
        final String path;
        switch (type) {
            case RingtoneManager.TYPE_ALARM:
                name = getProp("ro.config.alarm_alert");
                path = name != null ? SYSTEM_ALARMS_PATH + File.separator + name : null;
                break;
            case RingtoneManager.TYPE_NOTIFICATION:
                name = getProp("ro.config.notification_sound");
                path = name != null ? SYSTEM_NOTIFICATIONS_PATH + File.separator + name : null;
                break;
            case RingtoneManager.TYPE_RINGTONE:
                name = getProp("ro.config.ringtone");
                path = name != null ? SYSTEM_RINGTONES_PATH + File.separator + name : null;
                break;
            default:
                path = null;
                break;
        }
        return path;
    }

    private static boolean setUISounds(String sound_name, String location) {
        if (References.allowedUISound(sound_name)) {
            References.adjustContentProvider(SYSTEM_CONTENT_URI, sound_name, location);
            return true;
        }
        return false;
    }

    private static void setDefaultUISounds(String sound_name, String sound_file) {
        References.adjustContentProvider(SYSTEM_CONTENT_URI, sound_name,
                "/system/media/audio/ui/" + sound_file);
    }

    private static boolean setAudible(Context context, File ringtone, File ringtoneCache, int type,
                                      String name) {
        final String path = ringtone.getAbsolutePath();
        final String mimeType = name.endsWith(".ogg") ? "application/ogg" : "application/mp3";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, path);
        values.put(MediaStore.MediaColumns.TITLE, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.SIZE, ringtoneCache.length());
        values.put(MediaStore.Audio.Media.IS_RINGTONE, type == RingtoneManager.TYPE_RINGTONE);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION,
                type == RingtoneManager.TYPE_NOTIFICATION);
        values.put(MediaStore.Audio.Media.IS_ALARM, type == RingtoneManager.TYPE_ALARM);
        values.put(MediaStore.Audio.Media.IS_MUSIC, false);

        Uri uri = MediaStore.Audio.Media.getContentUriForPath(path);
        Uri newUri = null;
        Cursor c = context.getContentResolver().query(uri,
                new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DATA + "='" + path + "'",
                null, null);
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            long id = c.getLong(0);
            c.close();
            newUri = Uri.withAppendedPath(Uri.parse(MEDIA_CONTENT_URI), "" + id);
            context.getContentResolver().update(uri, values,
                    MediaStore.MediaColumns._ID + "=" + id, null);
        }
        if (newUri == null)
            newUri = context.getContentResolver().insert(uri, values);
        try {
            RingtoneManager.setActualDefaultRingtoneUri(context, type, newUri);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static boolean setUIAudible(Context context, File localized_ringtone,
                                        File ringtone_file, int type, String name) {
        final String path = ringtone_file.getAbsolutePath();

        final String path_clone = "/system/media/audio/ui/" + name + ".ogg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, path);
        values.put(MediaStore.MediaColumns.TITLE, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/ogg");
        values.put(MediaStore.MediaColumns.SIZE, localized_ringtone.length());
        values.put(MediaStore.Audio.Media.IS_RINGTONE, false);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
        values.put(MediaStore.Audio.Media.IS_ALARM, false);
        values.put(MediaStore.Audio.Media.IS_MUSIC, true);

        Uri uri = MediaStore.Audio.Media.getContentUriForPath(path);
        Uri newUri = null;
        Cursor c = context.getContentResolver().query(uri,
                new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DATA + "='" + path_clone + "'",
                null, null);
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            long id = c.getLong(0);
            Log.e("ContentResolver", id + "");
            c.close();
            newUri = Uri.withAppendedPath(Uri.parse(MEDIA_CONTENT_URI), "" + id);
            try {
                context.getContentResolver().update(uri, values,
                        MediaStore.MediaColumns._ID + "=" + id, null);
            } catch (Exception e) {
                Log.d("SoundsHandler", "The content provider does not need to be updated.");
            }
        }
        if (newUri == null)
            newUri = context.getContentResolver().insert(uri, values);
        try {
            RingtoneManager.setActualDefaultRingtoneUri(context, type, newUri);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean setDefaultAudible(Context context, int type) {
        final String audiblePath = getDefaultAudiblePath(type);
        if (audiblePath != null) {
            Uri uri = MediaStore.Audio.Media.getContentUriForPath(audiblePath);
            Cursor c = context.getContentResolver().query(uri,
                    new String[]{MediaStore.MediaColumns._ID},
                    MediaStore.MediaColumns.DATA + "='" + audiblePath + "'",
                    null, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                long id = c.getLong(0);
                c.close();
                uri = Uri.withAppendedPath(
                        Uri.parse(MEDIA_CONTENT_URI), "" + id);
            }
            if (uri != null)
                RingtoneManager.setActualDefaultRingtoneUri(context, type, uri);
        } else {
            return false;
        }
        return true;
    }

    private static void clearAudibles(Context context, String audiblePath) {
        final File audibleDir = new File(audiblePath);
        if (audibleDir.exists() && audibleDir.isDirectory()) {
            String[] files = audibleDir.list();
            final ContentResolver resolver = context.getContentResolver();
            for (String s : files) {
                final String filePath = audiblePath + File.separator + s;
                Uri uri = MediaStore.Audio.Media.getContentUriForPath(filePath);
                resolver.delete(uri, MediaStore.MediaColumns.DATA + "=\""
                        + filePath + "\"", null);
                boolean deleted = (new File(filePath)).delete();
                if (deleted) Log.e("SoundsHandler", "Database cleared");
            }
        }
    }
    // End of sounds processing methods

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

    @IgnoreExtraProperties
    @SuppressWarnings("WeakerAccess")
    /*
      Firebase statistics report
     */
    private static class DeviceCollection {

        public String CurrentTime;
        public String FireBaseID;
        public String ID;
        public String Reason;
        public int VersionCode;
        public String VersionName;

        public DeviceCollection(String CurrentTime, String FireBaseID, String ID, String Reason,
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