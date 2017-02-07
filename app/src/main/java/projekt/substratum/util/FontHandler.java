package projekt.substratum.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import projekt.substratum.R;
import projekt.substratum.config.References;

public class FontHandler {

    private Context mContext;
    private ProgressDialog progress;
    private String theme_pid;
    private SharedPreferences prefs;

    public void execute(String arguments, Context context, String theme_pid) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.mContext = context;
        this.theme_pid = theme_pid;
        new FontHandlerAsync().execute(arguments);
    }

    private class FontHandlerAsync extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(mContext, R.style.AppTheme_DialogAlert);
            progress.setMessage(mContext.getString(R.string.font_dialog_apply_text));
            progress.setIndeterminate(false);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();
            if (result == null) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("fonts_applied", theme_pid);
                editor.apply();
                Toast toast = Toast.makeText(mContext,
                        mContext.getString(R.string.font_dialog_apply_success), Toast.LENGTH_LONG);
                toast.show();
            } else {
                Toast toast = Toast.makeText(mContext,
                        mContext.getString(R.string.font_dialog_apply_failed), Toast.LENGTH_LONG);
                toast.show();
            }

            if (result == null) {
                // Finally, refresh the window
                if (References.checkMasquerade(mContext) <= 22 && References.checkOMS(mContext)) {
                    new References.ThreadRunner().execute("pkill -f com.android.systemui");
                } else {
                    final AlertDialog.Builder alertDialogBuilder =
                            new AlertDialog.Builder(mContext);
                    alertDialogBuilder.setTitle(mContext.getString(
                            R.string.legacy_dialog_soft_reboot_title));
                    alertDialogBuilder.setMessage(mContext.getString(
                            R.string.legacy_dialog_soft_reboot_text));
                    alertDialogBuilder.setPositiveButton(android.R.string.ok,
                            (dialog, id) -> References.reboot());
                    alertDialogBuilder.setNegativeButton(
                            R.string.remove_dialog_later, (dialog, id) -> dialog.dismiss());
                    alertDialogBuilder.setCancelable(false);
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            try {
                References.setFonts(mContext, theme_pid, sUrl[0]);
            } catch (Exception e) {
                e.printStackTrace();
                return "failed";
            }
            return null;
        }
    }
}