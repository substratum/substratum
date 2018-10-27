/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.activities.launch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.stephentuso.welcome.WelcomeHelper;
import projekt.substratum.MainActivity;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;
import projekt.substratum.util.views.AppIntro;

import static projekt.substratum.common.analytics.PackageAnalytics.isLowEnd;

public class AppIntroActivity extends AppCompatActivity {

    private SharedPreferences prefs = Substratum.getPreferences();
    private WelcomeHelper welcomeScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (prefs.getBoolean("first_run", true) && !isLowEnd()) {
            welcomeScreen = new WelcomeHelper(this, AppIntro.class);
            welcomeScreen.show(savedInstanceState);
        } else if (prefs.getBoolean("first_run", true) && isLowEnd()) {
            prefs.edit().putBoolean("first_run", false).apply();
            References.loadDefaultConfig(Substratum.getInstance());
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == WelcomeHelper.DEFAULT_WELCOME_SCREEN_REQUEST) &&
                (resultCode == RESULT_OK)) {
            prefs.edit().putBoolean("first_run", false).apply();
            References.loadDefaultConfig(Substratum.getInstance());
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        welcomeScreen.onSaveInstanceState(outState);
    }
}