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

package projekt.substratum.activities.floatui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import projekt.substratum.R;
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
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