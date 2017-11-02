package projekt.substratum.common;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import android.os.Build;
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
import static projekt.substratum.common.References.heroImageKenBurnsResourceName;
import static projekt.substratum.common.References.heroImageResourceName;
import static projekt.substratum.common.References.metadataAuthor;
import static projekt.substratum.common.References.metadataName;
import static projekt.substratum.common.References.metadataOverlayParent;
import static projekt.substratum.common.References.metadataOverlayTarget;
import static projekt.substratum.common.References.metadataOverlayVersion;
import static projekt.substratum.common.References.metadataSamsungSupport;
import static projekt.substratum.common.References.metadataThemeReady;
import static projekt.substratum.common.References.metadataVersion;
import static projekt.substratum.common.References.metadataWallpapers;
import static projekt.substratum.common.References.resourceChangelog;
import static projekt.substratum.common.References.wallpaperFragment;
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
     * @param context      Context
     * @param package_name Package ID to be analyzed
     * @return Returns string of installer ID, if null, can't obtain package or installed through ADB
     */
    public static String getInstallerId(Context context,
                                        String package_name) {
        try {
            PackageManager pm = context.getPackageManager();
            String installer = pm.getInstallerPackageName(package_name);
            return installer;
        } catch (Exception e) {
            // Suppress warning
        }
        return null;
    }

    /**
     * Returns whether the package is installed or not
     *
     * @param context      Context
     * @param package_name Package name of the desired app to be checked
     * @return True, if installed
     */
    public static boolean isPackageInstalled(Context context,
                                             String package_name) {
        return isPackageInstalled(context, package_name, true);
    }

    /**
     * Returns whether the package is installed or not, with an extra flag to check if enabled or
     * disabled
     *
     * @param context      Context
     * @param package_name Package name of the desired app to be checked
     * @param enabled      Check whether it is enabled or frozen
     * @return True, if it fits all criteria above
     */
    public static boolean isPackageInstalled(
            Context context,
            String package_name,
            boolean enabled) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo
                    (package_name, 0);
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
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
     * @param context      Context
     * @param package_name Package name of the desired app to be checked
     * @return True, if available
     */
    public static boolean isAvailablePackage(Context context,
                                             String package_name) {
        PackageManager pm = context.getPackageManager();
        if (isPackageInstalled(context, package_name)) {
            try {
                int enabled = pm.getApplicationEnabledSetting(package_name);
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
     * @param mContext      Context
     * @param package_name  Package name of the desired app to be checked
     * @param resource_name Resource name of the object to be checked
     * @param resource_type Resource type of the object to be checked
     * @return True, if present
     */
    public static Boolean validateResource(Context mContext,
                                           String package_name,
                                           String resource_name,
                                           String resource_type) {
        try {
            Context context = mContext.createPackageContext(package_name, 0);
            android.content.res.Resources resources = context.getResources();
            int drawablePointer = resources.getIdentifier(
                    resource_name, // Drawable name explicitly defined
                    resource_type, // Declared icon is a drawable, indeed.
                    package_name); // Icon pack package name
            return drawablePointer != 0;
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
    public static Bitmap getBitmapFromDrawable(Drawable drawable) {
        Bitmap bitmap = null;
        if (drawable instanceof VectorDrawable) {
            bitmap = getBitmapFromVector(drawable);
        } else if ((drawable instanceof BitmapDrawable)
                | (drawable instanceof ShapeDrawable)) {
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
     * @param context      Context
     * @param package_name Package name of the desired app to be checked
     * @return Returns a drawable of the app's icon
     */
    public static Drawable getAppIcon(Context context,
                                      String package_name) {
        try {
            Drawable icon;
            if (allowedSystemUIOverlay(package_name)) {
                icon = context.getPackageManager().getApplicationIcon(SYSTEMUI);
            } else if (allowedSettingsOverlay(package_name)) {
                icon = context.getPackageManager().getApplicationIcon(SETTINGS);
            } else if (allowedFrameworkOverlay(package_name)) {
                icon = context.getPackageManager().getApplicationIcon(FRAMEWORK);
            } else {
                icon = context.getPackageManager().getApplicationIcon(package_name);
            }
            return icon;
        } catch (Exception e) {
            // Suppress warning
        }
        if ((package_name != null) &&
                package_name.equals(INTERFACER_PACKAGE) &&
                !checkOMS(context)) {
            return context.getDrawable(R.mipmap.main_launcher);
        } else {
            return context.getDrawable(R.drawable.default_overlay_icon);
        }
    }

    /**
     * Grab a specific overlay's substratum compiler version
     *
     * @param context      Context
     * @param package_name Package name of the desired app to be checked
     * @return Returns the version of the substratum compiler
     */
    public static int getOverlaySubstratumVersion(Context context,
                                                  String package_name) {
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

    /**
     * Grab the overlay parent's icon
     *
     * @param context      Context
     * @param package_name Package name of the desired app to be checked
     * @return Returns a drawable of the parent's app icon
     */
    public static Drawable getOverlayParentIcon(Context context,
                                                String package_name) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if ((appInfo.metaData != null) &&
                    (appInfo.metaData.getString(metadataOverlayParent) != null)) {
                return getAppIcon(context, appInfo.metaData.getString(metadataOverlayParent));
            }
        } catch (Exception e) {
            // Suppress warning
        }
        return getAppIcon(context, package_name);
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
     * @param mContext     Context
     * @param package_name Package name of the desired app to be checked
     * @return Returns a string of the app's version
     */
    public static String getAppVersion(Context mContext,
                                       String package_name) {
        try {
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(package_name, 0);
            return pInfo.versionName;
        } catch (Exception e) {
            // Suppress warning
        }
        return null;
    }

    /**
     * Return the package's app version code
     *
     * @param mContext     Context
     * @param package_name Package name of the desired app to be checked
     * @return Returns an int of the app's version code
     */
    public static int getAppVersionCode(Context mContext,
                                        String package_name) {
        try {
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(package_name, 0);
            return pInfo.versionCode;
        } catch (Exception e) {
            // Suppress warning
        }
        return 0;
    }

    /**
     * Grabs a given theme's app version
     *
     * @param mContext     Context
     * @param package_name Package name of the desired app to be checked
     * @return Returns a string of the app's version
     */
    public static String getThemeVersion(Context mContext,
                                         String package_name) {
        try {
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(package_name, 0);
            return pInfo.versionName + " (" + pInfo.versionCode + ')';
        } catch (PackageManager.NameNotFoundException e) {
            // Suppress warning
        }
        return null;
    }

    /**
     * Grab the available API levels for a given theme
     *
     * @param mContext     Context
     * @param package_name Package name of the desired app to be checked
     * @return Returns a string of the theme's SDK APIs
     */
    public static String getThemeAPIs(Context mContext,
                                      String package_name) {
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

    /**
     * Grab a specified metadata from a theme
     *
     * @param mContext     Context
     * @param package_name Package name of the desired app to be checked
     * @param metadata     Name of the metadata to be acquired
     * @return Returns a string of the metadata's output
     */
    public static String getOverlayMetadata(
            Context mContext,
            String package_name,
            String metadata) {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (metadata.equals(metadataSamsungSupport)) {
                    if (appInfo.metaData.getBoolean(metadata)) {
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
        } catch (Exception e) {
            // Suppress warning
        }
        return null;
    }

    /**
     * Obtain a resource pointer from any package installed on the device
     *
     * @param mContext     Context
     * @param package_name Package name of the desired app to be checked
     * @param resourceName Resource name from the desired app to be checked
     * @param type         Resource type of the desired object
     * @return Returns the exact resource pointer given a proper otherContext value.
     * 0 denotes failure.
     */
    private static int getResource(Context mContext,
                                   String package_name,
                                   String resourceName,
                                   String type) {
        try {
            android.content.res.Resources res =
                    mContext.getPackageManager().getResourcesForApplication(package_name);
            return res.getIdentifier(
                    package_name + ':' + type + '/' + resourceName,
                    type,
                    package_name);
        } catch (Exception e) {
            // Suppress warning
        }
        return 0;
    }

    /**
     * Obtain a color resource
     *
     * @param mContext     Context
     * @param package_name Package name of the desired app to be checked
     * @param colorName    Name of the color
     * @return Returns the exact resource pointer given a proper otherContext value.
     * 0 denotes failure.
     */
    public static int getColorResource(Context mContext,
                                       String package_name,
                                       String colorName) {
        return getResource(mContext, package_name, colorName, "color");
    }

    /**
     * Grab the theme's changelog
     *
     * @param mContext     Context
     * @param package_name Package name of the desired app to be checked
     * @return Returns a string array for the given theme's changelog
     */
    public static String[] getThemeChangelog(Context mContext,
                                             String package_name) {
        try {
            android.content.res.Resources res =
                    mContext.getPackageManager().getResourcesForApplication(package_name);
            int array_position = getResource(mContext, package_name, resourceChangelog,
                    "array");
            return res.getStringArray(array_position);
        } catch (Exception e) {
            // Suppress warning
        }
        return null;
    }

    /**
     * Grab the theme's hero image
     *
     * @param mContext     Context
     * @param package_name Package name of the desired app to be checked
     * @return Returns a drawable for the given theme's hero image
     */
    public static Drawable getPackageHeroImage(Context mContext,
                                               String package_name,
                                               boolean isThemesView) {
        Drawable hero = mContext.getDrawable(android.R.color.transparent); // Initialize to be clear
        try {
            android.content.res.Resources res = mContext.getPackageManager()
                    .getResourcesForApplication(package_name);
            int resourceId;
            if ((PreferenceManager.
                    getDefaultSharedPreferences(mContext).
                    getInt("grid_style_cards_count", 1) != 1) && isThemesView) {
                resourceId = res.getIdentifier(
                        package_name + ":drawable/" + heroImageGridResourceName, null, null);
                if (resourceId == 0) resourceId = res.getIdentifier(
                        package_name + ":drawable/" + heroImageResourceName, null, null);
            } else if (!isThemesView) {
                resourceId = res.getIdentifier(
                        package_name + ":drawable/" + heroImageKenBurnsResourceName, null, null);
                if (resourceId == 0) resourceId = res.getIdentifier(
                        package_name + ":drawable/" + heroImageResourceName, null, null);
            } else {
                resourceId = res.getIdentifier(
                        package_name + ":drawable/" + heroImageResourceName, null, null);
            }
            if (resourceId != 0) {
                hero = mContext.getPackageManager().getDrawable(package_name, resourceId, null);
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
     * @param mContext     Context
     * @param package_name Package name of the desired app to be checked
     * @return String of the target package name
     */
    public static String getPackageName(Context mContext,
                                        String package_name) {
        PackageManager pm = mContext.getPackageManager();
        ApplicationInfo ai;
        try {
            switch (package_name) {
                case SYSTEMUI_NAVBARS:
                    return mContext.getString(R.string.systemui_navigation);
                case SYSTEMUI_HEADERS:
                    return mContext.getString(R.string.systemui_headers);
                case SYSTEMUI_QSTILES:
                    return mContext.getString(R.string.systemui_qs_tiles);
                case SYSTEMUI_STATUSBARS:
                    return mContext.getString(R.string.systemui_statusbar);
                case SETTINGS_ICONS:
                    return mContext.getString(R.string.settings_icons);
                case SAMSUNG_FRAMEWORK:
                    return mContext.getString(R.string.samsung_framework);
                case LG_FRAMEWORK:
                    return mContext.getString(R.string.lg_framework);
            }
            ai = pm.getApplicationInfo(package_name, 0);
        } catch (Exception e) {
            ai = null;
        }
        return (String) ((ai != null) ? pm.getApplicationLabel(ai) : null);
    }

    /**
     * Get Theme Ready support
     *
     * @param mContext     Context
     * @param package_name Package name of the desired app to be checked
     * @return String of whether the theme supports theme ready
     */
    public static String getThemeReadyVisibility(Context mContext,
                                                 String package_name) {
        return getOverlayMetadata(mContext, package_name, metadataThemeReady);
    }

    /**
     * Get theme plugin version
     *
     * @param mContext     Context
     * @param package_name Package name of the desired app to be checked
     * @return String of the theme's plugin version
     */
    public static String getPackageTemplateVersion(Context mContext,
                                                   String package_name) {
        String template_version = getOverlayMetadata(mContext, package_name, metadataVersion);
        if (template_version != null) {
            return mContext.getString(R.string.plugin_template) + ": " + template_version;
        }
        return null;
    }

    /**
     * Get theme parent from overlay
     *
     * @param mContext     Context
     * @param package_name Package name of the desired app to be checked
     * @return String of the overlay's parent
     */
    public static String getOverlayParent(Context mContext,
                                          String package_name) {
        return getOverlayMetadata(mContext, package_name, metadataOverlayParent);
    }

    /**
     * Get theme target from overlay
     *
     * @param mContext     Context
     * @param package_name Package name of the desired app to be checked
     * @return String of the overlay's target
     */
    public static String getOverlayTarget(Context mContext,
                                          String package_name) {
        return getOverlayMetadata(mContext, package_name, metadataOverlayTarget);
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
        } catch (PackageManager.NameNotFoundException e) {
            // Suppress warning
        }
        return false;
    }

    /**
     * Checks if the specified package is a Samsung supported theme
     *
     * @param context      Context
     * @param package_name Package name of the desired app to be checked
     * @return True, if it supports Samsung
     */
    public static boolean isSamsungTheme(Context context,
                                         String package_name) {
        String obtainedValue = getOverlayMetadata(context, package_name, metadataSamsungSupport);
        return obtainedValue != null && obtainedValue.toLowerCase().equals("true");
    }

    /**
     * Check whether a metadata is conflicting with the banned trigger
     *
     * @param context Context
     * @param trigger Trigger word
     * @return True, if triggered
     */
    static boolean getMetadata(Context context,
                               String trigger) {
        List<ApplicationInfo> list =
                context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).packageName.startsWith(trigger)) {
                return true;
            }
        }
        return false;
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
            ThemeInterfacerService.uninstallOverlays(context, list, false);
        } else {
            ElevatedCommands.runThreadedCommand("pm uninstall " + packageName);
        }
    }

    /**
     * Checks the packages for Substratum, then mutates the input
     *
     * @param context       Context
     * @param home_type     Home type
     * @param search_filter User input in search
     * @return Returns a map of substratum ready packages
     */
    @SuppressWarnings("unchecked")
    public static HashMap<String, String[]> getSubstratumPackages(Context context,
                                                                  CharSequence home_type,
                                                                  String search_filter) {
        try {
            HashMap returnMap = new HashMap<>();
            List<ResolveInfo> listOfThemes = getThemes(context);
            for (ResolveInfo ri : listOfThemes) {
                String packageName = ri.activityInfo.packageName;
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        packageName, PackageManager.GET_META_DATA);

                // By default, we will have to enforce continuation to false, if a poorly adapted
                // theme did not implement the proper meta data.
                Boolean can_continue = false;
                if ((appInfo.metaData.getString(metadataName) != null) &&
                        (appInfo.metaData.getString(metadataAuthor) != null) &&
                        (appInfo.metaData.getString(metadataVersion) != null)) {
                    // Check if Samsung, and block the showing of the theme if the theme does not
                    // support samsung intentionally!
                    Boolean samsung_support = null;
                    try {
                        samsung_support = appInfo.metaData.getBoolean(metadataSamsungSupport);
                    } catch (Exception e) {
                        // At this point, the themer did not specify a boolean value
                    }
                    if (samsung_support != null &&
                            !samsung_support &&
                            (Systems.isSamsungDevice(context) ||
                                    Systems.isNewSamsungDevice(context))) {
                        can_continue = false;
                    } else {
                        // The theme app contains the proper metadata
                        can_continue = true;
                        // If the user is searching using the search bar
                        if ((search_filter != null) && !search_filter.isEmpty()) {
                            @SuppressWarnings("StringBufferReplaceableByString") StringBuilder
                                    filtered = new StringBuilder();
                            filtered.append(appInfo.metaData.getString(metadataName));
                            filtered.append(appInfo.metaData.getString(metadataAuthor));
                            can_continue =
                                    filtered
                                            .toString()
                                            .toLowerCase()
                                            .contains(search_filter.toLowerCase());
                        }
                    }
                }
                if (can_continue) {
                    // Let's prepare ourselves for appending into the hash map for this theme
                    String[] data = {
                            appInfo.metaData.getString(metadataAuthor),
                            packageName
                    };
                    // Take the other package's context
                    Context other = context.createPackageContext(packageName, 0);
                    // Check if it is wallpaper mode, if it is, bail out early
                    if (home_type.equals(wallpaperFragment)) {
                        String wallpaperCheck = appInfo.metaData.getString
                                (metadataWallpapers);
                        if ((wallpaperCheck != null) && !wallpaperCheck.isEmpty()) {
                            returnMap.put(appInfo.metaData.getString(metadataName), data);
                        }
                    } else {
                        // Well, it's not wallpaper mode, so let's keep going!
                        if (home_type.length() == 0) {
                            returnMap.put(appInfo.metaData.getString(metadataName), data);
                            Log.d(PACKAGE_TAG, "Loaded Substratum Theme: [" + packageName + ']');
                            if (ENABLE_PACKAGE_LOGGING)
                                PackageAnalytics.logPackageInfo(context, packageName);
                        } else {
                            // We now have to open a specific fragment
                            try (ZipFile zf = new ZipFile(other.getApplicationInfo().sourceDir)) {
                                for (Enumeration<? extends ZipEntry> e = zf.entries();
                                     e.hasMoreElements(); ) {
                                    ZipEntry ze = e.nextElement();
                                    String name = ze.getName();
                                    if (name.startsWith("assets/" + home_type + '/')) {
                                        returnMap.put(
                                                appInfo.metaData.getString(metadataName),
                                                data);
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(SUBSTRATUM_LOG, "Unable to find package identifier");
                            }
                        }
                    }
                } else {
                    Log.e(PACKAGE_TAG, "Skipping package: '" + packageName +
                            "' - due to incorrect metadata installation");
                }
            }
            return returnMap;
        } catch (Exception e) {
            // Suppress warning
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
            String resource_name = ReadVariantPrioritizedColor.read(clone1);

            if (resource_name != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(clone2))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.contains('"' + resource_name + '"')) {
                            String[] split =
                                    line.substring(line.lastIndexOf("\">") + 2).split("<");
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

    /**
     * Check whether a content provider is conflicting with the banned trigger
     *
     * @param context Context
     * @param trigger Trigger word
     * @return True, if triggered
     */
    static boolean getProviders(Context context,
                                String trigger) {
        List<PackageInfo> list =
                context.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).packageName.startsWith(trigger)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether an intent is conflicting with the banned trigger
     *
     * @param context Context
     * @param trigger Trigger word
     * @return True, if triggered
     */
    static boolean getIntents(Context context,
                              String trigger) {
        Collection<Intent> intentArray = new ArrayList<>();
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
                if ((activityInfo != null) && activityInfo.name.startsWith(trigger)) {
                    return true;
                }
            }
        }
        return false;
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
     * @param context      Context
     * @param package_name Package name of the desired app to be checked
     * @return Returns the installation directory of the overlay
     */
    public static String getInstalledDirectory(Context context,
                                               String package_name) {
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
}