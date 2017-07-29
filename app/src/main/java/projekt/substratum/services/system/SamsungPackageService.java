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

package projekt.substratum.services.system;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import projekt.substratum.common.References;

public class SamsungPackageService extends Service {

    private int initialPackageCount = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(this.getClass().getSimpleName(),
                "The overlay package refresher for Samsung devices has been fully loaded.");
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> currentApps = pm.getInstalledApplications(0);
        initialPackageCount = currentApps.size();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<ApplicationInfo> currentApps = pm.getInstalledApplications(0);
                if (initialPackageCount < currentApps.size()) {
                    initialPackageCount = currentApps.size();
                    References.sendOverlayRefreshMessage(getApplicationContext());
                }
            }
        }, 0, 1000);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(this.getClass().getSimpleName(),
                "The overlay package refresher for Samsung devices has been fully unloaded.");
        super.onDestroy();
    }
}
