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
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import projekt.substratum.R;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.util.files.Root;
import projekt.substratum.util.tabs.BootAnimationUtils;
import projekt.substratum.util.views.Lunchbar;

import static projekt.substratum.InformationActivity.currentShownLunchBar;
import static projekt.substratum.common.Internal.BOOTANIMATION_CACHE;
import static projekt.substratum.common.Internal.BOOTANIMATION_PREVIEW_CACHE;
import static projekt.substratum.common.Internal.BOOT_ANIMATION_APPLIED;
import static projekt.substratum.common.Internal.BYTE_ACCESS_RATE;
import static projekt.substratum.common.Internal.ENCRYPTED_FILE_EXTENSION;
import static projekt.substratum.common.Internal.SHUTDOWNANIMATION_INTENT;
import static projekt.substratum.common.Internal.SHUTDOWN_ANIMATION_APPLIED;
import static projekt.substratum.common.Internal.START_JOB_ACTION;
import static projekt.substratum.common.Internal.THEME_PID;

public class BootAnimations extends Fragment {

    private static final int ANIMATION_FRAME_DURATION = 40;
    private static final String TAG = "BootAnimationUtils";
    private static final Boolean encrypted = false;
    private static String bootanimationsDir = "bootanimation";
    private final HandlerThread previewHandlerThread =
            new HandlerThread("BootAnimationPreviewThread");
    private final BitmapFactory.Options options = new BitmapFactory.Options();
    @BindView(R.id.nestedScrollView)
    NestedScrollView nsv;
    @BindView(R.id.bootAnimationPreview)
    ImageView bootAnimationPreview;
    @BindView(R.id.progress_bar_loader)
    ProgressBar progressBar;
    @BindView(R.id.bootanimation_placeholder)
    RelativeLayout bootanimation_placeholder;
    @BindView(R.id.restore_to_default)
    RelativeLayout defaults;
    @BindView(R.id.bootanimation_placeholder_text)
    TextView placeholderText;
    @BindView(R.id.restore_to_default_text)
    TextView restoreText;
    @BindView(R.id.bootAnimationSelection)
    Spinner bootAnimationSelector;
    private String theme_pid;
    private ProgressDialog mProgressDialog;
    private SharedPreferences prefs;
    private AsyncTask current;
    private AssetManager themeAssetManager;
    private boolean paused;
    private JobReceiver jobReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private Boolean shutdownBootAnimation;
    private Context context;
    private Handler previewHandler;
    private Runnable previewRunnable;
    private List<String> previewImages;
    private int previewIndex;

    private BootAnimations getInstance() {
        return this;
    }

    @Override
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        context = getContext();
        View view = inflater.inflate(R.layout.tab_bootanimations, container, false);
        ButterKnife.bind(this, view);

        if (getArguments() != null) {
            theme_pid = getArguments().getString(THEME_PID);

            // If this tab was launched with shutdown animations in mind, rename the tab
            if (getArguments().getBoolean(SHUTDOWNANIMATION_INTENT, false)) {
                bootanimationsDir = SHUTDOWNANIMATION_INTENT;
                shutdownBootAnimation = true;
            } else {
                shutdownBootAnimation = false;
            }
        } else {
            // At this point, the tab has been incorrectly loaded
            return null;
        }

        previewHandlerThread.start();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (shutdownBootAnimation) {
            placeholderText.setText(R.string.shutdownanimation_placeholder_text);
            restoreText.setText(R.string.shutdownanimation_defaults_text);
        }

