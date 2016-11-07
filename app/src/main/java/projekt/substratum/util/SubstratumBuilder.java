package projekt.substratum.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
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

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class SubstratumBuilder {

    /*

    All public methods in this class:

    1. initializeCache(Context, package_identifier) : extract assets for theme so no reuse needed
    2. beginAction(Context, package_name, theme_package) : start SubstratumBuilder function
        - this will create an AndroidManifest based on selected package
        - then it will compile using the new work zone

    */

    private static final Boolean enable_signing = true;
    public Boolean has_errored_out = false;
    public String parse2_themeName;
    public String no_install = "";
    private Context mContext;
    private String error_logs = "";

    public String getErrorLogs() {
        return error_logs;
    }

    private String getDeviceIMEI() {
        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    private String getDeviceID() {
        return Settings.Secure.getString(
                mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
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

    public void beginAction(Context context, String theme_pid, String overlay_package, String
            theme_name, String update_mode_input, String variant, String additional_variant,
                            String base_variant, String versionName, Boolean theme_oms) {

        has_errored_out = false;
        mContext = context;
        String work_area;
        Boolean update_mode = Boolean.valueOf(update_mode_input);
        String base_resources = base_variant;

        int typeMode = 1;
        if (additional_variant != null) {
            typeMode = 2;
        }

        // 1. Set work area to asset chosen based on the parameter passed into this class

        work_area = mContext.getCacheDir().getAbsolutePath() + "/SubstratumBuilder/" + theme_pid +
                "/assets/overlays/" + overlay_package;

        if (!theme_oms) {
            File check_legacy = new File(mContext.getCacheDir().getAbsolutePath() +
                    "/SubstratumBuilder/" + theme_pid + "/assets/overlays_legacy/" +
                    overlay_package);
            if (check_legacy.exists()) {
                work_area = check_legacy.getAbsolutePath();
            }
        }

        // 2. Create a modified Android Manifest for use with aopt

        File root = new File(work_area + "/AndroidManifest.xml");

        // 2a. Parse the theme's name before adding it into the new manifest to prevent any issues

        String parse1_themeName = theme_name.replaceAll("\\s+", "");
        parse2_themeName = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");

        String parse2_variantName = "";
        if (variant != null) {
            String parse1_variantName = variant.replaceAll("\\s+", "");
            parse2_variantName = parse1_variantName.replaceAll("[^a-zA-Z0-9]+", "");
        }
        if (parse2_variantName.length() > 0) parse2_variantName = "." + parse2_variantName;

        String parse2_baseName = "";
        if (base_resources != null) {
            String parse1_baseName = base_resources.replaceAll("\\s+", "");
            parse2_baseName = parse1_baseName.replaceAll("[^a-zA-Z0-9]+", "");
        }
        if (parse2_baseName.length() > 0) parse2_baseName = "." + parse2_baseName;

        if (parse2_themeName.equals("")) {
            parse2_themeName = "no_name";
        }

        // 2b. Create the manifest file based on the new parsed names

        String varianter = parse2_variantName + parse2_baseName;
        varianter.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");

        String targetPackage = overlay_package;
        if (References.allowedSettingsOverlay(overlay_package)) {
            targetPackage = "com.android.settings";
        }
        if (References.allowedSystemUIOverlay(overlay_package)) {
            targetPackage = "com.android.systemui";
        }

        int legacy_priority = References.DEFAULT_PRIORITY;
        if (!References.checkOMS(mContext)) {
            File work_area_array = new File(work_area);

            if (Arrays.asList(work_area_array.list()).contains("priority")) {
                Log.d("SubstratumBuilder",
                        "A specified priority file has been found for this overlay!");
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(
                            new InputStreamReader(new FileInputStream(
                                    new File(work_area_array.getAbsolutePath() + "/priority"))));
                    legacy_priority = Integer.parseInt(reader.readLine());
                } catch (IOException e) {
                    dumpErrorLogs("SubstratumBuilder", overlay_package,
                            "There was an error parsing priority file!");
                    legacy_priority = References.DEFAULT_PRIORITY;
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            dumpErrorLogs("SubstratumBuilder", overlay_package,
                                    "Could not read priority file" +
                                            " properly, falling back to default integer...");
                            legacy_priority = References.DEFAULT_PRIORITY;
                        }
                    }
                }
            } else {
                legacy_priority = References.DEFAULT_PRIORITY;
            }
            Log.d("SubstratumBuilder", "The priority for this overlay is " + legacy_priority);
        }

        if (!has_errored_out) {
            try (FileWriter fw = new FileWriter(root);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter pw = new PrintWriter(bw)) {
                root.createNewFile();
                if (variant != null) {
                    String manifest =
                            "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +
                                    "<manifest xmlns:android=\"http://schemas.android" +
                                    ".com/apk/res/android\" package=\"" + overlay_package + "." +
                                    parse2_themeName + parse2_variantName + parse2_baseName +
                                    "\"\n" +
                                    "        android:versionName=\"" + versionName + "\"> \n" +
                                    "    <overlay " + ((!theme_oms) ? "android:priority=\"" +
                                    legacy_priority + "\" " : "") +
                                    "android:targetPackage=\"" + targetPackage + "\"/>\n" +
                                    "    <application android:label=\"" + overlay_package + "." +
                                    parse2_themeName + parse2_variantName + parse2_baseName +
                                    "\">\n" +
                                    "        <meta-data android:name=\"Substratum_ID\" " +
                                    "android:value=\"" + getDeviceID() + "\"/>\n" +
                                    "        <meta-data android:name=\"Substratum_IMEI\" " +
                                    "android:value=\"!" + getDeviceIMEI() + "\"/>\n" +
                                    "        <meta-data android:name=\"Substratum_Parent\" " +
                                    "android:value=\"" + parse2_themeName + "\"/>\n" +
                                    "        <meta-data android:name=\"Substratum_Variant\" " +
                                    "android:value=\"" + varianter +
                                    "\"/>\n" +
                                    "    </application>\n" +
                                    "</manifest>\n";
                    pw.write(manifest);
                } else {
                    if (base_resources != null) {
                        String manifest =
                                "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +
                                        "<manifest xmlns:android=\"http://schemas.android" +
                                        ".com/apk/res/android\" package=\"" + overlay_package + "" +
                                        "." +
                                        parse2_themeName + parse2_variantName + parse2_baseName +
                                        "\"\n" +
                                        "        android:versionName=\"" + versionName + "\"> \n" +
                                        "    <overlay " + ((!theme_oms) ?
                                        "android:priority=\"" +
                                                legacy_priority + "\" " : "") +
                                        "android:targetPackage=\"" + targetPackage +
                                        "\"/>\n" +
                                        "    <application android:label=\"" + overlay_package + "" +
                                        "." +
                                        parse2_themeName + parse2_variantName + parse2_baseName +
                                        "\">\n" +
                                        "        <meta-data android:name=\"Substratum_ID\" " +
                                        "android:value=\"" + getDeviceID() + "\"/>\n" +
                                        "        <meta-data android:name=\"Substratum_IMEI\" " +
                                        "android:value=\"!" + getDeviceIMEI() + "\"/>\n" +
                                        "        <meta-data android:name=\"Substratum_Parent\" " +
                                        "android:value=\"" + parse2_themeName + "\"/>\n" +
                                        "        <meta-data android:name=\"Substratum_Variant\" " +
                                        "android:value=\"" + varianter
                                        + "\"/>\n" +
                                        "    </application>\n" +
                                        "</manifest>\n";
                        pw.write(manifest);
                    } else {
                        String manifest =
                                "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +
                                        "<manifest xmlns:android=\"http://schemas.android" +
                                        ".com/apk/res/android\" package=\"" + overlay_package + "" +
                                        "." +
                                        parse2_themeName + "\"\n" +
                                        "        android:versionName=\"" + versionName + "\"> \n" +
                                        "    <overlay " +
                                        ((!theme_oms) ?
                                                "android:priority=\"" +
                                                        legacy_priority + "\" " : "") +
                                        " android:targetPackage=\"" + targetPackage + "\"/>\n" +
                                        "    <application android:label=\"" + overlay_package + "" +
                                        "." +
                                        parse2_themeName + "\">\n" +
                                        "        <meta-data android:name=\"Substratum_ID\" " +
                                        "android:value=\"" + getDeviceID() + "\"/>\n" +
                                        "        <meta-data android:name=\"Substratum_IMEI\" " +
                                        "android:value=\"!" + getDeviceIMEI() + "\"/>\n" +
                                        "        <meta-data android:name=\"Substratum_Parent\" " +
                                        "android:value=\"" + parse2_themeName + "\"/>\n" +
                                        "    </application>\n" +
                                        "</manifest>\n";
                        pw.write(manifest);
                    }
                }
            } catch (Exception e) {
                dumpErrorLogs("SubstratumBuilder", overlay_package,
                        e.getMessage());
                dumpErrorLogs("SubstratumBuilder", overlay_package,
                        "There was an exception creating a new Manifest file!");
                has_errored_out = true;
                dumpErrorLogs("SubstratumBuilder", overlay_package,
                        "Installation of \"" + overlay_package + "\" has " +
                                "failed.");
            }
        }

        // Compile the new theme apk based on new manifest, framework-res.apk and extracted asset

        if (!has_errored_out) {
            Process nativeApp = null;
            try {
                String commands;
                if (typeMode == 1) {
                    commands = "aopt p " +
                            "-M " + work_area + "/AndroidManifest.xml " +
                            "-S " + work_area + "/workdir/ " +
                            "-I " + "/system/framework/framework-res.apk " +
                            "-F " + work_area + "/" + overlay_package +
                            "." + parse2_themeName + "-unsigned.apk " +
                            "-f --include-meta-data --auto-add-overlay\n";
                } else {
                    if (variant != null) {
                        commands = "aopt p " +
                                "-M " + work_area + "/AndroidManifest.xml " +
                                "-S " + work_area + "/" + "type2_" + additional_variant + "/ " +
                                "-S " + work_area + "/workdir/ " +
                                "-I " + "/system/framework/framework-res.apk " +
                                "-F " + work_area + "/" + overlay_package + "." +
                                parse2_themeName + "-unsigned.apk " +
                                "-f --include-meta-data --auto-add-overlay\n";
                    } else {
                        commands = "aopt p " +
                                "-M " + work_area + "/AndroidManifest.xml " +
                                "-S " + work_area + "/workdir/ " +
                                "-I " + "/system/framework/framework-res.apk " +
                                "-F " + work_area + "/" + overlay_package +
                                "." + parse2_themeName + "-unsigned.apk " +
                                "-f --include-meta-data --auto-add-overlay\n";
                    }
                }

                String line;
                nativeApp = Runtime.getRuntime().exec(commands);

                try (OutputStream stdin = nativeApp.getOutputStream();
                     InputStream stderr = nativeApp.getErrorStream();
                     InputStream stdout = nativeApp.getInputStream()) {
                    stdin.write(("ls\n").getBytes());
                    stdin.write("exit\n".getBytes());

                    try (BufferedReader br = new BufferedReader(new InputStreamReader(stdout))) {
                        while ((line = br.readLine()) != null) {
                            Log.d("OverlayOptimizer", line);
                        }
                    }
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(stderr))) {
                        while ((line = br.readLine()) != null) {
                            dumpErrorLogs("SubstratumBuilder", overlay_package, line);
                            has_errored_out = true;
                        }
                    }
                    if (has_errored_out) {
                        dumpErrorLogs("SubstratumBuilder", overlay_package,
                                "Installation of \"" + overlay_package + "\" has failed.");
                    }
                }

                if (!has_errored_out) {
                    // We need this Process to be waited for before moving on to the next function.
                    Log.d("SubstratumBuilder", "Overlay APK creation is running now...");
                    nativeApp.waitFor();
                    File unsignedAPK = new File(work_area + "/" + overlay_package + "." +
                            parse2_themeName + "-unsigned.apk");
                    if (unsignedAPK.exists()) {
                        Log.d("SubstratumBuilder", "Overlay APK creation has completed!");
                    } else {
                        dumpErrorLogs("SubstratumBuilder", overlay_package,
                                "Overlay APK creation has failed!");
                        has_errored_out = true;
                        dumpErrorLogs("SubstratumBuilder", overlay_package,
                                "Installation of \"" + overlay_package + "\" has failed.");
                    }
                }
            } catch (Exception e) {
                dumpErrorLogs("SubstratumBuilder", overlay_package,
                        "Unfortunately, there was an exception trying to create a new APK");
                has_errored_out = true;
                dumpErrorLogs("SubstratumBuilder", overlay_package,
                        "Installation of \"" + overlay_package + "\" has failed.");
            } finally {
                if (nativeApp != null) {
                    nativeApp.destroy();
                }
            }
        }

        // Sign the apk

        if (!has_errored_out) {
            try {
                // Delete the previous APK if it exists in the dashboard folder
                References.delete(Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/" + overlay_package + "." + parse2_themeName +
                        "-unsigned.apk");

                // Sign with the built-in test key/certificate.
                String source = work_area + "/" + overlay_package + "." + parse2_themeName +
                        "-unsigned.apk";
                String destination = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/" + overlay_package + "." + parse2_themeName + "-signed.apk";

                ZipSigner zipSigner = new ZipSigner();
                if (enable_signing) {
                    zipSigner.setKeymode("testkey");
                } else {
                    zipSigner.setKeymode("none");
                }
                zipSigner.signZip(source, destination);

                Log.d("SubstratumBuilder", "APK successfully signed!");
            } catch (Throwable t) {
                dumpErrorLogs("SubstratumBuilder", overlay_package,
                        "APK could not be signed. " + t.toString());
                has_errored_out = true;
                dumpErrorLogs("SubstratumBuilder", overlay_package,
                        "Installation of \"" + overlay_package + "\" has failed.");
            }
        }

        // Install the APK silently
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
                            Log.d("SubstratumBuilder", "Silently installing APK...");
                            if (checkIfPackageInstalled(overlay_package + "." + parse2_themeName +
                                    parse2_variantName + parse2_baseName, context)) {
                                Log.d("SubstratumBuilder", "Overlay APK has successfully been " +
                                        "installed!");
                            } else {
                                dumpErrorLogs("SubstratumBuilder", overlay_package,
                                        "Overlay APK has failed to install!");
                            }
                        } else {
                            References.installOverlay(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/" + overlay_package + "." +
                                    parse2_themeName + "-signed.apk");
                            Log.d("SubstratumBuilder", "Silently installing APK...");
                            if (checkIfPackageInstalled(overlay_package + "." + parse2_themeName,
                                    context)) {
                                Log.d("SubstratumBuilder", "Overlay APK has successfully been " +
                                        "installed!");
                            } else {
                                dumpErrorLogs("SubstratumBuilder", overlay_package,
                                        "Overlay APK has failed to install!");
                            }
                        }
                    } catch (Exception e) {
                        dumpErrorLogs("SubstratumBuilder", overlay_package,
                                "Overlay APK has failed to install! \"(Exception)");
                        has_errored_out = true;
                        dumpErrorLogs("SubstratumBuilder", overlay_package,
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
                Log.d("SubstratumBuilder", "Update mode flag disabled, returning one-line " +
                        "parsable command");
                no_install = "pm install -r " + Environment.getExternalStorageDirectory()
                        .getAbsolutePath() +
                        "/.substratum/" + overlay_package + "." + parse2_themeName +
                        "-signed.apk";
            }
        }
    }

    @NonNull
    private Boolean checkIfPackageInstalled(String packagename, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
