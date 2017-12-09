package projekt.substratum.common;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import projekt.substratum.activities.launch.ThemeLaunchActivity;

import static projekt.substratum.common.References.SUBSTRATUM_PACKAGE;
import static projekt.substratum.common.References.TEMPLATE_GET_KEYS;
import static projekt.substratum.common.References.TEMPLATE_THEME_MODE;
import static projekt.substratum.common.References.hashPassthrough;

public enum Theming {
    ;

    /**
     * Refresh installed themes shared preferences
     *
     * @param context Self explanatory, bud.
     */
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

    /**
     * Launch a specific theme
     *
     * @param mContext     Self explanatory, bud.
     * @param package_name Theme to be launched
     * @param theme_mode   Filter mode
     */
    public static void launchTheme(Context mContext,
                                   String package_name,
                                   String theme_mode) {
        if (mContext.getPackageName().equals(SUBSTRATUM_PACKAGE)) {
            Intent theme_intent = themeIntent(
                    mContext,
                    package_name,
                    theme_mode,
                    TEMPLATE_THEME_MODE);
            mContext.startActivity(theme_intent);
        }
    }

    /**
     * Grab the theme's keys
     *
     * @param mContext     Self explanatory, bud.
     * @param package_name Theme to obtain keys for
     */
    public static void getThemeKeys(Context mContext,
                                    String package_name) {
        if (mContext.getPackageName().equals(SUBSTRATUM_PACKAGE)) {
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
    }

    /**
     * Grab the theme's intent
     *
     * @param mContext     Self explanatory, bud.
     * @param package_name Theme to receive intent for
     * @param theme_mode   Filter mode
     * @param actionIntent Intent to be verified with a series of data
     * @return Returns an intent to launch the theme
     */
    public static Intent themeIntent(Context mContext,
                                     String package_name,
                                     String theme_mode,
                                     String actionIntent) {
        if (mContext.getPackageName().equals(SUBSTRATUM_PACKAGE)) {
            boolean should_debug = projekt.substratum.BuildConfig.DEBUG;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            if (should_debug) Log.d("ThemeLauncher", "Creating new intent...");
            Intent intentActivity;
            if (actionIntent.equals(TEMPLATE_GET_KEYS)) {
                intentActivity = new Intent();
            } else {
                intentActivity = new Intent(mContext, ThemeLaunchActivity.class);
            }
            intentActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
        } else {
            return null;
        }
    }
}