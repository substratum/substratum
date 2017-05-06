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

package projekt.substratum.services.packages;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import java.util.List;

import projekt.substratum.common.platform.ThemeManager;

import static projekt.substratum.common.References.PACKAGE_ADDED;

public class OverlayUpdater extends BroadcastReceiver {

    private final static String TAG = "OverlayUpdater";
    private SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (PACKAGE_ADDED.equals(intent.getAction())) {
            Uri packageName = intent.getData();
            String package_name = packageName.toString().substring(8);

            // When the package is being updated, continue.
            Boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

            if (replacing) {
                List<String> installed_overlays = ThemeManager.listOverlaysForTarget(package_name);
                if (installed_overlays.size() > 0) {
                    Log.d(TAG, "'" + package_name +
                            "' was just updated with overlays present, updating...");
                    for (int i = 0; i < installed_overlays.size(); i++) {
                        Log.d(TAG, "Current overlay found in stash: " + installed_overlays.get(i));
                    }
                }
            }
        }
    }
}