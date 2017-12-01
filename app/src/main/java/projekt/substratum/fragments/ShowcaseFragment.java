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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.adapters.showcase.ShowcaseTabsAdapter;
import projekt.substratum.common.References;
import projekt.substratum.util.files.FileDownloader;
import projekt.substratum.util.readers.ReadShowcaseTabsFile;

import static projekt.substratum.common.Internal.SHOWCASE_CACHE;

public class ShowcaseFragment extends Fragment {

    private static final String TAG = "ShowcaseFragment";
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.viewpager)
    ViewPager viewPager;
    @BindView(R.id.no_network)
    RelativeLayout no_network;
    private Context mContext;

    /**
     * Refresh the showcase layout by redownloading the tabs
     */
    private void refreshLayout() {
        if (References.isNetworkAvailable(mContext)) {
            no_network.setVisibility(View.GONE);
            tabLayout.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);

            DownloadTabs downloadTabs = new DownloadTabs(this);
            downloadTabs.execute(getString(R.string.showcase_tabs), "showcase_tabs.xml");
        } else {
            no_network.setVisibility(View.VISIBLE);
            tabLayout.setVisibility(View.GONE);
            viewPager.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.showcase_fragment, container, false);
        ButterKnife.bind(this, view);
        mContext = Substratum.getInstance();

        File showcase_directory = new File(mContext.getCacheDir() + SHOWCASE_CACHE);
        if (!showcase_directory.exists()) {
            Boolean made = showcase_directory.mkdir();
            if (!made) Log.e(TAG, "Could not make showcase directory...");
        }

        tabLayout.setTabTextColors(
                mContext.getColor(R.color.showcase_activity_text),
                mContext.getColor(R.color.showcase_activity_text));
        tabLayout.setVisibility(View.GONE);
        refreshLayout();
        return view;
    }

    /**
     * Class to download the tabs from the GitHub organization
     */
    private static class DownloadTabs extends AsyncTask<String, Integer, String> {

        private WeakReference<ShowcaseFragment> showcaseFragment;

        DownloadTabs(ShowcaseFragment activity) {
            showcaseFragment = new WeakReference<>(activity);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result == null) {
                return;
            }
            ShowcaseFragment showcaseFragment = this.showcaseFragment.get();
            if (showcaseFragment != null) {
                showcaseFragment.tabLayout.setVisibility(View.VISIBLE);
                Map<String, String> newArray =
                        ReadShowcaseTabsFile.read(
                                showcaseFragment.mContext.getCacheDir() +
                                        SHOWCASE_CACHE + "showcase_tabs.xml");

                ArrayList<String> links = new ArrayList<>();
                newArray.keySet()
                        .forEach(key -> {
                            links.add(newArray.get(key));
                            showcaseFragment.tabLayout.addTab(
                                    showcaseFragment.tabLayout.newTab().setText(key));
                        });

                ShowcaseTabsAdapter adapter = new ShowcaseTabsAdapter(
                        showcaseFragment.getFragmentManager(),
                        showcaseFragment.tabLayout.getTabCount(),
                        links);

                showcaseFragment.viewPager.setOffscreenPageLimit(
                        showcaseFragment.tabLayout.getTabCount());
                showcaseFragment.viewPager.setAdapter(adapter);
                showcaseFragment.viewPager.addOnPageChangeListener(
                        new TabLayout.TabLayoutOnPageChangeListener(showcaseFragment.tabLayout));
                showcaseFragment.tabLayout.addOnTabSelectedListener(
                        new TabLayout.OnTabSelectedListener() {
                            @Override
                            public void onTabSelected(TabLayout.Tab tab) {
                                showcaseFragment.viewPager.setCurrentItem(tab.getPosition());
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
            ShowcaseFragment showcaseFragment = this.showcaseFragment.get();
            if (showcaseFragment != null) {
                FileDownloader.init(
                        showcaseFragment.mContext,
                        sUrl[0],
                        inputFileName,
                        "ShowcaseCache"
                );
                return inputFileName;
            } else {
                return null;
            }
        }
    }
}