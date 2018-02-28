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

package projekt.substratum.util.compilers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.apksig.ApkSigner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Resources;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.CompilerCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;

import static projekt.substratum.common.References.BYPASS_SUBSTRATUM_BUILDER_DELETION;
import static projekt.substratum.common.References.ENABLE_DIRECT_ASSETS_LOGGING;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;
import static projekt.substratum.common.References.VENDOR_DIR;
import static projekt.substratum.common.Resources.SETTINGS;
import static projekt.substratum.common.Resources.SYSTEMUI;
import static projekt.substratum.common.commands.FileOperations.DA_LOG;

public class SubstratumBuilder {

    public boolean hasErroredOut = false;
    public boolean specialSnowflake = false;
    public String noInstall = "";
    private boolean debug = false;
    private String errorLogs = "";

    /**
     * Process the AAPT/AAPT commands to be used with the compilation binary
     *
     * @param workArea          Directory for the work area
     * @param targetPkg         Target package name
     * @param themeName         Theme's name
     * @param overlayPackage    Overlay package to be compiled
     * @param additionalVariant Additional variant (type2)
     * @param assetReplacement  Asset replacement (type4)
     * @param legacySwitch      Relates to the switch in Settings to fallback if referencing fails
     * @param context           Self explanatory, bud.
     * @param noCacheDir        Direct Assets directory
     * @return Returns a command that will be used with AAPT/AAPT
     */
    private static String processAAPTCommands(String workArea,
                                              String targetPkg,
                                              String themeName,
                                              String overlayPackage,
                                              CharSequence additionalVariant,
                                              CharSequence assetReplacement,
                                              boolean legacySwitch,
                                              Context context,
                                              String noCacheDir) {
        return CompilerCommands.createAAPTShellCommands(
                workArea,
                targetPkg,
                overlayPackage,
                themeName,
                legacySwitch,
                additionalVariant,
                assetReplacement,
                context,
                noCacheDir);
    }

