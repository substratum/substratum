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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import projekt.substratum.tabs.BootAnimations;
import projekt.substratum.tabs.Fonts;
import projekt.substratum.tabs.Overlays;
import projekt.substratum.tabs.Sounds;
import projekt.substratum.tabs.Wallpapers;

import static projekt.substratum.common.Internal.SHUTDOWN_ANIMATION;
import static projekt.substratum.common.References.bootAnimationsFragment;
import static projekt.substratum.common.References.fontsFragment;
import static projekt.substratum.common.References.overlaysFragment;
import static projekt.substratum.common.References.shutdownAnimationsFragment;
import static projekt.substratum.common.References.soundsFragment;
import static projekt.substratum.common.References.wallpaperFragment;

public class InformationTabsAdapter extends FragmentStatePagerAdapter {

    private Integer mNumOfTabs;
    private String theme_mode;
    private String wallpaperUrl;
    private Bundle bundle;
    private HashMap<String, Boolean> extras;
    private List package_checker;

    @SuppressWarnings("unchecked")
    public InformationTabsAdapter(FragmentManager fragmentManager,
                                  Integer NumOfTabs,
                                  String theme_mode,
                                  List package_checker,
                                  String wallpaperUrl,
                                  HashMap<String, Boolean> extras,
                                  Bundle bundle) {
        super(fragmentManager);
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
        if ((this.theme_mode != null) && !this.theme_mode.isEmpty()) {
            switch (this.theme_mode) {
                case overlaysFragment:
                    Overlays overlays = new Overlays();
                    overlays.setArguments(this.bundle);
                    return overlays;
                case bootAnimationsFragment:
                    BootAnimations bootAnimations = new BootAnimations();
                    bootAnimations.setArguments(this.bundle);
                    return bootAnimations;
                case shutdownAnimationsFragment:
                    BootAnimations shutdownanimations = new BootAnimations();
                    Bundle b = new Bundle(this.bundle);
                    b.putBoolean(SHUTDOWN_ANIMATION, true);
                    shutdownanimations.setArguments(b);
                    return shutdownanimations;
                case fontsFragment:
                    Fonts fonts = new Fonts();
                    fonts.setArguments(this.bundle);
                    return fonts;
                case soundsFragment:
                    Sounds sounds = new Sounds();
                    sounds.setArguments(this.bundle);
                    return sounds;
                case wallpaperFragment:
                    Wallpapers wallpapers = new Wallpapers();
                    wallpapers.setArguments(this.bundle);
                    return wallpapers;
            }
        }
        return this.getFragment();
    }

    @Override
    public int getCount() {
        return this.mNumOfTabs;
    }

    private Fragment getFragment() {
        if (this.package_checker.contains(overlaysFragment)) {
            this.package_checker.remove(overlaysFragment);
            Overlays overlays = new Overlays();
            overlays.setArguments(this.bundle);
            return overlays;
        } else if (this.package_checker.contains(bootAnimationsFragment) &&
                this.extras.get(bootAnimationsFragment)) {
            this.package_checker.remove(bootAnimationsFragment);
            BootAnimations bootAnimations = new BootAnimations();
            bootAnimations.setArguments(this.bundle);
            return bootAnimations;
        } else if (this.package_checker.contains(shutdownAnimationsFragment) &&
                this.extras.get(shutdownAnimationsFragment)) {
            this.package_checker.remove(shutdownAnimationsFragment);
            BootAnimations shutdownAnimations = new BootAnimations();
            Bundle b = new Bundle(this.bundle);
            b.putBoolean(shutdownAnimationsFragment, true);
            shutdownAnimations.setArguments(b);
            return shutdownAnimations;
        } else if (this.package_checker.contains(fontsFragment) &&
                this.extras.get(fontsFragment)) {
            this.package_checker.remove(fontsFragment);
            Fonts fonts = new Fonts();
            fonts.setArguments(this.bundle);
            return fonts;
        } else if (this.package_checker.contains(soundsFragment) &&
                this.extras.get(soundsFragment)) {
            this.package_checker.remove(soundsFragment);
            Sounds sounds = new Sounds();
            sounds.setArguments(this.bundle);
            return sounds;
        } else if (this.wallpaperUrl != null) {
            Wallpapers wallpapers = new Wallpapers();
            wallpapers.setArguments(this.bundle);
            return wallpapers;
        }
        return null;
    }
}