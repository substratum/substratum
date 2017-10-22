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

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Lunchbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;

import projekt.substratum.R;
import projekt.substratum.adapters.showcase.ShowcaseTabsAdapter;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.util.files.FileDownloader;
import projekt.substratum.util.files.MD5;
import projekt.substratum.util.readers.ReadShowcaseTabsFile;

public class ShowcaseActivity extends AppCompatActivity {

    private RelativeLayout no_network;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SharedPreferences prefs;
    private LocalBroadcastManager localBroadcastManager;
    private AndromedaReceiver andromedaReceiver;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Systems.isAndromedaDevice(this.getApplicationContext())) {
            try {
                this.localBroadcastManager.unregisterReceiver(this.andromedaReceiver);
            } catch (final Exception e) {
                // Unregistered already
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        this.getMenuInflater().inflate(R.menu.showcase_menu, menu);

        final MenuItem alphabetizeMenu = menu.findItem(R.id.alphabetize);
        final boolean alphabetize = this.prefs.getBoolean("alphabetize_showcase", false);
        if (alphabetize) {
            alphabetizeMenu.setIcon(R.drawable.actionbar_alphabetize);
        } else {
            alphabetizeMenu.setIcon(R.drawable.actionbar_randomize);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.onBackPressed();
                return true;
            case R.id.search:
                try {
                    final String playURL;
                    if (Systems.checkOMS(this.getApplicationContext())) {
                        playURL = this.getString(R.string.search_play_store_url);
                    } else if (Systems.isSamsung(this.getApplicationContext())) {
                        playURL = this.getString(R.string.search_play_store_url_samsung);
                    } else {
                        playURL = this.getString(R.string.search_play_store_url_legacy);
                    }
                    final Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(playURL));
                    this.startActivity(i);
                } catch (final ActivityNotFoundException activityNotFoundException) {
                    Lunchbar.make(this.findViewById(android.R.id.content),
                            this.getString(R.string.activity_missing_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
                return true;
            case R.id.info:
                this.launchShowcaseInfo();
                return true;
            case R.id.alphabetize:
                final boolean alphabetize = this.prefs.getBoolean("alphabetize_showcase", false);
                if (!alphabetize) {
                    this.prefs.edit().putBoolean("alphabetize_showcase", true).apply();
                } else {
                    this.prefs.edit().putBoolean("alphabetize_showcase", false).apply();
                }
                this.recreate();
                return true;
        }
        return false;
    }

    private void swipeRefresh() {
        this.swipeRefreshLayout = this.findViewById(R.id.swipeRefreshLayout);
        this.swipeRefreshLayout.setOnRefreshListener(this::recreate);
    }

    private void refreshLayout() {
        if (References.isNetworkAvailable(this.getApplicationContext())) {
            this.no_network.setVisibility(View.GONE);
            final DownloadTabs downloadTabs = new DownloadTabs(this);
            downloadTabs.execute(this.getString(R.string.showcase_tabs), "showcase_tabs.xml");
        } else {
            this.no_network.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.showcase_activity);

        this.prefs = PreferenceManager.getDefaultSharedPreferences(
                this.getApplicationContext());

        if (Systems.isAndromedaDevice(this.getApplicationContext())) {
            this.andromedaReceiver = new ShowcaseActivity.AndromedaReceiver();
            this.localBroadcastManager = LocalBroadcastManager.getInstance(this.getApplicationContext());
            this.localBroadcastManager.registerReceiver(this.andromedaReceiver,
                    new IntentFilter("AndromedaReceiver.KILL"));
        }

        final Toolbar toolbar = this.findViewById(R.id.toolbar);
        if (toolbar != null) {
            this.setSupportActionBar(toolbar);
            if (this.getSupportActionBar() != null) {
                this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                this.getSupportActionBar().setHomeButtonEnabled(false);
                this.getSupportActionBar().setTitle(R.string.showcase);
            }
            toolbar.setNavigationOnClickListener((view) -> this.onBackPressed());
        }

        final File showcase_directory =
                new File(this.getApplicationContext().getCacheDir() + "/ShowcaseCache/");
        if (!showcase_directory.exists()) {
            final Boolean made = showcase_directory.mkdir();
            if (!made)
                Log.e(References.SUBSTRATUM_LOG, "Could not make showcase directory...");
        }

        final TabLayout tabLayout = this.findViewById(R.id.tabs);
        tabLayout.setTabTextColors(
                this.getColor(R.color.showcase_activity_text),
                this.getColor(R.color.showcase_activity_text));
        tabLayout.setVisibility(View.GONE);
        this.no_network = this.findViewById(R.id.no_network);
        this.refreshLayout();
        this.swipeRefresh();
    }

    private void launchShowcaseInfo() {
        final Dialog dialog = new Dialog(this, R.style.ShowcaseDialog);
        dialog.setContentView(R.layout.showcase_info);
        dialog.show();
    }

    private static class DownloadTabs extends AsyncTask<String, Integer, String> {

        private final WeakReference<ShowcaseActivity> showcaseActivityWR;

        DownloadTabs(final ShowcaseActivity activity) {
            super();
            this.showcaseActivityWR = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(final String result) {
            super.onPostExecute(result);

            if (result == null) {
                return;
            }

            final ShowcaseActivity activity = this.showcaseActivityWR.get();

            if (activity != null) {

                final TabLayout tabLayout = activity.findViewById(R.id.tabs);
                final ViewPager viewPager = activity.findViewById(R.id.viewpager);
                final SwipeRefreshLayout swipeRefreshLayout = activity.swipeRefreshLayout;

                if ((tabLayout == null) || (viewPager == null) || (swipeRefreshLayout == null)) {
                    return;
                }

                tabLayout.setVisibility(View.VISIBLE);

                String resultant = result;

                if (resultant.endsWith("-temp.xml")) {
                    final String existing = MD5.calculateMD5(new File(activity.getCacheDir() +
                            "/ShowcaseCache/" + "showcase_tabs.xml"));
                    final String new_file = MD5.calculateMD5(new File(activity.getCacheDir() +
                            "/ShowcaseCache/" + "showcase_tabs-temp.xml"));
                    if ((existing != null) && !existing.equals(new_file)) {
                        // MD5s don't match
                        final File renameMe = new File(activity.getCacheDir() +
                                "/ShowcaseCache/" + "showcase_tabs-temp.xml");
                        final boolean move = renameMe.renameTo(
                                new File(activity.getCacheDir() +
                                        "/ShowcaseCache/" + "showcase_tabs.xml"));
                        if (move) Log.e("SubstratumShowcase",
                                "Successfully updated the showcase tabs database");
                    } else {
                        final File deleteMe = new File(activity.getCacheDir() +
                                "/ShowcaseCache/" + "showcase_tabs-temp.xml");
                        final boolean deleted = deleteMe.delete();
                        if (!deleted) Log.e("SubstratumShowcase",
                                "Unable to delete temporary tab file.");
                    }
                }

                resultant = "showcase_tabs.xml";

                final String[] checkerCommands = {activity.getCacheDir() +
                        "/ShowcaseCache/" + resultant};

                final Map<String, String> newArray =
                        ReadShowcaseTabsFile.main(checkerCommands);

                final ArrayList<String> links = new ArrayList<>();

                newArray.keySet()
                        .forEach(key -> {
                            links.add(newArray.get(key));
                            tabLayout.addTab(tabLayout.newTab().setText(key));
                        });

                final ShowcaseTabsAdapter adapter = new ShowcaseTabsAdapter(
                        activity.getSupportFragmentManager(),
                        tabLayout.getTabCount(),
                        links);

                viewPager.setOffscreenPageLimit(tabLayout.getTabCount());
                viewPager.setAdapter(adapter);
                viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener
                        (tabLayout));

                //Fix for SwipeToRefresh in ViewPager (without it horizontal scrolling is impossible)
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
                    public void onTabSelected(final TabLayout.Tab tab) {
                        viewPager.setCurrentItem(tab.getPosition());
                    }

                    @Override
                    public void onTabUnselected(final TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(final TabLayout.Tab tab) {
                    }
                });
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            String inputFileName = sUrl[1];

            final Activity activity = this.showcaseActivityWR.get();

            if (activity != null) {

                final File current_wallpapers = new File(
                        activity.getCacheDir() + "/ShowcaseCache/" + inputFileName);
                if (current_wallpapers.exists()) {
                    // We create a temporary file to check whether we should be replacing the current
                    inputFileName = inputFileName.substring(0, inputFileName.length() - 4) +
                            "-temp.xml";
                }

                FileDownloader.init(activity, sUrl[0], inputFileName, "ShowcaseCache");
                return inputFileName;
            } else {
                return null;
            }
        }
    }

    class AndromedaReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            ShowcaseActivity.this.finish();
        }
    }
}