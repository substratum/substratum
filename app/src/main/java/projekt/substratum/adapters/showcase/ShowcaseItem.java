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

import android.content.Context;
import android.support.annotation.NonNull;

public class ShowcaseItem implements Comparable<ShowcaseItem> {

    private Context mContext;
    private String themeName;
    private String themePackage;
    private String themeLink;
    private String themeIcon;
    private String themeBackgroundImage;
    private String themeAuthor;
    private String themePricing;
    private String themeSupport;

    public Context getContext() {
        return this.mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    CharSequence getThemeAuthor() {
        return this.themeAuthor;
    }

    public void setThemeAuthor(String themeAuthor) {
        this.themeAuthor = themeAuthor;
    }

    String getThemePricing() {
        return this.themePricing;
    }

    public void setThemePricing(String themePricing) {
        this.themePricing = themePricing;
    }

    public CharSequence getThemeName() {
        return this.themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    String getThemePackage() {
        return this.themePackage;
    }

    public void setThemePackage(String themePackage) {
        this.themePackage = themePackage;
    }

    String getThemeLink() {
        return this.themeLink;
    }

    public void setThemeLink(String themeLink) {
        this.themeLink = themeLink;
    }

    String getThemeIcon() {
        return this.themeIcon;
    }

    public void setThemeIcon(String themeIcon) {
        this.themeIcon = themeIcon;
    }

    String getThemeBackgroundImage() {
        return this.themeBackgroundImage;
    }

    public void setThemeBackgroundImage(String themeBackgroundImage) {
        this.themeBackgroundImage = themeBackgroundImage;
    }

    String getThemeSupport() {
        return this.themeSupport;
    }

    public void setThemeSupport(String themeSupport) {
        this.themeSupport = themeSupport;
    }

    @Override
    public int compareTo(@NonNull ShowcaseItem showcaseItem) {
        try {
            return themeName.compareToIgnoreCase(showcaseItem.themeName);
        } catch (NullPointerException e) {
            return 0;
        }
    }
}