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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.widget.TextView;

import java.io.File;

import projekt.substratum.R;
import projekt.substratum.common.References;

import static projekt.substratum.common.analytics.FirebaseAnalytics.NAMES_PREFS;
import static projekt.substratum.common.analytics.FirebaseAnalytics.PACKAGES_PREFS;
import static projekt.substratum.common.commands.FileOperations.delete;
import static projekt.substratum.common.commands.FileOperations.getFileSize;

public class ManageSpaceActivity extends AppCompatActivity {

    private TextView cacheCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_space);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> onBackPressed());

        CardView clearCacheButton = (CardView) findViewById(R.id.clear_cache_button);
        CardView resetAppButton = (CardView) findViewById(R.id.reset_app_button);
        cacheCounter = (TextView) findViewById(R.id.cache_counter);
        cacheCounter.setText(getString(R.string.clear_cache_button_loading));
        cacheCounter.setText(Formatter.formatFileSize(this, getFileSize(getCacheDir())));

        clearCacheButton.setOnClickListener(v -> {
            delete(this, getCacheDir().getAbsolutePath());
            cacheCounter.setText(getString(R.string.clear_cache_button_loading));
            cacheCounter.setText(Formatter.formatFileSize(this, getFileSize(getCacheDir())));
        });

        resetAppButton.setOnClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.manage_space_reset_dialog_title)
                    .setMessage(R.string.manage_space_reset_dialog_content)
                    .setNegativeButton(android.R.string.no, (dialog1, which) -> dialog1.dismiss())
                    .setPositiveButton(android.R.string.yes, (dialog12, which) -> {
                        dialog12.dismiss();
                        for (File f : getDataDir().listFiles()) {
                            if (!f.getName().equals("shared_prefs")) {
                                delete(this, f.getAbsolutePath());
                            } else {
                                for (File prefs : f.listFiles()) {
                                    String fileName = prefs.getName();
                                    if (!fileName.equals(NAMES_PREFS + ".xml") &&
                                            !fileName.equals(PACKAGES_PREFS + ".xml")) {
                                        delete(this, prefs.getAbsolutePath());
                                    }
                                }
                            }
                        }
                        References.loadDefaultConfig(this);
                        cacheCounter.setText(
                                Formatter.formatFileSize(this, getFileSize(getCacheDir())));
                    })
                    .create();
            dialog.show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cacheCounter.setText(getString(R.string.clear_cache_button_loading));
        cacheCounter.setText(Formatter.formatFileSize(this, getFileSize(getCacheDir())));
    }
}
