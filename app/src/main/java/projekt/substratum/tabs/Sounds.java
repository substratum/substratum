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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import projekt.substratum.R;
import projekt.substratum.adapters.tabs.sounds.SoundsAdapter;
import projekt.substratum.adapters.tabs.sounds.SoundsInfo;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.util.tabs.SoundUtils;
import projekt.substratum.util.views.RecyclerItemClickListener;

import static projekt.substratum.InformationActivity.currentShownLunchBar;
import static projekt.substratum.util.tabs.SoundUtils.finishReceiver;

public class Sounds extends Fragment {

    private static final String soundsDir = "audio";
    private static final String TAG = "SoundUtils";
    private String theme_pid;
    private ViewGroup root;
    private ProgressBar progressBar;
    private Spinner soundsSelector;
    private ArrayList<SoundsInfo> wordList;
    private RecyclerView recyclerView;
    private final MediaPlayer mp = new MediaPlayer();
    private int previous_position;
    private RelativeLayout relativeLayout, error;
    private RelativeLayout defaults;
    private SharedPreferences prefs;
    private AsyncTask current;
    private NestedScrollView nsv;
    private AssetManager themeAssetManager;
    private boolean paused;
    private JobReceiver jobReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private final Boolean encrypted = false;
    private Cipher cipher;
    private Context mContext;

    public Sounds getInstance() {
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

        this.root = (ViewGroup) inflater.inflate(R.layout.tab_sounds, container, false);
        this.nsv = this.root.findViewById(R.id.nestedScrollView);

        this.progressBar = this.root.findViewById(R.id.progress_bar_loader);
        this.progressBar.setVisibility(View.GONE);

        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        this.defaults = this.root.findViewById(R.id.restore_to_default);

        final RelativeLayout sounds_preview = this.root.findViewById(R.id.sounds_placeholder);
        this.relativeLayout = this.root.findViewById(R.id.relativeLayout);
        this.error = this.root.findViewById(R.id.error_loading_pack);
        this.error.setVisibility(View.GONE);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        this.recyclerView = this.root.findViewById(R.id.recycler_view);
        this.recyclerView.setHasFixedSize(true);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(this.mContext));
        final ArrayList<SoundsInfo> empty_array = new ArrayList<>();
        final RecyclerView.Adapter empty_adapter = new SoundsAdapter(empty_array);
        this.recyclerView.setAdapter(empty_adapter);

