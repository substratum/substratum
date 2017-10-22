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
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        this.mContext = this.getContext();
        this.wallpaperUrl = this.getArguments().getString("wallpaperUrl");
        this.root = (ViewGroup) inflater.inflate(R.layout.tab_wallpapers, container, false);
        this.materialProgressBar = this.root.findViewById(R.id.progress_bar_loader);
        this.no_network = this.root.findViewById(R.id.no_network);
        this.no_wallpapers = this.root.findViewById(R.id.none_found);

        this.swipeRefreshLayout = this.root.findViewById(R.id.swipeRefreshLayout);
        this.swipeRefreshLayout.setOnRefreshListener(() -> {
            this.refreshLayout();
            this.swipeRefreshLayout.setRefreshing(false);
        });
        this.refreshLayout();
        return this.root;
    }

    private void refreshLayout() {
        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        this.mRecyclerView = this.root.findViewById(R.id.wallpaperRecyclerView);
        this.mRecyclerView.setHasFixedSize(true);
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(this.mContext));
        final ArrayList<WallpaperEntries> empty_array = new ArrayList<>();
        final RecyclerView.Adapter empty_adapter = new WallpaperAdapter(empty_array);
        this.mRecyclerView.setAdapter(empty_adapter);
        this.no_wallpapers.setVisibility(View.GONE);
        this.no_network.setVisibility(View.GONE);

        if (References.isNetworkAvailable(this.mContext)) {
            final downloadResources downloadTask = new downloadResources(this);
            downloadTask.execute(this.wallpaperUrl, "current_wallpapers.xml");
        } else {
            this.mRecyclerView.setVisibility(View.GONE);
            this.materialProgressBar.setVisibility(View.GONE);
            this.no_wallpapers.setVisibility(View.GONE);
            this.no_network.setVisibility(View.VISIBLE);
        }

    }

    private static class downloadResources extends AsyncTask<String, Integer, String> {
        private final WeakReference<Wallpapers> ref;

        downloadResources(final Wallpapers wallpapers) {
            super();
            this.ref = new WeakReference<>(wallpapers);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            final Wallpapers wallpapers = this.ref.get();
            if (wallpapers != null) {
                wallpapers.mRecyclerView.setVisibility(View.GONE);
                wallpapers.materialProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            super.onPostExecute(result);
            final Wallpapers wallpapers = this.ref.get();
            if (wallpapers != null) {
                try {
                    final String[] checkerCommands = {
                            wallpapers.mContext.getCacheDir() + "/current_wallpapers.xml"};

                    @SuppressWarnings("unchecked") final Map<String, String> newArray =
                            ReadCloudWallpaperFile.main(checkerCommands);
                    final ArrayList<WallpaperEntries> wallpaperEntries = new ArrayList<>();
                    WallpaperEntries newEntry = new WallpaperEntries();

                    for (final String key : newArray.keySet()) {
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
                    final RecyclerView.Adapter mAdapter = new WallpaperAdapter(wallpaperEntries);
                    wallpapers.mRecyclerView.setAdapter(mAdapter);

                    if (wallpaperEntries.isEmpty())
                        wallpapers.no_wallpapers.setVisibility(View.VISIBLE);

                    wallpapers.mRecyclerView.setVisibility(View.VISIBLE);
                    wallpapers.materialProgressBar.setVisibility(View.GONE);
                } catch (final Exception e) {
                    // Suppress warning
                }
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final Wallpapers wallpapers = this.ref.get();
            if (wallpapers != null) {
                FileDownloader.init(wallpapers.mContext, sUrl[0], "", sUrl[1]);
            }
            return null;
        }
    }
}