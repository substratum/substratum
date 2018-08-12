/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.adapters.fragments.themes;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;

import static projekt.substratum.common.References.dynamicallyResize;

public class ThemeItem {

    private String themeName;
    private String themeAuthor;
    private String themePackage;
    private Drawable themeDrawable;
    private Context themeContext;

    public String getThemeName() {
        return this.themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    public String getThemeAuthor() {
        return this.themeAuthor;
    }

    public void setThemeAuthor(String themeAuthor) {
        this.themeAuthor = themeAuthor;
    }

    String getThemePackage() {
        return this.themePackage;
    }

    public void setThemePackage(String themePackage) {
        this.themePackage = themePackage;
    }

    public Drawable getThemeDrawable() {
        return this.themeDrawable;
    }

    public void setThemeDrawable(Drawable drawable) {
        this.themeDrawable = drawable instanceof VectorDrawable ? drawable : dynamicallyResize(drawable);
    }

    public Context getContext() {
        return this.themeContext;
    }

    public void setContext(Context context) {
        this.themeContext = context;
    }

}