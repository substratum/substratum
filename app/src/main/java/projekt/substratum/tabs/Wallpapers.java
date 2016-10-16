package projekt.substratum.tabs;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.adapters.WallpaperAdapter;
import projekt.substratum.model.WallpaperEntries;
import projekt.substratum.util.ReadCloudWallpaperFile;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class Wallpapers extends Fragment {

    private ViewGroup root;
    private String wallpaperUrl;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        wallpaperUrl = InformationActivity.getWallpaperUrl();
        root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_6, container, false);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        mRecyclerView = (RecyclerView) root.findViewById(R.id.wallpaperRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayList<WallpaperEntries> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new WallpaperAdapter(empty_array);
        mRecyclerView.setAdapter(empty_adapter);

        downloadResources downloadTask = new downloadResources();
        downloadTask.execute(wallpaperUrl, "current_wallpapers.xml");

        return root;
    }

    private class downloadResources extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            String[] checkerCommands = {getContext().getCacheDir() + "/current_wallpapers.xml"};

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
            mAdapter = new WallpaperAdapter(wallpapers);
            mRecyclerView.setAdapter(mAdapter);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            try {
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

                byte data[] = new byte[4096];
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
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }
    }
}