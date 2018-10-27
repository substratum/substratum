/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.activities.shortcuts;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.SamsungOverlayCacher;
import projekt.substratum.common.platform.ThemeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static projekt.substratum.common.References.SUBSTRATUM_PACKAGE;
import static projekt.substratum.common.Resources.FRAMEWORK;

public class RescueActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Systems.isSamsungDevice(Substratum.getInstance())) {
            if (Systems.isNewSamsungDeviceAndromeda(Substratum.getInstance()) ||
                    Systems.isNewSamsungDevice()) {
                SamsungOverlayCacher samsungOverlayCacher =
                        new SamsungOverlayCacher(Substratum.getInstance());
                List<String> listOfOverlays = samsungOverlayCacher.getOverlays(false);
                ArrayList<String> toUninstall = listOfOverlays.stream()
                        .filter(t -> t.startsWith(FRAMEWORK) || t.startsWith(SUBSTRATUM_PACKAGE))
                        .collect(Collectors.toCollection(ArrayList::new));
                ThemeManager.uninstallOverlay(Substratum.getInstance(), toUninstall);
            } else {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS));
            }
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