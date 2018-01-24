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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.fragments.ShowcaseTab;
import projekt.substratum.util.helpers.FileDownloader;
import projekt.substratum.util.helpers.MD5;
import projekt.substratum.util.readers.ReadShowcaseTabsFile;
import projekt.substratum.util.views.Lunchbar;

import static projekt.substratum.common.Internal.ANDROMEDA_RECEIVER;
import static projekt.substratum.common.Internal.SHOWCASE_CACHE;

public class ShowcaseActivity extends AppCompatActivity {

    private static final String TAG = "ShowcaseActivity";
    @BindView(R.id.no_network)
    RelativeLayout no_network;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(android.R.id.content)
    ViewGroup masterView;
    private LocalBroadcastManager localBroadcastManager;
    private AndromedaReceiver andromedaReceiver;
    private Bundle savedInstanceState;
    private Drawer drawer;

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
    public void onBackPressed() {
        if (drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else if (drawer.getCurrentSelectedPosition() != 0) {
            drawer.setSelectionAtPosition(0);
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
                            Snackbar.LENGTH_LONG)
                            .show();
                }
                return true;
            case R.id.info:
                launchShowcaseInfo();
                return true;
            case R.id.filter:
                if (drawer != null) {
                    if (drawer.isDrawerOpen()) {
                        drawer.closeDrawer();
                    } else {
                        drawer.openDrawer();
                    }
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
            no_network.setVisibility(View.GONE);
            DownloadTabs downloadTabs = new DownloadTabs(this);
            downloadTabs.execute(getString(R.string.showcase_tabs), "showcase_tabs.xml");
        } else {
            no_network.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean bottomBarUi = !prefs.getBoolean("advanced_ui", false);
        if (bottomBarUi) setTheme(R.style.AppTheme_SpecialUI);

        this.savedInstanceState = savedInstanceState;
        super.onCreate(savedInstanceState);

        setContentView(R.layout.showcase_activity);
        ButterKnife.bind(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Substratum.setLocale(prefs.getBoolean("force_english", false));

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setTitle(R.string.showcase);
        }
        toolbar.setNavigationOnClickListener((view) -> {
            if (drawer.isDrawerOpen()) {
                drawer.closeDrawer();
            } else {
                finish();
            }
        });

        if (bottomBarUi) {
            // Change the toolbar title size
            for (int i = 0; i < toolbar.getChildCount(); i++) {
                View child = toolbar.getChildAt(i);
                if (child instanceof TextView) {
                    TextView textView = ((TextView) child);
                    textView.setTextSize(22);
                    break;
                }
            }
        }

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
                        if (move) {
                            Log.e(TAG, "Successfully updated the showcase tabs database.");
                        }
                    } else {
                        File deleteMe = new File(activity.getCacheDir() +
                                SHOWCASE_CACHE + "showcase_tabs-temp.xml");
                        boolean deleted = deleteMe.delete();
                        if (!deleted) {
                            Log.e(TAG, "Unable to delete temporary tab file.");
                        }
                    }
                }

                resultant = "showcase_tabs.xml";

                Map<String, String> newArray =
                        ReadShowcaseTabsFile.read(activity.getCacheDir() +
                                SHOWCASE_CACHE + resultant);

                DrawerBuilder drawerBuilder = new DrawerBuilder();
                drawerBuilder.withActivity(activity);
                drawerBuilder.withRootView(R.id.rootView);
                drawerBuilder.withDisplayBelowStatusBar(false);
                drawerBuilder.withTranslucentStatusBar(false);
                drawerBuilder.withDrawerWidthDp(200);
                drawerBuilder.withDrawerLayout(R.layout.material_drawer_fits_not);
                drawerBuilder.withSavedInstance(activity.savedInstanceState);
                List<String> listOfTitles = new ArrayList<>();
                List<String> listOfLinks = new ArrayList<>(newArray.values());
                for (String tabName : newArray.keySet()) {
                    drawerBuilder.addDrawerItems(
                            new PrimaryDrawerItem().withName(tabName));
                    listOfTitles.add(tabName);
                }
                drawerBuilder.withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    switchFragment(
                            activity,
                            position,
                            listOfLinks.get(position),
                            listOfTitles.get(position));
                    return false;
                });
                drawerBuilder.withDrawerGravity(Gravity.END);
                activity.drawer = drawerBuilder.build();
                switchFragment(
                        activity,
                        0,
                        listOfLinks.get(0),
                        listOfTitles.get(0)
                );

                Handler handler = new Handler();
                handler.postDelayed(() -> activity.drawer.openDrawer(), 500);
            }
        }

        private void switchFragment(ShowcaseActivity activity,
                                    int position,
                                    String link_address,
                                    String title) {
            FragmentTransaction tx = activity.getSupportFragmentManager().beginTransaction();
            tx.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
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