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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import projekt.substratum.common.References;
import projekt.substratum.common.commands.CompilerCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.key.CertificateGenerator;

import static projekt.substratum.common.References.BYPASS_SUBSTRATUM_BUILDER_DELETION;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;
import static projekt.substratum.common.References.VENDOR_DIR;

public class SubstratumBuilder {

    public Boolean has_errored_out = false;
    public boolean special_snowflake;
    public String no_install = "";
    private String error_logs = "";
    private boolean debug;

    public String getErrorLogs() {
        return error_logs;
    }

    /**
     * Substratum Builder Build Function
     * <p>
     * Prior to running this function, you must have copied all the files to the working directory!
     *
     * @param context            self explanatory
     * @param theme_pid          the package ID used to compile the APK.
     * @param overlay_package    the target package to be overlaid (e.g. com.android.settings).
     * @param theme_name         the theme's name to be stripped of symbols for the new package.
     * @param variant            a String flag to tell the compiler to build variant mode. This
     *                           could be the name of the variant spinner, or a package name for
     *                           OverlayUpdater (used in conjunction with override_package).
     * @param additional_variant the additional variant (type2) that gets appended during aopt
     *                           compilation phase to the main /res folder.
     * @param base_variant       this is linked to variable base_spinner in Overlays.java, for
     *                           type3 base /res replacements.
     * @param versionName        the version to use for compiling the overlay's version.
     * @param theme_oms          runs the check if the system is running in RRO or OMS
     * @param theme_parent       the parent theme of the created overlay.
     * @param no_cache_dir       where the compilation files will be placed.
     * @param type1a             String location of the type1a file
     * @param type1b             String location of the type1b file
     * @param type1c             String location of the type1c file
     * @param type2              String location of the type2 file
     * @param type3              String location of the type3 file
     */
    @SuppressWarnings({"ConstantConditions", "UnusedReturnValue"})
    public boolean beginAction(Context context,
                               String theme_pid,
                               String overlay_package,
                               String theme_name,
                               String variant,
                               String additional_variant,
                               String base_variant,
                               String versionName,
                               Boolean theme_oms,
                               String theme_parent,
                               String no_cache_dir,
                               String type1a,
                               String type1b,
                               String type1c,
                               String type2,
                               String type3,
                               String override_package) {
        File checkCompileFolder = new File(EXTERNAL_STORAGE_CACHE);
        if (!checkCompileFolder.exists() && !checkCompileFolder.mkdirs()) {
            Log.e(SUBSTRATUM_BUILDER, "Could not create compilation folder on external storage...");
        }

        has_errored_out = false;

        debug = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("theme_debug", false);

        // 1. Quickly filter out what kind of type this overlay will be compiled with
        int typeMode = 1;
        if (additional_variant != null) {
            typeMode = 2;
        }

        // 2. Set work area to asset chosen based on the parameter passed into this class
        String work_area;
        if (References.isCachingEnabled(context)) {
            work_area = context.getCacheDir().getAbsolutePath() + SUBSTRATUM_BUILDER_CACHE +
                    theme_pid + "/assets/overlays/" + overlay_package;
        } else {
            work_area = context.getCacheDir().getAbsolutePath() + SUBSTRATUM_BUILDER_CACHE;
        }

        if (!theme_oms) {
            File check_legacy = new File(context.getCacheDir().getAbsolutePath() +
                    SUBSTRATUM_BUILDER_CACHE + theme_pid + "/assets/overlays_legacy/" +
                    overlay_package);
            if (check_legacy.exists()) {
                work_area = check_legacy.getAbsolutePath();
            }
        }

        // 3. Create a modified Android Manifest for use with aopt
        File root = new File(work_area + "/AndroidManifest.xml");

        // 4. Parse the theme's name before adding it into the new manifest to prevent any issues
        String parse1_themeName = theme_name.replaceAll("\\s+", "");
        String parse2_themeName = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");

        String parse2_variantName = "";
        if (variant != null) {
            String parse1_variantName = variant.replaceAll("\\s+", "");
            parse2_variantName = parse1_variantName.replaceAll("[^a-zA-Z0-9]+", "");
        }
        if (parse2_variantName.length() > 0) parse2_variantName = "." + parse2_variantName;

        String parse2_baseName = "";
        if (base_variant != null) {
            String parse1_baseName = base_variant.replaceAll("\\s+", "");
            parse2_baseName = parse1_baseName.replaceAll("[^a-zA-Z0-9]+", "");
        }
        if (parse2_baseName.length() > 0) parse2_baseName = "." + parse2_baseName;

        if (parse2_themeName.equals("")) {
            parse2_themeName = "no_name";
        }

        // 5. Create the manifest file based on the new parsed names
        String varianter = parse2_variantName + parse2_baseName;
        varianter = varianter.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");

        String targetPackage = overlay_package;
        if (References.allowedSettingsOverlay(overlay_package)) {
            targetPackage = "com.android.settings";
        }
        if (References.allowedSystemUIOverlay(overlay_package)) {
            targetPackage = "com.android.systemui";
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int legacy_priority = prefs.getInt("legacy_overlay_priority", References.DEFAULT_PRIORITY);
        if (!References.checkOMS(context)) {
            File work_area_array = new File(work_area);

            if (Arrays.asList(work_area_array.list()).contains("priority")) {
                Log.d(References.SUBSTRATUM_BUILDER,
                        "A specified priority file has been found for this overlay!");
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(
                                new File(work_area_array.getAbsolutePath() + "/priority")
                        )))) {
                    legacy_priority = Integer.parseInt(reader.readLine());
                } catch (IOException e) {
                    dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                            "There was an error parsing priority file!");
                    legacy_priority =
                            prefs.getInt("legacy_overlay_priority", References.DEFAULT_PRIORITY);
                }
            }
            Log.d(References.SUBSTRATUM_BUILDER,
                    "The priority for this overlay is " + legacy_priority);
        }

        if (!has_errored_out) {
            try (FileWriter fw = new FileWriter(root);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter pw = new PrintWriter(bw)) {
                Boolean created = root.createNewFile();
                if (!created)
                    if (variant != null) {
                        String manifest =
                                CompilerCommands.createOverlayManifest(
                                        context,
                                        overlay_package,
                                        parse2_themeName,
                                        parse2_variantName,
                                        parse2_baseName,
                                        versionName,
                                        targetPackage,
                                        theme_parent,
                                        theme_oms,
                                        legacy_priority,
                                        false,
                                        type1a,
                                        type1b,
                                        type1c,
                                        type2,
                                        type3,
                                        (override_package != null &&
                                                override_package.length() > 0) ?
                                                override_package : "");
                        pw.write(manifest);
                    } else {
                        if (base_variant != null) {
                            String manifest =
                                    CompilerCommands.createOverlayManifest(
                                            context,
                                            overlay_package,
                                            parse2_themeName,
                                            parse2_variantName,
                                            parse2_baseName,
                                            versionName,
                                            targetPackage,
                                            theme_parent,
                                            theme_oms,
                                            legacy_priority,
                                            false,
                                            type1a,
                                            type1b,
                                            type1c,
                                            type2,
                                            type3,
                                            (override_package != null &&
                                                    override_package.length() > 0) ?
                                                    override_package : "");
                            pw.write(manifest);
                        } else {
                            String manifest =
                                    CompilerCommands.createOverlayManifest(
                                            context,
                                            overlay_package,
                                            parse2_themeName,
                                            parse2_variantName,
                                            parse2_baseName,
                                            versionName,
                                            targetPackage,
                                            theme_parent,
                                            theme_oms,
                                            legacy_priority,
                                            true,
                                            type1a,
                                            type1b,
                                            type1c,
                                            type2,
                                            type3,
                                            (override_package != null &&
                                                    override_package.length() > 0) ?
                                                    override_package : "");
                            pw.write(manifest);
                        }
                    }
            } catch (Exception e) {
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package, e.getMessage());
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                        "There was an exception creating a new Manifest file!");
                has_errored_out = true;
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                        "Installation of \"" + overlay_package + "\" has failed.");
            }
        }

        // 6. Compile the new theme apk based on new manifest, framework-res.apk and extracted asset
        if (!has_errored_out) {
            String targetPkg = References.getInstalledDirectory(context, targetPackage);
            String commands = processAOPTCommands(
                    work_area,
                    targetPkg,
                    parse2_themeName,
                    overlay_package,
                    variant,
                    additional_variant,
                    typeMode,
                    false,
                    context,
                    no_cache_dir);

            has_errored_out = !runShellCommands(
                    commands,
                    work_area,
                    targetPkg,
                    parse2_themeName,
                    overlay_package,
                    variant,
                    additional_variant,
                    typeMode,
                    false,
                    context,
                    no_cache_dir);
        }

        // 7. Sign the apk
        if (!has_errored_out) {
            try {
                // Delete the previous APK if it exists in the dashboard folder
                FileOperations.delete(context,
                        EXTERNAL_STORAGE_CACHE + overlay_package + "." + parse2_themeName +
                                "-signed.apk");

                // Sign with the built-in test key/certificate.
                String source = work_area + "/" + overlay_package + "." + parse2_themeName +
                        "-unsigned.apk";
                String destination =
                        EXTERNAL_STORAGE_CACHE + overlay_package + "." + parse2_themeName +
                                "-signed.apk";

                File key = new File(context.getDataDir() + "/key");
                char[] keyPass = "overlay".toCharArray();

                if (!key.exists()) {
                    Log.d(SUBSTRATUM_BUILDER, "generating new keystore...");
                    // Generate private key
                    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                    keyGen.initialize(1024, SecureRandom.getInstance("SHA1PRNG"));
                    KeyPair keyPair = keyGen.generateKeyPair();
                    PrivateKey privateKey = keyPair.getPrivate();

                    // Generate certificate
                    X509Certificate[] chain = new X509Certificate[1];
                    X509Certificate certificate =
                            CertificateGenerator.generateX509Certificate(keyPair);
                    chain[0] = certificate;

                    // Store new keystore
                    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    keyStore.load(null, null);
                    keyStore.setKeyEntry("key", privateKey, keyPass, chain);
                    keyStore.setCertificateEntry("cert", certificate);
                    keyStore.store(new FileOutputStream(key), keyPass);
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
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                        "APK could not be signed. " + t.toString());
                has_errored_out = true;
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                        "Installation of \"" + overlay_package + "\" has failed.");
            }
        }

        // 8. Install the APK silently
        // Superuser needed as this requires elevated privileges to run these commands
        if (!has_errored_out) {
            if (theme_oms) {
                special_snowflake = false;
                if (overlay_package.equals("android") ||
                        overlay_package.equals("projekt.substratum")) {
                    String overlayName = variant == null ?
                            overlay_package + "." + parse2_themeName :
                            overlay_package + "." + parse2_themeName + "." + varianter;
                    special_snowflake = ThemeManager.isOverlayEnabled(context, overlayName);
                }

                if (!special_snowflake) {
                    try {
                        ThemeManager.installOverlay(context, EXTERNAL_STORAGE_CACHE +
                                overlay_package + "." + parse2_themeName + "-signed.apk");
                        Log.d(References.SUBSTRATUM_BUILDER, "Silently installing APK...");
                    } catch (Exception e) {
                        dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                "Overlay APK has failed to install! \" (Exception) " +
                                        "[Error: " + e.getMessage() + "]");
                        has_errored_out = true;
                        dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                "Installation of \"" + overlay_package + "\" has failed.");
                    }
                } else {
                    Log.d(References.SUBSTRATUM_BUILDER,
                            "Returning compiled APK path for later installation...");

                    if (variant != null) {
                        no_install =
                                EXTERNAL_STORAGE_CACHE + overlay_package + "." +
                                        parse2_themeName + "." + varianter + "-signed.apk";
                    } else {
                        no_install =
                                EXTERNAL_STORAGE_CACHE + overlay_package + "." +
                                        parse2_themeName + "-signed.apk";
                    }
                }
            } else {
                Boolean isSamsung = References.isSamsung(context);
                if (isSamsung) {
                    // Take account for Samsung's package manager installation mode
                    Log.d(References.SUBSTRATUM_BUILDER,
                            "Requesting PackageManager to launch signed overlay APK for " +
                                    "Samsung environment...");
                    no_install = EXTERNAL_STORAGE_CACHE + overlay_package +
                            "." + parse2_themeName + "-signed.apk";
                } else {
                    // At this point, it is detected to be legacy mode and Substratum will push to
                    // vendor/overlays directly.
                    String vendor_location = LEGACY_NEXUS_DIR;
                    String vendor_partition = VENDOR_DIR;
                    String vendor_symlink = PIXEL_NEXUS_DIR;

                    FileOperations.mountRW();
                    // For Non-Nexus devices
                    if (!References.inNexusFilter()) {
                        FileOperations.createNewFolder(vendor_location);
                        FileOperations.move(context, EXTERNAL_STORAGE_CACHE + overlay_package +
                                "." + parse2_themeName + "-signed.apk", vendor_location +
                                overlay_package + "." + parse2_themeName +
                                (variant == null ? "" : "." + varianter) + ".apk");
                        FileOperations.setPermissionsRecursively(644, vendor_location);
                        FileOperations.setPermissions(755, vendor_location);
                        FileOperations.setContext(vendor_location);
                    } else {
                        // For Nexus devices
                        FileOperations.mountRWVendor();
                        FileOperations.createNewFolder(vendor_symlink);
                        FileOperations.createNewFolder(vendor_partition);
                        // On nexus devices, put framework overlay to /vendor/overlay/
                        if (overlay_package.equals("android")) {
                            String android_overlay = vendor_partition + overlay_package + "."
                                    + parse2_themeName + (variant == null ? "" : "." + varianter) +
                                    ".apk";
                            FileOperations.move(context, EXTERNAL_STORAGE_CACHE + overlay_package +
                                    "." + parse2_themeName + "-signed.apk", android_overlay);
                        } else {
                            String overlay = vendor_symlink + overlay_package + "." +
                                    parse2_themeName + (variant == null ? "" : "." + varianter) +
                                    ".apk";
                            FileOperations.move(context, EXTERNAL_STORAGE_CACHE + overlay_package +
                                    "." + parse2_themeName + "-signed.apk", overlay);
                            FileOperations.symlink(overlay, vendor_partition);
                        }
                        FileOperations.setPermissionsRecursively(644, vendor_symlink);
                        FileOperations.setPermissionsRecursively(644, vendor_partition);
                        FileOperations.setPermissions(755, vendor_symlink);
                        FileOperations.setPermissions(755, vendor_partition);
                        FileOperations.setContext(vendor_symlink);
                        FileOperations.setContext(vendor_partition);
                        FileOperations.mountROVendor();
                    }
                    FileOperations.mountRO();
                }
            }
        }

        // Finally, clean this compilation code's cache
        if (!BYPASS_SUBSTRATUM_BUILDER_DELETION && !References.isCachingEnabled(context)) {
            String workingDirectory =
                    context.getCacheDir().getAbsolutePath() + SUBSTRATUM_BUILDER_CACHE;
            File deleted = new File(workingDirectory);
            FileOperations.delete(context, deleted.getAbsolutePath());
            if (!deleted.exists()) Log.d(References.SUBSTRATUM_BUILDER,
                    "Successfully cleared compilation cache!");
        }
        return !has_errored_out;
    }

    private String processAOPTCommands(String work_area,
                                       String targetPkg,
                                       String theme_name,
                                       String overlay_package,
                                       String variant,
                                       String additional_variant,
                                       int typeMode,
                                       boolean legacySwitch,
                                       Context context,
                                       String no_cache_dir) {
        String commands;
        if (typeMode == 1) {
            commands = CompilerCommands.createAOPTShellCommands(
                    work_area,
                    targetPkg,
                    overlay_package,
                    theme_name,
                    legacySwitch,
                    null,
                    context,
                    no_cache_dir
            );
        } else {
            if (variant != null) {
                commands = CompilerCommands.createAOPTShellCommands(
                        work_area,
                        targetPkg,
                        overlay_package,
                        theme_name,
                        legacySwitch,
                        additional_variant,
                        context,
                        no_cache_dir
                );
            } else {
                commands = CompilerCommands.createAOPTShellCommands(
                        work_area,
                        targetPkg,
                        overlay_package,
                        theme_name,
                        legacySwitch,
                        null,
                        context,
                        no_cache_dir
                );
            }
        }
        return commands;
    }

    private boolean runShellCommands(String commands,
                                     String work_area,
                                     String targetPkg,
                                     String theme_name,
                                     String overlay_package,
                                     String variant,
                                     String additional_variant,
                                     int typeMode,
                                     boolean legacySwitch,
                                     Context context,
                                     String no_cache_dir) {
        Process nativeApp = null;
        try {
            String line;
            nativeApp = Runtime.getRuntime().exec(commands);

            try (OutputStream stdin = nativeApp.getOutputStream();
                 InputStream stderr = nativeApp.getErrorStream()) {
                stdin.write(("ls\n").getBytes());
                stdin.write("exit\n".getBytes());

                Boolean errored = false;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(stderr))) {
                    while ((line = br.readLine()) != null) {
                        if (line.contains("types not allowed") && !legacySwitch && !debug) {
                            Log.e(References.SUBSTRATUM_BUILDER,
                                    "This overlay was designed using a legacy theming " +
                                            "style, now falling back to legacy compiler...");
                            String new_commands = processAOPTCommands(work_area, targetPkg,
                                    theme_name, overlay_package, variant, additional_variant,
                                    typeMode, true, context, no_cache_dir);
                            return runShellCommands(
                                    new_commands, work_area, targetPkg, theme_name,
                                    overlay_package, variant, additional_variant, typeMode,
                                    true, context, no_cache_dir);
                        } else {
                            dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package, line);
                            errored = true;
                        }
                    }
                }
                if (errored) {
                    dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                            "Installation of \"" + overlay_package + "\" has failed.");
                } else {
                    // We need this Process to be waited for before moving on to the next function.
                    Log.d(References.SUBSTRATUM_BUILDER, "Overlay APK creation is running now...");
                    nativeApp.waitFor();
                    File unsignedAPK = new File(work_area + "/" + overlay_package + "." +
                            theme_name + "-unsigned.apk");
                    if (unsignedAPK.isFile()) {
                        Log.d(References.SUBSTRATUM_BUILDER, "Overlay APK creation has completed!");
                        return true;
                    } else {
                        dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                "Overlay APK creation has failed!");
                        has_errored_out = true;
                        dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                "Installation of \"" + overlay_package + "\" has failed.");
                    }
                }
            }
        } catch (IOException ioe) {
            Log.d(SUBSTRATUM_BUILDER, "An Android Oreo specific error message has been " +
                    "detected and has been whitelisted to continue moving forward " +
                    "with overlay compilation.");
            return !has_errored_out;
        } catch (Exception e) {
            e.printStackTrace();
            dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                    "Unfortunately, there was an exception trying to create a new APK");
            has_errored_out = true;
            dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                    "Installation of \"" + overlay_package + "\" has failed.");
        } finally {
            if (nativeApp != null) {
                nativeApp.destroy();
            }
        }
        return false;
    }

    private void dumpErrorLogs(String tag, String overlay, String message) {
        if (message.length() > 0) {
            Log.e(tag, message);
            if (error_logs.length() == 0) {
                error_logs = "» [" + overlay + "]: " + message;
            } else {
                error_logs += "\n" + "» [" + overlay + "]: " + message;
            }
        }
    }
}