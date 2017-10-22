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

import static projekt.substratum.common.References.LOGCHAR_DIR;
import static projekt.substratum.common.analytics.FirebaseAnalytics.NAMES_PREFS;
import static projekt.substratum.common.analytics.FirebaseAnalytics.PACKAGES_PREFS;
import static projekt.substratum.common.commands.FileOperations.delete;
import static projekt.substratum.common.commands.FileOperations.getFileSize;

public class ManageSpaceActivity extends AppCompatActivity {

    private TextView cacheCounter;
    private TextView logsCounter;
    private String callingPackage;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_manage_space);
        final Toolbar toolbar = this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);
        if (this.getSupportActionBar() != null) {
            this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            this.getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> this.onBackPressed());

        final CardView clearCacheButton = this.findViewById(R.id.clear_cache_button);
        final CardView resetAppButton = this.findViewById(R.id.reset_app_button);
        this.cacheCounter = this.findViewById(R.id.cache_counter);
        this.cacheCounter.setText(this.getString(R.string.clear_cache_button_loading));
        this.cacheCounter.setText(Formatter.formatFileSize(this, getFileSize(this.getCacheDir())));

        clearCacheButton.setOnClickListener(v -> {
            this.cacheCounter.setText(this.getString(R.string.clear_cache_button_loading));
            new ClearCache(this).execute();
        });

        final CardView clearLogsButton = this.findViewById(R.id.clear_logs_button);
        this.logsCounter = this.findViewById(R.id.log_counter);
        this.logsCounter.setText(this.getString(R.string.clear_cache_button_loading));
        final File filer = new File(LOGCHAR_DIR);
        if (filer.isDirectory()) {
            this.logsCounter.setText(String.valueOf(filer.list().length));
        } else {
            this.logsCounter.setText(String.valueOf(0));
        }

        clearLogsButton.setOnClickListener(v -> {
            this.logsCounter.setText(this.getString(R.string.clear_cache_button_loading));
            new ClearLogs(this).execute();
        });

        resetAppButton.setOnClickListener(v -> {
            final AlertDialog dialog = new AlertDialog.Builder(this)
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
        this.callingPackage = this.getCallingPackage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.cacheCounter.setText(this.getString(R.string.clear_cache_button_loading));
        this.cacheCounter.setText(Formatter.formatFileSize(this, getFileSize(this.getCacheDir())));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.callingPackage != null) {
            Process.killProcess(Process.myPid());
        }
    }

    private static class ClearCache extends AsyncTask<Void, Void, Void> {
        private final WeakReference<ManageSpaceActivity> ref;

        ClearCache(final ManageSpaceActivity activity) {
            super();
            this.ref = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(final Void... params) {
            final ManageSpaceActivity activity = this.ref.get();
            if (activity != null) {
                final Context context = activity.getApplicationContext();
                delete(context, context.getCacheDir().getAbsolutePath());
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            final ManageSpaceActivity activity = this.ref.get();
            if (activity != null) {
                final Context context = activity.getApplicationContext();
                activity.cacheCounter.setText(
                        Formatter.formatFileSize(context, getFileSize(context.getCacheDir())));
            }
        }
    }

    private static class ClearLogs extends AsyncTask<Void, Void, Void> {
        private final WeakReference<ManageSpaceActivity> ref;

        ClearLogs(final ManageSpaceActivity activity) {
            super();
            this.ref = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(final Void... params) {
            final ManageSpaceActivity activity = this.ref.get();
            if (activity != null) {
                final Context context = this.ref.get().getApplicationContext();
                delete(context, new File(LOGCHAR_DIR).getAbsolutePath());
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            final ManageSpaceActivity activity = this.ref.get();
            if (activity != null) {
                final File filer = new File(LOGCHAR_DIR);
                if (filer.isDirectory()) {
                    activity.logsCounter.setText(String.valueOf(filer.list().length));
                } else {
                    activity.logsCounter.setText(String.valueOf(0));
                }
            }
        }
    }

    private static class ResetApp extends AsyncTask<Void, Void, Void> {
        private final WeakReference<ManageSpaceActivity> ref;

        ResetApp(final ManageSpaceActivity activity) {
            super();
            this.ref = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(final Void... params) {
            final ManageSpaceActivity activity = this.ref.get();
            if (activity != null) {
                final Context context = activity.getApplicationContext();
                for (final File f : context.getDataDir().listFiles()) {
                    if (!"shared_prefs".equals(f.getName())) {
                        delete(context, f.getAbsolutePath());
                    } else {
                        for (final File prefs : f.listFiles()) {
                            final String fileName = prefs.getName();
                            if (!fileName.equals(NAMES_PREFS + ".xml") &&
                                    !fileName.equals(PACKAGES_PREFS + ".xml")) {
                                delete(context, prefs.getAbsolutePath());
                            }
                        }
                    }
                }
                References.loadDefaultConfig(context);
            }
            return null;
        }


        @Override
        protected void onPostExecute(final Void result) {
            final ManageSpaceActivity activity = this.ref.get();
            if (activity != null) {
                final Context context = activity.getApplicationContext();
                activity.cacheCounter.setText(
                        Formatter.formatFileSize(context, getFileSize(context.getCacheDir())));
                activity.finishAffinity();
            }
        }
    }
}