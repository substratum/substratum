package projekt.substratum.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.util.Arrays;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class References {

    // This method is used to determine whether there the system is initiated with OMS

    public static int firebase_notification_id = 24862486;
    public static int notification_id = 2486;

    // Load SharedPreference defaults
    public static Boolean DEBUG = false;

    // This method configures the new devices and their configuration of their vendor folders
    public static int DEFAULT_PRIORITY = 50;

    public static Boolean checkOMS() {
        File om = new File("/system/bin/om");
        return om.exists();
    }

    public static void loadDefaultConfig(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("show_app_icon", true).apply();
        prefs.edit().putBoolean("systemui_recreate", false).apply();
        prefs.edit().putBoolean("is_updating", false).apply();
        prefs.edit().putBoolean("substratum_oms", References.checkOMS()).apply();
    }

    public static Boolean inNexusFilter() {
        String[] nexus_filter = {"angler", "bullhead", "flounder", "marlin", "sailfish"};
        return Arrays.asList(nexus_filter).contains(Build.DEVICE);
    }

    // This int controls the notification identifier

    // This string array contains all the SystemUI acceptable overlay packs
    public static Boolean allowedSystemUIOverlay(String current) {
        String[] allowed_overlays = {
                "com.android.systemui.headers",
                "com.android.systemui.navbars",
                "com.android.systemui.statusbars"
        };
        return Arrays.asList(allowed_overlays).contains(current);
    }

    // This boolean controls the DEBUG level of the application

    // This string array contains all the SystemUI acceptable sound files
    public static Boolean allowedUISound(String targetValue) {
        String[] allowed_themable = {
                "lock_sound",
                "unlock_sound",
                "low_battery_sound"};
        return Arrays.asList(allowed_themable).contains(targetValue);
    }

    // This int controls the default priority level for legacy overlays

    // This string array contains all the legacy allowed folders
    public static Boolean allowedForLegacy(String targetValue) {
        String[] allowed_themable = {
                "overlays",
                "overlays_legacy",
                "sounds"};
        return Arrays.asList(allowed_themable).contains(targetValue);
    }

    // This method determines whether a specified package is installed

    public static boolean isPackageInstalled(Context context, String package_name) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // This method obtains the application icon for a specified package

    public static Drawable grabAppIcon(Context context, String package_name) {
        Drawable icon = null;
        try {
            if (References.allowedSystemUIOverlay(package_name)) {
                icon = context.getPackageManager().getApplicationIcon("com.android.systemui");
            } else {
                icon = context.getPackageManager().getApplicationIcon(package_name);
            }
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e("SubstratumLogger", "Could not grab the application icon for \"" + package_name
                    + "\"");
        }
        return icon;
    }
}