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

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import projekt.substratum.config.References;

import static projekt.substratum.config.References.INTERFACER_PACKAGE;

public class Substratum extends Application {
    private static Substratum substratum;
    private IInterfacerInterface interfacerInterface;
    private boolean mBound;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            interfacerInterface = IInterfacerInterface.Stub.asInterface(service);
            mBound = true;
            Log.d("JobService", "service binded");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            interfacerInterface = null;
            mBound = false;
            Log.d("JobService", "service unbinded");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        substratum = this;
        bindInterfacer();
    }

    public static Substratum getInstance() {
        return substratum;
    }

    public IInterfacerInterface getInterfacerInterface() {
        return interfacerInterface;
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
