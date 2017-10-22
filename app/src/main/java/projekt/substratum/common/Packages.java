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
import projekt.substratum.common.platform.ThemeInterfacerService;
import projekt.substratum.util.readers.ReadVariantPrioritizedColor;

import static projekt.substratum.common.References.ENABLE_PACKAGE_LOGGING;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
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
import static projekt.substratum.common.Resources.allowedFrameworkOverlay;
import static projekt.substratum.common.Resources.allowedSettingsOverlay;
import static projekt.substratum.common.Resources.allowedSystemUIOverlay;
import static projekt.substratum.common.Systems.checkOMS;
import static projekt.substratum.common.Systems.checkThemeInterfacer;
import static projekt.substratum.common.analytics.PackageAnalytics.PACKAGE_TAG;

public enum Packages {
    ;

    public static String getInstallerId(final Context context, final String package_name) {
        return context.getPackageManager().getInstallerPackageName(package_name);
    }

    // This method determines whether a specified package is installed
    public static boolean isPackageInstalled(final Context context, final String package_name) {
        return isPackageInstalled(context, package_name, true);
    }

    // This method determines whether a specified package is installed (enabled OR disabled)
    public static boolean isPackageInstalled(
            final Context context,
            final String package_name,
            final boolean enabled) {
        try {
            final ApplicationInfo ai = context.getPackageManager().getApplicationInfo
                    (package_name, 0);
            final PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            if (enabled) return ai.enabled;
            // if package doesn't exist, an Exception will be thrown, so return true in every case
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    public static boolean isAvailablePackage(final Context context, final String packageName) {
        final PackageManager pm = context.getPackageManager();
        if (isPackageInstalled(context, packageName)) {
            try {
                final int enabled = pm.getApplicationEnabledSetting(packageName);
                return (enabled != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) &&
                        (enabled != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER);
            } catch (final Exception e) {
                return false;
            }
        }
        return false;
    }

    // This method validates the resources by their name in a specific package
    public static Boolean validateResource(final Context mContext, final String package_Name,
                                           final String resource_name, final String resource_type) {
        try {
            final Context context = mContext.createPackageContext(package_Name, 0);
            final android.content.res.Resources resources = context.getResources();
            final int drawablePointer = resources.getIdentifier(
                    resource_name, // Drawable name explicitly defined
                    resource_type, // Declared icon is a drawable, indeed.
                    package_Name); // Icon pack package name
            return drawablePointer != 0;
        } catch (final Exception e) {
            return false;
        }
    }

    // This method converts a vector drawable into a bitmap object
    public static Bitmap getBitmapFromVector(final Drawable drawable) {
        final Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap getBitmapFromDrawable(final Drawable drawable) {
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
                final Drawable backgroundDrawable = ((AdaptiveIconDrawable) drawable)
                        .getBackground();
                final Drawable foregroundDrawable = ((AdaptiveIconDrawable) drawable)
                        .getForeground();

                // Then we have to set up the drawable array to format these as an instantiation
                final Drawable[] drawableArray = new Drawable[2];
                drawableArray[0] = backgroundDrawable;
                drawableArray[1] = foregroundDrawable;

                // We then have to create a layered drawable based on the drawable array
                final LayerDrawable layerDrawable = new LayerDrawable(drawableArray);

                // Now set up the width and height of the output
                final int width = layerDrawable.getIntrinsicWidth();
                final int height = layerDrawable.getIntrinsicHeight();

                // Formulate the bitmap properly
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                // Draw the canvas
                final Canvas canvas = new Canvas(bitmap);

                // Finalize
                layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                layerDrawable.draw(canvas);
            }
        }
        return bitmap;
    }

    // This method obtains the application icon for a specified package
    public static Drawable getAppIcon(final Context context, final String package_name) {
        try {
            final Drawable icon;
            if (allowedSystemUIOverlay(package_name)) {
                icon = context.getPackageManager().getApplicationIcon("com.android.systemui");
            } else if (allowedSettingsOverlay(package_name)) {
                icon = context.getPackageManager().getApplicationIcon("com.android.settings");
            } else if (allowedFrameworkOverlay(package_name)) {
                icon = context.getPackageManager().getApplicationIcon("android");
            } else {
                icon = context.getPackageManager().getApplicationIcon(package_name);
            }
            return icon;
        } catch (final Exception e) {
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

    // This method obtains the overlay's compiler version
    public static int getOverlaySubstratumVersion(final Context context, final String
            package_name) {
        try {
            final ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                return appInfo.metaData.getInt(metadataOverlayVersion);
            }
        } catch (final Exception e) {
            // Suppress warning
        }
        return 0;
    }

    // This method obtains the overlay parent icon for specified package, returns self package icon
    // if not found
    public static Drawable getOverlayParentIcon(final Context context, final String package_name) {
        try {
            final ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if ((appInfo.metaData != null) &&
                    (appInfo.metaData.getString(metadataOverlayParent) != null)) {
                return getAppIcon(context, appInfo.metaData.getString(metadataOverlayParent));
            }
        } catch (final Exception e) {
            // Suppress warning
        }
        return getAppIcon(context, package_name);
    }

    public static List<ResolveInfo> getThemes(final Context context) {
        // Scavenge through the packages on the device with specific substratum metadata in
        // their manifest
        final PackageManager packageManager = context.getPackageManager();
        return packageManager.queryIntentActivities(new Intent(SUBSTRATUM_THEME),
                PackageManager.GET_META_DATA);
    }

    public static Collection<String> getThemesArray(final Context context) {
        final Collection<String> returnArray = new ArrayList<>();
        final List<ResolveInfo> themesResolveInfo = getThemes(context);
        for (int i = 0; i < themesResolveInfo.size(); i++) {
            returnArray.add(themesResolveInfo.get(i).activityInfo.packageName);
        }
        return returnArray;
    }

    // PackageName Crawling Methods
    public static String getAppVersion(final Context mContext, final String package_name) {
        try {
            final PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(package_name, 0);
            return pInfo.versionName;
        } catch (final Exception e) {
            // Suppress warning
        }
        return null;
    }

    public static int getAppVersionCode(final Context mContext, final String packageName) {
        try {
            final PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(packageName, 0);
            return pInfo.versionCode;
        } catch (final Exception e) {
            // Suppress warning
        }
        return 0;
    }

    public static String getThemeVersion(final Context mContext, final String package_name) {
        try {
            final PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(package_name, 0);
            return pInfo.versionName + " (" + pInfo.versionCode + ')';
        } catch (final PackageManager.NameNotFoundException e) {
            // Suppress warning
        }
        return null;
    }

    public static String getThemeAPIs(final Context mContext, final String package_name) {
        try {
            final ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                try {
                    if (appInfo.minSdkVersion == appInfo.targetSdkVersion) {
                        final int target = appInfo.targetSdkVersion;
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
                            final int min = appInfo.minSdkVersion;
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
                        final int target = appInfo.targetSdkVersion;
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
                } catch (final NoSuchFieldError noSuchFieldError) {
                    // The device is API 23 if it throws a NoSuchFieldError
                    if (appInfo.targetSdkVersion == 23) {
                        return mContext.getString(R.string.api_23);
                    } else {
                        String targetAPI = "";
                        final int target = appInfo.targetSdkVersion;
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
        } catch (final Exception e) {
            // Suppress warning
        }
        return null;
    }

    public static String getOverlayMetadata(
            final Context mContext,
            final String package_name,
            final String metadata) {
        try {
            final ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if ((appInfo.metaData != null) && (appInfo.metaData.getString(metadata) != null)) {
                return appInfo.metaData.getString(metadata);
            }
        } catch (final Exception e) {
            // Suppress warning
        }
        return null;
    }

    @SuppressWarnings("SameParameterValue")
    private static Boolean getOverlayMetadata(
            final Context mContext,
            final String package_name,
            final String metadata,
            final Boolean defaultValue) {
        try {
            final ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                return appInfo.metaData.getBoolean(metadata);
            }
        } catch (final Exception e) {
            // Suppress warning
        }
        return defaultValue;
    }

    public static int getOverlaySubstratumVersion(
            final Context mContext,
            final String package_name,
            final String metadata) {
        try {
            final ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                return appInfo.metaData.getInt(metadata);
            }
        } catch (final Exception e) {
            // Suppress warning
        }
        return 0;
    }

    // Get any resource from any package
    private static int getResource(final Context mContext,
                                   final String package_name,
                                   final String resourceName,
                                   final String type) {
        try {
            final android.content.res.Resources res =
                    mContext.getPackageManager().getResourcesForApplication(package_name);
            return res.getIdentifier(
                    package_name + ':' + type + '/' + resourceName,
                    type,
                    package_name);
        } catch (final Exception e) {
            // Suppress warning
        }
        return 0;
    }

    // Get Color Resource
    public static int getColorResource(final Context mContext, final String package_name, final
    String colorName) {
        return getResource(mContext, package_name, colorName, "color");
    }

    // Get Theme Changelog
    public static String[] getThemeChangelog(final Context mContext, final String package_name) {
        try {
            final android.content.res.Resources res =
                    mContext.getPackageManager().getResourcesForApplication(package_name);
            final int array_position = getResource(mContext, package_name, resourceChangelog,
                    "array");
            return res.getStringArray(array_position);
        } catch (final Exception e) {
            // Suppress warning
        }
        return null;
    }

    // Get Theme Hero Image
    public static Drawable getPackageHeroImage(final Context mContext, final String package_name,
                                               final boolean isThemesView) {
        Drawable hero = mContext.getDrawable(android.R.color.transparent); // Initialize to be clear
        try {
            final android.content.res.Resources res = mContext.getPackageManager()
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
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return hero;
    }

    // Get Overlay Target Package Name (Human Readable)
    public static String getPackageName(final Context mContext, final String package_name) {
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
        } catch (final Exception e) {
            ai = null;
        }
        return (String) ((ai != null) ? pm.getApplicationLabel(ai) : null);
    }

    // Get Theme Ready Metadata
    public static String getThemeReadyVisibility(final Context mContext, final String
            package_name) {
        return getOverlayMetadata(mContext, package_name, metadataThemeReady);
    }

    // Get Theme Plugin Metadata
    public static String getPackageTemplateVersion(final Context mContext, final String
            package_name) {
        final String template_version = getOverlayMetadata(mContext, package_name, metadataVersion);
        if (template_version != null) {
            return mContext.getString(R.string.plugin_template) + ": " + template_version;
        }
        return null;
    }

    // Get Overlay Parent
    public static String getOverlayParent(final Context mContext, final String package_name) {
        return getOverlayMetadata(mContext, package_name, metadataOverlayParent);
    }

    // Get Overlay Target
    public static String getOverlayTarget(final Context mContext, final String package_name) {
        return getOverlayMetadata(mContext, package_name, metadataOverlayTarget);
    }

    // Check if user application or not
    public static boolean isUserApp(final Context context, final String packageName) {
        try {
            final PackageManager pm = context.getPackageManager();
            final ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            final int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
            return (ai.flags & mask) == 0;
        } catch (final PackageManager.NameNotFoundException e) {
            // Suppress warning
        }
        return false;
    }

    // Check if theme is Samsung supported
    public static boolean isSamsungTheme(final Context context, final String package_name) {
        return getOverlayMetadata(context, package_name, metadataSamsungSupport, false);
    }

    // Obtain a live sample of the metadata in an app
    static boolean getMetaData(final Context context, final String trigger) {
        final List<ApplicationInfo> list =
                context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).packageName.startsWith(trigger)) {
                return true;
            }
        }
        return false;
    }

