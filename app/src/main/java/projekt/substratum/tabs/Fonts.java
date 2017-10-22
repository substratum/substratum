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

package projekt.substratum.tabs;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.tabs.FontManager;
import projekt.substratum.util.tabs.FontUtils;

public class Fonts extends Fragment {

    private static final String fontsDir = "fonts";
    private static final String TAG = "FontUtils";
    private String theme_pid;
    private ViewGroup root;
    private ProgressBar progressBar;
    private Spinner fontSelector;
    private RelativeLayout font_holder;
    private RelativeLayout font_placeholder;
    private RelativeLayout defaults;
    private ProgressDialog mProgressDialog;
    private SharedPreferences prefs;
    private AsyncTask current;
    private AssetManager themeAssetManager;
    private boolean paused;
    private JobReceiver jobReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private Context mContext;
    private static final Boolean encrypted = false;
    private Cipher cipher;

    private Fonts getInstance() {
        return this;
    }

    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        this.mContext = this.getContext();
        this.theme_pid = this.getArguments().getString("theme_pid");
        final byte[] encryption_key = this.getArguments().getByteArray("encryption_key");
        final byte[] iv_encrypt_key = this.getArguments().getByteArray("iv_encrypt_key");

        // encrypted = encryption_key != null && iv_encrypt_key != null;

