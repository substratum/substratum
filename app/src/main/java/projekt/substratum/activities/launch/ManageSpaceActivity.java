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
import android.databinding.DataBindingUtil;
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
import projekt.substratum.Substratum;
import projekt.substratum.common.References;
import projekt.substratum.databinding.ManageSpaceActivityBinding;
import projekt.substratum.util.helpers.LocaleHelper;

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
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ManageSpaceActivityBinding activityManageSpaceBinding =
                DataBindingUtil.setContentView(this, R.layout.manage_space_activity);
        Toolbar toolbar = activityManageSpaceBinding.toolbar;
        CardView clearCacheButton = activityManageSpaceBinding.clearCacheButton;
        CardView resetAppButton = activityManageSpaceBinding.resetAppButton;
        CardView clearLogsButton = activityManageSpaceBinding.clearLogsButton;
        cacheCounter = activityManageSpaceBinding.cacheCounter;
        logsCounter = activityManageSpaceBinding.logCounter;

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        cacheCounter.setText(getString(R.string.clear_cache_button_loading));
        cacheCounter.setText(Formatter.formatFileSize(this, getFileSize(getCacheDir())));
        clearCacheButton.setOnClickListener(v -> {
            cacheCounter.setText(getString(R.string.clear_cache_button_loading));
            new ClearCache(this).execute();
        });

        logsCounter.setText(getString(R.string.clear_cache_button_loading));
        File filer = new File(LOGCHAR_DIR);
        if (filer.isDirectory()) {
            logsCounter.setText(String.valueOf((filer.list() != null ? filer.list().length : 0)));
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

    /**
     * Clear the cache of the application
     */
    private static class ClearCache extends AsyncTask<Void, Void, Void> {
        private final WeakReference<ManageSpaceActivity> ref;

        ClearCache(ManageSpaceActivity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Context context = Substratum.getInstance();
            delete(context, context.getCacheDir().getAbsolutePath());
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ManageSpaceActivity activity = ref.get();
            if (activity != null) {
                Context context = Substratum.getInstance();
                activity.cacheCounter.setText(
                        Formatter.formatFileSize(context, getFileSize(context.getCacheDir())));
            }
        }
    }

    /**
     * Clear the saved LogChars
     */
    private static class ClearLogs extends AsyncTask<Void, Void, Void> {
        private final WeakReference<ManageSpaceActivity> ref;

        ClearLogs(ManageSpaceActivity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... params) {
            ManageSpaceActivity activity = ref.get();
            if (activity != null) {
                Context context = Substratum.getInstance();
                delete(context, new File(LOGCHAR_DIR).getAbsolutePath());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ManageSpaceActivity activity = ref.get();
            if (activity != null) {
                File filer = new File(LOGCHAR_DIR);
                if (filer.isDirectory()) {
                    activity.logsCounter.setText(String.valueOf(filer.list().length));
                } else {
                    activity.logsCounter.setText(String.valueOf(0));
                }
            }
        }
    }

    /**
     * Reset the application to the near-out-of-box experience
     */
    private static class ResetApp extends AsyncTask<Void, Void, Void> {
        private final WeakReference<ManageSpaceActivity> ref;

        ResetApp(ManageSpaceActivity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Context context = Substratum.getInstance();
            for (File f : context.getDataDir().listFiles()) {
                if (!"shared_prefs".equals(f.getName())) {
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
            if (activity != null) {
                Context context = Substratum.getInstance();
                activity.cacheCounter.setText(
                        Formatter.formatFileSize(context, getFileSize(context.getCacheDir())));
                activity.finishAffinity();
                Substratum.restartSubstratum(context);
            }
        }
    }
}