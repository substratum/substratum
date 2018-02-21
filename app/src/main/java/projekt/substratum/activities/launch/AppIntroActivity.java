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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.stephentuso.welcome.WelcomeHelper;

import projekt.substratum.MainActivity;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;
import projekt.substratum.util.views.AppIntro;

import static projekt.substratum.common.analytics.PackageAnalytics.isLowEnd;

public class AppIntroActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private WelcomeHelper welcomeScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(Substratum.getInstance());
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