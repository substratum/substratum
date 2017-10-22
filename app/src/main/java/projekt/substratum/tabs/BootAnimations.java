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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

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
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.tabs.BootAnimationManager;
import projekt.substratum.util.files.Root;
import projekt.substratum.util.tabs.BootAnimationUtils;

import static projekt.substratum.InformationActivity.currentShownLunchBar;

public class BootAnimations extends Fragment {

    private static final int ANIMATION_FRAME_DURATION = 40;
    private static final String TAG = "BootAnimationUtils";
    private static String bootanimationsDir = "bootanimation";
    private String theme_pid;
    private ImageView bootAnimationPreview;
    private ProgressBar progressBar;
    private Spinner bootAnimationSelector;
    private RelativeLayout bootanimation_placeholder;
    private RelativeLayout defaults;
    private ProgressDialog mProgressDialog;
    private SharedPreferences prefs;
    private AsyncTask current;
    private NestedScrollView nsv;
    private AssetManager themeAssetManager;
    private boolean paused;
    private JobReceiver jobReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private static final Boolean encrypted = false;
    private Cipher cipher;
    private Boolean shutdownBootAnimation;
    private Context mContext;
    private final HandlerThread previewHandlerThread = new HandlerThread("BootAnimationPreviewThread");
    private Handler previewHandler;
    private Runnable previewRunnable;
    private List<String> previewImages;
    private int previewIndex;
    private final BitmapFactory.Options options = new BitmapFactory.Options();

    private BootAnimations getInstance() {
        return this;
    }

    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        this.mContext = this.getContext();
        this.theme_pid = this.getArguments().getString("theme_pid");
        if (this.getArguments().getBoolean("shutdownanimation", false)) {
            bootanimationsDir = "shutdownanimation";
            this.shutdownBootAnimation = true;
        } else {
            this.shutdownBootAnimation = false;
        }

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

        final ViewGroup root = (ViewGroup) inflater.inflate(R.layout.tab_bootanimations, container,
                false);
        this.nsv = root.findViewById(R.id.nestedScrollView);

        this.bootAnimationPreview = root.findViewById(R.id.bootAnimationPreview);
        this.previewHandlerThread.start();

        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        this.progressBar = root.findViewById(R.id.progress_bar_loader);

        this.bootanimation_placeholder = root.findViewById(R.id.bootanimation_placeholder);
        this.defaults = root.findViewById(R.id.restore_to_default);

        if (this.shutdownBootAnimation) {
            final TextView placeholderText = root.findViewById(R.id.bootanimation_placeholder_text);
            placeholderText.setText(R.string.shutdownanimation_placeholder_text);
            final TextView restoreText = root.findViewById(R.id.restore_to_default_text);
            restoreText.setText(R.string.shutdownanimation_defaults_text);
        }

