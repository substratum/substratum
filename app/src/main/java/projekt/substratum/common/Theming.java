package projekt.substratum.common;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import projekt.substratum.activities.launch.ThemeLaunchActivity;

import static projekt.substratum.common.References.SUBSTRATUM_LAUNCHER_CLASS;
import static projekt.substratum.common.References.SUBSTRATUM_LAUNCHER_CLASS_PATH;
import static projekt.substratum.common.References.TEMPLATE_GET_KEYS;
import static projekt.substratum.common.References.TEMPLATE_THEME_MODE;
import static projekt.substratum.common.References.hashPassthrough;
import static projekt.substratum.common.References.spreadYourWingsAndFly;

public class Theming {

    public static void refreshInstalledThemesPref(Context context) {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();

        // Initial parse of what is installed on the device
        Set<String> installed_themes = new TreeSet<>();
        List<ResolveInfo> all_themes = Packages.getThemes(context);
        for (int i = 0; i < all_themes.size(); i++) {
            installed_themes.add(all_themes.get(i).activityInfo.packageName);
        }
        editor.putStringSet("installed_themes", installed_themes);
        editor.apply();
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
    public static void getThemeKeys(Context mContext, String package_name) {
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
        intentActivity.putExtra("oms_check", !Systems.checkOMS(mContext));
        intentActivity.putExtra("theme_mode", theme_mode);
        intentActivity.putExtra("notification", false);
        if (should_debug) Log.d("ThemeLauncher", "Obtaining APK signature hash...");
        intentActivity.putExtra("hash_passthrough", hashPassthrough(mContext));
        if (should_debug) Log.d("ThemeLauncher", "Checking for certification...");
        intentActivity.putExtra("certified", prefs.getBoolean("complexion", true));
        if (should_debug) Log.d("ThemeLauncher", "Starting Activity...");
        return intentActivity;
    }
}
