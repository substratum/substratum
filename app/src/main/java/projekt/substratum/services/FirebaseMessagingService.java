package projekt.substratum.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.RemoteMessage;

import projekt.substratum.R;
import projekt.substratum.config.References;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class FirebaseMessagingService extends
        com.google.firebase.messaging.FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Intent showIntent = new Intent();
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, showIntent, 0);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setContentIntent(contentIntent)
                        .setAutoCancel(true)
                        .setSmallIcon(R.mipmap.main_launcher)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(remoteMessage.getNotification().getBody());
        Notification notification = mBuilder.build();
        notificationManager.notify(References.firebase_notification_id, notification);
    }
}