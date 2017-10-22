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

package projekt.substratum.services.binder;

import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import projekt.andromeda.IAndromedaInterface;
import projekt.substratum.R;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.platform.AndromedaService;

import static projekt.substratum.common.References.ANDROMEDA_BINDED;
import static projekt.substratum.common.References.ANDROMEDA_PACKAGE;

public class AndromedaBinderService extends Service implements ServiceConnection {

    private static final String TAG = "AndromedaBinderService";
    private static IAndromedaInterface iAndromedaInterface;
    private boolean mBound;

    public static IAndromedaInterface getAndromedaInterface() {
        return iAndromedaInterface;
    }

    public void bindAndromeda() {
        if (Systems.checkAndromeda(this) && !this.mBound) {
            final Intent intent = new Intent(ANDROMEDA_BINDED);
            intent.setPackage(ANDROMEDA_PACKAGE);
            if (!this.bindService(intent, this, Context.BIND_AUTO_CREATE)) {
                this.stopSelf();
            }
        }
    }

    @Override
    public void onCreate() {
        this.bindAndromeda();

        new Thread(() -> {
            while (!this.mBound) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (AndromedaService.checkServerActivity()) {
                boolean failed = false;
                while (!failed) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (!AndromedaService.checkServerActivity()) {
                        this.sendBadNotification(this.getApplicationContext());
                        failed = true;
                    }
                }
            } else {
                this.sendBadNotification(this.getApplicationContext());
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        iAndromedaInterface = null;
    }

    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        iAndromedaInterface = IAndromedaInterface.Stub.asInterface(service);
        this.mBound = true;
        Log.d(TAG, "Substratum has successfully binded with the Andromeda module.");
        if (iAndromedaInterface != null && AndromedaService.checkServerActivity()) {
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(
                    this.getApplicationContext(), References.ANDROMEDA_NOTIFICATION_CHANNEL_ID);
            builder.setContentTitle(this.getString(R.string.andromeda_notification_title))
                    .setContentText(this.getString(R.string.andromeda_notification_text))
                    .setSmallIcon(R.drawable.notification_icon);
            this.startForeground(2018, builder.build());
        } else {
            this.stopSelf();
        }
    }

    @Override
    public void onServiceDisconnected(final ComponentName name) {
        iAndromedaInterface = null;
        this.mBound = false;
        Log.d(TAG, "Substratum has successfully unbinded with the Andromeda module.");
    }

    public void sendBadNotification(final Context context) {
        final NotificationManager mNotifyMgr =
                (NotificationManager)
                        this.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        boolean isBadNotificationShowing = false;
        final int badNotificationId = 2017;
        if (mNotifyMgr != null) {
            final StatusBarNotification[] notifications;
            notifications = mNotifyMgr.getActiveNotifications();
            for (final StatusBarNotification notification : notifications) {
                if (notification.getId() == badNotificationId) {
                    isBadNotificationShowing = true;
                }
            }
        }
        if (mNotifyMgr != null && !isBadNotificationShowing) {
            final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder
                    (this.getApplicationContext(), References.ANDROMEDA_NOTIFICATION_CHANNEL_ID);
            mBuilder.setContentTitle(
                    this.getApplicationContext().getString(
                            R.string.andromeda_notification_title_negation));
            mBuilder.setContentText(
                    this.getApplicationContext().getString(
                            R.string.andromeda_notification_text_negation));
            mBuilder.setOngoing(false);
            mBuilder.setSmallIcon(R.drawable.notification_warning_icon);
            mNotifyMgr.notify(badNotificationId, mBuilder.build());
        }

        Broadcasts.sendAndromedaRefreshMessage(context);
        this.stopSelf();
    }
}