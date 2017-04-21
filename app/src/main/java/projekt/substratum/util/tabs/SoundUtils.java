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

package projekt.substratum.util.tabs;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.design.widget.Lunchbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import java.lang.ref.WeakReference;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.tabs.SoundManager;

import static projekt.substratum.common.References.STATUS_CHANGED;

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
        new SoundsHandlerAsync(this).execute(arguments);
    }

    public void SoundsClearer(Context context) {
        SoundManager.clearSounds(context);
    }

    private void finishFunction() {
        if (!has_failed) {
            Lunchbar.make(view,
                    mContext.getString(R.string.sounds_dialog_apply_success),
                    Lunchbar.LENGTH_LONG)
                    .show();
        } else {
            Lunchbar.make(view,
                    mContext.getString(R.string.sounds_dialog_apply_failed),
                    Lunchbar.LENGTH_LONG)
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
                                                Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                        intent.setData(Uri.parse("package:" +
                                                mContext.getPackageName()));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        mContext.startActivity(intent);
                                    } else {
                                        Log.d(References.SUBSTRATUM_LOG,
                                                "Substratum was granted " +
                                                        "'android.permission.WRITE_SETTINGS' " +
                                                        "permissions for system runtime code " +
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

    private static class SoundsHandlerAsync extends AsyncTask<String, Integer, String> {
        private ProgressDialog progress;
        private WeakReference<SoundUtils> ref;

        private SoundsHandlerAsync(SoundUtils soundUtils) {
            ref = new WeakReference<>(soundUtils);
        }

        @Override
        protected void onPreExecute() {
            SoundUtils soundUtils = ref.get();
            Context context = soundUtils.mContext;

            // With masq 22+, dialog is started from receiver
            if (References.checkThemeInterfacer(context)) {
                progress = new ProgressDialog(context, R.style.AppTheme_DialogAlert);
                progress.setMessage(context.getString(R.string.sounds_dialog_apply_text));
                progress.setIndeterminate(false);
                progress.setCancelable(false);
                progress.show();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            SoundUtils soundUtils = ref.get();
            Context context = soundUtils.mContext;

            if (References.checkThemeInterfacer(context) &&
                    !References.isBinderInterfacer(context)) {
                if (finishReceiver == null) {
                    finishReceiver = new FinishReceiver(soundUtils, progress);
                }
                IntentFilter intentFilter = new IntentFilter(STATUS_CHANGED);
                context.getApplicationContext().registerReceiver(finishReceiver, intentFilter);
            } else {
                soundUtils.finishFunction();
                progress.dismiss();
                ThemeManager.restartSystemUI(context);
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            SoundUtils soundUtils = ref.get();
            Context context = soundUtils.mContext;
            boolean[] results = SoundManager.setSounds(context, soundUtils.theme_pid, sUrl[0]);
            soundUtils.has_failed = results[0];
            soundUtils.ringtone = results[1];

            if (!soundUtils.has_failed) {
                SharedPreferences.Editor editor = soundUtils.prefs.edit();
                editor.putString("sounds_applied", soundUtils.theme_pid);
                editor.apply();
                Log.d("SoundUtils", "Sound pack installed!");
                FileOperations.delete(context, context.getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/");
            } else {
                Log.e("SoundUtils", "Sound installation aborted!");
                FileOperations.delete(context, context.getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/");
            }
            return null;
        }
    }

    static class FinishReceiver extends BroadcastReceiver {
        private WeakReference<ProgressDialog> progressRef;
        private WeakReference<SoundUtils> soundRef;

        private FinishReceiver(SoundUtils soundUtils, ProgressDialog progress) {
            progressRef = new WeakReference<>(progress);
            soundRef = new WeakReference<>(soundUtils);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            ProgressDialog progress = progressRef.get();
            SoundUtils soundUtils = soundRef.get();
            String PRIMARY_COMMAND_KEY = "primary_command_key";
            String COMMAND_VALUE_JOB_COMPLETE = "job_complete";
            String command = intent.getStringExtra(PRIMARY_COMMAND_KEY);

            if (command.equals(COMMAND_VALUE_JOB_COMPLETE)) {
                context.getApplicationContext().unregisterReceiver(finishReceiver);
                soundUtils.finishFunction();
                progress.dismiss();
            }
        }
    }
}