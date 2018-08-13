/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.activities.shortcuts;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import projekt.substratum.Substratum;
import projekt.substratum.common.Theming;

import static projekt.substratum.common.Internal.THEME_PID;

public class AppShortcutLaunch extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Theming.launchTheme(
                Substratum.getInstance(),
                getIntent().getStringExtra(THEME_PID)
        );
        finish();
    }
}