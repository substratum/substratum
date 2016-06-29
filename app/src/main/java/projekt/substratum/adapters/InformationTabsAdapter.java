package projekt.substratum.adapters;

/**
 * @author Nicholas Chum (nicholaschum)
 */

import android.content.Context;
import android.content.res.AssetManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import java.util.Arrays;

import projekt.substratum.tabs.BootAnimation;
import projekt.substratum.tabs.FontInstaller;
import projekt.substratum.tabs.MainScreenTab;
import projekt.substratum.tabs.OverlaysList;
import projekt.substratum.tabs.SoundPackager;

public class InformationTabsAdapter extends FragmentStatePagerAdapter {
    public String[] package_checker;
    int mNumOfTabs;
    Context mContext;
    String theme_pid;

    public InformationTabsAdapter(FragmentManager fm, int NumOfTabs, Context context,
                                  String theme_pid) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
        this.mContext = context;
        this.theme_pid = theme_pid;
    }

    @Override
    public Fragment getItem(int position) {

        try {
            Context otherContext = mContext.createPackageContext(theme_pid, 0);
            AssetManager am = otherContext.getAssets();
            package_checker = am.list("");
        } catch (Exception e) {
            Log.e("SubstratumLogger", "Could not refresh list of asset folders.");
        }

        switch (position) {
            case 0:
                return new MainScreenTab();
            case 1:
                return new OverlaysList();
            case 2:
                if (Arrays.asList(package_checker).contains("bootanimation")) {
                    return new BootAnimation();
                }
                if (Arrays.asList(package_checker).contains("fonts")) {
                    return new FontInstaller();
                }
                if (Arrays.asList(package_checker).contains("audio")) {
                    return new SoundPackager();
                }
            case 3:
                if (Arrays.asList(package_checker).contains("fonts")) {
                    return new FontInstaller();
                }
                if (Arrays.asList(package_checker).contains("audio")) {
                    return new SoundPackager();
                }
            case 4:
                if (Arrays.asList(package_checker).contains("audio")) {
                    return new SoundPackager();
                }
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}