        if (this.encrypted) {
            try {
                this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                this.cipher.init(
                        Cipher.DECRYPT_MODE,
                        new SecretKeySpec(encryption_key, "AES"),
                        new IvParameterSpec(iv_encrypt_key)
                );
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        this.root = (ViewGroup) inflater.inflate(R.layout.tab_fonts, container, false);
        this.progressBar = this.root.findViewById(R.id.progress_bar_loader);
        this.defaults = this.root.findViewById(R.id.restore_to_default);
        this.font_holder = this.root.findViewById(R.id.font_holder);
        this.font_placeholder = this.root.findViewById(R.id.font_placeholder);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        try {
            // Parses the list of items in the fonts folder
            final Resources themeResources = this.mContext.getPackageManager().getResourcesForApplication
                    (this.theme_pid);
            this.themeAssetManager = themeResources.getAssets();
            final String[] fileArray = this.themeAssetManager.list(fontsDir);
            final List<String> unparsedFonts = new ArrayList<>();
            Collections.addAll(unparsedFonts, fileArray);

            // Creates the list of dropdown items
            final ArrayList<String> fonts = new ArrayList<>();
            fonts.add(this.getString(R.string.font_default_spinner));
            fonts.add(this.getString(R.string.font_spinner_set_defaults));
            for (int i = 0; i < unparsedFonts.size(); i++) {
                fonts.add(unparsedFonts.get(i).substring(0,
                        unparsedFonts.get(i).length() - (this.encrypted ? 8 : 4)));
            }

            final SpinnerAdapter adapter1 = new ArrayAdapter<>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, fonts);
            this.fontSelector = this.root.findViewById(R.id.fontSelection);
            this.fontSelector.setAdapter(adapter1);
            this.fontSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(final AdapterView<?> arg0, final View arg1,
                                           final int pos, final long id) {
                    switch (pos) {
                        case 0:
                            if (Fonts.this.current != null)
                                Fonts.this.current.cancel(true);
                            Fonts.this.font_placeholder.setVisibility(View.VISIBLE);
                            Fonts.this.defaults.setVisibility(View.GONE);
                            Fonts.this.font_holder.setVisibility(View.GONE);
                            Fonts.this.progressBar.setVisibility(View.GONE);
                            Fonts.this.paused = true;
                            break;

                        case 1:
                            if (Fonts.this.current != null)
                                Fonts.this.current.cancel(true);
                            Fonts.this.defaults.setVisibility(View.VISIBLE);
                            Fonts.this.font_placeholder.setVisibility(View.GONE);
                            Fonts.this.font_holder.setVisibility(View.GONE);
                            Fonts.this.progressBar.setVisibility(View.GONE);
                            Fonts.this.paused = false;
                            break;

                        default:
                            if (Fonts.this.current != null)
                                Fonts.this.current.cancel(true);
                            Fonts.this.defaults.setVisibility(View.GONE);
                            Fonts.this.font_placeholder.setVisibility(View.GONE);
                            final String[] commands = {arg0.getSelectedItem().toString()};
                            Fonts.this.current = new FontPreview(Fonts.this.getInstance()).execute(commands);
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
        this.jobReceiver = new JobReceiver();
        this.localBroadcastManager = LocalBroadcastManager.getInstance(this.mContext);
        this.localBroadcastManager.registerReceiver(this.jobReceiver, new IntentFilter("Fonts.START_JOB"));

        return this.root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            this.localBroadcastManager.unregisterReceiver(this.jobReceiver);
        } catch (final IllegalArgumentException e) {
            // unregistered already
        }
    }


    private void startApply() {
        if (!this.paused) {
            if (Systems.checkThemeInterfacer(this.mContext) ||
                    Settings.System.canWrite(this.mContext)) {
                if (this.fontSelector.getSelectedItemPosition() == 1) {
                    new FontsClearer(this).execute("");
                } else {
                    new FontUtils().execute(this.fontSelector.getSelectedItem().toString(),
                            this.mContext, this.theme_pid, this.cipher);
                }
            } else {
                final Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + this.getActivity().getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(intent);
                final Toast toast = Toast.makeText(this.mContext,
                        this.getString(R.string.fonts_dialog_permissions_grant_toast2),
                        Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    private static final class FontsClearer extends AsyncTask<String, Integer, String> {

        private final WeakReference<Fonts> ref;

        private FontsClearer(final Fonts fragment) {
            super();
            this.ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            final Fonts fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.mContext;
                if (References.ENABLE_EXTRAS_DIALOG) {
                    fragment.mProgressDialog = new ProgressDialog(context, R.style.RestoreDialog);
                    fragment.mProgressDialog.setMessage(
                            context.getString(R.string.manage_dialog_performing));
                    fragment.mProgressDialog.setIndeterminate(true);
                    fragment.mProgressDialog.setCancelable(false);
                    fragment.mProgressDialog.show();
                }
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            final Fonts fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.mContext;
                if (References.ENABLE_EXTRAS_DIALOG) {
                    fragment.mProgressDialog.dismiss();
                }
                final SharedPreferences.Editor editor = fragment.prefs.edit();
                editor.remove("fonts_applied");
                editor.apply();

                if (Systems.checkOMS(context)) {
                    final Toast toast = Toast.makeText(
                            context,
                            R.string.manage_fonts_toast,
                            Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    final Toast toast = Toast.makeText(
                            context,
                            R.string.manage_fonts_toast,
                            Toast.LENGTH_SHORT);
                    toast.show();
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
            final Fonts fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.mContext;
                FontManager.clearFonts(context);
            }
            return null;
        }
    }

    private static class FontPreview extends AsyncTask<String, Integer, String> {

        private final WeakReference<Fonts> ref;

        FontPreview(final Fonts fonts) {
            super();
            this.ref = new WeakReference<>(fonts);
        }

        @Override
        protected void onPreExecute() {
            final Fonts fonts = this.ref.get();
            if (fonts != null) {
                fonts.paused = true;
                fonts.font_holder.setVisibility(View.INVISIBLE);
                fonts.progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            final Fonts fonts = this.ref.get();
            if (fonts != null) {
                try {
                    Log.d(TAG, "Fonts have been loaded on the drawing panel.");

                    final String work_directory = fonts.mContext.getCacheDir().getAbsolutePath() +
                            "/FontCache/font_preview/";

                    try {
                        final Typeface normal_tf = Typeface.createFromFile(work_directory +
                                "Roboto-Regular.ttf");
                        final TextView normal = fonts.root.findViewById(R.id.text_normal);
                        normal.setTypeface(normal_tf);
                    } catch (final Exception e) {
                        Log.e(TAG, "Could not load font from directory for normal template." +
                                " Maybe it wasn't themed?");
                    }

                    try {
                        final Typeface bold_tf = Typeface.createFromFile(work_directory +
                                "Roboto-Black.ttf");

                        final TextView normal_bold = fonts.root.findViewById(R.id.text_bold);
                        normal_bold.setTypeface(bold_tf);
                    } catch (final Exception e) {
                        Log.e(TAG, "Could not load font from directory for normal-bold " +
                                "template. Maybe it wasn't themed?");
                    }

                    try {
                        final Typeface italics_tf = Typeface.createFromFile(work_directory +
                                "Roboto-Italic" +
                                ".ttf");
                        final TextView italics = fonts.root.findViewById(R.id.text_normal_italics);
                        italics.setTypeface(italics_tf);
                    } catch (final Exception e) {
                        Log.e(TAG, "Could not load font from directory for italic template." +
                                " Maybe it wasn't themed?");
                    }

                    try {
                        final Typeface italics_bold_tf = Typeface.createFromFile(work_directory +
                                "Roboto-BoldItalic.ttf");
                        final TextView italics_bold = fonts.root.findViewById(
                                R.id.text_normal_bold_italics);
                        italics_bold.setTypeface(italics_bold_tf);
                    } catch (final Exception e) {
                        Log.e(TAG, "Could not load font from directory for italic-bold " +
                                "template. Maybe it wasn't themed?");
                    }

                    FileOperations.delete(fonts.mContext,
                            fonts.mContext.getCacheDir().getAbsolutePath() +
                                    "/FontCache/font_preview/");
                    fonts.font_holder.setVisibility(View.VISIBLE);
                    fonts.progressBar.setVisibility(View.GONE);
                    fonts.paused = false;
                } catch (final Exception e) {
                    Log.e("Fonts",
                            "Window was destroyed before AsyncTask could complete postExecute()");
                }
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final Fonts fonts = this.ref.get();
            if (fonts != null) {
                try {
                    final File cacheDirectory = new File(fonts.mContext.getCacheDir(), "/FontCache/");
                    if (!cacheDirectory.exists()) {
                        if (cacheDirectory.mkdirs()) Log.d(TAG, "FontCache folder created");
                    }
                    final File cacheDirectory2 = new File(fonts.mContext.getCacheDir(),
                            "/FontCache/font_preview/");

                    if (!cacheDirectory2.exists()) {
                        if (cacheDirectory2.mkdirs()) Log.d(TAG,
                                "FontCache work folder created");
                    } else {
                        FileOperations.delete(fonts.mContext,
                                fonts.mContext.getCacheDir().getAbsolutePath() +
                                        "/FontCache/font_preview/");
                        if (cacheDirectory2.mkdirs()) Log.d(TAG, "FontCache folder recreated");
                    }

                    // Copy the font.zip from assets/fonts of the theme's assets
                    final String source = sUrl[0] + ".zip";

                    if (fonts.encrypted) {
                        FileOperations.copyFileOrDir(
                                fonts.themeAssetManager,
                                fontsDir + "/" + source + ".enc",
                                fonts.mContext.getCacheDir().getAbsolutePath() +
                                        "/FontCache/" + source,
                                fontsDir + "/" + source + ".enc",
                                fonts.cipher);
                    } else {
                        try (InputStream inputStream =
                                     fonts.themeAssetManager.open(fontsDir + "/" + source);

                             OutputStream outputStream =
                                     new FileOutputStream(
                                             fonts.mContext.getCacheDir().getAbsolutePath() +
                                                     "/FontCache/" + source)) {
                            this.CopyStream(inputStream, outputStream);
                        } catch (final Exception e) {
                            Log.e(TAG,
                                    "There is no fonts.zip found within the assets of this theme!");
                        }
                    }

                    // Unzip the fonts to get it prepared for the preview
                    this.unzip(fonts.mContext.getCacheDir().getAbsolutePath() + "/FontCache/" + source,
                            fonts.mContext.getCacheDir().getAbsolutePath() +
                                    "/FontCache/font_preview/");
                } catch (final Exception e) {
                    Log.e(TAG, "Unexpectedly lost connection to the application host");
                }
            }
            return null;
        }

        private void unzip(final String source, final String destination) {
            try (ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(source)))) {
                ZipEntry zipEntry;
                final byte[] buffer = new byte[8192];
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

        private void CopyStream(final InputStream Input, final OutputStream Output) throws IOException {
            final byte[] buffer = new byte[5120];
            int length = Input.read(buffer);
            while (length > 0) {
                Output.write(buffer, 0, length);
                length = Input.read(buffer);
            }
        }
    }

    class JobReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (!Fonts.this.isAdded()) return;
            Fonts.this.startApply();
        }
    }
}