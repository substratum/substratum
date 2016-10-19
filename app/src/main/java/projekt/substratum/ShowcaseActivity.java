package projekt.substratum;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

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
import projekt.substratum.util.ReadShowcaseTabsFile;

/**
 * @author Nicholas Chum (nicholaschum)
 */

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
                    if (References.checkOMS()) {
                        playURL = getString(R.string.search_play_store_url);
                    } else {
                        playURL = getString(R.string.search_play_store_url_legacy);
                    }
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(playURL));
                    startActivity(i);
                } catch (ActivityNotFoundException activityNotFoundException) {
                    Toast toaster = Toast.makeText(getApplicationContext(), getString(R.string
                                    .activity_missing_toast),
                            Toast.LENGTH_SHORT);
                    toaster.show();
                }
                return true;
            case R.id.refresh:
                finish();
                startActivity(getIntent());
                return true;
        }
        return false;
    }

    private void swipeRefresh() {
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id
                .swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                finish();
                startActivity(getIntent());
            }
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(false);
        getSupportActionBar().setTitle(R.string.showcase);
        if (toolbar != null) toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        tabLayout = (TabLayout) findViewById(R.id.tabs);
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

            String[] checkerCommands = {getApplicationContext().getCacheDir() + "/" + result};
            final Map<String, String> newArray = ReadShowcaseTabsFile.main(checkerCommands);
            ArrayList<String> links = new ArrayList<>();

            for (String key : newArray.keySet()) {
                if (tabLayout != null) {
                    links.add(newArray.get(key));
                    tabLayout.addTab(tabLayout.newTab().setText(key));
                }
            }
            final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
            final ShowcaseTabsAdapter adapter = new ShowcaseTabsAdapter
                    (getSupportFragmentManager(), tabLayout.getTabCount(), getApplicationContext(),
                            links);
            if (viewPager != null) {
                viewPager.setOffscreenPageLimit(tabLayout.getTabCount());
                viewPager.setAdapter(adapter);
                viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener
                        (tabLayout));
                viewPager.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        swipeRefreshLayout.setEnabled(false);
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_UP:
                                swipeRefreshLayout.setEnabled(true);
                                break;
                        }
                        return false;
                    }
                });
                tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
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
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            File current_wallpapers = new File(getApplicationContext().getCacheDir() +
                    "/" + sUrl[1]);
            if (current_wallpapers.exists()) {
                current_wallpapers.delete();
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
                        getApplicationContext().getCacheDir().getAbsolutePath() + "/" + sUrl[1]);

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
            return sUrl[1];
        }
    }
}