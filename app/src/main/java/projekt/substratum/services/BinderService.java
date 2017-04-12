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

public class BinderService extends Service implements ServiceConnection {

    private static BinderService binderService;
    private IInterfacerInterface interfacerInterface;
    private boolean mBound;
    private ScheduledProfileReceiver scheduledProfileReceiver;

    public static BinderService getInstance() {
        return binderService;
    }

    public IInterfacerInterface getInterfacerInterface() {
        return interfacerInterface;
    }

    public void registerProfileScreenOffReceiver(Context context) {
        scheduledProfileReceiver = new ScheduledProfileReceiver();
        context.registerReceiver(scheduledProfileReceiver,
                new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    public void unregisterProfileScreenOffReceiver(Context context) {
        try {
            context.unregisterReceiver(scheduledProfileReceiver);
        } catch (Exception e) {
            // Don't mind it.
        }
    }

    public void bindInterfacer() {
        if (References.isBinderInterfacer(this) && !mBound) {
            Intent intent = new Intent(INTERFACER_PACKAGE + ".INITIALIZE");
            intent.setPackage(INTERFACER_PACKAGE);
            bindService(intent, this, Context.BIND_AUTO_CREATE);
        }
    }

    public void unbindInterfacer() {
        if (References.isBinderInterfacer(this) && mBound) {
            unbindService(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        binderService = this;
        bindInterfacer();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        interfacerInterface = IInterfacerInterface.Stub.asInterface(service);
        mBound = true;
        Log.d("BinderService", "Substratum has successfully binded with the Interfacer module.");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        interfacerInterface = null;
        mBound = false;
        Log.d("BinderService", "Substratum has successfully unbinded with the Interfacer module.");
    }
}
