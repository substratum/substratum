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
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.databinding.ShowcaseActivityBinding;
import projekt.substratum.fragments.ShowcaseTab;
import projekt.substratum.util.helpers.FileDownloader;
import projekt.substratum.util.helpers.LocaleHelper;
import projekt.substratum.util.helpers.MD5;
import projekt.substratum.util.readers.ReadShowcaseTabsFile;
import projekt.substratum.util.views.Lunchbar;

import static projekt.substratum.common.Internal.SHOWCASE_CACHE;

public class ShowcaseActivity extends AppCompatActivity {

    private static final String TAG = "ShowcaseActivity";
    private static final String showcaseTabsFile = "showcase_tabs.xml";
    private static final String showcaseTabsTempFile = "showcase_tabs-temp.xml";
    private RelativeLayout noNetwork;
    private Toolbar toolbar;
    private ViewGroup masterView;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.showcase:
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
                            Snackbar.LENGTH_LONG)
                            .show();
                }
                return true;
            case R.id.info:
                launchShowcaseInfo();
                return true;
            case R.id.filter:
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawer(navigationView);
                } else {
                    drawerLayout.openDrawer(navigationView);
                }

        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.showcase_menu, menu);
        return true;
    }

    /**
     * Refresh the showcase layout by redownloading the tabs
     */
    private void refreshLayout() {
        if (References.isNetworkAvailable(getApplicationContext())) {
            noNetwork.setVisibility(View.GONE);
            DownloadTabs downloadTabs = new DownloadTabs(this);
            downloadTabs.execute(getString(R.string.showcase_tabs), showcaseTabsFile);
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            noNetwork.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShowcaseActivityBinding binding =
                DataBindingUtil.setContentView(this, R.layout.showcase_activity);

        noNetwork = binding.noNetwork;
        toolbar = binding.toolbar;
        masterView = binding.rootView;
        navigationView = binding.navigationView;
        drawerLayout = binding.drawerLayout;

        toolbar.setOnClickListener(v -> {
            if (ShowcaseTab.recyclerView != null)
                ShowcaseTab.recyclerView.smoothScrollToPosition(0);
        });

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setTitle(R.string.showcase);
        }
        toolbar.setNavigationOnClickListener((view) -> onBackPressed());

        File showcase_directory =
                new File(getApplicationContext().getCacheDir() + SHOWCASE_CACHE);
        if (!showcase_directory.exists()) {
            boolean made = showcase_directory.mkdir();
            if (!made)
                Log.e(TAG, "Could not make showcase directory...");
        }
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
     * Class to download the tabs from the GitHub organization
     */
    private static class DownloadTabs extends AsyncTask<String, Integer, String> {

        private WeakReference<ShowcaseActivity> showcaseActivityWR;

        DownloadTabs(ShowcaseActivity activity) {
            super();
            showcaseActivityWR = new WeakReference<>(activity);
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
                String resultant = result;
                if (resultant.endsWith("-temp.xml")) {
                    String existing = MD5.calculateMD5(new File(activity.getCacheDir() +
                            SHOWCASE_CACHE + showcaseTabsFile));
                    String new_file = MD5.calculateMD5(new File(activity.getCacheDir() +
                            SHOWCASE_CACHE + showcaseTabsTempFile));
                    if ((existing != null) && !existing.equals(new_file)) {
                        // MD5s don't match
                        File renameMe = new File(activity.getCacheDir() +
                                SHOWCASE_CACHE + showcaseTabsTempFile);
                        boolean move = renameMe.renameTo(
                                new File(activity.getCacheDir() +
                                        SHOWCASE_CACHE + showcaseTabsFile));
                        if (move) {
                            Log.e(TAG, "Successfully updated the showcase tabs database.");
                        }
                    } else {
                        File deleteMe = new File(activity.getCacheDir() +
                                SHOWCASE_CACHE + showcaseTabsTempFile);
                        boolean deleted = deleteMe.delete();
                        if (!deleted) {
                            Log.e(TAG, "Unable to delete temporary tab file.");
                        }
                    }
                }

                resultant = showcaseTabsFile;

                Map<String, String> newArray =
                        ReadShowcaseTabsFile.read(activity.getCacheDir() +
                                SHOWCASE_CACHE + resultant);

                List<String> listOfTitles = new ArrayList<>();
                List<String> listOfLinks = new ArrayList<>(newArray.values());
                Menu menu = activity.navigationView.getMenu();
                int i = 0; // Beautifully count for menus unique id
                for (String tabName : newArray.keySet()) {
                    listOfTitles.add(tabName);
                    menu.add(Menu.NONE, i, Menu.NONE, tabName);
                    i++;
                }

                activity.navigationView.setNavigationItemSelectedListener(item -> {
                    int position = item.getItemId();
                    switchFragment(
                            activity,
                            position,
                            listOfLinks.get(position),
                            listOfTitles.get(position));
                    item.setCheckable(true);
                    activity.navigationView.setCheckedItem(position);
                    activity.drawerLayout.closeDrawer(activity.navigationView);
                    return false;
                });

                // Update menus
                activity.navigationView.invalidate();

                switchFragment(
                        activity,
                        0,
                        listOfLinks.get(0),
                        listOfTitles.get(0)
                );

                new Handler().postDelayed(() -> {
                    activity.drawerLayout.openDrawer(activity.navigationView);
                    activity.navigationView.getMenu()
                            .getItem(0)
                            .setCheckable(true)
                            .setChecked(true);
                }, 500);
            }
        }

        private void switchFragment(ShowcaseActivity activity,
                                    int position,
                                    String link_address,
                                    String title) {
            FragmentTransaction tx = activity.getSupportFragmentManager().beginTransaction();
            Bundle bundle = new Bundle();
            bundle.putInt("tab_count", position);
            bundle.putString("tabbed_address", link_address);
            Fragment fragment = new ShowcaseTab();
            fragment.setArguments(bundle);
            tx.replace(R.id.main, fragment);
            tx.commitAllowingStateLoss();
            activity.toolbar.setTitle(title);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            String inputFileName = sUrl[1];

            Activity activity = showcaseActivityWR.get();

            if (activity != null) {

                File currentWallpapers = new File(
                        activity.getCacheDir() + SHOWCASE_CACHE + inputFileName);
                if (currentWallpapers.exists()) {
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
}