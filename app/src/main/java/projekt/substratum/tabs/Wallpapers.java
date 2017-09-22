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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import projekt.substratum.R;
import projekt.substratum.adapters.tabs.wallpapers.WallpaperAdapter;
import projekt.substratum.adapters.tabs.wallpapers.WallpaperEntries;
import projekt.substratum.common.References;
import projekt.substratum.util.files.FileDownloader;
import projekt.substratum.util.readers.ReadCloudWallpaperFile;

public class Wallpapers extends Fragment {

    private ViewGroup root;
    private String wallpaperUrl;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar materialProgressBar;
    private View no_network, no_wallpapers;
    private Context mContext;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        mContext = getContext();
        wallpaperUrl = getArguments().getString("wallpaperUrl");
        root = (ViewGroup) inflater.inflate(R.layout.tab_wallpapers, container, false);
        materialProgressBar = root.findViewById(R.id.progress_bar_loader);
        no_network = root.findViewById(R.id.no_network);
        no_wallpapers = root.findViewById(R.id.none_found);

        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshLayout();
            swipeRefreshLayout.setRefreshing(false);
        });
        refreshLayout();
        return root;
    }

    private void refreshLayout() {
        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        mRecyclerView = root.findViewById(R.id.wallpaperRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        ArrayList<WallpaperEntries> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new WallpaperAdapter(empty_array);
        mRecyclerView.setAdapter(empty_adapter);
        no_wallpapers.setVisibility(View.GONE);
        no_network.setVisibility(View.GONE);

        if (References.isNetworkAvailable(mContext)) {
            downloadResources downloadTask = new downloadResources(this);
            downloadTask.execute(wallpaperUrl, "current_wallpapers.xml");
        } else {
            mRecyclerView.setVisibility(View.GONE);
            materialProgressBar.setVisibility(View.GONE);
            no_wallpapers.setVisibility(View.GONE);
            no_network.setVisibility(View.VISIBLE);
        }

    }

    private static class downloadResources extends AsyncTask<String, Integer, String> {
        private WeakReference<Wallpapers> ref;

        downloadResources(Wallpapers wallpapers) {
            ref = new WeakReference<>(wallpapers);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Wallpapers wallpapers = ref.get();
            if (wallpapers != null) {
                wallpapers.mRecyclerView.setVisibility(View.GONE);
                wallpapers.materialProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Wallpapers wallpapers = ref.get();
            if (wallpapers != null) {
                try {
                    String[] checkerCommands = {
                            wallpapers.mContext.getCacheDir() + "/current_wallpapers.xml"};

                    @SuppressWarnings("unchecked") final Map<String, String> newArray =
                            ReadCloudWallpaperFile.main(checkerCommands);
                    ArrayList<WallpaperEntries> wallpaperEntries = new ArrayList<>();
                    WallpaperEntries newEntry = new WallpaperEntries();

                    for (String key : newArray.keySet()) {
                        if (!key.toLowerCase(Locale.US)
                                .endsWith("-preview".toLowerCase(Locale.US))) {
                            newEntry.setCallingActivity(wallpapers.getActivity());
                            newEntry.setContext(wallpapers.mContext);
                            newEntry.setWallpaperName(key.replaceAll("~", " "));
                            newEntry.setWallpaperLink(newArray.get(key));
                        } else {
                            // This is a preview image to be displayed on the card
                            newEntry.setWallpaperPreview(newArray.get(key));
                            wallpaperEntries.add(newEntry);
                            newEntry = new WallpaperEntries();
                        }
                    }
                    RecyclerView.Adapter mAdapter = new WallpaperAdapter(wallpaperEntries);
                    wallpapers.mRecyclerView.setAdapter(mAdapter);

                    if (wallpaperEntries.size() == 0)
                        wallpapers.no_wallpapers.setVisibility(View.VISIBLE);

                    wallpapers.mRecyclerView.setVisibility(View.VISIBLE);
                    wallpapers.materialProgressBar.setVisibility(View.GONE);
                } catch (Exception e) {
                    // Suppress warning
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            Wallpapers wallpapers = ref.get();
            if (wallpapers != null) {
                FileDownloader.init(wallpapers.mContext, sUrl[0], "", sUrl[1]);
            }
            return null;
        }
    }
}