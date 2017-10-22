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
        return this.error_logs;
    }

    /**
     * Substratum Builder Build Function
     * <p>
     * Prior to running this function, you must have copied all the files to the working directory!
     *
     * @param context            self explanatory
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
     * @param type4              String location of the type4 file
     * @param override_package   String package to tell whether we should change the package name
     * @param overlay_updater    Boolean flag to tell whether special_snowflake should be skipped
     */
    @SuppressWarnings({"UnusedReturnValue"})
    public boolean beginAction(final Context context,
                               final String overlay_package,
                               final String theme_name,
                               final String variant,
                               final String additional_variant,
                               final String base_variant,
                               final String versionName,
                               final Boolean theme_oms,
                               final String theme_parent,
                               final String no_cache_dir,
                               final String type1a,
                               final String type1b,
                               final String type1c,
                               final String type2,
                               final String type3,
                               final String type4,
                               final String override_package,
                               final Boolean overlay_updater) {

        // 1. Initialize the setup
        final File checkCompileFolder = new File(EXTERNAL_STORAGE_CACHE);
        if (!checkCompileFolder.exists() && !checkCompileFolder.mkdirs()) {
            Log.e(SUBSTRATUM_BUILDER, "Could not create compilation folder on external storage...");
        }
        this.has_errored_out = false;
        this.debug = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("theme_debug", false);

        // 2. Set work area to asset chosen based on the parameter passed into this class
        final String work_area = context.getCacheDir().getAbsolutePath() + SUBSTRATUM_BUILDER_CACHE;

        // 3. Create a modified Android Manifest for use with aopt

        // 4. Parse the theme's name before adding it into the new manifest to prevent any issues

        String parse2_variantName = "";
        if (variant != null) {
            final String parse1_variantName = variant.replaceAll("\\s+", "");
            parse2_variantName = parse1_variantName.replaceAll("[^a-zA-Z0-9]+", "");
        }
        if (!parse2_variantName.isEmpty()) parse2_variantName = "." + parse2_variantName;

        String parse2_baseName = "";
        if (base_variant != null) {
            final String parse1_baseName = base_variant.replaceAll("\\s+", "");
            parse2_baseName = parse1_baseName.replaceAll("[^a-zA-Z0-9]+", "");
        }
        if (!parse2_baseName.isEmpty()) parse2_baseName = "." + parse2_baseName;

        final String parse1_themeName = theme_name.replaceAll("\\s+", "");
        String parse2_themeName = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");
        if ("".equals(parse2_themeName)) {
            parse2_themeName = "no_name";
        }

        // 5. Create the manifest file based on the new parsed names
        String targetPackage = overlay_package;
        if (Resources.allowedSettingsOverlay(overlay_package)) {
            targetPackage = "com.android.settings";
        }
        if (Resources.allowedSystemUIOverlay(overlay_package)) {
            targetPackage = "com.android.systemui";
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int legacy_priority = prefs.getInt("legacy_overlay_priority", References.DEFAULT_PRIORITY);
        if (!Systems.checkOMS(context)) {
            final File work_area_array = new File(work_area);

            if (Arrays.asList(work_area_array.list()).contains("priority")) {
                Log.d(References.SUBSTRATUM_BUILDER,
                        "A specified priority file has been found for this overlay!");
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(
                                new File(work_area_array.getAbsolutePath() + "/priority")
                        )))) {
                    legacy_priority = Integer.parseInt(reader.readLine());
                } catch (final IOException e) {
                    this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                            "There was an error parsing priority file!");
                    legacy_priority =
                            prefs.getInt("legacy_overlay_priority", References.DEFAULT_PRIORITY);
                }
            }
            Log.d(References.SUBSTRATUM_BUILDER,
                    "The priority for this overlay is " + legacy_priority);
        }

        if (!this.has_errored_out) {
            final File root = new File(work_area + "/AndroidManifest.xml");
            try (FileWriter fw = new FileWriter(root);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter pw = new PrintWriter(bw)) {
                final Boolean created = root.createNewFile();
                if (!created)
                    if (variant != null) {
                        final String manifest =
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
                                        type4,
                                        ((override_package != null) &&
                                                !override_package.isEmpty()) ?
                                                override_package : "");
                        pw.write(manifest);
                    } else {
                        if (base_variant != null) {
                            final String manifest =
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
                                            type4,
                                            ((override_package != null) &&
                                                    !override_package.isEmpty()) ?
                                                    override_package : "");
                            pw.write(manifest);
                        } else {
                            final String manifest =
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
                                            type4,
                                            ((override_package != null) &&
                                                    !override_package.isEmpty()) ?
                                                    override_package : "");
                            pw.write(manifest);
                        }
                    }
            } catch (final Exception e) {
                this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package, e.getMessage());
                this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                        "There was an exception creating a new Manifest file!");
                this.has_errored_out = true;
                this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                        "Installation of \"" + overlay_package + "\" has failed.");
            }
        }

        // 6. Compile the new theme apk based on new manifest, framework-res.apk and extracted asset
        if (!this.has_errored_out) {
            final String targetPkg = Packages.getInstalledDirectory(context, targetPackage);
            final String commands = this.processAOPTCommands(
                    work_area,
                    targetPkg,
                    parse2_themeName,
                    overlay_package,
                    additional_variant,
                    type4,
                    false,
                    context,
                    no_cache_dir);

            this.has_errored_out = !this.runAOPTShellCommands(
                    commands,
                    work_area,
                    targetPkg,
                    parse2_themeName,
                    overlay_package,
                    additional_variant,
                    type4,
                    false,
                    context,
                    no_cache_dir);
        }

        // 7. Zipalign the apk
        if (!this.has_errored_out) {
            final String source = work_area + "/" + overlay_package + "." + parse2_themeName +
                    "-unsigned.apk";
            final String destination = work_area + "/" + overlay_package + "." + parse2_themeName +
                    "-unsigned-aligned.apk";
            final String commands = CompilerCommands.createZipAlignShellCommands(context, source,
                    destination);

            Process nativeApp = null;
            try {
                nativeApp = Runtime.getRuntime().exec(commands);

                // We need this Process to be waited for before moving on to the next function.
                Log.d(References.SUBSTRATUM_BUILDER, "Aligning APK now...");
                nativeApp.waitFor();
                final File alignedAPK = new File(destination);
                if (alignedAPK.isFile()) {
                    Log.d(References.SUBSTRATUM_BUILDER, "Zipalign successful!");
                } else {
                    this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                            "Zipalign has failed!");
                    this.has_errored_out = true;
                    this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                            "Zipalign of \"" + overlay_package + "\" has failed.");
                }
            } catch (final Exception e) {
                e.printStackTrace();
                this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                        "Unfortunately, there was an exception trying to zipalign a new APK");
                this.has_errored_out = true;
                this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                        "Installation of \"" + overlay_package + "\" has failed.");
            } finally {
                if (nativeApp != null) {
                    nativeApp.destroy();
                }
            }
        }

        // 8. Sign the apk
        final String overlayName = (variant == null) ?
                (overlay_package + "." + parse2_themeName) :
                (overlay_package + "." + parse2_themeName + parse2_variantName + parse2_baseName);
        if (!this.has_errored_out) {
            try {
                // Delete the previous APK if it exists in the dashboard folder
                FileOperations.delete(context,
                        EXTERNAL_STORAGE_CACHE + overlayName + "-signed.apk");

                // Sign with the built-in test key/certificate.
                final String source = work_area + "/" + overlay_package + "." + parse2_themeName +
                        "-unsigned-aligned.apk";

                final File key = new File(context.getDataDir() + "/key");
                final char[] keyPass = "overlay".toCharArray();

                if (!key.exists()) {
                    Log.d(SUBSTRATUM_BUILDER, "Loading keystore...");
                    FileOperations.copyFromAsset(context, "key", key.getAbsolutePath());
                }

                final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(new FileInputStream(key), keyPass);
                final PrivateKey privateKey = (PrivateKey) keyStore.getKey("key", keyPass);
                final List<X509Certificate> certs = new ArrayList<>();
                certs.add((X509Certificate) keyStore.getCertificateChain("key")[0]);

                final ApkSigner.SignerConfig signerConfig =
                        new ApkSigner.SignerConfig.Builder("overlay", privateKey, certs).build();
                final List<ApkSigner.SignerConfig> signerConfigs = new ArrayList<>();
                signerConfigs.add(signerConfig);
                final ApkSigner.Builder apkSigner = new ApkSigner.Builder(signerConfigs);
                final String destination = EXTERNAL_STORAGE_CACHE + overlayName + "-signed.apk";
                apkSigner
                        .setV1SigningEnabled(false)
                        .setV2SigningEnabled(true)
                        .setInputApk(new File(source))
                        .setOutputApk(new File(destination))
                        .setMinSdkVersion(Build.VERSION.SDK_INT)
                        .build()
                        .sign();

                Log.d(References.SUBSTRATUM_BUILDER, "APK successfully signed!");
            } catch (final Throwable t) {
                t.printStackTrace();
                this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                        "APK could not be signed. " + t.toString());
                this.has_errored_out = true;
                this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                        "Installation of \"" + overlay_package + "\" has failed.");
            }
        }

        // 9. Install the APK silently
        // Superuser needed as this requires elevated privileges to run these commands
        if (!this.has_errored_out) {
            if (theme_oms) {
                this.special_snowflake = false;
                if ("android".equals(overlay_package) ||
                        "projekt.substratum".equals(overlay_package)) {
                    this.special_snowflake = ThemeManager.isOverlayEnabled(context, overlayName) ||
                            (Systems.checkOreo() && !overlay_updater);
                }

                if (!this.special_snowflake) {
                    try {
                        ThemeManager.installOverlay(context, EXTERNAL_STORAGE_CACHE +
                                overlayName + "-signed.apk");
                        Log.d(References.SUBSTRATUM_BUILDER, "Silently installing APK...");
                    } catch (final Exception e) {
                        this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                "Overlay APK has failed to install! \" (Exception) " +
                                        "[Error: " + e.getMessage() + "]");
                        this.has_errored_out = true;
                        this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                "Installation of \"" + overlay_package + "\" has failed.");
                    }
                } else {
                    Log.d(References.SUBSTRATUM_BUILDER,
                            "Returning compiled APK path for later installation...");
                    this.no_install =
                            EXTERNAL_STORAGE_CACHE + overlayName + "-signed.apk";
                }
            } else {
                final Boolean isSamsung = Systems.isSamsung(context);
                if (isSamsung) {
                    // Take account for Samsung's package manager installation mode
                    Log.d(References.SUBSTRATUM_BUILDER,
                            "Requesting PackageManager to launch signed overlay APK for " +
                                    "Samsung environment...");
                    this.no_install = EXTERNAL_STORAGE_CACHE + overlayName + "-signed.apk";
                } else {
                    // At this point, it is detected to be legacy mode and Substratum will push to
                    // vendor/overlays directly.

                    FileOperations.mountRW();
                    // For Non-Nexus devices
                    if (!Resources.inNexusFilter()) {
                        final String vendor_location = LEGACY_NEXUS_DIR;
                        FileOperations.createNewFolder(vendor_location);
                        FileOperations.move(context, EXTERNAL_STORAGE_CACHE + overlayName +
                                "-signed.apk", vendor_location + overlayName + ".apk");
                        FileOperations.setPermissionsRecursively(644, vendor_location);
                        FileOperations.setPermissions(755, vendor_location);
                        FileOperations.setContext(vendor_location);
                    } else {
                        // For Nexus devices
                        FileOperations.mountRWVendor();
                        final String vendor_symlink = PIXEL_NEXUS_DIR;
                        FileOperations.createNewFolder(vendor_symlink);
                        final String vendor_partition = VENDOR_DIR;
                        FileOperations.createNewFolder(vendor_partition);
                        // On nexus devices, put framework overlay to /vendor/overlay/
                        if ("android".equals(overlay_package)) {
                            final String android_overlay = vendor_partition + overlayName + ".apk";
                            FileOperations.move(context, EXTERNAL_STORAGE_CACHE + overlayName +
                                    "-signed.apk", android_overlay);
                        } else {
                            final String overlay = vendor_symlink + overlayName + ".apk";
                            FileOperations.move(context, EXTERNAL_STORAGE_CACHE + overlayName +
                                    "-signed.apk", overlay);
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
        if (!BYPASS_SUBSTRATUM_BUILDER_DELETION) {
            final String workingDirectory =
                    context.getCacheDir().getAbsolutePath() + SUBSTRATUM_BUILDER_CACHE;
            final File deleted = new File(workingDirectory);
            FileOperations.delete(context, deleted.getAbsolutePath());
            if (!deleted.exists()) Log.d(References.SUBSTRATUM_BUILDER,
                    "Successfully cleared compilation cache!");
        }
        return !this.has_errored_out;
    }

    private String processAOPTCommands(final String work_area,
                                       final String targetPkg,
                                       final String theme_name,
                                       final String overlay_package,
                                       final CharSequence additional_variant,
                                       final CharSequence asset_replacement,
                                       final boolean legacySwitch,
                                       final Context context,
                                       final String no_cache_dir) {
        return CompilerCommands.createAOPTShellCommands(
                work_area,
                targetPkg,
                overlay_package,
                theme_name,
                legacySwitch,
                additional_variant,
                asset_replacement,
                context,
                no_cache_dir);
    }

    private boolean runAOPTShellCommands(final String commands,
                                         final String work_area,
                                         final String targetPkg,
                                         final String theme_name,
                                         final String overlay_package,
                                         final String additional_variant,
                                         final String asset_replacement,
                                         final boolean legacySwitch,
                                         final Context context,
                                         final String no_cache_dir) {
        Process nativeApp = null;
        try {
            nativeApp = Runtime.getRuntime().exec(commands);

            try (OutputStream stdin = nativeApp.getOutputStream();
                 InputStream stderr = nativeApp.getErrorStream()) {
                stdin.write(("ls\n").getBytes());
                stdin.write("exit\n".getBytes());

                Boolean errored = false;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(stderr))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.contains("types not allowed") && !legacySwitch && !this.debug) {
                            Log.e(References.SUBSTRATUM_BUILDER,
                                    "This overlay was designed using a legacy theming " +
                                            "style, now falling back to legacy compiler...");
                            final String new_commands = this.processAOPTCommands(work_area, targetPkg,
                                    theme_name, overlay_package, additional_variant,
                                    asset_replacement, true, context, no_cache_dir);
                            return this.runAOPTShellCommands(
                                    new_commands, work_area, targetPkg, theme_name,
                                    overlay_package, additional_variant, asset_replacement,
                                    true, context, no_cache_dir);
                        } else {
                            this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package, line);
                            errored = true;
                        }
                    }
                }
                if (errored) {
                    this.has_errored_out = true;
                    this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                            "Installation of \"" + overlay_package + "\" has failed.");
                } else {
                    // We need this Process to be waited for before moving on to the next function.
                    Log.d(References.SUBSTRATUM_BUILDER, "Overlay APK creation is running now...");
                    nativeApp.waitFor();
                    final File unsignedAPK = new File(work_area + "/" + overlay_package + "." +
                            theme_name + "-unsigned.apk");
                    if (unsignedAPK.isFile()) {
                        Log.d(References.SUBSTRATUM_BUILDER, "Overlay APK creation has completed!");
                        return true;
                    } else {
                        this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                "Overlay APK creation has failed!");
                        this.has_errored_out = true;
                        this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                "Installation of \"" + overlay_package + "\" has failed.");
                    }
                }
            }
        } catch (final IOException ioe) {
            if (Systems.checkOMS(context)) {
                Log.d(SUBSTRATUM_BUILDER, "An Android Oreo specific error message has been " +
                        "detected and has been whitelisted to continue moving forward " +
                        "with overlay compilation.");
                return !this.has_errored_out;
            } else {
                ioe.printStackTrace();
            }
        } catch (final Exception e) {
            e.printStackTrace();
            this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                    "Unfortunately, there was an exception trying to create a new APK");
            this.has_errored_out = true;
            this.dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                    "Installation of \"" + overlay_package + "\" has failed.");
        } finally {
            if (nativeApp != null) {
                nativeApp.destroy();
            }
        }
        return false;
    }

    private void dumpErrorLogs(final String tag, final String overlay, final String message) {
        if (!message.isEmpty()) {
            Log.e(tag, message);
            if (this.error_logs.isEmpty()) {
                this.error_logs = "» [" + overlay + "]: " + message;
            } else {
                this.error_logs += "\n" + "» [" + overlay + "]: " + message;
            }
        }
    }
}