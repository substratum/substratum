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
import android.support.annotation.NonNull;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import projekt.substratum.R;
import projekt.substratum.adapters.tabs.wallpapers.WallpaperAdapter;
import projekt.substratum.adapters.tabs.wallpapers.WallpaperEntries;
import projekt.substratum.common.References;
import projekt.substratum.util.files.FileDownloader;
import projekt.substratum.util.readers.ReadCloudWallpaperFile;

import static projekt.substratum.common.Internal.CURRENT_WALLPAPERS;
import static projekt.substratum.common.Internal.THEME_WALLPAPER;

public class Wallpapers extends Fragment {

    public static AsyncTask<String, Integer, String> mainLoader = null;
    @BindView(R.id.progress_bar_loader)
    ProgressBar materialProgressBar;
    @BindView(R.id.no_network)
    View no_network;
    @BindView(R.id.none_found)
    View no_wallpapers;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.wallpaperRecyclerView)
    RecyclerView mRecyclerView;
    private String wallpaperUrl;
    private Context context;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        context = getContext();
        View view = inflater.inflate(R.layout.tab_wallpapers, container, false);
        ButterKnife.bind(this, view);

        if (getArguments() != null) {
            wallpaperUrl = getArguments().getString(THEME_WALLPAPER);
        } else {
            // At this point, the tab has been incorrectly loaded
            return null;
        }

        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshLayout();
            swipeRefreshLayout.setRefreshing(false);
        });
        refreshLayout();
        return view;
    }

    private void refreshLayout() {
        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        ArrayList<WallpaperEntries> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new WallpaperAdapter(empty_array);
        mRecyclerView.setAdapter(empty_adapter);
        no_wallpapers.setVisibility(View.GONE);
        no_network.setVisibility(View.GONE);

        if (References.isNetworkAvailable(context)) {
            mainLoader = new downloadResources(this);
            mainLoader.execute(wallpaperUrl, CURRENT_WALLPAPERS);
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
            super();
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
                    @SuppressWarnings("unchecked")
                    Map<String, String> newArray =
                            ReadCloudWallpaperFile.read(
                                    wallpapers.context.getCacheDir() + "/" + CURRENT_WALLPAPERS);
                    ArrayList<WallpaperEntries> wallpaperEntries = new ArrayList<>();
                    WallpaperEntries newEntry = new WallpaperEntries();

                    for (Map.Entry<String, String> stringStringEntry : newArray.entrySet()) {
                        if (!stringStringEntry.getKey().toLowerCase(Locale.US)
                                .endsWith("-preview".toLowerCase(Locale.US))) {
                            newEntry.setCallingActivity(wallpapers.getActivity());
                            newEntry.setContext(wallpapers.context);
                            newEntry.setWallpaperName(stringStringEntry.getKey()
                                    .replaceAll("~", " "));
                            newEntry.setWallpaperLink(stringStringEntry.getValue());
                        } else {
                            // This is a preview image to be displayed on the card
                            newEntry.setWallpaperPreview(stringStringEntry.getValue());
                            wallpaperEntries.add(newEntry);
                            newEntry = new WallpaperEntries();
                        }
                    }
                    RecyclerView.Adapter mAdapter = new WallpaperAdapter(wallpaperEntries);
                    wallpapers.mRecyclerView.setAdapter(mAdapter);

                    if (wallpaperEntries.isEmpty())
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
                FileDownloader.init(wallpapers.context, sUrl[0], "", sUrl[1]);
            }
            return null;
        }
    }
}