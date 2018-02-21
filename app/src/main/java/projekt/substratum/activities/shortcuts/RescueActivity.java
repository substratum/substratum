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

package projekt.substratum.activities.shortcuts;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.Systems;
import projekt.substratum.common.platform.ThemeManager;

import static projekt.substratum.common.References.SUBSTRATUM_PACKAGE;
import static projekt.substratum.common.Resources.FRAMEWORK;

public class RescueActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Systems.isSamsungDevice(Substratum.getInstance())) {
            startActivity(new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS));
        } else {
            Toast.makeText(this, getString(R.string.rescue_toast), Toast.LENGTH_LONG).show();
            new Handler().postDelayed(() -> {
                List<String> frameworkOverlays = ThemeManager.listEnabledOverlaysForTarget(
                        Substratum.getInstance(),
                        FRAMEWORK);
                List<String> substratumOverlays = ThemeManager.listEnabledOverlaysForTarget(
                        Substratum.getInstance(),
                        SUBSTRATUM_PACKAGE);

                ArrayList<String> toBeDisabled = new ArrayList<>(frameworkOverlays);
                toBeDisabled.addAll(substratumOverlays);
                ThemeManager.disableOverlay(
                        Substratum.getInstance(),
                        toBeDisabled);
            }, 500L);
        }
        finish();
    }
}