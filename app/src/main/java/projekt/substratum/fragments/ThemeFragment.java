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
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
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

import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.activities.showcase.ShowcaseActivity;
import projekt.substratum.adapters.fragments.themes.ThemeAdapter;
import projekt.substratum.adapters.fragments.themes.ThemeItem;
import projekt.substratum.common.Activities;
import projekt.substratum.common.Packages;

import static projekt.substratum.common.References.DEFAULT_GRID_COUNT;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;

public class ThemeFragment extends Fragment {

    private static final int THEME_FRAGMENT_INITIAL_DELAY = 300;
    private Context mContext;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver refreshReceiver;
    private SharedPreferences prefs;
    private ViewGroup root;
    private String home_type;
    private String toolbar_title;
    private Boolean first_boot = true;

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
            themeItem.setThemeMode((home_type.length() == 0) ? null : home_type);
            themeItem.setContext(context);
            themeItem.setActivity(activity);
            themes.add(themeItem);
        }
        return themes;
    }

    public static void refreshLayout(SharedPreferences prefs,
                                     ViewGroup root,
                                     Context mContext,
                                     Activity activity,
                                     String toolbarTitle,
                                     Map<String, String[]> substratum_packages,
                                     ArrayList<ThemeItem> themeItems) {

        RecyclerView recyclerView = root.findViewById(R.id.theme_list);
        SwipeRefreshLayout swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        CardView cardView = root.findViewById(R.id.no_entry_card_view);
        TextView cardViewText = cardView.findViewById(R.id.no_themes_description);
        ImageView cardViewImage = cardView.findViewById(R.id.no_themes_installed);

        if (substratum_packages != null) {
            if (substratum_packages.size() == 0) {
                if (((MainActivity) activity).searchView != null &&
                        !((MainActivity) activity).searchView.isIconified()) {
                    if (MainActivity.userInput.length() > 0) {
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
                cardView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                cardView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }

            // Now let's place the proper amount of theme count into the context text
            String parse;
            WeakReference<MainActivity> ref = new WeakReference<>((MainActivity) activity);
            if (substratum_packages.size() == 0 && ref.get() != null) {
                ref.get().switchToStockToolbar(toolbarTitle);
            } else if (substratum_packages.size() == 1 && ref.get() != null) {
                parse = String.format(mContext.getString(R.string.actionbar_theme_count_singular),
                        String.valueOf(substratum_packages.size()));
                ref.get().switchToCustomToolbar(toolbarTitle, parse);
            } else if (ref.get() != null) {
                parse = String.format(mContext.getString(R.string.actionbar_theme_count_plural),
                        String.valueOf(substratum_packages.size()));
                ref.get().switchToCustomToolbar(toolbarTitle, parse);
            }

            // Now we need to sort the buffered installed themes
            ThemeAdapter adapter = new ThemeAdapter(themeItems);

            // Assign adapter to RecyclerView
            recyclerView.setAdapter(adapter);

            // Begin to set the formatting of the layouts
            if (prefs.getInt("grid_style_cards_count", DEFAULT_GRID_COUNT) > 1) {
                if (!prefs.getBoolean("nougat_style_cards", false)) {
                    recyclerView.setPadding(10, 0, 10, 0);
                } else if (prefs.getBoolean("nougat_style_cards", false) &&
                        prefs.getInt("grid_style_cards_count", 1) == 1) {
                    recyclerView.setPadding(0, 0, 0, 0);
                } else {
                    recyclerView.setPadding(0, 0, 0, 0);
                }
            } else if (prefs.getBoolean("nougat_style_cards", false)) {
                recyclerView.setPadding(0, 0, 0, 0);
            }
            if (prefs.getBoolean("grid_layout", true)) {
                recyclerView.setLayoutManager(new GridLayoutManager(mContext,
                        prefs.getInt("grid_style_cards_count", DEFAULT_GRID_COUNT)));
            } else {
                RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(mContext);
                recyclerView.setLayoutManager(layoutManager);
            }
        } else {
            Log.e(SUBSTRATUM_LOG, "Queuing of the installed themes have resulted in a null list.");
        }

        // Conclude
        swipeRefreshLayout.setRefreshing(false);
    }

    private void resetRecyclerView(RecyclerView mRecyclerView) {
        // Initialize the recycler view with an empty adapter first
        ArrayList<ThemeItem> empty_array = new ArrayList<>();
        ThemeAdapter empty_adapter = new ThemeAdapter(empty_array);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.setAdapter(empty_adapter);
    }

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
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        root = (ViewGroup) inflater.inflate(R.layout.home_fragment, container, false);

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new ThemeInstallReceiver();
        IntentFilter intentFilter = new IntentFilter("ThemeFragment.REFRESH");
        localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.registerReceiver(refreshReceiver, intentFilter);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            home_type = bundle.getString("home_type");
            toolbar_title = bundle.getString("title");
        }

        RecyclerView recyclerView = root.findViewById(R.id.theme_list);
        resetRecyclerView(recyclerView);

        SwipeRefreshLayout swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> new LayoutLoader(this).execute());
        swipeRefreshLayout.setRefreshing(true);

        ((MainActivity)
                getActivity()).actionbar_content.setText(R.string.actionbar_theme_count_loading);

        View cardView = root.findViewById(R.id.no_entry_card_view);
        cardView.setOnClickListener(v ->
                Activities.launchInternalActivity(mContext, ShowcaseActivity.class));
        cardView.setVisibility(View.GONE);

        // Let's start loading everything
        new LayoutLoader(this).execute();

        return root;
    }

    private static class LayoutLoader extends AsyncTask<String, Integer, String> {
        private WeakReference<ThemeFragment> fragment;
        private HashMap<String, String[]> substratum_packages;
        private ArrayList<ThemeItem> themeItems;

        LayoutLoader(ThemeFragment themeFragment) {
            this.fragment = new WeakReference<>(themeFragment);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ThemeFragment themeFragment = fragment.get();
            if (themeFragment != null) {
                refreshLayout(
                        themeFragment.prefs,
                        themeFragment.root,
                        themeFragment.mContext,
                        themeFragment.getActivity(),
                        themeFragment.toolbar_title,
                        substratum_packages,
                        themeItems);
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            ThemeFragment themeFragment = fragment.get();
            if (themeFragment != null) {
                this.substratum_packages = Packages.getSubstratumPackages(
                        themeFragment.mContext,
                        themeFragment.home_type,
                        MainActivity.userInput);
                Map<String, String[]> map = new TreeMap<>(this.substratum_packages);
                this.themeItems = prepareData(
                        map,
                        themeFragment.mContext,
                        themeFragment.getActivity(),
                        themeFragment.home_type);
                try {
                    Thread.sleep(themeFragment.first_boot ? THEME_FRAGMENT_INITIAL_DELAY : 0);
                } catch (InterruptedException ie) {
                    // Suppress warning
                }
                if (themeFragment.first_boot) themeFragment.first_boot = false;
            }
            return null;
        }
    }

    class ThemeInstallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            new LayoutLoader(getInstance()).execute();
        }
    }
}
