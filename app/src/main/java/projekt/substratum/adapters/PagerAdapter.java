package projekt.substratum.adapters;

/**
 * Created by Nicholas on 2016-06-17.
 */

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import projekt.substratum.tabs.BootAnimation;
import projekt.substratum.tabs.MainScreenTab;
import projekt.substratum.tabs.OverlaysList;

public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                MainScreenTab tab1 = new MainScreenTab();
                return tab1;
            case 1:
                OverlaysList tab2 = new OverlaysList();
                return tab2;
            case 2:
                BootAnimation tab3 = new BootAnimation();
                return tab3;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}