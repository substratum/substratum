/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.tabs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import projekt.substratum.R;
import projekt.substratum.adapters.tabs.wallpapers.WallpaperAdapter;
import projekt.substratum.adapters.tabs.wallpapers.WallpaperItem;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.databinding.TabWallpapersBinding;
import projekt.substratum.util.helpers.FileDownloader;
import projekt.substratum.util.readers.ReadCloudWallpaperFile;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import static projekt.substratum.common.Internal.CURRENT_WALLPAPERS;
import static projekt.substratum.common.Internal.START_JOB_ACTION;
import static projekt.substratum.common.Internal.THEME_WALLPAPER;

public class Wallpapers extends Fragment {

    public static AsyncTask<String, Integer, String> mainLoader = null;
    private ProgressBar materialProgressBar;
    private View noNetwork;
    private View noWallpapers;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private String wallpaperUrl;
    private Context context;
    private JobReceiver jobReceiver;
    private LocalBroadcastManager localBroadcastManager;

    /**
     * Scroll up the RecyclerView smoothly
     */
    private void scrollUp() {
        recyclerView.smoothScrollToPosition(0);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        context = getContext();

        TabWallpapersBinding tabWallpapersBinding =
                DataBindingUtil.inflate(inflater, R.layout.tab_wallpapers, container, false);

        View view = tabWallpapersBinding.getRoot();

        materialProgressBar = tabWallpapersBinding.progressBarLoader;
        noNetwork = tabWallpapersBinding.noNetwork;
        noWallpapers = tabWallpapersBinding.noneFound;
        swipeRefreshLayout = tabWallpapersBinding.swipeRefreshLayout;
        recyclerView = tabWallpapersBinding.wallpaperRecyclerView;

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

        jobReceiver = new JobReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(jobReceiver,
                new IntentFilter(getClass().getSimpleName() + START_JOB_ACTION));
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            localBroadcastManager.unregisterReceiver(jobReceiver);
        } catch (final IllegalArgumentException ignored) {
            // unregistered already
        }
    }

    private void refreshLayout() {
        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        ArrayList<WallpaperItem> emptyArray = new ArrayList<>();
        RecyclerView.Adapter emptyAdapter = new WallpaperAdapter(emptyArray);
        recyclerView.setAdapter(emptyAdapter);
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
        noWallpapers.setVisibility(View.GONE);
        noNetwork.setVisibility(View.GONE);

        if (References.isNetworkAvailable(context)) {
            mainLoader = new downloadResources(this);
            mainLoader.execute(wallpaperUrl, CURRENT_WALLPAPERS);
        } else {
            recyclerView.setVisibility(View.GONE);
            materialProgressBar.setVisibility(View.GONE);
            noWallpapers.setVisibility(View.GONE);
            noNetwork.setVisibility(View.VISIBLE);
        }
    }

    private static class downloadResources extends AsyncTask<String, Integer, String> {
        private final WeakReference<Wallpapers> ref;

        downloadResources(Wallpapers wallpapers) {
            super();
            ref = new WeakReference<>(wallpapers);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Wallpapers wallpapers = ref.get();
            if (wallpapers != null) {
                wallpapers.recyclerView.setVisibility(View.GONE);
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
                    ArrayList<WallpaperItem> wallpaperEntries = new ArrayList<>();
                    WallpaperItem newEntry = new WallpaperItem();

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
                            newEntry = new WallpaperItem();
                        }
                    }
                    RecyclerView.Adapter mAdapter = new WallpaperAdapter(wallpaperEntries);
                    wallpapers.recyclerView.setAdapter(mAdapter);

                    if (wallpaperEntries.isEmpty())
                        wallpapers.noWallpapers.setVisibility(View.VISIBLE);

                    wallpapers.recyclerView.setVisibility(View.VISIBLE);
                    wallpapers.materialProgressBar.setVisibility(View.GONE);
                } catch (Exception ignored) {
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

    /**
     * Receiver to pick data up from InformationActivity to start the process of scroll up
     */
    class JobReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (!isAdded()) return;
            scrollUp();
        }
    }
}