package projekt.substratum.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;

import projekt.substratum.config.References;

public class AOPTCheck {

    private Context mContext;

    public void injectAOPT(Context context, Boolean forced) {
        mContext = context;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Check if AOPT is installed on the device
        File aopt = new File("/system/bin/aopt");
        if (!aopt.exists() || forced) {
            if (!Arrays.toString(Build.SUPPORTED_ABIS).contains("86")) {
                // Developers: AOPT-ARM (32bit) is using the legacy AAPT binary, while AAPT-ARM64
                //             (64bit) is using the brand new AOPT binary.
                String architecture =
                        Arrays.asList(Build.SUPPORTED_64_BIT_ABIS).size() > 0 ? "ARM64" : "ARM";
                try {
                    if (prefs.getString("compiler", "aopt").equals("aopt")) {
                        copyAOPT("aopt" + (architecture.equals("ARM64") ? "64" : ""));
                        References.mountRW();
                        References.copy(context.getFilesDir().getAbsolutePath() +
                                        "/aopt" + (architecture.equals("ARM64") ? "64" : ""),
                                "/system/bin/aopt");
                        References.setPermissions(777, "/system/bin/aopt");
                        References.mountRO();
                        Log.d(References.SUBSTRATUM_LOG,
                                "Android Overlay Packaging Tool (" + architecture + ") has been"
                                        + " added into the system partition.");
                    } else {
                        copyAOPT("aapt");
                        References.mountRW();
                        References.copy(context.getFilesDir().getAbsolutePath() +
                                "/aapt", "/system/bin/aopt");
                        References.setPermissions(777, "/system/bin/aopt");
                        References.mountRO();
                        Log.d(References.SUBSTRATUM_LOG,
                                "Android Assets Packaging Tool (" + architecture + ") has been"
                                        + " added into the system partition.");
                    }
                } catch (Exception e) {
                    // Suppress warning
                }
            } else {
                // Take account for x86 devices
                try {
                    copyAOPT("aapt86");
                    References.mountRW();
                    References.copy(context.getFilesDir().getAbsolutePath() +
                            "/aapt86", "/system/bin/aopt");
                    References.setPermissions(777, "/system/bin/aopt");
                    References.mountRO();
                    Log.d(References.SUBSTRATUM_LOG, "Android Assets Packaging Tool (x86) has " +
                            "been" +
                            " added into the system partition.");
                } catch (Exception e) {
                    //
                }
            }
        } else if (aopt.exists()) {
            String integrityCheck = checkAOPTIntegrity();
            // AOPT outputs different ui
            if (integrityCheck != null &&
                    (integrityCheck.equals("Android Asset Packaging Tool, v0.2-") ||
                            integrityCheck.equals(
                                    "Android Overlay Packaging Tool, v0.2-android-7" +
                                            ".0-userdebug")
                    )) {
                Log.d(References.SUBSTRATUM_LOG, "The system partition already contains an " +
                        "existing " +
                        "compiler and Substratum is locked and loaded!");
            } else {
                Log.e(References.SUBSTRATUM_LOG,
                        "The system partition already contains an existing compiler, " +
                                "however it does not match Substratum integrity.");
                if (!Arrays.toString(Build.SUPPORTED_ABIS).contains("86")) {
                    // Developers: AOPT-ARM (32bit) is using the legacy AAPT binary,
                    //             while AAPT-ARM64 (64bit) is using the brand new AOPT binary.
                    String architecture =
                            Arrays.asList(Build.SUPPORTED_64_BIT_ABIS).size() > 0 ? "ARM64" : "ARM";
                    try {
                        if (prefs.getString("compiler", "aopt").equals("aopt")) {
                            copyAOPT("aopt" + (architecture.equals("ARM64") ? "64" : ""));
                            References.mountRW();
                            References.copy(context.getFilesDir().getAbsolutePath() +
                                            "/aopt" + (architecture.equals("ARM64") ? "64" : ""),
                                    "/system/bin/aopt");
                            References.setPermissions(777, "/system/bin/aopt");
                            References.mountRO();
                            Log.d(References.SUBSTRATUM_LOG,
                                    "Android Overlay Packaging Tool (" + architecture + ") has been"
                                            + " added into the system partition.");
                        } else {
                            copyAOPT("aapt");
                            References.mountRW();
                            References.copy(context.getFilesDir().getAbsolutePath() +
                                    "/aapt", "/system/bin/aopt");
                            References.setPermissions(777, "/system/bin/aopt");
                            References.mountRO();
                            Log.d(References.SUBSTRATUM_LOG,
                                    "Android Assets Packaging Tool (" + architecture + ") has been"
                                            + " added into the system partition.");
                        }
                    } catch (Exception e) {
                        // Suppress warning
                    }
                } else {
                    // Take account for x86 devices
                    try {
                        copyAOPT("aapt86");
                        References.mountRW();
                        References.copy(context.getFilesDir().getAbsolutePath() +
                                "/aapt86", "/system/bin/aopt");
                        References.setPermissions(777, "/system/bin/aopt");
                        References.mountRO();
                        Log.d(References.SUBSTRATUM_LOG, "Android Assets Packaging Tool (x86) has" +
                                " been" +
                                " added into the system partition.");
                    } catch (Exception e) {
                        //
                    }
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

    private String checkAOPTIntegrity() {
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