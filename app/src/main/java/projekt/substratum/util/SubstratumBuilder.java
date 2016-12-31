package projekt.substratum.util;

import android.content.Context;
import android.os.Environment;
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
import projekt.substratum.config.References;

public class SubstratumBuilder {

    public Boolean has_errored_out = false;
    public String no_install = "";
    private String error_logs = "";

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

    private String processAOPTCommands(String work_area, String targetPkg,
                                       String theme_name, String overlay_package,
                                       String variant, String additional_variant,
                                       int typeMode, boolean legacySwitch) {
        String commands;
        if (typeMode == 1) {
            commands = CommandCompiler.createAOPTShellCommands(
                    work_area,
                    targetPkg,
                    overlay_package,
                    theme_name,
                    legacySwitch,
                    null);
        } else {
            if (variant != null) {
                commands = CommandCompiler.createAOPTShellCommands(
                        work_area,
                        targetPkg,
                        overlay_package,
                        theme_name,
                        legacySwitch,
                        additional_variant);
            } else {
                commands = CommandCompiler.createAOPTShellCommands(
                        work_area,
                        targetPkg,
                        overlay_package,
                        theme_name,
                        legacySwitch,
                        null);
            }
        }
        return commands;
    }

    private boolean runShellCommands(String commands, String work_area,
                                     String targetPkg, String theme_name,
                                     String overlay_package, String variant,
                                     String additional_variant, int typeMode,
                                     boolean legacySwitch) {
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
                        if (line.contains("types not allowed") && !legacySwitch) {
                            Log.e(References.SUBSTRATUM_BUILDER,
                                    "This overlay was designed using a legacy theming " +
                                            "style, now falling back to legacy compiler...");
                            String new_commands = processAOPTCommands(work_area, targetPkg,
                                    theme_name, overlay_package, variant, additional_variant,
                                    typeMode, true);
                            return runShellCommands(
                                    new_commands, work_area, targetPkg, theme_name,
                                    overlay_package, variant, additional_variant, typeMode,
                                    true);
                        } else {
                            dumpErrorLogs(
                                    References.SUBSTRATUM_BUILDER, overlay_package, line);
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
        } catch (Exception e) {
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

    public void beginAction(Context context, String theme_pid, String overlay_package, String
            theme_name, Boolean update_mode, String variant, String additional_variant,
                            String base_variant, String versionName, Boolean theme_oms, String
                                    theme_parent) {
        has_errored_out = false;

        // 1. Quickly filter out what kind of type this overlay will be compiled with
        int typeMode = 1;
        if (additional_variant != null) {
            typeMode = 2;
        }

        // 2. Set work area to asset chosen based on the parameter passed into this class

        String work_area = context.getCacheDir().getAbsolutePath() + "/SubstratumBuilder/" +
                theme_pid + "/assets/overlays/" + overlay_package;

        if (!theme_oms) {
            File check_legacy = new File(context.getCacheDir().getAbsolutePath() +
                    "/SubstratumBuilder/" + theme_pid + "/assets/overlays_legacy/" +
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
                                    "Could not read priority file" +
                                            " properly, falling back to default integer...");
                            legacy_priority = References.DEFAULT_PRIORITY;
                        }
                    }
                }
            } else {
                legacy_priority = References.DEFAULT_PRIORITY;
            }
            Log.d(References.SUBSTRATUM_BUILDER, "The priority for this overlay is " +
                    legacy_priority);
        }

        if (!has_errored_out) {
            try (FileWriter fw = new FileWriter(root);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter pw = new PrintWriter(bw)) {
                Boolean created = root.createNewFile();
                if (!created)
                    if (variant != null) {
                        String manifest =
                                CommandCompiler.createOverlayManifest(
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
                                    CommandCompiler.createOverlayManifest(
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
                                    CommandCompiler.createOverlayManifest(
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
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                        e.getMessage());
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                        "There was an exception creating a new Manifest file!");
                has_errored_out = true;
                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                        "Installation of \"" + overlay_package + "\" has " +
                                "failed.");
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
                    false);

            has_errored_out = !runShellCommands(
                    commands, work_area, targetPkg, parse2_themeName, overlay_package,
                    variant, additional_variant, typeMode, false);
        }

        // 7. Sign the apk

        if (!has_errored_out) {
            try {
                // Delete the previous APK if it exists in the dashboard folder
                References.delete(Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/" + overlay_package + "." + parse2_themeName +
                        "-signed.apk");

                // Sign with the built-in test key/certificate.
                String source = work_area + "/" + overlay_package + "." + parse2_themeName +
                        "-unsigned.apk";
                String destination = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/" + overlay_package + "." + parse2_themeName + "-signed.apk";

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
            if (update_mode) {
                if (theme_oms) {
                    try {
                        if (variant != null) {
                            References.installOverlay(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/.substratum/" +
                                    overlay_package + "." + parse2_themeName +
                                    "-signed.apk");
                            Log.d(References.SUBSTRATUM_BUILDER, "Silently installing APK...");
                            if (References.isPackageInstalled(context,
                                    overlay_package + "." + parse2_themeName +
                                            parse2_variantName + parse2_baseName)) {
                                Log.d(References.SUBSTRATUM_BUILDER,
                                        "Overlay APK has successfully been installed!");
                            } else {
                                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                        "Overlay APK has failed to install!");
                            }
                        } else {
                            References.installOverlay(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/" + overlay_package + "." +
                                    parse2_themeName + "-signed.apk");
                            Log.d(References.SUBSTRATUM_BUILDER, "Silently installing APK...");
                            if (References.isPackageInstalled(context,
                                    overlay_package + "." + parse2_themeName)) {
                                Log.d(References.SUBSTRATUM_BUILDER,
                                        "Overlay APK has successfully been installed!");
                            } else {
                                dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                        "Overlay APK has failed to install!");
                            }
                        }
                    } catch (Exception e) {
                        dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                "Overlay APK has failed to install! \"(Exception)");
                        has_errored_out = true;
                        dumpErrorLogs(References.SUBSTRATUM_BUILDER, overlay_package,
                                "Installation of \"" + overlay_package + "\" has failed.");
                    }
                } else {
                    // At this point, it is detected to be legacy mode and Substratum will push to
                    // vendor/overlays directly.

                    String vendor_location = "/system/vendor/overlay/";
                    String vendor_partition = "/vendor/overlay/";
                    String vendor_symlink = "/system/overlay/";
                    String current_vendor =
                            ((References.inNexusFilter()) ? vendor_partition :
                                    vendor_location);

                    References.mountRW();
                    File vendor = new File(current_vendor);
                    if (!vendor.exists()) {
                        if (current_vendor.equals(vendor_location)) {
                            References.createNewFolder(current_vendor);
                        } else {
                            References.mountRWVendor();
                            References.createNewFolder(vendor_symlink);
                            References.symlink(vendor_symlink, "/vendor");
                            References.setPermissions(755, vendor_partition);
                            References.mountROVendor();
                        }
                    }
                    if (current_vendor.equals(vendor_location)) {
                        References.move(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/.substratum/" + overlay_package +
                                "." + parse2_themeName + "-signed.apk", vendor_location +
                                overlay_package + "." + parse2_themeName + ".apk");
                        References.setPermissionsRecursively(644, vendor_location);
                        References.setPermissions(755, vendor_location);
                        References.setContext(vendor_location);
                    } else {
                        References.move(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/.substratum/" + overlay_package +
                                "." + parse2_themeName + "-signed.apk", vendor_symlink +
                                "/" + overlay_package + "." + parse2_themeName + ".apk");
                        References.setPermissionsRecursively(644, vendor_symlink);
                        References.setPermissions(755, vendor_symlink);
                        References.setContext(vendor_symlink);
                    }
                    References.mountRO();
                }
            } else {
                Log.d(References.SUBSTRATUM_BUILDER,
                        "Update mode flag disabled, returning one-line parsable command");
                no_install = "pm install -r " + Environment.getExternalStorageDirectory()
                        .getAbsolutePath() +
                        "/.substratum/" + overlay_package + "." + parse2_themeName +
                        "-signed.apk";
            }
        }
    }
}