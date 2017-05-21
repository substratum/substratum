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
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import javax.crypto.Cipher;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.tabs.FontManager;

public class FontUtils {

    private Context mContext;
    private ProgressDialog progress;
    private String theme_pid;
    private SharedPreferences prefs;
    private Cipher cipher;

    public void execute(String arguments, Context context, String theme_pid, Cipher cipher) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.mContext = context;
        this.theme_pid = theme_pid;
        this.cipher = cipher;
        new FontHandlerAsync(this).execute(arguments);
    }

    private static class FontHandlerAsync extends AsyncTask<String, Integer, String> {

        private WeakReference<FontUtils> ref;

        private FontHandlerAsync(FontUtils fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            FontUtils fragment = ref.get();
            Context context = fragment.mContext;
            if (References.ENABLE_EXTRAS_DIALOG) {
                fragment.progress = new ProgressDialog(context, R.style.AppTheme_DialogAlert);
                fragment.progress.setMessage(context.getString(R.string.font_dialog_apply_text));
                fragment.progress.setIndeterminate(false);
                fragment.progress.setCancelable(false);
                fragment.progress.show();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            FontUtils fragment = ref.get();
            Context context = fragment.mContext;
            if (References.ENABLE_EXTRAS_DIALOG) {
                fragment.progress.dismiss();
            }
            if (result == null) {
                SharedPreferences.Editor editor = fragment.prefs.edit();
                editor.putString("fonts_applied", fragment.theme_pid);
                editor.apply();
                Toast toast = Toast.makeText(context,
                        context.getString(R.string.font_dialog_apply_success), Toast.LENGTH_LONG);
                toast.show();
            } else {
                Toast toast = Toast.makeText(context,
                        context.getString(R.string.font_dialog_apply_failed), Toast.LENGTH_LONG);
                toast.show();
            }

            if (result == null) {
                // Finally, refresh the window
                if (!References.checkThemeInterfacer(context) &&
                        References.checkOMS(context)) {
                    ThemeManager.restartSystemUI(context);
                } else if (!References.checkOMS(context)) {
                    final AlertDialog.Builder alertDialogBuilder =
                            new AlertDialog.Builder(context);
                    alertDialogBuilder.setTitle(context.getString(
                            R.string.legacy_dialog_soft_reboot_title));
                    alertDialogBuilder.setMessage(context.getString(
                            R.string.legacy_dialog_soft_reboot_text));
                    alertDialogBuilder.setPositiveButton(android.R.string.ok,
                            (dialog, id) -> ElevatedCommands.reboot());
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
            FontUtils fragment = ref.get();
            Context context = fragment.mContext;
            try {
                FontManager.setFonts(
                        context,
                        fragment.theme_pid,
                        sUrl[0],
                        fragment.cipher);
            } catch (Exception e) {
                e.printStackTrace();
                return "failed";
            }
            return null;
        }
    }
}