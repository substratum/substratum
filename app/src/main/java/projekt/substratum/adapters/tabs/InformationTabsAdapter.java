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

package projekt.substratum.adapters.tabs;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.tabs.BootAnimations;
import projekt.substratum.tabs.Fonts;
import projekt.substratum.tabs.Overlays;
import projekt.substratum.tabs.Sounds;
import projekt.substratum.tabs.Wallpapers;

import static projekt.substratum.common.References.bootAnimationsFragment;
import static projekt.substratum.common.References.fontsFragment;
import static projekt.substratum.common.References.overlaysFragment;
import static projekt.substratum.common.References.shutdownAnimationsFragment;
import static projekt.substratum.common.References.soundsFragment;
import static projekt.substratum.common.References.wallpaperFragment;

public class InformationTabsAdapter extends FragmentStatePagerAdapter {

    private ArrayList package_checker;
    private Integer mNumOfTabs;
    private String theme_mode;
    private String wallpaperUrl;
    private Bundle bundle;
    private boolean[] extras;

    @SuppressWarnings("unchecked")
    public InformationTabsAdapter(FragmentManager fm, int NumOfTabs, String theme_mode,
                                  List package_checker, String wallpaperUrl, boolean[] extras,
                                  Bundle bundle) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
        this.theme_mode = theme_mode;
        try {
            this.package_checker = new ArrayList<>(package_checker);
        } catch (NullPointerException npe) {
            // Suppress this warning for theme_mode launches
        }
        this.wallpaperUrl = wallpaperUrl;
        this.bundle = bundle;
        this.extras = extras;
    }

    @Override
    public Fragment getItem(int position) {
        if (theme_mode != null && theme_mode.length() > 0) {
            switch (theme_mode) {
                case overlaysFragment:
                    Overlays overlays = new Overlays();
                    overlays.setArguments(bundle);
                    return overlays;
                case bootAnimationsFragment:
                    BootAnimations bootAnimations = new BootAnimations();
                    bootAnimations.setArguments(bundle);
                    return bootAnimations;
                case shutdownAnimationsFragment:
                    BootAnimations shutdownanimations = new BootAnimations();
                    Bundle b = new Bundle(bundle);
                    b.putBoolean("shutdownanimation", true);
                    shutdownanimations.setArguments(b);
                    return shutdownanimations;
                case fontsFragment:
                    Fonts fonts = new Fonts();
                    fonts.setArguments(bundle);
                    return fonts;
                case soundsFragment:
                    Sounds sounds = new Sounds();
                    sounds.setArguments(bundle);
                    return sounds;
                case wallpaperFragment:
                    Wallpapers wallpapers = new Wallpapers();
                    wallpapers.setArguments(bundle);
                    return wallpapers;
            }
        }
        return getFragment();
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }

    private Fragment getFragment() {
        if (package_checker.contains(overlaysFragment)) {
            package_checker.remove(overlaysFragment);
            Overlays overlays = new Overlays();
            overlays.setArguments(bundle);
            return overlays;
        } else if (package_checker.contains(bootAnimationsFragment) && extras[0]) {
            package_checker.remove(bootAnimationsFragment);
            BootAnimations bootAnimations = new BootAnimations();
            bootAnimations.setArguments(bundle);
            return bootAnimations;
        } else if (package_checker.contains(shutdownAnimationsFragment) && extras[1]) {
            package_checker.remove(shutdownAnimationsFragment);
            BootAnimations shutdownAnimations = new BootAnimations();
            Bundle b = new Bundle(bundle);
            b.putBoolean("shutdownanimation", true);
            shutdownAnimations.setArguments(b);
            return shutdownAnimations;
        } else if (package_checker.contains(fontsFragment) && extras[2]) {
            package_checker.remove(fontsFragment);
            Fonts fonts = new Fonts();
            fonts.setArguments(bundle);
            return fonts;
        } else if (package_checker.contains(soundsFragment) && extras[3]) {
            package_checker.remove(soundsFragment);
            Sounds sounds = new Sounds();
            sounds.setArguments(bundle);
            return sounds;
        } else if (wallpaperUrl != null) {
            Wallpapers wallpapers = new Wallpapers();
            wallpapers.setArguments(bundle);
            return wallpapers;
        }
        return null;
    }
}