/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.services.tiles;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;
import projekt.substratum.LaunchActivity;
import projekt.substratum.LauncherActivity;

@TargetApi(Build.VERSION_CODES.N)
public class SubstratumTile extends TileService {

    @Override
    public void onClick() {
        try {
            Intent intent = new Intent(this, LauncherActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(intent);
        } catch (ActivityNotFoundException ignored) {
            // At this point, the app is most likely hidden and set to only open from Settings
            Intent intent = new Intent(this, LaunchActivity.class);
            startActivityAndCollapse(intent);
        }
    }
}