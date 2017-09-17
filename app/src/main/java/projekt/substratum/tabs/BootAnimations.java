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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.tabs.BootAnimationManager;
import projekt.substratum.util.files.Root;
import projekt.substratum.util.tabs.BootAnimationUtils;

import static projekt.substratum.InformationActivity.currentShownLunchBar;

public class BootAnimations extends Fragment {

    private static final String bootanimationsDir = "bootanimation";
    private static final int ANIMATION_FRAME_DURATION = 40;
    private static final String TAG = "BootAnimationUtils";
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
    private boolean paused = false;
    private JobReceiver jobReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private Boolean encrypted = false;
    private Cipher cipher = null;

    private HandlerThread previewHandlerThread = new HandlerThread("BootAnimationPreviewThread");
    private Handler previewHandler;
    private Runnable previewRunnable;
    private List<String> previewImages;
    private int previewIndex;
    private BitmapFactory.Options options = new BitmapFactory.Options();

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        theme_pid = getArguments().getString("theme_pid");
        byte[] encryption_key = getArguments().getByteArray("encryption_key");
        byte[] iv_encrypt_key = getArguments().getByteArray("iv_encrypt_key");

        // encrypted = encryption_key != null && iv_encrypt_key != null;

        if (encrypted) {
            try {
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(
                        Cipher.DECRYPT_MODE,
                        new SecretKeySpec(encryption_key, "AES"),
                        new IvParameterSpec(iv_encrypt_key)
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.tab_bootanimations, container,
                false);
        nsv = root.findViewById(R.id.nestedScrollView);

        bootAnimationPreview = root.findViewById(R.id.bootAnimationPreview);
        previewHandlerThread.start();

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        progressBar = root.findViewById(R.id.progress_bar_loader);

        bootanimation_placeholder = root.findViewById(R.id.bootanimation_placeholder);
        defaults = root.findViewById(R.id.restore_to_default);

        try {
            // Parses the list of items in the boot animation folder
            Resources themeResources =
                    getContext().getPackageManager().getResourcesForApplication(theme_pid);
            themeAssetManager = themeResources.getAssets();
            String[] fileArray = themeAssetManager.list(bootanimationsDir);
            ArrayList<String> unparsedBootAnimations = new ArrayList<>();
            Collections.addAll(unparsedBootAnimations, fileArray);

            // Creates the list of dropdown items
            ArrayList<String> parsedBootAnimations = new ArrayList<>();
            parsedBootAnimations.add(getString(R.string.bootanimation_default_spinner));
            parsedBootAnimations.add(getString(R.string.bootanimation_spinner_set_defaults));
            for (int i = 0; i < unparsedBootAnimations.size(); i++) {
                parsedBootAnimations.add(unparsedBootAnimations.get(i).substring(0,
                        unparsedBootAnimations.get(i).length() - (encrypted ? 8 : 4)));
            }

            ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, parsedBootAnimations);
            bootAnimationSelector = root.findViewById(R.id.bootAnimationSelection);
            bootAnimationSelector.setAdapter(adapter1);
            bootAnimationSelector.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> arg0, View arg1,
                                                   int pos, long id) {
                            switch (pos) {
                                case 0:
                                    if (current != null) current.cancel(true);
                                    if (previewHandler != null && previewRunnable != null) {
                                        previewHandler.removeCallbacks(previewRunnable);
                                    }
                                    bootanimation_placeholder.setVisibility(View.VISIBLE);
                                    defaults.setVisibility(View.GONE);
                                    bootAnimationPreview.setImageDrawable(null);
                                    bootAnimationPreview.setVisibility(View.GONE);
                                    progressBar.setVisibility(View.GONE);
                                    paused = true;
                                    break;
                                case 1:
                                    if (current != null) current.cancel(true);
                                    if (previewHandler != null && previewRunnable != null) {
                                        previewHandler.removeCallbacks(previewRunnable);
                                    }
                                    defaults.setVisibility(View.VISIBLE);
                                    bootanimation_placeholder.setVisibility(View.GONE);
                                    progressBar.setVisibility(View.GONE);
                                    bootAnimationPreview.setImageDrawable(null);
                                    bootAnimationPreview.setVisibility(View.GONE);
                                    progressBar.setVisibility(View.GONE);
                                    paused = false;
                                    break;
                                default:
                                    if (current != null) current.cancel(true);
                                    bootAnimationPreview.setVisibility(View.VISIBLE);
                                    defaults.setVisibility(View.GONE);
                                    bootanimation_placeholder.setVisibility(View.GONE);
                                    String[] commands = {arg0.getSelectedItem().toString()};
                                    if (previewHandler != null && previewRunnable != null) {
                                        previewHandler.removeCallbacks(previewRunnable);
                                    }
                                    current = new BootAnimationPreview().execute(commands);
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> arg0) {
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "There is no bootanimation.zip found within the assets of this theme!");
        }

        // Enable job listener
        jobReceiver = new JobReceiver();
        IntentFilter intentFilter = new IntentFilter("BootAnimations.START_JOB");
        localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        localBroadcastManager.registerReceiver(jobReceiver, intentFilter);

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            localBroadcastManager.unregisterReceiver(jobReceiver);
        } catch (IllegalArgumentException e) {
            // Unregistered already
        }
    }

    public void startApply() {
        if (!paused) {
            if (References.getDeviceEncryptionStatus(getContext()) <= 1 ||
                    !References.checkOMS(getContext())) {
                if (bootAnimationSelector.getSelectedItemPosition() == 1) {
                    new BootAnimationClearer().execute("");
                } else {
                    new BootAnimationUtils().execute(nsv,
                            bootAnimationSelector.getSelectedItem().toString(),
                            getContext(), theme_pid, encrypted, cipher);
                }
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.root_required_title)
                        .setMessage(R.string.root_required_boot_animation)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            if (Root.requestRootAccess()) {
                                if (bootAnimationSelector.getSelectedItemPosition() == 1) {
                                    new BootAnimationClearer().execute("");
                                } else {
                                    new BootAnimationUtils().execute(nsv,
                                            bootAnimationSelector.getSelectedItem().toString(),
                                            getContext(), theme_pid, encrypted, cipher);
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel,
                                (dialog, which) -> dialog.cancel())
                        .show();
            }
        }
    }

    private class BootAnimationClearer extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(getActivity(), R.style.RestoreDialog);
            mProgressDialog.setMessage(getString(R.string.manage_dialog_performing));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mProgressDialog.dismiss();
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("bootanimation_applied");
            editor.apply();
            if (getView() != null) {
                currentShownLunchBar = Lunchbar.make(getView(),
                        getString(R.string.manage_bootanimation_toast),
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            BootAnimationManager.clearBootAnimation(getContext());
            return null;
        }
    }

    private class BootAnimationPreview extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            FileOperations.delete(getContext(), getContext().getCacheDir().getAbsolutePath() +
                    "/BootAnimationCache/animation_preview/");
            paused = true;
            bootAnimationPreview.setImageDrawable(null);
            progressBar.setVisibility(View.VISIBLE);
            previewImages = new ArrayList<>();
            previewHandler = new Handler(previewHandlerThread.getLooper());
            previewIndex = 0;
            previewRunnable = () -> {
                try {
                    Bitmap bmp = BitmapFactory.decodeFile(previewImages.get(previewIndex), options);
                    getActivity().runOnUiThread(() -> {
                        bootAnimationPreview.setImageBitmap(bmp);
                        previewIndex++;
                        if (previewIndex == previewImages.size()) previewIndex = 0;
                    });
                    previewHandler.postDelayed(previewRunnable, ANIMATION_FRAME_DURATION);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                Log.d(TAG, "Loaded boot animation contains " + previewImages.size() + " frames.");
                if (bootAnimationSelector.getSelectedItemPosition() > 1) {
                    Log.d(TAG, "Displaying bootanimation after render task complete!");
                    previewHandler.post(previewRunnable);
                }
                progressBar.setVisibility(View.GONE);
                paused = false;
            } catch (Exception e) {
                Log.e(TAG, "Window was destroyed before AsyncTask could perform postExecute()");
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            try {
                File cacheDirectory = new File(getContext().getCacheDir(), "/BootAnimationCache/");
                if (!cacheDirectory.exists() && cacheDirectory.mkdirs()) {
                    Log.d(TAG, "Bootanimation folder created");
                }
                File cacheDirectory2 = new File(getContext().getCacheDir(),
                        "/BootAnimationCache/animation_preview/");
                if (!cacheDirectory2.exists() && cacheDirectory2.mkdirs()) {
                    Log.d(TAG, "Bootanimation work folder created");
                } else {
                    FileOperations.delete(getContext(),
                            getContext().getCacheDir().getAbsolutePath() +
                                    "/BootAnimationCache/animation_preview/");
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d(TAG, "Bootanimation folder recreated");
                }

                // Copy the bootanimation.zip from assets/bootanimation of the theme's assets
                String source = sUrl[0] + ".zip";

                if (encrypted) {
                    FileOperations.copyFileOrDir(
                            themeAssetManager,
                            bootanimationsDir + "/" + source + ".enc",
                            getContext().getCacheDir().getAbsolutePath() +
                                    "/BootAnimationCache/" + source,
                            bootanimationsDir + "/" + source + ".enc",
                            cipher);
                } else {
                    try (InputStream inputStream = themeAssetManager.open(
                            bootanimationsDir + "/" + source);
                         OutputStream outputStream =
                                 new FileOutputStream(getContext().getCacheDir().getAbsolutePath() +
                                         "/BootAnimationCache/" + source)) {
                        CopyStream(inputStream, outputStream);
                    } catch (Exception e) {
                        Log.e(TAG,
                                "There is no bootanimation.zip found within the assets of " +
                                        "this theme!");
                    }
                }

                // Unzip the boot animation to get it prepared for the preview
                unzip(getContext().getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/" + source,
                        getContext().getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/animation_preview/");

                options.inPreferredConfig = Bitmap.Config.RGB_565;
                options.inSampleSize = previewDeterminator(
                        getContext().getCacheDir().getAbsolutePath() + "/BootAnimationCache/" +
                                source);

                // List images files in directory
                File previewDirectory = new File(getContext().getCacheDir(),
                        "/BootAnimationCache/animation_preview/");
                String[] dirs = previewDirectory.list();
                String[] supportedFile = {"jpg", "png", "jpeg"};
                for (String content : dirs) {
                    File current = new File(previewDirectory, content);
                    if (current.isDirectory()) {
                        String[] currentDirs = current.list();
                        for (String currentDirContent : currentDirs) {
                            File probablyImage = new File(current, currentDirContent);
                            for (String ext : supportedFile) {
                                if (probablyImage.getName().toLowerCase().endsWith(ext)) {
                                    previewImages.add(probablyImage.getAbsolutePath());
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Unexpectedly lost connection to the application host");
            }
            return null;
        }

        private int previewDeterminator(String file_location) {
            File checkFile = new File(file_location);
            int file_size = Integer.parseInt(String.valueOf(checkFile.length() / 1024 / 1024));
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

        private void unzip(String source, String destination) {
            try (ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(source)))) {
                ZipEntry zipEntry;
                int count;
                byte[] buffer = new byte[8192];
                while ((zipEntry = inputStream.getNextEntry()) != null) {
                    File file = new File(destination, zipEntry.getName());
                    File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException(
                                "Failed to ensure directory: " + dir.getAbsolutePath());
                    if (zipEntry.isDirectory())
                        continue;
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        while ((count = inputStream.read(buffer)) != -1)
                            outputStream.write(buffer, 0, count);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "An issue has occurred while attempting to decompress this archive.");
            }
        }

        private void CopyStream(InputStream Input, OutputStream Output) throws IOException {
            byte[] buffer = new byte[5120];
            int length = Input.read(buffer);
            while (length > 0) {
                Output.write(buffer, 0, length);
                length = Input.read(buffer);
            }
        }
    }

    class JobReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isAdded()) return;
            startApply();
        }
    }
}