    /**
     * Substratum Builder Build Function
     * <p>
     * Prior to running this function, you must have copied all the files to the working directory!
     *
     * @param context           self explanatory
     * @param overlayPackage    the target package to be overlaid (e.g. com.android.settings).
     * @param themeName         the theme's name to be stripped of symbols for the new package.
     * @param variant           a String flag to tell the compiler to build variant mode. This
     *                          could be the name of the variant spinner, or a package name for
     *                          OverlayUpdater (used in conjunction with overridePackage).
     * @param additionalVariant the additional variant (type2) that gets appended during aapt
     *                          compilation phase to the main /res folder.
     * @param baseVariant       this is linked to variable baseSpinner in Overlays.java, for
     *                          type3 base /res replacements.
     * @param versionName       the version to use for compiling the overlay's version.
     * @param themeOms          runs the check if the system is running in RRO or OMS
     * @param themeParent       the parent theme of the created overlay.
     * @param noCacheDir        where the compilation files will be placed.
     * @param type1a            String location of the type1a file
     * @param type1b            String location of the type1b file
     * @param type1c            String location of the type1c file
     * @param type2             String location of the type2 file
     * @param type3             String location of the type3 file
     * @param type4             String location of the type4 file
     * @param overridePackage   String package to tell whether we should change the package name
     * @param overlayUpdater    boolean flag to tell whether specialSnowflake should be skipped
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean beginAction(Context context,
                               String overlayPackage,
                               String themeName,
                               String variant,
                               String additionalVariant,
                               String baseVariant,
                               String versionName,
                               boolean themeOms,
                               String themeParent,
                               String noCacheDir,
                               String type1a,
                               String type1b,
                               String type1c,
                               String type2,
                               String type3,
                               String type4,
                               String overridePackage,
                               boolean overlayUpdater) {

        // 1. Initialize the setup
        File checkCompileFolder = new File(EXTERNAL_STORAGE_CACHE);
        if (!checkCompileFolder.exists() && !checkCompileFolder.mkdirs()) {
            Log.e(SUBSTRATUM_BUILDER, "Could not create compilation folder on external storage...");
        }
        hasErroredOut = false;
        debug = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("theme_debug", false);

        // 2. Set work area to asset chosen based on the parameter passed into this class
        String workArea = context.getCacheDir().getAbsolutePath() + SUBSTRATUM_BUILDER_CACHE;

        // 3. Create a modified Android Manifest for use with aapt
        // TODO: Need to git blame this file and find out what we removed

        // 4. Parse the theme's name before adding it into the new manifest to prevent any issues

        String parse2VariantName = "";
        if (variant != null) {
            String parse1VariantName = variant.replaceAll("\\s+", "");
            parse2VariantName = parse1VariantName.replaceAll("[^a-zA-Z0-9]+", "");
        }
        if (!parse2VariantName.isEmpty()) parse2VariantName = '.' + parse2VariantName;

        String parse2BaseName = "";
        if (baseVariant != null) {
            String parse1BaseName = baseVariant.replaceAll("\\s+", "");
            parse2BaseName = parse1BaseName.replaceAll("[^a-zA-Z0-9]+", "");
        }
        if (!parse2BaseName.isEmpty()) parse2BaseName = '.' + parse2BaseName;

        String parse1ThemeName = themeName.replaceAll("\\s+", "");
        String parse2ThemeName = parse1ThemeName.replaceAll("[^a-zA-Z0-9]+", "");
        if (parse2ThemeName != null && parse2ThemeName.isEmpty()) {
            parse2ThemeName = "no_name";
        }

        // 5. Create the manifest file based on the new parsed names
        String targetPackage = overlayPackage;
        if (Resources.allowedSettingsOverlay(overlayPackage)) {
            targetPackage = SETTINGS;
        }
        if (Resources.allowedSystemUIOverlay(overlayPackage)) {
            targetPackage = SYSTEMUI;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int legacyPriority = prefs.getInt("legacy_overlay_priority", References.DEFAULT_PRIORITY);
        if (!Systems.checkOMS(context)) {
            File workAreaArray = new File(workArea);

            if (Arrays.asList(workAreaArray.list()).contains("priority")) {
                Log.d(References.SUBSTRATUM_BUILDER,
                        "A specified priority file has been found for this overlay!");
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(
                                new File(workAreaArray.getAbsolutePath() + "/priority")
                        )))) {
                    legacyPriority = Integer.parseInt(reader.readLine());
                } catch (IOException ignored) {
                    dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                            "There was an error parsing priority file!");
                    legacyPriority =
                            prefs.getInt("legacy_overlay_priority", References.DEFAULT_PRIORITY);
                }
            }
            Log.d(References.SUBSTRATUM_BUILDER,
                    "The priority for this overlay is " + legacyPriority);
        }

        if (!hasErroredOut) {
            File root = new File(workArea + "/AndroidManifest.xml");
            try (FileWriter fw = new FileWriter(root);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter pw = new PrintWriter(bw)) {
                boolean created = root.createNewFile();
                if (!created)
                    if (variant != null) {
                        String manifest =
                                CompilerCommands.createOverlayManifest(
                                        context,
                                        overlayPackage,
                                        parse2ThemeName,
                                        parse2VariantName,
                                        parse2BaseName,
                                        versionName,
                                        targetPackage,
                                        themeParent,
                                        themeOms,
                                        legacyPriority,
                                        false,
                                        type1a,
                                        type1b,
                                        type1c,
                                        type2,
                                        type3,
                                        type4,
                                        ((overridePackage != null) &&
                                                !overridePackage.isEmpty()) ?
                                                overridePackage : "");
                        pw.write(manifest);
                    } else {
                        if (baseVariant != null) {
                            String manifest =
                                    CompilerCommands.createOverlayManifest(
                                            context,
                                            overlayPackage,
                                            parse2ThemeName,
                                            parse2VariantName,
                                            parse2BaseName,
                                            versionName,
                                            targetPackage,
                                            themeParent,
                                            themeOms,
                                            legacyPriority,
                                            false,
                                            type1a,
                                            type1b,
                                            type1c,
                                            type2,
                                            type3,
                                            type4,
                                            ((overridePackage != null) &&
                                                    !overridePackage.isEmpty()) ?
                                                    overridePackage : "");
                            pw.write(manifest);
                        } else {
                            String manifest =
                                    CompilerCommands.createOverlayManifest(
                                            context,
                                            overlayPackage,
                                            parse2ThemeName,
                                            parse2VariantName,
                                            parse2BaseName,
                                            versionName,
                                            targetPackage,
                                            themeParent,
                                            themeOms,
                                            legacyPriority,
                                            true,
                                            type1a,
                                            type1b,
                                            type1c,
                                            type2,
                                            type3,
                                            type4,
                                            ((overridePackage != null) &&
                                                    !overridePackage.isEmpty()) ?
                                                    overridePackage : "");
                            pw.write(manifest);
                        }
                    }
            } catch (Exception e) {
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage, e.getMessage());
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                        "There was an exception creating a new Manifest file!");
                hasErroredOut = true;
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                        "Installation of \"" + overlayPackage + "\" has failed.");
            }
        }

        // 6. Compile the new theme apk based on new manifest, framework-res.apk and extracted asset
        if (!hasErroredOut) {
            String targetPkg = Packages.getInstalledDirectory(context, targetPackage);
            String commands = SubstratumBuilder.processAAPTCommands(
                    workArea,
                    targetPkg,
                    parse2ThemeName,
                    overlayPackage,
                    additionalVariant,
                    type4,
                    false,
                    context,
                    noCacheDir);

            if (ENABLE_DIRECT_ASSETS_LOGGING) Log.d(DA_LOG, "Running commands: " + commands);

            hasErroredOut = !runAAPTShellCommands(
                    commands,
                    workArea,
                    targetPkg,
                    parse2ThemeName,
                    overlayPackage,
                    additionalVariant,
                    type4,
                    false,
                    context,
                    noCacheDir);
        }

        // 7. Zipalign the apk
        if (!hasErroredOut) {
            String source = workArea + '/' + overlayPackage + '.' + parse2ThemeName +
                    "-unsigned.apk";
            String destination = workArea + '/' + overlayPackage + '.' + parse2ThemeName +
                    "-unsigned-aligned.apk";
            String commands = CompilerCommands.createZipAlignShellCommands(context, source,
                    destination);

            Process nativeApp = null;
            try {
                nativeApp = Runtime.getRuntime().exec(commands);

                // We need this Process to be waited for before moving on to the next function.
                Log.d(References.SUBSTRATUM_BUILDER, "Aligning APK now...");
                nativeApp.waitFor();
                File alignedAPK = new File(destination);
                if (alignedAPK.isFile()) {
                    Log.d(References.SUBSTRATUM_BUILDER, "Zipalign successful!");
                } else {
                    dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                            "Zipalign has failed!");
                    hasErroredOut = true;
                    dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                            "Zipalign of \"" + overlayPackage + "\" has failed.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                        "Unfortunately, there was an exception trying to zipalign a new APK");
                hasErroredOut = true;
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                        "Installation of \"" + overlayPackage + "\" has failed.");
            } finally {
                if (nativeApp != null) {
                    nativeApp.destroy();
                }
            }
        }

        // 8. Sign the apk
        String overlayName = (variant == null) ?
                (overlayPackage + '.' + parse2ThemeName) :
                (overlayPackage + '.' + parse2ThemeName + parse2VariantName + parse2BaseName);
        if (!hasErroredOut) {
            try {
                // Delete the previous APK if it exists in the dashboard folder
                FileOperations.delete(context,
                        EXTERNAL_STORAGE_CACHE + overlayName + "-signed.apk");

                // Sign with the built-in test key/certificate.
                String source = workArea + '/' + overlayPackage + '.' + parse2ThemeName +
                        "-unsigned-aligned.apk";

                File key = new File(context.getDataDir() + "/key");
                char[] keyPass = "overlay".toCharArray();

                if (!key.exists()) {
                    Log.d(SUBSTRATUM_BUILDER, "Loading keystore...");
                    FileOperations.copyFromAsset(context, "key", key.getAbsolutePath());
                }

                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(new FileInputStream(key), keyPass);
                PrivateKey privateKey = (PrivateKey) keyStore.getKey("key", keyPass);
                List<X509Certificate> certs = new ArrayList<>();
                certs.add((X509Certificate) keyStore.getCertificateChain("key")[0]);

                ApkSigner.SignerConfig signerConfig =
                        new ApkSigner.SignerConfig.Builder("overlay", privateKey, certs).build();
                List<ApkSigner.SignerConfig> signerConfigs = new ArrayList<>();
                signerConfigs.add(signerConfig);
                ApkSigner.Builder apkSigner = new ApkSigner.Builder(signerConfigs);
                String destination = EXTERNAL_STORAGE_CACHE + overlayName + "-signed.apk";
                apkSigner
                        .setV1SigningEnabled(false)
                        .setV2SigningEnabled(true)
                        .setInputApk(new File(source))
                        .setOutputApk(new File(destination))
                        .setMinSdkVersion(Build.VERSION.SDK_INT)
                        .build()
                        .sign();

                Log.d(References.SUBSTRATUM_BUILDER, "APK successfully signed!");
            } catch (Throwable t) {
                t.printStackTrace();
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                        "APK could not be signed. " + t.toString());
                hasErroredOut = true;
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                        "Installation of \"" + overlayPackage + "\" has failed.");
            }
        }

        // 9. Install the APK silently
        // Superuser needed as this requires elevated privileges to run these commands
        if (!hasErroredOut) {
            if (themeOms) {
                specialSnowflake = false;
                if (Resources.FRAMEWORK.equals(overlayPackage) ||
                        "projekt.substratum".equals(overlayPackage)) {
                    specialSnowflake = ThemeManager.isOverlayEnabled(context, overlayName) ||
                            (Systems.checkOreo() && !overlayUpdater);
                }

                if (!specialSnowflake) {
                    try {
                        ThemeManager.installOverlay(context, EXTERNAL_STORAGE_CACHE +
                                overlayName + "-signed.apk");
                        Log.d(References.SUBSTRATUM_BUILDER, "Silently installing APK...");
                    } catch (Exception e) {
                        dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                                "Overlay APK has failed to install! \" (Exception) " +
                                        "[Error: " + e.getMessage() + ']');
                        hasErroredOut = true;
                        dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                                "Installation of \"" + overlayPackage + "\" has failed.");
                    }
                } else {
                    Log.d(References.SUBSTRATUM_BUILDER,
                            "Returning compiled APK path for later installation...");
                    noInstall = EXTERNAL_STORAGE_CACHE + overlayName + "-signed.apk";
                }
            } else {
                boolean isSamsung = Systems.isSamsungDevice(context);
                if (isSamsung) {
                    // Take account for Samsung's package manager installation mode
                    Log.d(References.SUBSTRATUM_BUILDER,
                            "Requesting PackageManager to launch signed overlay APK for " +
                                    "Samsung environment...");
                    noInstall = EXTERNAL_STORAGE_CACHE + overlayName + "-signed.apk";
                } else {
                    // At this point, it is detected to be legacy mode and Substratum will push to
                    // vendor/overlays directly.

                    FileOperations.mountRW();
                    // For Non-Nexus devices
                    if (!Resources.inNexusFilter()) {
                        String vendorLocation = LEGACY_NEXUS_DIR;
                        FileOperations.createNewFolder(vendorLocation);
                        FileOperations.move(context, EXTERNAL_STORAGE_CACHE + overlayName +
                                "-signed.apk", vendorLocation + overlayName + ".apk");
                        FileOperations.setPermissionsRecursively(644, vendorLocation);
                        FileOperations.setPermissions(755, vendorLocation);
                        FileOperations.setSystemFileContext(vendorLocation);
                    } else {
                        // For Nexus devices
                        FileOperations.mountRWVendor();
                        String vendorSymlink = PIXEL_NEXUS_DIR;
                        FileOperations.createNewFolder(vendorSymlink);
                        String vendorPartition = VENDOR_DIR;
                        FileOperations.createNewFolder(vendorPartition);
                        // On nexus devices, put framework overlay to /vendor/overlay/
                        if ("android".equals(overlayPackage)) {
                            String androidOverlay = vendorPartition + overlayName + ".apk";
                            FileOperations.move(context, EXTERNAL_STORAGE_CACHE + overlayName +
                                    "-signed.apk", androidOverlay);
                        } else {
                            String overlay = vendorSymlink + overlayName + ".apk";
                            FileOperations.move(context, EXTERNAL_STORAGE_CACHE + overlayName +
                                    "-signed.apk", overlay);
                            FileOperations.symlink(overlay, vendorPartition);
                        }
                        FileOperations.setPermissionsRecursively(644, vendorSymlink);
                        FileOperations.setPermissionsRecursively(644, vendorPartition);
                        FileOperations.setPermissions(755, vendorSymlink);
                        FileOperations.setPermissions(755, vendorPartition);
                        FileOperations.setSystemFileContext(vendorSymlink);
                        FileOperations.setSystemFileContext(vendorPartition);
                        FileOperations.mountROVendor();
                    }
                    FileOperations.mountRO();
                }
            }
        }

        // Finally, clean this compilation code's cache
        if (!BYPASS_SUBSTRATUM_BUILDER_DELETION) {
            String workingDirectory =
                    context.getCacheDir().getAbsolutePath() + SUBSTRATUM_BUILDER_CACHE;
            File deleted = new File(workingDirectory);
            FileOperations.delete(context, deleted.getAbsolutePath());
            if (!deleted.exists()) Log.d(References.SUBSTRATUM_BUILDER,
                    "Successfully cleared compilation cache!");
        }
        return !hasErroredOut;
    }

    /**
     * Returns a string of error logs during compilation
     *
     * @return Returns a string of error logs during compilation
     */
    public String getErrorLogs() {
        return errorLogs;
    }