        try {
            // Parses the list of items in the sounds folder
            final Resources themeResources =
                    this.mContext.getPackageManager().getResourcesForApplication(this.theme_pid);
            this.themeAssetManager = themeResources.getAssets();
            final String[] fileArray = this.themeAssetManager.list(soundsDir);
            final List<String> archivedSounds = new ArrayList<>();
            Collections.addAll(archivedSounds, fileArray);

            // Creates the list of dropdown items
            final ArrayList<String> unarchivedSounds = new ArrayList<>();
            unarchivedSounds.add(this.getString(R.string.sounds_default_spinner));
            unarchivedSounds.add(this.getString(R.string.sounds_spinner_set_defaults));
            for (int i = 0; i < archivedSounds.size(); i++) {
                unarchivedSounds.add(archivedSounds.get(i).substring(0,
                        archivedSounds.get(i).length() - (this.encrypted ? 8 : 4)));
            }

            final SpinnerAdapter adapter1 = new ArrayAdapter<>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, unarchivedSounds);
            this.soundsSelector = this.root.findViewById(R.id.soundsSelection);
            this.soundsSelector.setAdapter(adapter1);
            this.soundsSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(final AdapterView<?> arg0, final View arg1, final int pos, final long id) {
                    switch (pos) {
                        case 0:
                            if (Sounds.this.current != null) Sounds.this.current.cancel(true);
                            Sounds.this.defaults.setVisibility(View.GONE);
                            Sounds.this.error.setVisibility(View.GONE);
                            Sounds.this.relativeLayout.setVisibility(View.GONE);
                            sounds_preview.setVisibility(View.VISIBLE);
                            Sounds.this.paused = true;
                            break;

                        case 1:
                            if (Sounds.this.current != null) Sounds.this.current.cancel(true);
                            Sounds.this.defaults.setVisibility(View.VISIBLE);
                            Sounds.this.error.setVisibility(View.GONE);
                            Sounds.this.relativeLayout.setVisibility(View.GONE);
                            sounds_preview.setVisibility(View.GONE);
                            Sounds.this.paused = false;
                            break;

                        default:
                            if (Sounds.this.current != null) Sounds.this.current.cancel(true);
                            Sounds.this.defaults.setVisibility(View.GONE);
                            Sounds.this.error.setVisibility(View.GONE);
                            sounds_preview.setVisibility(View.GONE);
                            Sounds.this.relativeLayout.setVisibility(View.VISIBLE);
                            final String[] commands = {arg0.getSelectedItem().toString()};
                            Sounds.this.current = new SoundsPreview(Sounds.this.getInstance()).execute(commands);
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

        final RecyclerView recyclerView = this.root.findViewById(R.id.recycler_view);
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this.mContext, (view, position) -> {
                    this.wordList.get(position);
                    try {
                        if (!this.mp.isPlaying() || position != this.previous_position) {
                            this.stopPlayer();
                            ((ImageButton)
                                    view.findViewById(R.id.play)).setImageResource(
                                    R.drawable.sounds_preview_stop);
                            this.mp.setDataSource(this.wordList.get(position).getAbsolutePath());
                            this.mp.prepare();
                            this.mp.start();
                        } else {
                            this.stopPlayer();
                        }
                        this.previous_position = position;
                    } catch (final IOException ioe) {
                        Log.e(TAG, "Playback has failed for " + this.wordList.get(position).getTitle());
                    }
                })
        );
        this.mp.setOnCompletionListener(mediaPlayer -> this.stopPlayer());

        // Enable job listener
        this.jobReceiver = new JobReceiver();
        this.localBroadcastManager = LocalBroadcastManager.getInstance(this.mContext);
        this.localBroadcastManager.registerReceiver(this.jobReceiver, new IntentFilter("Sounds.START_JOB"));

