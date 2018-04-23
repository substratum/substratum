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

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Set;

import projekt.andromeda.IAndromedaInterface;
import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.platform.AndromedaService;

import static android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences;
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

                SharedPreferences prefs = getDefaultSharedPreferences(getApplicationContext());
                Set<String> overlays = prefs.getStringSet("to_be_disabled_overlays", null);
                if (overlays != null) {
                    AndromedaService.disableOverlays(new ArrayList<>(overlays));
                    prefs.edit().remove("to_be_disabled_overlays").apply();
                }

                Log.d(TAG, "Substratum has successfully binded with the Andromeda module.");
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
        Log.d(TAG, "Substratum has successfully unbinded with the Andromeda module.");

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