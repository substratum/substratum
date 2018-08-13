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
import android.os.IBinder;
import androidx.annotation.Nullable;
import projekt.substratum.IInterfacerInterface;
import projekt.substratum.Substratum;
import projekt.substratum.common.Systems;

import static projekt.substratum.common.References.INTERFACER_BINDED;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;

public class InterfacerBinderService extends Service implements ServiceConnection {

    private static final String TAG = "InterfacerBinderService";
    private static InterfacerBinderService binderService;
    private IInterfacerInterface interfacerInterface;
    private boolean bound;

    public static InterfacerBinderService getInstance() {
        return binderService;
    }

    public IInterfacerInterface getInterfacerInterface() {
        return interfacerInterface;
    }

    private void bindInterfacer() {
        if (Systems.isBinderInterfacer(this) && !bound) {
            Intent intent = new Intent(INTERFACER_BINDED);
            intent.setPackage(INTERFACER_PACKAGE);
            bindService(intent, this, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        binderService = this;
        bindInterfacer();
    }

    @Override
    public void onDestroy() {
        interfacerInterface = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        interfacerInterface = IInterfacerInterface.Stub.asInterface(service);
        bound = true;
        Substratum.log(TAG, "Substratum has successfully binded with the Interfacer module.");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        interfacerInterface = null;
        bound = false;
        Substratum.log(TAG, "Substratum has successfully unbinded with the Interfacer module.");
    }
}