        try {
            // Parses the list of items in the boot animation folder
            final Resources themeResources =
                    this.mContext.getPackageManager().getResourcesForApplication(this.theme_pid);
            this.themeAssetManager = themeResources.getAssets();
            final String[] fileArray = this.themeAssetManager.list(bootanimationsDir);
            final List<String> unparsedBootAnimations = new ArrayList<>();
            Collections.addAll(unparsedBootAnimations, fileArray);

            // Creates the list of dropdown items
            final ArrayList<String> parsedBootAnimations = new ArrayList<>();
            parsedBootAnimations.add(this.getString(this.shutdownBootAnimation ?
                    R.string.shutdownanimation_default_spinner :
                    R.string.bootanimation_default_spinner));
            parsedBootAnimations.add(this.getString(this.shutdownBootAnimation ?
                    R.string.shutdownanimation_spinner_set_defaults :
                    R.string.bootanimation_spinner_set_defaults));
            for (int i = 0; i < unparsedBootAnimations.size(); i++) {
                parsedBootAnimations.add(unparsedBootAnimations.get(i).substring(0,
                        unparsedBootAnimations.get(i).length() - (this.encrypted ? 8 : 4)));
            }

            final SpinnerAdapter adapter1 = new ArrayAdapter<>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, parsedBootAnimations);
            this.bootAnimationSelector = root.findViewById(R.id.bootAnimationSelection);
            this.bootAnimationSelector.setAdapter(adapter1);
            this.bootAnimationSelector.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(final AdapterView<?> arg0, final View arg1,
                                                   final int pos, final long id) {
                            switch (pos) {
                                case 0:
                                    if (BootAnimations.this.current != null) BootAnimations.this.current.cancel(true);
                                    if ((BootAnimations.this.previewHandler != null) && (BootAnimations.this.previewRunnable != null)) {
                                        BootAnimations.this.previewHandler.removeCallbacks(BootAnimations.this.previewRunnable);
                                    }
                                    BootAnimations.this.bootanimation_placeholder.setVisibility(View.VISIBLE);
                                    BootAnimations.this.defaults.setVisibility(View.GONE);
                                    BootAnimations.this.bootAnimationPreview.setImageDrawable(null);
                                    BootAnimations.this.bootAnimationPreview.setVisibility(View.GONE);
                                    BootAnimations.this.progressBar.setVisibility(View.GONE);
                                    BootAnimations.this.paused = true;
                                    break;
                                case 1:
                                    if (BootAnimations.this.current != null) BootAnimations.this.current.cancel(true);
                                    if ((BootAnimations.this.previewHandler != null) && (BootAnimations.this.previewRunnable != null)) {
                                        BootAnimations.this.previewHandler.removeCallbacks(BootAnimations.this.previewRunnable);
                                    }
                                    BootAnimations.this.defaults.setVisibility(View.VISIBLE);
                                    BootAnimations.this.bootanimation_placeholder.setVisibility(View.GONE);
                                    BootAnimations.this.progressBar.setVisibility(View.GONE);
                                    BootAnimations.this.bootAnimationPreview.setImageDrawable(null);
                                    BootAnimations.this.bootAnimationPreview.setVisibility(View.GONE);
                                    BootAnimations.this.progressBar.setVisibility(View.GONE);
                                    BootAnimations.this.paused = false;
                                    break;
                                default:
                                    if (BootAnimations.this.current != null) BootAnimations.this.current.cancel(true);
                                    BootAnimations.this.bootAnimationPreview.setVisibility(View.VISIBLE);
                                    BootAnimations.this.defaults.setVisibility(View.GONE);
                                    BootAnimations.this.bootanimation_placeholder.setVisibility(View.GONE);
                                    final String[] commands = {arg0.getSelectedItem().toString()};
                                    if ((BootAnimations.this.previewHandler != null) && (BootAnimations.this.previewRunnable != null)) {
                                        BootAnimations.this.previewHandler.removeCallbacks(BootAnimations.this.previewRunnable);
                                    }
                                    BootAnimations.this.current = new BootAnimationPreview(BootAnimations.this.getInstance())
                                            .execute(commands);
                            }
                        }

