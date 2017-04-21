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

package projekt.substratum.adapters.studio;

import android.content.Context;

public class IconInfo {
    private String package_name, package_drawable, package_theme;
    private Context mContext;
    private byte[] mDrawable;
    private String parsedName;

    public IconInfo(Context mContext, String package_name,
                    String package_drawable, String package_theme, String parsedName) {
        this.mContext = mContext;
        this.package_name = package_name;
        this.package_drawable = package_drawable;
        this.package_theme = package_theme;
        this.parsedName = parsedName;
    }

    public String getPackageName() {
        return package_name;
    }

    public Context getContext() {
        return mContext;
    }

    public String getPackageDrawable() {
        return package_drawable;
    }

    String getThemePackage() {
        return package_theme;
    }

    public byte[] getDrawable() {
        return mDrawable;
    }

    public void setDrawable(byte[] drawable) {
        this.mDrawable = drawable;
    }

    public String getParsedName() {
        return parsedName;
    }

    void setParsedName(String parsedName) {
        this.parsedName = parsedName;
    }
}