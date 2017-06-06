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

package projekt.substratum.activities.showcase;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Lunchbar;
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
import java.util.ArrayList;
import java.util.Map;

import projekt.substratum.R;
import projekt.substratum.adapters.showcase.ShowcaseTabsAdapter;
import projekt.substratum.common.References;
import projekt.substratum.util.files.FileDownloader;
import projekt.substratum.util.files.MD5;
import projekt.substratum.util.readers.ReadShowcaseTabsFile;

public class ShowcaseActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RelativeLayout no_network;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SharedPreferences prefs;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.showcase_menu, menu);

        MenuItem alphabetizeMenu = menu.findItem(R.id.alphabetize);
        boolean alphabetize = prefs.getBoolean("alphabetize_showcase", false);
        if (alphabetize) {
            alphabetizeMenu.setIcon(R.drawable.actionbar_alphabetize);
        } else {
            alphabetizeMenu.setIcon(R.drawable.actionbar_randomize);
        }
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
                    Lunchbar.make(findViewById(android.R.id.content),
                            getString(R.string.activity_missing_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
                return true;
            case R.id.info:
                launchShowcaseInfo();
                return true;
            case R.id.alphabetize:
                boolean alphabetize = prefs.getBoolean("alphabetize_showcase", false);
                if (!alphabetize) {
                    prefs.edit().putBoolean("alphabetize_showcase", true).apply();
                } else {
                    prefs.edit().putBoolean("alphabetize_showcase", false).apply();
                }
                this.recreate();
                return true;
        }
        return false;
    }

    private void swipeRefresh() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::recreate);
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

        prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(false);
                getSupportActionBar().setTitle(R.string.showcase);
            }
            toolbar.setNavigationOnClickListener((view) -> onBackPressed());
        }

        File showcase_directory =
                new File(getApplicationContext().getCacheDir() + "/ShowcaseCache/");
        if (!showcase_directory.exists()) {
            Boolean made = showcase_directory.mkdir();
            if (!made)
                Log.e(References.SUBSTRATUM_LOG, "Could not make showcase directory...");
        }

        tabLayout = findViewById(R.id.tabs);
        tabLayout.setTabTextColors(
                getColor(R.color.showcase_activity_text),
                getColor(R.color.showcase_activity_text));
        tabLayout.setVisibility(View.GONE);
        no_network = findViewById(R.id.no_network);
        refreshLayout();
        swipeRefresh();
    }

    private void launchShowcaseInfo() {
        Dialog dialog = new Dialog(this, R.style.ShowcaseDialog);
        dialog.setContentView(R.layout.showcase_info);
        dialog.show();
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

            @SuppressWarnings("unchecked") final Map<String, String> newArray =
                    ReadShowcaseTabsFile.main(checkerCommands);

            ArrayList<String> links = new ArrayList<>();

            newArray.keySet().stream().filter(key -> tabLayout != null).forEach(key -> {
                links.add(newArray.get(key));
                tabLayout.addTab(tabLayout.newTab().setText(key));
            });
            final ViewPager viewPager = findViewById(R.id.viewpager);
            final ShowcaseTabsAdapter adapter = new ShowcaseTabsAdapter(
                    getSupportFragmentManager(),
                    tabLayout.getTabCount(),
                    links);
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

            File current_wallpapers = new File(
                    getApplicationContext().getCacheDir() + "/ShowcaseCache/" + inputFileName);
            if (current_wallpapers.exists()) {
                // We create a temporary file to check whether we should be replacing the current
                inputFileName = inputFileName.substring(0, inputFileName.length() - 4) +
                        "-temp.xml";
            }

            FileDownloader.init(getApplicationContext(), sUrl[0], inputFileName, "ShowcaseCache");
            return inputFileName;
        }
    }
}