    public static void uninstallPackage(final Context context, final String packageName) {
        if (checkThemeInterfacer(context)) {
            final ArrayList<String> list = new ArrayList<>();
            list.add(packageName);
            ThemeInterfacerService.uninstallOverlays(context, list, false);
        } else {
            ElevatedCommands.runThreadedCommand("pm uninstall " + packageName);
        }
    }

    // This method checks whether these are legitimate packages for Substratum,
    // then mutates the input.
    @SuppressWarnings("unchecked")
    public static HashMap<String, String[]> getSubstratumPackages(final Context context,
                                                                  final CharSequence home_type,
                                                                  final String search_filter) {
        try {
            final HashMap returnMap = new HashMap<>();
            final List<ResolveInfo> listOfThemes = getThemes(context);
            for (final ResolveInfo ri : listOfThemes) {
                final String packageName = ri.activityInfo.packageName;
                final ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        packageName, PackageManager.GET_META_DATA);

                // By default, we will have to enforce continuation to false, if a poorly adapted
                // theme did not implement the proper meta data.
                Boolean can_continue = false;
                if ((appInfo.metaData.getString(metadataName) != null) &&
                        (appInfo.metaData.getString(metadataAuthor) != null) &&
                        (appInfo.metaData.getString(metadataVersion) != null)) {
                    // The theme app contains the proper metadata
                    can_continue = true;
                    // If the user is searching using the search bar
                    if ((search_filter != null) && !search_filter.isEmpty()) {
                        @SuppressWarnings("StringBufferReplaceableByString") final StringBuilder
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
                if (can_continue) {
                    // Let's prepare ourselves for appending into the hash map for this theme
                    final String[] data = {
                            appInfo.metaData.getString(metadataAuthor),
                            packageName
                    };
                    // Take the other package's context
                    final Context other = context.createPackageContext(packageName, 0);
                    // Check if it is wallpaper mode, if it is, bail out early
                    if (home_type.equals(wallpaperFragment)) {
                        final String wallpaperCheck = appInfo.metaData.getString
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
                                for (final Enumeration<? extends ZipEntry> e = zf.entries();
                                     e.hasMoreElements(); ) {
                                    final ZipEntry ze = e.nextElement();
                                    final String name = ze.getName();
                                    if (name.startsWith("assets/" + home_type + '/')) {
                                        returnMap.put(
                                                appInfo.metaData.getString(metadataName),
                                                data);
                                        break;
                                    }
                                }
                            } catch (final Exception e) {
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
        } catch (final Exception e) {
            // Suppress warning
        }
        return null;
    }

    // This method parses a specific overlay resource file (.xml) and returns the specified value
    public static String getOverlayResource(final InputStream overlay) {

        // We need to clone the InputStream so that we can ensure that the name and color are
        // mutually exclusive
        final byte[] byteArray;
        try {
            byteArray = IOUtils.toByteArray(overlay);
        } catch (final IOException e) {
            Log.e(SUBSTRATUM_LOG, "Unable to clone InputStream");
            return null;
        }

        String hex = null;
        try (InputStream in = new ByteArrayInputStream(byteArray)) {
            // Find the name of the top most color in the file first.
            final String resource_name = ReadVariantPrioritizedColor.run();

            if (resource_name != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.contains('"' + resource_name + '"')) {
                            final String[] split =
                                    line.substring(line.lastIndexOf("\">") + 2).split("<");
                            hex = split[0];
                            if (hex.startsWith("?")) hex = "#00000000";
                        }
                    }
                } catch (final IOException ioe) {
                    Log.e(SUBSTRATUM_LOG, "Unable to find " + resource_name + " in this overlay!");
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return hex;
    }

    // Obtain a live sample of the content providers in an app
    static boolean getProviders(final Context context, final String trigger) {
        final List<PackageInfo> list =
                context.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).packageName.startsWith(trigger)) {
                return true;
            }
        }
        return false;
    }

    // Obtain a live sample of the intents in an app
    static boolean getIntents(final Context context, final String trigger) {
        final Collection<Intent> intentArray = new ArrayList<>();
        intentArray.add(new Intent(Intent.ACTION_BOOT_COMPLETED));
        intentArray.add(new Intent(Intent.ACTION_PACKAGE_ADDED));
        intentArray.add(new Intent(Intent.ACTION_PACKAGE_CHANGED));
        intentArray.add(new Intent(Intent.ACTION_PACKAGE_REPLACED));
        intentArray.add(new Intent(Intent.ACTION_PACKAGE_REMOVED));
        intentArray.add(new Intent(Intent.ACTION_MEDIA_SCANNER_FINISHED));
        intentArray.add(new Intent(Intent.ACTION_MEDIA_SCANNER_STARTED));
        intentArray.add(new Intent(Intent.ACTION_MEDIA_MOUNTED));
        intentArray.add(new Intent(Intent.ACTION_MEDIA_REMOVED));
        for (final Intent intent : intentArray) {
            final List<ResolveInfo> activities =
                    context.getPackageManager().queryBroadcastReceivers(intent, 0);
            for (final ResolveInfo resolveInfo : activities) {
                final ActivityInfo activityInfo = resolveInfo.activityInfo;
                if ((activityInfo != null) && activityInfo.name.startsWith(trigger)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean needsRecreate(final Context context, final Iterable<String> list) {
        for (final String o : list) {
            if ("android".equals(o) || "projekt.substratum".equals(o)) {
                return false;
            }
        }
        return checkOMS(context);
    }

    // This method determines the installed directory of the overlay for legacy mode
    public static String getInstalledDirectory(final Context context, final String package_name) {
        final PackageManager pm = context.getPackageManager();
        for (final ApplicationInfo app : pm.getInstalledApplications(0)) {
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
