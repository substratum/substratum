package projekt.substratum.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class AAPTCheck {

    private Context mContext;

    public void injectAAPT(Context context) {
        mContext = context;

        // Check if aapt is installed on the device

        File aapt = new File("/system/bin/aapt");
        if (!aapt.exists()) {
            if (!Build.SUPPORTED_ABIS.toString().contains("86")) {
                // Take account for ARM/ARM64 devices
                try {
                    copyAAPT("aapt");
                    Root.runCommand("mount -o rw,remount /system");
                    Root.runCommand(
                            "cp " + context.getFilesDir().getAbsolutePath() +
                                    "/aapt " +
                                    "/system/bin/aapt");
                    Root.runCommand("chmod 755 /system/bin/aapt");
                    Root.runCommand("mount -o ro,remount /system");
                    Log.d("SubstratumLogger", "Android Assets Packaging Tool (ARM) has been" +
                            " injected into the system partition.");
                } catch (Exception e) {
                    //
                }
            } else {
                // Take account for x86 devices
                try {
                    copyAAPT("aapt-x86");
                    Root.runCommand("mount -o rw,remount /system");
                    Root.runCommand(
                            "cp " + context.getFilesDir().getAbsolutePath() +
                                    "/aapt-x86 " +
                                    "/system/bin/aapt");
                    Root.runCommand("chmod 755 /system/bin/aapt");
                    Root.runCommand("mount -o ro,remount /system");
                    Log.d("SubstratumLogger", "Android Assets Packaging Tool (x86) has been" +
                            " injected into the system partition.");
                } catch (Exception e) {
                    //
                }
            }
        } else {
            String integrityCheck = checkAAPTIntegrity();

            if (integrityCheck != null && integrityCheck.equals("Android Asset Packaging Tool")) {
                Log.d("SubstratumLogger", "The system partition already contains an existing " +
                        "AAPT binary and Substratum is locked and loaded!");
            } else {
                Log.e("SubstratumLogger",
                        "The system partition already contains an existing AAPT " +
                                "binary, however it does not match Substratum integrity.");
                if (!Build.SUPPORTED_ABIS.toString().contains("86")) {
                    // Take account for ARM/ARM64 devices
                    copyAAPT("aapt");
                    Root.runCommand("mount -o rw,remount /system");
                    Root.runCommand("rm -rf /system/bin/aapt");
                    Root.runCommand(
                            "cp " + context.getFilesDir().getAbsolutePath() +
                                    "/aapt " +
                                    "/system/bin/aapt");
                    Root.runCommand("chmod 755 /system/bin/aapt");
                    Root.runCommand("mount -o ro,remount /system");
                    Log.d("SubstratumLogger", "Android Assets Packaging Tool (ARM) has been " +
                            "injected into the system partition.");
                } else {
                    // Take account for x86 devices
                    copyAAPT("aapt-x86");
                    Root.runCommand("mount -o rw,remount /system");
                    Root.runCommand("rm -rf /system/bin/aapt");
                    Root.runCommand(
                            "cp " + context.getFilesDir().getAbsolutePath() +
                                    "/aapt-x86 " +
                                    "/system/bin/aapt");
                    Root.runCommand("chmod 755 /system/bin/aapt");
                    Root.runCommand("mount -o ro,remount /system");
                    Log.d("SubstratumLogger", "Android Assets Packaging Tool (x86) has been " +
                            "injected into the system partition.");
                }
            }
        }
    }

    private void copyAAPT(String filename) {
        AssetManager assetManager = mContext.getAssets();
        String TARGET_BASE_PATH = mContext.getFilesDir().getAbsolutePath() + "/";
        String newFileName = TARGET_BASE_PATH + filename;
        try (InputStream in = assetManager.open(filename);
             OutputStream out = new FileOutputStream(newFileName)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String checkAAPTIntegrity() {
        Process proc = null;
        try {
            Runtime rt = Runtime.getRuntime();
            String[] commands = {"aapt"};
            proc = rt.exec(commands);
            try (BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()))) {
                return stdError.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (proc != null) {
                proc.destroy();
            }
        }
        return null;
    }
}
