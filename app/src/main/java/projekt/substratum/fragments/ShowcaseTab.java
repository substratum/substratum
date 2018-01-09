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

package projekt.substratum.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.adapters.showcase.ShowcaseItem;
import projekt.substratum.adapters.showcase.ShowcaseItemAdapter;
import projekt.substratum.adapters.tabs.wallpapers.WallpaperAdapter;
import projekt.substratum.adapters.tabs.wallpapers.WallpaperEntries;
import projekt.substratum.common.References;
import projekt.substratum.util.files.FileDownloader;
import projekt.substratum.util.readers.ReadCloudShowcaseFile;

import static projekt.substratum.common.References.SHOWCASE_SHUFFLE_COUNT;

public class ShowcaseTab extends Fragment {

    @BindView(R.id.progress_bar_loader)
    ProgressBar materialProgressBar;
    @BindView(R.id.wallpaperRecyclerView)
    RecyclerView mRecyclerView;
    private int current_tab_position;
    private String current_tab_address;
    private SharedPreferences prefs;
    private Context context;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        context = Substratum.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        View view = inflater.inflate(R.layout.showcase_tab, container, false);
        ButterKnife.bind(this, view);
        Bundle bundle = getArguments();
        if (bundle != null) {
            current_tab_position = bundle.getInt("tab_count", 0);
            current_tab_address = bundle.getString("tabbed_address");
        } else {
            return null;
        }
        refreshLayout();
        return view;
    }

    /**
     * Refresh the layout of the showcase tab entries
     */
    private void refreshLayout() {
        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        ArrayList<WallpaperEntries> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new WallpaperAdapter(empty_array);
        mRecyclerView.setAdapter(empty_adapter);
        if (References.isNetworkAvailable(context)) {
            downloadResources downloadTask = new downloadResources(this);
            downloadTask.execute(
                    current_tab_address,
                    "showcase_tab_" + current_tab_position + ".xml");
        } else {
            mRecyclerView.setVisibility(View.GONE);
            materialProgressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Download the showcase entry list on our GitHub repository at:
     * https://github.com/substratum/database
     */
    private static class downloadResources extends AsyncTask<String, Integer, ArrayList> {
        private WeakReference<ShowcaseTab> ref;

        downloadResources(ShowcaseTab showcaseTab) {
            super();
            ref = new WeakReference<>(showcaseTab);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ShowcaseTab showcaseTab = ref.get();
            if (showcaseTab != null) {
                showcaseTab.mRecyclerView.setVisibility(View.GONE);
                showcaseTab.materialProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void onPostExecute(ArrayList result) {
            super.onPostExecute(result);
            ShowcaseTab showcaseTab = ref.get();
            if (showcaseTab != null) {
                ShowcaseItemAdapter mAdapter = new ShowcaseItemAdapter(result);
                showcaseTab.mRecyclerView.setAdapter(mAdapter);
                showcaseTab.mRecyclerView.setVisibility(View.VISIBLE);
                showcaseTab.materialProgressBar.setVisibility(View.GONE);
            }
        }

        @Override
        protected ArrayList doInBackground(String... sUrl) {
            ShowcaseTab showcaseTab = ref.get();
            ArrayList<ShowcaseItem> wallpapers = new ArrayList<>();
            if (showcaseTab != null) {
                String inputFileName = sUrl[1];
                File showcase_directory = new File(
                        showcaseTab.context.getCacheDir() + "/ShowcaseCache/");
                if (!showcase_directory.exists()) {
                    Boolean made = showcase_directory.mkdir();
                    if (!made)
                        Log.e(References.SUBSTRATUM_LOG, "Could not make showcase directory...");
                }

                File current_wallpapers = new File(showcaseTab.context.getCacheDir() +
                        "/ShowcaseCache/" + inputFileName);
                if (current_wallpapers.exists()) {
                    inputFileName = inputFileName.substring(0, inputFileName.length() - 4) + ".xml";
                }

                FileDownloader.init(showcaseTab.context, sUrl[0], inputFileName, "ShowcaseCache");
                inputFileName = sUrl[1];

                @SuppressWarnings("unchecked") Map<String, String> newArray =
                        ReadCloudShowcaseFile.read(
                                showcaseTab.context.getCacheDir() +
                                        "/ShowcaseCache/" + inputFileName);
                ShowcaseItem newEntry = new ShowcaseItem();

                for (Map.Entry<String, String> stringStringEntry : newArray.entrySet()) {
                    if (!stringStringEntry.getKey().toLowerCase(Locale.US)
                            .contains("-".toLowerCase(Locale.getDefault()))) {
                        newEntry.setContext(showcaseTab.context);
                        newEntry.setThemeName(stringStringEntry.getKey());
                        newEntry.setThemeLink(stringStringEntry.getValue());
                    } else {
                        String entry = stringStringEntry.getKey().toLowerCase(Locale.US);
                        if (entry.contains("-author".toLowerCase(Locale.US))) {
                            newEntry.setThemeAuthor(stringStringEntry.getValue());
                        } else if (entry.contains("-pricing".toLowerCase(Locale.US))) {
                            newEntry.setThemePricing(stringStringEntry.getValue());
                        } else if (entry.contains("-image-override".toLowerCase(Locale.US))) {
                            newEntry.setThemeIcon(stringStringEntry.getValue());
                        } else if (entry.contains("-feature-image".toLowerCase(Locale.US))) {
                            newEntry.setThemeBackgroundImage(stringStringEntry.getValue());
                        } else if (entry.contains("-package-name".toLowerCase(Locale.US))) {
                            newEntry.setThemePackage(stringStringEntry.getValue());
                        } else if (entry.contains("-support".toLowerCase(Locale.US))) {
                            newEntry.setThemeSupport(stringStringEntry.getValue());
                            wallpapers.add(newEntry);
                            newEntry = new ShowcaseItem();
                            newEntry.setContext(showcaseTab.context);
                        }
                    }
                }
                // Shuffle the deck - every time it will change the order of themes!
                long seed = System.nanoTime();
                boolean alphabetize = showcaseTab.prefs.getBoolean("alphabetize_showcase",
                        false);
                if (!alphabetize) {
                    for (int i = 0; i <= SHOWCASE_SHUFFLE_COUNT; i++)
                        Collections.shuffle(wallpapers, new Random(seed));
                } else {
                    Collections.sort(wallpapers);
                }
            }
            return wallpapers;
        }
    }
}