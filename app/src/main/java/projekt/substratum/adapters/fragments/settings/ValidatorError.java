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

import java.util.ArrayList;

public class ValidatorError {
    private ArrayList<String> bools = new ArrayList<>();
    private ArrayList<String> colors = new ArrayList<>();
    private ArrayList<String> dimens = new ArrayList<>();
    private ArrayList<String> styles = new ArrayList<>();
    private String packageName;

    public ValidatorError(String packageName) {
        this.packageName = packageName;
    }

    public void addBoolError(String bools) {
        this.bools.add(bools);
    }

    public void addColorError(String colors) {
        this.colors.add(colors);
    }

    public void addDimenError(String dimens) {
        this.dimens.add(dimens);
    }

    public void addStyleError(String styles) {
        this.styles.add(styles);
    }

    public String getPackageName() {
        return this.packageName;
    }

    ArrayList<String> getBoolErrors() {
        return this.bools;
    }

    ArrayList<String> getColorErrors() {
        return this.colors;
    }

    ArrayList<String> getDimenErrors() {
        return this.dimens;
    }

    ArrayList<String> getStyleErrors() {
        return this.styles;
    }
}