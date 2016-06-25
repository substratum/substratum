package projekt.substratum.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import kellinwood.security.zipsigner.ZipSigner;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class SubstratumBuilder {

    /*

    All public methods in this class:

    1. injectAAPT(Context) : initial check/injection for AAPT access on device
    2. initializeCache(Context, package_identifier) : extract assets for theme so no reuse needed
    3. beginAction(Context, package_name, theme_package) : start SubstratumBuilder function
        - this will create an AndroidManifest based on selected package
        - then it will compile using the new work zone

     */

    public Boolean has_errored_out = false;
    public String parse2_themeName;
    public String no_install = "";
    private Context mContext;
    private Boolean enable_signing = true;

    public void injectAAPT(Context context) {
        mContext = context;

        // aaptChecker: Check if aapt is installed on the device

        File aapt = new File("/system/bin/aapt");
        if (!aapt.exists()) {
            if (!Build.SUPPORTED_ABIS.toString().contains("86")) {
                // Take account for ARM/ARM64 devices
                copyAAPT("aapt");
                eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /system");
                eu.chainfire.libsuperuser.Shell.SU.run(
                        "cp " + context.getFilesDir().getAbsolutePath() +
                                "/aapt " +
                                "/system/bin/aapt");
                eu.chainfire.libsuperuser.Shell.SU.run("chmod 755 /system/bin/aapt");
                eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,ro /system");
                Log.d("SubstratumBuilder", "Android Assets Packaging Tool (ARM) has been injected" +
                        " into the " +
                        "system partition.");
            } else {
                // Take account for x86 devices
                copyAAPT("aapt-x86");
                eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /system");
                eu.chainfire.libsuperuser.Shell.SU.run(
                        "cp " + context.getFilesDir().getAbsolutePath() +
                                "/aapt-x86 " +
                                "/system/bin/aapt");
                eu.chainfire.libsuperuser.Shell.SU.run("chmod 755 /system/bin/aapt");
                eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,ro /system");
                Log.d("SubstratumBuilder", "Android Assets Packaging Tool (x86) has been injected" +
                        " into the " +
                        "system partition.");
            }
        } else {
            Log.d("SubstratumBuilder", "The system partition already contains an existing AAPT " +
                    "binary and Substratum is locked and loaded!");
        }
    }

    public void initializeCache(Context context, String package_identifier) {
        mContext = context;
        try {
            unzip(package_identifier);
            Log.d("SubstratumBuilder", "The theme's assets have been successfully expanded to the" +
                    " work area!");
        } catch (IOException ioe) {
        }
    }

    private int checkCurrentThemeSelectionLocation(String packageName) {
        try {
            mContext.getPackageManager().getApplicationInfo(packageName, 0);
            File directory1 = new File("/data/app/" + packageName + "-1/base.apk");
            if (directory1.exists()) {
                return 1;
            } else {
                File directory2 = new File("/data/app/" + packageName + "-2/base.apk");
                if (directory2.exists()) {
                    return 2;
                } else {
                    return 0;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private String getDeviceIMEI() {
        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context
                .TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    private String getDeviceID() {
        return Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    private void unzip(String package_identifier) throws IOException {
        // First, extract the APK as a zip so we don't have to access the APK multiple times

        int folder_abbreviation = checkCurrentThemeSelectionLocation(package_identifier);
        if (folder_abbreviation != 0) {
            String source = "/data/app/" + package_identifier + "-" +
                    folder_abbreviation + "/base.apk";
            File myDir = new File(mContext.getCacheDir(), "SubstratumBuilder");
            if (!myDir.exists()) {
                myDir.mkdir();
            }
            String destination = mContext.getCacheDir().getAbsolutePath() + "/SubstratumBuilder";

            ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(source)));
            try {
                ZipEntry zipEntry;
                int count;
                byte[] buffer = new byte[8192];
                while ((zipEntry = inputStream.getNextEntry()) != null) {
                    File file = new File(destination, zipEntry.getName());
                    File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException("Failed to ensure directory: " +
                                dir.getAbsolutePath());
                    if (zipEntry.isDirectory())
                        continue;
                    FileOutputStream outputStream = new FileOutputStream(file);
                    try {
                        while ((count = inputStream.read(buffer)) != -1)
                            outputStream.write(buffer, 0, count);
                    } finally {
                        outputStream.close();
                    }
                }
            } finally {
                inputStream.close();
            }
        } else {
            Log.e("SubstratumLogger",
                    "There is no valid package name under this abbreviated folder " +
                            "count.");
        }

        // Second, clean out the cache folder for files that aren't needed
        // Superuser is used due to some files being held hostage by the system

        File[] fileList = new File(mContext.getCacheDir().getAbsolutePath() +
                "/SubstratumBuilder/").listFiles();
        for (int i = 0; i < fileList.length; i++) {
            if (!fileList[i].getName().equals("assets")) {
                eu.chainfire.libsuperuser.Shell.SU.run(
                        "rm -r " + mContext.getCacheDir().getAbsolutePath() +
                                "/SubstratumBuilder/" + fileList[i].getName());
            }
        }
    }

    public void beginAction(Context context, String overlay_package, String theme_name, String
            update_mode_input, String variant, String additional_variant, String base_variant,
                            String versionName) {

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

        work_area = mContext.getCacheDir().getAbsolutePath() + "/SubstratumBuilder/assets/overlays/"
                + overlay_package;

        // 2. Create a modified Android Manifest for use with aapt

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
        Log.d("PackageProcessor", "Processing package \"" + overlay_package + "." +
                parse2_themeName +
                ((variant != null || additional_variant != null) ? parse2_variantName : "") +
                ((base_resources != null) ? parse2_variantName : "") + "\"");

        // 2b. Create the manifest file based on the new parsed names

        if (!has_errored_out) {
            try {
                root.createNewFile();
                FileWriter fw = new FileWriter(root);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter pw = new PrintWriter(bw);
                if (variant != null) {
                    String manifest =
                            "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +
                                    "<manifest xmlns:android=\"http://schemas.android" +
                                    ".com/apk/res/android\" package=\"" + overlay_package + "." +
                                    parse2_themeName + parse2_variantName + parse2_baseName +
                                    "\"\n" +
                                    "        android:versionName=\"" + versionName + "\"> \n" +
                                    "    <overlay android:targetPackage=\"" + overlay_package +
                                    "\"/>\n" +
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
                                    "android:value=\"" + parse2_variantName + parse2_baseName +
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
                                        "    <overlay android:targetPackage=\"" + overlay_package +
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
                                        "android:value=\"" + parse2_variantName + parse2_baseName
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
                                        "    <overlay android:targetPackage=\"" + overlay_package +
                                        "\"/>\n" +
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
                pw.close();
                bw.close();
                fw.close();
            } catch (Exception e) {
                Log.e("SubstratumBuilder", "There was an exception creating a new Manifest file!");
                has_errored_out = true;
                Log.e("SubstratumBuilder", "Installation of \"" + overlay_package + "\" has " +
                        "failed.");
            }
        }

        // Compile the new theme apk based on new manifest, framework-res.apk and extracted asset

        if (!has_errored_out) {
            try {
                File type3directory = new File(work_area + "/type3_" + base_resources + "/");
                String commands;
                if (typeMode == 1) {
                    commands = "aapt p -M " + work_area +
                            "/AndroidManifest.xml -S " +
                            work_area +
                            (((base_resources == null) || !type3directory.exists()) ? "/res/ -I " :
                                    "/" + "type3_" +
                                            base_resources + "/ -I ") +
                            "/system/framework/framework-res.apk -F " +
                            work_area +
                            "/" + overlay_package + "." + parse2_themeName + "-unsigned.apk " +
                            "-f --include-meta-data\n";
                } else {
                    if (variant != null) {
                        commands = "aapt p -M " + work_area +
                                "/AndroidManifest.xml -S " +
                                work_area +
                                "/" + "type2_" + additional_variant + "/ -S " +
                                work_area +
                                (((base_resources == null) || !type3directory.exists()) ? "/res/ " +
                                        "-I " : "/" + "type3_" +
                                        base_resources + "/ -I ") +
                                "/system/framework/framework-res.apk -F " +
                                work_area +
                                "/" + overlay_package + "." + parse2_themeName + "-unsigned" +
                                ".apk " +

                                "-f --include-meta-data\n";
                    } else {
                        commands = "aapt p -M " + work_area +
                                "/AndroidManifest.xml -S " +
                                work_area +
                                (((base_resources == null) || !type3directory.exists()) ? "/res/ " +
                                        "-I " : "/" + "type3_" +
                                        base_resources + "/ -I ") +
                                "/system/framework/framework-res.apk -F " +
                                work_area +
                                "/" + overlay_package + "." + parse2_themeName + "-unsigned" +
                                ".apk " +
                                "-f --include-meta-data\n";
                    }
                }

                String line;
                Process nativeApp = Runtime.getRuntime().exec(commands);

                OutputStream stdin = nativeApp.getOutputStream();
                InputStream stderr = nativeApp.getErrorStream();
                InputStream stdout = nativeApp.getInputStream();
                stdin.write(("ls\n").getBytes());
                stdin.write("exit\n".getBytes());
                stdin.flush();
                stdin.close();

                BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                while ((line = br.readLine()) != null) {
                    Log.d("OverlayOptimizer", line);
                }
                br.close();
                br = new BufferedReader(new InputStreamReader(stderr));
                while ((line = br.readLine()) != null) {
                    Log.e("SubstratumBuilder", line);
                    has_errored_out = true;
                }
                if (has_errored_out) {
                    Log.e("SubstratumBuilder", "Installation of \"" + overlay_package + "\" has " +
                            "failed.");
                }
                br.close();

                if (!has_errored_out) {
                    // We need this Process to be waited for before moving on to the next function.
                    Log.d("SubstratumBuilder", "Overlay APK creation is running now...");
                    nativeApp.waitFor();
                    File unsignedAPK = new File(work_area + "/" + overlay_package + "." +
                            parse2_themeName + "-unsigned.apk");
                    if (unsignedAPK.exists()) {
                        Log.d("SubstratumBuilder", "Overlay APK creation has completed!");
                    } else {
                        Log.e("SubstratumBuilder", "Overlay APK creation has failed!");
                        has_errored_out = true;
                        Log.e("SubstratumBuilder", "Installation of \"" + overlay_package + "\" " +
                                "overlay has failed.");
                    }
                }
            } catch (Exception e) {
                Log.e("SubstratumBuilder", "Unfortunately, there was an exception trying to " +
                        "create a new " +
                        "APK");
                has_errored_out = true;
                Log.e("SubstratumBuilder", "Installation of \"" + overlay_package + "\" has " +
                        "failed.");
            }
        }

        // Sign the apk

        if (!has_errored_out && enable_signing) {
            try {
                // Delete the previous APK if it exists in the dashboard folder
                eu.chainfire.libsuperuser.Shell.SU.run(
                        "rm -r " + Environment.getExternalStorageDirectory().getAbsolutePath() +
                                "/.substratum/" + overlay_package + "." + parse2_themeName +
                                "-unsigned.apk");

                // Sign with the built-in test key/certificate.
                String source = work_area + "/" + overlay_package + "." + parse2_themeName +
                        "-unsigned.apk";
                String destination = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/" + overlay_package + "." + parse2_themeName + "-signed.apk";

                ZipSigner zipSigner = new ZipSigner();
                zipSigner.setKeymode("testkey");
                zipSigner.signZip(source, destination);

                Log.d("SubstratumBuilder", "APK successfully signed!");
            } catch (Throwable t) {
                Log.e("SubstratumBuilder", "APK could not be signed. " + t.toString());
                has_errored_out = true;
                Log.e("SubstratumBuilder", "Installation of \"" + overlay_package + "\" has " +
                        "failed.");
            }
        }

        // Install the APK silently
        // Superuser needed as this requires elevated privileges to run these commands

        if (!has_errored_out) {
            if (update_mode) {
                try {
                    if (variant != null) {
                        eu.chainfire.libsuperuser.Shell.SU.run(
                                "pm install -r " + Environment.getExternalStorageDirectory()
                                        .getAbsolutePath() +
                                        "/.substratum/" + overlay_package + "." + parse2_themeName +
                                        "-signed" +
                                        ".apk");
                        Log.d("SubstratumBuilder", "Silently installing APK...");
                        if (checkIfPackageInstalled(overlay_package + "." + parse2_themeName +
                                parse2_variantName + parse2_baseName, context)) {
                            Log.d("SubstratumBuilder", "Overlay APK has successfully been " +
                                    "installed!");
                        } else {
                            Log.e("SubstratumBuilder", "Overlay APK has failed to install!");
                        }
                    } else {
                        eu.chainfire.libsuperuser.Shell.SU.run(
                                "pm install -r " + Environment.getExternalStorageDirectory()
                                        .getAbsolutePath() +
                                        "/.substratum/" + overlay_package + "." + parse2_themeName +
                                        "-signed" +
                                        ".apk");
                        Log.d("SubstratumBuilder", "Silently installing APK...");
                        if (checkIfPackageInstalled(overlay_package + "." + parse2_themeName,
                                context)) {
                            Log.d("SubstratumBuilder", "Overlay APK has successfully been " +
                                    "installed!");
                        } else {
                            Log.e("SubstratumBuilder", "Overlay APK has failed to install!");
                        }
                    }
                } catch (Exception e) {
                    Log.e("SubstratumBuilder", "Overlay APK has failed to install! (Exception)");
                    has_errored_out = true;
                    Log.e("SubstratumBuilder", "Installation of \"" + overlay_package + "\" has " +
                            "failed.");
                }
            } else {
                Log.d("SubstratumBuilder", "Update mode flag disabled, returning one-line " +
                        "parsable command");
                no_install = "pm install -r " + Environment.getExternalStorageDirectory()
                        .getAbsolutePath() +
                        "/.substratum/" + overlay_package + "." + parse2_themeName +
                        "-signed" +
                        ".apk";
            }
        }
    }

    private Boolean checkIfPackageInstalled(String packagename, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void copyAAPT(String filename) {
        AssetManager assetManager = mContext.getAssets();
        String TARGET_BASE_PATH = mContext.getFilesDir().getAbsolutePath() + "/";

        InputStream in;
        OutputStream out;
        String newFileName;
        try {
            in = assetManager.open(filename);
            newFileName = TARGET_BASE_PATH + filename;
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
