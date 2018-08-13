/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.adapters.fragments.themes.ThemeAdapter;
import projekt.substratum.adapters.fragments.themes.ThemeItem;
import projekt.substratum.common.Packages;
import projekt.substratum.common.Systems;
import projekt.substratum.databinding.ThemeFragmentBinding;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static projekt.substratum.common.Internal.THEME_FRAGMENT_REFRESH;
import static projekt.substratum.common.References.DEFAULT_GRID_COUNT;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.Systems.isSamsungDevice;

public class ThemeFragment extends Fragment {

    private static final int THEME_FRAGMENT_INITIAL_DELAY = 300;
    public static AsyncTask layoutReloader;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View cardView;
    private Context context;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver refreshReceiver;
    private boolean firstBoot = true;
    private ThemeAdapter themeAdapter;

    /**
     * Prepares the data to be used by the cards
     *
     * @param map     Map of packages that have been processed
     * @param context Self explantory, bud
     * @return Returns an ArrayList to be used to parse further data
     */
    private static ArrayList<ThemeItem> prepareData(Map<String, String[]> map,
                                                    Context context) {
        ArrayList<ThemeItem> themes = new ArrayList<>();
        for (int i = 0; i < map.size(); i++) {
            ThemeItem themeItem = new ThemeItem();
            themeItem.setThemeName(map.keySet().toArray()[i].toString());
            themeItem.setThemeAuthor(map.get(map.keySet().toArray()[i].toString())[0]);
            themeItem.setThemePackage(map.get(map.keySet().toArray()[i].toString())[1]);
            try {
                themeItem.setThemeDrawable(
                        Packages.getPackageHeroImage(
                                context, map.get(map.keySet().toArray()[i].toString())[1], true));
            } catch (Exception ignored) {
            }
            themeItem.setContext(context);
            themes.add(themeItem);
        }
        return themes;
    }

