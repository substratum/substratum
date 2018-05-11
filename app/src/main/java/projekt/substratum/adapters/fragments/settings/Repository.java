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

package projekt.substratum.adapters.fragments.settings;

public class Repository {
    private final String packageName;
    private String bools;
    private String colors;
    private String dimens;
    private String styles;

    public Repository(String packageName) {
        super();
        this.packageName = packageName;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public String getBools() {
        return this.bools;
    }

    public void setBools(String bools) {
        this.bools = bools;
    }

    public String getColors() {
        return this.colors;
    }

    public void setColors(String colors) {
        this.colors = colors;
    }

    public String getDimens() {
        return this.dimens;
    }

    public void setDimens(String dimens) {
        this.dimens = dimens;
    }

    public String getStyles() {
        return this.styles;
    }

    public void setStyles(String styles) {
        this.styles = styles;
    }
}