package projekt.substratum.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import kellinwood.security.zipsigner.ZipSigner;
import projekt.substratum.config.References;

public class SubstratumIconBuilder {

    public Boolean has_errored_out = false;
    public String no_install = "";
    private String error_logs = "";

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

    private void createIconParameters(Context context, String theme_pack, int drawable,
                                      File icon_location, String drawable_name, String size,
                                      Bitmap icon_override) {
        int resolution;
        switch (size) {
            // Let's take account of the size of the icon to be created
            case "mdpi":
                resolution = 48;
                break;
            case "hdpi":
                resolution = 72;
                break;
            case "xhdpi":
                resolution = 96;
                break;
            case "xxhdpi":
                resolution = 144;
                break;
            case "xxxhdpi":
                resolution = 192;
                break;
            default:
                resolution = 0;
                break;
        }
        FileOutputStream out = null;
        try {
            String drawableName = drawable_name;
            if (icon_override == null) {
                if (drawableName.endsWith(".png")) {
                    drawableName = drawable_name.substring(0, drawable_name.length() - 4);
                }
                Context packContext = context.createPackageContext(theme_pack, 0);
                Resources resources = packContext.getResources();
                Bitmap b = BitmapFactory.decodeResource(resources, drawable);
                Bitmap bScaled = Bitmap.createScaledBitmap(b, resolution, resolution, true);
                out = new FileOutputStream(icon_location.getAbsolutePath() + "/" +
                        drawableName);
                bScaled.compress(Bitmap.CompressFormat.PNG, 100, out);
            } else {
                out = new FileOutputStream(icon_location.getAbsolutePath() + "/" +
                        drawableName);
                icon_override.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
        } catch (Exception e) {
            has_errored_out = true;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                has_errored_out = true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void beginAction(Context context, String theme_pack, String overlay_package,
                            String versionName, Boolean theme_oms, HashMap iconLocations,
                            String hashOne, int drawable, String drawable_name,
                            String parsedIconName, Boolean update_mode, Bitmap iconOverride) {
        Boolean icon_override;
        Bitmap icon_override_value;
        if (iconOverride != null) {
            icon_override = true;
            icon_override_value = iconOverride;
        } else {
            icon_override = false;
            icon_override_value = null;
        }

        has_errored_out = false;

        // 1. Set work area to asset chosen based on the parameter passed into this class

        String work_area = context.getCacheDir().getAbsolutePath() + "/IconStudio";
        File workArea = new File(work_area);
        if (new File(work_area).exists()) {
            // Every time this executes, ALWAYS reset the previous set up
            References.delete(work_area);
            boolean created = workArea.mkdir();
            if (!created) Log.e(References.SUBSTRATUM_ICON_BUILDER,
                    "Failed to create directory structure");
        } else {
            boolean created = workArea.mkdir();
            if (!created) Log.e(References.SUBSTRATUM_ICON_BUILDER,
                    "Failed to create directory structure");
        }

        // 2. Instantiate a modified Android Manifest for use with aopt

        File root = new File(work_area + "/AndroidManifest.xml");

        // 3. Now let's begin creating the directory structure, first the resources folder

        File res = new File(work_area + "/res");
        if (res.exists()) {
            References.delete(res.getAbsolutePath());
            boolean created = res.mkdir();
            if (!created) Log.e(References.SUBSTRATUM_ICON_BUILDER,
                    "Failed to create directory structure");
        } else {
            boolean created = res.mkdir();
            if (!created) Log.e(References.SUBSTRATUM_ICON_BUILDER,
                    "Failed to create directory structure");
        }

        // 4. Now let's take the objects from the HashMap and create the directory structure

        ArrayList<String> directories = (ArrayList) iconLocations.get(hashOne);
        for (int i = 0; i < directories.size(); i++) {
            if (directories.get(i).contains("drawable-mdpi")) {
                File icon_location = new File(res.getAbsolutePath() + "/drawable-mdpi");
                boolean created = icon_location.mkdir();
                if (created)
                    createIconParameters(
                            context, theme_pack, drawable, icon_location, drawable_name +
                                    ".png", "mdpi", (icon_override) ? icon_override_value : null);
            } else if (directories.get(i).contains("drawable-hdpi")) {
                File icon_location = new File(res.getAbsolutePath() + "/drawable-hdpi");
                boolean created = icon_location.mkdir();
                if (created)
                    createIconParameters(
                            context, theme_pack, drawable, icon_location, drawable_name +
                                    ".png", "hdpi", (icon_override) ? icon_override_value : null);
            } else if (directories.get(i).contains("drawable-xhdpi")) {
                File icon_location = new File(res.getAbsolutePath() + "/drawable-xhdpi");
                boolean created = icon_location.mkdir();
                if (created)
                    createIconParameters(
                            context, theme_pack, drawable, icon_location, drawable_name +
                                    ".png", "xhdpi", (icon_override) ? icon_override_value : null);
            } else if (directories.get(i).contains("drawable-xxhdpi")) {
                File icon_location = new File(res.getAbsolutePath() + "/drawable-xxhdpi");
                boolean created = icon_location.mkdir();
                if (created)
                    createIconParameters(
                            context, theme_pack, drawable, icon_location, drawable_name +
                                    ".png", "xxhdpi", (icon_override) ? icon_override_value : null);
            } else if (directories.get(i).contains("drawable-xxxhdpi")) {
                File icon_location = new File(res.getAbsolutePath() + "/drawable-xxxhdpi");
                boolean created = icon_location.mkdir();
                if (created)
                    createIconParameters(
                            context, theme_pack, drawable, icon_location, drawable_name +
                                    ".png", "xxxhdpi", (icon_override) ? icon_override_value :
                                    null);
            } else if (directories.get(i).contains("mipmap-mdpi")) {
                File icon_location = new File(res.getAbsolutePath() + "/mipmap-mdpi");
                boolean created = icon_location.mkdir();
                if (created)
                    createIconParameters(
                            context, theme_pack, drawable, icon_location, drawable_name +
                                    ".png", "mdpi", (icon_override) ? icon_override_value : null);
            } else if (directories.get(i).contains("mipmap-hdpi")) {
                File icon_location = new File(res.getAbsolutePath() + "/mipmap-hdpi");
                boolean created = icon_location.mkdir();
                if (created)
                    createIconParameters(
                            context, theme_pack, drawable, icon_location, drawable_name +
                                    ".png", "hdpi", (icon_override) ? icon_override_value : null);
            } else if (directories.get(i).contains("mipmap-xhdpi")) {
                File icon_location = new File(res.getAbsolutePath() + "/mipmap-xhdpi");
                boolean created = icon_location.mkdir();
                if (created)
                    createIconParameters(
                            context, theme_pack, drawable, icon_location, drawable_name +
                                    ".png", "xhdpi", (icon_override) ? icon_override_value : null);
            } else if (directories.get(i).contains("mipmap-xxhdpi")) {
                File icon_location = new File(res.getAbsolutePath() + "/mipmap-xxhdpi");
                boolean created = icon_location.mkdir();
                if (created)
                    createIconParameters(
                            context, theme_pack, drawable, icon_location, drawable_name +
                                    ".png", "xxhdpi", (icon_override) ? icon_override_value : null);
            } else if (directories.get(i).contains("mipmap-xxxhdpi")) {
                File icon_location = new File(res.getAbsolutePath() + "/mipmap-xxxhdpi");
                boolean created = icon_location.mkdir();
                if (created)
                    createIconParameters(
                            context, theme_pack, drawable, icon_location, drawable_name +
                                    ".png", "xxxhdpi", (icon_override) ? icon_override_value :
                                    null);
            }
        }

        // 5. Create the manifest file

        if (!has_errored_out) {
            try (FileWriter fw = new FileWriter(root);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter pw = new PrintWriter(bw)) {
                Boolean created = root.createNewFile();
                if (!created) {
                    String manifest = CommandCompiler.createIconOverlayManifest(
                            context,
                            overlay_package,
                            theme_pack,
                            versionName,
                            parsedIconName,
                            theme_oms,
                            80);
                    pw.write(manifest);
                }
            } catch (Exception e) {
                dumpErrorLogs(References.SUBSTRATUM_ICON_BUILDER, overlay_package, e.getMessage());
                dumpErrorLogs(References.SUBSTRATUM_ICON_BUILDER, overlay_package,
                        "There was an exception creating a new Manifest file!");
                has_errored_out = true;
                dumpErrorLogs(References.SUBSTRATUM_ICON_BUILDER, overlay_package,
                        "Installation of \"" + overlay_package + "\" has failed.");
            }
        }

        // 6. Compile the new theme apk based on new manifest, framework-res.apk and icons

        if (!has_errored_out) {
            Process nativeApp = null;
            try {
                String commands = "aopt p " +
                        "-M " + work_area + "/AndroidManifest.xml " +
                        "-S " + work_area + "/res/ " +
                        "-I " + "/system/framework/framework-res.apk " +
                        "-F " + work_area + "/" + overlay_package + ".icon-unsigned.apk " +
                        "-f --include-meta-data --auto-add-overlay" +
                        ((References.ENABLE_AOPT_OUTPUT) ? " -v" : "") +
                        "\n";

                String line;
                nativeApp = Runtime.getRuntime().exec(commands);

                try (OutputStream stdin = nativeApp.getOutputStream();
                     InputStream stderr = nativeApp.getErrorStream();
                     InputStream stdout = nativeApp.getInputStream()) {
                    stdin.write(("ls\n").getBytes());
                    stdin.write("exit\n".getBytes());

                    try (BufferedReader br = new BufferedReader(new InputStreamReader(stdout))) {
                        while ((line = br.readLine()) != null) {
                            Log.d("SubstratumCompiler", line);
                        }
                    }
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(stderr))) {
                        while ((line = br.readLine()) != null) {
                            dumpErrorLogs(References.SUBSTRATUM_ICON_BUILDER, overlay_package,
                                    line);
                            has_errored_out = true;
                        }
                    }
                    if (has_errored_out) {
                        dumpErrorLogs(References.SUBSTRATUM_ICON_BUILDER, overlay_package,
                                "Installation of \"" + overlay_package + "\" has failed.");
                    }
                }

                if (!has_errored_out) {
                    // We need this Process to be waited for before moving on to the next function.
                    Log.d(References.SUBSTRATUM_ICON_BUILDER, "Overlay APK creation is running " +
                            "now...");
                    nativeApp.waitFor();
                    File unsignedAPK = new File(work_area + "/" + overlay_package +
                            ".icon-unsigned.apk");
                    if (unsignedAPK.exists()) {
                        Log.d(References.SUBSTRATUM_ICON_BUILDER, "Overlay APK creation has " +
                                "completed!");
                    } else {
                        dumpErrorLogs(References.SUBSTRATUM_ICON_BUILDER, overlay_package,
                                "Overlay APK creation has failed!");
                        has_errored_out = true;
                        dumpErrorLogs(References.SUBSTRATUM_ICON_BUILDER, overlay_package,
                                "Installation of \"" + overlay_package + "\" has failed.");
                    }
                }
            } catch (Exception e) {
                dumpErrorLogs(References.SUBSTRATUM_ICON_BUILDER, overlay_package,
                        "Unfortunately, there was an exception trying to create a new APK");
                has_errored_out = true;
                dumpErrorLogs(References.SUBSTRATUM_ICON_BUILDER, overlay_package,
                        "Installation of \"" + overlay_package + "\" has failed.");
            } finally {
                if (nativeApp != null) {
                    nativeApp.destroy();
                }
            }
        }

        // 7. Sign the apk

        if (!has_errored_out) {
            try {
                // Delete the previous APK if it exists in the dashboard folder
                References.delete(Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/" + overlay_package + ".icon-signed.apk");

                // Sign with the built-in test key/certificate.
                String source = work_area + "/" + overlay_package + ".icon-unsigned.apk";
                String destination = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/" + overlay_package + ".icon-signed.apk";

                ZipSigner zipSigner = new ZipSigner();
                if (References.ENABLE_SIGNING) {
                    zipSigner.setKeymode("testkey");
                } else {
                    zipSigner.setKeymode("none");
                }
                zipSigner.signZip(source, destination);

                Log.d(References.SUBSTRATUM_ICON_BUILDER, "APK successfully signed!");
            } catch (Throwable t) {
                dumpErrorLogs(References.SUBSTRATUM_ICON_BUILDER, overlay_package,
                        "APK could not be signed. " + t.toString());
                has_errored_out = true;
                dumpErrorLogs(References.SUBSTRATUM_ICON_BUILDER, overlay_package,
                        "Installation of \"" + overlay_package + "\" has failed.");
            }
        }

        // Install the APK silently
        // Superuser needed as this requires elevated privileges to run these commands

        if (!has_errored_out) {
            if (update_mode) {
                if (theme_oms) {
                    try {
                        References.installOverlay(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/.substratum/" +
                                overlay_package + ".icon-signed.apk");
                        Log.d(References.SUBSTRATUM_ICON_BUILDER, "Silently installing APK...");
                        if (References.isPackageInstalled(context, overlay_package + ".icon")) {
                            Log.d(References.SUBSTRATUM_ICON_BUILDER, "Overlay APK has " +
                                    "successfully been " +
                                    "installed!");
                        } else {
                            dumpErrorLogs(References.SUBSTRATUM_ICON_BUILDER, overlay_package,
                                    "Overlay APK has failed to install!");
                        }
                    } catch (Exception e) {
                        dumpErrorLogs(References.SUBSTRATUM_ICON_BUILDER, overlay_package,
                                "Overlay APK has failed to install! \"(Exception)");
                        has_errored_out = true;
                        dumpErrorLogs(References.SUBSTRATUM_ICON_BUILDER, overlay_package,
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
                                ".icon-signed.apk", vendor_location +
                                overlay_package + ".icon.apk");
                        References.setPermissionsRecursively(644, vendor_location);
                        References.setPermissions(755, vendor_location);
                        References.setContext(vendor_location);
                    } else {
                        References.move(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/.substratum/" + overlay_package +
                                ".icon-signed.apk", vendor_symlink +
                                "/" + overlay_package + ".icon.apk");
                        References.setPermissionsRecursively(644, vendor_symlink);
                        References.setPermissions(755, vendor_symlink);
                        References.setContext(vendor_symlink);
                    }
                    References.mountRO();
                }
            } else {
                Log.d(References.SUBSTRATUM_ICON_BUILDER,
                        "Update mode flag disabled, returning one-line parsable command");
                no_install = "pm install -r " + Environment.getExternalStorageDirectory()
                        .getAbsolutePath() +
                        "/.substratum/" + overlay_package + ".icon-signed.apk";
            }
        }
    }
}