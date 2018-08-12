/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.adapters.activities;

import android.content.Context;

public class ShowcaseItem {

    private Context context;
    private String themeName;
    private String themePackage;
    private String themeBackgroundImage;
    private String themeAuthor;
    private String themePricing;
    private boolean isInstalled;
    private boolean isPaid;

    public Context getContext() {
        return this.context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public CharSequence getThemeAuthor() {
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

    public String getThemePackage() {
        return this.themePackage;
    }

    public void setThemePackage(String themePackage) {
        this.themePackage = themePackage;
    }

    public String getThemeBackgroundImage() {
        return this.themeBackgroundImage;
    }

    public void setThemeBackgroundImage(String themeBackgroundImage) {
        this.themeBackgroundImage = themeBackgroundImage;
    }

    public boolean isInstalled() {
        return isInstalled;
    }

    public void setInstalled(boolean installed) {
        this.isInstalled = installed;
    }

    public boolean isPaid() {
        return isPaid;
    }

    void setPaid(boolean paid) {
        this.isPaid = paid;
    }
}