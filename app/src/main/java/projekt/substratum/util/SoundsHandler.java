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
import projekt.substratum.config.References;

public class SoundsHandler {

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
        References.clearSounds(context);
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

        if (References.checkMasquerade(mContext) < 22) {
            References.mountROData();
            References.mountRO();
        }

        if (ringtone) {
            ringtone = false;
            if (References.checkMasquerade(mContext) < 22 &&
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
            if (References.checkMasquerade(mContext) < 22) {
                progress = new ProgressDialog(mContext, R.style.AppTheme_DialogAlert);
                progress.setMessage(mContext.getString(R.string.sounds_dialog_apply_text));
                progress.setIndeterminate(false);
                progress.setCancelable(false);
                progress.show();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (References.checkMasquerade(mContext) >= 22) {
                if (finishReceiver == null) finishReceiver = new FinishReceiver();
                IntentFilter intentFilter = new IntentFilter("masquerade.substratum" +
                        ".STATUS_CHANGED");
                mContext.registerReceiver(finishReceiver, intentFilter);
            } else {
                finishFunction();
                progress.dismiss();
                References.restartSystemUI(mContext);
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {

            boolean[] results = References.setSounds(mContext, theme_pid, sUrl[0]);
            has_failed = results[0];
            ringtone = results[1];

            if (!has_failed) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("sounds_applied", theme_pid);
                editor.apply();
                Log.d("SoundsHandler", "Sound pack installed!");
                References.delete(mContext, mContext.getCacheDir().getAbsolutePath() +
                        "/SoundsCache/SoundsInjector/");
            } else {
                Log.e("SoundsHandler", "Sound installation aborted!");
                References.delete(mContext, mContext.getCacheDir().getAbsolutePath() +
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