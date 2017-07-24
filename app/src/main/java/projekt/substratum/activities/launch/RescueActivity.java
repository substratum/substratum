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

import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.activities.base.SubstratumActivity;
import projekt.substratum.common.platform.ThemeManager;

public class RescueActivity extends SubstratumActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createToast(getString(R.string.rescue_toast), Toast.LENGTH_LONG);
        Handler handler = new Handler();
        handler.postDelayed(() ->
                runOnUiThread(() -> {
                    List<String> android = ThemeManager.listEnabledOverlaysForTarget(
                            getApplicationContext(), "android");
                    List<String> substratum = ThemeManager.listEnabledOverlaysForTarget(
                            getApplicationContext(), "projekt.substratum");
                    List<String> systemui = ThemeManager.listEnabledOverlaysForTarget(
                            getApplicationContext(), "com.android.systemui");
                    ArrayList<String> to_be_disabled = new ArrayList<>(android);
                    to_be_disabled.addAll(substratum);
                    to_be_disabled.addAll(systemui);
                    ThemeManager.disableOverlay(getApplicationContext(), to_be_disabled);
                }), 500);
        finish();
    }
}