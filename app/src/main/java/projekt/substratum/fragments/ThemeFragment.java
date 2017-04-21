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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.activities.showcase.ShowcaseActivity;
import projekt.substratum.adapters.fragments.themes.ThemeAdapter;
import projekt.substratum.adapters.fragments.themes.ThemeItem;
import projekt.substratum.common.References;

public class ThemeFragment extends Fragment {

    private HashMap<String, String[]> substratum_packages;
    private RecyclerView recyclerView;
    private Map<String, String[]> map;
    private Context mContext;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<ApplicationInfo> list;
    private ThemeAdapter adapter;
    private View cardView;
    private ViewGroup root;
    private String home_type = "", title = "";
    private SharedPreferences prefs;
    private TextView cardViewText;
    private ImageView cardViewImage;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver refreshReceiver;

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

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new ThemeInstallReceiver();
        IntentFilter intentFilter = new IntentFilter("ThemeFragment.REFRESH");
        localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        localBroadcastManager.registerReceiver(refreshReceiver, intentFilter);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (prefs.getBoolean("nougat_style_cards", false)) {
            root = (ViewGroup) inflater.inflate(R.layout.home_fragment_n, container, false);
        } else {
            root = (ViewGroup) inflater.inflate(R.layout.home_fragment, container, false);
        }

