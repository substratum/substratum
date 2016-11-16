package projekt.substratum;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
                    Toast toaster = Toast.makeText(getApplicationContext(), getString(R.string
                                    .activity_missing_toast),
                            Toast.LENGTH_SHORT);
                    toaster.show();
                }
                return true;
            case R.id.refresh:
                References.delete(getApplicationContext().getCacheDir().getAbsolutePath() +
                        "/ShowcaseCache/");
                SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                        "showcase_tabs", 0);
                prefs.edit().clear().apply();
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

            final SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                    "showcase_tabs", 0);
            if (!prefs.contains("acknowledgement")) {
                final AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle(R.string.showcase_dialog_title);
                alertDialogBuilder.setMessage(R.string.showcase_dialog_content);
                alertDialogBuilder.setCancelable(false);
                alertDialogBuilder
                        .setPositiveButton(R.string.showcase_dialog_agree, new DialogInterface
                                .OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                prefs.edit().putBoolean("acknowledgement", true).apply();
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
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
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(false);
                getSupportActionBar().setTitle(R.string.showcase);
            }
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
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
                        "/" + "showcase_tabs.xml"));
                String new_file = MD5.calculateMD5(new File(getApplicationContext().getCacheDir() +
                        "/" + "showcase_tabs-temp.xml"));
                if (existing != null && !existing.equals(new_file)) {
                    // MD5s don't match
                    File renameMe = new File(getApplicationContext().getCacheDir() +
                            "/" + "showcase_tabs-temp.xml");
                    boolean move = renameMe.renameTo(
                            new File(getApplicationContext().getCacheDir() +
                                    "/" + "showcase_tabs.xml"));
                    if (move) {
                        // Also clear the tabs cache
                        References.delete(getApplicationContext().getCacheDir().getAbsolutePath() +
                                "/ShowcaseCache/");
                        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                                "showcase_tabs", 0);
                        prefs.edit().clear().apply();
                    }
                } else {
                    File deleteMe = new File(getApplicationContext().getCacheDir() +
                            "/" + "showcase_tabs-temp.xml");
                    boolean deleted = deleteMe.delete();
                    if (!deleted) Log.e("SubstratumShowcase",
                            "Unable to delete temporary tab file.");
                }
            }

            resultant = "showcase_tabs.xml";

            String[] checkerCommands = {getApplicationContext().getCacheDir() + "/" + resultant};

            @SuppressWarnings("unchecked")
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
                    (getSupportFragmentManager(), tabLayout.getTabCount(), links);
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
                    "/" + inputFileName);
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
                        getApplicationContext().getCacheDir().getAbsolutePath() + "/" +
                                inputFileName);

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