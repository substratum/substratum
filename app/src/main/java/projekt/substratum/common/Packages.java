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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.VectorDrawable;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import projekt.substratum.R;
import projekt.substratum.common.analytics.PackageAnalytics;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.platform.SubstratumService;
import projekt.substratum.common.platform.ThemeInterfacerService;
import projekt.substratum.util.readers.ReadVariantPrioritizedColor;

import static projekt.substratum.common.References.ENABLE_PACKAGE_LOGGING;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.References.SUBSTRATUM_PACKAGE;
import static projekt.substratum.common.References.SUBSTRATUM_THEME;
import static projekt.substratum.common.References.heroImageGridResourceName;
import static projekt.substratum.common.References.heroImageMainResourceName;
import static projekt.substratum.common.References.heroImageResourceName;
import static projekt.substratum.common.References.metadataAuthor;
import static projekt.substratum.common.References.metadataName;
import static projekt.substratum.common.References.metadataOverlayParent;
import static projekt.substratum.common.References.metadataOverlayTarget;
import static projekt.substratum.common.References.metadataOverlayVersion;
import static projekt.substratum.common.References.metadataSamsungSupport;
import static projekt.substratum.common.References.metadataVersion;
import static projekt.substratum.common.References.resourceChangelog;
import static projekt.substratum.common.Resources.FRAMEWORK;
import static projekt.substratum.common.Resources.LG_FRAMEWORK;
import static projekt.substratum.common.Resources.SAMSUNG_FRAMEWORK;
import static projekt.substratum.common.Resources.SETTINGS;
import static projekt.substratum.common.Resources.SETTINGS_ICONS;
import static projekt.substratum.common.Resources.SYSTEMUI;
import static projekt.substratum.common.Resources.SYSTEMUI_HEADERS;
import static projekt.substratum.common.Resources.SYSTEMUI_NAVBARS;
import static projekt.substratum.common.Resources.SYSTEMUI_QSTILES;
import static projekt.substratum.common.Resources.SYSTEMUI_STATUSBARS;
import static projekt.substratum.common.Resources.allowedFrameworkOverlay;
import static projekt.substratum.common.Resources.allowedSettingsOverlay;
import static projekt.substratum.common.Resources.allowedSystemUIOverlay;
import static projekt.substratum.common.Systems.checkOMS;
import static projekt.substratum.common.Systems.checkSubstratumService;
import static projekt.substratum.common.Systems.checkThemeInterfacer;
import static projekt.substratum.common.analytics.PackageAnalytics.PACKAGE_TAG;

public enum Packages {
    ;

