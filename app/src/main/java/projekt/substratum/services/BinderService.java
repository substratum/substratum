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

package projekt.substratum.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import projekt.substratum.IInterfacerInterface;
import projekt.substratum.config.References;

import static projekt.substratum.config.References.INTERFACER_PACKAGE;

public class BinderService extends Service {

    private static BinderService binderService;
    private static IInterfacerInterface interfacerInterface;
    private static ServiceConnection serviceConnection;
    private static boolean mBound;
    private static ScheduledProfileReceiver scheduledProfileReceiver;

    public static BinderService getInstance() {
        return binderService;
    }

    public static IInterfacerInterface getInterfacerInterface() {
        return interfacerInterface;
    }

    public static void registerProfileScreenOffReceiver(Context context) {
        scheduledProfileReceiver = new ScheduledProfileReceiver();
        context.registerReceiver(scheduledProfileReceiver,
                new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    public static void unregisterProfileScreenOffReceiver(Context context) {
        try {
            context.unregisterReceiver(scheduledProfileReceiver);
        } catch (Exception e) {
            // Don't mind it.
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        binderService = this;
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                interfacerInterface = IInterfacerInterface.Stub.asInterface(service);
                mBound = true;
                Log.d("BinderService",
                        "Substratum has successfully binded with the Interfacer module.");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                interfacerInterface = null;
                mBound = false;
                Log.d("BinderService",
                        "Substratum has successfully unbinded with the Interfacer module.");
            }
        };
        bindInterfacer();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("BinderService", "The service has successfully binded to the system.");
        return null;
    }

    public void bindInterfacer() {
        if (References.isBinderfacer(this) && !mBound) {
            Intent intent = new Intent(INTERFACER_PACKAGE + ".INITIALIZE");
            intent.setPackage(INTERFACER_PACKAGE);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void unbindInterfacer() {
        if (References.isBinderfacer(this) && mBound) {
            unbindService(serviceConnection);
        }
    }
}
