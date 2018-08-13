/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import projekt.substratum.activities.launch.SplashScreenActivity;

public class LauncherActivity extends AppCompatActivity {

    /**
     * The main launcher activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, SplashScreenActivity.class);
        intent.putExtra("first_run", Substratum.getPreferences().getBoolean("first_run", true));
        this.startActivity(intent);
        this.finish();
    }
}