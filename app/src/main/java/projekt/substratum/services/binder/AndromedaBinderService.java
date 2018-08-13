/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.services.binder;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import projekt.andromeda.IAndromedaInterface;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.platform.AndromedaService;

import java.util.ArrayList;
import java.util.Set;

import static projekt.substratum.common.References.ANDROMEDA_BINDED;
import static projekt.substratum.common.References.ANDROMEDA_PACKAGE;

public class AndromedaBinderService extends Service implements ServiceConnection {

    private static final String TAG = "AndromedaBinderService";
    private static IAndromedaInterface iAndromedaInterface;
    private boolean bound;

    public static IAndromedaInterface getAndromedaInterface() {
        return iAndromedaInterface;
    }

    private void bindAndromeda() {
        if (Systems.checkAndromeda(this) && !bound) {
            Intent intent = new Intent(ANDROMEDA_BINDED);
            intent.setPackage(ANDROMEDA_PACKAGE);
            try {
                if (!bindService(intent, this, Context.BIND_AUTO_CREATE)) {
                    stopSelf();
                }
            } catch (Exception ignored) {
                // Don't crash substratum on andromeda update
            }
        }
    }

    @Override
    public void onCreate() {
        bindAndromeda();

        new Thread(() -> {
            while (!bound) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (AndromedaService.checkServerActivity()) {
                boolean failed = false;
                while (!failed) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (!AndromedaService.checkServerActivity()) {
                        failed = true;
                    }
                }
            }
        }).start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        iAndromedaInterface = IAndromedaInterface.Stub.asInterface(service);
        bound = true;
        try {
            if ((iAndromedaInterface != null) && AndromedaService.checkServerActivity()) {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(
                        getApplicationContext(), References.ANDROMEDA_NOTIFICATION_CHANNEL_ID);
                builder.setContentTitle(getString(R.string.andromeda_notification_title))
                        .setContentText(getString(R.string.andromeda_notification_text))
                        .setSmallIcon(R.drawable.notification_icon);
                startForeground(2018, builder.build());

                SharedPreferences prefs = Substratum.getPreferences();
                Set<String> overlays = prefs.getStringSet("to_be_disabled_overlays", null);
                if (overlays != null) {
                    AndromedaService.disableOverlays(new ArrayList<>(overlays));
                    prefs.edit().remove("to_be_disabled_overlays").apply();
                }

                Substratum.log(TAG, "Substratum has successfully binded with the Andromeda module.");
            } else {
                stopSelf();
            }
        } catch (Exception e) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        iAndromedaInterface = null;
        bound = false;
        Substratum.log(TAG, "Substratum has successfully unbinded with the Andromeda module.");

    }

    @Override
    public void onBindingDied(ComponentName name) {
        onDestroy();
    }


    @Override
    public void onServiceDisconnected(ComponentName name) {
        onDestroy();
    }
}