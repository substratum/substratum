/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.activities.floatui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.services.floatui.SubstratumFloatInterface;
import projekt.substratum.services.tiles.FloatUiTile;

import static projekt.substratum.common.References.isServiceRunning;
import static projekt.substratum.common.Systems.checkUsagePermissions;

public class FloatUILaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Settings.canDrawOverlays(getApplicationContext()) &&
                checkUsagePermissions(getApplicationContext())) {
            if (!isServiceRunning(SubstratumFloatInterface.class, getApplicationContext())) {
                triggerFloatingHead(true);
            } else {
                triggerFloatingHead(false);
            }
        } else {
            Toast.makeText(this, getString(R.string.per_app_manual_grant),
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }

    /**
     * Trigger the floating head to show on the screen
     *
     * @param show True to show, false to hide
     */
    private void triggerFloatingHead(boolean show) {
        SharedPreferences prefs = Substratum.getPreferences();
        int active = (show) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        prefs.edit().putInt("float_tile", active).apply();
        FloatUiTile.requestListeningState(getApplicationContext(),
                new ComponentName(getApplicationContext(), FloatUiTile.class));
        if (show) {
            getApplicationContext().startService(new Intent(getApplicationContext(),
                    SubstratumFloatInterface.class));
        } else {
            stopService(new Intent(getApplicationContext(),
                    SubstratumFloatInterface.class));
        }
    }
}