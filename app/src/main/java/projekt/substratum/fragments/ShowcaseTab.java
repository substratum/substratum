package projekt.substratum.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import projekt.substratum.R;
import projekt.substratum.adapters.ShowcaseItemAdapter;
import projekt.substratum.adapters.WallpaperAdapter;
import projekt.substratum.config.References;
import projekt.substratum.model.ShowcaseItem;
import projekt.substratum.model.WallpaperEntries;
import projekt.substratum.util.MD5;
import projekt.substratum.util.ReadCloudShowcaseFile;

public class ShowcaseTab extends Fragment {

    private ViewGroup root;
    private RecyclerView mRecyclerView;
    private ShowcaseItemAdapter mAdapter;
    private MaterialProgressBar materialProgressBar;
    private View no_network, no_wallpapers;
    private int current_tab_position;
    private String current_tab_address;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

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
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayList<WallpaperEntries> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new WallpaperAdapter(empty_array);
        mRecyclerView.setAdapter(empty_adapter);
        no_wallpapers.setVisibility(View.GONE);
        no_network.setVisibility(View.GONE);

        if (References.isNetworkAvailable(getContext())) {
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

            mAdapter = new ShowcaseItemAdapter(result);
            mRecyclerView.setAdapter(mAdapter);

            if (result.size() == 0) no_wallpapers.setVisibility(View.VISIBLE);

            mRecyclerView.setVisibility(View.VISIBLE);
            materialProgressBar.setVisibility(View.GONE);
        }

        @Override
        protected ArrayList doInBackground(String... sUrl) {
            String inputFileName = sUrl[1];
            ArrayList<ShowcaseItem> wallpapers = new ArrayList<>();
            try {
                InputStream input = null;
                OutputStream output = null;
                HttpURLConnection connection = null;

                File showcase_directory = new File(getContext().getCacheDir() +
                        "/ShowcaseCache/");
                if (!showcase_directory.exists()) {
                    Boolean made = showcase_directory.mkdir();
                    if (!made)
                        Log.e(References.SUBSTRATUM_LOG, "Could not make showcase directory...");
                }

                File current_wallpapers = new File(getContext().getCacheDir() +
                        "/ShowcaseCache/" + inputFileName);
                if (current_wallpapers.exists()) {
                    // We create a temporary file to check whether we should be replacing the
                    // current
                    inputFileName = inputFileName.substring(0, inputFileName.length() - 4) +
                            ".xml";
                }

                try {
                    URL url = new URL(sUrl[0]);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    // expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        Log.e("Server returned HTTP", connection.getResponseCode()
                                + " " + connection.getResponseMessage());
                    }

                    // this will be useful to display download percentage
                    // might be -1: server did not report the length
                    int fileLength = connection.getContentLength();

                    // download the file
                    input = connection.getInputStream();

                    output = new FileOutputStream(
                            getContext().getCacheDir().getAbsolutePath() +
                                    "/ShowcaseCache/" + inputFileName);

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
                    e.printStackTrace();
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

                if (inputFileName.endsWith("-temp.xml")) {
                    String existing = MD5.calculateMD5(new File(getContext().getCacheDir() +
                            "/ShowcaseCache/" + sUrl[1]));
                    String new_file = MD5.calculateMD5(new File(getContext().getCacheDir() +
                            "/ShowcaseCache/" + inputFileName));
                    if (existing != null && !existing.equals(new_file)) {
                        Log.e("ShowcaseActivity", "Tab " + current_tab_position +
                                " has been updated from the cloud!");
                        File renameMe = new File(getContext().getCacheDir() +
                                "/ShowcaseCache/" +
                                sUrl[1].substring(0, sUrl[1].length() - 4) + "-temp.xml");
                        Boolean renamed = renameMe.renameTo(new File(getContext().getCacheDir() +
                                "/ShowcaseCache/" + sUrl[1]));
                        if (!renamed) Log.e(References.SUBSTRATUM_LOG,
                                "Could not replace the old tab file with the new tab file...");
                    } else {
                        File deleteMe = new File(getContext().getCacheDir() +
                                "/" + inputFileName);
                        Boolean deleted = deleteMe.delete();
                        if (!deleted) Log.e(References.SUBSTRATUM_LOG,
                                "Could not delete temporary tab file...");
                    }
                }

                inputFileName = sUrl[1];

                String[] checkerCommands = {getContext().getCacheDir() +
                        "/ShowcaseCache/" + inputFileName};

                @SuppressWarnings("unchecked")
                final Map<String, String> newArray = ReadCloudShowcaseFile.main(checkerCommands);
                ShowcaseItem newEntry = new ShowcaseItem();

                for (String key : newArray.keySet()) {
                    if (!key.toLowerCase().contains("-".toLowerCase())) {
                        newEntry.setContext(getContext());
                        newEntry.setThemeName(key);
                        newEntry.setThemeLink(newArray.get(key));
                    } else {
                        if (key.toLowerCase().contains("-author".toLowerCase())) {
                            newEntry.setThemeAuthor(newArray.get(key));
                        } else if (key.toLowerCase().contains("-pricing".toLowerCase())) {
                            newEntry.setThemePricing(newArray.get(key));
                        } else if (key.toLowerCase().contains("-image-override")) {
                            newEntry.setThemeIcon(newArray.get(key));
                        } else if (key.toLowerCase().contains("-support".toLowerCase())) {
                            newEntry.setThemeSupport(newArray.get(key));
                            wallpapers.add(newEntry);
                            newEntry = new ShowcaseItem();
                            newEntry.setContext(getContext());
                        }
                    }
                }
            } catch (Exception e) {
                //
            }
            return wallpapers;
        }
    }
}