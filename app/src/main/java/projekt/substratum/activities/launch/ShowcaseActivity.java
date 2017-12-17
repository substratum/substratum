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

package projekt.substratum.activities.launch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Lunchbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import projekt.substratum.R;
import projekt.substratum.adapters.showcase.ShowcaseTabsAdapter;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.util.files.FileDownloader;
import projekt.substratum.util.files.MD5;
import projekt.substratum.util.helpers.ContextWrapper;
import projekt.substratum.util.readers.ReadShowcaseTabsFile;

import static projekt.substratum.common.Internal.ANDROMEDA_RECEIVER;
import static projekt.substratum.common.Internal.SHOWCASE_CACHE;

public class ShowcaseActivity extends AppCompatActivity {

    private static final String TAG = "ShowcaseActivity";
    @BindView(R.id.no_network)
    RelativeLayout no_network;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.viewpager)
    ViewPager viewPager;
    @BindView(android.R.id.content)
    ViewGroup masterView;
    private SharedPreferences prefs;
    private LocalBroadcastManager localBroadcastManager;
    private AndromedaReceiver andromedaReceiver;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Systems.isAndromedaDevice(getApplicationContext())) {
            try {
                localBroadcastManager.unregisterReceiver(andromedaReceiver);
            } catch (Exception e) {
                // Unregistered already
            }
        }
    }

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
                    if (Systems.checkOMS(getApplicationContext())) {
                        playURL = getString(R.string.search_play_store_url);
                    } else if (Systems.isSamsung(getApplicationContext())) {
                        playURL = getString(R.string.search_play_store_url_samsung);
                    } else {
                        playURL = getString(R.string.search_play_store_url_legacy);
                    }
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(playURL));
                    startActivity(i);
                } catch (ActivityNotFoundException activityNotFoundException) {
                    Lunchbar.make(masterView,
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
                recreate();
                return true;
        }
        return false;
    }

    /**
     * Refresh the showcase layout by redownloading the tabs
     */
    private void refreshLayout() {
        if (References.isNetworkAvailable(getApplicationContext())) {
            no_network.setVisibility(View.GONE);
            DownloadTabs downloadTabs = new DownloadTabs(this);
            downloadTabs.execute(getString(R.string.showcase_tabs), "showcase_tabs.xml");
        } else {
            no_network.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.showcase_activity);
        ButterKnife.bind(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Check if we should activate the custom font
        boolean bottomBarUi = !prefs.getBoolean("advanced_ui", false);
        if (bottomBarUi) {
            setTheme(R.style.AppTheme_SpecialUI);
            // Change the toolbar font
            for (int i = 0; i < toolbar.getChildCount(); i++) {
                View child = toolbar.getChildAt(i);
                if (child instanceof TextView) {
                    Typeface typeface = ResourcesCompat.getFont(this, R.font.toolbar_new_ui);
                    TextView textView = ((TextView) child);
                    textView.setTypeface(typeface);
                    textView.setTextSize(22);
                    break;
                }
            }
        }

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setTitle(R.string.showcase);
        }
        toolbar.setNavigationOnClickListener((view) -> onBackPressed());

        swipeRefreshLayout.setOnRefreshListener(this::recreate);

        if (Systems.isAndromedaDevice(getApplicationContext())) {
            andromedaReceiver = new ShowcaseActivity.AndromedaReceiver();
            localBroadcastManager = LocalBroadcastManager.getInstance(this
                    .getApplicationContext());
            localBroadcastManager.registerReceiver(andromedaReceiver,
                    new IntentFilter(ANDROMEDA_RECEIVER));
        }

        File showcase_directory =
                new File(getApplicationContext().getCacheDir() + SHOWCASE_CACHE);
        if (!showcase_directory.exists()) {
            Boolean made = showcase_directory.mkdir();
            if (!made)
                Log.e(TAG, "Could not make showcase directory...");
        }

        tabLayout.setTabTextColors(
                getColor(R.color.showcase_activity_text),
                getColor(R.color.showcase_activity_text));
        tabLayout.setVisibility(View.GONE);
        refreshLayout();
    }

    /**
     * Launch information on what the small icons mean
     */
    private void launchShowcaseInfo() {
        Dialog dialog = new Dialog(this, R.style.ShowcaseDialog);
        dialog.setContentView(R.layout.showcase_info);
        dialog.show();
    }

    /**
     * Attach the base context for locale changes
     *
     * @param context Self explanatory, bud.
     */
    @Override
    protected void attachBaseContext(Context context) {
        Context newBase = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean languageCheck = prefs.getBoolean("force_english", false);
        if (languageCheck) {
            Locale newLocale = new Locale(Locale.ENGLISH.getLanguage());
            newBase = ContextWrapper.wrapNewLocale(context, newLocale);
        }
        super.attachBaseContext(newBase);
    }

    /**
     * Class to download the tabs from the GitHub organization
     */
    private static class DownloadTabs extends AsyncTask<String, Integer, String> {

        private WeakReference<ShowcaseActivity> showcaseActivityWR;

        DownloadTabs(ShowcaseActivity activity) {
            super();
            showcaseActivityWR = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result == null) {
                return;
            }

            ShowcaseActivity activity = showcaseActivityWR.get();

            if (activity != null) {
                activity.tabLayout.setVisibility(View.VISIBLE);
                String resultant = result;
                if (resultant.endsWith("-temp.xml")) {
                    String existing = MD5.calculateMD5(new File(activity.getCacheDir() +
                            SHOWCASE_CACHE + "showcase_tabs.xml"));
                    String new_file = MD5.calculateMD5(new File(activity.getCacheDir() +
                            SHOWCASE_CACHE + "showcase_tabs-temp.xml"));
                    if ((existing != null) && !existing.equals(new_file)) {
                        // MD5s don't match
                        File renameMe = new File(activity.getCacheDir() +
                                SHOWCASE_CACHE + "showcase_tabs-temp.xml");
                        boolean move = renameMe.renameTo(
                                new File(activity.getCacheDir() +
                                        SHOWCASE_CACHE + "showcase_tabs.xml"));
                        if (move) Log.e(TAG, "Successfully updated the showcase tabs database");
                    } else {
                        File deleteMe = new File(activity.getCacheDir() +
                                SHOWCASE_CACHE + "showcase_tabs-temp.xml");
                        boolean deleted = deleteMe.delete();
                        if (!deleted) Log.e(TAG, "Unable to delete temporary tab file.");
                    }
                }

                resultant = "showcase_tabs.xml";

                Map<String, String> newArray =
                        ReadShowcaseTabsFile.read(activity.getCacheDir() +
                                SHOWCASE_CACHE + resultant);

                ArrayList<String> links = new ArrayList<>();

                newArray.keySet()
                        .forEach(key -> {
                            links.add(newArray.get(key));
                            activity.tabLayout.addTab(activity.tabLayout.newTab().setText(key));
                        });

                ShowcaseTabsAdapter adapter = new ShowcaseTabsAdapter(
                        activity.getSupportFragmentManager(),
                        activity.tabLayout.getTabCount(),
                        links);

                activity.viewPager.setOffscreenPageLimit(activity.tabLayout.getTabCount());
                activity.viewPager.setAdapter(adapter);
                activity.viewPager.addOnPageChangeListener(
                        new TabLayout.TabLayoutOnPageChangeListener(activity.tabLayout));

                // Fix for SwipeToRefresh in ViewPager
                // (without it horizontal scrolling is impossible)
                activity.viewPager.setOnTouchListener((v, event) -> {
                    activity.swipeRefreshLayout.setEnabled(false);
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            activity.swipeRefreshLayout.setEnabled(true);
                            break;
                    }
                    return false;
                });
                activity.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        activity.viewPager.setCurrentItem(tab.getPosition());
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

            Activity activity = showcaseActivityWR.get();

            if (activity != null) {

                File current_wallpapers = new File(
                        activity.getCacheDir() + SHOWCASE_CACHE + inputFileName);
                if (current_wallpapers.exists()) {
                    // We create a temporary file to check whether we should be replacing the
                    // current
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

    /**
     * Receiver to pick up when Andromeda is no longer connected
     */
    class AndromedaReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }
}