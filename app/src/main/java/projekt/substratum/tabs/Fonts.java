/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.tabs;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.databinding.TabFontsBinding;
import projekt.substratum.util.tabs.FontUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static projekt.substratum.common.Internal.BOLD_FONT;
import static projekt.substratum.common.Internal.BOLD_ITALICS_FONT;
import static projekt.substratum.common.Internal.BYTE_ACCESS_RATE;
import static projekt.substratum.common.Internal.ENCRYPTED_FILE_EXTENSION;
import static projekt.substratum.common.Internal.FONTS_APPLIED;
import static projekt.substratum.common.Internal.FONT_CACHE;
import static projekt.substratum.common.Internal.FONT_PREVIEW_CACHE;
import static projekt.substratum.common.Internal.ITALICS_FONT;
import static projekt.substratum.common.Internal.NORMAL_FONT;
import static projekt.substratum.common.Internal.START_JOB_ACTION;
import static projekt.substratum.common.Internal.THEME_PID;
import static projekt.substratum.common.References.getThemeAssetManager;
import static projekt.substratum.common.References.setThemeExtraLists;

public class Fonts extends Fragment {

    private static final String fontsDir = "fonts";
    private static final String TAG = "FontUtils";
    private static final boolean encrypted = false;
    private TextView normal;
    private TextView normalBold;
    private TextView italics;
    private TextView italicsBold;
    private ProgressBar progressBar;
    private RelativeLayout defaults;
    private RelativeLayout fontHolder;
    private RelativeLayout fontPlaceholder;
    private Spinner fontSelector;
    private String themePid;
    private ProgressDialog progressDialog;
    private SharedPreferences prefs = Substratum.getPreferences();
    private AsyncTask current;
    private boolean paused;
    private JobReceiver jobReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private Context context;

    private Fonts getInstance() {
        return this;
    }

    @Override
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        context = getContext();

        TabFontsBinding tabFontsBinding =
                DataBindingUtil.inflate(inflater, R.layout.tab_fonts, container, false);

        View view = tabFontsBinding.getRoot();

        progressBar = tabFontsBinding.progressBarLoader;
        defaults = tabFontsBinding.restoreToDefault;
        fontSelector = tabFontsBinding.fontSelection;
        normal = tabFontsBinding.textNormal;
        normalBold = tabFontsBinding.textBold;
        italics = tabFontsBinding.textNormalItalics;
        italicsBold = tabFontsBinding.textNormalBoldItalics;
        fontHolder = tabFontsBinding.fontHolder;
        fontPlaceholder = tabFontsBinding.fontPlaceholder;

        if (getArguments() != null) {
            themePid = getArguments().getString(THEME_PID);
        } else {
            // At this point, the tab has been incorrectly loaded
            return null;
        }

