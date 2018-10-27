/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.services.notification;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import projekt.substratum.common.Packages;

public class UnsupportedThemeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String packageToUninstall = intent.getStringExtra("package_to_uninstall");
        int notificationToKill = intent.getIntExtra("notification_to_close", 0);
        if (packageToUninstall != null && packageToUninstall.length() > 0) {
            if (Packages.isPackageInstalled(context, packageToUninstall)) {
                // At this point, we can instantiate an uninstall on notification action click
                Uri packageURI = Uri.parse("package:" + packageToUninstall);

                // Cancel the notification
                if (notificationToKill > 0) {
                    NotificationManager notificationManager = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        notificationManager.cancel(notificationToKill);
                    }
                }

                // Close the notification panel
                Intent closeSysUi = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.getApplicationContext().sendBroadcast(closeSysUi);

                // Fire the uninstall intent
                Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
                uninstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(uninstallIntent);
            }
        }
    }
}