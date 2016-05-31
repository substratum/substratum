package projekt.substratum.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
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

public class LayersBuilder {

    /*

    How to use this class:

    1. injectAAPT(Context) : initial check/injection for AAPT access on device
    2. initializeCache(Context, package_identifier) : extract assets for theme so no reuse needed
    3. beginAction(Context, package_name, theme_package) : start LayersBuilder function
        - this will create an AndroidManifest based on selected package
        - then it will compile using the new work zone

     */

    public Boolean has_errored_out = false;
    private Context mContext;

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
                Log.d("Phase 1", "Android Assets Packaging Tool (ARM) has been injected into the " +
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
                Log.d("Phase 1", "Android Assets Packaging Tool (x86) has been injected into the " +
                        "system partition.");
            }
        } else {
            Log.d("Phase 1", "There is no need to inject AAPT into the system partition.");
        }
    }

    public void initializeCache(Context context, String package_identifier) {
        mContext = context;
        try {
            unzip(package_identifier);
            Log.d("Phase 2", "The cache has been built from the APK assets");
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

    private void unzip(String package_identifier) throws IOException {
        // First, extract the APK as a zip so we don't have to access the APK multiple times

        int folder_abbreviation = checkCurrentThemeSelectionLocation(package_identifier);
        if (folder_abbreviation != 0) {
            String source = "/data/app/" + package_identifier + "-" +
                    folder_abbreviation + "/base.apk";
            File checkFile = new File(source);
            long fileSize = checkFile.length();
            File myDir = new File(mContext.getCacheDir(), "LayersBuilder");
            if (!myDir.exists()) {
                myDir.mkdir();
            }
            String destination = mContext.getCacheDir().getAbsolutePath() + "/LayersBuilder";

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
            Log.d("Unzip",
                    "There is no valid package name under this abbreviated folder " +
                            "count.");
        }

        // Second, clean out the cache folder for files that aren't needed
        // Superuser is used due to some files being held hostage by the system

        File[] fileList = new File(mContext.getCacheDir().getAbsolutePath() +
                "/LayersBuilder/").listFiles();
        for (int i = 0; i < fileList.length; i++) {
            if (!fileList[i].getName().equals("assets")) {
                eu.chainfire.libsuperuser.Shell.SU.run(
                        "rm -r " + mContext.getCacheDir().getAbsolutePath() +
                                "/LayersBuilder/" + fileList[i].getName());
            }
        }
    }

    public void beginAction(Context context, String overlay_package, String theme_name) {

        has_errored_out = false;
        mContext = context;
        String work_area;

        // 1. Set work area to asset chosen based on the parameter passed into this class


        work_area = mContext.getCacheDir().getAbsolutePath() + "/LayersBuilder/assets/overlays/"
                + overlay_package;


        // 2. Create a modified Android Manifest for use with aapt

        File root = new File(work_area + "/AndroidManifest.xml");

        // 2a. Parse the theme's name before adding it into the new manifest to prevent any issues

        String parse1_themeName = theme_name.replaceAll("\\s+", "");
        String parse2_themeName = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");

        if (parse2_themeName.equals("")) {
            int inputNumber = 1;
            parse2_themeName = "no_name";
        }

        // 2b. Create the manifest file based on the new parsed names

        if (!has_errored_out) {
            try {
                root.createNewFile();
                FileWriter fw = new FileWriter(root);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter pw = new PrintWriter(bw);
                String manifest =
                        "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +
                                "<manifest xmlns:android=\"http://schemas.android" +
                                ".com/apk/res/android\" package=\"" + overlay_package + "." +
                                parse2_themeName + "\">\n" +
                                "    <overlay android:targetPackage=\"" + overlay_package + "\" " +
                                "android:priority=\"100\"/>\n" +
                                "</manifest>\n";
                pw.write(manifest);
                pw.close();
                bw.close();
                fw.close();
            } catch (IOException e) {
                Log.e("Phase 3", "There was an exception creating a new Manifest file!");
                has_errored_out = true;
                Log.e("LayersBuilder", "Installation of \"" + overlay_package + "\" has failed.");
            }
        }

        // Compile the new theme apk based on new manifest, framework-res.apk and extracted asset

        if (!has_errored_out) {
            try {
                String line;
                Process nativeApp = Runtime.getRuntime().exec(
                        "aapt p -M " + work_area +
                                "/AndroidManifest.xml -S " +
                                work_area +
                                "/res/ -I " +
                                "/system/framework/framework-res.apk -F " +
                                work_area +
                                "/" + overlay_package + "." + parse2_themeName + "-unsigned.apk " +
                                "-f\n");


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
                    Log.e("LayersBuilder", line);
                    has_errored_out = true;
                    Log.e("LayersBuilder", "Installation of \"" + overlay_package + "\" has " +
                            "failed.");
                }
                br.close();

                if (!has_errored_out) {
                    // We need this Process to be waited for before moving on to the next function.
                    Log.d("Phase 3", "Overlay APK creation is running now...");
                    nativeApp.waitFor();
                    File unsignedAPK = new File(work_area + "/" + overlay_package + "." +
                            parse2_themeName + "-unsigned.apk");
                    if (unsignedAPK.exists()) {
                        Log.d("Phase 3", "Overlay APK creation has completed!");
                    } else {
                        Log.e("Phase 3", "Overlay APK creation has failed!");
                        has_errored_out = true;
                        Log.e("LayersBuilder", "Installation of \"" + overlay_package + "\" " +
                                "overlay has failed.");

                    }
                }
            } catch (Exception e) {
                Log.e("Phase 3", "Unfortunately, there was an exception trying to create a new " +
                        "APK");
                has_errored_out = true;
                Log.e("LayersBuilder", "Installation of \"" + overlay_package + "\" has failed.");
            }
        }

        // Sign the apk

        if (!has_errored_out) {
            try {
                // Delete the previous APK if it exists in the dashboard folder
                eu.chainfire.libsuperuser.Shell.SU.run(
                        "rm -r " + Environment.getExternalStorageDirectory().getAbsolutePath() +
                                "/substratum/" + overlay_package + "." + parse2_themeName +
                                "-unsigned.apk");

                // Sign with the built-in auto-test key/certificate.
                String source = work_area + "/" + overlay_package + "." + parse2_themeName +
                        "-unsigned.apk";
                String destination = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/" + overlay_package + "." + parse2_themeName + "-signed.apk";

                ZipSigner zipSigner = new ZipSigner();
                zipSigner.setKeymode("testkey");
                zipSigner.signZip(source, destination);

                Log.d("Phase 3", "APK successfully signed!");
            } catch (Throwable t) {
                Log.e("Phase 3", "APK could not be signed. " + t.toString());
                has_errored_out = true;
                Log.e("LayersBuilder", "Installation of \"" + overlay_package + "\" has failed.");
            }
        }

        // Install the APK silently
        // Superuser needed as this requires elevated privileges to run these commands

        if (!has_errored_out) {
            try {
                eu.chainfire.libsuperuser.Shell.SU.run(
                        "pm install " + Environment.getExternalStorageDirectory().getAbsolutePath
                                () +

                                "/substratum/" + overlay_package + "." + parse2_themeName +
                                "-signed" +
                                ".apk");

                // We need this Process to be waited for before moving on to the next function.
                Log.d("Phase 3", "Silently installing APK...");
                if (checkIfPackageInstalled(overlay_package + "." + parse2_themeName, context)) {
                    Log.d("Phase 3", "Overlay APK has successfully been installed!");
                } else {
                    Log.e("Phase 3", "Overlay APK has failed to install!");
                }
            } catch (Exception e) {
                Log.e("Phase 3", "Overlay APK has failed to install! (Exception)");
                has_errored_out = true;
                Log.e("LayersBuilder", "Installation of \"" + overlay_package + "\" has failed.");
            }
        }

        // Enable the APK using om list, om enable
        // Superuser needed as this requires elevated privileges to run these commands

        if (!has_errored_out) {
            try {
                eu.chainfire.libsuperuser.Shell.SU.run(
                        "om enable " + overlay_package + "." + parse2_themeName);

                // We need this Process to be waited for before moving on to the next function.
                Log.d("Phase 3", "(OverlayManagerService) Enabling overlay \"" + overlay_package
                        + "." +

                        parse2_themeName + "\"");
                if (checkIfPackageInstalled(overlay_package + "." + parse2_themeName, context)) {
                    Log.d("Phase 3", "(OverlayManagerService) Successfully enabled overlay!");
                } else {
                    Log.e("Phase 3", "(OverlayManagerService) Failed to enable overlay!");
                    has_errored_out = true;
                    Log.e("LayersBuilder", "Installation of \"" + overlay_package + "\" has " +
                            "failed.");
                }
            } catch (Exception e) {
                Log.e("Phase 3", "(OverlayManagerService) Failed to enable overlay! (Exception)");
                has_errored_out = true;
                Log.e("LayersBuilder", "Installation of \"" + overlay_package + "\" has failed.");
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
