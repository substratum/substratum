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

package projekt.substratum.adapters.activities;

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

import static projekt.substratum.common.Internal.SHUTDOWNANIMATION_INTENT;
import static projekt.substratum.common.References.bootAnimationsFolder;
import static projekt.substratum.common.References.fontsFolder;
import static projekt.substratum.common.References.overlaysFolder;
import static projekt.substratum.common.References.shutdownAnimationsFolder;
import static projekt.substratum.common.References.soundsFolder;

public class IATabsAdapter extends FragmentStatePagerAdapter {

    private Integer numOfTabs;
    private String wallpaperUrl;
    private Bundle bundle;
    private HashMap<String, Boolean> extras;
    private List<Object> packageChecker;

    public IATabsAdapter(FragmentManager fragmentManager,
                         Integer NumOfTabs,
                         List<? extends String> packageChecker,
                         String wallpaperUrl,
                         HashMap<String, Boolean> extras,
                         Bundle bundle) {
        super(fragmentManager);
        this.numOfTabs = NumOfTabs;
        try {
            this.packageChecker = new ArrayList<>(packageChecker);
        } catch (NullPointerException ignored) {
            // Suppress this warning for themeMode launches
        }
        this.wallpaperUrl = wallpaperUrl;
        this.bundle = bundle;
        this.extras = extras;
    }

    @Override
    public Fragment getItem(int position) {
        return this.getFragment();
    }

    @Override
    public int getCount() {
        return this.numOfTabs;
    }

    private Fragment getFragment() {
        if (this.packageChecker.contains(overlaysFolder)) {
            this.packageChecker.remove(overlaysFolder);
            Overlays overlays = new Overlays();
            overlays.setArguments(this.bundle);
            return overlays;
        } else if (this.packageChecker.contains(bootAnimationsFolder) &&
                this.extras.get(bootAnimationsFolder)) {
            this.packageChecker.remove(bootAnimationsFolder);
            BootAnimations bootAnimations = new BootAnimations();
            bootAnimations.setArguments(this.bundle);
            return bootAnimations;
        } else if (this.packageChecker.contains(shutdownAnimationsFolder) &&
                this.extras.get(shutdownAnimationsFolder)) {
            this.packageChecker.remove(shutdownAnimationsFolder);
            BootAnimations shutdownAnimations = new BootAnimations();
            Bundle b = new Bundle(this.bundle);
            b.putBoolean(SHUTDOWNANIMATION_INTENT, true);
            shutdownAnimations.setArguments(b);
            return shutdownAnimations;
        } else if (this.packageChecker.contains(fontsFolder) &&
                this.extras.get(fontsFolder)) {
            this.packageChecker.remove(fontsFolder);
            Fonts fonts = new Fonts();
            fonts.setArguments(this.bundle);
            return fonts;
        } else if (this.packageChecker.contains(soundsFolder) &&
                this.extras.get(soundsFolder)) {
            this.packageChecker.remove(soundsFolder);
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