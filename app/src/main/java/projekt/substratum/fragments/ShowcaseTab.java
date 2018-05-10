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

import android.annotation.SuppressLint;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.adapters.activities.ShowcaseAdapter;
import projekt.substratum.adapters.activities.ShowcaseItem;
import projekt.substratum.adapters.tabs.wallpapers.WallpaperAdapter;
import projekt.substratum.adapters.tabs.wallpapers.WallpaperItem;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.databinding.ShowcaseTabBinding;
import projekt.substratum.util.helpers.FileDownloader;
import projekt.substratum.util.readers.ReadCloudShowcaseFile;

import static projekt.substratum.common.References.SHOWCASE_SHUFFLE_COUNT;

public class ShowcaseTab extends Fragment {

    @SuppressLint("StaticFieldLeak")
    public static RecyclerView recyclerView;
    ProgressBar materialProgressBar;
    private int currentTabPosition;
    private String currentTabAddress;
    private Context context;

    @Override
    public void onDestroy() {
        super.onDestroy();
        recyclerView.invalidate();
        recyclerView = null;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        context = Substratum.getInstance();
        ShowcaseTabBinding viewBinding =
                DataBindingUtil.inflate(inflater, R.layout.showcase_tab, container, false);
        View view = viewBinding.getRoot();
        materialProgressBar = viewBinding.progressBarLoader;
        recyclerView = viewBinding.wallpaperRecyclerView;
        Bundle bundle = getArguments();

        if (bundle != null) {
            currentTabPosition = bundle.getInt("tab_count", 0);
            currentTabAddress = bundle.getString("tabbed_address");
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
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 2));
        ArrayList<WallpaperItem> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new WallpaperAdapter(empty_array);
        recyclerView.setAdapter(empty_adapter);
        recyclerView.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                        boolean slowDevice = Systems.isSamsungDevice(context);
                        for (int i = 0; i < recyclerView.getChildCount(); i++) {
                            View v = recyclerView.getChildAt(i);
                            v.setAlpha(0.0f);
                            v.animate().alpha(1.0f)
                                    .setDuration(300)
                                    .setStartDelay(slowDevice ? i * 50 : i * 30)
                                    .start();
                        }
                        return true;
                    }
                });
        if (References.isNetworkAvailable(context)) {
            downloadResources downloadTask = new downloadResources(this);
            downloadTask.execute(
                    currentTabAddress,
                    "showcase_tab_" + currentTabPosition + ".xml");
        } else {
            recyclerView.setVisibility(View.GONE);
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
                recyclerView.setVisibility(View.GONE);
                showcaseTab.materialProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void onPostExecute(ArrayList result) {
            super.onPostExecute(result);
            ShowcaseTab showcaseTab = ref.get();
            if (showcaseTab != null) {
                ShowcaseAdapter mAdapter = new ShowcaseAdapter(result);
                recyclerView.setAdapter(mAdapter);
                recyclerView.setVisibility(View.VISIBLE);
                showcaseTab.materialProgressBar.setVisibility(View.GONE);
            }
        }

        @Override
        protected ArrayList doInBackground(String... sUrl) {
            ShowcaseTab showcaseTab = ref.get();
            ArrayList<ShowcaseItem> wallpapers = new ArrayList<>();
            if (showcaseTab != null) {
                String inputFileName = sUrl[1];
                File showcaseDirectory = new File(
                        showcaseTab.context.getCacheDir() + "/ShowcaseCache/");
                if (!showcaseDirectory.exists()) {
                    boolean made = showcaseDirectory.mkdir();
                    if (!made)
                        Log.e(References.SUBSTRATUM_LOG, "Could not make showcase directory...");
                }

                File currentWallpapers = new File(showcaseTab.context.getCacheDir() +
                        "/ShowcaseCache/" + inputFileName);
                if (currentWallpapers.exists()) {
                    boolean deleted = currentWallpapers.delete();
                    if (!deleted) Log.e("ShowcaseTab", "Could not delete the current tab file...");
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
                        newEntry.setThemeName(stringStringEntry.getKey().replaceAll("%", " "));
                    } else {
                        String entry = stringStringEntry.getKey().toLowerCase(Locale.US);
                        if (entry.contains("-author".toLowerCase(Locale.US))) {
                            newEntry.setThemeAuthor(stringStringEntry.getValue());
                        } else if (entry.contains("-feature-image".toLowerCase(Locale.US))) {
                            newEntry.setThemeBackgroundImage(stringStringEntry.getValue());
                        } else if (entry.contains("-package-name".toLowerCase(Locale.US))) {
                            newEntry.setThemePackage(stringStringEntry.getValue());
                        } else if (entry.contains("-pricing".toLowerCase(Locale.US))) {
                            newEntry.setThemePricing(stringStringEntry.getValue());
                            wallpapers.add(newEntry);
                            newEntry = new ShowcaseItem();
                            newEntry.setContext(showcaseTab.context);
                        }
                    }
                }

                // Shuffle the deck - every time it will change the order of themes!
                long seed = System.nanoTime();
                for (int i = 0; i <= SHOWCASE_SHUFFLE_COUNT; i++)
                    Collections.shuffle(wallpapers, new Random(seed));
            }
            return wallpapers;
        }
    }
}