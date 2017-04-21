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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.adapters.tabs.wallpapers.WallpaperAdapter;
import projekt.substratum.adapters.tabs.wallpapers.WallpaperEntries;
import projekt.substratum.common.References;
import projekt.substratum.util.readers.ReadCloudWallpaperFile;

public class Wallpapers extends Fragment {

    private static final String TAG = "WallpaperUtils";
    private ViewGroup root;
    private String wallpaperUrl;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialProgressBar materialProgressBar;
    private View no_network, no_wallpapers;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        wallpaperUrl = InformationActivity.getWallpaperUrl();
        root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_5, container, false);
        materialProgressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);
        no_network = root.findViewById(R.id.no_network);
        no_wallpapers = root.findViewById(R.id.none_found);

        swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshLayout();
            swipeRefreshLayout.setRefreshing(false);
        });
        refreshLayout();
        return root;
    }

    private void refreshLayout() {
        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        mRecyclerView = (RecyclerView) root.findViewById(R.id.wallpaperRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayList<WallpaperEntries> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new WallpaperAdapter(empty_array);
        mRecyclerView.setAdapter(empty_adapter);
        no_wallpapers.setVisibility(View.GONE);
        no_network.setVisibility(View.GONE);

        if (References.isNetworkAvailable(getContext())) {
            downloadResources downloadTask = new downloadResources();
            downloadTask.execute(wallpaperUrl, "current_wallpapers.xml");
        } else {
            mRecyclerView.setVisibility(View.GONE);
            materialProgressBar.setVisibility(View.GONE);
            no_wallpapers.setVisibility(View.GONE);
            no_network.setVisibility(View.VISIBLE);
        }

    }

    private class downloadResources extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mRecyclerView.setVisibility(View.GONE);
            materialProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                String[] checkerCommands = {getContext().getCacheDir() + "/current_wallpapers.xml"};

                @SuppressWarnings("unchecked")
                final Map<String, String> newArray = ReadCloudWallpaperFile.main(checkerCommands);
                ArrayList<WallpaperEntries> wallpapers = new ArrayList<>();
                WallpaperEntries newEntry = new WallpaperEntries();

                for (String key : newArray.keySet()) {
                    if (!key.toLowerCase().contains("-preview".toLowerCase())) {
                        newEntry.setCallingActivity(getActivity());
                        newEntry.setContext(getContext());
                        newEntry.setWallpaperName(key);
                        newEntry.setWallpaperLink(newArray.get(key));
                    } else {
                        // This is a preview image to be displayed on the card
                        newEntry.setWallpaperPreview(newArray.get(key));
                        wallpapers.add(newEntry);
                        newEntry = new WallpaperEntries();
                    }
                }
                RecyclerView.Adapter mAdapter = new WallpaperAdapter(wallpapers);
                mRecyclerView.setAdapter(mAdapter);

                if (wallpapers.size() == 0)
                    no_wallpapers.setVisibility(View.VISIBLE);

                mRecyclerView.setVisibility(View.VISIBLE);
                materialProgressBar.setVisibility(View.GONE);
            } catch (Exception e) {
                // Suppress warning
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {

            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            try {
                File current_wallpapers = new File(getContext().getCacheDir() +
                        "/current_wallpapers.xml");
                if (current_wallpapers.exists() && !current_wallpapers.delete()) {
                    Log.e(TAG, "Unable to delete current wallpaper stash.");
                }

                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();

                output = new FileOutputStream(
                        getContext().getCacheDir().getAbsolutePath() + "/" + sUrl[1]);

                byte data[] = new byte[8192];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                    // Suppress warning
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }
    }
}