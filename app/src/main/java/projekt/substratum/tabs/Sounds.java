/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.tabs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.adapters.tabs.sounds.SoundsAdapter;
import projekt.substratum.adapters.tabs.sounds.SoundsItem;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.databinding.TabSoundsBinding;
import projekt.substratum.util.tabs.SoundUtils;
import projekt.substratum.util.views.Lunchbar;
import projekt.substratum.util.views.RecyclerItemClickListener;

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
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static projekt.substratum.InformationActivity.currentShownLunchBar;
import static projekt.substratum.common.Internal.BYTE_ACCESS_RATE;
import static projekt.substratum.common.Internal.ENCRYPTED_FILE_EXTENSION;
import static projekt.substratum.common.Internal.SOUNDS_APPLIED;
import static projekt.substratum.common.Internal.SOUNDS_CACHE;
import static projekt.substratum.common.Internal.SOUNDS_PREVIEW_CACHE;
import static projekt.substratum.common.Internal.START_JOB_ACTION;
import static projekt.substratum.common.Internal.THEME_PID;
import static projekt.substratum.common.References.getThemeAssetManager;
import static projekt.substratum.common.References.setThemeExtraLists;
import static projekt.substratum.util.tabs.SoundUtils.finishReceiver;

public class Sounds extends Fragment {

    private static final String soundsDir = "audio";
    private static final String TAG = "SoundUtils";
    private static final boolean encrypted = false;
    private final MediaPlayer mp = new MediaPlayer();
    private NestedScrollView nestedScrollView;
    private ProgressBar progressBar;
    private RelativeLayout restoreToDefault;
    private RelativeLayout soundsPreview;
    private RelativeLayout errorLoadingPack;
    private RecyclerView recyclerView;
    private Spinner soundsSelector;
    private RelativeLayout relativeLayout;
    private String themePid;
    private ArrayList<SoundsItem> wordList;
    private int previousPosition;
    private SharedPreferences prefs = Substratum.getPreferences();
    private AsyncTask current;
    private boolean paused;
    private JobReceiver jobReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private Context context;

    private Sounds getInstance() {
        return this;
    }

    @Override
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        context = getContext();

        TabSoundsBinding tabSoundsBinding =
                DataBindingUtil.inflate(inflater, R.layout.tab_sounds, container, false);

        View view = tabSoundsBinding.getRoot();

        nestedScrollView = tabSoundsBinding.nestedScrollView;
        progressBar = tabSoundsBinding.progressBarLoader;
        restoreToDefault = tabSoundsBinding.restoreToDefault;
        soundsPreview = tabSoundsBinding.soundsPlaceholder;
        errorLoadingPack = tabSoundsBinding.errorLoadingPack;
        recyclerView = tabSoundsBinding.recyclerView;
        soundsSelector = tabSoundsBinding.soundsSelection;
        relativeLayout = tabSoundsBinding.relativeLayout;

        if (getArguments() != null) {
            themePid = getArguments().getString(THEME_PID);
        } else {
            // At this point, the tab has been incorrectly loaded
            return null;
        }

        progressBar.setVisibility(View.GONE);
        errorLoadingPack.setVisibility(View.GONE);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        final ArrayList<SoundsItem> emptyArray = new ArrayList<>();
        final RecyclerView.Adapter emptyAdapter = new SoundsAdapter(emptyArray);
        recyclerView.setAdapter(emptyAdapter);

