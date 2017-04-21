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

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.adapters.tabs.sounds.SoundsAdapter;
import projekt.substratum.adapters.tabs.sounds.SoundsInfo;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.util.tabs.SoundUtils;
import projekt.substratum.util.views.RecyclerItemClickListener;

import static projekt.substratum.util.tabs.SoundUtils.finishReceiver;

public class Sounds extends Fragment {

    private static final String soundsDir = "audio";
    private static final String TAG = "SoundUtils";
    private String theme_pid;
    private ViewGroup root;
    private MaterialProgressBar progressBar;
    private Spinner soundsSelector;
    private ArrayList<SoundsInfo> wordList;
    private RecyclerView recyclerView;
    private MediaPlayer mp = new MediaPlayer();
    private int previous_position;
    private RelativeLayout relativeLayout, error;
    private RelativeLayout defaults;
    private ProgressDialog mProgressDialog;
    private SharedPreferences prefs;
    private AsyncTask current;
    private NestedScrollView nsv;
    private AssetManager themeAssetManager;
    private boolean paused = false;
    private JobReceiver jobReceiver;
    private LocalBroadcastManager localBroadcastManager;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        theme_pid = InformationActivity.getThemePID();

        root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_4, container, false);
        nsv = (NestedScrollView) root.findViewById(R.id.nestedScrollView);

        progressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);
        progressBar.setVisibility(View.GONE);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        defaults = (RelativeLayout) root.findViewById(R.id.restore_to_default);

        final RelativeLayout sounds_preview =
                (RelativeLayout) root.findViewById(R.id.sounds_placeholder);
        relativeLayout = (RelativeLayout) root.findViewById(R.id.relativeLayout);
        error = (RelativeLayout) root.findViewById(R.id.error_loading_pack);
        error.setVisibility(View.GONE);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayList<SoundsInfo> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new SoundsAdapter(empty_array);
        recyclerView.setAdapter(empty_adapter);

        try {
            // Parses the list of items in the fonts folder
            Resources themeResources =
                    getContext().getPackageManager().getResourcesForApplication(theme_pid);
            themeAssetManager = themeResources.getAssets();
            String[] fileArray = themeAssetManager.list(soundsDir);
            ArrayList<String> archivedSounds = new ArrayList<>();
            Collections.addAll(archivedSounds, fileArray);

            // Creates the list of dropdown items
            ArrayList<String> unarchivedSounds = new ArrayList<>();
            unarchivedSounds.add(getString(R.string.sounds_default_spinner));
            unarchivedSounds.add(getString(R.string.sounds_spinner_set_defaults));
            for (int i = 0; i < archivedSounds.size(); i++) {
                unarchivedSounds.add(archivedSounds.get(i).substring(0,
                        archivedSounds.get(i).length() - 4));
            }

            ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, unarchivedSounds);
            soundsSelector = (Spinner) root.findViewById(R.id.soundsSelection);
            soundsSelector.setAdapter(adapter1);
            soundsSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long id) {
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
                            String[] commands = {arg0.getSelectedItem().toString()};
                            current = new SoundsPreview().execute(commands);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "There is no sounds.zip found within the assets of this theme!");
        }

        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getContext(), (view, position) -> {
                    wordList.get(position);
                    try {
                        if (!mp.isPlaying() || position != previous_position) {
                            stopPlayer();
                            ((ImageButton)
                                    view.findViewById(R.id.play)).setImageResource(
                                    R.drawable.sounds_preview_stop);
                            mp.setDataSource(wordList.get(position).getAbsolutePath());
                            mp.prepare();
                            mp.start();
                        } else {
                            stopPlayer();
                        }
                        previous_position = position;
                    } catch (IOException ioe) {
                        Log.e(TAG, "Playback has failed for " + wordList.get(position).getTitle());
                    }
                })
        );
        mp.setOnCompletionListener(mediaPlayer -> stopPlayer());

        // Enable job listener
        jobReceiver = new JobReceiver();
        IntentFilter intentFilter = new IntentFilter("Sounds.START_JOB");
        localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        localBroadcastManager.registerReceiver(jobReceiver, intentFilter);

        return root;
    }

    public void startApply() {
        if (!paused) {
            if (soundsSelector.getSelectedItemPosition() == 1) {
                new SoundsClearer().execute("");
            } else {
                new SoundUtils().execute(nsv,
                        soundsSelector.getSelectedItem().toString(), getContext(), theme_pid);
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
                getContext().getApplicationContext().unregisterReceiver(finishReceiver);
            }
            localBroadcastManager.unregisterReceiver(jobReceiver);
        } catch (IllegalArgumentException e) {
            // Unregistered already
        }
    }

    private void stopPlayer() {
        final int childCount = recyclerView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((ImageButton) recyclerView.getChildAt(i).findViewById(R.id.play))
                    .setImageResource(R.drawable.sounds_preview_play);
        }
        mp.reset();
    }

    private class SoundsClearer extends AsyncTask<String, Integer, String> {

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
            editor.remove("sounds_applied");
            editor.apply();
            Lunchbar.make(nsv,
                    getString(R.string.manage_sounds_toast),
                    Lunchbar.LENGTH_LONG)
                    .show();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            new SoundUtils().SoundsClearer(getContext());
            return null;
        }
    }

    private class SoundsPreview extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            paused = true;
            progressBar.setVisibility(View.VISIBLE);
            recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                List<SoundsInfo> adapter1 = new ArrayList<>(wordList);

                if (adapter1.size() > 0) {
                    recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
                    SoundsAdapter mAdapter = new SoundsAdapter(adapter1);
                    RecyclerView.LayoutManager mLayoutManager =
                            new LinearLayoutManager(getContext());

                    recyclerView.setLayoutManager(mLayoutManager);
                    recyclerView.setItemAnimator(new DefaultItemAnimator());
                    recyclerView.setAdapter(mAdapter);
                    paused = false;
                } else {
                    recyclerView.setVisibility(View.GONE);
                    relativeLayout.setVisibility(View.GONE);
                    error.setVisibility(View.VISIBLE);
                }
                progressBar.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e(TAG, "Window was destroyed before AsyncTask could perform postExecute()");
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            try {
                File cacheDirectory = new File(getContext().getCacheDir(), "/SoundsCache/");
                if (!cacheDirectory.exists() && cacheDirectory.mkdirs()) {
                    Log.d(TAG, "Sounds folder created");
                }
                File cacheDirectory2 = new File(getContext().getCacheDir(), "/SoundCache/" +
                        "sounds_preview/");
                if (!cacheDirectory2.exists() && cacheDirectory2.mkdirs()) {
                    Log.d(TAG, "Sounds work folder created");
                } else {
                    FileOperations.delete(getContext(),
                            getContext().getCacheDir().getAbsolutePath() +
                                    "/SoundsCache/sounds_preview/");
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d(TAG, "Sounds folder recreated");
                }

                // Copy the sounds.zip from assets/sounds of the theme's assets

                String source = sUrl[0] + ".zip";

                try (InputStream inputStream = themeAssetManager.open(
                        soundsDir + "/" + source);
                     OutputStream outputStream =
                             new FileOutputStream(getContext().getCacheDir().getAbsolutePath() +
                                     "/SoundsCache/" + source)) {
                    CopyStream(inputStream, outputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "There is no sounds.zip found within the assets of this theme!");
                }

                // Unzip the sounds archive to get it prepared for the preview
                unzip(getContext().getCacheDir().getAbsolutePath() + "/SoundsCache/" + source,
                        getContext().getCacheDir().getAbsolutePath() +
                                "/SoundsCache/sounds_preview/");

                wordList = new ArrayList<>();
                File testDirectory =
                        new File(getContext().getCacheDir().getAbsolutePath() +
                                "/SoundsCache/sounds_preview/");
                listFilesForFolder(testDirectory);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Unexpectedly lost connection to the application host");
            }
            return null;
        }

        void listFilesForFolder(final File folder) {
            for (final File fileEntry : folder.listFiles()) {
                if (fileEntry.isDirectory()) {
                    listFilesForFolder(fileEntry);
                } else {
                    if (!fileEntry.getName().substring(0, 1).equals(".") &&
                            References.allowedSounds(fileEntry.getName())) {
                        wordList.add(new SoundsInfo(getContext(), fileEntry.getName(),
                                fileEntry.getAbsolutePath()));
                    }
                }
            }
        }

        private void unzip(String source, String destination) {
            try (ZipInputStream inputStream =
                         new ZipInputStream(new BufferedInputStream(new FileInputStream(source)))) {
                ZipEntry zipEntry;
                int count;
                byte[] buffer = new byte[8192];
                while ((zipEntry = inputStream.getNextEntry()) != null) {
                    File file = new File(destination, zipEntry.getName());
                    File dir = zipEntry.isDirectory() ? file : file.getParentFile();
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