    private boolean runAAPTShellCommands(String commands,
                                         String workArea,
                                         String targetPkg,
                                         String themeName,
                                         String overlayPackage,
                                         String additionalVariant,
                                         String assetReplacement,
                                         boolean legacySwitch,
                                         Context context,
                                         String noCacheDir) {
        Process nativeApp = null;
        try {
            nativeApp = Runtime.getRuntime().exec(commands);

            try (OutputStream stdin = nativeApp.getOutputStream();
                 InputStream stderr = nativeApp.getErrorStream()) {
                stdin.write(("ls\n").getBytes());
                stdin.write("exit\n".getBytes());

                boolean errored = false;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(stderr))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.contains("types not allowed") && !legacySwitch && !debug) {
                            Log.e(References.SUBSTRATUM_BUILDER,
                                    "This overlay was designed using a legacy theming " +
                                            "style, now falling back to legacy compiler...");
                            String newCommands = SubstratumBuilder.processAAPTCommands
                                    (workArea, targetPkg,
                                            themeName, overlayPackage, additionalVariant,
                                            assetReplacement, true, context, noCacheDir);
                            return runAAPTShellCommands(
                                    newCommands, workArea, targetPkg, themeName,
                                    overlayPackage, additionalVariant, assetReplacement,
                                    true, context, noCacheDir);
                        } else {
                            dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                                    line);
                            errored = true;
                        }
                    }
                }
                if (errored) {
                    hasErroredOut = true;
                    dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                            "Installation of \"" + overlayPackage + "\" has failed.");
                } else {
                    // We need this Process to be waited for before moving on to the next function.
                    Log.d(References.SUBSTRATUM_BUILDER, "Overlay APK creation is running now...");
                    nativeApp.waitFor();
                    File unsignedAPK = new File(workArea + '/' + overlayPackage + '.' +
                            themeName + "-unsigned.apk");
                    if (unsignedAPK.isFile()) {
                        Log.d(References.SUBSTRATUM_BUILDER, "Overlay APK creation has completed!");
                        return true;
                    } else {
                        dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                                "Overlay APK creation has failed!");
                        hasErroredOut = true;
                        dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                                "Installation of \"" + overlayPackage + "\" has failed.");
                    }
                }
            }
        } catch (IOException ioe) {
            if (Systems.checkOMS(context)) {
                Log.d(SUBSTRATUM_BUILDER, "An Android Oreo specific error message has been " +
                        "detected and has been whitelisted to continue moving forward " +
                        "with overlay compilation.");
                return !hasErroredOut;
            } else {
                ioe.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                    "Unfortunately, there was an exception trying to create a new APK");
            hasErroredOut = true;
            dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlayPackage,
                    "Installation of \"" + overlayPackage + "\" has failed.");
        } finally {
            if (nativeApp != null) {
                nativeApp.destroy();
            }
        }
        return false;
    }

    /**
     * Save a series of error logs to be callable
     *
     * @param tag     Internal logcat tag
     * @param overlay Overlay that has failed to compile
     * @param message Failure message
     */
    private void dumpErrorLogs(String tag, String overlay, String message) {
        if (!message.isEmpty()) {
            Log.e(tag, message);
            if (errorLogs.isEmpty()) {
                errorLogs = "» [" + overlay + "]: " + message;
            } else {
                errorLogs += '\n' + "» [" + overlay + "]: " + message;
            }
        }
    }
}