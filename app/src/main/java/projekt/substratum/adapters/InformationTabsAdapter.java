package projekt.substratum.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.tabs.BootAnimation;
import projekt.substratum.tabs.FontInstaller;
import projekt.substratum.tabs.MainScreenTab;
import projekt.substratum.tabs.OverlaysList;
import projekt.substratum.tabs.SoundPackager;
import projekt.substratum.tabs.Wallpapers;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class InformationTabsAdapter extends FragmentStatePagerAdapter {
    public ArrayList<String> package_checker;
    int mNumOfTabs;
    Context mContext;
    String theme_pid;
    Boolean allow_quick_apply;
    String theme_mode;
    String wallpaperUrl;

    public InformationTabsAdapter(FragmentManager fm, int NumOfTabs, Context context,
                                  String theme_pid, Boolean allow_quick_apply, String theme_mode,
                                  List package_checker, String wallpaperUrl) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
        this.mContext = context;
        this.theme_pid = theme_pid;
        this.allow_quick_apply = allow_quick_apply;
        this.theme_mode = theme_mode;
        try {
            this.package_checker = new ArrayList<>(package_checker);
        } catch (NullPointerException npe) {
            // Suppress this warning for theme_mode launches
        }
        this.wallpaperUrl = wallpaperUrl;
    }

    @Override
    public Fragment getItem(int position) {
        if (theme_mode != null && theme_mode.length() > 0) {
            if (theme_mode.equals("overlays")) {
                return new OverlaysList();
            } else if (theme_mode.equals("bootanimation")) {
                return new BootAnimation();
            } else if (theme_mode.equals("fonts")) {
                return new FontInstaller();
            } else if (theme_mode.equals("audio")) {
                return new SoundPackager();
            } else if (theme_mode.equals("wallpapers")) {
                return new Wallpapers();
            }
        }
        return getFragment();
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }

    public Fragment getFragment() {
        if (allow_quick_apply) {
            allow_quick_apply = false;
            return new MainScreenTab();
        } else if (package_checker.contains("overlays")) {
            package_checker.remove("overlays");
            return new OverlaysList();
        } else if (package_checker.contains("bootanimation")) {
            package_checker.remove("bootanimation");
            return new BootAnimation();
        } else if (package_checker.contains("fonts")) {
            package_checker.remove("fonts");
            return new FontInstaller();
        } else if (package_checker.contains("audio")) {
            package_checker.remove("audio");
            return new SoundPackager();
        } else if (wallpaperUrl != null) {
            return new Wallpapers();
        }
        return null;
    }
}