        mContext = getContext();

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            home_type = bundle.getString("home_type");
            title = bundle.getString("title");
        }

        // Initialize a proper loading sequence so the user does not see the unparsed string
        MainActivity.actionbar_content.setText(getString(R.string.actionbar_theme_count_loading));

        substratum_packages = new HashMap<>();
        recyclerView = (RecyclerView) root.findViewById(R.id.theme_list);
        cardView = root.findViewById(R.id.no_entry_card_view);
        cardView.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), ShowcaseActivity.class);
                    startActivity(intent);
                }
        );
        cardView.setVisibility(View.GONE);
        cardViewText = (TextView) cardView.findViewById(R.id.no_themes_description);
        cardViewImage = (ImageView) cardView.findViewById(R.id.no_themes_installed);

        // Create it so it uses a recyclerView to parse substratum-based themes
        PackageManager packageManager = mContext.getPackageManager();
        list = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshLayout);

        if (home_type.equals("")) {
            // We use this because our splash screen is meant to hang (to load) than show a wheel
            refreshLayout();
        } else {
            // This allows it to not hang the nav drawer activity
            LayoutLoader layoutLoader = new LayoutLoader();
            layoutLoader.execute("");
        }

        // Now we need to sort the buffered installed Layers themes
        map = new TreeMap<>(substratum_packages);

        ArrayList<ThemeItem> themeItems = prepareData();
        adapter = new ThemeAdapter(themeItems);

        // Assign adapter to RecyclerView
        recyclerView.setAdapter(adapter);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        return root;
    }

    private ArrayList<ThemeItem> prepareData() {

        ArrayList<ThemeItem> themes = new ArrayList<>();
        for (int i = 0; i < map.size(); i++) {
            ThemeItem themeItem = new ThemeItem();
            themeItem.setThemeName(map.keySet().toArray()[i].toString());
            themeItem.setThemeAuthor(map.get(map.keySet().toArray()[i].toString())[0]);
            themeItem.setThemePackage(map.get(map.keySet().toArray()[i].toString())[1]);
            themeItem.setThemeDrawable(
                    References.grabPackageHeroImage(
                            mContext, map.get(map.keySet().toArray()[i].toString())[1]));
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            if (prefs.getBoolean("show_theme_ready_indicators", true)) {
                themeItem.setThemeReadyVariable(References.grabThemeReadyVisibility(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
            }
            if (prefs.getBoolean("show_template_version", false)) {
                themeItem.setPluginVersion(References.grabPackageTemplateVersion(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
                themeItem.setSDKLevels(References.grabThemeAPIs(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
                themeItem.setThemeVersion(References.grabThemeVersion(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
            } else {
                themeItem.setPluginVersion(null);
                themeItem.setSDKLevels(null);
                themeItem.setThemeVersion(null);
            }
            if (home_type.length() == 0) {
                themeItem.setThemeMode(null);
            } else {
                themeItem.setThemeMode(home_type);
            }
            themeItem.setContext(mContext);
            themeItem.setActivity(getActivity());
            themes.add(themeItem);
        }
        return themes;
    }

    public void refreshLayout() {
        MaterialProgressBar materialProgressBar =
                (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);
        materialProgressBar.setVisibility(View.VISIBLE);
        PackageManager packageManager = mContext.getPackageManager();
        list.clear();
        substratum_packages = new HashMap<>();
        list = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        if (prefs.getBoolean("display_old_themes", true)) {
            list.stream().filter(packageInfo ->
                    (packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0).forEach(packageInfo ->
                    References.getSubstratumPackages(
                            mContext,
                            packageInfo.packageName,
                            substratum_packages,
                            home_type,
                            true,
                            MainActivity.userInput));
            Log.d(References.SUBSTRATUM_LOG,
                    "Substratum has loaded themes using the pre-499 theme database filter");
        } else {
            References.getSubstratumPackages(
                    mContext,
                    null,
                    substratum_packages,
                    home_type,
                    false,
                    MainActivity.userInput);
            Log.d(References.SUBSTRATUM_LOG,
                    "Substratum has loaded themes using the post-499 theme database filter");
        }

        if (substratum_packages.size() == 0) {
            if (MainActivity.searchView != null && !MainActivity.searchView.isIconified()) {
                if (MainActivity.userInput.length() > 0) {
                    String parse = String.format(
                            getString(R.string.no_themes_description_search),
                            MainActivity.userInput);
                    cardViewText.setText(parse);
                    cardViewImage.setImageDrawable(
                            getContext().getDrawable(R.drawable.no_themes_found));
                } else {
                    cardViewText.setText(getString(R.string.no_themes_description));
                    cardViewImage.setImageDrawable(
                            getContext().getDrawable(R.drawable.no_themes_installed));
                }
            } else {
                cardViewText.setText(getString(R.string.no_themes_description));
                cardViewImage.setImageDrawable(
                        getContext().getDrawable(R.drawable.no_themes_installed));
            }
            cardView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            cardView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        // Now let's place the proper amount of theme count into the context text
        String parse;
        if (substratum_packages.size() == 0) {
            MainActivity.switchToStockToolbar(title);
        } else if (substratum_packages.size() == 1) {
            parse = String.format(getString(R.string.actionbar_theme_count_singular),
                    String.valueOf(substratum_packages.size()));
            MainActivity.switchToCustomToolbar(title, parse);
        } else {
            parse = String.format(getString(R.string.actionbar_theme_count_plural),
                    String.valueOf(substratum_packages.size()));
            MainActivity.switchToCustomToolbar(title, parse);
        }

        // Now we need to sort the buffered installed themes
        map = new TreeMap<>(substratum_packages);
        if (adapter == null) {
            adapter = new ThemeAdapter(prepareData());
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateInformation(prepareData());
            adapter.notifyDataSetChanged();
        }
        swipeRefreshLayout.setRefreshing(false);
        materialProgressBar.setVisibility(View.GONE);
    }

    private class LayoutLoader extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            if (isAdded()) refreshLayout();
            if (substratum_packages.size() == 0) {
                if (MainActivity.searchView != null && !MainActivity.searchView.isIconified()) {
                    if (MainActivity.userInput.length() > 0) {
                        String parse = String.format(
                                getString(R.string.no_themes_description_search),
                                MainActivity.userInput);
                        cardViewText.setText(parse);
                        cardViewImage.setImageDrawable(
                                getContext().getDrawable(R.drawable.no_themes_found));
                    } else {
                        cardViewText.setText(getString(R.string.no_themes_description));
                        cardViewImage.setImageDrawable(
                                getContext().getDrawable(R.drawable.no_themes_installed));
                    }
                } else {
                    cardViewText.setText(getString(R.string.no_themes_description));
                    cardViewImage.setImageDrawable(
                            getContext().getDrawable(R.drawable.no_themes_installed));
                }
                cardView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                cardView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            try {
                if (prefs.getBoolean("display_old_themes", true)) {
                    list.stream().filter(packageInfo ->
                            (packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0).forEach(
                            packageInfo ->
                                    References.getSubstratumPackages(
                                            mContext,
                                            packageInfo.packageName,
                                            substratum_packages,
                                            home_type,
                                            true,
                                            MainActivity.userInput));
                    Log.d(References.SUBSTRATUM_LOG,
                            "Substratum has loaded themes using the pre-499 theme database filter");
                } else {
                    References.getSubstratumPackages(
                            mContext,
                            null,
                            substratum_packages,
                            home_type,
                            false,
                            MainActivity.userInput);
                    Log.d(References.SUBSTRATUM_LOG,
                            "Substratum has loaded themes using the " +
                                    "post-499 theme database filter");
                }
            } catch (Exception e) {
                // Exception
            }
            return null;
        }
    }

    class ThemeInstallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshLayout();
        }
    }
}