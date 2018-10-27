/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.common;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import projekt.substratum.R;

public class Activities {

    /**
     * Launches a specified activity URL but on error, throws a toast
     *
     * @param context  Self explanatory, bud.
     * @param resource Link to be launched
     */
    public static void launchActivityUrl(Context context,
                                         int resource) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(context.getString(resource)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(context,
                    context.getString(R.string.activity_missing_toast),
                    Toast.LENGTH_SHORT).show();
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
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(packageName, packageName + '.' + className));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(context,
                    context.getString(R.string.activity_missing_toast),
                    Toast.LENGTH_SHORT).show();
        }
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