        return this.root;
    }

    public void startApply() {
        if (!this.paused) {
            if (this.soundsSelector.getSelectedItemPosition() == 1) {
                new SoundsClearer(this).execute("");
            } else {
                new SoundUtils().execute(
                        this.nsv,
                        this.soundsSelector.getSelectedItem().toString(),
                        this.mContext,
                        this.theme_pid,
                        this.cipher);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mp.release();

        // Unregister finish receiver
        try {
            if (finishReceiver != null) {
                this.mContext.getApplicationContext().unregisterReceiver(finishReceiver);
            }
            this.localBroadcastManager.unregisterReceiver(this.jobReceiver);
        } catch (final IllegalArgumentException e) {
            // Unregistered already
        }
    }

    private void stopPlayer() {
        final int childCount = this.recyclerView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((ImageButton) this.recyclerView.getChildAt(i).findViewById(R.id.play))
                    .setImageResource(R.drawable.sounds_preview_play);
        }
        this.mp.reset();
    }

    private static class SoundsClearer extends AsyncTask<String, Integer, String> {
        private final WeakReference<Sounds> ref;

        SoundsClearer(final Sounds sounds) {
            super();
            this.ref = new WeakReference<>(sounds);
        }

        @Override
        protected void onPostExecute(final String result) {
            final Sounds sounds = this.ref.get();
            if (sounds != null) {
                final SharedPreferences.Editor editor = sounds.prefs.edit();
                editor.remove("sounds_applied");
                editor.apply();
                currentShownLunchBar = Lunchbar.make(sounds.nsv,
                        sounds.getString(R.string.manage_sounds_toast),
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final Sounds sounds = this.ref.get();
            if (sounds != null) {
                new SoundUtils().SoundsClearer(sounds.mContext);
            }
            return null;
        }
    }

    private static class SoundsPreview extends AsyncTask<String, Integer, String> {
        private final WeakReference<Sounds> ref;

        SoundsPreview(final Sounds sounds) {
            super();
            this.ref = new WeakReference<>(sounds);
        }

        @Override
        protected void onPreExecute() {
            final Sounds sounds = this.ref.get();
            if (sounds != null) {
                sounds.paused = true;
                sounds.progressBar.setVisibility(View.VISIBLE);
                sounds.recyclerView = sounds.root.findViewById(R.id.recycler_view);
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            final Sounds sounds = this.ref.get();
            if (sounds != null) {
                try {
                    final List<SoundsInfo> adapter1 = new ArrayList<>(sounds.wordList);

                    if (!adapter1.isEmpty()) {
                        sounds.recyclerView = sounds.root.findViewById(R.id.recycler_view);
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
            final Sounds sounds = this.ref.get();
            if (sounds != null) {
                try {
                    final File cacheDirectory = new File(sounds.mContext.getCacheDir(), "/SoundsCache/");
                    if (!cacheDirectory.exists() && cacheDirectory.mkdirs()) {
                        Log.d(TAG, "Sounds folder created");
                    }
                    final File cacheDirectory2 = new File(sounds.mContext.getCacheDir(), "/SoundCache/" +
                            "sounds_preview/");
                    if (!cacheDirectory2.exists() && cacheDirectory2.mkdirs()) {
                        Log.d(TAG, "Sounds work folder created");
                    } else {
                        FileOperations.delete(sounds.mContext,
                                sounds.mContext.getCacheDir().getAbsolutePath() +
                                        "/SoundsCache/sounds_preview/");
                        final boolean created = cacheDirectory2.mkdirs();
                        if (created) Log.d(TAG, "Sounds folder recreated");
                    }

                    // Copy the sounds.zip from assets/sounds of the theme's assets

                    final String source = sUrl[0] + ".zip";
                    if (sounds.encrypted) {
                        FileOperations.copyFileOrDir(
                                sounds.themeAssetManager,
                                soundsDir + "/" + source + ".enc",
                                sounds.mContext.getCacheDir().getAbsolutePath() +
                                        "/SoundsCache/" + source,
                                soundsDir + "/" + source + ".enc",
                                sounds.cipher);
                    } else {
                        try (InputStream inputStream = sounds.themeAssetManager.open(
                                soundsDir + "/" + source);
                             OutputStream outputStream =
                                     new FileOutputStream(
                                             sounds.mContext.getCacheDir().getAbsolutePath() +
                                                     "/SoundsCache/" + source)) {
                            this.CopyStream(inputStream, outputStream);
                        } catch (final Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "There is no sounds.zip found within the assets " +
                                    "of this theme!");

                        }
                    }

                    // Unzip the sounds archive to get it prepared for the preview
                    this.unzip(sounds.mContext.getCacheDir().
                                    getAbsolutePath() + "/SoundsCache/" + source,
                            sounds.mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/sounds_preview/");

                    sounds.wordList = new ArrayList<>();
                    final File testDirectory =
                            new File(sounds.mContext.getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/sounds_preview/");
                    this.listFilesForFolder(testDirectory);
                } catch (final Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Unexpectedly lost connection to the application host");
                }
            }
            return null;
        }

        void listFilesForFolder(final File folder) {
            final Sounds sounds = this.ref.get();
            if (sounds != null) {
                for (final File fileEntry : folder.listFiles()) {
                    if (fileEntry.isDirectory()) {
                        this.listFilesForFolder(fileEntry);
                    } else {
                        if (!".".equals(fileEntry.getName().substring(0, 1)) &&
                                projekt.substratum.common.Resources.allowedSounds(fileEntry
                                        .getName())) {
                            sounds.wordList.add(new SoundsInfo(sounds.mContext, fileEntry.getName(),
                                    fileEntry.getAbsolutePath()));
                        }
                    }
                }
            }
        }

        private void unzip(final String source, final String destination) {
            try (ZipInputStream inputStream =
                         new ZipInputStream(new BufferedInputStream(new FileInputStream(source)))) {
                ZipEntry zipEntry;
                int count;
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
            if (!Sounds.this.isAdded()) return;
            Sounds.this.startApply();
        }
    }
}