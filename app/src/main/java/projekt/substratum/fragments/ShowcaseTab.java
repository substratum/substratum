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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
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
    private MaterialProgressBar materialProgressBar;
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
        mContext = getContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            current_tab_position = bundle.getInt("tab_count", 0);
            current_tab_address = bundle.getString("tabbed_address");
        }

        root = (ViewGroup) inflater.inflate(R.layout.showcase_tab, container, false);
        materialProgressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);
        no_network = root.findViewById(R.id.no_network);
        no_wallpapers = root.findViewById(R.id.none_found);

        refreshLayout();
        return root;
    }

    private void refreshLayout() {
        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        mRecyclerView = (RecyclerView) root.findViewById(R.id.wallpaperRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        ArrayList<WallpaperEntries> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new WallpaperAdapter(empty_array);
        mRecyclerView.setAdapter(empty_adapter);
        no_wallpapers.setVisibility(View.GONE);
        no_network.setVisibility(View.GONE);

        if (References.isNetworkAvailable(mContext)) {
            downloadResources downloadTask = new downloadResources();
            downloadTask.execute(current_tab_address, "showcase_tab_" + current_tab_position + "" +
                    ".xml");
        } else {
            mRecyclerView.setVisibility(View.GONE);
            materialProgressBar.setVisibility(View.GONE);
            no_wallpapers.setVisibility(View.GONE);
            no_network.setVisibility(View.VISIBLE);
        }

    }

    private class downloadResources extends AsyncTask<String, Integer, ArrayList> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mRecyclerView.setVisibility(View.GONE);
            materialProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void onPostExecute(ArrayList result) {
            super.onPostExecute(result);

            ShowcaseItemAdapter mAdapter = new ShowcaseItemAdapter(result);
            mRecyclerView.setAdapter(mAdapter);

            if (result.size() == 0) no_wallpapers.setVisibility(View.VISIBLE);

            mRecyclerView.setVisibility(View.VISIBLE);
            materialProgressBar.setVisibility(View.GONE);
        }

        @Override
        protected ArrayList doInBackground(String... sUrl) {
            String inputFileName = sUrl[1];
            ArrayList<ShowcaseItem> wallpapers = new ArrayList<>();

            File showcase_directory = new File(mContext.getCacheDir() + "/ShowcaseCache/");
            if (!showcase_directory.exists()) {
                Boolean made = showcase_directory.mkdir();
                if (!made)
                    Log.e(References.SUBSTRATUM_LOG, "Could not make showcase directory...");
            }

            File current_wallpapers = new File(mContext.getCacheDir() +
                    "/ShowcaseCache/" + inputFileName);
            if (current_wallpapers.exists()) {
                // We create a temporary file to check whether we should be replacing the
                // current
                inputFileName = inputFileName.substring(0, inputFileName.length() - 4) + ".xml";
            }

            FileDownloader.init(mContext, sUrl[0], inputFileName, "ShowcaseCache");

            if (inputFileName.endsWith("-temp.xml")) {
                String existing = MD5.calculateMD5(new File(mContext.getCacheDir() +
                        "/ShowcaseCache/" + sUrl[1]));
                String new_file = MD5.calculateMD5(new File(mContext.getCacheDir() +
                        "/ShowcaseCache/" + inputFileName));
                if (existing != null && !existing.equals(new_file)) {
                    Log.e("ShowcaseActivity", "Tab " + current_tab_position +
                            " has been updated from the cloud!");
                    File renameMe = new File(mContext.getCacheDir() +
                            "/ShowcaseCache/" +
                            sUrl[1].substring(0, sUrl[1].length() - 4) + "-temp.xml");
                    Boolean renamed = renameMe.renameTo(new File(mContext.getCacheDir() +
                            "/ShowcaseCache/" + sUrl[1]));
                    if (!renamed) Log.e(References.SUBSTRATUM_LOG,
                            "Could not replace the old tab file with the new tab file...");
                } else {
                    File deleteMe = new File(mContext.getCacheDir() +
                            "/" + inputFileName);
                    Boolean deleted = deleteMe.delete();
                    if (!deleted) Log.e(References.SUBSTRATUM_LOG,
                            "Could not delete temporary tab file...");
                }
            }

            inputFileName = sUrl[1];

            String[] checkerCommands = {mContext.getCacheDir() + "/ShowcaseCache/" + inputFileName};

            @SuppressWarnings("unchecked")
            final Map<String, String> newArray = ReadCloudShowcaseFile.main(checkerCommands);
            ShowcaseItem newEntry = new ShowcaseItem();

            for (String key : newArray.keySet()) {
                if (!key.toLowerCase().contains("-".toLowerCase())) {
                    newEntry.setContext(mContext);
                    newEntry.setThemeName(key);
                    newEntry.setThemeLink(newArray.get(key));
                } else {
                    if (key.toLowerCase().contains("-author".toLowerCase())) {
                        newEntry.setThemeAuthor(newArray.get(key));
                    } else if (key.toLowerCase().contains("-pricing".toLowerCase())) {
                        newEntry.setThemePricing(newArray.get(key));
                    } else if (key.toLowerCase().contains("-image-override")) {
                        newEntry.setThemeIcon(newArray.get(key));
                    } else if (key.toLowerCase().contains("-feature-image")) {
                        newEntry.setThemeBackgroundImage(newArray.get(key));
                    } else if (key.toLowerCase().contains("-package-name")) {
                        newEntry.setThemePackage(newArray.get(key));
                    } else if (key.toLowerCase().contains("-support".toLowerCase())) {
                        newEntry.setThemeSupport(newArray.get(key));
                        wallpapers.add(newEntry);
                        newEntry = new ShowcaseItem();
                        newEntry.setContext(mContext);
                    }
                }
            }
            // Shuffle the deck - every time it will change the order of themes!
            long seed = System.nanoTime();
            boolean alphabetize = prefs.getBoolean("alphabetize_showcase", false);
            if (!alphabetize) {
                for (int i = 0; i <= SHOWCASE_SHUFFLE_COUNT; i++)
                    Collections.shuffle(wallpapers, new Random(seed));
            }
            return wallpapers;
        }
    }
}