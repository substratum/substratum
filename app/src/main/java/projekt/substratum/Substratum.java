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
import android.content.Context;
import android.content.Intent;
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
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}