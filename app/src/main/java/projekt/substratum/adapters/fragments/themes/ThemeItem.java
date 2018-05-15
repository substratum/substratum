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