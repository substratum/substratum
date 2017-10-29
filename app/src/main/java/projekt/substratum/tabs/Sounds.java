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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

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
import projekt.substratum.adapters.tabs.sounds.SoundsAdapter;
import projekt.substratum.adapters.tabs.sounds.SoundsInfo;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.util.tabs.SoundUtils;
import projekt.substratum.util.views.RecyclerItemClickListener;

import static projekt.substratum.InformationActivity.currentShownLunchBar;
import static projekt.substratum.common.Internal.BYTE_ACCESS_RATE;
import static projekt.substratum.common.Internal.ENCRYPTED_FILE_EXTENSION;
import static projekt.substratum.common.Internal.SOUNDS_APPLIED;
import static projekt.substratum.common.Internal.SOUNDS_CACHE;
import static projekt.substratum.common.Internal.SOUNDS_PREVIEW_CACHE;
import static projekt.substratum.common.Internal.START_JOB_ACTION;
import static projekt.substratum.common.Internal.THEME_PID;
import static projekt.substratum.util.tabs.SoundUtils.finishReceiver;

public class Sounds extends Fragment {

    private static final String soundsDir = "audio";
    private static final String TAG = "SoundUtils";
    private static final Boolean encrypted = false;
    private final MediaPlayer mp = new MediaPlayer();
    @BindView(R.id.nestedScrollView)
    NestedScrollView nsv;
    @BindView(R.id.progress_bar_loader)
    ProgressBar progressBar;
    @BindView(R.id.restore_to_default)
    RelativeLayout defaults;
    @BindView(R.id.sounds_placeholder)
    RelativeLayout sounds_preview;
    @BindView(R.id.error_loading_pack)
    RelativeLayout error;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.soundsSelection)
    Spinner soundsSelector;
    @BindView(R.id.relativeLayout)
    RelativeLayout relativeLayout;
    private String theme_pid;
    private ArrayList<SoundsInfo> wordList;
    private int previous_position;
    private SharedPreferences prefs;
    private AsyncTask current;
    private AssetManager themeAssetManager;
    private boolean paused;
    private JobReceiver jobReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private Context mContext;

    private Sounds getInstance() {
        return this;
    }

    @Override
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        mContext = getContext();
        View view = inflater.inflate(R.layout.tab_sounds, container, false);
        ButterKnife.bind(this, view);

        if (getArguments() != null) {
            theme_pid = getArguments().getString(THEME_PID);
        } else {
            // At this point, the tab has been incorrectly loaded
            return null;
        }

        progressBar.setVisibility(View.GONE);
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        error.setVisibility(View.GONE);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        final ArrayList<SoundsInfo> empty_array = new ArrayList<>();
        final RecyclerView.Adapter empty_adapter = new SoundsAdapter(empty_array);
        recyclerView.setAdapter(empty_adapter);

        try {
            // Parses the list of items in the sounds folder
            final Resources themeResources =
                    mContext.getPackageManager().getResourcesForApplication(theme_pid);
            themeAssetManager = themeResources.getAssets();
            final String[] fileArray = themeAssetManager.list(soundsDir);
            final List<String> archivedSounds = new ArrayList<>();
            Collections.addAll(archivedSounds, fileArray);

            // Creates the list of dropdown items
            final ArrayList<String> unarchivedSounds = new ArrayList<>();
            unarchivedSounds.add(getString(R.string.sounds_default_spinner));
            unarchivedSounds.add(getString(R.string.sounds_spinner_set_defaults));
            for (int i = 0; i < archivedSounds.size(); i++) {
                unarchivedSounds.add(archivedSounds.get(i).substring(0,
                        archivedSounds.get(i).length() - (encrypted ? 8 : 4)));
            }

            assert getActivity() != null;
            final SpinnerAdapter adapter1 = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, unarchivedSounds);
            soundsSelector.setAdapter(adapter1);
            soundsSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(
                        final AdapterView<?> arg0,
                        final View arg1,
                        final int pos,
                        final long id) {
                    switch (pos) {
                        case 0:
                            if (current != null) current.cancel(true);
                            defaults.setVisibility(View.GONE);
                            error.setVisibility(View.GONE);
                            relativeLayout.setVisibility(View.GONE);
                            sounds_preview.setVisibility(View.VISIBLE);
                            paused = true;
                            break;

                        case 1:
                            if (current != null) current.cancel(true);
                            defaults.setVisibility(View.VISIBLE);
                            error.setVisibility(View.GONE);
                            relativeLayout.setVisibility(View.GONE);
                            sounds_preview.setVisibility(View.GONE);
                            paused = false;
                            break;

                        default:
                            if (current != null) current.cancel(true);
                            defaults.setVisibility(View.GONE);
                            error.setVisibility(View.GONE);
                            sounds_preview.setVisibility(View.GONE);
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
                new RecyclerItemClickListener(mContext, (v, position) -> {
                    wordList.get(position);
                    try {
                        if (!mp.isPlaying() || (position != previous_position)) {
                            stopPlayer();
                            ((ImageButton)
                                    v.findViewById(R.id.play)).setImageResource(
                                    R.drawable.sounds_preview_stop);
                            mp.setDataSource(wordList.get(position).getAbsolutePath());
                            mp.prepare();
                            mp.start();
                        } else {
                            stopPlayer();
                        }
                        previous_position = position;
                    } catch (final IOException ioe) {
                        Log.e(TAG, "Playback has failed for " + wordList.get(position).getTitle());
                    }
                })
        );
        mp.setOnCompletionListener(mediaPlayer -> stopPlayer());

        // Enable job listener
        jobReceiver = new JobReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
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
                        nsv,
                        soundsSelector.getSelectedItem().toString(),
                        mContext,
                        theme_pid,
                        null);
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
                mContext.getApplicationContext().unregisterReceiver(finishReceiver);
            }
            localBroadcastManager.unregisterReceiver(jobReceiver);
        } catch (final IllegalArgumentException e) {
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
                currentShownLunchBar = Lunchbar.make(sounds.nsv,
                        sounds.getString(R.string.manage_sounds_toast),
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final Sounds sounds = ref.get();
            if (sounds != null) {
                SoundUtils.SoundsClearer(sounds.mContext);
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
                    final List<SoundsInfo> adapter1 = new ArrayList<>(sounds.wordList);

                    if (!adapter1.isEmpty()) {
                        final SoundsAdapter mAdapter = new SoundsAdapter(adapter1);
                        final RecyclerView.LayoutManager mLayoutManager =
                                new LinearLayoutManager(sounds.mContext);

                        sounds.recyclerView.setLayoutManager(mLayoutManager);
                        sounds.recyclerView.setItemAnimator(new DefaultItemAnimator());
                        sounds.recyclerView.setAdapter(mAdapter);
                        sounds.paused = false;
                    } else {
                        sounds.recyclerView.setVisibility(View.GONE);
                        sounds.relativeLayout.setVisibility(View.GONE);
                        sounds.error.setVisibility(View.VISIBLE);
                    }
                    sounds.progressBar.setVisibility(View.GONE);
                } catch (final Exception e) {
                    Log.e(TAG, "Window was destroyed before AsyncTask could perform postExecute()");
                }
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final Sounds sounds = ref.get();
            if (sounds != null) {
                try {
                    final File cacheDirectory = new File(sounds.mContext.getCacheDir(),
                            SOUNDS_CACHE);
                    if (!cacheDirectory.exists() && cacheDirectory.mkdirs()) {
                        Log.d(TAG, "Sounds folder created");
                    }
                    final File cacheDirectory2 = new File(sounds.mContext.getCacheDir(),
                            SOUNDS_PREVIEW_CACHE);
                    if (!cacheDirectory2.exists() && cacheDirectory2.mkdirs()) {
                        Log.d(TAG, "Sounds work folder created");
                    } else {
                        FileOperations.delete(sounds.mContext,
                                sounds.mContext.getCacheDir().getAbsolutePath() +
                                        SOUNDS_PREVIEW_CACHE);
                        final boolean created = cacheDirectory2.mkdirs();
                        if (created) Log.d(TAG, "Sounds folder recreated");
                    }

                    // Copy the sounds.zip from assets/sounds of the theme's assets

                    final String source = sUrl[0] + ".zip";
                    if (encrypted) {
                        FileOperations.copyFileOrDir(
                                sounds.themeAssetManager,
                                soundsDir + '/' + source + ENCRYPTED_FILE_EXTENSION,
                                sounds.mContext.getCacheDir().getAbsolutePath() +
                                        SOUNDS_CACHE + source,
                                soundsDir + '/' + source + ENCRYPTED_FILE_EXTENSION,
                                null);
                    } else {
                        try (InputStream inputStream = sounds.themeAssetManager.open(
                                soundsDir + '/' + source);
                             OutputStream outputStream =
                                     new FileOutputStream(
                                             sounds.mContext.getCacheDir().getAbsolutePath() +
                                                     SOUNDS_CACHE + source)) {
                            SoundsPreview.CopyStream(inputStream, outputStream);
                        } catch (final Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "There is no sounds.zip found within the assets " +
                                    "of this theme!");

                        }
                    }

                    // Unzip the sounds archive to get it prepared for the preview
                    SoundsPreview.unzip(sounds.mContext.getCacheDir().
                                    getAbsolutePath() + SOUNDS_CACHE + source,
                            sounds.mContext.getCacheDir().getAbsolutePath() + SOUNDS_PREVIEW_CACHE);

                    sounds.wordList = new ArrayList<>();
                    final File testDirectory =
                            new File(sounds.mContext.getCacheDir().getAbsolutePath() +
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
                            sounds.wordList.add(new SoundsInfo(sounds.mContext, fileEntry.getName(),
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