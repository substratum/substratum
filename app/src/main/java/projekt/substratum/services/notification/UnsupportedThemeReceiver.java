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
        Integer notificationToKill = intent.getIntExtra("notification_to_close", 0);
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
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                context.startActivity(uninstallIntent);
            }
        }
    }
}