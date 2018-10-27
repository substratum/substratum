/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.helpers;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.FileOperations;

import java.io.File;
import java.util.Arrays;

public class BinaryInstaller {

    /**
     * Install the AAPT/AAPT and ZipAlign binaries to the working files of Substratum
     *
     * @param context Self explanatory, bud.
     * @param forced  Ignore the dynamic check and just install no matter what
     */
    public static void install(Context context, boolean forced) {
        injectAAPT(context, forced);
        injectZipAlign(context, forced);
    }

    /**
     * Inject AAPT/AAPT binaries into the device
     *
     * @param context Self explanatory, bud.
     * @param forced  Ignore the dynamic check and just install no matter what
     */
    private static void injectAAPT(Context context, boolean forced) {
        String aaptPath = context.getFilesDir().getAbsolutePath() + "/aapt";

        // Check if AAPT is installed on the device
        File aapt = new File(aaptPath);

        if (!aapt.isFile() || forced) {
            inject(context, aaptPath);
        } else if (aapt.exists()) {
            Substratum.log(References.SUBSTRATUM_LOG,
                    "The system partition already contains an existing compiler " +
                            "and Substratum is locked and loaded!");
        } else {
            Log.e(References.SUBSTRATUM_LOG,
                    "The system partition already contains an existing compiler, " +
                            "however it does not match Substratum integrity.");
            inject(context, aaptPath);
        }
    }

    /**
     * Inject a file into the device
     *
     * @param context  Self explanatory, bud.
     * @param aaptPath Location of AAPT
     */
    private static void inject(Context context, String aaptPath) {
        if (!Arrays.toString(Build.SUPPORTED_ABIS).contains("86")) {
            String architecture =
                    !Arrays.asList(Build.SUPPORTED_64_BIT_ABIS).isEmpty() ? "ARM64" : "ARM";
            FileOperations.copyFromAsset(context, "aapt" + ("ARM64".equals(architecture) ?
                    "64" :
                    ""), aaptPath);
            Substratum.log(References.SUBSTRATUM_LOG,
                    "Android Asset Packaging Tool (" + architecture + ") " +
                            "has been added into the compiler directory.");
        } else {
            // Take account for x86 devices
            try {
                FileOperations.copyFromAsset(context, "aaptx86", aaptPath);
                Substratum.log(References.SUBSTRATUM_LOG,
                        "Android Asset Packaging Tool (x86) " +
                                "has been added into the compiler directory.");
            } catch (Exception ignored) {
            }
        }
        File f = new File(aaptPath);
        if (f.isFile()) {
            if (!f.setExecutable(true, true))
                Log.e("BinaryInstaller", "Could not set executable...");
        }
    }

    /**
     * Inject ZipAlign binaries into the device
     *
     * @param context Self explanatory, bud.
     * @param forced  Ignore the dynamic check and just install no matter what
     */
    private static void injectZipAlign(Context context, boolean forced) {
        String zipalignPath = context.getFilesDir().getAbsolutePath() + "/zipalign";
        File f = new File(zipalignPath);

        // Check if ZipAlign is already installed
        if (f.exists() && !forced)
            return;

        if (!Arrays.toString(Build.SUPPORTED_ABIS).contains("86")) {
            String architecture =
                    !Arrays.asList(Build.SUPPORTED_64_BIT_ABIS).isEmpty() ? "ARM64" : "ARM";
            FileOperations.copyFromAsset(context, "zipalign" + ("ARM64".equals(architecture) ?
                    "64" :
                    ""), zipalignPath);
            Substratum.log(References.SUBSTRATUM_LOG,
                    "ZipAlign (" + architecture + ") " +
                            "has been added into the compiler directory.");
        } else {
            // Take account for x86 devices
            try {
                FileOperations.copyFromAsset(context, "zipalign86", zipalignPath);
                Substratum.log(References.SUBSTRATUM_LOG,
                        "ZipAlign (x86) " +
                                "has been added into the compiler directory.");
            } catch (Exception ignored) {
            }
        }

        if (f.isFile()) {
            if (!f.setExecutable(true, true))
                Log.e("BinaryInstaller", "Could not set executable...");
        }
    }
}