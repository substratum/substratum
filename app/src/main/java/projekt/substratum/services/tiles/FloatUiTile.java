/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.services.tiles;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import projekt.substratum.Substratum;
import projekt.substratum.activities.floatui.FloatUILaunchActivity;

public class FloatUiTile extends TileService {

    @Override
    public void onTileAdded() {
        switchState();
        super.onTileAdded();
    }

    @Override
    public void onStartListening() {
        switchState();
        super.onStartListening();
    }

    @Override
    public void onClick() {
        Intent intent = new Intent(this, FloatUILaunchActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapse(intent);
    }

    private void switchState() {
        int state = Substratum.getPreferences().getInt("float_tile", Tile.STATE_INACTIVE);
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(state);
            tile.updateTile();
        }
    }
}