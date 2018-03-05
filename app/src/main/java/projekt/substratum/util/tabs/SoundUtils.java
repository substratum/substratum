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
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import java.lang.ref.WeakReference;

import projekt.substratum.R;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.tabs.SoundsManager;
import projekt.substratum.util.views.Lunchbar;

import static projekt.substratum.common.Internal.JOB_COMPLETE;
import static projekt.substratum.common.Internal.SOUNDS_APPLIED;
import static projekt.substratum.common.Internal.SOUNDS_CREATION_CACHE;
import static projekt.substratum.common.References.STATUS_CHANGED;

public class SoundUtils {

    private static final String PRIMARY_COMMAND_KEY = "primary_command_key";
    public static FinishReceiver finishReceiver;
    private Context context;
    private String themePid;
    private boolean hasFailed;
    private boolean ringtone;
    private SharedPreferences prefs;
    private View view;

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
     * @param themePid  Theme's package name
     */
    public void execute(View view,
                        String arguments,
                        Context context,
                        String themePid) {
        this.context = context;
        this.themePid = themePid;
        this.view = view;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        new SoundsHandlerAsync(this).execute(arguments);
    }

    private void finishFunction() {
        if (!hasFailed) {
            Lunchbar.make(view,
                    context.getString(R.string.sounds_dialog_apply_success),
                    Snackbar.LENGTH_LONG)
                    .show();
        } else {
            Lunchbar.make(view,
                    context.getString(R.string.sounds_dialog_apply_failed),
                    Snackbar.LENGTH_LONG)
                    .show();
        }

        if (ringtone) ringtone = false;
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
                Context context = soundUtils.context;
                if (Systems.checkThemeInterfacer(context) &&
                        !Systems.isBinderInterfacer(context)) {
                    if (finishReceiver == null) {
                        finishReceiver = new FinishReceiver(soundUtils);
                    }
                    IntentFilter intentFilter = new IntentFilter(STATUS_CHANGED);
                    context.getApplicationContext().registerReceiver(finishReceiver, intentFilter);
                } else {
                    soundUtils.finishFunction();
                    if (!Systems.checkSubstratumService(context)) {
                        ThemeManager.restartSystemUI(context);
                    }
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            SoundUtils soundUtils = ref.get();
            if (soundUtils != null) {
                Context context = soundUtils.context;
                boolean[] results = SoundsManager.setSounds(
                        context,
                        soundUtils.themePid,
                        sUrl[0]);
                soundUtils.hasFailed = results[0];
                soundUtils.ringtone = results[1];

                if (!soundUtils.hasFailed) {
                    SharedPreferences.Editor editor = soundUtils.prefs.edit();
                    editor.putString(SOUNDS_APPLIED, soundUtils.themePid);
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