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

import projekt.substratum.R;
import projekt.substratum.adapters.showcase.ShowcaseItem;
import projekt.substratum.adapters.showcase.ShowcaseItemAdapter;
import projekt.substratum.adapters.tabs.wallpapers.WallpaperAdapter;
import projekt.substratum.adapters.tabs.wallpapers.WallpaperEntries;
import projekt.substratum.common.References;
import projekt.substratum.util.files.FileDownloader;
import projekt.substratum.util.files.MD5;
import projekt.substratum.util.readers.ReadCloudShowcaseFile;

import static projekt.substratum.common.References.SHOWCASE_SHUFFLE_COUNT;

public class ShowcaseTab extends Fragment {

    private ViewGroup root;
    private RecyclerView mRecyclerView;
    private ProgressBar materialProgressBar;
    private View no_network, no_wallpapers;
    private int current_tab_position;
    private String current_tab_address;
    private SharedPreferences prefs;
    private Context mContext;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        this.mContext = this.getContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            this.current_tab_position = bundle.getInt("tab_count", 0);
            this.current_tab_address = bundle.getString("tabbed_address");
        }

        this.root = (ViewGroup) inflater.inflate(R.layout.showcase_tab, container, false);
        this.materialProgressBar = this.root.findViewById(R.id.progress_bar_loader);
        this.no_network = this.root.findViewById(R.id.no_network);
        this.no_wallpapers = this.root.findViewById(R.id.none_found);

        this.refreshLayout();
        return this.root;
    }

    private void refreshLayout() {
        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        this.mRecyclerView = this.root.findViewById(R.id.wallpaperRecyclerView);
        this.mRecyclerView.setHasFixedSize(true);
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(this.mContext));
        ArrayList<WallpaperEntries> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new WallpaperAdapter(empty_array);
        this.mRecyclerView.setAdapter(empty_adapter);
        this.no_wallpapers.setVisibility(View.GONE);
        this.no_network.setVisibility(View.GONE);

        if (References.isNetworkAvailable(this.mContext)) {
            downloadResources downloadTask = new downloadResources(this);
            downloadTask.execute(this.current_tab_address,
                    "showcase_tab_" + this.current_tab_position + "" + ".xml");
        } else {
            this.mRecyclerView.setVisibility(View.GONE);
            this.materialProgressBar.setVisibility(View.GONE);
            this.no_wallpapers.setVisibility(View.GONE);
            this.no_network.setVisibility(View.VISIBLE);
        }

    }

    private static class downloadResources extends AsyncTask<String, Integer, ArrayList> {
        private final WeakReference<ShowcaseTab> ref;

        downloadResources(ShowcaseTab showcaseTab) {
            super();
            this.ref = new WeakReference<>(showcaseTab);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ShowcaseTab showcaseTab = this.ref.get();
            if (showcaseTab != null) {
                showcaseTab.mRecyclerView.setVisibility(View.GONE);
                showcaseTab.materialProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void onPostExecute(ArrayList result) {
            super.onPostExecute(result);
            ShowcaseTab showcaseTab = this.ref.get();
            if (showcaseTab != null) {
                ShowcaseItemAdapter mAdapter = new ShowcaseItemAdapter(result);
                showcaseTab.mRecyclerView.setAdapter(mAdapter);

                if (result.size() == 0) showcaseTab.no_wallpapers.setVisibility(View.VISIBLE);

                showcaseTab.mRecyclerView.setVisibility(View.VISIBLE);
                showcaseTab.materialProgressBar.setVisibility(View.GONE);
            }
        }

        @Override
        protected ArrayList doInBackground(String... sUrl) {
            ShowcaseTab showcaseTab = this.ref.get();
            ArrayList<ShowcaseItem> wallpapers = new ArrayList<>();
            if (showcaseTab != null) {
                String inputFileName = sUrl[1];

                File showcase_directory = new File(
                        showcaseTab.mContext.getCacheDir() + "/ShowcaseCache/");
                if (!showcase_directory.exists()) {
                    Boolean made = showcase_directory.mkdir();
                    if (!made)
                        Log.e(References.SUBSTRATUM_LOG, "Could not make showcase directory...");
                }

                File current_wallpapers = new File(showcaseTab.mContext.getCacheDir() +
                        "/ShowcaseCache/" + inputFileName);
                if (current_wallpapers.exists()) {
                    // We create a temporary file to check whether we should be replacing the
                    // current
                    inputFileName = inputFileName.substring(0, inputFileName.length() - 4) + ".xml";
                }

                FileDownloader.init(showcaseTab.mContext, sUrl[0], inputFileName, "ShowcaseCache");

                if (inputFileName.endsWith("-temp.xml")) {
                    String existing = MD5.calculateMD5(new File(showcaseTab.mContext.getCacheDir() +
                            "/ShowcaseCache/" + sUrl[1]));
                    String new_file = MD5.calculateMD5(new File(showcaseTab.mContext.getCacheDir() +
                            "/ShowcaseCache/" + inputFileName));
                    if (existing != null && !existing.equals(new_file)) {
                        Log.e("ShowcaseActivity", "Tab " + showcaseTab.current_tab_position +
                                " has been updated from the cloud!");
                        File renameMe = new File(showcaseTab.mContext.getCacheDir() +
                                "/ShowcaseCache/" +
                                sUrl[1].substring(0, sUrl[1].length() - 4) + "-temp.xml");
                        Boolean renamed = renameMe.renameTo(new File(
                                showcaseTab.mContext.getCacheDir() +
                                        "/ShowcaseCache/" + sUrl[1]));
                        if (!renamed) Log.e(References.SUBSTRATUM_LOG,
                                "Could not replace the old tab file with the new tab file...");
                    } else {
                        File deleteMe = new File(showcaseTab.mContext.getCacheDir() +
                                "/" + inputFileName);
                        Boolean deleted = deleteMe.delete();
                        if (!deleted) Log.e(References.SUBSTRATUM_LOG,
                                "Could not delete temporary tab file...");
                    }
                }

                inputFileName = sUrl[1];

                String[] checkerCommands = {
                        showcaseTab.mContext.getCacheDir() + "/ShowcaseCache/" + inputFileName};


                @SuppressWarnings("unchecked") final Map<String, String> newArray =
                        ReadCloudShowcaseFile.main(checkerCommands);
                ShowcaseItem newEntry = new ShowcaseItem();

                for (String key : newArray.keySet()) {
                    if (!key.toLowerCase(Locale.US).contains("-".toLowerCase(Locale
                            .getDefault()))) {
                        newEntry.setContext(showcaseTab.mContext);
                        newEntry.setThemeName(key);
                        newEntry.setThemeLink(newArray.get(key));
                    } else {
                        if (key.toLowerCase(Locale.US).contains("-author".toLowerCase
                                (Locale.US))) {
                            newEntry.setThemeAuthor(newArray.get(key));
                        } else if (key.toLowerCase(Locale.US).contains("-pricing"
                                .toLowerCase(Locale.US))) {
                            newEntry.setThemePricing(newArray.get(key));
                        } else if (key.toLowerCase(Locale.US).contains("-image-override" +
                                "")) {
                            newEntry.setThemeIcon(newArray.get(key));
                        } else if (key.toLowerCase(Locale.US).contains
                                ("-feature-image")) {
                            newEntry.setThemeBackgroundImage(newArray.get(key));
                        } else if (key.toLowerCase(Locale.US).contains("-package-name")) {
                            newEntry.setThemePackage(newArray.get(key));
                        } else if (key.toLowerCase(Locale.US).contains("-support"
                                .toLowerCase(Locale.US))) {
                            newEntry.setThemeSupport(newArray.get(key));
                            wallpapers.add(newEntry);
                            newEntry = new ShowcaseItem();
                            newEntry.setContext(showcaseTab.mContext);
                        }
                    }
                }
                // Shuffle the deck - every time it will change the order of themes!
                long seed = System.nanoTime();
                boolean alphabetize = showcaseTab.prefs.getBoolean("alphabetize_showcase", false);
                if (!alphabetize) {
                    for (int i = 0; i <= SHOWCASE_SHUFFLE_COUNT; i++)
                        Collections.shuffle(wallpapers, new Random(seed));
                }
            }
            return wallpapers;
        }
    }
}