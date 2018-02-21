/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.common.analytics;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public enum PackageAnalytics {
    ;

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
        Log.d(RUNTIME_TAG, "Max Memory: " + max);
        Log.d(RUNTIME_TAG, "Total Memory: " + total);
        Log.d(RUNTIME_TAG, "Free Memory: " + free);
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

            Log.d(PACKAGE_TAG, "Package Information for: " + packageName);
            Log.d(PACKAGE_TAG, "Installation date: " + text);
            Log.d(PACKAGE_TAG, "Installation time: " + text2);
            Log.d(PACKAGE_TAG, "Installation location: " + installer);
        } catch (Exception ignored) {
        }
    }
}