        try {
            fontSelector = setThemeExtraLists(context,
                    themePid,
                    fontsDir,
                    getString(R.string.font_default_spinner),
                    getString(R.string.font_spinner_set_defaults),
                    encrypted,
                    getActivity(),
                    fontSelector);
            if (fontSelector == null) throw new Exception();
            fontSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(final AdapterView<?> arg0,
                                           final View arg1,
                                           final int pos,
                                           final long id) {
                    switch (pos) {
                        case 0:
                            if (current != null)
                                current.cancel(true);
                            fontPlaceholder.setVisibility(View.VISIBLE);
                            defaults.setVisibility(View.GONE);
                            fontHolder.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            paused = true;
                            break;

                        case 1:
                            if (current != null)
                                current.cancel(true);
                            defaults.setVisibility(View.VISIBLE);
                            fontPlaceholder.setVisibility(View.GONE);
                            fontHolder.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            paused = false;
                            break;

                        default:
                            if (current != null)
                                current.cancel(true);
                            defaults.setVisibility(View.GONE);
                            fontPlaceholder.setVisibility(View.GONE);
                            final String[] commands = {arg0.getSelectedItem().toString()};
                            current = new FontPreview(getInstance())
                                    .execute(commands);
                    }
                }

                @Override
                public void onNothingSelected(final AdapterView<?> arg0) {
                }
            });
        } catch (final Exception e) {
            e.printStackTrace();
            Log.e(TAG, "There is no font.zip found within the assets of this theme!");
        }

        // Enable job listener
        jobReceiver = new JobReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(jobReceiver,
                new IntentFilter(getClass().getSimpleName() + START_JOB_ACTION));

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            localBroadcastManager.unregisterReceiver(jobReceiver);
        } catch (final IllegalArgumentException ignored) {
            // unregistered already
        }
    }

    /**
     * Apply the fonts
     */
    private void startApply() {
        if (!paused) {
            if (Systems.checkSubstratumService(context) ||
                    Systems.checkThemeInterfacer(context) ||
                    Settings.System.canWrite(context)) {
                if (fontSelector.getSelectedItemPosition() == 1) {
                    new FontsClearer(this).execute("");
                } else {
                    new FontUtils().execute(
                            fontSelector.getSelectedItem().toString(),
                            context,
                            themePid);
                }
            } else {
                final Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                assert getActivity() != null;
                intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                final Toast toast = Toast.makeText(context,
                        getString(R.string.fonts_dialog_permissions_grant_toast2),
                        Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    /**
     * Clears the currently applied fonts
     */
    private static final class FontsClearer extends AsyncTask<String, Integer, String> {

        private final WeakReference<Fonts> ref;

        private FontsClearer(final Fonts fragment) {
            super();
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            final Fonts fragment = ref.get();
            if (fragment != null) {
                final Context context = fragment.context;
                if (References.ENABLE_EXTRAS_DIALOG) {
                    fragment.progressDialog = new ProgressDialog(context, R.style.RestoreDialog);
                    fragment.progressDialog.setMessage(
                            context.getString(R.string.manage_dialog_performing));
                    fragment.progressDialog.setIndeterminate(true);
                    fragment.progressDialog.setCancelable(false);
                    fragment.progressDialog.show();
                }
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            final Fonts fragment = ref.get();
            if (fragment != null) {
                final Context context = fragment.context;
                if (References.ENABLE_EXTRAS_DIALOG) {
                    fragment.progressDialog.dismiss();
                }
                final SharedPreferences.Editor editor = fragment.prefs.edit();
                editor.remove(FONTS_APPLIED);
                editor.apply();

                if (Systems.checkOMS(context)) {
                    final Toast toast = Toast.makeText(
                            context,
                            R.string.manage_fonts_toast,
                            Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    Toast.makeText(
                            context,
                            R.string.manage_fonts_toast,
                            Toast.LENGTH_SHORT)
                            .show();
                    final AlertDialog.Builder alertDialogBuilder =
                            new AlertDialog.Builder(context);
                    alertDialogBuilder.setTitle(R.string.legacy_dialog_soft_reboot_title);
                    alertDialogBuilder.setMessage(R.string.legacy_dialog_soft_reboot_text);
                    alertDialogBuilder.setPositiveButton(android.R.string.ok,
                            (dialog, id) -> ElevatedCommands.reboot());
                    alertDialogBuilder.setNegativeButton(R.string.remove_dialog_later,
                            (dialog, id) -> dialog.dismiss());
                    alertDialogBuilder.setCancelable(false);
                    final AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final Fonts fragment = ref.get();
            if (fragment != null) {
                final Context context = fragment.context;
                FontsManager.clearFonts(context);
            }
            return null;
        }
    }

    /**
     * Load up the preview for the fonts
     */
    private static class FontPreview extends AsyncTask<String, Integer, String> {

        private final WeakReference<Fonts> ref;

        FontPreview(final Fonts fonts) {
            super();
            ref = new WeakReference<>(fonts);
        }

        private static void unzip(final String source, final String destination) {
            try (ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(source)))) {
                ZipEntry zipEntry;
                final byte[] buffer = new byte[BYTE_ACCESS_RATE];
                while ((zipEntry = inputStream.getNextEntry()) != null) {
                    final File file = new File(destination, zipEntry.getName());
                    final File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException("Failed to ensure directory: " +
                                dir.getAbsolutePath());
                    if (zipEntry.isDirectory())
                        continue;
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        int count;
                        while ((count = inputStream.read(buffer)) != -1)
                            outputStream.write(buffer, 0, count);
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
                Log.e(TAG,
                        "An issue has occurred while attempting to decompress this archive.");
            }
        }

        private static void CopyStream(final InputStream Input, final OutputStream Output) throws
                IOException {
            final byte[] buffer = new byte[BYTE_ACCESS_RATE];
            int length = Input.read(buffer);
            while (length > 0) {
                Output.write(buffer, 0, length);
                length = Input.read(buffer);
            }
        }

        @Override
        protected void onPreExecute() {
            final Fonts fonts = ref.get();
            if (fonts != null) {
                fonts.paused = true;
                fonts.fontHolder.setVisibility(View.INVISIBLE);
                fonts.progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            final Fonts fonts = ref.get();
            if (fonts != null) {
                try {
                    Substratum.log(TAG, "Fonts have been loaded on the drawing panel.");

                    final String work_directory =
                            fonts.context.getCacheDir().getAbsolutePath() + FONT_PREVIEW_CACHE;

                    try {
                        final Typeface normalTf = Typeface.createFromFile(
                                work_directory + NORMAL_FONT);
                        fonts.normal.setTypeface(normalTf);
                    } catch (final Exception e) {
                        Log.e(TAG, "Could not load font from directory for normal template." +
                                " Maybe it wasn't themed?");
                    }

                    try {
                        final Typeface boldTf = Typeface.createFromFile(
                                work_directory + BOLD_FONT);
                        fonts.normalBold.setTypeface(boldTf);
                    } catch (final Exception e) {
                        Log.e(TAG, "Could not load font from directory for normal-bold " +
                                "template. Maybe it wasn't themed?");
                    }

                    try {
                        final Typeface italicsTf = Typeface.createFromFile(
                                work_directory + ITALICS_FONT);
                        fonts.italics.setTypeface(italicsTf);
                    } catch (final Exception e) {
                        Log.e(TAG, "Could not load font from directory for italic template." +
                                " Maybe it wasn't themed?");
                    }

                    try {
                        final Typeface italicsBoldTf = Typeface.createFromFile(
                                work_directory + BOLD_ITALICS_FONT);
                        fonts.italicsBold.setTypeface(italicsBoldTf);
                    } catch (final Exception e) {
                        Log.e(TAG, "Could not load font from directory for italic-bold " +
                                "template. Maybe it wasn't themed?");
                    }

                    FileOperations.delete(fonts.context,
                            fonts.context.getCacheDir().getAbsolutePath() + FONT_PREVIEW_CACHE);
                    fonts.fontHolder.setVisibility(View.VISIBLE);
                    fonts.progressBar.setVisibility(View.GONE);
                    fonts.paused = false;
                } catch (final Exception ignored) {
                    Log.e("Fonts",
                            "Window was destroyed before AsyncTask could complete postExecute()");
                }
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final Fonts fonts = ref.get();
            if (fonts != null) {
                try {
                    final File cacheDirectory = new File(fonts.context.getCacheDir(), FONT_CACHE);
                    if (!cacheDirectory.exists()) {
                        if (cacheDirectory.mkdirs())
                            Substratum.log(TAG, "FontCache folder created");
                    }
                    final File cacheDirectory2 =
                            new File(fonts.context.getCacheDir(), FONT_PREVIEW_CACHE);

                    if (!cacheDirectory2.exists()) {
                        if (cacheDirectory2.mkdirs()) Substratum.log(TAG,
                                "FontCache work folder created");
                    } else {
                        FileOperations.delete(fonts.context,
                                fonts.context.getCacheDir().getAbsolutePath() +
                                        FONT_PREVIEW_CACHE);
                        if (cacheDirectory2.mkdirs())
                            Substratum.log(TAG, "FontCache folder recreated");
                    }

                    // Copy the font.zip from assets/fonts of the theme's assets
                    final String source = sUrl[0] + ".zip";

                    if (encrypted) {
                        FileOperations.copyFileOrDir(
                                Objects.requireNonNull(
                                        getThemeAssetManager(fonts.context, fonts.themePid)
                                ),
                                fontsDir + '/' + source + ENCRYPTED_FILE_EXTENSION,
                                fonts.context.getCacheDir().getAbsolutePath() +
                                        FONT_CACHE + source,
                                fontsDir + '/' + source + ENCRYPTED_FILE_EXTENSION,
                                null);
                    } else {
                        try (InputStream inputStream =
                                     Objects.requireNonNull(
                                             getThemeAssetManager(fonts.context, fonts.themePid)
                                     ).open(fontsDir + '/' + source);
                             OutputStream outputStream =
                                     new FileOutputStream(
                                             fonts.context.getCacheDir().getAbsolutePath() +
                                                     FONT_CACHE + source)) {
                            FontPreview.CopyStream(inputStream, outputStream);
                        } catch (final Exception e) {
                            Log.e(TAG,
                                    "There is no fonts.zip found within the assets of this theme!");
                        }
                    }

                    // Unzip the fonts to get it prepared for the preview
                    FontPreview.unzip(fonts.context.getCacheDir().getAbsolutePath() +
                                    FONT_CACHE + source,
                            fonts.context.getCacheDir().getAbsolutePath() +
                                    FONT_PREVIEW_CACHE);
                } catch (final Exception ignored) {
                    Log.e(TAG, "Unexpectedly lost connection to the application host");
                }
            }
            return null;
        }
    }

    /**
     * Receiver to pick data up from InformationActivity to start the process
     */
    class JobReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (!isAdded()) return;
            startApply();
        }
    }
}