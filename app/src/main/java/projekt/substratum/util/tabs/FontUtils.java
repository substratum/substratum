/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.tabs;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.tabs.FontsManager;

import java.lang.ref.WeakReference;

import static projekt.substratum.common.Internal.FONTS_APPLIED;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.Systems.checkOMS;
import static projekt.substratum.common.Systems.checkSubstratumService;
import static projekt.substratum.common.Systems.checkThemeInterfacer;

public class FontUtils {

    private Context context;
    private ProgressDialog progress;
    private String themePid;
    private SharedPreferences prefs = Substratum.getPreferences();

    /**
     * Apply the font pack
     *
     * @param arguments Arguments to pass
     * @param context   Self explanatory, bud
     * @param themePid  Theme's package name
     */
    public void execute(String arguments,
                        Context context,
                        String themePid) {
        this.context = context;
        this.themePid = themePid;
        new FontHandlerAsync(this).execute(arguments);
    }

    /**
     * Main function to apply the font pack on the device
     */
    private static class FontHandlerAsync extends AsyncTask<String, Integer, String> {

        private final WeakReference<FontUtils> ref;

        private FontHandlerAsync(FontUtils fontUtils) {
            super();
            ref = new WeakReference<>(fontUtils);
        }

        @Override
        protected void onPreExecute() {
            FontUtils fontUtils = ref.get();
            if (fontUtils != null) {
                Context context = fontUtils.context;
                if (References.ENABLE_EXTRAS_DIALOG) {
                    fontUtils.progress = new ProgressDialog(context, R.style.AppTheme_DialogAlert);
                    fontUtils.progress.setMessage(context.getString(R.string
                            .font_dialog_apply_text));
                    fontUtils.progress.setIndeterminate(false);
                    fontUtils.progress.setCancelable(false);
                    fontUtils.progress.show();
                }
                boolean isRootless =
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
                FontUtils fontUtils = ref.get();
                if (fontUtils != null) {
                    Context context = fontUtils.context;
                    if (References.ENABLE_EXTRAS_DIALOG) {
                        fontUtils.progress.dismiss();
                    }
                    if (result == null) {
                        SharedPreferences.Editor editor = fontUtils.prefs.edit();
                        editor.putString(FONTS_APPLIED, fontUtils.themePid);
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
            FontUtils fontUtils = ref.get();
            if (fontUtils != null) {
                Context context = fontUtils.context;
                try {
                    boolean isRootless = checkOMS(context) &&
                            (checkThemeInterfacer(context) &&
                                    checkSubstratumService(context));

                    if (isRootless) {
                        SharedPreferences.Editor editor = fontUtils.prefs.edit();
                        editor.putString(FONTS_APPLIED, fontUtils.themePid);
                        editor.apply();
                    }

                    // Inform the font manager to start setting fonts!
                    FontsManager.setFonts(
                            context,
                            fontUtils.themePid,
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