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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;

import projekt.substratum.R;
import projekt.substratum.common.References;

import static projekt.substratum.common.analytics.FirebaseAnalytics.NAMES_PREFS;
import static projekt.substratum.common.analytics.FirebaseAnalytics.PACKAGES_PREFS;
import static projekt.substratum.common.commands.FileOperations.delete;
import static projekt.substratum.common.commands.FileOperations.getFileSize;

public class ManageSpaceActivity extends AppCompatActivity {

    private TextView cacheCounter;
    private TextView logsCounter;
    private String callingPackage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_space);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> onBackPressed());

        CardView clearCacheButton = findViewById(R.id.clear_cache_button);
        CardView resetAppButton = findViewById(R.id.reset_app_button);
        cacheCounter = findViewById(R.id.cache_counter);
        cacheCounter.setText(getString(R.string.clear_cache_button_loading));
        cacheCounter.setText(Formatter.formatFileSize(this, getFileSize(getCacheDir())));

        clearCacheButton.setOnClickListener(v -> {
            cacheCounter.setText(getString(R.string.clear_cache_button_loading));
            new ClearCache(this).execute();
        });

        CardView clearLogsButton = findViewById(R.id.clear_logs_button);
        logsCounter = findViewById(R.id.log_counter);
        logsCounter.setText(getString(R.string.clear_cache_button_loading));
        File filer = new File(Environment.getExternalStorageDirectory() +
                File.separator + "substratum" + File.separator + "LogChar Reports");
        if (filer.isDirectory()) {
            logsCounter.setText(String.valueOf(filer.list().length));
        } else {
            logsCounter.setText(String.valueOf(0));
        }

        clearLogsButton.setOnClickListener(v -> {
            logsCounter.setText(getString(R.string.clear_cache_button_loading));
            new ClearLogs(this).execute();
        });

        resetAppButton.setOnClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.manage_space_reset_dialog_title)
                    .setMessage(R.string.manage_space_reset_dialog_content)
                    .setNegativeButton(android.R.string.no, (dialog1, which) -> dialog1.dismiss())
                    .setPositiveButton(android.R.string.yes, (dialog12, which) -> {
                        dialog12.dismiss();
                        new ResetApp(this).execute();
                    })
                    .create();
            dialog.show();
        });
        callingPackage = getCallingPackage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cacheCounter.setText(getString(R.string.clear_cache_button_loading));
        cacheCounter.setText(Formatter.formatFileSize(this, getFileSize(getCacheDir())));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (callingPackage != null) {
            Process.killProcess(Process.myPid());
        }
    }

    private static class ClearCache extends AsyncTask<Void, Void, Void> {
        private WeakReference<ManageSpaceActivity> ref;

        ClearCache(ManageSpaceActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Context context = ref.get().getApplicationContext();
            delete(context, context.getCacheDir().getAbsolutePath());
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ManageSpaceActivity activity = ref.get();
            Context context = activity.getApplicationContext();
            activity.cacheCounter.setText(
                    Formatter.formatFileSize(context, getFileSize(context.getCacheDir())));
        }
    }

    private static class ClearLogs extends AsyncTask<Void, Void, Void> {
        private WeakReference<ManageSpaceActivity> ref;

        ClearLogs(ManageSpaceActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Context context = ref.get().getApplicationContext();
            delete(context, new File(Environment.getExternalStorageDirectory() +
                    File.separator + "substratum" + File.separator + "LogChar Reports")
                    .getAbsolutePath());
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ManageSpaceActivity activity = ref.get();

            File filer = new File(Environment.getExternalStorageDirectory() +
                    File.separator + "substratum" + File.separator + "LogChar Reports");
            if (filer.isDirectory()) {
                activity.logsCounter.setText(String.valueOf(filer.list().length));
            } else {
                activity.logsCounter.setText(String.valueOf(0));
            }
        }
    }

    private static class ResetApp extends AsyncTask<Void, Void, Void> {
        private WeakReference<ManageSpaceActivity> ref;

        ResetApp(ManageSpaceActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... params) {
            ManageSpaceActivity activity = ref.get();
            Context context = activity.getApplicationContext();

            for (File f : context.getDataDir().listFiles()) {
                if (!f.getName().equals("shared_prefs")) {
                    delete(context, f.getAbsolutePath());
                } else {
                    for (File prefs : f.listFiles()) {
                        String fileName = prefs.getName();
                        if (!fileName.equals(NAMES_PREFS + ".xml") &&
                                !fileName.equals(PACKAGES_PREFS + ".xml")) {
                            delete(context, prefs.getAbsolutePath());
                        }
                    }
                }
            }
            References.loadDefaultConfig(context);
            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
            ManageSpaceActivity activity = ref.get();
            Context context = activity.getApplicationContext();
            activity.cacheCounter.setText(
                    Formatter.formatFileSize(context, getFileSize(context.getCacheDir())));
            activity.finishAffinity();
        }
    }
}
