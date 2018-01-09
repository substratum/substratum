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

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.tabs.FontsManager;

import static projekt.substratum.common.Internal.FONTS_APPLIED;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.Systems.checkOMS;
import static projekt.substratum.common.Systems.checkSubstratumService;
import static projekt.substratum.common.Systems.checkThemeInterfacer;

public class FontUtils {

    private Context mContext;
    private ProgressDialog progress;
    private String theme_pid;
    private SharedPreferences prefs;

    /**
     * Apply the font pack
     *
     * @param arguments Arguments to pass
     * @param context   Self explanatory, bud
     * @param theme_pid Theme's package name
     */
    public void execute(String arguments,
                        Context context,
                        String theme_pid) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.mContext = context;
        this.theme_pid = theme_pid;
        new FontHandlerAsync(this).execute(arguments);
    }

    /**
     * Main function to apply the font pack on the device
     */
    private static class FontHandlerAsync extends AsyncTask<String, Integer, String> {

        private WeakReference<FontUtils> ref;

        private FontHandlerAsync(FontUtils fragment) {
            super();
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            FontUtils fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.mContext;
                if (References.ENABLE_EXTRAS_DIALOG) {
                    fragment.progress = new ProgressDialog(context, R.style.AppTheme_DialogAlert);
                    fragment.progress.setMessage(context.getString(R.string
                            .font_dialog_apply_text));
                    fragment.progress.setIndeterminate(false);
                    fragment.progress.setCancelable(false);
                    fragment.progress.show();
                }
                Boolean isRootless =
                        (checkThemeInterfacer(context) || checkSubstratumService(context));
                if (isRootless)
                    Toast.makeText(context,
                            context.getString(R.string.font_dialog_apply_success), Toast
                                    .LENGTH_LONG).show();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if ((result == null) || !result.equals(INTERFACER_PACKAGE)) {
                FontUtils fragment = ref.get();
                if (fragment != null) {
                    Context context = fragment.mContext;
                    if (References.ENABLE_EXTRAS_DIALOG) {
                        fragment.progress.dismiss();
                    }
                    if (result == null) {
                        SharedPreferences.Editor editor = fragment.prefs.edit();
                        editor.putString(FONTS_APPLIED, fragment.theme_pid);
                        editor.apply();
                        Toast toast = Toast.makeText(context,
                                context.getString(R.string.font_dialog_apply_success), Toast
                                        .LENGTH_LONG);
                        toast.show();
                    } else {
                        Toast toast = Toast.makeText(context,
                                context.getString(R.string.font_dialog_apply_failed), Toast
                                        .LENGTH_LONG);
                        toast.show();
                    }
                    if (!Systems.checkSubstratumService(context) &&
                            !Systems.checkThemeInterfacer(context) &&
                            Systems.checkOMS(context)) {
                        ThemeManager.restartSystemUI(context);
                    } else if (!Systems.checkOMS(context)) {
                        AlertDialog.Builder alertDialogBuilder =
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
        }

        @Override
        protected String doInBackground(String... sUrl) {
            FontUtils fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.mContext;
                try {
                    Boolean isRootless = checkOMS(context) &&
                            (checkThemeInterfacer(context) &&
                                    checkSubstratumService(context));

                    if (isRootless) {
                        SharedPreferences.Editor editor = fragment.prefs.edit();
                        editor.putString(FONTS_APPLIED, fragment.theme_pid);
                        editor.apply();
                    }

                    // Inform the font manager to start setting fonts!
                    FontsManager.setFonts(
                            context,
                            fragment.theme_pid,
                            sUrl[0]);

                    if (isRootless)
                        return INTERFACER_PACKAGE;
                } catch (Exception e) {
                    e.printStackTrace();
                    return "failed";
                }
            }
            return null;
        }
    }
}