        try {
            // Parses the list of items in the boot animation folder
            final Resources themeResources =
                    context.getPackageManager().getResourcesForApplication(theme_pid);
            themeAssetManager = themeResources.getAssets();
            final String[] fileArray = themeAssetManager.list(bootanimationsDir);
            final List<String> unparsedBootAnimations = new ArrayList<>();
            Collections.addAll(unparsedBootAnimations, fileArray);

            // Creates the list of dropdown items
            final ArrayList<String> parsedBootAnimations = new ArrayList<>();
            parsedBootAnimations.add(getString(shutdownBootAnimation ?
                    R.string.shutdownanimation_default_spinner :
                    R.string.bootanimation_default_spinner));
            parsedBootAnimations.add(getString(shutdownBootAnimation ?
                    R.string.shutdownanimation_spinner_set_defaults :
                    R.string.bootanimation_spinner_set_defaults));
            for (int i = 0; i < unparsedBootAnimations.size(); i++) {
                parsedBootAnimations.add(unparsedBootAnimations.get(i).substring(0,
                        unparsedBootAnimations.get(i).length() - (encrypted ? 8 : 4)));
            }

            assert getActivity() != null;
            final SpinnerAdapter adapter1 = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, parsedBootAnimations);
            bootAnimationSelector.setAdapter(adapter1);
            bootAnimationSelector.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(final AdapterView<?> arg0,
                                                   final View arg1,
                                                   final int pos,
                                                   final long id) {
                            switch (pos) {
                                case 0:
                                    if (current != null) current.cancel(true);
                                    if ((previewHandler != null) && (previewRunnable != null)) {
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
                                    if ((previewHandler != null) && (previewRunnable != null)) {
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
                                    final String[] commands = {arg0.getSelectedItem().toString()};
                                    if ((previewHandler != null) && (previewRunnable != null)) {
                                        previewHandler.removeCallbacks(previewRunnable);
                                    }
                                    current = new BootAnimationPreview(getInstance())
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
        jobReceiver = new JobReceiver();
        final IntentFilter intentFilter = new IntentFilter(
                (shutdownBootAnimation ? "ShutdownAnimations" : "BootAnimations") +
                        START_JOB_ACTION);
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(jobReceiver, intentFilter);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            localBroadcastManager.unregisterReceiver(jobReceiver);
        } catch (final IllegalArgumentException e) {
            // Unregistered already
        }
    }

    /**
     * Check if this tab was loaded as a shutdown animation
     *
     * @return True, if shutdown animation
     */
    public boolean isShutdownTab() {
        return shutdownBootAnimation;
    }

    /**
     * Apply the boot animation
     */
    private void startApply() {
        if (!paused) {
            if (((Systems.getDeviceEncryptionStatus(context) <= 1) ||
                    shutdownBootAnimation) ||
                    !Systems.checkOMS(context)) {
                if (bootAnimationSelector.getSelectedItemPosition() == 1) {
                    new BootAnimationClearer(this).execute("");
                } else {
                    BootAnimationUtils.execute(nsv,
                            bootAnimationSelector.getSelectedItem().toString(),
                            context,
                            theme_pid,
                            encrypted,
                            shutdownBootAnimation,
                            null);
                }
            } else {
                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.root_required_title)
                        .setMessage(R.string.root_required_boot_animation)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            if (Root.requestRootAccess()) {
                                if (bootAnimationSelector.getSelectedItemPosition() == 1) {
                                    new BootAnimationClearer(this).execute("");
                                } else {
                                    BootAnimationUtils.execute(nsv,
                                            bootAnimationSelector.getSelectedItem().toString(),
                                            context,
                                            theme_pid,
                                            encrypted,
                                            shutdownBootAnimation,
                                            null);
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel,
                                (dialog, which) -> dialog.cancel())
                        .show();
            }
        }
    }

    /**
     * Clears the currently applied boot animation
     */
    private static class BootAnimationClearer extends AsyncTask<String, Integer, String> {
        private final WeakReference<BootAnimations> ref;

        BootAnimationClearer(final BootAnimations bootAnimations) {
            super();
            ref = new WeakReference<>(bootAnimations);
        }

        @Override
        protected void onPreExecute() {
            final BootAnimations bootAnimations = ref.get();
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
            final BootAnimations bootAnimations = ref.get();
            if (bootAnimations != null) {
                bootAnimations.mProgressDialog.dismiss();
                final SharedPreferences.Editor editor = bootAnimations.prefs.edit();
                if (bootAnimations.shutdownBootAnimation) {
                    editor.remove(SHUTDOWN_ANIMATION_APPLIED);
                } else {
                    editor.remove(BOOT_ANIMATION_APPLIED);
                }
                editor.apply();
                if (bootAnimations.getView() != null) {
                    currentShownLunchBar = Lunchbar.make(bootAnimations.getView(),
                            bootAnimations.getString(bootAnimations.shutdownBootAnimation ?
                                    R.string.manage_shutdownanimation_toast :
                                    R.string.manage_bootanimation_toast),
                            Snackbar.LENGTH_LONG);
                    currentShownLunchBar.show();
                }
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final BootAnimations bootAnimations = ref.get();
            if (bootAnimations != null) {
                BootAnimationsManager.clearBootAnimation(bootAnimations.context,
                        bootAnimations.shutdownBootAnimation);
            }
            return null;
        }
    }

    /**
     * Load up the preview for the boot animation
     */
    private static class BootAnimationPreview extends AsyncTask<String, Integer, String> {
        private final WeakReference<BootAnimations> ref;

        BootAnimationPreview(final BootAnimations bootAnimations) {
            super();
            ref = new WeakReference<>(bootAnimations);
        }

        private static int previewDeterminator(final String file_location) {
            final File checkFile = new File(file_location);
            final int file_size = Integer.parseInt(
                    String.valueOf(checkFile.length() / 1024L / 1024L));
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
                final byte[] buffer = new byte[BYTE_ACCESS_RATE];
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
            final BootAnimations bootAnimations = ref.get();
            if (bootAnimations != null) {
                FileOperations.delete(
                        bootAnimations.context,
                        bootAnimations.context.getCacheDir().getAbsolutePath() +
                                BOOTANIMATION_PREVIEW_CACHE);
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

                        assert bootAnimations.getActivity() != null;
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
            final BootAnimations bootAnimations = ref.get();
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
            final BootAnimations bootAnimations = ref.get();
            if (bootAnimations != null) {
                try {
                    final File cacheDirectory = new File(
                            bootAnimations.context.getCacheDir(), BOOTANIMATION_CACHE);

                    if (!cacheDirectory.exists() && cacheDirectory.mkdirs()) {
                        Log.d(TAG, "Bootanimation folder created");
                    }
                    final File cacheDirectory2 = new File(bootAnimations.context.getCacheDir(),
                            BOOTANIMATION_PREVIEW_CACHE);
                    if (!cacheDirectory2.exists() && cacheDirectory2.mkdirs()) {
                        Log.d(TAG, "Bootanimation work folder created");
                    } else {
                        FileOperations.delete(bootAnimations.context,
                                bootAnimations.context.getCacheDir().getAbsolutePath() +
                                        BOOTANIMATION_PREVIEW_CACHE);
                        final boolean created = cacheDirectory2.mkdirs();
                        if (created) Log.d(TAG, "Bootanimation folder recreated");
                    }

                    // Copy the bootanimation.zip from assets/bootanimation of the theme's assets
                    final String source = sUrl[0] + ".zip";

                    if (encrypted) {
                        FileOperations.copyFileOrDir(
                                bootAnimations.themeAssetManager,
                                bootanimationsDir + '/' + source + ENCRYPTED_FILE_EXTENSION,
                                bootAnimations.context.getCacheDir().getAbsolutePath() +
                                        BOOTANIMATION_CACHE + source,
                                bootanimationsDir + '/' + source + ENCRYPTED_FILE_EXTENSION,
                                null);
                    } else {
                        try (InputStream inputStream = bootAnimations.themeAssetManager.open(
                                bootanimationsDir + '/' + source);
                             OutputStream outputStream =
                                     new FileOutputStream(bootAnimations.context.getCacheDir()
                                             .getAbsolutePath() +
                                             BOOTANIMATION_CACHE + source)) {
                            BootAnimationPreview.CopyStream(inputStream, outputStream);
                        } catch (final Exception e) {
                            Log.e(TAG,
                                    "There is no bootanimation.zip found within the assets of " +
                                            "this theme!");
                        }
                    }

                    // Unzip the boot animation to get it prepared for the preview
                    BootAnimationPreview.unzip(bootAnimations.context.getCacheDir()
                                    .getAbsolutePath() +
                                    BOOTANIMATION_CACHE + source,
                            bootAnimations.context.getCacheDir().getAbsolutePath() +
                                    BOOTANIMATION_PREVIEW_CACHE);

                    bootAnimations.options.inPreferredConfig = Bitmap.Config.RGB_565;
                    bootAnimations.options.inSampleSize = BootAnimationPreview.previewDeterminator(
                            bootAnimations.context.getCacheDir().getAbsolutePath() +
                                    BOOTANIMATION_CACHE + source);

                    // List images files in directory
                    final File previewDirectory = new File(bootAnimations.context.getCacheDir(),
                            BOOTANIMATION_PREVIEW_CACHE);
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