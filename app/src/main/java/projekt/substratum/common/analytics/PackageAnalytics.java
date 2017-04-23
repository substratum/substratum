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

public class PackageAnalytics {

    public static void logPackageInfo(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo;

            PackageManager packageManager = context.getPackageManager();
            String installer = packageManager.getInstallerPackageName(packageName);
            appInfo = pm.getApplicationInfo(packageName, 0);

            long installed = new File(appInfo.sourceDir).lastModified();
            Date date = new Date(installed);
            SimpleDateFormat format =
                    new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            SimpleDateFormat format2 =
                    new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

            String text = format.format(date);
            String text2 = format2.format(date);

            Log.d("PackageLogger", "Package Information for: " + packageName);
            Log.d("PackageLogger", "Installation date: " + text);
            Log.d("PackageLogger", "Installation time: " + text2);
            Log.d("PackageLogger", "Installation location: " + installer + "");
        } catch (Exception e) {
            // Suppress warning
        }
    }
}