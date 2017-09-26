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

package projekt.substratum.adapters.showcase;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;

import projekt.substratum.fragments.ShowcaseTab;

public class ShowcaseTabsAdapter extends FragmentPagerAdapter {
    private ArrayList fragment_links;
    private Integer mNumOfTabs;

    public ShowcaseTabsAdapter(FragmentManager fm, int mNumOfTabs, ArrayList fragment_links) {
        super(fm);
        this.mNumOfTabs = mNumOfTabs;
        this.fragment_links = fragment_links;
    }

    @Override
    public Fragment getItem(int position) {
        Bundle bundle = new Bundle();
        bundle.putInt("tab_count", position);
        bundle.putString("tabbed_address", fragment_links.get(position).toString());
        Fragment fragment = new ShowcaseTab();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}