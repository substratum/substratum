package projekt.substratum.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

import projekt.substratum.fragments.ShowcaseTab;

public class ShowcaseTabsAdapter extends FragmentStatePagerAdapter {
    private ArrayList fragment_links;
    private Integer mNumOfTabs;

    public ShowcaseTabsAdapter(FragmentManager fm, int mNumOfTabs, ArrayList fragment_links) {
        super(fm);
        this.mNumOfTabs = mNumOfTabs;
        this.fragment_links = fragment_links;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = new ShowcaseTab();
        Bundle bundle = new Bundle();
        bundle.putInt("tab_count", position);
        bundle.putString("tabbed_address", fragment_links.get(position).toString());
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}