package projekt.substratum;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import projekt.substratum.adapters.ShowcaseTabsAdapter;
import projekt.substratum.config.References;
import projekt.substratum.util.MD5;
import projekt.substratum.util.ReadShowcaseTabsFile;

public class ShowcaseActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RelativeLayout no_network;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.showcase_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.search:
                try {
                    String playURL;
                    if (References.checkOMS(getApplicationContext())) {
                        playURL = getString(R.string.search_play_store_url);
                    } else {
                        playURL = getString(R.string.search_play_store_url_legacy);
                    }
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(playURL));
                    startActivity(i);
                } catch (ActivityNotFoundException activityNotFoundException) {
                    Snackbar.make(findViewById(android.R.id.content),
                            getString(R.string.activity_missing_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
                return true;
        }
        return false;
    }

    private void swipeRefresh() {
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id
                .swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            finish();
            startActivity(getIntent());
        });
    }

    private void refreshLayout() {
        if (References.isNetworkAvailable(getApplicationContext())) {
            no_network.setVisibility(View.GONE);
            DownloadTabs downloadTabs = new DownloadTabs();
            downloadTabs.execute(getString(R.string.showcase_tabs), "showcase_tabs.xml");
        } else {
            no_network.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.showcase_activity);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());

        boolean languageCheck = prefs.getBoolean("force_english", false);
        if (languageCheck) {
            References.forceEnglishLocale(getApplicationContext());
        } else {
            References.forceSystemLocale(getApplicationContext());
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(false);
                getSupportActionBar().setTitle(R.string.showcase);
            }
            toolbar.setNavigationOnClickListener((view) -> onBackPressed());
        }

        File showcase_directory = new File(getApplicationContext().getCacheDir() +
                "/ShowcaseCache/");
        if (!showcase_directory.exists()) {
            Boolean made = showcase_directory.mkdir();
            if (!made)
                Log.e(References.SUBSTRATUM_LOG, "Could not make showcase directory...");
        }

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setTabTextColors(
                getColor(R.color.showcase_activity_text),
                getColor(R.color.showcase_activity_text));
        tabLayout.setVisibility(View.GONE);
        no_network = (RelativeLayout) findViewById(R.id.no_network);
        refreshLayout();
        swipeRefresh();
    }

    private class DownloadTabs extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            tabLayout.setVisibility(View.VISIBLE);

            String resultant = result;

            if (resultant.endsWith("-temp.xml")) {
                String existing = MD5.calculateMD5(new File(getApplicationContext().getCacheDir() +
                        "/ShowcaseCache/" + "showcase_tabs.xml"));
                String new_file = MD5.calculateMD5(new File(getApplicationContext().getCacheDir() +
                        "/ShowcaseCache/" + "showcase_tabs-temp.xml"));
                if (existing != null && !existing.equals(new_file)) {
                    // MD5s don't match
                    File renameMe = new File(getApplicationContext().getCacheDir() +
                            "/ShowcaseCache/" + "showcase_tabs-temp.xml");
                    boolean move = renameMe.renameTo(
                            new File(getApplicationContext().getCacheDir() +
                                    "/ShowcaseCache/" + "showcase_tabs.xml"));
                    if (move) Log.e("SubstratumShowcase",
                            "Successfully updated the showcase tabs database");
                } else {
                    File deleteMe = new File(getApplicationContext().getCacheDir() +
                            "/ShowcaseCache/" + "showcase_tabs-temp.xml");
                    boolean deleted = deleteMe.delete();
                    if (!deleted) Log.e("SubstratumShowcase",
                            "Unable to delete temporary tab file.");
                }
            }

            resultant = "showcase_tabs.xml";

            String[] checkerCommands = {getApplicationContext().getCacheDir() +
                    "/ShowcaseCache/" + resultant};

            @SuppressWarnings("unchecked")
            final Map<String, String> newArray = ReadShowcaseTabsFile.main(checkerCommands);

            ArrayList<String> links = new ArrayList<>();

            newArray.keySet().stream().filter(key -> tabLayout != null).forEach(key -> {
                links.add(newArray.get(key));
                tabLayout.addTab(tabLayout.newTab().setText(key));
            });
            final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
            final ShowcaseTabsAdapter adapter = new ShowcaseTabsAdapter
                    (getSupportFragmentManager(), tabLayout.getTabCount(), links);
            if (viewPager != null) {
                viewPager.setOffscreenPageLimit(tabLayout.getTabCount());
                viewPager.setAdapter(adapter);
                viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener
                        (tabLayout));
                viewPager.setOnTouchListener((v, event) -> {
                    swipeRefreshLayout.setEnabled(false);
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            swipeRefreshLayout.setEnabled(true);
                            break;
                    }
                    return false;
                });
                tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        viewPager.setCurrentItem(tab.getPosition());
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                    }
                });
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            String inputFileName = sUrl[1];

            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            File current_wallpapers = new File(getApplicationContext().getCacheDir() +
                    "/ShowcaseCache/" + inputFileName);
            if (current_wallpapers.exists()) {
                // We create a temporary file to check whether we should be replacing the current
                inputFileName = inputFileName.substring(0, inputFileName.length() - 4) + "-temp" +
                        ".xml";
            }

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
                        getApplicationContext().getCacheDir().getAbsolutePath() +
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
            return inputFileName;
        }
    }
}