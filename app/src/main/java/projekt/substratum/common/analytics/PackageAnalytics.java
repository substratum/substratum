/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.common.analytics;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import projekt.substratum.Substratum;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PackageAnalytics {

    public static final String PACKAGE_TAG = "PackageLogger";
    private static final String RUNTIME_TAG = "RuntimeMemory";

    public static boolean isLowEnd() {
        Float maximumMemory = logRuntimeMemoryLimits()[0];
        return maximumMemory <= 130.0F;
    }

    private static Float[] logRuntimeMemoryLimits() {
        String max = humanReadableByteCount(Runtime.getRuntime().maxMemory())
                .replaceAll(",", ".");
        String total = humanReadableByteCount(Runtime.getRuntime().totalMemory())
                .replaceAll(",", ".");
        String free = humanReadableByteCount(Runtime.getRuntime().freeMemory())
                .replaceAll(",", ".");
        Substratum.log(RUNTIME_TAG, "Max Memory: " + max);
        Substratum.log(RUNTIME_TAG, "Total Memory: " + total);
        Substratum.log(RUNTIME_TAG, "Free Memory: " + free);
        return new Float[]{
                Float.valueOf(max.replaceAll("[a-zA-Z]", "")),
                Float.valueOf(total.replaceAll("[a-zA-Z]", "")),
                Float.valueOf(free.replaceAll("[a-zA-Z]", ""))
        };
    }

    private static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < (long) unit) return bytes + " B";
        int exp = (int) (StrictMath.log((double) bytes) / StrictMath.log((double) unit));
        String pre = ("KMGTPE").charAt(exp - 1) + ("i");
        return String.format(Locale.US, "%.1f %sB",
                (double) bytes / StrictMath.pow((double) unit, (double) exp), pre);
    }

    public static void logPackageInfo(Context context,
                                      String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            String installer = packageManager.getInstallerPackageName(packageName);
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);

            long installed = new File(appInfo.sourceDir).lastModified();
            Date date = new Date(installed);
            SimpleDateFormat format =
                    new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            SimpleDateFormat format2 =
                    new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

            String text = format.format(date);
            String text2 = format2.format(date);

            Substratum.log(PACKAGE_TAG, "Package Information for: " + packageName);
            Substratum.log(PACKAGE_TAG, "Installation date: " + text);
            Substratum.log(PACKAGE_TAG, "Installation time: " + text2);
            Substratum.log(PACKAGE_TAG, "Installation location: " + installer);
        } catch (Exception ignored) {
        }
    }
}