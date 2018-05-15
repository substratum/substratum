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

package projekt.substratum.common.systems;

public class ProfileItem {
    private String targetPackage;
    private String parentTheme;
    private String type1a;
    private String type1b;
    private String type1c;
    private String type2;
    private String type3;
    private String type4;

    ProfileItem(final String packageName) {
        super();
    }

    public String getTargetPackage() {
        return this.targetPackage;
    }

    void setTargetPackage(final String targetPackage) {
        this.targetPackage = targetPackage;
    }

    public String getParentTheme() {
        return this.parentTheme;
    }

    void setParentTheme(final String parentTheme) {
        this.parentTheme = parentTheme;
    }

    public String getType1a() {
        return this.type1a;
    }

    public void setType1a(final String type1a) {
        this.type1a = type1a;
    }

    public String getType1b() {
        return this.type1b;
    }

    public void setType1b(final String type1b) {
        this.type1b = type1b;
    }

    public String getType1c() {
        return this.type1c;
    }

    public void setType1c(final String type1c) {
        this.type1c = type1c;
    }

    public String getType2() {
        return this.type2;
    }

    public void setType2(final String type2) {
        this.type2 = type2;
    }

    public String getType3() {
        return this.type3;
    }

    public void setType3(final String type3) {
        this.type3 = type3;
    }

    public String getType4() {
        return this.type4;
    }

    public void setType4(final String type4) {
        this.type4 = type4;
    }
}