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

import javax.crypto.Cipher;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.tabs.SoundsManager;

import static projekt.substratum.common.Internal.JOB_COMPLETE;
import static projekt.substratum.common.Internal.SOUNDS_APPLIED;
import static projekt.substratum.common.Internal.SOUNDS_CREATION_CACHE;
import static projekt.substratum.common.References.STATUS_CHANGED;
import static projekt.substratum.common.platform.ThemeInterfacerService.PRIMARY_COMMAND_KEY;

public class SoundUtils {

    public static FinishReceiver finishReceiver;

    private Context mContext;
    private String theme_pid;
    private boolean has_failed;
    private boolean ringtone;
    private SharedPreferences prefs;
    private View view;
    private Cipher cipher;

    /**
     * Clear the applied sound pack
     *
     * @param context Self explanatory, bud.
     */
    public static void SoundsClearer(Context context) {
        SoundsManager.clearSounds(context);
    }

    /**
     * Apply the sound pack
     *
     * @param view      The view of the caller
     * @param arguments Arguments to pass
     * @param context   Self explanatory, bud
     * @param theme_pid Theme's package name
     * @param cipher    Encryption handshake
     */
    public void execute(View view,
                        String arguments,
                        Context context,
                        String theme_pid,
                        Cipher cipher) {
        this.mContext = context;
        this.theme_pid = theme_pid;
        this.view = view;
        this.cipher = cipher;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        new SoundsHandlerAsync(this).execute(arguments);
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

        if (!Systems.checkThemeInterfacer(mContext)) {
            FileOperations.mountROData();
            FileOperations.mountRO();
        }

        if (ringtone) {
            ringtone = false;
            if (!Systems.checkThemeInterfacer(mContext) &&
                    !Settings.System.canWrite(mContext)) {
                new AlertDialog.Builder(mContext)
                        .setTitle(mContext.getString(R.string.sounds_dialog_permissions_title))
                        .setMessage(mContext.getString(R.string
                                .sounds_dialog_permissions_text))
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
        private WeakReference<SoundUtils> ref;

        private SoundsHandlerAsync(SoundUtils soundUtils) {
            super();
            ref = new WeakReference<>(soundUtils);
        }

        @Override
        protected void onPostExecute(String result) {
            SoundUtils soundUtils = ref.get();
            if (soundUtils != null) {
                Context context = soundUtils.mContext;
                if (Systems.checkThemeInterfacer(context) &&
                        !Systems.isBinderInterfacer(context)) {
                    if (finishReceiver == null) {
                        finishReceiver = new FinishReceiver(soundUtils);
                    }
                    IntentFilter intentFilter = new IntentFilter(STATUS_CHANGED);
                    context.getApplicationContext().registerReceiver(finishReceiver, intentFilter);
                } else {
                    soundUtils.finishFunction();
                    ThemeManager.restartSystemUI(context);
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            SoundUtils soundUtils = ref.get();
            if (soundUtils != null) {
                Context context = soundUtils.mContext;
                boolean[] results = SoundsManager.setSounds(
                        context,
                        soundUtils.theme_pid,
                        sUrl[0]);
                soundUtils.has_failed = results[0];
                soundUtils.ringtone = results[1];

                if (!soundUtils.has_failed) {
                    SharedPreferences.Editor editor = soundUtils.prefs.edit();
                    editor.putString(SOUNDS_APPLIED, soundUtils.theme_pid);
                    editor.apply();
                    Log.d("SoundUtils", "Sound pack installed!");
                    FileOperations.delete(context, context.getCacheDir().getAbsolutePath() +
                            SOUNDS_CREATION_CACHE);
                } else {
                    Log.e("SoundUtils", "Sound installation aborted!");
                    FileOperations.delete(context, context.getCacheDir().getAbsolutePath() +
                            SOUNDS_CREATION_CACHE);
                }
            }
            return null;
        }
    }

    /**
     * Receiver to wait for the process to be complete
     */
    static class FinishReceiver extends BroadcastReceiver {
        private WeakReference<SoundUtils> soundRef;

        private FinishReceiver(SoundUtils soundUtils) {
            super();
            soundRef = new WeakReference<>(soundUtils);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            SoundUtils soundUtils = soundRef.get();
            if (soundUtils != null) {
                String command = intent.getStringExtra(PRIMARY_COMMAND_KEY);
                if (command != null && command.equals(JOB_COMPLETE)) {
                    context.getApplicationContext().unregisterReceiver(finishReceiver);
                    soundUtils.finishFunction();
                }
            }
        }
    }
}