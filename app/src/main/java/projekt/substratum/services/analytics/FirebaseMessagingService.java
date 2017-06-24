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

package projekt.substratum.services.analytics;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.RemoteMessage;

import projekt.substratum.R;
import projekt.substratum.common.References;

public class FirebaseMessagingService extends
        com.google.firebase.messaging.FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Intent showIntent = new Intent();
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, showIntent, 0);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, References.DEFAULT_NOTIFICATION_CHANNEL_ID)
                        .setContentIntent(contentIntent)
                        .setAutoCancel(true)
                        .setSmallIcon(R.mipmap.main_launcher)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(remoteMessage.getNotification().getBody());
        Notification notification = mBuilder.build();
        notificationManager.notify(References.firebase_notification_id, notification);
    }
}