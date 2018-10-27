/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.activities.shortcuts;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;

public class OverlayManagerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(
                References.createLauncherIcon(Substratum.getInstance(),
                        null,
                        getString(R.string.app_name),
                        true));
        finish();
    }
}