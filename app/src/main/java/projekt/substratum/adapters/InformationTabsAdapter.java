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
import java.util.List;

import projekt.substratum.tabs.BootAnimation;
import projekt.substratum.tabs.FontInstaller;
import projekt.substratum.tabs.MainScreenTab;
import projekt.substratum.tabs.OverlaysList;
import projekt.substratum.tabs.SoundPackager;

public class InformationTabsAdapter extends FragmentStatePagerAdapter {
    public List package_checker;
    int mNumOfTabs;
    Context mContext;
    String theme_pid;
    Boolean allow_quick_apply;
    String theme_mode;

    public InformationTabsAdapter(FragmentManager fm, int NumOfTabs, Context context,
                                  String theme_pid, Boolean allow_quick_apply, String theme_mode,
                                  List package_checker) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
        this.mContext = context;
        this.theme_pid = theme_pid;
        this.allow_quick_apply = allow_quick_apply;
        this.theme_mode = theme_mode;
        this.package_checker = package_checker;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                if (allow_quick_apply) {
                    return new MainScreenTab();
                }
                if (theme_mode.equals("overlays")) {
                    return new OverlaysList();
                }
                if (theme_mode.equals("bootanimation")) {
                    return new BootAnimation();
                }
                if (theme_mode.equals("fonts")) {
                    return new FontInstaller();
                }
                if (theme_mode.equals("sounds")) {
                    return new SoundPackager();
                }
            case 1:
                if (package_checker.contains("overlays")) {
                    return new OverlaysList();
                }
                if (package_checker.contains("bootanimation")) {
                    return new BootAnimation();
                }
                if (package_checker.contains("fonts")) {
                    return new FontInstaller();
                }
                if (package_checker.contains("audio")) {
                    return new SoundPackager();
                }
            case 2:
                if (package_checker.contains("bootanimation")) {
                    return new BootAnimation();
                }
                if (package_checker.contains("fonts")) {
                    return new FontInstaller();
                }
                if (package_checker.contains("audio")) {
                    return new SoundPackager();
                }
            case 3:
                if (package_checker.contains("fonts")) {
                    return new FontInstaller();
                }
                if (package_checker.contains("audio")) {
                    return new SoundPackager();
                }
            case 4:
                if (package_checker.contains("audio")) {
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