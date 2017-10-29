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

package projekt.substratum.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.activities.showcase.ShowcaseActivity;
import projekt.substratum.adapters.fragments.themes.ThemeAdapter;
import projekt.substratum.adapters.fragments.themes.ThemeItem;
import projekt.substratum.common.Activities;
import projekt.substratum.common.Packages;
import projekt.substratum.util.helpers.ThemeCallback;

import static projekt.substratum.common.Internal.HOME_TITLE;
import static projekt.substratum.common.Internal.HOME_TYPE;
import static projekt.substratum.common.Internal.THEME_FRAGMENT_REFRESH;
import static projekt.substratum.common.References.DEFAULT_GRID_COUNT;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;

public class ThemeFragment extends Fragment {

    private static final int THEME_FRAGMENT_INITIAL_DELAY = 300;
    @BindView(R.id.theme_list)
    RecyclerView recyclerView;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.no_entry_card_view)
    View cardView;
    private Context mContext;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver refreshReceiver;
    private SharedPreferences prefs;
    private String home_type;
    private String toolbar_title;
    private Boolean first_boot = true;
    private ThemeAdapter mAdapter;

    /**
     * Prepares the data to be used by the cards
     *
     * @param map       Map of packages that have been processed
     * @param context   Self explantory, bud
     * @param activity  Activity of the calling function
     * @param home_type Home type such as Theme packs, Overlays, Boot Animations, Sounds, Fonts
     * @return Returns an ArrayList to be used to parse further data
     */
    private static ArrayList<ThemeItem> prepareData(Map<String, String[]> map,
                                                    Context context,
                                                    Activity activity,
                                                    String home_type) {
        ArrayList<ThemeItem> themes = new ArrayList<>();
        for (int i = 0; i < map.size(); i++) {
            ThemeItem themeItem = new ThemeItem();
            themeItem.setThemeName(map.keySet().toArray()[i].toString());
            themeItem.setThemeAuthor(map.get(map.keySet().toArray()[i].toString())[0]);
            themeItem.setThemePackage(map.get(map.keySet().toArray()[i].toString())[1]);
            themeItem.setThemeDrawable(
                    Packages.getPackageHeroImage(
                            context, map.get(map.keySet().toArray()[i].toString())[1], true));
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            themeItem.setThemeReadyVariable(Packages.getThemeReadyVisibility(context,
                    map.get(map.keySet().toArray()[i].toString())[1]));
            if (prefs.getBoolean("show_template_version", false) &&
                    !prefs.getBoolean("grid_layout", true)) {
                themeItem.setPluginVersion(Packages.getPackageTemplateVersion(context,
                        map.get(map.keySet().toArray()[i].toString())[1]));
                themeItem.setSDKLevels(Packages.getThemeAPIs(context,
                        map.get(map.keySet().toArray()[i].toString())[1]));
                themeItem.setThemeVersion(Packages.getThemeVersion(context,
                        map.get(map.keySet().toArray()[i].toString())[1]));
            } else {
                themeItem.setPluginVersion(null);
                themeItem.setSDKLevels(null);
                themeItem.setThemeVersion(null);
            }
            themeItem.setThemeMode((home_type.isEmpty()) ? null : home_type);
            themeItem.setContext(context);
            themeItem.setActivity(activity);
            themes.add(themeItem);
        }
        return themes;
    }

    /**
     * Lightly refresh the themes
     *
     * @param themeFragment Theme Fragment
     * @param themeItems    List of collected themes
     */
    private static void refreshLayout(ThemeFragment themeFragment,
                                      ArrayList<ThemeItem> themeItems) {
        ThemeAdapter adapter = themeFragment.mAdapter;
        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new ThemeCallback(adapter.getList(), themeItems));
        adapter.setList(themeItems);
        diffResult.dispatchUpdatesTo(adapter);
        themeFragment.swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * Deeply refresh the themes
     *
     * @param themeFragment       Theme Fragment
     * @param prefs               Shared Preferences instance
     * @param mContext            Self explanatory, bud
     * @param activity            Activity of calling function
     * @param toolbarTitle        Requested toolbar title
     * @param substratum_packages List of collected substratum packages
     * @param themeItems          List of collected themes
     */
    private static void refreshLayout(ThemeFragment themeFragment,
                                      SharedPreferences prefs,
                                      Context mContext,
                                      Activity activity,
                                      CharSequence toolbarTitle,
                                      Map<String, String[]> substratum_packages,
                                      ArrayList<ThemeItem> themeItems) {
        TextView cardViewText = themeFragment.cardView.findViewById(R.id.no_themes_description);
        ImageView cardViewImage = themeFragment.cardView.findViewById(R.id.no_themes_installed);
        if (substratum_packages != null) {
            if (substratum_packages.isEmpty()) {
                if ((((MainActivity) activity).searchView != null) &&
                        !((MainActivity) activity).searchView.isIconified()) {
                    if (!MainActivity.userInput.isEmpty()) {
                        String parse = String.format(
                                mContext.getString(R.string.no_themes_description_search),
                                MainActivity.userInput);
                        cardViewText.setText(parse);
                        cardViewImage.setImageDrawable(
                                mContext.getDrawable(R.drawable.no_themes_found));
                    } else {
                        cardViewText.setText(mContext.getString(R.string.no_themes_description));
                        cardViewImage.setImageDrawable(
                                mContext.getDrawable(R.drawable.no_themes_installed));
                    }
                } else {
                    cardViewText.setText(mContext.getString(R.string.no_themes_description));
                    cardViewImage.setImageDrawable(
                            mContext.getDrawable(R.drawable.no_themes_installed));
                }
                themeFragment.cardView.setVisibility(View.VISIBLE);
                themeFragment.recyclerView.setVisibility(View.GONE);
            } else {
                themeFragment.cardView.setVisibility(View.GONE);
                themeFragment.recyclerView.setVisibility(View.VISIBLE);
            }

            // Now let's place the proper amount of theme count into the context text
            String parse;
            WeakReference<MainActivity> ref = new WeakReference<>((MainActivity) activity);
            if (substratum_packages.isEmpty() && (ref.get() != null)) {
                ref.get().switchToStockToolbar(toolbarTitle);
            } else if ((substratum_packages.size() == 1) && (ref.get() != null)) {
                parse = String.format(mContext.getString(R.string.actionbar_theme_count_singular),
                        String.valueOf(substratum_packages.size()));
                ref.get().switchToCustomToolbar(toolbarTitle, parse);
            } else if (ref.get() != null) {
                parse = String.format(mContext.getString(R.string.actionbar_theme_count_plural),
                        String.valueOf(substratum_packages.size()));
                ref.get().switchToCustomToolbar(toolbarTitle, parse);
            }

            // Now we need to sort the buffered installed themes
            themeFragment.mAdapter = new ThemeAdapter(themeItems);

            // Assign adapter to RecyclerView
            themeFragment.recyclerView.setAdapter(themeFragment.mAdapter);

            // Begin to set the formatting of the layouts
            if (prefs.getInt("grid_style_cards_count", DEFAULT_GRID_COUNT) > 1) {
                if (!prefs.getBoolean("nougat_style_cards", false)) {
                    themeFragment.recyclerView.setPadding(10, 0, 10, 0);
                } else if (prefs.getBoolean("nougat_style_cards", false) &&
                        (prefs.getInt("grid_style_cards_count", 1) == 1)) {
                    themeFragment.recyclerView.setPadding(0, 0, 0, 0);
                } else {
                    themeFragment.recyclerView.setPadding(0, 0, 0, 0);
                }
            } else if (prefs.getBoolean("nougat_style_cards", false)) {
                themeFragment.recyclerView.setPadding(0, 0, 0, 0);
            }
            if (prefs.getBoolean("grid_layout", true)) {
                themeFragment.recyclerView.setLayoutManager(new GridLayoutManager(mContext,
                        prefs.getInt("grid_style_cards_count", DEFAULT_GRID_COUNT)));
            } else {
                RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(mContext);
                themeFragment.recyclerView.setLayoutManager(layoutManager);
            }
        } else {
            Log.e(SUBSTRATUM_LOG, "Queuing of the installed themes have resulted in a null list.");
        }

        // Conclude
        themeFragment.swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * Reset all parameters of the recycler view
     *
     * @param mRecyclerView Specify the recycler view to reset
     */
    private void resetRecyclerView(RecyclerView mRecyclerView) {
        // Initialize the recycler view with an empty adapter first
        ArrayList<ThemeItem> empty_array = new ArrayList<>();
        ThemeAdapter empty_adapter = new ThemeAdapter(empty_array);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.setAdapter(empty_adapter);
    }

    /**
     * Obtain an instance of this fragment
     *
     * @return Returns an instance of Theme Fragment
     */
    private ThemeFragment getInstance() {
        return this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            localBroadcastManager.unregisterReceiver(refreshReceiver);
        } catch (IllegalArgumentException e) {
            // Unregistered already
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        View view = inflater.inflate(R.layout.home_fragment, container, false);
        ButterKnife.bind(this, view);

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new ThemeInstallReceiver();
        IntentFilter intentFilter = new IntentFilter(THEME_FRAGMENT_REFRESH);
        localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.registerReceiver(refreshReceiver, intentFilter);

        Bundle bundle = getArguments();
        if (bundle != null) {
            home_type = bundle.getString(HOME_TYPE);
            toolbar_title = bundle.getString(HOME_TITLE);
        }

        resetRecyclerView(recyclerView);
        swipeRefreshLayout.setOnRefreshListener(() -> new LayoutLoader(this).execute());
        swipeRefreshLayout.setRefreshing(true);

        if (getActivity() != null) {
            ((MainActivity) getActivity()).actionbar_content.setText(R.string
                    .actionbar_theme_count_loading);
        }

        cardView.setOnClickListener(v ->
                Activities.launchInternalActivity(mContext, ShowcaseActivity.class));
        cardView.setVisibility(View.GONE);

        // Let's start loading everything
        new LayoutLoader(this).execute();

        return view;
    }

    /**
     * The core structure to reload the layout asynchronously
     */
    private static class LayoutLoader extends AsyncTask<String, Integer, String> {
        private WeakReference<ThemeFragment> fragment;
        private HashMap<String, String[]> substratum_packages;
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
                if (themeFragment.getActivity() != null) {
                    boolean iconified =
                            ((MainActivity) themeFragment.getActivity()).searchView.isIconified();
                    if (themeFragment.mAdapter != null && iconified) {
                        refreshLayout(
                                themeFragment,
                                themeItems);
                    } else {
                        refreshLayout(
                                themeFragment,
                                themeFragment.prefs,
                                themeFragment.mContext,
                                themeFragment.getActivity(),
                                themeFragment.toolbar_title,
                                substratum_packages,
                                themeItems);
                    }
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            ThemeFragment themeFragment = fragment.get();
            if (themeFragment != null) {
                substratum_packages = Packages.getSubstratumPackages(
                        themeFragment.mContext,
                        themeFragment.home_type,
                        MainActivity.userInput);
                Map<String, String[]> map = new TreeMap<>(substratum_packages);
                themeItems = prepareData(
                        map,
                        themeFragment.mContext,
                        themeFragment.getActivity(),
                        themeFragment.home_type);
                try {
                    Thread.sleep((long)
                            (themeFragment.first_boot ? THEME_FRAGMENT_INITIAL_DELAY : 0));
                } catch (InterruptedException ie) {
                    // Suppress warning
                }
                if (themeFragment.first_boot) themeFragment.first_boot = false;
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
            new LayoutLoader(ThemeFragment.this.getInstance()).execute();
        }
    }
}