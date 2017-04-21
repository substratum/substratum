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
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

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
import java.util.Arrays;

import kellinwood.security.zipsigner.ZipSigner;
import projekt.substratum.common.References;
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
        return error_logs;
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
                    no_cache_dir);
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
                        no_cache_dir);
            } else {
                commands = CompilerCommands.createAOPTShellCommands(
                        work_area,
                        targetPkg,
                        overlay_package,
                        theme_name,
                        legacySwitch,
                        null,
                        context,
                        no_cache_dir);
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
                    if (unsignedAPK.exists()) {
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

    @SuppressWarnings("ConstantConditions")
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
                               String no_cache_dir) {
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

        int legacy_priority = References.DEFAULT_PRIORITY;
        if (!References.checkOMS(context)) {
            File work_area_array = new File(work_area);

            if (Arrays.asList(work_area_array.list()).contains("priority")) {
                Log.d(References.SUBSTRATUM_BUILDER,
                        "A specified priority file has been found for this overlay!");
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(
                            new InputStreamReader(new FileInputStream(
                                    new File(work_area_array.getAbsolutePath() + "/priority"))));
                    legacy_priority = Integer.parseInt(reader.readLine());
                } catch (IOException e) {
                    dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                            "There was an error parsing priority file!");
                    legacy_priority = References.DEFAULT_PRIORITY;
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                    "Could not read priority file " +
                                            "properly, falling back to default integer...");
                            legacy_priority = References.DEFAULT_PRIORITY;
                        }
                    }
                }
            } else {
                legacy_priority = References.DEFAULT_PRIORITY;
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
                                        varianter,
                                        theme_oms,
                                        legacy_priority,
                                        false);
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
                                            varianter,
                                            theme_oms,
                                            legacy_priority,
                                            false);
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
                                            varianter,
                                            theme_oms,
                                            legacy_priority,
                                            true);
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
                FileOperations.delete(context, Environment.getExternalStorageDirectory()
                        .getAbsolutePath() +
                        EXTERNAL_STORAGE_CACHE + overlay_package + "." + parse2_themeName +
                        "-signed.apk");

                // Sign with the built-in test key/certificate.
                String source = work_area + "/" + overlay_package + "." + parse2_themeName +
                        "-unsigned.apk";
                String destination = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        EXTERNAL_STORAGE_CACHE + overlay_package + "." + parse2_themeName +
                        "-signed.apk";

                ZipSigner zipSigner = new ZipSigner();
                if (References.ENABLE_SIGNING) {
                    zipSigner.setKeymode("testkey");
                } else {
                    zipSigner.setKeymode("none");
                }
                zipSigner.signZip(source, destination);

                Log.d(References.SUBSTRATUM_BUILDER, "APK successfully signed!");
            } catch (Throwable t) {
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
                    special_snowflake = ThemeManager.isOverlayEnabled(overlayName);
                }

                if (!special_snowflake) {
                    try {
                        ThemeManager.installOverlay(context, Environment
                                .getExternalStorageDirectory()
                                .getAbsolutePath() + EXTERNAL_STORAGE_CACHE +
                                overlay_package + "." + parse2_themeName + "-signed.apk");
                        Log.d(References.SUBSTRATUM_BUILDER, "Silently installing APK...");
                    } catch (Exception e) {
                        dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                "Overlay APK has failed to install! \"(Exception)");
                        has_errored_out = true;
                        dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                "Installation of \"" + overlay_package + "\" has failed.");
                    }
                } else {
                    Log.d(References.SUBSTRATUM_BUILDER,
                            "Returning compiled APK path for later installation...");

                    if (variant != null) {
                        no_install = Environment
                                .getExternalStorageDirectory()
                                .getAbsolutePath() + EXTERNAL_STORAGE_CACHE +
                                overlay_package + "." + parse2_themeName +
                                "-signed.apk";
                    } else {
                        no_install = Environment
                                .getExternalStorageDirectory()
                                .getAbsolutePath() +
                                EXTERNAL_STORAGE_CACHE + overlay_package + "." +
                                parse2_themeName + "-signed.apk";
                    }
                }
            } else {
                // At this point, it is detected to be legacy mode and Substratum will push to
                // vendor/overlays directly.
                String vendor_location = LEGACY_NEXUS_DIR;
                String vendor_partition = VENDOR_DIR;
                String vendor_symlink = PIXEL_NEXUS_DIR;
                String current_vendor =
                        ((References.inNexusFilter()) ? vendor_partition : vendor_location);

                FileOperations.mountRW();
                File vendor = new File(current_vendor);
                if (!vendor.exists()) {
                    if (current_vendor.equals(vendor_location)) {
                        FileOperations.createNewFolder(current_vendor);
                    } else {
                        FileOperations.mountRWVendor();
                        FileOperations.createNewFolder(vendor_symlink);
                        FileOperations.createNewFolder(vendor_partition);
                        FileOperations.mountROVendor();
                    }
                }
                if (current_vendor.equals(vendor_location)) {
                    FileOperations.move(context, Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + EXTERNAL_STORAGE_CACHE + overlay_package +
                            "." + parse2_themeName + "-signed.apk", vendor_location +
                            overlay_package + "." + parse2_themeName + ".apk");
                    FileOperations.setPermissionsRecursively(644, vendor_location);
                    FileOperations.setPermissions(755, vendor_location);
                    FileOperations.setContext(vendor_location);
                } else {
                    FileOperations.mountRWVendor();
                    // On nexus devices, put framework overlay to /vendor/overlay/
                    if (overlay_package.equals("android")) {
                        String android_overlay = vendor_partition + "/" + overlay_package + "."
                                + parse2_themeName + ".apk";
                        FileOperations.move(context, Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + EXTERNAL_STORAGE_CACHE + overlay_package +
                                "." + parse2_themeName + "-signed.apk", android_overlay);
                    } else {
                        String overlay = vendor_symlink + "/" + overlay_package + "." +
                                parse2_themeName + ".apk";
                        FileOperations.move(context, Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + EXTERNAL_STORAGE_CACHE + overlay_package +
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
}