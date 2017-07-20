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

package projekt.substratum;

import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.util.Log;

import projekt.substratum.common.References;
import projekt.substratum.services.binder.BinderService;

public class Substratum extends Application {

    private static final String BINDER_TAG = "BinderService";
    private static Substratum substratum;

    public static Substratum getInstance() {
        return substratum;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        substratum = this;
        startBinderService();
        References.registerBroadcastReceivers(this);
        createNotificationChannel();
    }

    public void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel mainChannel = new NotificationChannel(
                    References.DEFAULT_NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_default),
                    NotificationManager.IMPORTANCE_DEFAULT);
            mainChannel.setDescription(
                    getString(R.string.notification_channel_default_description));
            mainChannel.setSound(null, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build());
            assert notificationManager != null;
            notificationManager.createNotificationChannel(mainChannel);

            NotificationChannel compileChannel = new NotificationChannel(
                    References.ONGOING_NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_ongoing),
                    NotificationManager.IMPORTANCE_LOW);
            mainChannel.setDescription(
                    getString(R.string.notification_channel_ongoing_description));
            notificationManager.createNotificationChannel(compileChannel);
        }
    }

    public void startBinderService() {
        if (References.isBinderInterfacer(getApplicationContext())) {
            if (checkServiceActivation(BinderService.class)) {
                Log.d(BINDER_TAG, "This session will utilize the pre-connected Binder service!");
            } else {
                Log.d(BINDER_TAG, "Substratum is now connecting to the Binder service...");
                startService(new Intent(getApplicationContext(),
                        BinderService.class));
            }
        }
    }

    private boolean checkServiceActivation(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}