                        @Override
                        public void onNothingSelected(final AdapterView<?> arg0) {
                        }
                    });
        } catch (final Exception e) {
            e.printStackTrace();
            Log.e(TAG, "There is no animation.zip found within the assets of this theme!");
        }

        // Enable job listener
        this.jobReceiver = new JobReceiver();
        final IntentFilter intentFilter = new IntentFilter(
                (this.shutdownBootAnimation ? "ShutdownAnimations" : "BootAnimations") + ".START_JOB");
        this.localBroadcastManager = LocalBroadcastManager.getInstance(this.mContext);
        this.localBroadcastManager.registerReceiver(this.jobReceiver, intentFilter);

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            this.localBroadcastManager.unregisterReceiver(this.jobReceiver);
        } catch (final IllegalArgumentException e) {
            // Unregistered already
        }
    }

    public boolean isShutdownTab() {
        return this.shutdownBootAnimation;
    }

    private void startApply() {
        if (!this.paused) {
            if (((Systems.getDeviceEncryptionStatus(this.mContext) <= 1) ||
                    this.shutdownBootAnimation) ||
                    !Systems.checkOMS(this.mContext)) {
                if (this.bootAnimationSelector.getSelectedItemPosition() == 1) {
                    new BootAnimationClearer(this).execute("");
                } else {
                    BootAnimationUtils.execute(this.nsv,
                            this.bootAnimationSelector.getSelectedItem().toString(),
                            this.mContext, this.theme_pid, this.encrypted, this.shutdownBootAnimation, this.cipher);
                }
            } else {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
                builder.setTitle(R.string.root_required_title)
                        .setMessage(R.string.root_required_boot_animation)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            if (Root.requestRootAccess()) {
                                if (this.bootAnimationSelector.getSelectedItemPosition() == 1) {
                                    new BootAnimationClearer(this).execute("");
                                } else {
                                    BootAnimationUtils.execute(this.nsv,
                                            this.bootAnimationSelector.getSelectedItem().toString(),
                                            this.mContext, this.theme_pid, this.encrypted,
                                            this.shutdownBootAnimation, this.cipher);
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel,
                                (dialog, which) -> dialog.cancel())
                        .show();
            }
        }
    }

    private static class BootAnimationClearer extends AsyncTask<String, Integer, String> {
        private final WeakReference<BootAnimations> ref;

        BootAnimationClearer(final BootAnimations bootAnimations) {
            super();
            this.ref = new WeakReference<>(bootAnimations);
        }

        @Override
        protected void onPreExecute() {
            final BootAnimations bootAnimations = this.ref.get();
            if (bootAnimations != null) {
                bootAnimations.mProgressDialog = new ProgressDialog(
                        bootAnimations.getActivity(), R.style.RestoreDialog);
                bootAnimations.mProgressDialog.setMessage(
                        bootAnimations.getString(R.string.manage_dialog_performing));
                bootAnimations.mProgressDialog.setIndeterminate(true);
                bootAnimations.mProgressDialog.setCancelable(false);
                bootAnimations.mProgressDialog.show();
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            final BootAnimations bootAnimations = this.ref.get();
            if (bootAnimations != null) {
                bootAnimations.mProgressDialog.dismiss();
                final SharedPreferences.Editor editor = bootAnimations.prefs.edit();
                if (bootAnimations.shutdownBootAnimation) {
                    editor.remove("shutdownanimation_applied");
                } else {
                    editor.remove("bootanimation_applied");
                }
                editor.apply();
                if (bootAnimations.getView() != null) {
                    currentShownLunchBar = Lunchbar.make(bootAnimations.getView(),
                            bootAnimations.getString(bootAnimations.shutdownBootAnimation ?
                                    R.string.manage_shutdownanimation_toast :
                                    R.string.manage_bootanimation_toast),
                            Lunchbar.LENGTH_LONG);
                    currentShownLunchBar.show();
                }
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final BootAnimations bootAnimations = this.ref.get();
            if (bootAnimations != null) {
                BootAnimationManager.clearBootAnimation(bootAnimations.mContext,
                        bootAnimations.shutdownBootAnimation);
            }
            return null;
        }
    }

    private static class BootAnimationPreview extends AsyncTask<String, Integer, String> {
        private final WeakReference<BootAnimations> ref;

        BootAnimationPreview(final BootAnimations bootAnimations) {
            super();
            this.ref = new WeakReference<>(bootAnimations);
        }

        @Override
        protected void onPreExecute() {
            final BootAnimations bootAnimations = this.ref.get();
            if (bootAnimations != null) {
                FileOperations.delete(
                        bootAnimations.mContext,
                        bootAnimations.mContext.getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/animation_preview/");
                bootAnimations.paused = true;
                bootAnimations.bootAnimationPreview.setImageDrawable(null);
                bootAnimations.progressBar.setVisibility(View.VISIBLE);
                bootAnimations.previewImages = new ArrayList<>();
                bootAnimations.previewHandler = new Handler(
                        bootAnimations.previewHandlerThread.getLooper());
                bootAnimations.previewIndex = 0;
                bootAnimations.previewRunnable = () -> {
                    try {
                        final Bitmap bmp = BitmapFactory.decodeFile(
                                bootAnimations.previewImages.get(bootAnimations.previewIndex),
                                bootAnimations.options);

                        bootAnimations.getActivity().runOnUiThread(() -> {
                            bootAnimations.bootAnimationPreview.setImageBitmap(bmp);
                            bootAnimations.previewIndex++;
                            if (bootAnimations.previewIndex == bootAnimations.previewImages.size())
                                bootAnimations.previewIndex = 0;
                        });
                        bootAnimations.previewHandler.postDelayed(
                                bootAnimations.previewRunnable, (long) ANIMATION_FRAME_DURATION);
                    } catch (final Exception e) {
                        // Suppress warning
                    }
                };
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            final BootAnimations bootAnimations = this.ref.get();
            if (bootAnimations != null) {
                try {
                    Log.d(TAG, "Loaded boot animation contains " +
                            bootAnimations.previewImages.size() + " frames.");

                    if (bootAnimations.bootAnimationSelector.getSelectedItemPosition() > 1) {
                        Log.d(TAG, "Displaying bootanimation after render task complete!");
                        bootAnimations.previewHandler.post(bootAnimations.previewRunnable);
                    }
                    bootAnimations.progressBar.setVisibility(View.GONE);
                    bootAnimations.paused = false;
                } catch (final Exception e) {
                    Log.e(TAG, "Window was destroyed before AsyncTask could perform postExecute()");
                }
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final BootAnimations bootAnimations = this.ref.get();
            if (bootAnimations != null) {
                try {
                    final File cacheDirectory = new File(
                            bootAnimations.mContext.getCacheDir(), "/BootAnimationCache/");

                    if (!cacheDirectory.exists() && cacheDirectory.mkdirs()) {
                        Log.d(TAG, "Bootanimation folder created");
                    }
                    final File cacheDirectory2 = new File(bootAnimations.mContext.getCacheDir(),
                            "/BootAnimationCache/animation_preview/");
                    if (!cacheDirectory2.exists() && cacheDirectory2.mkdirs()) {
                        Log.d(TAG, "Bootanimation work folder created");
                    } else {
                        FileOperations.delete(bootAnimations.mContext,
                                bootAnimations.mContext.getCacheDir().getAbsolutePath() +
                                        "/BootAnimationCache/animation_preview/");
                        final boolean created = cacheDirectory2.mkdirs();
                        if (created) Log.d(TAG, "Bootanimation folder recreated");
                    }

                    // Copy the bootanimation.zip from assets/bootanimation of the theme's assets
                    final String source = sUrl[0] + ".zip";

                    if (bootAnimations.encrypted) {
                        FileOperations.copyFileOrDir(
                                bootAnimations.themeAssetManager,
                                bootanimationsDir + "/" + source + ".enc",
                                bootAnimations.mContext.getCacheDir().getAbsolutePath() +
                                        "/BootAnimationCache/" + source,
                                bootanimationsDir + "/" + source + ".enc",
                                bootAnimations.cipher);
                    } else {
                        try (InputStream inputStream = bootAnimations.themeAssetManager.open(
                                bootanimationsDir + "/" + source);
                             OutputStream outputStream =
                                     new FileOutputStream(bootAnimations.mContext.getCacheDir()
                                             .getAbsolutePath() +
                                             "/BootAnimationCache/" + source)) {
                            BootAnimationPreview.CopyStream(inputStream, outputStream);
                        } catch (final Exception e) {
                            Log.e(TAG,
                                    "There is no bootanimation.zip found within the assets of " +
                                            "this theme!");
                        }
                    }

                    // Unzip the boot animation to get it prepared for the preview
                    BootAnimationPreview.unzip(bootAnimations.mContext.getCacheDir().getAbsolutePath() +
                                    "/BootAnimationCache/" + source,
                            bootAnimations.mContext.getCacheDir().getAbsolutePath() +
                                    "/BootAnimationCache/animation_preview/");

                    bootAnimations.options.inPreferredConfig = Bitmap.Config.RGB_565;
                    bootAnimations.options.inSampleSize = BootAnimationPreview.previewDeterminator(
                            bootAnimations.mContext.getCacheDir().getAbsolutePath() +
                                    "/BootAnimationCache/" + source);

                    // List images files in directory
                    final File previewDirectory = new File(bootAnimations.mContext.getCacheDir(),
                            "/BootAnimationCache/animation_preview/");
                    final String[] dirs = previewDirectory.list();
                    final String[] supportedFile = {"jpg", "png", "jpeg"};
                    for (final String content : dirs) {
                        final File current = new File(previewDirectory, content);
                        if (current.isDirectory()) {
                            final String[] currentDirs = current.list();
                            for (final String currentDirContent : currentDirs) {
                                final File probablyImage = new File(current, currentDirContent);
                                for (final String ext : supportedFile) {
                                    if (probablyImage.getName().toLowerCase().endsWith(ext)) {
                                        bootAnimations.previewImages.add(
                                                probablyImage.getAbsolutePath());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Unexpectedly lost connection to the application host");
                }
            }
            return null;
        }

        private static int previewDeterminator(final String file_location) {
            final File checkFile = new File(file_location);
            final int file_size = Integer.parseInt(String.valueOf(checkFile.length() / 1024L / 1024L));
            Log.d(TAG, "Managing bootanimation with size: " + file_size + "MB");

            if (file_size <= 5) {
                return 1;
            } else {
                if (file_size > 5) {
                    if (file_size >= 10) {
                        return 4;
                    } else {
                        return 3;
                    }
                }
            }
            return 1;
        }

        private static void unzip(final String source, final String destination) {
            try (ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(source)))) {
                ZipEntry zipEntry;
                final byte[] buffer = new byte[8192];
                while ((zipEntry = inputStream.getNextEntry()) != null) {
                    final File file = new File(destination, zipEntry.getName());
                    final File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException(
                                "Failed to ensure directory: " + dir.getAbsolutePath());
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
                Log.e(TAG, "An issue has occurred while attempting to decompress this archive.");
            }
        }

        private static void CopyStream(final InputStream Input, final OutputStream Output) throws IOException {
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
            if (!BootAnimations.this.isAdded()) return;
            BootAnimations.this.startApply();
        }
    }
}