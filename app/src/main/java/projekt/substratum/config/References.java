package projekt.substratum.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import projekt.substratum.R;
import projekt.substratum.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class References {

    // This int controls the notification identifier
    public static int firebase_notification_id = 24862486;
    public static int notification_id = 2486;

    // Universal switch for Application-wide Debugging
    public static Boolean DEBUG = true;

    // These strings control the current filter for themes
    public static String metadataName = "Substratum_Name";
    public static String metadataAuthor = "Substratum_Author";
    public static String metadataLegacy = "Substratum_Legacy";
    public static String metadataVersion = "Substratum_Plugin";

    // This int controls the default priority level for legacy overlays
    public static int DEFAULT_PRIORITY = 50;

    // This method is used to determine whether there the system is initiated with OMS
    public static Boolean checkOMS() {
        File om = new File("/system/bin/om");
        return om.exists();
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
        prefs.edit().putBoolean("show_app_icon", true).apply();
        if (References.getProp("ro.substratum.recreate").equals("true")) {
            prefs.edit().putBoolean("systemui_recreate", true).apply();
        } else {
            prefs.edit().putBoolean("systemui_recreate", false).apply();
        }
        prefs.edit().putBoolean("substratum_oms", References.checkOMS()).apply();
        prefs.edit().putBoolean("show_template_version", false).apply();
        prefs.edit().putBoolean("vibrate_on_compiled", false).apply();
        prefs = context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("is_updating", false).apply();
    }

    // This method configures the new devices and their configuration of their vendor folders
    public static Boolean inNexusFilter() {
        String[] nexus_filter = {"angler", "bullhead", "flounder", "dragon", "marlin", "sailfish"};
        return Arrays.asList(nexus_filter).contains(Build.DEVICE);
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

    // This method determines whether a specified package is installed

    public static boolean isPackageInstalled(Context context, String package_name) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
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

    // This method obtains the application icon for a specified package

    public static Drawable grabAppIcon(Context context, String package_name) {
        Drawable icon = null;
        try {
            if (References.allowedSystemUIOverlay(package_name)) {
                icon = context.getPackageManager().getApplicationIcon("com.android.systemui");
            } else {
                if (References.allowedSettingsOverlay(package_name)) {
                    icon = context.getPackageManager().getApplicationIcon("com.android.settings");
                } else {
                    icon = context.getPackageManager().getApplicationIcon(package_name);
                }
            }
        } catch (Exception e) {
            Log.e("SubstratumLogger", "Could not grab the application icon for \"" + package_name
                    + "\"");
        }
        return icon;
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
                        }
                    } else {
                        String minSdk = "";
                        int min = appInfo.minSdkVersion;
                        if (min == 23) {
                            minSdk = mContext.getString(R.string.api_23);
                        } else if (min == 24) {
                            minSdk = mContext.getString(R.string.api_24);
                        } else if (min == 25) {
                            minSdk = mContext.getString(R.string.api_25);
                        }
                        String targetSdk = "";
                        int target = appInfo.targetSdkVersion;
                        if (target == 23) {
                            targetSdk = mContext.getString(R.string.api_23);
                        } else if (target == 24) {
                            targetSdk = mContext.getString(R.string.api_24);
                        } else if (target == 25) {
                            targetSdk = mContext.getString(R.string.api_25);
                        }
                        return minSdk + " - " + targetSdk;
                    }
                } catch (NoSuchFieldError nsfe) {
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

    public static String grabPackageTemplateVersion(Context mContext, String package_name) {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString(References.metadataVersion) != null) {
                    return mContext.getString(R.string.plugin_template) + ": " +
                            appInfo.metaData.getString(References.metadataVersion);
                }
            }
        } catch (Exception e) {
            //
        }
        return null;
    }

    public static Drawable grabPackageHeroImage(Context mContext, String package_name) {
        Resources res;
        Drawable hero = null;
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
}