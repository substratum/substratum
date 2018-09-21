/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.compilers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import com.android.apksig.ApkSigner;
import projekt.substratum.Substratum;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Resources;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.CompilerCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;

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

import static projekt.substratum.common.Packages.getLiveOverlayVersion;
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
     * @param isDeviceOMS       runs the check if the system is running in RRO or OMS
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
                               boolean isDeviceOMS,
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
        debug = Substratum.getPreferences().getBoolean("theme_debug", false);

        // 2. Set work area to asset chosen based on the parameter passed into this class
        String workArea = context.getCacheDir().getAbsolutePath() + SUBSTRATUM_BUILDER_CACHE;

        // 3. Parse the theme's name before adding it into the new manifest to prevent any issues

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

        // 4. Create the manifest file based on the new parsed names
        String targetPackage = overlayPackage;
        if (Resources.allowedSettingsOverlay(overlayPackage)) {
            targetPackage = SETTINGS;
        }
        if (Resources.allowedSystemUIOverlay(overlayPackage)) {
            targetPackage = SYSTEMUI;
        }

        SharedPreferences prefs = Substratum.getPreferences();
        int legacyPriority = prefs.getInt("legacy_overlay_priority", References.DEFAULT_PRIORITY);
        if (!Systems.checkOMS(context)) {
            File workAreaArray = new File(workArea);

            if (Arrays.asList(workAreaArray.list()).contains("priority")) {
                Substratum.log(References.SUBSTRATUM_BUILDER,
                        "A specified priority file has been found for this overlay!");
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(
                                new File(workAreaArray.getAbsolutePath() + "/priority")
                        )))) {
                    legacyPriority = Integer.parseInt(reader.readLine());
                } catch (IOException ignored) {
                    dumpErrorLogs(overlayPackage,
                            "There was an error parsing priority file!");
                    legacyPriority =
                            prefs.getInt("legacy_overlay_priority", References.DEFAULT_PRIORITY);
                }
            }
            Substratum.log(References.SUBSTRATUM_BUILDER,
                    "The priority for this overlay is " + legacyPriority);
        }

        String overlayVersionCode =
                String.valueOf(getLiveOverlayVersion(context, themeParent, targetPackage));
        if (!overlayVersionCode.equals("0"))
            Substratum.log(References.SUBSTRATUM_BUILDER,
                    "The version for this overlay is " + overlayVersionCode);

        if (!hasErroredOut) {
            File root = new File(workArea + "/AndroidManifest.xml");
            try (FileWriter fw = new FileWriter(root);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter pw = new PrintWriter(bw)) {
                boolean created = root.createNewFile();
                String manifest = "";
                if (!created)
                    if (variant != null) {
                        manifest = CompilerCommands.createOverlayManifest(
                                        context,
                                        overlayPackage,
                                        parse2ThemeName,
                                        parse2VariantName,
                                        parse2BaseName,
                                        versionName,
                                        overlayVersionCode,
                                        targetPackage,
                                        themeParent,
                                        isDeviceOMS,
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
                    } else {
                        manifest = CompilerCommands.createOverlayManifest(
                                        context,
                                        overlayPackage,
                                        parse2ThemeName,
                                        parse2VariantName,
                                        parse2BaseName,
                                        versionName,
                                        overlayVersionCode,
                                        targetPackage,
                                        themeParent,
                                        isDeviceOMS,
                                        legacyPriority,
                                        baseVariant == null,
                                        type1a,
                                        type1b,
                                        type1c,
                                        type2,
                                        type3,
                                        type4,
                                        ((overridePackage != null) &&
                                                !overridePackage.isEmpty()) ?
                                                overridePackage : "");
                    }
                pw.write(manifest);
            } catch (Exception e) {
                dumpErrorLogs(overlayPackage, e.getMessage());
                dumpErrorLogs(overlayPackage,
                        "There was an exception creating a new Manifest file!");
                hasErroredOut = true;
                dumpErrorLogs(overlayPackage,
                        "Installation of \"" + overlayPackage + "\" has failed.");
            }
        }

        // 5. Compile the new theme apk based on new manifest, framework-res.apk and extracted asset
        if (!hasErroredOut) {
            String targetPkg = Packages.getInstalledDirectory(context, targetPackage);
            String commands = CompilerCommands.createAAPTShellCommands(
                    workArea,
                    targetPkg,
                    overlayPackage,
                    parse2ThemeName,
                    false,
                    additionalVariant,
                    type4,
                    context,
                    noCacheDir);

            if (ENABLE_DIRECT_ASSETS_LOGGING)
                Substratum.log(DA_LOG, "Running commands: " + commands);

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

        // 6. Zipalign the apk
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
                Substratum.log(References.SUBSTRATUM_BUILDER, "Aligning APK now...");
                nativeApp.waitFor();
                File alignedAPK = new File(destination);
                if (alignedAPK.isFile()) {
                    Substratum.log(References.SUBSTRATUM_BUILDER, "Zipalign successful!");
                } else {
                    dumpErrorLogs(overlayPackage,
                            "Zipalign has failed!");
                    hasErroredOut = true;
                    dumpErrorLogs(overlayPackage,
                            "Zipalign of \"" + overlayPackage + "\" has failed.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                dumpErrorLogs(overlayPackage,
                        "Unfortunately, there was an exception trying to zipalign a new APK");
                hasErroredOut = true;
                dumpErrorLogs(overlayPackage,
                        "Installation of \"" + overlayPackage + "\" has failed.");
            } finally {
                if (nativeApp != null) {
                    nativeApp.destroy();
                }
            }
        }

        // 7. Sign the apk
        String overlayName = (variant == null) ?
                (overlayPackage + '.' + parse2ThemeName) :
                (overlayPackage + '.' + parse2ThemeName + parse2VariantName + parse2BaseName);
        String signedOverlayAPKPath = EXTERNAL_STORAGE_CACHE + overlayName + "-signed.apk";
        if (!hasErroredOut) {
            try {
                // Delete the previous APK if it exists in the dashboard folder
                FileOperations.delete(context, signedOverlayAPKPath);

                // Sign with the built-in test key/certificate.
                String source = workArea + '/' + overlayPackage + '.' + parse2ThemeName +
                        "-unsigned-aligned.apk";

                File key = new File(context.getDataDir() + "/key");
                char[] keyPass = "overlay".toCharArray();

                if (!key.exists()) {
                    Substratum.log(SUBSTRATUM_BUILDER, "Loading keystore...");
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
                new ApkSigner.Builder(signerConfigs)
                        .setV1SigningEnabled(false)
                        .setV2SigningEnabled(true)
                        .setInputApk(new File(source))
                        .setOutputApk(new File(signedOverlayAPKPath))
                        .setMinSdkVersion(Build.VERSION.SDK_INT)
                        .build()
                        .sign();

                Substratum.log(References.SUBSTRATUM_BUILDER, "APK successfully signed!");
            } catch (Throwable t) {
                t.printStackTrace();
                dumpErrorLogs(overlayPackage,
                        "APK could not be signed. " + t.toString());
                hasErroredOut = true;
                dumpErrorLogs(overlayPackage,
                        "Installation of \"" + overlayPackage + "\" has failed.");
            }
        }

        // 8. Install the APK silently
        // Superuser needed as this requires elevated privileges to run these commands
        if (!hasErroredOut) {
            if (isDeviceOMS) {
                if (Systems.IS_PIE && !Systems.checkSubstratumService(context)) {
                    // Brute force install APKs because thanks Google
                    FileOperations.mountRW();
                    final String overlay = References.getPieDir() + "_" + overlayName + ".apk";
                    FileOperations.move(context, signedOverlayAPKPath, overlay);
                    FileOperations.setPermissions(644, overlay);
                    FileOperations.mountRO();
                } else if (!Systems.isNewSamsungDeviceAndromeda(context)) {
                    specialSnowflake = false;
                    if (Resources.FRAMEWORK.equals(overlayPackage) ||
                            "projekt.substratum".equals(overlayPackage)) {
                        specialSnowflake = ThemeManager.isOverlayEnabled(context, overlayName) ||
                                (Systems.IS_OREO && !overlayUpdater);
                    }

                    if (!specialSnowflake) {
                        try {
                            ThemeManager.installOverlay(context, signedOverlayAPKPath);
                            Substratum.log(References.SUBSTRATUM_BUILDER, "Silently installing APK...");
                        } catch (Exception e) {
                            dumpErrorLogs(overlayPackage,
                                    "Overlay APK has failed to install! \" (Exception) " +
                                            "[Error: " + e.getMessage() + ']');
                            hasErroredOut = true;
                            dumpErrorLogs(overlayPackage,
                                    "Installation of \"" + overlayPackage + "\" has failed.");
                        }
                    } else {
                        Substratum.log(References.SUBSTRATUM_BUILDER,
                                "Returning compiled APK path for later installation...");
                        noInstall = signedOverlayAPKPath;
                    }
                }
            } else {
                if (Systems.isSamsungDevice(context)) {
                    // Take account for Samsung's package manager installation mode
                    Substratum.log(References.SUBSTRATUM_BUILDER,
                            "Requesting PackageManager to launch signed overlay APK for " +
                                    "Samsung environment...");
                    noInstall = signedOverlayAPKPath;
                } else {
                    // At this point, it is detected to be legacy mode and Substratum will push to
                    // vendor/overlays directly.

                    FileOperations.mountRW();
                    // For Non-Nexus devices
                    if (!Resources.inNexusFilter()) {
                        String vendorLocation = LEGACY_NEXUS_DIR;
                        FileOperations.createNewFolder(vendorLocation);
                        FileOperations.move(context, signedOverlayAPKPath,
                                vendorLocation + overlayName + ".apk");
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
                            FileOperations.move(context, signedOverlayAPKPath
                                    , androidOverlay);
                        } else {
                            String overlay = vendorSymlink + overlayName + ".apk";
                            FileOperations.move(context, signedOverlayAPKPath
                                    , overlay);
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
            if (!deleted.exists()) Substratum.log(References.SUBSTRATUM_BUILDER,
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
                            String newCommands = CompilerCommands.createAAPTShellCommands(workArea, targetPkg,
                                            overlayPackage, themeName, true, additionalVariant,
                                            assetReplacement, context, noCacheDir);
                            return runAAPTShellCommands(
                                    newCommands, workArea, targetPkg, themeName,
                                    overlayPackage, additionalVariant, assetReplacement,
                                    true, context, noCacheDir);
                        } else {
                            dumpErrorLogs(overlayPackage,
                                    line);
                            errored = true;
                        }
                    }
                }
                if (errored) {
                    hasErroredOut = true;
                    dumpErrorLogs(overlayPackage,
                            "Installation of \"" + overlayPackage + "\" has failed.");
                } else {
                    // We need this Process to be waited for before moving on to the next function.
                    Substratum.log(References.SUBSTRATUM_BUILDER, "Overlay APK creation is running now...");
                    nativeApp.waitFor();
                    File unsignedAPK = new File(workArea + '/' + overlayPackage + '.' +
                            themeName + "-unsigned.apk");
                    if (unsignedAPK.isFile()) {
                        Substratum.log(References.SUBSTRATUM_BUILDER, "Overlay APK creation has completed!");
                        return true;
                    } else {
                        dumpErrorLogs(overlayPackage,
                                "Overlay APK creation has failed!");
                        hasErroredOut = true;
                        dumpErrorLogs(overlayPackage,
                                "Installation of \"" + overlayPackage + "\" has failed.");
                    }
                }
            }
        } catch (IOException ioe) {
            if (Systems.checkOMS(context) || Systems.isNewSamsungDeviceAndromeda(context)) {
                Substratum.log(SUBSTRATUM_BUILDER, "An Android Oreo/Pie specific error message has been " +
                        "detected and has been whitelisted to continue moving forward " +
                        "with overlay compilation.");
                return !hasErroredOut;
            } else {
                ioe.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            dumpErrorLogs(overlayPackage,
                    "Unfortunately, there was an exception trying to create a new APK");
            hasErroredOut = true;
            dumpErrorLogs(overlayPackage,
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
     * @param overlay Overlay that has failed to compile
     * @param message Failure message
     */
    private void dumpErrorLogs(String overlay, String message) {
        if (!message.isEmpty()) {
            Log.e(References.SUBSTRATUM_BUILDER, message);
            if (errorLogs.isEmpty()) {
                errorLogs = "» [" + overlay + "]: " + message;
            } else {
                errorLogs += '\n' + "» [" + overlay + "]: " + message;
            }
        }
    }
}