        try {
            soundsSelector = setThemeExtraLists(context,
                    themePid,
                    soundsDir,
                    getString(R.string.sounds_default_spinner),
                    getString(R.string.sounds_spinner_set_defaults),
                    encrypted,
                    getActivity(),
                    soundsSelector);
            if (soundsSelector == null) throw new Exception();
            soundsSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(final AdapterView<?> arg0,
                                           final View arg1,
                                           final int pos,
                                           final long id) {
                    switch (pos) {
                        case 0:
                            if (current != null) current.cancel(true);
                            restoreToDefault.setVisibility(View.GONE);
                            errorLoadingPack.setVisibility(View.GONE);
                            relativeLayout.setVisibility(View.GONE);
                            soundsPreview.setVisibility(View.VISIBLE);
                            paused = true;
                            break;

                        case 1:
                            if (current != null) current.cancel(true);
                            restoreToDefault.setVisibility(View.VISIBLE);
                            errorLoadingPack.setVisibility(View.GONE);
                            relativeLayout.setVisibility(View.GONE);
                            soundsPreview.setVisibility(View.GONE);
                            paused = false;
                            break;

                        default:
                            if (current != null) current.cancel(true);
                            restoreToDefault.setVisibility(View.GONE);
                            errorLoadingPack.setVisibility(View.GONE);
                            soundsPreview.setVisibility(View.GONE);
                            relativeLayout.setVisibility(View.VISIBLE);
                            final String[] commands = {arg0.getSelectedItem().toString()};
                            current = new SoundsPreview(getInstance())
                                    .execute(commands);
                    }
                }

                @Override
                public void onNothingSelected(final AdapterView<?> arg0) {
                }
            });
        } catch (final Exception e) {
            e.printStackTrace();
            Log.e(TAG, "There is no sounds.zip found within the assets of this theme!");
        }

        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(context, (v, position) -> {
                    final SoundsItem info = wordList.get(position);
                    try {
                        if (!mp.isPlaying() || (position != previousPosition)) {
                            stopPlayer();
                            ((ImageButton)
                                    v.findViewById(R.id.play)).setImageResource(
                                    R.drawable.sounds_preview_stop);
                            mp.setDataSource(info.getAbsolutePath());
                            mp.prepare();
                            mp.start();
                        } else {
                            stopPlayer();
                        }
                        previousPosition = position;
                    } catch (final IOException ignored) {
                        Log.e(TAG, "Playback has failed for " + info.getWorkingTitle());
                    }
                })
        );
        mp.setOnCompletionListener(mediaPlayer -> stopPlayer());

        // Enable job listener
        jobReceiver = new JobReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(jobReceiver,
                new IntentFilter(getClass().getSimpleName() + START_JOB_ACTION));

        return view;
    }

    private void startApply() {
        if (!paused) {
            if (soundsSelector.getSelectedItemPosition() == 1) {
                new SoundsClearer(this).execute("");
            } else {
                new SoundUtils().execute(
                        nestedScrollView,
                        soundsSelector.getSelectedItem().toString(),
                        context,
                        themePid
                );
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mp.release();

        // Unregister finish receiver
        try {
            if (finishReceiver != null) {
                context.getApplicationContext().unregisterReceiver(finishReceiver);
            }
            localBroadcastManager.unregisterReceiver(jobReceiver);
        } catch (final IllegalArgumentException ignored) {
            // Unregistered already
        }
    }

    /**
     * Stop the sounds previews
     */
    private void stopPlayer() {
        final int childCount = recyclerView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((ImageButton) recyclerView.getChildAt(i).findViewById(R.id.play))
                    .setImageResource(R.drawable.sounds_preview_play);
        }
        mp.reset();
    }

    /**
     * Clear the currently applied sounds
     */
    private static class SoundsClearer extends AsyncTask<String, Integer, String> {
        private final WeakReference<Sounds> ref;

        SoundsClearer(final Sounds sounds) {
            super();
            ref = new WeakReference<>(sounds);
        }

        @Override
        protected void onPostExecute(final String result) {
            final Sounds sounds = ref.get();
            if (sounds != null) {
                final SharedPreferences.Editor editor = sounds.prefs.edit();
                editor.remove(SOUNDS_APPLIED);
                editor.apply();
                currentShownLunchBar = Lunchbar.make(sounds.nestedScrollView,
                        sounds.getString(R.string.manage_sounds_toast),
                        Snackbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final Sounds sounds = ref.get();
            if (sounds != null) {
                SoundUtils.SoundsClearer(sounds.context);
            }
            return null;
        }
    }

    /**
     * Load up the preview for the sounds
     */
    private static class SoundsPreview extends AsyncTask<String, Integer, String> {
        private final WeakReference<Sounds> ref;

        SoundsPreview(final Sounds sounds) {
            super();
            ref = new WeakReference<>(sounds);
        }

        private static void unzip(final String source, final String destination) {
            try (ZipInputStream inputStream =
                         new ZipInputStream(new BufferedInputStream(new FileInputStream(source)))) {
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
                        while ((count = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, count);
                        }
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
            final Sounds sounds = ref.get();
            if (sounds != null) {
                sounds.paused = true;
                sounds.progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            final Sounds sounds = ref.get();
            if (sounds != null) {
                try {
                    final List<SoundsItem> adapter1 = new ArrayList<>(sounds.wordList);

                    if (!adapter1.isEmpty()) {
                        final SoundsAdapter mAdapter = new SoundsAdapter(adapter1);
                        final RecyclerView.LayoutManager mLayoutManager =
                                new LinearLayoutManager(sounds.context);

                        sounds.recyclerView.setLayoutManager(mLayoutManager);
                        sounds.recyclerView.setItemAnimator(new DefaultItemAnimator());
                        sounds.recyclerView.setAdapter(mAdapter);
                        sounds.paused = false;
                    } else {
                        sounds.recyclerView.setVisibility(View.GONE);
                        sounds.relativeLayout.setVisibility(View.GONE);
                        sounds.errorLoadingPack.setVisibility(View.VISIBLE);
                    }
                    sounds.progressBar.setVisibility(View.GONE);
                } catch (final Exception ignored) {
                    Log.e(TAG, "Window was destroyed before AsyncTask could perform postExecute()");
                }
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final Sounds sounds = ref.get();
            if (sounds != null) {
                try {
                    final File cacheDirectory = new File(sounds.context.getCacheDir(),
                            SOUNDS_CACHE);
                    if (!cacheDirectory.exists() && cacheDirectory.mkdirs()) {
                        Substratum.log(TAG, "Sounds folder created");
                    }
                    final File cacheDirectory2 = new File(sounds.context.getCacheDir(),
                            SOUNDS_PREVIEW_CACHE);
                    if (!cacheDirectory2.exists() && cacheDirectory2.mkdirs()) {
                        Substratum.log(TAG, "Sounds work folder created");
                    } else {
                        FileOperations.delete(sounds.context,
                                sounds.context.getCacheDir().getAbsolutePath() +
                                        SOUNDS_PREVIEW_CACHE);
                        final boolean created = cacheDirectory2.mkdirs();
                        if (created) Substratum.log(TAG, "Sounds folder recreated");
                    }

                    // Copy the sounds.zip from assets/sounds of the theme's assets

                    final String source = sUrl[0] + ".zip";
                    if (encrypted) {
                        FileOperations.copyFileOrDir(
                                Objects.requireNonNull(
                                        getThemeAssetManager(sounds.context, sounds.themePid)
                                ),
                                soundsDir + '/' + source + ENCRYPTED_FILE_EXTENSION,
                                sounds.context.getCacheDir().getAbsolutePath() +
                                        SOUNDS_CACHE + source,
                                soundsDir + '/' + source + ENCRYPTED_FILE_EXTENSION,
                                null);
                    } else {
                        try (InputStream inputStream =
                                     Objects.requireNonNull(
                                             getThemeAssetManager(sounds.context, sounds.themePid)
                                     ).open(soundsDir + '/' + source);
                             OutputStream outputStream =
                                     new FileOutputStream(
                                             sounds.context.getCacheDir().getAbsolutePath() +
                                                     SOUNDS_CACHE + source)) {
                            SoundsPreview.CopyStream(inputStream, outputStream);
                        } catch (final Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "There is no sounds.zip found within the assets " +
                                    "of this theme!");

                        }
                    }

                    // Unzip the sounds archive to get it prepared for the preview
                    SoundsPreview.unzip(sounds.context.getCacheDir().
                                    getAbsolutePath() + SOUNDS_CACHE + source,
                            sounds.context.getCacheDir().getAbsolutePath() + SOUNDS_PREVIEW_CACHE);

                    sounds.wordList = new ArrayList<>();
                    final File testDirectory =
                            new File(sounds.context.getCacheDir().getAbsolutePath() +
                                    SOUNDS_PREVIEW_CACHE);
                    listFilesForFolder(testDirectory);
                } catch (final Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Unexpectedly lost connection to the application host");
                }
            }
            return null;
        }

        void listFilesForFolder(final File folder) {
            final Sounds sounds = ref.get();
            if (sounds != null) {
                for (final File fileEntry : folder.listFiles()) {
                    if (fileEntry.isDirectory()) {
                        listFilesForFolder(fileEntry);
                    } else {
                        if (!".".equals(fileEntry.getName().substring(0, 1)) &&
                                projekt.substratum.common.Resources.allowedSounds(
                                        fileEntry.getName())) {
                            sounds.wordList.add(new SoundsItem(sounds.context, fileEntry.getName(),
                                    fileEntry.getAbsolutePath()));
                        }
                    }
                }
            }
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