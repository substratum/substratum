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

package projekt.substratum.common;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import projekt.substratum.R;
import projekt.substratum.util.views.Lunchbar;

public enum Activities {
    ;

    /**
     * Launches a specified activity URL but on error, throws a toast
     *
     * @param context  Self explanatory, bud.
     * @param resource Link to be launched
     */
    public static void launchActivityUrl(Context context,
                                         int resource) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(context.getString(resource)));
            context.startActivity(i);
        } catch (ActivityNotFoundException activityNotFoundException) {
            Toast.makeText(context,
                    context.getString(R.string.activity_missing_toast),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Launches a specified activity URL but on error, throws a LunchBar
     *
     * @param context  Self explanatory, bud.
     * @param view     RootView of the activity for the LunchBar to be invoked
     * @param resource Link to be launched
     */
    public static void launchActivityUrl(Context context,
                                         View view,
                                         int resource) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(context.getString(resource)));
            context.startActivity(i);
        } catch (ActivityNotFoundException activityNotFoundException) {
            Lunchbar.make(view,
                    context.getString(R.string.activity_missing_toast),
                    Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Launches a specific activity from another app
     *
     * @param context     Self explanatory, bud.
     * @param packageName Package name of app to be launched
     * @param className   Class name of the activity
     */
    public static void launchExternalActivity(Context context,
                                              String packageName,
                                              String className) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, packageName + '.' + className));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Launches a specific activity from another app
     *
     * @param context Self explanatory, bud.
     * @param target  Class name of the activity
     */
    public static void launchInternalActivity(Context context,
                                              Class target) {
        Intent intent = new Intent(context, target);
        context.startActivity(intent);
    }
}