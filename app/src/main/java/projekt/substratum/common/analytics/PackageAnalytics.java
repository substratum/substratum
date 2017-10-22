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

import android.annotation.SuppressLint;
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
        final Float maximum_memory = PackageAnalytics.logRuntimeMemoryLimits()[0];
        return maximum_memory <= 130.0F;
    }

    private static Float[] logRuntimeMemoryLimits() {
        final String max = humanReadableByteCount(Runtime.getRuntime().maxMemory(), false)
                .replaceAll(",", ".");
        final String total = humanReadableByteCount(Runtime.getRuntime().totalMemory(), false)
                .replaceAll(",", ".");
        final String free = humanReadableByteCount(Runtime.getRuntime().freeMemory(), false)
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

    @SuppressWarnings("SameParameterValue")
    @SuppressLint("DefaultLocale")
    private static String humanReadableByteCount(final long bytes, final boolean si) {
        final int unit = si ? 1000 : 1024;
        if (bytes < (long) unit) return bytes + " B";
        final int exp = (int) (Math.log((double) bytes) / Math.log((double) unit));
        final String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", (double) bytes / Math.pow((double) unit, (double) exp), pre);
    }

    public static void logPackageInfo(final Context context, final String packageName) {
        try {
            final PackageManager packageManager = context.getPackageManager();
            final String installer = packageManager.getInstallerPackageName(packageName);
            final ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);

            final long installed = new File(appInfo.sourceDir).lastModified();
            final Date date = new Date(installed);
            final SimpleDateFormat format =
                    new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            final SimpleDateFormat format2 =
                    new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

            final String text = format.format(date);
            final String text2 = format2.format(date);

            Log.d(PACKAGE_TAG, "Package Information for: " + packageName);
            Log.d(PACKAGE_TAG, "Installation date: " + text);
            Log.d(PACKAGE_TAG, "Installation time: " + text2);
            Log.d(PACKAGE_TAG, "Installation location: " + installer + "");
        } catch (final Exception e) {
            // Suppress warning
        }
    }

    public static String getPackageInstaller(final Context context, final String packageName) {
        try {
            final PackageManager packageManager = context.getPackageManager();
            return packageManager.getInstallerPackageName(packageName);
        } catch (final Exception e) {
            // Suppress warning
        }
        return null;
    }
}