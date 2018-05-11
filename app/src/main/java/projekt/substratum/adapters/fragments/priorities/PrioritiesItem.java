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

package projekt.substratum.adapters.fragments.priorities;

import android.graphics.drawable.Drawable;

public class PrioritiesItem implements PrioritiesInterface {

    private final String mName;
    private final Drawable mDrawableId;
    private String type1a;
    private String type1b;
    private String type1c;
    private String type2;
    private String type3;
    private String themeName;

    public PrioritiesItem(String name, Drawable drawable) {
        super();
        this.mName = name;
        this.mDrawableId = drawable;
    }

    @Override
    public PrioritiesItemType getType() {
        return PrioritiesItemType.CONTENT;
    }

    public String getName() {
        return this.mName;
    }

    Drawable getDrawableId() {
        return this.mDrawableId;
    }

    public String getType1a() {
        return this.type1a;
    }

    public void setType1a(String name) {
        this.type1a = name;
    }

    public String getType1b() {
        return this.type1b;
    }

    public void setType1b(String name) {
        this.type1b = name;
    }

    public String getType1c() {
        return this.type1c;
    }

    public void setType1c(String name) {
        this.type1c = name;
    }

    public String getType2() {
        return this.type2;
    }

    public void setType2(String name) {
        this.type2 = name;
    }

    public String getType3() {
        return this.type3;
    }

    public void setType3(String name) {
        this.type3 = name;
    }

    public String getThemeName() {
        return this.themeName;
    }

    void setThemeName(String name) {
        this.themeName = name;
    }
}