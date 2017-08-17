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
    private String packageName;
    private String targetPackage;
    private String parentTheme;
    private String type1a;
    private String type1b;
    private String type1c;
    private String type2;
    private String type3;
    private String type4;

    ProfileItem(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getTargetPackage() {
        return targetPackage;
    }

    public void setTargetPackage(String targetPackage) {
        this.targetPackage = targetPackage;
    }

    public String getParentTheme() {
        return parentTheme;
    }

    void setParentTheme(String parentTheme) {
        this.parentTheme = parentTheme;
    }

    public String getType1a() {
        return type1a;
    }

    public void setType1a(String type1a) {
        this.type1a = type1a;
    }

    public String getType1b() {
        return type1b;
    }

    public void setType1b(String type1b) {
        this.type1b = type1b;
    }

    public String getType1c() {
        return type1c;
    }

    public void setType1c(String type1c) {
        this.type1c = type1c;
    }

    public String getType2() {
        return type2;
    }

    public void setType2(String type2) {
        this.type2 = type2;
    }

    public String getType3() {
        return type3;
    }

    public void setType3(String type3) {
        this.type3 = type3;
    }

    public String getType4() {
        return type4;
    }

    public void setType4(String type4) {
        this.type4 = type4;
    }
}