    /**
     * Deeply refresh the themes
     *
     * @param themeFragment      Theme Fragment
     * @param context            Self explanatory, bud
     * @param activity           Activity of calling function
     * @param substratumPackages List of collected substratum packages
     * @param themeItems         List of collected themes
     */
    private static void refreshLayout(ThemeFragment themeFragment,
                                      Context context,
                                      Activity activity,
                                      Map<String, String[]> substratumPackages,
                                      ArrayList<ThemeItem> themeItems) {
        SharedPreferences prefs = Substratum.getPreferences();

        TextView cardViewText = themeFragment.cardView.findViewById(R.id.no_themes_description);
        ImageView cardViewImage = themeFragment.cardView.findViewById(R.id.no_themes_installed);

        if (substratumPackages != null) {
            if (substratumPackages.isEmpty()) {
                if ((((MainActivity) activity).searchView != null) &&
                        !((MainActivity) activity).searchView.isIconified()) {
                    if (!MainActivity.userInput.isEmpty()) {
                        String parse = String.format(
                                context.getString(R.string.no_themes_description_search),
                                MainActivity.userInput);
                        cardViewText.setText(parse);
                        cardViewImage.setImageDrawable(
                                context.getDrawable(R.drawable.no_themes_found));
                    } else {
                        cardViewText.setText(context.getString(R.string.no_themes_description));
                        cardViewImage.setImageDrawable(
                                context.getDrawable(R.drawable.no_themes_installed));
                    }
                } else {
                    cardViewText.setText(context.getString(R.string.no_themes_description));
                    cardViewImage.setImageDrawable(
                            context.getDrawable(R.drawable.no_themes_installed));
                }
                themeFragment.cardView.setVisibility(View.VISIBLE);
                themeFragment.recyclerView.setVisibility(View.GONE);
            } else {
                themeFragment.cardView.setVisibility(View.GONE);
                themeFragment.recyclerView.setVisibility(View.VISIBLE);
            }

            // Now we need to sort the buffered installed themes
            themeFragment.themeAdapter = new ThemeAdapter(themeItems);

            // Assign adapter to RecyclerView
            themeFragment.recyclerView.setAdapter(themeFragment.themeAdapter);
            // Now let's animate the objects one by one!
            themeFragment.recyclerView.getViewTreeObserver().addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            themeFragment.recyclerView
                                    .getViewTreeObserver().removeOnPreDrawListener(this);
                            boolean slowDevice = Systems.isSamsungDevice(themeFragment.context);
                            for (int i = 0; i < themeFragment.recyclerView.getChildCount(); i++) {
                                View v = themeFragment.recyclerView.getChildAt(i);
                                v.setAlpha(0.0f);
                                v.animate().alpha(1.0f)
                                        .setDuration(300)
                                        .setStartDelay(slowDevice ? i * 50 : i * 30)
                                        .start();
                            }
                            return true;
                        }
                    });

            // Begin to set the formatting of the layouts
            if (prefs.getInt("grid_style_cards_count", DEFAULT_GRID_COUNT) > 1) {
                themeFragment.recyclerView.setPadding(10, 0, 10, 0);
            }
            if (prefs.getBoolean("grid_layout", true)) {
                themeFragment.recyclerView.setLayoutManager(new GridLayoutManager(context,
                        prefs.getInt("grid_style_cards_count", DEFAULT_GRID_COUNT)));
            } else {
                RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
                themeFragment.recyclerView.setLayoutManager(layoutManager);
            }
        } else {
            Log.e(SUBSTRATUM_LOG, "Queuing of the installed themes have resulted in a null list.");
        }

        // Conclude
        themeFragment.swipeRefreshLayout.setRefreshing(false);
        themeFragment.swipeRefreshLayout.setEnabled(false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        layoutReloader = new LayoutLoader(this).execute();
    }

    /**
     * Reset all parameters of the recycler view
     *
     * @param recyclerView Specify the recycler view to reset
     */
    private void resetRecyclerView(RecyclerView recyclerView) {
        // Initialize the recycler view with an empty adapter first
        ArrayList<ThemeItem> emptyArray = new ArrayList<>();
        ThemeAdapter emptyAdapter = new ThemeAdapter(emptyArray);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(emptyAdapter);
    }

    /**
     * Scroll up the RecyclerView smoothly
     */
    public void scrollUp() {
        recyclerView.smoothScrollToPosition(0);
    }

    /**
     * Obtain an instance of this fragment
     *
     * @return Returns an instance of Theme Fragment
     */
    private ThemeFragment getInstance() {
        return this;
    }

    /**
     * Creating the options menu (3dot overflow menu)
     *
     * @param menu     Menu object
     * @param inflater The inflated menu object
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.themes_list_menu, menu);
        boolean isOMS = Systems.checkOMS(context);
        if (isOMS || isSamsungDevice(context)) {
            menu.findItem(R.id.reboot_device).setVisible(false);
            menu.findItem(R.id.soft_reboot).setVisible(false);
        }
        if (!isOMS) menu.findItem(R.id.per_app).setVisible(false);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.searchView = (SearchView) searchItem.getActionView();
            mainActivity.searchView.setOnQueryTextListener(mainActivity);
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem menuItem) {
                    mainActivity.searchView.setIconified(false);
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                    if (!MainActivity.userInput.equals("")) {
                        MainActivity.userInput = "";
                        layoutReloader = new LayoutLoader(getInstance()).execute();
                    }
                    return true;
                }
            });
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            localBroadcastManager.unregisterReceiver(refreshReceiver);
        } catch (IllegalArgumentException ignored) {
            // Unregistered already
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        context = getContext();
        ThemeFragmentBinding viewBinding =
                DataBindingUtil.inflate(inflater, R.layout.theme_fragment, container, false);
        View view = viewBinding.getRoot();
        recyclerView = viewBinding.themeList;
        swipeRefreshLayout = viewBinding.swipeRefreshLayout;
        cardView = viewBinding.noEntryCardView;

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new ThemeInstallReceiver();
        IntentFilter intentFilter = new IntentFilter(THEME_FRAGMENT_REFRESH);
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(refreshReceiver, intentFilter);

        resetRecyclerView(recyclerView);
        swipeRefreshLayout.setOnRefreshListener(() ->
                layoutReloader = new LayoutLoader(this).execute());
        swipeRefreshLayout.setRefreshing(true);

        if (getActivity() != null) {
            ((MainActivity) getActivity()).actionbarContent.setText(R.string
                    .actionbar_theme_count_loading);
        }
        cardView.setVisibility(View.GONE);

        // Let's start loading everything
        layoutReloader = new LayoutLoader(this).execute();

        return view;
    }

    /**
     * The core structure to reload the layout asynchronously
     */
    private static class LayoutLoader extends AsyncTask<String, Integer, String> {
        private final WeakReference<ThemeFragment> fragment;
        private HashMap<String, String[]> substratumPackages;
        private ArrayList<ThemeItem> themeItems;

        LayoutLoader(ThemeFragment themeFragment) {
            super();
            fragment = new WeakReference<>(themeFragment);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ThemeFragment themeFragment = fragment.get();
            if (themeFragment != null) {
                refreshLayout(
                        themeFragment,
                        themeFragment.context,
                        themeFragment.getActivity(),
                        this.substratumPackages,
                        this.themeItems);
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            ThemeFragment themeFragment = fragment.get();
            if (themeFragment != null) {
                substratumPackages = Packages.getSubstratumPackages(
                        themeFragment.context,
                        MainActivity.userInput);
                Map<String, String[]> map = new TreeMap<>(substratumPackages);
                themeItems = prepareData(
                        map,
                        themeFragment.context
                );
                try {
                    Thread.sleep((long)
                            (themeFragment.firstBoot ? THEME_FRAGMENT_INITIAL_DELAY : 0));
                } catch (InterruptedException ignored) {
                }
                if (themeFragment.firstBoot) themeFragment.firstBoot = false;
            }
            return null;
        }
    }

    /**
     * Receiver to reload the list when a new theme is installed
     */
    class ThemeInstallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            layoutReloader = new LayoutLoader(ThemeFragment.this.getInstance()).execute();
        }
    }
}