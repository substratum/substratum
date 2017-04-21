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

package projekt.substratum.activities.launch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import java.io.File;

import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.activities.base.SubstratumActivity;
import projekt.substratum.common.References;

public class AppShortcutLaunch extends SubstratumActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent currentIntent = getIntent();
        String theme_pid = currentIntent.getStringExtra("theme_pid");

        if (!References.isCachingEnabled(getApplicationContext())) {
            References.launchTheme(this, theme_pid, null, false);
        } else {
            SharedPreferences prefs = this.getSharedPreferences(
                    "substratum_state", Context.MODE_PRIVATE);
            if (!prefs.contains("is_updating"))
                prefs.edit().putBoolean("is_updating", false).apply();
            if (!prefs.getBoolean("is_updating", true)) {
                // Process fail case if user uninstalls an app and goes back an activity
                if (References.isPackageInstalled(this, theme_pid)) {
                    File checkSubstratumVerity = new File(
                            getBuildDirPath() + theme_pid, "substratum.xml");

                    if (checkSubstratumVerity.exists()) {
                        References.launchTheme(this, theme_pid, null, false);
                    } else {
                        createToast(getString(R.string.toast_needs_caching), Toast.LENGTH_LONG);
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);
                    }
                } else {
                    createToast(getString(R.string.toast_uninstalled), Toast.LENGTH_LONG);
                }
            }
        }
        finish();
    }
}