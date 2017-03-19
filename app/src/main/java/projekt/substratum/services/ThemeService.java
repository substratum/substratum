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
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import projekt.substratum.util.AntiPiracyCheck;

public class ThemeService extends Service {

    private static Runnable runnable = null;
    private Context context = this;
    private Handler handler = null;

    private int CONFIG_TIME_PIRACY_CHECKER = 60000; // 1 sec == 1000ms

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        handler = new Handler();
        runnable = () -> {
            new AntiPiracyCheck().execute(context);
            handler.postDelayed(runnable, CONFIG_TIME_PIRACY_CHECKER);
        };
        handler.postDelayed(runnable, CONFIG_TIME_PIRACY_CHECKER);
    }
}