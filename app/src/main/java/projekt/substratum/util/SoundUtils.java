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

package projekt.substratum.util;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import projekt.substratum.R;
import projekt.substratum.config.FileOperations;
import projekt.substratum.config.References;
import projekt.substratum.config.SoundManager;
import projekt.substratum.config.ThemeManager;

import static projekt.substratum.config.References.INTERFACER_PACKAGE;

public class SoundUtils {

    public static FinishReceiver finishReceiver;

    private Context mContext;
    private String theme_pid;
    private boolean has_failed;
    private boolean ringtone = false;
    private SharedPreferences prefs;
    private View view;

    public void execute(View view, String arguments, Context context, String theme_pid) {
        this.mContext = context;
        this.theme_pid = theme_pid;
        this.view = view;

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        new SoundsHandlerAsync().execute(arguments);
    }

    public void SoundsClearer(Context context) {
        SoundManager.clearSounds(context);
    }

    private void finishFunction() {
        if (!has_failed) {
            Snackbar.make(view,
                    mContext.getString(R.string.sounds_dialog_apply_success),
                    Snackbar.LENGTH_LONG)
                    .show();
        } else {
            Snackbar.make(view,
                    mContext.getString(R.string.sounds_dialog_apply_failed),
                    Snackbar.LENGTH_LONG)
                    .show();
        }

        if (!References.checkThemeInterfacer(mContext)) {
            FileOperations.mountROData();
            FileOperations.mountRO();
        }

        if (ringtone) {
            ringtone = false;
            if (!References.checkThemeInterfacer(mContext) &&
                    !Settings.System.canWrite(mContext)) {
                new AlertDialog.Builder(mContext)
                        .setTitle(mContext.getString(R.string.sounds_dialog_permissions_title))
                        .setMessage(mContext.getString(R.string.sounds_dialog_permissions_text))
                        .setPositiveButton(R.string.sounds_dialog_permissions_grant,
                                (dialog, which) -> {
                                    if (!Settings.System.canWrite(mContext)) {
                                        Intent intent = new Intent(
                                                Settings
                                                        .ACTION_MANAGE_WRITE_SETTINGS);
                                        intent.setData(Uri.parse("package:" +
                                                mContext.getPackageName()));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        mContext.startActivity(intent);
                                    } else {
                                        Log.d(References.SUBSTRATUM_LOG,
                                                "Substratum was granted " +
                                                        "'android.permission" +
                                                        ".WRITE_SETTINGS' " +
                                                        "permissions for system " +
                                                        "runtime code " +
                                                        "execution.");
                                    }
                                })
                        .setNegativeButton(R.string.sounds_dialog_permissions_deny,
                                (dialog, which) -> dialog.dismiss())
                        .setIcon(mContext.getDrawable(R.drawable.sounds_dialog_alert))
                        .show();
            }
        }
    }

    private class SoundsHandlerAsync extends AsyncTask<String, Integer, String> {
        ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            // With masq 22+ dialog is started from receiver
            if (!References.checkThemeInterfacer(mContext)) {
                progress = new ProgressDialog(mContext, R.style.AppTheme_DialogAlert);
                progress.setMessage(mContext.getString(R.string.sounds_dialog_apply_text));
                progress.setIndeterminate(false);
                progress.setCancelable(false);
                progress.show();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (References.checkThemeInterfacer(mContext)) {
                if (finishReceiver == null) finishReceiver = new FinishReceiver();
                IntentFilter intentFilter = new IntentFilter(INTERFACER_PACKAGE + "" +
                        ".STATUS_CHANGED");
                mContext.registerReceiver(finishReceiver, intentFilter);
            } else {
                finishFunction();
                progress.dismiss();
                ThemeManager.restartSystemUI(mContext);
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {

            boolean[] results = SoundManager.setSounds(mContext, theme_pid, sUrl[0]);
            has_failed = results[0];
            ringtone = results[1];

            if (!has_failed) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("sounds_applied", theme_pid);
                editor.apply();
                Log.d("SoundUtils", "Sound pack installed!");
                FileOperations.delete(mContext, mContext.getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/");
            } else {
                Log.e("SoundUtils", "Sound installation aborted!");
                FileOperations.delete(mContext, mContext.getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/");
            }
            return null;
        }
    }

    class FinishReceiver extends BroadcastReceiver {
        ProgressDialog progress;

        public FinishReceiver() {
            progress = new ProgressDialog(mContext, R.style.AppTheme_DialogAlert);
            progress.setMessage(mContext.getString(R.string.sounds_dialog_apply_text));
            progress.setIndeterminate(false);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String PRIMARY_COMMAND_KEY = "primary_command_key";
            String COMMAND_VALUE_JOB_COMPLETE = "job_complete";
            String command = intent.getStringExtra(PRIMARY_COMMAND_KEY);

            if (command.equals(COMMAND_VALUE_JOB_COMPLETE)) {
                mContext.unregisterReceiver(finishReceiver);
                finishFunction();
                progress.dismiss();
            }
        }
    }
}
