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
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;
import projekt.substratum.util.helpers.ContextWrapper;

import static projekt.substratum.common.References.LOGCHAR_DIR;
import static projekt.substratum.common.analytics.FirebaseAnalytics.NAMES_PREFS;
import static projekt.substratum.common.analytics.FirebaseAnalytics.PACKAGES_PREFS;
import static projekt.substratum.common.commands.FileOperations.delete;
import static projekt.substratum.common.commands.FileOperations.getFileSize;

public class ManageSpaceActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.clear_cache_button)
    CardView clearCacheButton;
    @BindView(R.id.reset_app_button)
    CardView resetAppButton;
    @BindView(R.id.clear_logs_button)
    CardView clearLogsButton;
    @BindView(R.id.cache_counter)
    TextView cacheCounter;
    @BindView(R.id.log_counter)
    TextView logsCounter;
    private String callingPackage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_space);
        ButterKnife.bind(this);

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
     * Attach the base context for locale changes
     *
     * @param context Self explanatory, bud.
     */
    @Override
    protected void attachBaseContext(Context context) {
        Context newBase = context;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean languageCheck = prefs.getBoolean("force_english", false);
        if (languageCheck) {
            Locale newLocale = new Locale(Locale.ENGLISH.getLanguage());
            newBase = ContextWrapper.wrapNewLocale(context, newLocale);
        }
        super.attachBaseContext(newBase);
    }

    /**
     * Clear the cache of the application
     */
    private static class ClearCache extends AsyncTask<Void, Void, Void> {
        private WeakReference<ManageSpaceActivity> ref;

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
        private WeakReference<ManageSpaceActivity> ref;

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
        private WeakReference<ManageSpaceActivity> ref;

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
            }
        }
    }
}