    /**
     * Grab the installer ID on a given package
     *
     * @param context     Context
     * @param packageName Package ID to be analyzed
     * @return Returns string of installer ID, if null, can't obtain package or installed through ADB
     */
    public static String getInstallerId(Context context,
                                        String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getInstallerPackageName(packageName);
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Returns whether the package is installed or not
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return True, if installed
     */
    public static boolean isPackageInstalled(Context context,
                                             String packageName) {
        return isPackageInstalled(context, packageName, true);
    }

    /**
     * Returns whether the package is installed or not, with an extra flag to check if enabled or
     * disabled
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @param enabled     Check whether it is enabled or frozen
     * @return True, if it fits all criteria above
     */
    public static boolean isPackageInstalled(Context context,
                                             String packageName,
                                             boolean enabled) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            if (enabled) return ai.enabled;
            // if package doesn't exist, an Exception will be thrown, so return true in every case
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a package is available to be used
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return True, if available
     */
    public static boolean isAvailablePackage(Context context,
                                             String packageName) {
        PackageManager pm = context.getPackageManager();
        if (isPackageInstalled(context, packageName)) {
            try {
                int enabled = pm.getApplicationEnabledSetting(packageName);
                return (enabled != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) &&
                        (enabled != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Validate whether a resource is present in a given package name
     *
     * @param context      Context
     * @param packageName  Package name of the desired app to be checked
     * @param resourceName Resource name of the object to be checked
     * @param resourceType Resource type of the object to be checked
     * @return True, if present
     */
    public static boolean validateResource(Context context,
                                           String packageName,
                                           String resourceName,
                                           String resourceType) {
        try {
            Context ctx = context.createPackageContext(packageName, 0);
            android.content.res.Resources resources = ctx.getResources();
            int resourcePointer = resources.getIdentifier(
                    resourceName, // Resource name
                    resourceType, // Type of resource to check
                    packageName); // Package name to check
            return resourcePointer != 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convert a VectorDrawable to a Bitmap
     *
     * @param drawable Insert a VectorDrawable casted as a Drawable
     * @return Returns the converted Bitmap
     */
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

    /**
     * Convert a Drawable to a Bitmap
     *
     * @param drawable Insert a Drawable casted as a Drawable
     * @return Returns the converted Bitmap
     */
    @SuppressLint("NewApi")
    public static Bitmap getBitmapFromDrawable(Drawable drawable) {
        Bitmap bitmap = null;
        if (drawable instanceof VectorDrawable) {
            bitmap = getBitmapFromVector(drawable);
        } else if ((drawable instanceof BitmapDrawable)
                | (drawable instanceof ShapeDrawable)) {
            // Lint doesn't realise that ShapeDrawable and
            // BitmapDrawable are interoperable classes so
            // keeps throwing the warning.
            //noinspection ConstantConditions
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof AdaptiveIconDrawable) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // First we must get the top and bottom layers of the drawable
                Drawable backgroundDrawable = ((AdaptiveIconDrawable) drawable)
                        .getBackground();
                Drawable foregroundDrawable = ((AdaptiveIconDrawable) drawable)
                        .getForeground();

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

    /**
     * Grab the app icon of a given package
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return Returns a drawable of the app's icon
     */
    public static Drawable getAppIcon(Context context,
                                      String packageName) {
        try {
            Drawable icon;
            if (allowedSystemUIOverlay(packageName)) {
                icon = context.getPackageManager().getApplicationIcon(SYSTEMUI);
            } else if (allowedSettingsOverlay(packageName)) {
                icon = context.getPackageManager().getApplicationIcon(SETTINGS);
            } else if (allowedFrameworkOverlay(packageName)) {
                icon = context.getPackageManager().getApplicationIcon(FRAMEWORK);
            } else {
                icon = context.getPackageManager().getApplicationIcon(packageName);
            }
            return icon;
        } catch (Exception ignored) {
        }
        if ((packageName != null) &&
                packageName.equals(INTERFACER_PACKAGE) &&
                !checkOMS(context)) {
            return context.getDrawable(R.mipmap.main_launcher);
        } else {
            return context.getDrawable(R.drawable.default_overlay_icon);
        }
    }

    /**
     * Grab a specific overlay's substratum compiler version
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return Returns the version of the substratum compiler
     */
    public static int getOverlaySubstratumVersion(Context context,
                                                  String packageName) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    packageName, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                return appInfo.metaData.getInt(metadataOverlayVersion);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * Grab the overlay parent's icon
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return Returns a drawable of the parent's app icon
     */
    public static Drawable getOverlayParentIcon(Context context,
                                                String packageName) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    packageName, PackageManager.GET_META_DATA);
            if ((appInfo.metaData != null) &&
                    (appInfo.metaData.getString(metadataOverlayParent) != null)) {
                return getAppIcon(context, appInfo.metaData.getString(metadataOverlayParent));
            }
        } catch (Exception ignored) {
        }
        return getAppIcon(context, packageName);
    }

    /**
     * Get the list of themes on the device
     *
     * @param context Context
     * @return Returns a list of themes on the device
     */
    public static List<ResolveInfo> getThemes(Context context) {
        // Scavenge through the packages on the device with specific substratum metadata
        PackageManager packageManager = context.getPackageManager();
        return packageManager.queryIntentActivities(new Intent(SUBSTRATUM_THEME),
                PackageManager.GET_META_DATA);
    }

    /**
     * Grab a collection of themes on the device
     *
     * @param context Context
     * @return Returns a collection of themes on the device
     */
    public static Collection<String> getThemesArray(Context context) {
        Collection<String> returnArray = new ArrayList<>();
        List<ResolveInfo> themesResolveInfo = getThemes(context);
        for (int i = 0; i < themesResolveInfo.size(); i++) {
            returnArray.add(themesResolveInfo.get(i).activityInfo.packageName);
        }
        return returnArray;
    }

    /**
     * Grabs a given package's app version
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return Returns a string of the app's version
     */
    public static String getAppVersion(Context context,
                                       String packageName) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return pInfo.versionName;
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Return the package's app version code
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return Returns an int of the app's version code
     */
    public static int getAppVersionCode(Context context,
                                        String packageName) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return pInfo.versionCode;
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * Grab a specified metadata from a theme
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @param metadata    Name of the metadata to be acquired
     * @return Returns a string of the metadata's output
     */
    public static String getOverlayMetadata(
            Context context,
            String packageName,
            String metadata) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    packageName, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (metadata.equals(metadataSamsungSupport)) {
                    try {
                        boolean samsungSupport = appInfo.metaData.getBoolean(metadata);
                        return String.valueOf(samsungSupport);
                    } catch (Exception e) {
                        return String.valueOf(true);
                    }
                } else {
                    String returnMetadata = appInfo.metaData.getString(metadata);
                    if (returnMetadata != null) {
                        return returnMetadata;
                    } else {
                        throw new Exception();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Obtain a resource pointer from any package installed on the device
     *
     * @param context      Context
     * @param packageName  Package name of the desired app to be checked
     * @param resourceName Resource name from the desired app to be checked
     * @param type         Resource type of the desired object
     * @return Returns the exact resource pointer given a proper otherContext value.
     * 0 denotes failure.
     */
    private static int getResource(Context context,
                                   String packageName,
                                   String resourceName,
                                   String type) {
        try {
            android.content.res.Resources res =
                    context.getPackageManager().getResourcesForApplication(packageName);
            return res.getIdentifier(
                    packageName + ':' + type + '/' + resourceName,
                    type,
                    packageName);
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * Obtain a color resource
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @param colorName   Name of the color
     * @return Returns the exact resource pointer given a proper otherContext value.
     * 0 denotes failure.
     */
    public static int getColorResource(Context context,
                                       String packageName,
                                       String colorName) {
        return getResource(context, packageName, colorName, "color");
    }

    /**
     * Grab the theme's changelog
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return Returns a string array for the given theme's changelog
     */
    public static String[] getThemeChangelog(Context context,
                                             String packageName) {
        try {
            android.content.res.Resources res =
                    context.getPackageManager().getResourcesForApplication(packageName);
            int array_position = getResource(context, packageName, resourceChangelog,
                    "array");
            return res.getStringArray(array_position);
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Grab the theme's hero image
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return Returns a drawable for the given theme's hero image
     */
    public static Drawable getPackageHeroImage(Context context,
                                               String packageName,
                                               boolean isThemesView) {
        Drawable hero = context.getDrawable(android.R.color.transparent); // Initialize to be clear
        try {
            android.content.res.Resources res = context.getPackageManager()
                    .getResourcesForApplication(packageName);
            int resourceId;
            if ((PreferenceManager.
                    getDefaultSharedPreferences(context).
                    getInt("grid_style_cards_count", 1) != 1) && isThemesView) {
                resourceId = res.getIdentifier(
                        packageName + ":drawable/" + heroImageGridResourceName, null, null);
                if (resourceId == 0) resourceId = res.getIdentifier(
                        packageName + ":drawable/" + heroImageResourceName, null, null);
            } else if (!isThemesView) {
                resourceId = res.getIdentifier(
                        packageName + ":drawable/" + heroImageMainResourceName, null, null);
                if (resourceId == 0) resourceId = res.getIdentifier(
                        packageName + ":drawable/" + heroImageResourceName, null, null);
            } else {
                resourceId = res.getIdentifier(
                        packageName + ":drawable/" + heroImageResourceName, null, null);
            }
            if (resourceId != 0) {
                hero = context.getPackageManager().getDrawable(packageName, resourceId, null);
            }
            return hero;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hero;
    }

    /**
     * Get a human readable target package name
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return String of the target package name
     */
    public static String getPackageName(Context context,
                                        String packageName) {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo ai;
        try {
            switch (packageName) {
                case SYSTEMUI_NAVBARS:
                    return context.getString(R.string.systemui_navigation);
                case SYSTEMUI_HEADERS:
                    return context.getString(R.string.systemui_headers);
                case SYSTEMUI_QSTILES:
                    return context.getString(R.string.systemui_qs_tiles);
                case SYSTEMUI_STATUSBARS:
                    return context.getString(R.string.systemui_statusbar);
                case SETTINGS_ICONS:
                    return context.getString(R.string.settings_icons);
                case SAMSUNG_FRAMEWORK:
                    return context.getString(R.string.samsung_framework);
                case LG_FRAMEWORK:
                    return context.getString(R.string.lg_framework);
            }
            ai = pm.getApplicationInfo(packageName, 0);
        } catch (Exception e) {
            ai = null;
        }
        return (String) ((ai != null) ? pm.getApplicationLabel(ai) : packageName);
    }

    /**
     * Get theme plugin version
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return String of the theme's plugin version
     */
    public static String getPackageTemplateVersion(Context context,
                                                   String packageName) {
        String template_version = getOverlayMetadata(context, packageName, metadataVersion);
        if (template_version != null) {
            return context.getString(R.string.plugin_template) + ": " + template_version;
        }
        return null;
    }

    /**
     * Get theme parent from overlay
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return String of the overlay's parent
     */
    public static String getOverlayParent(Context context,
                                          String packageName) {
        return getOverlayMetadata(context, packageName, metadataOverlayParent);
    }

    /**
     * Get theme target from overlay
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return String of the overlay's target
     */
    public static String getOverlayTarget(Context context,
                                          String packageName) {
        return getOverlayMetadata(context, packageName, metadataOverlayTarget);
    }

    /**
     * Checks whether an app is a user app
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return True, if user app
     */
    public static boolean isUserApp(Context context,
                                    String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
            return (ai.flags & mask) == 0;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }

    /**
     * Checks if the specified package is a Samsung supported theme
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return True, if it supports Samsung
     */
    public static boolean isSamsungTheme(Context context,
                                         String packageName) {
        String obtainedValue = getOverlayMetadata(context, packageName, metadataSamsungSupport);
        return obtainedValue == null || !obtainedValue.toLowerCase(Locale.US).equals("true");
    }

    /**
     * Uninstall a specific package
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be uninstalled
     */
    public static void uninstallPackage(Context context,
                                        String packageName) {
        ArrayList<String> list = new ArrayList<>();
        list.add(packageName);
        if (checkSubstratumService(context)) {
            SubstratumService.uninstallOverlay(list, false);
        } else if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.uninstallOverlays(list);
        } else {
            ElevatedCommands.runThreadedCommand("pm uninstall " + packageName);
        }
    }

    /**
     * Checks the packages for Substratum, then mutates the input
     *
     * @param context      Context
     * @param searchFilter User input in search
     * @return Returns a map of substratum ready packages
     */
    @SuppressWarnings("unchecked")
    public static HashMap<String, String[]> getSubstratumPackages(Context context,
                                                                  String searchFilter) {
        try {
            HashMap returnMap = new HashMap<>();
            List<ResolveInfo> listOfThemes = getThemes(context);
            for (ResolveInfo ri : listOfThemes) {
                String packageName = ri.activityInfo.packageName;
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        packageName, PackageManager.GET_META_DATA);

                // By default, we will have to enforce continuation to false, if a poorly adapted
                // theme did not implement the proper meta data.
                boolean canContinue = false;
                if ((appInfo.metaData.getString(metadataName) != null) &&
                        (appInfo.metaData.getString(metadataAuthor) != null) &&
                        (appInfo.metaData.getString(metadataVersion) != null)) {
                    // Check if Samsung, and block the showing of the theme if the theme does not
                    // support samsung intentionally!
                    boolean samsungSupport =
                            appInfo.metaData.getBoolean(metadataSamsungSupport, true);
                    if (!samsungSupport &&
                            (Systems.isSamsungDevice(context) || Systems.isNewSamsungDevice())) {
                        canContinue = false;
                    } else {
                        // The theme app contains the proper metadata
                        canContinue = true;
                        // If the user is searching using the search bar
                        if ((searchFilter != null) && !searchFilter.isEmpty()) {
                            @SuppressWarnings("StringBufferReplaceableByString") StringBuilder
                                    filtered = new StringBuilder();
                            filtered.append(appInfo.metaData.getString(metadataName));
                            filtered.append(appInfo.metaData.getString(metadataAuthor));
                            canContinue = filtered.toString()
                                    .toLowerCase(Locale.US)
                                    .contains(searchFilter.toLowerCase(Locale.US));
                        }
                    }
                }
                if (canContinue) {
                    // Let's prepare ourselves for appending into the hash map for this theme
                    String[] data = {
                            appInfo.metaData.getString(metadataAuthor),
                            packageName
                    };
                    returnMap.put(appInfo.metaData.getString(metadataName), data);
                    Log.d(PACKAGE_TAG, "Loaded Substratum Theme: [" + packageName + ']');
                    if (ENABLE_PACKAGE_LOGGING)
                        PackageAnalytics.logPackageInfo(context, packageName);
                } else {
                    Log.e(PACKAGE_TAG, "Skipping package: '" + packageName +
                            "' - due to incorrect metadata installation");
                }
            }
            return returnMap;
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Parse a specific overlay resource file (.xml) and return the specified value
     *
     * @param overlay File to check
     * @return String of overlay's resource
     */
    public static String getOverlayResource(InputStream overlay) {
        byte[] byteArray;
        try {
            byteArray = IOUtils.toByteArray(overlay);
        } catch (IOException e) {
            Log.e(SUBSTRATUM_LOG, "Unable to clone InputStream");
            return null;
        }

        String hex = null;
        // We need to clone the InputStream so that we can ensure that the name and color are
        // mutually exclusive
        try (InputStream clone1 = new ByteArrayInputStream(byteArray);
             InputStream clone2 = new ByteArrayInputStream(byteArray)) {
            // Find the name of the top most color in the file first.
            String resourceName = ReadVariantPrioritizedColor.read(clone1);

            if (resourceName != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(clone2))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.contains('"' + resourceName + '"')) {
                            String[] split =
                                    line.substring(line.lastIndexOf("\">") + 2).split("<");
                            hex = split[0];
                            if (hex.startsWith("?")) hex = "#00000000";
                        }
                    }
                } catch (IOException ioe) {
                    Log.e(SUBSTRATUM_LOG, "Unable to find " + resourceName + " in this overlay!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hex;
    }

    /**
     * Determine whether the package requires a recreate
     *
     * @param context Context
     * @param list    List of packages
     * @return True, if needing recreate
     */
    public static boolean needsRecreate(Context context,
                                        Iterable<String> list) {
        for (String o : list) {
            if (o.equals(FRAMEWORK) || o.equals(SUBSTRATUM_PACKAGE)) {
                return false;
            }
        }
        return checkOMS(context);
    }

    /**
     * Determine the installed directory of the overlay for legacy mode
     *
     * @param context     Context
     * @param packageName Package name of the desired app to be checked
     * @return Returns the installation directory of the overlay
     */
    public static String getInstalledDirectory(Context context,
                                               String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return ai.sourceDir;
        } catch (Exception ignored) {
        }
        return null;
    }
}