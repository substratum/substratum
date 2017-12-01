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
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

import projekt.substratum.fragments.ShowcaseTab;

public class ShowcaseTabsAdapter extends FragmentStatePagerAdapter {
    private List fragment_links;
    private Integer mNumOfTabs;

    public ShowcaseTabsAdapter(FragmentManager fm,
                               int mNumOfTabs,
                               List fragment_links) {
        super(fm);
        this.mNumOfTabs = mNumOfTabs;
        this.fragment_links = fragment_links;
    }

    @Override
    public Fragment getItem(int position) {
        Bundle bundle = new Bundle();
        bundle.putInt("tab_count", position);
        bundle.putString("tabbed_address", this.fragment_links.get(position).toString());
        ShowcaseTab showcaseTab = new ShowcaseTab();
        showcaseTab.setArguments(bundle);
        return showcaseTab;
    }

    @Override
    public int getCount() {
        return this.mNumOfTabs;
    }
}