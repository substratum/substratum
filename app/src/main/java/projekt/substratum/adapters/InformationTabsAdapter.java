package projekt.substratum.adapters;

/**
 * @author Nicholas Chum (nicholaschum)
 */

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

import projekt.substratum.tabs.BootAnimation;
import projekt.substratum.tabs.FontInstaller;
import projekt.substratum.tabs.MainScreenTab;
import projekt.substratum.tabs.OverlaysList;
import projekt.substratum.tabs.SoundPackager;
import projekt.substratum.tabs.Wallpapers;

public class InformationTabsAdapter extends FragmentStatePagerAdapter {
    public List package_checker;
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
        this.package_checker = package_checker;
        this.wallpaperUrl = wallpaperUrl;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
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
                if (theme_mode.equals("wallpapers")) {
                    return new Wallpapers();
                }
                if (allow_quick_apply) {
                    return new MainScreenTab();
                } else {
                    return new OverlaysList();
                }
            case 1:
                if (package_checker.contains("overlays") &&
                        allow_quick_apply) {
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
                if (wallpaperUrl != null) {
                    return new Wallpapers();
                }
            case 2:
                if (package_checker.contains("bootanimation") &&
                        package_checker.contains("overlays") &&
                        allow_quick_apply) {
                    return new BootAnimation();
                }
                if (package_checker.contains("fonts")) {
                    return new FontInstaller();
                }
                if (package_checker.contains("audio")) {
                    return new SoundPackager();
                }
                if (wallpaperUrl != null) {
                    return new Wallpapers();
                }
            case 3:
                if (package_checker.contains("fonts") &&
                        package_checker.contains("bootanimation") &&
                        package_checker.contains("overlays") &&
                        allow_quick_apply) {
                    return new FontInstaller();
                }
                if (package_checker.contains("audio")) {
                    return new SoundPackager();
                }
                if (wallpaperUrl != null) {
                    return new Wallpapers();
                }
            case 4:
                if (package_checker.contains("audio") &&
                        package_checker.contains("fonts") &&
                        package_checker.contains("bootanimation") &&
                        package_checker.contains("overlays") &&
                        allow_quick_apply) {
                    return new SoundPackager();
                }
                if (wallpaperUrl != null) {
                    return new Wallpapers();
                }
            case 5:
                if (wallpaperUrl != null &&
                        package_checker.contains("audio") &&
                        package_checker.contains("fonts") &&
                        package_checker.contains("bootanimation") &&
                        package_checker.contains("overlays") &&
                        allow_quick_apply) {
                    return new Wallpapers();
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