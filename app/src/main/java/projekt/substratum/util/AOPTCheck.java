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

import projekt.substratum.config.References;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class AOPTCheck {

    private Context mContext;

    public void injectAOPT(Context context) {
        mContext = context;

        // Check if aopt is installed on the device

        File aopt = new File("/system/bin/aopt");
        if (!aopt.exists()) {
            if (!Build.SUPPORTED_ABIS.toString().contains("86")) {
                if (!Build.SUPPORTED_ABIS.toString().contains("64")) {
                    try {
                        // Take account for ARM64 first
                        copyAOPT("aopt");
                        References.mountRW();
                        References.copy(context.getFilesDir().getAbsolutePath() +
                                "/aopt", "/system/bin/aopt");
                        References.setPermissions(777, "/system/bin/aopt");
                        References.mountRO();
                        Log.d("SubstratumLogger", "Android Overlay Packaging Tool (ARM64) has been"
                                + " added into the system partition.");
                    } catch (Exception e) {
                        //
                    }
                } else {
                    // Take account for ARM devices
                    try {
                        copyAOPT("aopt");
                        References.mountRW();
                        References.copy(context.getFilesDir().getAbsolutePath() +
                                "/aopt", "/system/bin/aopt");
                        References.setPermissions(777, "/system/bin/aopt");
                        References.mountRO();
                        Log.d("SubstratumLogger", "Android Overlay Packaging Tool (ARM) has been" +
                                " added into the system partition.");
                    } catch (Exception e) {
                        //
                    }
                }
            } else {
                // Take account for x86 devices
                try {
                    copyAOPT("aopt-x86");
                    References.mountRW();
                    References.copy(context.getFilesDir().getAbsolutePath() +
                            "/aopt-x86", "/system/bin/aopt");
                    References.setPermissions(777, "/system/bin/aopt");
                    References.mountRO();
                    Log.d("SubstratumLogger", "Android Overlay Packaging Tool (x86) has been" +
                            " added into the system partition.");
                } catch (Exception e) {
                    //
                }
            }
        } else if (aopt.exists()) {
            String integrityCheck = checkAOPTIntegrity();
            // AOPT outputs different ui
            if (integrityCheck != null && integrityCheck.equals("Android Asset Packaging " +
                    "Tool, v0.2-")) {
                Log.d("SubstratumLogger", "The system partition already contains an existing " +
                        "AOPT binary and Substratum is locked and loaded!");
            } else {
                Log.e("SubstratumLogger",
                        "The system partition already contains an existing AOPT " +
                                "binary, however it does not match Substratum integrity.");
                if (!Build.SUPPORTED_ABIS.toString().contains("86")) {
                    // Take account for ARM/ARM64 devices
                    copyAOPT("aopt");
                    References.mountRW();
                    References.delete("/system/bin/aopt");
                    References.copy(context.getFilesDir().getAbsolutePath() +
                            "/aopt", "/system/bin/aopt");
                    References.setPermissions(777, "/system/bin/aopt");
                    References.mountRO();
                    Log.d("SubstratumLogger", "Android Overlay Packaging Tool (ARM) has been " +
                            "injected into the system partition.");
                } else {
                    // Take account for x86 devices
                    copyAOPT("aopt-x86");
                    References.mountRW();
                    References.delete("/system/bin/aopt");
                    References.copy(context.getFilesDir().getAbsolutePath() +
                            "/aopt-x86", "/system/bin/aopt");
                    References.setPermissions(777, "/system/bin/aopt");
                    References.mountRO();
                    Log.d("SubstratumLogger", "Android Overlay Packaging Tool (x86) has been " +
                            "injected into the system partition.");
                }
            }
        }
    }

    private void copyAOPT(String filename) {
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

    public String checkAOPTIntegrity() {
        Process proc = null;
        try {
            Runtime rt = Runtime.getRuntime();
            proc = rt.exec(new String[]{"aopt", "version"});
            try (BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()))) {
                return stdInput.readLine();
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
